package com.apkpackager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apkpackager.data.auth.AuthRedirectBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRedirectBus: AuthRedirectBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dispatchOAuthRedirect(intent)
        setContent {
            APKPackagerTheme {
                AppNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchOAuthRedirect(intent)
    }

    private fun dispatchOAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "apkpackager" && data.host == "oauth") {
            authRedirectBus.publish(data)
        }
    }
}
