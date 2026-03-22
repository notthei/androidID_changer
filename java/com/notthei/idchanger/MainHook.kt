package com.notthei.idchanger

import android.content.ContentResolver
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * LSPosed/Xposed hook entry point.
 *
 * Hooks Settings.Secure.getString() in every app process.
 * When the key is "android_id", returns a per-package fake ID
 * instead of the real device ID.
 *
 * Also hooks the 3-argument variant getString(cr, name, default) and
 * getStringForUser(cr, name, userId) to cover all code paths.
 */
class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "IdChanger"
        private const val ANDROID_ID_KEY = "android_id"
        private const val SETTINGS_SECURE = "android.provider.Settings\$Secure"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Skip our own package to avoid self-interference
        if (lpparam.packageName == "com.notthei.idchanger") return

        val fakeId = IdManager.getFakeId(lpparam.packageName)
        Log.i(TAG, "Hooking ${lpparam.packageName} → fake android_id = $fakeId")

        hookGetString2(lpparam, fakeId)
        hookGetString3(lpparam, fakeId)
        hookGetStringForUser(lpparam, fakeId)
    }

    /**
     * Hook: Settings.Secure.getString(ContentResolver cr, String name)
     */
    private fun hookGetString2(lpparam: LoadPackageParam, fakeId: String) {
        try {
            XposedHelpers.findAndHookMethod(
                SETTINGS_SECURE,
                lpparam.classLoader,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == ANDROID_ID_KEY) {
                            param.result = fakeId
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to hook getString(2): ${e.message}")
        }
    }

    /**
     * Hook: Settings.Secure.getString(ContentResolver cr, String name, String default)
     * Available since API 19.
     */
    private fun hookGetString3(lpparam: LoadPackageParam, fakeId: String) {
        try {
            XposedHelpers.findAndHookMethod(
                SETTINGS_SECURE,
                lpparam.classLoader,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == ANDROID_ID_KEY) {
                            param.result = fakeId
                        }
                    }
                }
            )
        } catch (_: Throwable) {
            // 3-arg variant may not exist on all ROMs; silently ignore
        }
    }

    /**
     * Hook: Settings.Secure.getStringForUser(ContentResolver cr, String name, int userId)
     * This is the internal method that getString() delegates to on most ROMs.
     * Hooking both ensures we catch every call path.
     */
    private fun hookGetStringForUser(lpparam: LoadPackageParam, fakeId: String) {
        try {
            XposedHelpers.findAndHookMethod(
                SETTINGS_SECURE,
                lpparam.classLoader,
                "getStringForUser",
                ContentResolver::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == ANDROID_ID_KEY) {
                            param.result = fakeId
                        }
                    }
                }
            )
        } catch (_: Throwable) {
            // Not all ROMs expose getStringForUser; silently ignore
        }
    }
}
