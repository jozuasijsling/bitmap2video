package jozua.sijsling.bitmap2video.app

import android.media.MediaFormat
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jozua.sijsling.bitmap2video.app.FileUtils.getVideoFile
import jozua.sijsling.bitmap2video.*
import jozua.sijsling.bitmap2video.app.databinding.ActivityMainBinding
import java.io.File

/*
 * Copyright (C) 2023 Jozua Sijsling
 * Copyright (C) 2020 Israel Flores
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class MainActivity : AppCompatActivity() {
    companion object {
        val imageArray: List<Int> = listOf(
            R.raw.im1,
            R.raw.im2,
            R.raw.im3,
            R.raw.im4
        )
    }

    private var videoFile: File? = null
    private var muxerConfig: MuxerConfig? = null
    private var mimeType = MediaFormat.MIMETYPE_VIDEO_AVC

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        viewBinding.avc.isEnabled = isCodecSupported(mimeType)

        setListeners()
    }

    private fun setListeners() {
        viewBinding.apply {
            btMake.setOnClickListener {
                btMake.isEnabled = false

                basicVideoCreation()
            }

            avc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) setCodec(MediaFormat.MIMETYPE_VIDEO_AVC)
            }

            hevc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) setCodec(MediaFormat.MIMETYPE_VIDEO_HEVC)
            }

            btPlay.setOnClickListener {
                videoFile?.run {
                    player.setVideoPath(this.absolutePath)
                    player.start()
                }
            }
        }
    }

    private fun setCodec(codec: String) {
        if (isCodecSupported(codec)) {
            mimeType = codec
            muxerConfig = muxerConfig?.copy(mimeType = mimeType)
        } else {
            Toast.makeText(this@MainActivity, "Codec $codec not supported", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    // Basic implementation
    private fun basicVideoCreation() {
        videoFile = getVideoFile(this@MainActivity, "test.mp4")
        videoFile?.run {
            muxerConfig = MuxerConfig(this, 600, 600, mimeType, 3, 1F, 1500000)
            val muxer = Muxer(this@MainActivity, muxerConfig!!)

            createVideo(muxer) // using callbacks
        }
    }

    // Callback-style approach
    private fun createVideo(muxer: Muxer) {
        muxer.setOnMuxingCompletedListener(object : MuxingCompletionListener {
            override fun onVideoSuccessful(file: File) {
                onMuxerCompleted()
            }

            override fun onVideoError(error: Throwable) {
                onMuxerCompleted()
            }
        })

        // Needs to happen on a background thread (long-running process)
        Thread {
            muxer.mux(imageArray, R.raw.bensound_happyrock)
        }.start()
    }

    private fun onMuxerCompleted() {
        runOnUiThread {
            viewBinding.btMake.isEnabled = true
            viewBinding.btPlay.isEnabled = true
        }
    }
}
