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

package com.crrl.beatplayer.ui.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.crrl.beatplayer.R
import com.crrl.beatplayer.databinding.FragmentAlbumDetailBinding
import com.crrl.beatplayer.extensions.inflateWithBinding
import com.crrl.beatplayer.extensions.observe
import com.crrl.beatplayer.extensions.toIDList
import com.crrl.beatplayer.models.Album
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.repository.AlbumsRepository
import com.crrl.beatplayer.repository.FavoritesRepository
import com.crrl.beatplayer.ui.adapters.AlbumDetailAdapter
import com.crrl.beatplayer.ui.fragments.base.BaseFragment
import com.crrl.beatplayer.utils.PlayerConstants

class AlbumDetailFragment : BaseFragment<Song>() {

    private lateinit var album: Album
    private lateinit var albumDetailAdapter: AlbumDetailAdapter
    private lateinit var binding: FragmentAlbumDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = inflater.inflateWithBinding(R.layout.fragment_album_detail, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        val id = arguments!!.getLong(PlayerConstants.ALBUM_KEY)
        album = AlbumsRepository.getInstance(context)?.getAlbum(id)!!

        albumDetailAdapter = AlbumDetailAdapter(context, mainViewModel).apply {
            showHeader = true
            itemClickListener = this@AlbumDetailFragment
            this.album = this@AlbumDetailFragment.album
        }

        binding.apply {
            albumSongList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = albumDetailAdapter
            }
            addFavorites.setOnClickListener { toggleAddFav() }
        }

        mainViewModel.getSongsByAlbum(album.id)!!.observe(this) {
            albumDetailAdapter.updateDataSet(it)
            if (it.isEmpty()) {
                mainViewModel.favoriteRepository.deleteFavorites(longArrayOf(id))
                activity?.onBackPressed()
            }
        }

        binding.let {
            it.viewModel = mainViewModel
            it.album = album
            it.lifecycleOwner = this
            it.executePendingBindings()
        }
    }

    override fun onItemClick(view: View, position: Int, item: Song) {
        mainViewModel.update(item)
        mainViewModel.update(albumDetailAdapter.songList.toIDList())
    }

    override fun onShuffleClick(view: View) {
        mainViewModel.update(albumDetailAdapter.songList.toIDList())
        mainViewModel.update(mainViewModel.random(-1))
    }

    override fun onPlayAllClick(view: View) {
        mainViewModel.update(albumDetailAdapter.songList.first())
        mainViewModel.update(albumDetailAdapter.songList.toIDList())
    }

    override fun onPopupMenuClick(view: View, position: Int, item: Song, itemList: List<Song>) {
        super.onPopupMenuClick(view, position, item, itemList)
        powerMenu!!.showAsAnchorRightTop(view)
        mainViewModel.playLists().observe(this) {
            buildPlaylistMenu(it, item)
        }
    }

    private fun toggleAddFav() {
        val favoritesRepository = FavoritesRepository(context)
        val libType = getString(R.string.album)
        if (favoritesRepository.favExist(album.id)) {
            val resp = favoritesRepository.deleteFavorites(longArrayOf(album.id))
            showSnackBar(view, resp, 0, libType)
        } else {
            val resp = favoritesRepository.createFavorite(album.toFavorite())
            showSnackBar(view, resp, 1, libType)
        }
    }
}
