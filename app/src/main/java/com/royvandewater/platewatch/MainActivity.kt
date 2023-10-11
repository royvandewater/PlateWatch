package com.royvandewater.platewatch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.royvandewater.platewatch.ui.theme.PlateWatchTheme
import kotlinx.coroutines.runBlocking

const val TAG: String = "MainActivity"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "states")

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StatesViewModel
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            StatesViewModelFactory(dataStore)
        )[StatesViewModel::class.java]

        val statesList: MutableState<List<String>> = mutableStateOf(listOf())

        viewModel.states.observe(this) {states ->
            statesList.value = states
        }

        setContent {
            PlateWatchTheme {
                Scaffold(
                    topBar = { TopAppBar(
                        colors = topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = { Text("Plate Watch") },
                        actions = {
                            IconButton(onClick = { viewModel.resetStates() }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Clear Viewed"
                                )
                            }
                        }
                    ) }
                ) {innerPadding ->
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StatesList(innerPadding, statesList, onSelectState = {state ->
                            viewModel.setStateViewed(state)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun StatesList(innerPadding: PaddingValues, statesList: MutableState<List<String>>, onSelectState: (state: String) -> Unit) {
    val states by rememberSaveable { statesList }

    LazyColumn(Modifier.padding(innerPadding)) {
        items(items = states) { state ->
            StateRow(state = state, onClick = {
                runBlocking {
                    onSelectState(state)
                }
            })
        }
    }
}

@Composable
fun StateRow(state: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .clickable(true, "Select", null, onClick = onClick)
        .fillMaxWidth()) {
        Column {
            Divider(color = Color.Black)
            Text(text = state, fontSize =24.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlateWatchTheme {
        Greeting("Android")
    }
}