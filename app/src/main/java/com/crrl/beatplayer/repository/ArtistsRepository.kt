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

package com.crrl.beatplayer.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Artists.Albums.*
import com.crrl.beatplayer.extensions.toList
import com.crrl.beatplayer.models.Album
import com.crrl.beatplayer.models.Artist
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.utils.SettingsUtility
import com.crrl.beatplayer.utils.SortModes

interface ArtistsRepositoryInterface {
    fun getArtist(id: Long): Artist?
    fun getAllArtist(): List<Artist>
    fun getSongsForArtist(artistId: Long): List<Song>
    fun getAlbumsForArtist(artistId: Long): List<Album>
}

class ArtistsRepository() : ArtistsRepositoryInterface {

    private lateinit var contentResolver: ContentResolver
    private lateinit var settingsUtility: SettingsUtility

    companion object {
        private var instance: ArtistsRepository? = null

        fun getInstance(context: Context?): ArtistsRepository? {
            if (instance == null)
                instance = ArtistsRepository(context)
            return instance
        }
    }

    constructor(context: Context? = null) : this() {
        contentResolver = context!!.contentResolver
        settingsUtility = SettingsUtility.getInstance(context)
    }

    override fun getArtist(id: Long): Artist? {
        makeArtistCursor("_id=?", arrayOf(id.toString())).use {
            return if (it.moveToFirst()) {
                Artist.createFromCursor(it).apply { albumId = getAlbumId(this.id) }
            } else {
                Artist()
            }
        }
    }

    override fun getAllArtist(): List<Artist> {
        val artistList =
            makeArtistCursor(null, null).toList(true) {
                Artist.createFromCursor(this)
            }
        SortModes.sortArtistList(artistList, settingsUtility.artistSortOrder)
        return artistList
    }


    fun search(paramString: String, limit: Int): List<Artist> {
        val results = makeArtistCursor("artist LIKE ?", arrayOf("$paramString%"))
            .toList(true) { Artist.createFromCursor(this) }
        if (results.size < limit) {
            val moreArtists = makeArtistCursor("artist LIKE ?", arrayOf("%_$paramString%"))
                .toList(true) { Artist.createFromCursor(this) }
            results += moreArtists
        }
        return if (results.size < limit) {
            results
        } else {
            results.subList(0, limit)
        }
    }

    override fun getSongsForArtist(artistId: Long): List<Song> {
        return makeArtistSongCursor(artistId)
            .toList(true) { Song.createFromCursor(this) }
    }

    override fun getAlbumsForArtist(artistId: Long): List<Album> {
        return makeAlbumForArtistCursor(artistId)
            .toList(true) { Album.createFromCursor(this, artistId) }
    }

    private fun getAlbumId(id: Long): Long {
        makeAlbumCursor("artist_id=?", arrayOf("$id")).use {
            return if (it.moveToFirst()) {
                it.getLong(0)
            } else {
                -1L
            }
        }
    }


    private fun makeAlbumForArtistCursor(artistID: Long): Cursor? {
        if (artistID == -1L) {
            return null
        }
        return contentResolver.query(
            getContentUri("external", artistID),
            arrayOf(ALBUM_ID, ALBUM, ARTIST, NUMBER_OF_SONGS, FIRST_YEAR),
            null,
            null,
            SortModes.AlbumModes.ALBUM_A_Z
        )
    }

    private fun makeArtistCursor(
        selection: String?,
        paramArrayOfString: Array<String>?
    ): Cursor {
        return contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            arrayOf("_id", "artist", "number_of_albums"),
            selection,
            paramArrayOfString,
            null
        )!!
    }

    private fun makeAlbumCursor(
        selection: String?,
        paramArrayOfString: Array<String>?
    ): Cursor {
        return contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf("_id"),
            selection,
            paramArrayOfString,
            null
        )!!
    }

    private fun makeArtistSongCursor(artistId: Long): Cursor? {
        val artistSongSortOrder = SortModes.SongModes.SONG_A_Z
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "is_music=1 AND title != '' AND artist_id=$artistId"
        return contentResolver.query(
            uri,
            arrayOf(
                "_id",
                "title",
                "artist",
                "album",
                "duration",
                "track",
                "artist_id",
                "album_id",
                "_data"
            ),
            selection,
            null,
            artistSongSortOrder
        )
    }
}