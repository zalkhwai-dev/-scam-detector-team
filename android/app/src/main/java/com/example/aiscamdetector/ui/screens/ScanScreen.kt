package com.example.aiscamdetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aiscamdetector.data.model.ScanType
import com.example.aiscamdetector.ui.screens.components.ResultCard
import com.example.aiscamdetector.ui.screens.components.UrlFoundCard
import com.example.aiscamdetector.ui.state.ScanUiState
import com.example.aiscamdetector.ui.theme.GreenPrimary
import com.example.aiscamdetector.ui.theme.NavyPrimary
import com.example.aiscamdetector.ui.theme.WarningBackground
import com.example.aiscamdetector.ui.theme.WarningText
import com.example.aiscamdetector.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onOpenHistory: () -> Unit,
    onLogOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ScanType.MESSAGE) }
    val urlState by viewModel.urlState.collectAsState()
    val messageState by viewModel.messageState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scam Detector") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                    Text("History")
                }
                OutlinedButton(onClick = onLogOut, modifier = Modifier.weight(1f)) {
                    Text("Log Out")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            TabRow(
                selectedTabIndex = if (selectedTab == ScanType.URL) 0 else 1,
                contentColor = GreenPrimary
            ) {
                Tab(
                    selected = selectedTab == ScanType.URL,
                    onClick = { selectedTab = ScanType.URL },
                    text = { Text("SCAN URL") }
                )
                Tab(
                    selected = selectedTab == ScanType.MESSAGE,
                    onClick = { selectedTab = ScanType.MESSAGE },
                    text = { Text("SCAN MESSAGE") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(WarningBackground).padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningText)
                Spacer(Modifier.width(8.dp))
                Text(
                    "This tool is a helper, not a guarantee. Always use your own " +
                        "judgment, especially with sensitive information.",
                    color = WarningText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == ScanType.URL) {
                    UrlTabContent(viewModel, urlState)
                } else {
                    MessageTabContent(
                        viewModel = viewModel,
                        state = messageState,
                        onScanThisLink = { url ->
                            selectedTab = ScanType.URL
                            viewModel.scanDetectedUrl(url)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UrlTabContent(viewModel: ScanViewModel, state: ScanUiState) {
    OutlinedTextField(
        value = viewModel.urlInput,
        onValueChange = viewModel::onUrlInputChange,
        label = { Text("Paste a URL, e.g. https://example.com") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { viewModel.scanUrl() },
        enabled = state !is ScanUiState.Loading,
        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Scan URL")
    }
    Spacer(Modifier.height(24.dp))
    ScanResultArea(state = state, onScanThisLink = null)
}

@Composable
private fun MessageTabContent(
    viewModel: ScanViewModel,
    state: ScanUiState,
    onScanThisLink: (String) -> Unit
) {
    Text(
        "Message analysis currently supports English text only.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = viewModel.messageInput,
        onValueChange = viewModel::onMessageInputChange,
        label = { Text("Paste a suspicious message, email, or chat text") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { viewModel.scanMessage() },
        enabled = state !is ScanUiState.Loading,
        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Analyze Message")
    }
    Spacer(Modifier.height(24.dp))
    ScanResultArea(state = state, onScanThisLink = onScanThisLink)
}

/**
 * Renders exactly one of: nothing (idle), a spinner (loading), an error
 * card, or the result. Errors are shown as errors - they are never
 * swapped for a "Safe" result, and the verdict/risk numbers shown here
 * are only ever the ones the repository returned.
 */
@Composable
private fun ScanResultArea(
    state: ScanUiState,
    onScanThisLink: ((String) -> Unit)?
) {
    when (state) {
        is ScanUiState.Idle -> Unit
        is ScanUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ScanUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = state.message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        is ScanUiState.Success -> {
            Column {
                ResultCard(result = state.result)
                val detectedUrl = state.result.detectedUrl
                if (detectedUrl != null && onScanThisLink != null) {
                    Spacer(Modifier.height(16.dp))
                    UrlFoundCard(url = detectedUrl, onScanThisLink = { onScanThisLink(detectedUrl) })
                }
            }
        }
    }
}
