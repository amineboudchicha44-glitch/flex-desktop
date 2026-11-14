package com.example.data.cache

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class IconCacheStats(
    val memoryCachedCount: Int = 0,
    val diskCachedCount: Int = 0,
    val diskSizeKb: Long = 0L,
    val memoryHits: Int = 0,
    val diskHits: Int = 0,
    val pmLoads: Int = 0,
    val warmUpMs: Long = 0L,
    val memoryBudgetKb: Int = 0
)

class IconCacheManager(context: Context) {

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "icon_cache").apply { mkdirs() }

    private var memoryBudgetKb: Int = computeBudgetKb(liteMode = false)
    private var memoryCache: LruCache<String, CachedIconEntry> = createLruCache(memoryBudgetKb)

    private data class CachedIconEntry(
        val imageBitmap: ImageBitmap,
        val byteSizeKb: Int
    )

    private var memoryHits = 0
    private var diskHits = 0
    private var pmLoads = 0
    private var warmUpMs = 0L

    private val _stats = MutableStateFlow(IconCacheStats(memoryBudgetKb = memoryBudgetKb))
    val stats: StateFlow<IconCacheStats> = _stats.asStateFlow()

    fun configureMemoryBudget(liteMode: Boolean) {
        val targetKb = computeBudgetKb(liteMode)
        if (targetKb != memoryBudgetKb) {
            memoryBudgetKb = targetKb
            memoryCache.trimToSize(targetKb)
            publishStats()
        }
    }

    suspend fun warmCacheFromDisk(packageNames: List<String>) = withContext(Dispatchers.IO) {
        val startNs = System.nanoTime()
        val pm = appContext.packageManager
        for (pkg in packageNames.take(36)) {
            if (memoryCache.get(pkg) == null) {
                val diskFile = diskFileFor(pkg)
                if (diskFile.exists()) {
                    decodeSampledBitmap(diskFile)?.let { bmp ->
                        val entry = CachedIconEntry(bmp.asImageBitmap(), (bmp.allocationByteCount / 1024).coerceAtLeast(1))
                        memoryCache.put(pkg, entry)
                        diskHits++
                    }
                } else {
                    loadFromPackageManagerAndPersist(pkg, pm)
                }
            }
        }
        warmUpMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
        publishStats()
    }

    suspend fun getOrLoadIcon(packageName: String): ImageBitmap? {
        memoryCache.get(packageName)?.let {
            memoryHits++
            publishStats()
            return it.imageBitmap
        }
        return withContext(Dispatchers.IO) {
            val diskFile = diskFileFor(packageName)
            if (diskFile.exists()) {
                decodeSampledBitmap(diskFile)?.let { bmp ->
                    val entry = CachedIconEntry(bmp.asImageBitmap(), (bmp.allocationByteCount / 1024).coerceAtLeast(1))
                    memoryCache.put(packageName, entry)
                    diskHits++
                    publishStats()
                    return@withContext entry.imageBitmap
                }
            }
            val loaded = loadFromPackageManagerAndPersist(packageName, appContext.packageManager)
            publishStats()
            loaded
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
        memoryHits = 0
        diskHits = 0
        pmLoads = 0
        warmUpMs = 0L
        publishStats()
    }

    private fun loadFromPackageManagerAndPersist(packageName: String, pm: PackageManager): ImageBitmap? {
        return try {
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToScaledBitmap(drawable, ICON_TARGET_PX)
            val diskFile = diskFileFor(packageName)
            FileOutputStream(diskFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            val entry = CachedIconEntry(bitmap.asImageBitmap(), (bitmap.allocationByteCount / 1024).coerceAtLeast(1))
            memoryCache.put(packageName, entry)
            pmLoads++
            entry.imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(file: File): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToScaledBitmap(drawable: Drawable, targetPx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width == targetPx && src.height == targetPx) {
                return src
            }
            return Bitmap.createScaledBitmap(src, targetPx, targetPx, true)
        }
        val bitmap = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun diskFileFor(packageName: String): File {
        val safeName = packageName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
        return File(cacheDir, "$safeName.png")
    }

    private fun publishStats() {
        val files = cacheDir.listFiles() ?: emptyArray()
        val diskBytes = files.sumOf { it.length() }
        _stats.value = IconCacheStats(
            memoryCachedCount = memoryCache.putCount() - memoryCache.evictionCount(),
            diskCachedCount = files.size,
            diskSizeKb = (diskBytes / 1024L),
            memoryHits = memoryHits,
            diskHits = diskHits,
            pmLoads = pmLoads,
            warmUpMs = warmUpMs,
            memoryBudgetKb = memoryBudgetKb
        )
    }

    private fun createLruCache(maxKb: Int): LruCache<String, CachedIconEntry> {
        return object : LruCache<String, CachedIconEntry>(maxKb) {
            override fun sizeOf(key: String, value: CachedIconEntry): Int = value.byteSizeKb
        }
    }

    private companion object {
        const val ICON_TARGET_PX = 96

        fun computeBudgetKb(liteMode: Boolean): Int {
            val maxHeapKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
            return if (liteMode) {
                (maxHeapKb / 24).coerceIn(2048, 6144)
            } else {
                (maxHeapKb / 12).coerceIn(4096, 16384)
            }
        }
    }
}
