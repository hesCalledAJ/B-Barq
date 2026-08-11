package com.aliJafari.bbarq.ui.auth

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.auth.viewmodel.LoginUiState
import com.aliJafari.bbarq.ui.auth.viewmodel.LoginViewModel
import com.aliJafari.bbarq.ui.theme.BBarqTheme


class LoginActivity : ComponentActivity() {
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            BBarqTheme {
                LoginScreen(vm, this)
            }
        }
    }
}

@Composable
fun LoginScreen(vm: LoginViewModel, context: Context) {
    val state by vm.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is LoginUiState.EnterPhone -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.login_phone), color = MaterialTheme.colorScheme.primary) })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.sendOtp(phone) }) { Text(stringResource(R.string.login_send_code)) }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.login_attention), color = MaterialTheme.colorScheme.error)
            }

            is LoginUiState.EnterCode -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.login_sms_code), color = MaterialTheme.colorScheme.primary) })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.verifyOtp(context, code) }) { Text(stringResource(R.string.login_verify_code)) }
            }

            is LoginUiState.Loading -> {
                CircularProgressIndicator()
            }

            is LoginUiState.Error -> {
                Text((state as LoginUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }

            is LoginUiState.Success -> {
                Text(
                    stringResource(
                        R.string.login_success_token,
                        (state as LoginUiState.Success).token.take(20)
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val i: Intent = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName())!!
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i)
                    System.exit(0)
                }) { Text(stringResource(R.string.login_restart_app)) }
            }
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun Test(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp), verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {  },
            label = { Text(stringResource(R.string.login_phone), color = MaterialTheme.colorScheme.onSurface) })
    }
}
