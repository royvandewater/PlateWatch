package com.royvandewater.platewatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// const val TAG: String = "StatesViewModel"

val allStates: List<String> = arrayListOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")

data class StatesUiModel(
    val nonViewed: List<String>,
    val viewed: List<String>,
    val hasUndoHistory: Boolean,
)

class StatesViewModel(private val dataStore: DataStore<Preferences>): ViewModel(){
    private val undoHistory: ArrayList<Preferences> = ArrayList()

    private val nonViewedStatesFlow: Flow<List<String>> = getNonViewedStatesFlow()
    private val viewedStatesFlow: Flow<List<String>> = getViewedStatesFlow()
    private val hasUndoHistoryFlow: Flow<Boolean> = getUndoHistoryFlow()


    private val statesUiModelFlow = combine(nonViewedStatesFlow, viewedStatesFlow, hasUndoHistoryFlow) { nonViewedStates, viewedStates, hasUndoHistory ->
       return@combine StatesUiModel(nonViewedStates, viewedStates, hasUndoHistory)
    }
    val statesUiModel = statesUiModelFlow.asLiveData()

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
                undoHistory.removeLast()
            }
        }
    }

    private suspend fun addStateToUndoHistory() {
        val settings = dataStore.data.first()
        undoHistory.add(settings)
    }

    private fun getNonViewedStatesFlow(): Flow<List<String>> {
        val settings: Flow<ArrayList<String>> = dataStore.data.map { settings ->
            val nonViewedStates = ArrayList<String>()

            allStates.forEach { state ->
                val key = booleanPreferencesKey(state)
                val viewed = settings[key] ?: false
                if (!viewed) {
                    nonViewedStates.add(state)
                }
            }

            nonViewedStates
        }

        return settings
    }

    private fun getViewedStatesFlow(): Flow<List<String>> {
        return dataStore.data.map { settings ->
            val viewedStates = ArrayList<String>()

            allStates.forEach { state ->
                val key = booleanPreferencesKey(state)
                val viewed = settings[key] ?: false
                if (viewed) {
                    viewedStates.add(state)
                }
            }

            viewedStates
        }
    }

    private fun getUndoHistoryFlow(): Flow<Boolean> {
        return flow<Boolean> {
            while(true) {
                emit(undoHistory.isNotEmpty())
                delay(10)
            }
        }
    }
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
