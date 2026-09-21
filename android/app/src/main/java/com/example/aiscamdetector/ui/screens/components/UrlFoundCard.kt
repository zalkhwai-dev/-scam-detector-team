package com.example.aiscamdetector.ui.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aiscamdetector.ui.theme.WarningBackground
import com.example.aiscamdetector.ui.theme.WarningText


@Composable
fun UrlFoundCard(
    url: String,
    onScanThisLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "This message contains a link: $url",
                color = WarningText,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onScanThisLink) {
                Text("Scan This Link")
            }
        }
    }
}
