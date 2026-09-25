package com.cardscanner.app

import android.app.Application
import com.cardscanner.app.data.AppDatabase
import com.cardscanner.app.data.CardRepository

class CardScannerApp : Application() {
    val repository: CardRepository by lazy {
        CardRepository(AppDatabase.create(this).cardDao())
    }
}
