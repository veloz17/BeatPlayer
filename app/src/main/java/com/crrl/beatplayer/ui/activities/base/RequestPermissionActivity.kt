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

package com.crrl.beatplayer.ui.activities.base

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crrl.beatplayer.models.Favorite
import com.crrl.beatplayer.repository.FavoritesRepository
import com.crrl.beatplayer.repository.PlaylistRepository
import com.crrl.beatplayer.utils.DBHelper
import com.crrl.beatplayer.utils.PlayerConstants.FAVORITE_ID
import com.crrl.beatplayer.utils.PlayerConstants.FAVORITE_NAME
import com.crrl.beatplayer.utils.PlayerConstants.FAVORITE_TYPE


open class RequestPermissionActivity : AppCompatActivity() {

    protected var permissionsGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verify()
    }

    private fun verify() {
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isWriteStoragePermissionGranted()
            isReadStoragePermissionGranted()
        } else {
            permissionsGranted = true
        }
        val frF = FavoritesRepository(this)
        val frP = PlaylistRepository(this)
        try {
            createFavList(frF)
        } catch (ex: SQLiteException) {
            createDB(frF)
        }
        try {
            frP.getPlayLists()
        } catch (ex: SQLiteException) {
            createDB(frP)
        }
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), 3)
            false
        }
    }

    private fun createFavList(favoritesRepository: FavoritesRepository) {
        val favorite = favoritesRepository.getFavorite(FAVORITE_ID)
        if (favorite.id == -1L) {
            favoritesRepository.createFavorite(
                Favorite(
                    FAVORITE_ID,
                    FAVORITE_NAME,
                    FAVORITE_NAME,
                    FAVORITE_ID,
                    0,
                    0,
                    FAVORITE_TYPE
                )
            )
        }
    }

    private fun createDB(fr: DBHelper) {
        fr.onCreate(fr.writableDatabase)
    }

    private fun isWriteStoragePermissionGranted(): Boolean {
        return if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), 2)
            false
        }
    }

    protected open fun recreateActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recreateActivity()
                } else {
                    finish()
                }
            }
        }
    }
}
