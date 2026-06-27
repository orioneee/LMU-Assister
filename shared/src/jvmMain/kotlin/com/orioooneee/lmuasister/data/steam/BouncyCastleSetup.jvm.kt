package com.orioooneee.lmuasister.data.steam

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@Volatile
private var bouncyCastleReady = false

internal fun ensureBouncyCastleProvider() {
    if (bouncyCastleReady) return
    synchronized(BouncyCastleProvider::class.java) {
        if (bouncyCastleReady) return
        runCatching {
            if (Security.getProvider("BC") !is BouncyCastleProvider) {
                Security.removeProvider("BC")
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            }
        }
        bouncyCastleReady = true
    }
}

