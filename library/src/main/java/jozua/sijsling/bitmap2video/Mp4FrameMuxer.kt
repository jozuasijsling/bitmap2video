package jozua.sijsling.bitmap2video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/*
 * Copyright (C) 2023 Jozua Sijsling
 * Copyright (C) 2020 Homesoft, LLC
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

class Mp4FrameMuxer(path: String, fps: Float) : FrameMuxer {

    private val frameUsec: Long = (TimeUnit.SECONDS.toMicros(1L) / fps).toLong()
    private val muxer: MediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private var started = false
    private var videoTrackIndex = 0
    private var audioTrackIndex = 0
    private var videoFrames = 0
    private var finalVideoTime: Long = 0

    override fun isStarted(): Boolean {
        return started
    }

    override fun start(videoFormat: MediaFormat, audioExtractor: MediaExtractor?) {
        // now that we have the Magic Goodies, start the muxer
        audioExtractor?.selectTrack(0)
        val audioFormat = audioExtractor?.getTrackFormat(0)
        videoTrackIndex = muxer.addTrack(videoFormat)
        audioFormat?.run {
            audioTrackIndex = muxer.addTrack(audioFormat)
        }
        muxer.start()
        started = true
    }

    override fun muxVideoFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        // This code will break if the encoder supports B frames.
        // Ideally we would use set the value in the encoder,
        // don't know how to do that without using OpenGL
        finalVideoTime = frameUsec * videoFrames++
        bufferInfo.presentationTimeUs = finalVideoTime

        muxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
    }

    override fun muxAudioFrame(encodedData: ByteBuffer, audioBufferInfo: MediaCodec.BufferInfo) {
        muxer.writeSampleData(audioTrackIndex, encodedData, audioBufferInfo)
    }

    override fun release() {
        muxer.stop()
        muxer.release()
    }

    override fun getVideoTime(): Long {
        return finalVideoTime
    }
}
