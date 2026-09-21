package com.example.aiscamdetector.data.repository

import com.example.aiscamdetector.data.model.ScanResult

interface ScanRepository {
    suspend fun scanUrl(url: String): Result<ScanResult>
    suspend fun scanMessage(message: String): Result<ScanResult>
}
