package io.jhoyt.bubbletimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

import com.amplifyframework.ui.authenticator.ui.Authenticator
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.amplifyframework.core.Amplify

import io.jhoyt.bubbletimer.ui.theme.BubbleTimerTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BubbleTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                )

                {
                    Authenticator { state ->
                        Column {
                            Text(
                                text = "Hello ${state.user.username}!",
                            )
                            Button(onClick = {
                                Amplify.Auth.signOut {  }
                            }) {
                                Text(text = "Sign Out")
                            }
                        }
                    }
                }

            }
        }
    }
}
