package com.orioooneee.lmuasister.ui.profile

internal class QrCodeMatrix private constructor(
    val size: Int,
    private val modules: BooleanArray,
) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    companion object {
        fun encode(text: String): QrCodeMatrix? {
            val payload = text.encodeToByteArray()
            val info = VersionInfo.all.firstOrNull { version ->
                val countBits = if (version.version <= 9) 8 else 16
                4 + countBits + payload.size * 8 <= version.dataCodewords * 8
            } ?: return null
            val data = encodeData(payload, info)
            val codewords = addErrorCorrection(data, info)
            return drawCodewords(codewords, info)
        }

        private fun encodeData(payload: ByteArray, info: VersionInfo): IntArray {
            val bits = BitBuffer()
            bits.append(0x4, 4)
            bits.append(payload.size, if (info.version <= 9) 8 else 16)
            payload.forEach { bits.append(it.toInt() and 0xFF, 8) }

            val capacityBits = info.dataCodewords * 8
            bits.append(0, minOf(4, capacityBits - bits.size))
            while (bits.size % 8 != 0) bits.append(false)

            val padBytes = intArrayOf(0xEC, 0x11)
            var padIndex = 0
            while (bits.size < capacityBits) {
                bits.append(padBytes[padIndex % padBytes.size], 8)
                padIndex++
            }

            return IntArray(info.dataCodewords) { index -> bits.byteAt(index) }
        }

        private fun addErrorCorrection(data: IntArray, info: VersionInfo): IntArray {
            val blocks = mutableListOf<Block>()
            var offset = 0
            for (length in info.dataBlockLengths) {
                val blockData = data.copyOfRange(offset, offset + length)
                blocks += Block(blockData, reedSolomonRemainder(blockData, info.eccCodewordsPerBlock))
                offset += length
            }

            val result = mutableListOf<Int>()
            val maxDataLength = blocks.maxOf { it.data.size }
            for (i in 0 until maxDataLength) {
                blocks.forEach { if (i < it.data.size) result += it.data[i] }
            }
            for (i in 0 until info.eccCodewordsPerBlock) {
                blocks.forEach { result += it.ecc[i] }
            }
            return result.toIntArray()
        }

        private fun drawCodewords(codewords: IntArray, info: VersionInfo): QrCodeMatrix {
            val size = info.size
            val modules = BooleanArray(size * size)
            val function = BooleanArray(size * size)

            fun index(x: Int, y: Int) = y * size + x
            fun set(x: Int, y: Int, value: Boolean, isFunction: Boolean = true) {
                if (x !in 0 until size || y !in 0 until size) return
                modules[index(x, y)] = value
                if (isFunction) function[index(x, y)] = true
            }

            fun drawFinder(left: Int, top: Int) {
                for (dy in -1..7) {
                    for (dx in -1..7) {
                        val x = left + dx
                        val y = top + dy
                        if (x !in 0 until size || y !in 0 until size) continue
                        val black = dx in 0..6 && dy in 0..6 &&
                            (dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4))
                        set(x, y, black)
                    }
                }
            }

            fun drawAlignment(cx: Int, cy: Int) {
                if (function[index(cx, cy)]) return
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        set(cx + dx, cy + dy, distance != 1)
                    }
                }
            }

            drawFinder(0, 0)
            drawFinder(size - 7, 0)
            drawFinder(0, size - 7)

            for (i in 0 until size) {
                if (!function[index(i, 6)]) set(i, 6, i % 2 == 0)
                if (!function[index(6, i)]) set(6, i, i % 2 == 0)
            }

            info.alignmentPositions.forEach { y ->
                info.alignmentPositions.forEach { x ->
                    drawAlignment(x, y)
                }
            }

            reserveFormat(size, function)

            val dataBits = codewords.flatMap { byte -> (7 downTo 0).map { bit -> ((byte ushr bit) and 1) != 0 } }
            var bitIndex = 0
            var upward = true
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right--
                val yRange = if (upward) size - 1 downTo 0 else 0 until size
                for (y in yRange) {
                    for (x in right downTo right - 1) {
                        val idx = index(x, y)
                        if (function[idx]) continue
                        modules[idx] = dataBits.getOrNull(bitIndex) == true
                        bitIndex++
                    }
                }
                upward = !upward
                right -= 2
            }

            val mask = 0
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val idx = index(x, y)
                    if (!function[idx] && (x + y) % 2 == 0) {
                        modules[idx] = !modules[idx]
                    }
                }
            }
            drawFormatBits(size, mask, modules, function)

            return QrCodeMatrix(size, modules)
        }

        private fun reserveFormat(size: Int, function: BooleanArray) {
            fun mark(x: Int, y: Int) {
                function[y * size + x] = true
            }
            for (i in 0..8) {
                if (i != 6) {
                    mark(8, i)
                    mark(i, 8)
                }
            }
            for (i in 0 until 8) mark(size - 1 - i, 8)
            for (i in 0 until 7) mark(8, size - 1 - i)
            mark(8, size - 8)
        }

        private fun drawFormatBits(size: Int, mask: Int, modules: BooleanArray, function: BooleanArray) {
            fun set(x: Int, y: Int, value: Boolean) {
                modules[y * size + x] = value
                function[y * size + x] = true
            }
            val bits = formatBits(mask)
            for (i in 0..5) set(8, i, bits bit i)
            set(8, 7, bits bit 6)
            set(8, 8, bits bit 7)
            set(7, 8, bits bit 8)
            for (i in 9 until 15) set(14 - i, 8, bits bit i)

            for (i in 0 until 8) set(size - 1 - i, 8, bits bit i)
            for (i in 8 until 15) set(8, size - 15 + i, bits bit i)
            set(8, size - 8, true)
        }

        private fun formatBits(mask: Int): Int {
            val errorCorrectionLevelLow = 1
            val data = (errorCorrectionLevelLow shl 3) or mask
            var rem = data
            repeat(10) {
                rem = (rem shl 1) xor if (((rem ushr 9) and 1) != 0) 0x537 else 0
            }
            return ((data shl 10) or rem) xor 0x5412
        }

        private infix fun Int.bit(index: Int): Boolean = ((this ushr index) and 1) != 0

        private fun reedSolomonRemainder(data: IntArray, degree: Int): IntArray {
            val generator = reedSolomonGenerator(degree)
            val result = IntArray(degree)
            for (byte in data) {
                val factor = byte xor result[0]
                for (i in 0 until degree - 1) result[i] = result[i + 1]
                result[degree - 1] = 0
                for (i in 0 until degree) {
                    result[i] = result[i] xor gfMultiply(generator[i], factor)
                }
            }
            return result
        }

        private fun reedSolomonGenerator(degree: Int): IntArray {
            var coefficients = intArrayOf(1)
            var root = 1
            repeat(degree) {
                val next = IntArray(coefficients.size + 1)
                for (i in coefficients.indices) {
                    next[i] = next[i] xor coefficients[i]
                    next[i + 1] = next[i + 1] xor gfMultiply(coefficients[i], root)
                }
                coefficients = next
                root = gfMultiply(root, 0x02)
            }
            return coefficients.copyOfRange(1, coefficients.size)
        }

        private fun gfMultiply(x: Int, y: Int): Int {
            var a = x
            var b = y
            var product = 0
            while (b != 0) {
                if ((b and 1) != 0) product = product xor a
                a = a shl 1
                if ((a and 0x100) != 0) a = a xor 0x11D
                b = b ushr 1
            }
            return product and 0xFF
        }
    }
}

