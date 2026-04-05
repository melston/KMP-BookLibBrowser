package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.elsoft.bkdb.repository.BookRepository
import org.elsoft.bkdb.repository.RepositoryProvider
import org.elsoft.bkdb.utils.ConfigManager

@Composable
fun SetupScreen(onConfigSaved: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val repository = RepositoryProvider.repository

    var dbUrl by remember { mutableStateOf(ConfigManager.dbUrl) }
    var dbUser by remember { mutableStateOf(ConfigManager.dbUser) }
    var dbPass by remember { mutableStateOf(ConfigManager.dbPassword) }
    var viewerCommand by remember { mutableStateOf(ConfigManager.viewerCommand) }
    var dropboxAppKey by remember { mutableStateOf(ConfigManager.dropboxAppKey) }
    var dropboxAppSecret by remember { mutableStateOf(ConfigManager.dropboxAppSecret) }
    var dropboxRefreshToken by remember { mutableStateOf(ConfigManager.dropboxRefreshToken) }
    var dropboxRoot by remember { mutableStateOf(ConfigManager.dropboxRoot) }
    var isTesting by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState) // Makes it scrollable
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Configuration Setup",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Group Fields
                Text(
                    "Database Setup",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = dbUrl,
                    onValueChange = { dbUrl = it },
                    label = { Text("DB URL") }
                )
                OutlinedTextField(
                    value = dbUser,
                    onValueChange = { dbUser = it },
                    label = { Text("DB Username") }
                )
                OutlinedTextField(
                    value = dbPass,
                    onValueChange = { dbPass = it },
                    label = { Text("DB Password") },
                    visualTransformation = PasswordVisualTransformation() // Hides characters
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp, // Optional: default is 1.dp
                    color = MaterialTheme.colorScheme.outlineVariant // Optional: default theme color
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp, // Optional: default is 1.dp
                    color = MaterialTheme.colorScheme.outlineVariant // Optional: default theme color
                )

                Text("Dropbox Settings", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = dropboxAppKey,
                    onValueChange = { dropboxAppKey = it },
                    label = { Text("Dropbox App Key") }
                )
                OutlinedTextField(
                    value = dropboxAppSecret,
                    onValueChange = { dropboxAppSecret = it },
                    label = { Text("Dropbox App Secret") }
                )
                OutlinedTextField(
                    value = dropboxRefreshToken,
                    onValueChange = { dropboxRefreshToken = it },
                    label = { Text("Dropbox Refresh Token") }
                )
                OutlinedTextField(
                    value = dropboxRoot,
                    onValueChange = { dropboxRoot = it },
                    label = { Text("Dropbox Root") }
                )

                Text("Misc Settings", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = viewerCommand,
                    onValueChange = { viewerCommand = it },
                    label = { Text("Viewer Application") }
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    enabled = !isTesting,
                    onClick = {
                        scope.launch {
                            isTesting = true
                            val success = withContext(Dispatchers.IO) {
                                repository.isAvailable()
                            }

                            if (success) {
                                ConfigManager.dbUser = dbUser
                                ConfigManager.dbUrl = dbUrl
                                ConfigManager.dbPassword = dbPass
                                ConfigManager.viewerCommand = viewerCommand
                                ConfigManager.dropboxAppKey = dropboxAppKey
                                ConfigManager.dropboxAppSecret = dropboxAppSecret
                                ConfigManager.dropboxRefreshToken = dropboxRefreshToken
                                ConfigManager.dropboxRoot = dropboxRoot
                                onConfigSaved()
                            } else {
                                isTesting = false
                                snackbarHostState.showSnackbar(
                                    message = "Connection failed. Check your URL, user, or password.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Save and Connect")
                    }
                }
            }

            CommonScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

        }
    }
}
