package com.example.aiscamdetector.data

import android.content.Context
import com.example.aiscamdetector.data.repository.HistoryRepository
import com.example.aiscamdetector.data.repository.RealHistoryRepository
import com.example.aiscamdetector.data.repository.RealScanRepository
import com.example.aiscamdetector.data.repository.ScanRepository

/**
 * Swapped from FixtureScanRepository/FixtureHistoryRepository to the real
 * implementations, which call Zainab's NetworkClient and ScanHistoryDbHelper.
 *
 * Unlike the fixture version, these repositories need a Context (for
 * SQLite), so ServiceLocator now needs init(context) called once before
 * anything reads scanRepository/historyRepository - simplest place is the
 * very top of MainActivity.onCreate(), before setContent { }:
 *
 *     ServiceLocator.init(applicationContext)
 */
object ServiceLocator {

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val scanRepository: ScanRepository by lazy {
        RealScanRepository(requireContext())
    }

    val historyRepository: HistoryRepository by lazy {
        RealHistoryRepository(requireContext())
    }

    private fun requireContext(): Context =
        appContext ?: error("ServiceLocator.init(context) must be called before use - call it in MainActivity.onCreate() before setContent { }")
}
