/*
 * Copyright (c) 2020. Carlos René Ramos López. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crrl.beatplayer.utils

import android.content.ContentUris.withAppendedId
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.view.Surface.*
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.utils.PlayerConstants.ARTWORK_URI
import java.io.*


object GeneralUtils {

    const val VERTICAL = 0
    const val HORIZONTAL = 1

    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    val screenHeight: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    @Throws(IllegalArgumentException::class)
    fun getRotation(context: Context): Int {
        val rotation =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        return when (rotation) {
            ROTATION_0, ROTATION_180 -> VERTICAL
            ROTATION_90 -> HORIZONTAL
            else -> HORIZONTAL
        }
    }

    fun formatMilliseconds(duration: Long): String {
        val seconds = (duration / 1000).toInt() % 60
        val minutes = (duration / (1000 * 60) % 60).toInt()
        val hours = (duration / (1000 * 60 * 60) % 24).toInt()
        "${timeAddZeros(hours, false)}:${timeAddZeros(minutes)}:${timeAddZeros(seconds)}".apply {
            return if (this[0] == ':') replaceFirst(":", "") else this
        }
    }

    fun getTotalTime(songList: List<Song>): Long {
        var minutes = 0L
        var hours = 0L
        var seconds = 0L
        for (song in songList) {
            seconds += (song.duration / 1000 % 60).toLong()
            minutes += (song.duration / (1000 * 60) % 60).toLong()
            hours += (song.duration / (1000 * 60 * 60) % 24).toLong()
        }
        return hours * (1000 * 60 * 60) + minutes * (1000 * 60) + seconds * 1000
    }

    fun audio2Raw(context: Context, uri: Uri): ByteArray? {
        val parcelFileDescriptor = try {
            context.contentResolver.openFileDescriptor(uri, PlayerConstants.READ_ONLY_MODE, null)
                ?: return null
        } catch (ex: FileNotFoundException) {
            return null
        }
        val fis = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val data = try {
            fis.readBytes()
        } catch (ex: Exception) {
            audio2Raw(context, uri)
        }
        fis.close()
        return data
    }

    @Throws(IOException::class, InterruptedIOException::class)
    fun toByteArray(input: InputStream, size: Int): ByteArray? {
        require(size >= 0) { "Size must be equal or greater than zero: $size" }
        if (size == 0) {
            return ByteArray(0)
        }
        val data = ByteArray(size)
        var offset = 0
        var read = input.read(data, offset, size - offset)
        while (offset < size && read != PlayerConstants.EOF) {
            offset += read
            read = input.read(data, offset, size - offset)
        }
        if (offset != size) {
            throw IOException("Unexpected readed size. current: $offset, excepted: $size")
        }
        return data
    }

    fun toggleShowKeyBoard(context: Context?, editText: EditText, show: Boolean) {
        if (show) {
            editText.apply {
                requestFocus()
                val imm =
                    context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm!!.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            editText.apply {
                clearFocus()
                val imm =
                    context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm!!.hideSoftInputFromWindow(editText.windowToken, 0)
            }
        }
    }

    fun dip2px(context: Context, dpValue: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun addZeros(number: Int?): String {
        if (number!! < 10) return "00${number}"
        if (number < 100) return "0${number}"
        return number.toString()
    }

    private fun timeAddZeros(number: Int?, showIfIsZero: Boolean = true): String {
        return when (number) {
            0 -> if (showIfIsZero) "0${number}" else ""
            1, 2, 3, 4, 5, 6, 7, 8, 9 -> "0${number}"
            else -> number.toString()
        }
    }

    fun getAlbumArtUri(albumId: Long): Uri = withAppendedId(ARTWORK_URI, albumId)
}
