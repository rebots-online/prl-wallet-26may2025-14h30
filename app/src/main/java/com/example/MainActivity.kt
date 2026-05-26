package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PearlWalletApp
import com.example.ui.theme.PearlTheme
import com.example.ui.viewmodel.WalletViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PearlTheme {
        val viewModel = viewModel<WalletViewModel>()
        PearlWalletApp(viewModel = viewModel)
      }
    }
  }
}
