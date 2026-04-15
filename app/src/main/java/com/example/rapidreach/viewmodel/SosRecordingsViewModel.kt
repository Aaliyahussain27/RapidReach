package com.example.rapidreach.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.data.local.entity.SosLogEntity
import com.example.rapidreach.data.repository.SosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File

class SosRecordingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SosRepository(application)
    private var mediaPlayer: MediaPlayer? = null

    val allLogs: StateFlow<List<SosLogEntity>> = repository.getAllLogsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun playRecording(audioPath: String) {
        try {
            stopPlayback()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
