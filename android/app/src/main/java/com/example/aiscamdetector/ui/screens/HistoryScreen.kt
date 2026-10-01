package com.example.aiscamdetector.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.aiscamdetector.data.model.HistoryEntry
import com.example.aiscamdetector.data.model.ScanType
import com.example.aiscamdetector.data.model.Verdict
import com.example.aiscamdetector.ui.theme.NavyPrimary
import com.example.aiscamdetector.ui.theme.SafeGreen
import com.example.aiscamdetector.ui.theme.ScamRed
import com.example.aiscamdetector.ui.theme.UnknownAmber
import com.example.aiscamdetector.ui.viewmodel.HistoryUiState
import com.example.aiscamdetector.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }

    val entry = selectedEntry
    if (entry != null) {
        ScanDetailsScreen(entry = entry, onClose = { selectedEntry = null })
        return
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is HistoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HistoryUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.message, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load() }) { Text("Retry") }
                    }
                }
                is HistoryUiState.Success -> {
                    if (s.entries.isEmpty()) {
                        Text(
                            "No scans yet.",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.entries) { entry ->
                                HistoryRow(entry, onClick = { selectedEntry = entry })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val color = when (entry.verdict) {
        Verdict.SAFE -> SafeGreen
        Verdict.SCAM -> ScamRed
        Verdict.UNKNOWN -> UnknownAmber
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (entry.type == ScanType.URL) "URL" else "Message",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(text = entry.timestamp, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = entry.input, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            val verdictLabel = entry.verdict.name.lowercase().replaceFirstChar { it.uppercase() }
            val scoreLabel = entry.riskScorePercent?.let { "${it}%" } ?: "not yet scored"
            Text(
                text = "$verdictLabel - $scoreLabel",
                color = color,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Full-screen scan detail view, shown when a HistoryRow is tapped.
 * Mirrors ResultCard's layout/colors so a fresh scan and a past scan
 * look consistent, including the suspicious-phrases list when present.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanDetailsScreen(entry: HistoryEntry, onClose: () -> Unit) {
    val color = when (entry.verdict) {
        Verdict.SAFE -> SafeGreen
        Verdict.SCAM -> ScamRed
        Verdict.UNKNOWN -> UnknownAmber
    }
    val verdictLabel = entry.verdict.name.lowercase().replaceFirstChar { it.uppercase() }
    val scoreText = entry.riskScorePercent?.let { "Risk score: ${it}%" } ?: "Risk score: not yet available"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Details") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (entry.type == ScanType.URL) "URL Scan" else "Message Scan",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = verdictLabel, style = MaterialTheme.typography.titleLarge, color = color)
                    Spacer(Modifier.height(8.dp))
                    Text(text = scoreText, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = entry.input,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = entry.summary, style = MaterialTheme.typography.bodyLarge)

                    if (entry.suspiciousPhrases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Suspicious phrases found:",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        entry.suspiciousPhrases.forEach { phrase ->
                            Text(
                                text = "• $phrase",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ScamRed,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
