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

            // Only message scans carry a real model score - a URL scan is a
            // Safe Browsing threat-database lookup (Safe/Malicious/
            // Unavailable per Use-Case 3/4), not a scored prediction, so
            // riskScorePercent is null for every URL result and no "Risk
            // score" line is shown at all (never "not yet available", which
            // wrongly implied a score was coming later). "%.1f" rounds the
            // real score to one clean decimal place when it does exist.
            result.riskScorePercent?.let { score ->
                Text(
                    text = "Risk score: ${"%.1f".format(score)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text(text = result.summary, style = MaterialTheme.typography.bodyLarge)

            if (result.suspiciousPhrases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Suspicious phrases found:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                result.suspiciousPhrases.forEach { phrase ->
                    Text(
                        text = "• $phrase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScamRed,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
