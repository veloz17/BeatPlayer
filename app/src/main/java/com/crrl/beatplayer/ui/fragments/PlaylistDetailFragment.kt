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
import androidx.recyclerview.widget.SimpleItemAnimator
import com.crrl.beatplayer.R
import com.crrl.beatplayer.databinding.FragmentPlaylistDetailBinding
import com.crrl.beatplayer.extensions.inflateWithBinding
import com.crrl.beatplayer.extensions.observe
import com.crrl.beatplayer.extensions.toIDList
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.repository.PlaylistRepository
import com.crrl.beatplayer.ui.activities.MainActivity
import com.crrl.beatplayer.ui.adapters.SongAdapter
import com.crrl.beatplayer.ui.fragments.base.BaseFragment
import com.crrl.beatplayer.utils.PlayerConstants.PLAY_LIST_DETAIL


class PlaylistDetailFragment : BaseFragment<Song>() {

    lateinit var binding: FragmentPlaylistDetailBinding
    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflater.inflateWithBinding(R.layout.fragment_playlist_detail, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        val id = arguments!!.getLong(PLAY_LIST_DETAIL)

        binding.playlist = PlaylistRepository(context).getPlaylist(id)

        // Set up adapter
        songAdapter = SongAdapter(activity, (activity as MainActivity).viewModel).apply {
            showHeader = true
            isPlaylist = true
            itemClickListener = this@PlaylistDetailFragment
        }

        // Set up RecyclerView
        binding.songList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        mainViewModel.songsByPlayList(binding.playlist!!.id).observe(this) {
            songAdapter.updateDataSet(it)
        }

        binding.let {
            it.viewModel = mainViewModel
            it.lifecycleOwner = this
            it.executePendingBindings()
        }
    }

    override fun removeFromList(playListId: Long, item: Song?) {
        mainViewModel.playlistRepository.removeFromPlaylist(playListId, item!!.id)
    }

    override fun onItemClick(view: View, position: Int, item: Song) {
        mainViewModel.update(item)
        mainViewModel.update(songAdapter.songList.toIDList())
    }

    override fun onShuffleClick(view: View) {
        mainViewModel.update(songAdapter.songList.toIDList())
        mainViewModel.update(mainViewModel.random(-1))
    }

    override fun onPlayAllClick(view: View) {
        mainViewModel.update(songAdapter.songList.first())
        mainViewModel.update(songAdapter.songList.toIDList())
    }

    override fun onPopupMenuClick(view: View, position: Int, item: Song, itemList: List<Song>) {
        super.onPopupMenuClick(view, position, item, itemList)
        powerMenu!!.showAsAnchorRightTop(view)
        mainViewModel.playLists().observe(this) {
            buildPlaylistMenu(it, item)
        }
    }
}
