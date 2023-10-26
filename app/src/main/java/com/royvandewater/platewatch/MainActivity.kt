package com.royvandewater.platewatch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.royvandewater.platewatch.ui.theme.PlateWatchTheme
import kotlinx.coroutines.runBlocking


//const val TAG: String = "MainActivity"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "states")

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StatesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            StatesViewModelFactory(dataStore)
        )[StatesViewModel::class.java]

        setContent {
            PlateWatchTheme { App(viewModel) }
        }
    }
}

@Composable
fun App(
    viewModel: StatesViewModel,
) {
    val hasUndo = viewModel.hasUndoHistoryFlow.collectAsStateWithLifecycle()
    val nonViewedStates = viewModel.nonViewedStatesFlow.collectAsStateWithLifecycle()
    val viewedStates = viewModel.viewedStatesFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            StatesTopBar(
                hasUndo = hasUndo.value,
                onUndo  = { viewModel.undo() },
                onReset = { viewModel.resetStates() },
            )
        }
    ) { innerPadding ->
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            StatesList(
                innerPadding = innerPadding,
                nonViewedStates = nonViewedStates.value,
                viewedStates = viewedStates.value,
                onStateViewed = { state -> viewModel.setStateViewed(state) },
                onStateUnViewed = { state -> viewModel.setStateUnViewed(state) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatesTopBar(hasUndo: Boolean, onUndo: () -> Unit, onReset: () -> Unit ) {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text("Plate Watch") },
        actions = {
            IconButton( onClick = { onUndo() }, enabled = hasUndo) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_undo_24),
                    contentDescription = "Undo"
                )
            }
            IconButton(onClick = { onReset() }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_sweep_24),
                    contentDescription = "Clear Viewed"
                )
            }
        }
    )
}

@Composable
fun StatesList(
    innerPadding: PaddingValues,
    nonViewedStates: List<String>,
    viewedStates: List<String>,
    onStateViewed: (state: String) -> Unit,
    onStateUnViewed: (state: String) -> Unit
) {
    LazyColumn(Modifier.padding(innerPadding)) {
        items(items = nonViewedStates) { state ->
            StateRow(state = state, onClick = {
                runBlocking {
                    onStateViewed(state)
                }
            })
        }

        item {
            Separator()
        }

        items(items = viewedStates) { state ->
            ViewedStateRow(state = state, onClick = {
                runBlocking {
                    onStateUnViewed(state)
                }
            })
        }
    }
}

@Composable
fun Separator() {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(0.dp, 16.dp, 0.dp, 0.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer)) {
        Column {
            Text(text = "Checked", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f), modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun StateRow(state: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .clickable(true, "Select", null, onClick = onClick)
        .fillMaxWidth()) {
        Column {
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = state, fontSize =24.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun ViewedStateRow(state: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .clickable(true, "Select", null, onClick = onClick)
        .fillMaxWidth()) {
        Column {
            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f))
            Text(text = state, fontSize =24.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f), modifier = Modifier.padding(8.dp))
        }
    }
}