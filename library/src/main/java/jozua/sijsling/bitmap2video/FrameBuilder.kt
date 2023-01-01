package jozua.sijsling.bitmap2video

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.*
import android.media.MediaCodecList.REGULAR_CODECS
import android.view.Surface
import androidx.annotation.RawRes
import java.io.IOException
import java.nio.ByteBuffer

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

private const val SECOND_IN_USEC = 1000000
private const val TIMEOUT_USEC = 10000

class FrameBuilder(
    private val context: Context,
    private val muxerConfig: MuxerConfig,
    @RawRes private val audioTrackResource: Int?
) {

    private val mediaFormat: MediaFormat = MediaFormat.createVideoFormat(
        muxerConfig.mimeType,
        muxerConfig.videoWidth,
        muxerConfig.videoHeight,
    ).apply {
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        setInteger(MediaFormat.KEY_BIT_RATE, muxerConfig.bitrate)
        setFloat(MediaFormat.KEY_FRAME_RATE, muxerConfig.framesPerSecond)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, muxerConfig.iFrameInterval)
    }

    private val mediaCodec: MediaCodec = MediaCodec.createByCodecName(
        MediaCodecList(REGULAR_CODECS).findEncoderForFormat(mediaFormat)
    )

    private val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private var frameMuxer: FrameMuxer = muxerConfig.frameMuxer

    private var surface: Surface? = null
    private var rect: Rect? = null

    private var audioExtractor: MediaExtractor? = if (audioTrackResource != null) {
        val assetFileDescriptor: AssetFileDescriptor =
            context.resources.openRawResourceFd(audioTrackResource)
        val extractor = MediaExtractor()
        extractor.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )
        extractor
    } else {
        null
    }

    /**
     * @throws IOException
     */
    fun start() {
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = mediaCodec.createInputSurface()
        mediaCodec.start()
        drainCodec(false)
    }

    fun createFrame(image: Any) {
        for (i in 0 until muxerConfig.framesPerImage) {
            val canvas = createCanvas()
            when (image) {
                is Int -> {
                    val bitmap = BitmapFactory.decodeResource(context.resources, image)
                    drawBitmapAndPostCanvas(bitmap, canvas)
                }
                is Bitmap -> drawBitmapAndPostCanvas(image, canvas)
                is Canvas -> postCanvasFrame(image)
                else -> error("Image type $image is not supported. Try using a Canvas or a Bitmap")
            }
        }
    }

    private fun createCanvas(): Canvas? {
        val supportsHardwareCanvas = true
        return if (supportsHardwareCanvas) {
            surface?.lockHardwareCanvas()
        } else {
            surface?.lockCanvas(rect)
        }
    }

    /**
     *
     * @param canvas acquired from createCanvas()
     */
    private fun drawBitmapAndPostCanvas(bitmap: Bitmap, canvas: Canvas?) {
        canvas?.drawBitmap(bitmap, 0f, 0f, null)
        postCanvasFrame(canvas)
    }

    /**
     *
     * @param canvas acquired from createCanvas()
     */
    private fun postCanvasFrame(canvas: Canvas?) {
        surface?.unlockCanvasAndPost(canvas)
        drainCodec(false)
    }

    /**
     * Extracts all pending data from the encoder.
     *
     *
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     *
     * Borrows heavily from https://bigflake.com/mediacodec/EncodeAndMuxTest.java.txt
     */
    private fun drainCodec(endOfStream: Boolean) {
        if (endOfStream) {
            mediaCodec.signalEndOfInputStream()
        }
        var encoderOutputBuffers: Array<ByteBuffer?>? = mediaCodec.outputBuffers
        while (true) {
            val encoderStatus: Int = mediaCodec.dequeueOutputBuffer(
                bufferInfo, TIMEOUT_USEC.toLong()
            )
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mediaCodec.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (frameMuxer.isStarted()) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat: MediaFormat = mediaCodec.outputFormat

                // now that we have the Magic Goodies, start the muxer
                frameMuxer.start(newFormat, audioExtractor)
            } else if (encoderStatus < 0) {
                // unexpected result from encoder.dequeueOutputBuffer: $encoderStatus
                // let's ignore it
            } else {
                val encodedData = encoderOutputBuffers?.get(encoderStatus)
                    ?: throw RuntimeException("encoderOutputBuffer  $encoderStatus was null")
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!frameMuxer.isStarted()) {
                        throw RuntimeException("muxer hasn't started")
                    }
                    frameMuxer.muxVideoFrame(encodedData, bufferInfo)
                }
                mediaCodec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    // reached end of stream unexpectedly
                    break // out of while
                }
            }
        }
    }

    fun muxAudioFrames() {
        if (audioExtractor == null) return
        val sampleSize = 256 * 1024
        val offset = 100
        val audioBuffer = ByteBuffer.allocate(sampleSize)
        val audioBufferInfo = MediaCodec.BufferInfo()
        var sawEOS = false
        audioExtractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        var finalAudioTime: Long
        val finalVideoTime: Long = frameMuxer.getVideoTime()
        var audioTrackFrameCount = 0
        while (!sawEOS) {
            audioBufferInfo.offset = offset
            audioBufferInfo.size = audioExtractor!!.readSampleData(audioBuffer, offset)
            if (audioBufferInfo.size < 0) {
                audioBufferInfo.size = 0
                sawEOS = true
            } else {
                finalAudioTime = audioExtractor!!.sampleTime
                audioBufferInfo.presentationTimeUs = finalAudioTime
                audioBufferInfo.flags = audioExtractor!!.sampleFlags
                frameMuxer.muxAudioFrame(audioBuffer, audioBufferInfo)
                audioExtractor!!.advance()
                audioTrackFrameCount++
                // We want the sound to play for a few more seconds after the last image
                if ((finalAudioTime > finalVideoTime) &&
                    (finalAudioTime % finalVideoTime > muxerConfig.framesPerImage * SECOND_IN_USEC)
                ) {
                    sawEOS = true
                }
            }
        }
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    fun releaseVideoCodec() {
        // Release the video layer
        drainCodec(true)
        mediaCodec.stop()
        mediaCodec.release()
        surface?.release()
    }

    fun releaseAudioExtractor() {
        audioExtractor?.release()
    }

    fun releaseMuxer() {
        // Release MediaMuxer
        frameMuxer.release()
    }

}
