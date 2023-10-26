package com.royvandewater.platewatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// const val TAG: String = "StatesViewModel"

val allStates: List<String> = arrayListOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")

class StatesViewModel(private val dataStore: DataStore<Preferences>): ViewModel() {
    private val undoHistory: ArrayList<Preferences> = ArrayList()
    private var updateHasUndoHistory: UndoCallback? = null

    val nonViewedStatesFlow: StateFlow<List<String>> = getNonViewedStates()
    val viewedStatesFlow: StateFlow<List<String>> = getViewedStates()
    val hasUndoHistoryFlow: StateFlow<Boolean> = getUndoHistory()

    fun setStateViewed(state: String) {
        viewModelScope.launch {
            addStateToUndoHistory()

            val key = booleanPreferencesKey(state)
            dataStore.edit { settings ->
                settings[key] = true
            }
        }
    }

    fun setStateUnViewed(state: String) {
        viewModelScope.launch {
            addStateToUndoHistory()

            val key = booleanPreferencesKey(state)
            dataStore.edit { settings ->
                settings[key] = false
            }
        }
    }

    fun resetStates() {
        viewModelScope.launch {
            addStateToUndoHistory()

            dataStore.edit { settings ->
                allStates.forEach { state ->
                    val key = booleanPreferencesKey(state)
                    settings[key] = false
                }
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            dataStore.updateData {
                val preferences = undoHistory.removeLast()
                updateHasUndoHistory?.handleMessage(undoHistory.isNotEmpty())
                return@updateData preferences
            }
        }
    }

    private suspend fun addStateToUndoHistory() {
        val settings = dataStore.data.first()
        undoHistory.add(settings)
        updateHasUndoHistory?.handleMessage(true)
    }

    private fun getNonViewedStates(): StateFlow<List<String>> {
        return dataStore.data.map { settings ->
            allStates.filter { state ->
                val key = booleanPreferencesKey(state)
                val viewed = settings[key] ?: false
                return@filter !viewed
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(),
        )
    }

    private fun getViewedStates(): StateFlow<List<String>> {
        return dataStore.data.map { settings ->
            allStates.filter { state ->
                val key = booleanPreferencesKey(state)
                return@filter settings[key] ?: false
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(),
        )
    }

    private fun getUndoHistory(): StateFlow<Boolean> {
        return callbackFlow {
            val callback = object : UndoCallback {
                override fun handleMessage(hasUndoHistory: Boolean) {
                    trySendBlocking(hasUndoHistory)
                }
            }

            updateHasUndoHistory = callback
            awaitClose { updateHasUndoHistory = null }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )
    }
}

interface UndoCallback {
    fun handleMessage(hasUndoHistory: Boolean)
}

class StatesViewModelFactory(private val dataStore: DataStore<Preferences>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatesViewModel(dataStore) as T
        }
        return super.create(modelClass)
    }
}