private data class VersionInfo(
    val version: Int,
    val dataBlockLengths: IntArray,
    val eccCodewordsPerBlock: Int,
    val alignmentPositions: IntArray,
) {
    val size: Int = 17 + version * 4
    val dataCodewords: Int = dataBlockLengths.sum()

    companion object {
        val all = listOf(
            VersionInfo(1, intArrayOf(19), 7, intArrayOf()),
            VersionInfo(2, intArrayOf(34), 10, intArrayOf(6, 18)),
            VersionInfo(3, intArrayOf(55), 15, intArrayOf(6, 22)),
            VersionInfo(4, intArrayOf(80), 20, intArrayOf(6, 26)),
            VersionInfo(5, intArrayOf(108), 26, intArrayOf(6, 30)),
            VersionInfo(6, intArrayOf(68, 68), 18, intArrayOf(6, 34)),
        )
    }
}

private data class Block(val data: IntArray, val ecc: IntArray)

private class BitBuffer {
    private val bits = mutableListOf<Boolean>()
    val size: Int get() = bits.size

    fun append(value: Int, length: Int) {
        for (i in length - 1 downTo 0) {
            append(((value ushr i) and 1) != 0)
        }
    }

    fun append(value: Boolean) {
        bits += value
    }

    fun byteAt(index: Int): Int {
        var value = 0
        repeat(8) { bit ->
            if (bits[index * 8 + bit]) value = value or (1 shl (7 - bit))
        }
        return value
    }
}
