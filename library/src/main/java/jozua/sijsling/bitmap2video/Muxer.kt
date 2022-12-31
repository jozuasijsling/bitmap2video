package jozua.sijsling.bitmap2video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.util.Log
import androidx.annotation.RawRes
import java.io.File
import java.io.IOException

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

class Muxer(private val context: Context, private val file: File) {

    constructor(context: Context, config: MuxerConfig) : this(context, config.file) {
        muxerConfig = config
    }

    // Initialize a default configuration
    var muxerConfig: MuxerConfig = MuxerConfig(file)

    private var muxingCompletionListener: MuxingCompletionListener? = null
    /**
     * List containing images in any of the following formats:
     * [Bitmap] [@DrawRes Int] [Canvas]
     */
    fun mux(imageList: List<Any>,
            @RawRes audioTrack: Int? = null): MuxingResult {
        // Returns on a callback a finished video
        Log.d(TAG, "Generating video")
        val frameBuilder = FrameBuilder(context, muxerConfig, audioTrack)

        try {
            frameBuilder.start()
        } catch (e: IOException) {
            Log.e(TAG, "Start Encoder Failed")
            e.printStackTrace()
            muxingCompletionListener?.onVideoError(e)
            return MuxingError("Start encoder failed", e)
        }

        for (image in imageList) {
            frameBuilder.createFrame(image)
        }

        // Release the video codec so we can mux in the audio frames separately
        frameBuilder.releaseVideoCodec()

        // Add audio
        frameBuilder.muxAudioFrames()

        // Release everything
        frameBuilder.releaseAudioExtractor()
        frameBuilder.releaseMuxer()

        muxingCompletionListener?.onVideoSuccessful(file)
        return MuxingSuccess(file)
    }

    fun setOnMuxingCompletedListener(muxingCompletionListener: MuxingCompletionListener) {
        this.muxingCompletionListener = muxingCompletionListener
    }

    companion object {
        private val TAG = Muxer::class.java.simpleName
    }
}

fun isCodecSupported(mimeType: String?): Boolean {
    val codecs = MediaCodecList(REGULAR_CODECS)
    for (codec in codecs.codecInfos) {
        if (!codec.isEncoder) {
            continue
        }
        for (type in codec.supportedTypes) {
            if (type == mimeType) return true
        }
    }
    return false
}
