package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Transaction
import com.example.data.model.WalletSettings
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PearlScreen {
    WALLET,
    HISTORY,
    TRANSACT,
    SETTINGS
}

enum class TransactTab {
    SEND,
    RECEIVE
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository
    
    // UI Screen navigation
    private val _currentScreen = MutableStateFlow(PearlScreen.WALLET)
    val currentScreen: StateFlow<PearlScreen> = _currentScreen.asStateFlow()

    private val _currentTransactTab = MutableStateFlow(TransactTab.SEND)
    val currentTransactTab: StateFlow<TransactTab> = _currentTransactTab.asStateFlow()

    // Database state flows
    val transactions: StateFlow<List<Transaction>>
    val settings: StateFlow<WalletSettings>

    // Real-time market state flows
    private val _prlPrice = MutableStateFlow(1.24)
    val prlPrice: StateFlow<Double> = _prlPrice.asStateFlow()

    private val _btcPrice = MutableStateFlow(64230.00)
    val btcPrice: StateFlow<Double> = _btcPrice.asStateFlow()

    private val _pingMs = MutableStateFlow(24)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    // Indicators for live flickering (tick animation)
    private val _prlTickColor = MutableStateFlow(0) // 0 for neutral, 1 for green, -1 for red
    val prlTickColor: StateFlow<Int> = _prlTickColor.asStateFlow()

    private val _btcTickColor = MutableStateFlow(0)
    val btcTickColor: StateFlow<Int> = _btcTickColor.asStateFlow()

    // Send Form Input state
    var sendAsset = MutableStateFlow("PRL")
    var sendAmount = MutableStateFlow("")
    var sendAddress = MutableStateFlow("")
    
    // Transaction History Filter State
    private val _historyFilter = MutableStateFlow("ALL") // "ALL", "SENT", "RECEIVED", "STAKED"
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Biometric Modal Simulation State
    private val _isBiometricPromptVisible = MutableStateFlow(false)
    val isBiometricPromptVisible: StateFlow<Boolean> = _isBiometricPromptVisible.asStateFlow()

    private val _biometricStatusMessage = MutableStateFlow("Place your finger on the sensor or face the camera")
    val biometricStatusMessage: StateFlow<String> = _biometricStatusMessage.asStateFlow()

    private val _biometricApproved = MutableStateFlow(false)
    val biometricApproved: StateFlow<Boolean> = _biometricApproved.asStateFlow()

    private var wsJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WalletRepository(database.walletDao())

        // Safe setup flows
        transactions = repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        settings = repository.walletSettings
            .map { it ?: WalletSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = WalletSettings()
            )

        // Prepopulate db
        viewModelScope.launch {
            repository.prepopulateDatabaseIfEmpty()
        }

        // Engage mock high-speed WebSockets price feeds
        startLiveWebSocketSimulation()
    }

    fun setScreen(screen: PearlScreen) {
        _currentScreen.value = screen
    }

    fun setTransactTab(tab: TransactTab) {
        _currentTransactTab.value = tab
    }
    
    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Settings actions mutators
    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(biometricEnabled = enabled))
        }
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            val endpoint = if (enabled) "wss://testnet.pearl.network" else "wss://mainnet.pearl.network"
            repository.saveSettings(settings.value.copy(
                developerModeEnabled = enabled,
                activeNetwork = endpoint
            ))
        }
    }

    fun setLocalCurrency(currency: String) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(localCurrency = currency))
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(language = lang))
        }
    }

    fun markSeedBackedUp() {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(seedPhraseBackedUp = true))
        }
    }

    // Biometric prompt action mockup
    fun showBiometricConfirmation(onSuccess: () -> Unit) {
        if (!settings.value.biometricEnabled) {
            // Bypass straight to submit if biometric toggle is off
            onSuccess()
            return
        }
        _isBiometricPromptVisible.value = true
        _biometricApproved.value = false
        _biometricStatusMessage.value = "Initiating Pearl Secure Sign..."
        
        viewModelScope.launch {
            delay(800)
            _biometricStatusMessage.value = "Scanning credential..."
            delay(1200)
            _biometricStatusMessage.value = "Authenticating signature..."
            delay(600)
            _biometricApproved.value = true
            _biometricStatusMessage.value = "Access Granted"
            delay(1000)
            _isBiometricPromptVisible.value = false
            onSuccess()
        }
    }

    fun dismissBiometricPrompt() {
        _isBiometricPromptVisible.value = false
    }

    // Transaction Submission Pipeline
    fun processSendTx(address: String, amountDouble: Double, asset: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val recipientAddressFormatted = if (address.length > 10) {
                "${address.take(4)}...${address.takeLast(4)}"
            } else {
                address
            }
            val tx = Transaction(
                type = "SENT",
                assetSymbol = asset,
                assetName = if (asset == "PRL") "Pearl" else "Bitcoin",
                address = "To: $recipientAddressFormatted",
                amount = amountDouble,
                timestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            )
            repository.insertTransaction(tx)
            
            // Clean inputs
            sendAmount.value = ""
            sendAddress.value = ""
            
            onComplete()
        }
    }

    fun processSwapTx(fromAsset: String, toAsset: String, fromAmount: Double, toAmount: Double) {
        viewModelScope.launch {
            val tx = Transaction(
                type = "SWAPPED",
                assetSymbol = toAsset,
                assetName = if (toAsset == "PRL") "Pearl" else "Bitcoin",
                address = "Via Uniswap V3",
                amount = toAmount,
                counterAmount = fromAmount,
                counterSymbol = fromAsset,
                timestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            )
            repository.insertTransaction(tx)
        }
    }

    fun performDisconnectWallet() {
        viewModelScope.launch {
            // Clear items, turn off settings
            repository.clearTransactions()
            repository.saveSettings(WalletSettings(
                biometricEnabled = true,
                developerModeEnabled = false,
                activeNetwork = "wss://mainnet.pearl.network",
                seedPhraseBackedUp = false
            ))
            // Re populate database to seed status
            repository.prepopulateDatabaseIfEmpty()
        }
    }

    private fun startLiveWebSocketSimulation() {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(2000, 4500))
                
                // Simulate some live network variations
                _pingMs.value = Random.nextInt(18, 32)

                val directionPrl = if (Random.nextBoolean()) 1 else -1
                val prlDiff = Random.nextDouble(0.001, 0.009) * directionPrl
                val nextPrl = (_prlPrice.value + prlDiff).coerceIn(1.15, 1.35)
                _prlPrice.value = Math.round(nextPrl * 1000.0) / 1000.0
                _prlTickColor.value = directionPrl
                
                val directionBtc = if (Random.nextBoolean()) 1 else -1
                val btcDiff = Random.nextDouble(15.0, 110.0) * directionBtc
                val nextBtc = (_btcPrice.value + btcDiff).coerceIn(61000.00, 68000.00)
                _btcPrice.value = Math.round(nextBtc * 100.0) / 100.0
                _btcTickColor.value = directionBtc

                // Rest tick animation indicators back to neutral
                delay(1200)
                _prlTickColor.value = 0
                _btcTickColor.value = 0
            }
        }
    }

    override fun onCleared() {
        wsJob?.cancel()
        super.onCleared()
    }
}
