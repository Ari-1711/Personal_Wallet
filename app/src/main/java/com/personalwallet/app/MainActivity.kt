package com.personalwallet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.personalwallet.app.ui.navigation.MainNavGraph
import com.personalwallet.app.ui.theme.PersonalWalletTheme
import dagger.hilt.android.AndroidEntryPoint

import com.personalwallet.app.ui.navigation.MainNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalWalletTheme {
                MainNavGraph()
            }
        }
    }
}
