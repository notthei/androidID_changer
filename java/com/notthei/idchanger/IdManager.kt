package com.notthei.idchanger

import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Manages fake Android ID generation.
 *
 * Strategy:
 *   fakeId(pkg) = SHA-256(seed + ":" + packageName).take(16 hex chars)
 *
 * The seed is installed by the companion Magisk module at:
 *   /data/adb/idchanger/seed  (chmod 644, world-readable)
 *
 * This gives:
 *   - Per-app unique IDs (different packages → different IDs)
 *   - Stable across reboots (same seed → same IDs)
 *   - Random across reinstalls (new seed on reinstall)
 *   - No per-app file writes needed at hook time
 */
object IdManager {

    private const val TAG = "IdChanger/IdManager"
    private const val SEED_PATH = "/data/adb/idchanger/seed"

    /** Lazily read the seed once per process lifecycle. */
    private val seed: String by lazy { readSeed() }

    /**
     * Returns a stable fake Android ID for the given package name.
     * The returned string is 16 lowercase hex characters, matching the
     * standard Android ID format.
     */
    fun getFakeId(packageName: String): String {
        val input = "${seed}:${packageName}"
        val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        // Take first 8 bytes → 16 hex chars (same length as real ANDROID_ID)
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun readSeed(): String {
        return try {
            val seed = File(SEED_PATH).readText().trim()
            if (seed.isNotEmpty()) {
                Log.d(TAG, "Seed loaded from $SEED_PATH")
                seed
            } else {
                Log.w(TAG, "Seed file is empty, using fallback seed")
                fallbackSeed()
            }
        } catch (e: Exception) {
            // Seed file not found (Magisk module not installed or first boot).
            // Use a stable fallback derived from a build property so IDs are
            // still consistent within a device even without the Magisk module.
            Log.w(TAG, "Cannot read seed: ${e.message}. Using fallback seed.")
            fallbackSeed()
        }
    }

    /** Fallback: derive seed from stable build fingerprint bytes. */
    private fun fallbackSeed(): String {
        val fingerprint = android.os.Build.FINGERPRINT
        val hash = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
