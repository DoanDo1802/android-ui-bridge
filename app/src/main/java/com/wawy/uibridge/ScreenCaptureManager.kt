package com.wawy.uibridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ScreenCaptureManager {
    @Volatile private var resultCode: Int? = null
    @Volatile private var resultData: Intent? = null
    @Volatile private var mediaProjection: MediaProjection? = null

    fun saveGrant(code: Int, data: Intent?) {
        resultCode = code
        resultData = data
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun hasGrant(): Boolean = resultCode != null && resultData != null

    private fun getProjection(context: Context): MediaProjection? {
        mediaProjection?.let { return it }
        val code = resultCode ?: return null
        val data = resultData ?: return null
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(code, data)
        return mediaProjection
    }

    fun captureToFile(context: Context, outFile: File): Boolean {
        val projection = getProjection(context) ?: return false
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val density = dm.densityDpi

        val thread = HandlerThread("capture-thread")
        thread.start()
        val handler = Handler(thread.looper)

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null
        val latch = CountDownLatch(1)
        var success = false

        try {
            virtualDisplay = projection.createVirtualDisplay(
                "uibridge-capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

            imageReader.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bmp = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bmp.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
                    FileOutputStream(outFile).use { fos ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    }
                    image.close()
                    bmp.recycle()
                    cropped.recycle()
                    success = outFile.exists() && outFile.length() > 0
                } catch (_: Exception) {
                    success = false
                } finally {
                    latch.countDown()
                }
            }, handler)

            latch.await(1800, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            success = false
        } finally {
            imageReader.setOnImageAvailableListener(null, null)
            virtualDisplay?.release()
            imageReader.close()
            thread.quitSafely()
        }

        return success
    }
}
