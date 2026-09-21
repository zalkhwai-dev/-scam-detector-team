package com.example.aiscamdetector.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aiscamdetector.data.model.ScanResult
import com.example.aiscamdetector.data.model.Verdict
import com.example.aiscamdetector.ui.theme.SafeGreen
import com.example.aiscamdetector.ui.theme.ScamRed
import com.example.aiscamdetector.ui.theme.UnknownAmber

@Composable
fun ResultCard(result: ScanResult, modifier: Modifier = Modifier) {
    val (label, color) = when (result.verdict) {
        Verdict.SAFE -> "Safe" to SafeGreen
        Verdict.SCAM -> "Scam" to ScamRed
        Verdict.UNKNOWN -> "Unknown" to UnknownAmber
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.titleLarge, color = color)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val scoreText = result.riskScorePercent?.let { "Risk score: ${it}%" } ?: "Risk score: not yet available"

            Text(
                text = scoreText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text(text = result.summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
