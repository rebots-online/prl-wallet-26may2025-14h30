package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // "RECEIVED", "SENT", "STAKED", "SWAPPED"
    val assetSymbol: String,   // "PRL", "BTC", "USDC"
    val assetName: String,     // "Pearl", "Bitcoin", "USD Coin"
    val address: String,       // Hex or description (e.g., "From: 0x8a...4f2b", "To: 0x2b...1c4d")
    val amount: Double,
    val counterAmount: Double = 0.0,
    val counterSymbol: String = "",
    val timestamp: Long,
    val status: String,        // "SUCCESS", "PENDING", "REVERTED"
    val fee: Double = 0.0,
    val validator: String = "" // e.g. "Validator #42"
)
