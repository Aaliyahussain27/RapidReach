package com.example.rapidreach.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.data.local.RapidReachDatabase
import com.example.rapidreach.data.local.entity.CustomHelplineEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HelplineViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RapidReachDatabase.getInstance(application)
    private val customHelplineDao = database.customHelplineDao()

    val customHelplines: StateFlow<List<CustomHelplineEntity>> = customHelplineDao.getAllCustomHelplines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCustomHelpline(name: String, number: String) {
        viewModelScope.launch {
            customHelplineDao.insert(CustomHelplineEntity(name = name, number = number))
        }
    }

    fun deleteCustomHelpline(helpline: CustomHelplineEntity) {
        viewModelScope.launch {
            customHelplineDao.delete(helpline)
        }
    }
}
