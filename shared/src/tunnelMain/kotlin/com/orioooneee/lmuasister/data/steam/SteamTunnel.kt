package com.orioooneee.lmuasister.data.steam

/*
 * TUNNEL_DISABLED:
 * Device egress tunneling is no longer part of the active auth path. The relay remains
 * here as commented reference code, but should not compile into any target.
 *
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * Device egress tunnel: opens a WebSocket to the sidecar's /agent, then relays raw TCP
 * for every stream the sidecar opens (so the sidecar's Steam login egresses from THIS
 * device's IP). Multiplexed by a 4-byte stream id.
 *
 * Wire frame (binary):  [streamId:uint32 BE][type:byte][payload]
 *   1 OPEN  payload = "host:port"  (sidecar→device: dial this, raw TCP, NO TLS)
 *   2 DATA  payload = bytes        (both ways)
 *   3 CLOSE payload = (empty)
 */
internal class SteamTunnel(
    private val agentUrl: String,
    private val token: String,
    private val scope: CoroutineScope,
) {
    private val client = tunnelWsClient()
    private val selector = SelectorManager(Dispatchers.Default)
    // sidecar→steam bytes per stream. Created at OPEN (before the dial finishes) so DATA that
    // races ahead of connect is buffered in order instead of dropped. UNLIMITED = trySend never
    // blocks the WS receive loop.
    private val outbox = HashMap<UInt, Channel<ByteArray>>()
    private val sockets = HashMap<UInt, Socket>()
    private val dialMarks = HashMap<UInt, TimeSource.Monotonic.ValueTimeMark>() // per-stream t0 for phase timing
    private val firstUp = HashSet<UInt>() // streams that already logged their first sidecar→steam byte
    private val lock = Mutex()
    private val sendLock = Mutex()
    private val connected = CompletableDeferred<Unit>()
    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null

    suspend fun connect() {
        SteamLog.d("tunnel: connecting WS → $agentUrl (token=${SteamLog.short(token)})")
        job = scope.launch {
            try {
                client.webSocket("$agentUrl?token=$token") {
                    session = this
                    connected.complete(Unit)
                    SteamLog.d("tunnel: WS connected")
                    for (frame in incoming) {
                        if (frame is Frame.Binary) handleFrame(frame.readBytes())
                    }
                    SteamLog.d("tunnel: WS receive loop ended (closed by peer)")
                }
            } catch (t: Throwable) {
                SteamLog.e("tunnel: WS failed", t)
                if (!connected.isCompleted) connected.completeExceptionally(t)
            } finally {
                closeAll()
            }
        }
        connected.await()
    }

    fun close() {
        SteamLog.d("tunnel: closing")
        job?.cancel()
        runCatching { client.close() }
        runCatching { selector.close() }
    }

    private suspend fun handleFrame(buf: ByteArray) {
        if (buf.size < 5) return
        val id = ((buf[0].toUInt() and 0xFFu) shl 24) or ((buf[1].toUInt() and 0xFFu) shl 16) or
            ((buf[2].toUInt() and 0xFFu) shl 8) or (buf[3].toUInt() and 0xFFu)
        val payload = buf.copyOfRange(5, buf.size)
        when (buf[4].toInt()) {
            1 -> openStream(id, payload.decodeToString())
            2 -> {
                var box: Channel<ByteArray>? = null
                var firstMark: TimeSource.Monotonic.ValueTimeMark? = null
                lock.withLock {
                    box = outbox[id]
                    if (box != null && firstUp.add(id)) firstMark = dialMarks[id]
                }
                val chan = box
                if (chan != null) {
                    firstMark?.let { SteamLog.d("[dial] stream=$id first → steam ${it.elapsedNow().inWholeMilliseconds}ms") }
                    chan.trySend(payload) // buffered; the dial coroutine drains it into the socket in order
                }
            }
            3 -> closeStream(id)
        }
    }

    private suspend fun openStream(id: UInt, hostPort: String) {
        // t0 for this stream — every phase below is logged as ms since recv OPEN.
        val mark = TimeSource.Monotonic.markNow()
        fun ms() = mark.elapsedNow().inWholeMilliseconds
        val sep = hostPort.lastIndexOf(':')
        val host = hostPort.substring(0, sep)
        val port = hostPort.substring(sep + 1).toIntOrNull()
        SteamLog.d("[dial] stream=$id $hostPort recv OPEN")
        if (port == null) { SteamLog.e("[dial] stream=$id $hostPort bad port"); return }
        // Register the outbox NOW, before dialing. The sidecar replies SOCKS-success
        // optimistically and SteamKit fires the TLS ClientHello immediately, so DATA can
        // arrive while we're still connecting — buffering it here is what stopped Steam from
        // waiting on a never-delivered handshake until its ~35s idle timeout.
        val box = Channel<ByteArray>(Channel.UNLIMITED)
        lock.withLock { outbox[id] = box; dialMarks[id] = mark }
        scope.launch {
            // 1. DNS — measured on its own, IPv4-first (see resolveDialAddresses) so a dead
            //    IPv6 route can't stall the connect ~35s on the first AAAA.
            val addrs = runCatching { resolveDialAddresses(host) }
                .getOrElse { SteamLog.e("[dial] stream=$id $host DNS failed ${ms()}ms", it); listOf(host) }
            SteamLog.d("[dial] stream=$id $host DNS ${ms()}ms -> " +
                addrs.joinToString { a -> "$a${if (a.contains(':')) "(v6)" else "(v4)"}" })

            // 2. TCP connect — happy-eyeballs: each address gets a short explicit budget so we
            //    fall through to the next family fast instead of hanging on the first.
            var socket: Socket? = null
            var chosen: String? = null
            for ((i, addr) in addrs.withIndex()) {
                val budget = if (i == addrs.lastIndex) LAST_CONNECT_MS else PER_CONNECT_MS
                val s = withTimeoutOrNull(budget) {
                    runCatching {
                        aSocket(selector).tcp().connect(InetSocketAddress(addr, port)) { noDelay = true }
                    }.onFailure { SteamLog.e("[dial] stream=$id connect $addr failed ${ms()}ms", it) }.getOrNull()
                }
                if (s != null) { socket = s; chosen = addr; break }
                SteamLog.e("[dial] stream=$id connect $addr gave up after ${budget}ms (${ms()}ms total)")
            }
            val sock = socket
            if (sock == null) {
                SteamLog.e("[dial] stream=$id $host:$port NO ADDRESS connected ${ms()}ms")
                runCatching { sendFrame(id, 3, ByteArray(0)) }
                closeStream(id)
                return@launch
            }
            SteamLog.d("[dial] stream=$id $host:$port CONNECTED via $chosen ${ms()}ms")

            val write = sock.openWriteChannel(autoFlush = true)
            val read = sock.openReadChannel()
            lock.withLock { sockets[id] = sock }
            // Drain buffered + subsequent sidecar→steam bytes into the socket, in order.
            val pump = launch {
                runCatching {
                    for (chunk in box) { write.writeFully(chunk, 0, chunk.size); write.flush() }
                }.onFailure { SteamLog.e("[dial] stream=$id $host:$port write error", it) }
            }
            var why = "eof"
            try {
                val b = ByteArray(32 * 1024)
                var firstDown = true
                while (true) {
                    val n = read.readAvailable(b, 0, b.size)
                    if (n == -1) break
                    if (n > 0) {
                        if (firstDown) { SteamLog.d("[dial] stream=$id $host:$port first ← steam ${ms()}ms"); firstDown = false }
                        sendFrame(id, 2, b.copyOfRange(0, n))
                    }
                }
            } catch (t: Throwable) {
                why = t.message ?: t::class.simpleName ?: "error"
                SteamLog.e("[dial] stream=$id $host:$port relay error", t)
            } finally {
                pump.cancel()
                runCatching { sendFrame(id, 3, ByteArray(0)) }
                closeStream(id)
                SteamLog.d("[dial] stream=$id $host:$port CLOSED ${ms()}ms reason=$why")
            }
        }
    }

    private suspend fun closeStream(id: UInt) {
        val socket: Socket?
        val box: Channel<ByteArray>?
        lock.withLock {
            box = outbox.remove(id)
            socket = sockets.remove(id)
            dialMarks.remove(id); firstUp.remove(id)
        }
        box?.close()
        runCatching { socket?.close() }
    }

    private suspend fun closeAll() {
        val socks: List<Socket>
        val boxes: List<Channel<ByteArray>>
        lock.withLock {
            socks = sockets.values.toList(); boxes = outbox.values.toList()
            sockets.clear(); outbox.clear(); dialMarks.clear(); firstUp.clear()
        }
        boxes.forEach { runCatching { it.close() } }
        socks.forEach { runCatching { it.close() } }
    }

    private suspend fun sendFrame(id: UInt, type: Int, payload: ByteArray) {
        val buf = ByteArray(5 + payload.size)
        buf[0] = ((id shr 24) and 0xFFu).toByte()
        buf[1] = ((id shr 16) and 0xFFu).toByte()
        buf[2] = ((id shr 8) and 0xFFu).toByte()
        buf[3] = (id and 0xFFu).toByte()
        buf[4] = type.toByte()
        payload.copyInto(buf, 5)
        val s = session ?: return
        sendLock.withLock { runCatching { s.send(Frame.Binary(true, buf)) } }
    }

    private companion object {
        const val PER_CONNECT_MS = 3_000L // short budget per non-final address → fail fast, try next family
        const val LAST_CONNECT_MS = 10_000L // last address gets the full budget
    }
}
*/
