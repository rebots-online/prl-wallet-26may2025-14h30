package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_settings")
data class WalletSettings(
    @PrimaryKey val id: Int = 1,
    val biometricEnabled: Boolean = true,
    val developerModeEnabled: Boolean = false,
    val activeNetwork: String = "wss://mainnet.pearl.network",
    val localCurrency: String = "USD",
    val language: String = "English (US)",
    val seedPhraseBackedUp: Boolean = false
)
