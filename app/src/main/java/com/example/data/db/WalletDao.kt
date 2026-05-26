package com.example.data.db

import androidx.room.*
import com.example.data.model.Transaction
import com.example.data.model.WalletSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    // Transaction queries
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Wallet settings queries
    @Query("SELECT * FROM wallet_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<WalletSettings?>

    @Query("SELECT * FROM wallet_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): WalletSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: WalletSettings)
}
