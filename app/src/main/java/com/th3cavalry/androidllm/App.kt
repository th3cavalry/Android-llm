package com.th3cavalry.androidllm

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.google.gson.Gson
import com.th3cavalry.androidllm.service.InferenceBackend

class App : Application() {

    companion object {
        lateinit var instance: App
            private set

        val gson: Gson by lazy { Gson() }
    }

    /** Listeners that want to react to system low-memory signals. */
    private val memoryListeners = mutableListOf<() -> Unit>()

    /**
     * Application-scoped backend cache. Keeps the last loaded [InferenceBackend]
     * alive across ViewModel recreation so models don't need to reinitialize.
     * Cleared on system memory pressure or explicit [releaseBackendCache].
     */
    @Volatile
    var cachedBackend: InferenceBackend? = null
        private set

    lateinit var boxStore: io.objectbox.BoxStore
        private set

    /** Cache the given backend, closing the previous one if it's a different type. */
    fun cacheBackend(backend: InferenceBackend) {
        val current = cachedBackend
        if (current !== backend) {
            current?.close()
        }
        cachedBackend = backend
    }

    /** Release and close the cached backend, freeing its resources. */
    fun releaseBackendCache() {
        cachedBackend?.close()
        cachedBackend = null
    }

    fun addMemoryPressureListener(listener: () -> Unit) {
        memoryListeners.add(listener)
    }

    fun removeMemoryPressureListener(listener: () -> Unit) {
        memoryListeners.remove(listener)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize ObjectBox
        try {
            boxStore = com.th3cavalry.androidllm.data.MyObjectBox.builder()
                .androidContext(this)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("App", "Failed to initialize ObjectBox: ${e.message}")
        }

        // Migrate any legacy plaintext secrets to encrypted storage
        Prefs.migrateSecretsToEncrypted(this)

        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    android.util.Log.w("SovereignDroid", "System memory pressure (level=$level), releasing cached backend")
                    releaseBackendCache()
                    memoryListeners.toList().forEach { it() }
                }
            }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            @Deprecated("Deprecated in API level 34")
            override fun onLowMemory() {
                releaseBackendCache()
                memoryListeners.toList().forEach { it() }
            }
        })
    }
}
