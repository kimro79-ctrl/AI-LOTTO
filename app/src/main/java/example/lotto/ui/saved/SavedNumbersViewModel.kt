// File Path: app/src/main/java/com/example/lotto/ui/saved/SavedNumbersViewModel.kt
package com.example.lotto.ui.saved

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SavedNumbersViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("lotto_saved_prefs", Context.MODE_PRIVATE)

    private val _savedSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val savedSets: StateFlow<List<List<Int>>> = _savedSets.asStateFlow()

    init {
        loadSavedNumbers()
    }

    fun loadSavedNumbers() {
        val storedSet = sharedPreferences.getStringSet("saved_number_sets", emptySet()) ?: emptySet()
        val parsedList = storedSet.map { str ->
            str.split(",").mapNotNull { it.trim().toIntOrNull() }
        }.filter { it.size == 6 }

        _savedSets.value = parsedList
    }

    fun clearAllSavedNumbers() {
        sharedPreferences.edit().remove("saved_number_sets").apply()
        _savedSets.value = emptyList()
    }
}

