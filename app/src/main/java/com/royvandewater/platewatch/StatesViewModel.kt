package com.royvandewater.platewatch

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val allStates: List<String> = arrayListOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")

class StatesViewModel(private val dataStore: DataStore<Preferences>): ViewModel(){
    private val statesFlow: Flow<List<String>> = getNonViewedStates()
    val states = statesFlow.asLiveData()

    fun setStateViewed(state: String) {
        viewModelScope.launch {
            Log.i(TAG, "setStateViewed($state)")
            val key = booleanPreferencesKey(state)
            dataStore.edit { settings ->
                settings[key] = true
            }
        }
    }

    fun resetStates() {
        viewModelScope.launch {
            dataStore.edit { settings ->
                allStates.forEach { state ->
                    val key = booleanPreferencesKey(state)
                    settings[key] = false
                }
            }
        }
    }

    private fun getNonViewedStates(): Flow<List<String>> {
        Log.i(TAG, "getNonViewedState")
        val settings: Flow<ArrayList<String>> = dataStore.data.map { settings ->
            val nonViewedStates = ArrayList<String>()
            Log.i(TAG, "nonViewedStates:init")

            allStates.forEach { state ->
                val key = booleanPreferencesKey(state)
                val viewed = settings[key] ?: false
                if (!viewed) {
                    Log.i(TAG, "nonViewedStates:add($state)")
                    nonViewedStates.add(state)
                }
            }

            nonViewedStates
        }

        return settings
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
