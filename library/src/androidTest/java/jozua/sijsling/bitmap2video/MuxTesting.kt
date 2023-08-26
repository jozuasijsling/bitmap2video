package jozua.sijsling.bitmap2video

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import java.io.File

fun loadBitmapFromRawRes(@RawRes image: Int): Bitmap {
    val resources = ApplicationProvider.getApplicationContext<Application>().resources
    return BitmapFactory.decodeResource(resources, image)
}

fun mux(bitmaps: List<Bitmap>, outFile: File) {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val config = MuxerConfig(
        outFile,
        videoWidth = bitmaps.minOf { it.width },
        videoHeight = bitmaps.minOf { it.height },
        mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
        framesPerSecond = 1F,
    )
    FrameBuilder(application, config, null).apply {
        start()
        for (bitmap in bitmaps) {
            createFrame(bitmap)
        }
        releaseVideoCodec()
        releaseMuxer()
    }
}

fun verifyBitmapToVideoFrames(bitmaps: List<Bitmap>, video: File) {
    val retriever = MediaMetadataRetriever().apply {
        setDataSource(video.path)
    }
    bitmaps.forEachIndexed { i, bitmap ->
        val frameTimeUs = i * 1_000_000L
        val videoFrame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)!!
        File(video.parentFile, "frame-$i-out.jpg").outputStream().use { stream ->
            videoFrame.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        File(video.parentFile, "frame-$i-in.jpg").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        // TODO compare bitmap against video frame, how?
    }

}