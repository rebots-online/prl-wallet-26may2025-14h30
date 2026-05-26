package com.example.data.repository

import com.example.data.db.WalletDao
import com.example.data.model.Transaction
import com.example.data.model.WalletSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WalletRepository(private val walletDao: WalletDao) {

    val allTransactions: Flow<List<Transaction>> = walletDao.getAllTransactionsFlow()
    val walletSettings: Flow<WalletSettings?> = walletDao.getSettingsFlow()

    suspend fun insertTransaction(transaction: Transaction) {
        walletDao.insertTransaction(transaction)
    }

    suspend fun saveSettings(settings: WalletSettings) {
        walletDao.saveSettings(settings)
    }

    suspend fun clearTransactions() {
        walletDao.clearTransactions()
    }

    suspend fun prepopulateDatabaseIfEmpty() {
        // Prepare some realistic seed transactions matching the screenshot designs and specs
        val currentSettings = walletDao.getSettingsDirect()
        if (currentSettings == null) {
            walletDao.saveSettings(WalletSettings())
        }

        val existingTx = allTransactions.first()
        if (existingTx.isEmpty()) {
            val now = System.currentTimeMillis()
            val hourMs = 3600_000L
            val dayMs = 24 * hourMs

            val seeds = listOf(
                Transaction(
                    type = "RECEIVED",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "From: 0x7a...9f2b",
                    amount = 1250.00,
                    timestamp = now - 45 * 60 * 1000, // 45 mins ago
                    status = "SUCCESS"
                ),
                Transaction(
                    type = "SENT",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "To: 0x2b...1c4d",
                    amount = 45.50,
                    timestamp = now - 5 * hourMs, // 5 hours ago
                    status = "SUCCESS"
                ),
                Transaction(
                    type = "STAKED",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "Validator #42",
                    amount = 5000.00,
                    timestamp = now - 6 * hourMs, // 6 hours ago
                    status = "PENDING",
                    validator = "Validator #42"
                ),
                Transaction(
                    type = "SWAPPED",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "DEX Router",
                    amount = 342.15,
                    counterAmount = 500.00,
                    counterSymbol = "USDC",
                    timestamp = now - dayMs - 2 * hourMs, // Yesterday
                    status = "SUCCESS"
                ),
                Transaction(
                    type = "SENT", // Failed
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "To: 0x9a...3f1e",
                    amount = 100.00,
                    timestamp = now - dayMs - 11 * hourMs, // Yesterday
                    status = "REVERTED"
                ),
                Transaction(
                    type = "RECEIVED",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "From: 0x8a...4f2b",
                    amount = 450.00,
                    timestamp = now - 2 * dayMs, // 2 days ago
                    status = "SUCCESS"
                ),
                Transaction(
                    type = "SENT",
                    assetSymbol = "PRL",
                    assetName = "Pearl",
                    address = "To: 0x2c...9d1e",
                    amount = 120.50,
                    timestamp = now - 5 * dayMs, // 5 days ago
                    status = "SUCCESS"
                )
            )
            walletDao.insertTransactions(seeds)
        }
    }
}
