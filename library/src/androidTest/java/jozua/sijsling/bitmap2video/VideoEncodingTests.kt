package jozua.sijsling.bitmap2video

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jozua.sijsling.bitmap2video.test.R as testR
import org.junit.Test
import java.io.File
import java.util.*

class VideoEncodingTests {

    @Test
    fun t4() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val dir = File(application.filesDir, UUID.randomUUID().toString()).apply {
            mkdirs()
        }
        val file = File(dir, "video.mp4")
        file.deleteOnExit()
        try {
            val bitmaps = listOf(testR.raw.im1, testR.raw.im2, testR.raw.im3, testR.raw.im4)
                .map(::loadBitmapFromRawRes)
            mux(bitmaps, file)
            verifyBitmapToVideoFrames(bitmaps, file)
        } finally {
            file.delete()
        }
    }

}