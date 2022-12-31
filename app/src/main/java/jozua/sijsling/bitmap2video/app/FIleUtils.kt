package jozua.sijsling.bitmap2video.app

import android.content.Context
import java.io.File

/*
 * Copyright (C) 2023 Jozua Sijsling
 * Copyright (C) 2019 Israel Flores
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

object FileUtils {
    private const val MEDIA_FILE_PATH = "media"

    /**
     * Get a File object where we will be storing the video
     *
     * @param context - Activity or application context
     * @param fileName - name of the file
     * @return - created file object at media/fileName
     */
    fun getVideoFile(context: Context, fileName: String): File {
        return getVideoFile(context, MEDIA_FILE_PATH, fileName)
    }

    /**
     * Get a File object where we will be storing the video
     *
     * @param context - Activity or application context
     * @param fileDir - name of directory where video file is stored
     * IMPORTANT: if setting this, you must provide the appropriate `paths`
     * in your xml resources {@see file_paths.xml}, as well as set up your
     * provider with the appropriate file paths.
     * @param fileName - name of the file
     * @return - created file object at fileDir/fileName
     */
    fun getVideoFile(
        context: Context, fileDir: String,
        fileName: String
    ): File {
        val mediaFolder = File(context.filesDir, fileDir)
        // Create the directory if it does not exist
        if (!mediaFolder.exists()) mediaFolder.mkdirs()
        return File(mediaFolder, fileName)
    }
}
