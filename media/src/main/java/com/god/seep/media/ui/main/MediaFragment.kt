package com.god.seep.media.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.god.seep.media.R

class MediaFragment : Fragment() {

    companion object {
        fun newInstance() = MediaFragment()
    }

    private lateinit var viewModel: MediaViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.media_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MediaViewModel::class.java)
        // TODO: Use the ViewModel
    }

}