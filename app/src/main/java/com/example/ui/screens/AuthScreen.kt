package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.HunterRepository
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    initialIsRegister: Boolean = false,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()

    var isRegister by remember { mutableStateOf(initialIsRegister) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hunterName by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    fun submitAuth() {
        errorMessage = null
        successMessage = null

        if (email.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorMessage = "Invalid email format."
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (isRegister && password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = if (isRegister) {
                HunterRepository.register(email, password, hunterName.ifBlank { "Hunter" })
            } else {
                HunterRepository.signIn(email, password)
            }

            isLoading = false
            result.onSuccess {
                onAuthSuccess()
            }.onFailure {
                errorMessage = it.message ?: "Authentication failed."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with Logo
            Image(
                painter = painterResource(id = R.drawable.ic_arise_logo),
                contentDescription = "ARISE System",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ARISE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
            )

            Text(
                text = "HUNTER SYSTEM TERMINAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Mode Selector (Sign In vs Register)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isRegister = false
                            errorMessage = null
                        },
                    color = if (!isRegister) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (!isRegister) accent else SystemCardBorder)
                ) {
                    Text(
                        text = "SIGN IN",
                        color = if (!isRegister) accent else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isRegister = true
                            errorMessage = null
                        },
                    color = if (isRegister) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isRegister) accent else SystemCardBorder)
                ) {
                    Text(
                        text = "REGISTER",
                        color = if (isRegister) accent else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card Form
            SystemWindowCard(borderColor = accent.copy(alpha = 0.4f)) {
                if (isRegister) {
                    OutlinedTextField(
                        value = hunterName,
                        onValueChange = { hunterName = it },
                        label = { Text("Hunter Codename") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accent) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Association Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = accent) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password (min 6 chars)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accent) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitAuth() })
                )

                if (isRegister) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accent) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitAuth() })
                    )
                }

                if (!isRegister) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = accent,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Error message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        color = CrimsonPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = CrimsonPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Submit Action Button
                Button(
                    onClick = { submitAuth() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegister) "INITIALIZE HUNTER ACCOUNT" else "ACCESS SYSTEM",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val googleResult = com.example.data.HunterAuthManager.launchGoogleSignIn(context)
                        isLoading = false
                        googleResult.onSuccess { (displayName, email) ->
                            HunterRepository.signInWithGoogle(displayName, email)
                            onAuthSuccess()
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Google Sign-In failed or was cancelled."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accent)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "G", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "SIGN IN WITH GOOGLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Demo Credentials: hunter@arise.io / hunter123",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var resetResult by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SystemSurface),
                border = BorderStroke(1.dp, accent)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "RESET SYSTEM CREDENTIALS",
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your registered Association email to transmit a recovery protocol.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (resetResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetResult ?: "",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("CLOSE", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = HunterRepository.resetPassword(resetEmail)
                                    res.onSuccess {
                                        resetResult = "Recovery protocol dispatched."
                                    }.onFailure {
                                        resetResult = it.message ?: "Failed to reset."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("TRANSMIT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
