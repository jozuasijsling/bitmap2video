package jozua.sijsling.bitmap2video

import android.media.MediaFormat
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

data class MuxerConfig(
    val file: File,
    val videoWidth: Int = 320,
    val videoHeight: Int = 240,
    val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    val framesPerImage: Int = 1,
    val framesPerSecond: Float = 10F,
    val bitrate: Int = 1500000,
    val frameMuxer: FrameMuxer = Mp4FrameMuxer(file.absolutePath, framesPerSecond),
    val iFrameInterval: Int = 10,
)

interface MuxingCompletionListener {
    fun onVideoSuccessful(file: File)
    fun onVideoError(error: Throwable)
}

sealed interface MuxingResult
data class MuxingSuccess(val file: File): MuxingResult
data class MuxingError(val message: String, val exception: Exception): MuxingResult
