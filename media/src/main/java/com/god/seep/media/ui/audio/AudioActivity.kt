package com.god.seep.media.ui.audio

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.god.seep.base.adapter.BaseRecyclerViewAdapter
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.base.arch.viewmodel.BaseViewModel
import com.god.seep.media.R
import com.god.seep.media.audio.*
import com.god.seep.media.databinding.ActivityAudioBinding
import com.god.seep.media.databinding.ItemAudioBinding
import java.io.File
import java.util.*

class AudioActivity : BaseActivity<ActivityAudioBinding, BaseViewModel>() {
    private lateinit var fileAdapter: FileAdapter
    private lateinit var audioRecord: AudioRecord

    override fun getLayoutId(): Int {
        return R.layout.activity_audio
    }

    override fun createViewModel(): BaseViewModel {
        return getViewModel(BaseViewModel::class.java)
    }

    override fun initData() {
        mBinding.view = this
        audioRecord = AudioRecord(this)
        fileAdapter = FileAdapter()
        mBinding.list.adapter = fileAdapter
        fileAdapter.setOnItemClickListener { adapter, view, position ->
            //play
            val item = fileAdapter.getItem(position)
            if (item.name.endsWith("pcm")) {
                playPcmStream(item)
//                playPcmStatic(item)
            }
        }
        refreshFile()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission_group.STORAGE) != PackageManager.PERMISSION_GRANTED)
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission_group.STORAGE), 100)
    }

    override fun registerEvent() {
        mBinding.ivSpeaker.setOnClickListener {
            mBinding.recording = true
            audioRecord.startRecord()
        }
        mBinding.ivSpeakerStop.setOnClickListener {
            mBinding.recording = false
            audioRecord.stopRecord()
            refreshFile()
        }
    }

    private fun refreshFile() {
        fileAdapter.setNewInstance(audioRecord.fetchFiles().toMutableList())
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord.release()
    }
}

class FileAdapter : BaseRecyclerViewAdapter<ItemAudioBinding, File>(R.layout.item_audio) {
    override fun bindItem(binding: ItemAudioBinding, item: File) {
        with(binding) {
            tvName.text = item.name
            tvTime.text = Date(item.lastModified()).format("yyyy-MM-dd'T'HH:mm")
            tvPath.text = item.absolutePath
            tvSize.text = item.length().formatFileSize()
        }
    }
}