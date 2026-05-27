package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MotorcycleDrawing
import com.example.ui.MotoTrackerViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MotoTrackerViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: MotoTrackerViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeBikeId by viewModel.activeBikeId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val toastIsError by viewModel.toastIsError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var isAddingBike by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MotoDarkBg)
    ) {
        // App routing screen
        when {
            currentUser == null -> {
                LoginRegisterScreen(
                    onLoginSuccess = { isAddingBike = false },
                    viewModel = viewModel
                )
            }
            isAddingBike -> {
                AddBikeScreen(
                    onBack = { isAddingBike = false },
                    onSuccess = { isAddingBike = false },
                    viewModel = viewModel
                )
            }
            activeBikeId == null -> {
                GarageScreen(
                    onAddBikeClick = { isAddingBike = true },
                    viewModel = viewModel
                )
            }
            else -> {
                DashboardScreen(
                    onBackClick = { viewModel.selectBike(null) },
                    viewModel = viewModel
                )
            }
        }

        // Shared Feedback Toast alert
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = MotoCardBg,
                    border = BorderStroke(
                        1.dp,
                        if (toastIsError) MotoDanger.copy(alpha = 0.5f) else MotoSuccess.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 76.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (toastIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (toastIsError) MotoDanger else MotoSuccess
                        )
                        Text(
                            text = msg,
                            color = MotoTextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Auto-dismiss logic in Compose
            LaunchedEffect(msg) {
                delay(3000)
                viewModel.dismissToast()
            }
        }

        // Shared Loader
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MotoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MotoPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Por favor espera...",
                            color = MotoTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 1. LOGIN & REGISTRATION SCREEN
// --------------------------------------------------------------------------
@Composable
fun LoginRegisterScreen(
    onLoginSuccess: () -> Unit,
    viewModel: MotoTrackerViewModel
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    var showGooglePicker by remember { mutableStateOf(false) }
    var showOtherEmailInput by remember { mutableStateOf(false) }
    var inputOtherEmail by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MotoPrimary.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 1.2f
                    )
                )
            }
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Moto logo banner
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(MotoPrimary, Color(0xFF00B0FF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = MotoDarkBg,
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MotoTracker",
                    color = MotoTextMain,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Tu control de mantenimiento inteligente",
                    color = MotoTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Tab switch control
            Surface(
                color = MotoSecondaryBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Button(
                        onClick = { isLoginTab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoginTab) MotoCardBg else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Ingresar",
                            color = if (isLoginTab) MotoTextMain else MotoTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = { isLoginTab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isLoginTab) MotoCardBg else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Registrarse",
                            color = if (!isLoginTab) MotoTextMain else MotoTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Primary user inputs
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Email input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CORREO ELECTRÓNICO",
                            color = MotoTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("ejemplo@correo.com", color = MotoTextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MotoTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoPrimary,
                                unfocusedBorderColor = MotoBorder,
                                focusedContainerColor = MotoInputBg,
                                unfocusedContainerColor = MotoInputBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )
                    }

                    // Password input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CONTRASEÑA",
                            color = MotoTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••", color = MotoTextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MotoTextMuted) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoPrimary,
                                unfocusedBorderColor = MotoBorder,
                                focusedContainerColor = MotoInputBg,
                                unfocusedContainerColor = MotoInputBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )
                    }

                    // Register password check
                    if (!isLoginTab) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CONFIRMAR CONTRASEÑA",
                                color = MotoTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                placeholder = { Text("Repita su contraseña", color = MotoTextMuted.copy(alpha = 0.5f)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MotoTextMuted) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder,
                                    focusedContainerColor = MotoInputBg,
                                    unfocusedContainerColor = MotoInputBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_confirm_input")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isLoginTab) {
                                viewModel.login(email, password, onLoginSuccess)
                            } else {
                                if (password != passwordConfirm) {
                                    viewModel.showToast("Las contraseñas no coinciden", isError = true)
                                } else {
                                    viewModel.signUp(email, password, onLoginSuccess)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoPrimary,
                            contentColor = MotoDarkBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLoginTab) "Iniciar Sesión" else "Crear Cuenta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = if (isLoginTab) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MotoBorder)
                        Text("O de lo contrario", color = MotoTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MotoBorder)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showGooglePicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E1E24)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_login_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini Google Icon 'G'
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Continuar con Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Demo Mode switch links
            TextButton(
                onClick = { viewModel.setupDemoMode(onLoginSuccess) },
                modifier = Modifier.testTag("demo_mode_button")
            ) {
                Text(
                    text = "Probar en modo demostración (sin registro)",
                    color = MotoPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showGooglePicker) {
        Dialog(onDismissRequest = { showGooglePicker = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)), // Slate grey Google look
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF33333C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("google_picker_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Google Logo Icon (Simulated elegant svg)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Drawing G logo
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Acceder con Google",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Elige una cuenta para continuar en MotoTracker",
                        color = MotoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Accounts List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Account 1: jesuserl@gmail.com
                        Surface(
                            onClick = {
                                showGooglePicker = false
                                viewModel.signInWithGoogle("jesuserl@gmail.com", onLoginSuccess)
                            },
                            color = MotoCardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF33333C)),
                            modifier = Modifier.fillMaxWidth().testTag("google_account_primary")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Initials Avatar
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MotoPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("J", color = MotoDarkBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Column {
                                    Text("Jesús E. (Usuario Principal)", color = MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("jesuserl@gmail.com", color = MotoTextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        // Option 2: Choose another account
                        Surface(
                            onClick = {
                                showGooglePicker = false
                                showOtherEmailInput = true
                            },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF33333C)),
                            modifier = Modifier.fillMaxWidth().testTag("google_account_other")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF33333C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MotoTextMain, modifier = Modifier.size(16.dp))
                                }
                                Text("Usar otra cuenta", color = MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(onClick = { showGooglePicker = false }) {
                        Text("Cancelar", color = MotoPrimary)
                    }
                }
            }
        }
    }

    if (showOtherEmailInput) {
        Dialog(onDismissRequest = { showOtherEmailInput = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF33333C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("google_other_email_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Introduce tu correo Google",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = inputOtherEmail,
                        onValueChange = { inputOtherEmail = it },
                        placeholder = { Text("tu_cuenta@gmail.com", color = MotoTextMuted.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MotoTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder,
                            focusedContainerColor = MotoInputBg,
                            unfocusedContainerColor = MotoInputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("other_google_email_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showOtherEmailInput = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF33333C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Atrás")
                        }

                        Button(
                            onClick = {
                                if (inputOtherEmail.trim().isNotEmpty() && inputOtherEmail.contains("@")) {
                                    showOtherEmailInput = false
                                    viewModel.signInWithGoogle(inputOtherEmail.trim(), onLoginSuccess)
                                    inputOtherEmail = ""
                                } else {
                                    viewModel.showToast("Introduce un correo válido", isError = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoPrimary, contentColor = MotoDarkBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aceptar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 2. GARAGE SCREEN (MY GARAGE)
// --------------------------------------------------------------------------
@Composable
fun GarageScreen(
    onAddBikeClick: () -> Unit,
    viewModel: MotoTrackerViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val bikes by viewModel.bikesList.collectAsState()
    val customStyles by viewModel.customStylesList.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allHistory by viewModel.allHistoryList.collectAsState()

    var showBaasDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MotoDarkBg,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hola,",
                            color = MotoTextMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = currentUser?.email?.split("@")?.get(0) ?: "Motero",
                            color = MotoTextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(12.dp))
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Cambiar tema",
                                tint = MotoTextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showBaasDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "BaaS Cloud",
                                tint = MotoTextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(12.dp))
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Cerrar sesión",
                                tint = MotoTextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MotoBorder)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBikeClick,
                containerColor = MotoPrimary,
                contentColor = MotoDarkBg,
                shape = CircleShape,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("add_bike_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar Moto",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (bikes.isEmpty()) {
                // Empty state card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MotoSecondaryBg)
                            .border(1.dp, MotoBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = MotoTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Aún no tienes motos",
                        color = MotoTextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Registra tu primera motocicleta para comenzar con el seguimiento de tus servicios.",
                        color = MotoTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 260.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddBikeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoPrimary, contentColor = MotoDarkBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Agregar Moto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "Mi Garaje",
                                color = MotoTextMain,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Selecciona una moto o agrega una nueva",
                                color = MotoTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    items(bikes) { bike ->
                        // Gather alerts status for this bike
                        // Filter for this specific bike
                        val bikeHistories = allHistory.filter { it.bikeId == bike.id }
                        
                        var hasDanger = false
                        var hasWarning = false
                        
                        // Use static default list to calculate simple status
                        val defaultIntervals = listOf(
                            "aceite" to 3000,
                            "frenos" to 10000,
                            "llantas" to 15000,
                            "cadena" to 1000
                        )
                        
                        defaultIntervals.forEach { (id, km) ->
                            val lastLog = bikeHistories.find { it.typeId == id }
                            val lastKm = lastLog?.mileage ?: 0
                            val remaining = (lastKm + km) - bike.currentMileage
                            if (remaining < 0) hasDanger = true
                            else if (remaining <= 500) hasWarning = true
                        }

                        val (statusPillText, statusPillColor) = when {
                            hasDanger -> "Servicio Vencido" to MotoDanger
                            hasWarning -> "Próximo Servicio" to MotoAccent
                            else -> "Al Día" to MotoSuccess
                        }

                        Surface(
                            color = MotoCardBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MotoBorder),
                            onClick = { viewModel.selectBike(bike.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bike_item_card_${bike.id}")
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom avatar motor drawing or photo url!
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MotoSecondaryBg)
                                            .border(1.dp, MotoBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val styleDetails = getStyleDetails(bike.style, customStyles)
                                        if (bike.photoUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = bike.photoUrl,
                                                contentDescription = "Foto de la moto",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            MotorcycleDrawing(
                                                style = styleDetails.second,
                                                modifier = Modifier.size(60.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${bike.brand} ${bike.model}",
                                            color = MotoTextMain,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val styleDetails = getStyleDetails(bike.style, customStyles)
                                        Text(
                                            text = "${bike.year} • ${styleDetails.first.uppercase()}",
                                            color = MotoTextMuted,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = MotoPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${bike.currentMileage.toLocaleString()} km",
                                                color = MotoPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Status Pill upper right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusPillColor.copy(alpha = 0.12f))
                                        .border(1.dp, statusPillColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusPillText.uppercase(),
                                        color = statusPillColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // BaaS Settings Dialog
    if (showBaasDialog) {
        Dialog(onDismissRequest = { showBaasDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoSecondaryBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configurar BaaS",
                            color = MotoTextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showBaasDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MotoTextMuted)
                        }
                    }

                    Text(
                        text = "Vincule MotoTracker con su base de datos Firebase o Supabase para sincronizar en tiempo real.",
                        color = MotoTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    var providerIndex by remember { mutableIntStateOf(0) }
                    val providers = listOf(
                        "SQLite Local (Persistente)",
                        "Supabase Cloud",
                        "Firebase Firestore"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PROVEEDOR ACTIVO",
                            color = MotoTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MotoDarkBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MotoBorder),
                            onClick = {
                                providerIndex = (providerIndex + 1) % providers.size
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(providers[providerIndex], color = MotoTextMain, fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MotoTextMuted)
                            }
                        }
                    }

                    if (providerIndex > 0) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Configurar URL de conexión", color = MotoTextMuted.copy(alpha = 0.3f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoPrimary,
                                unfocusedBorderColor = MotoBorder,
                                focusedContainerColor = MotoInputBg,
                                unfocusedContainerColor = MotoInputBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showBaasDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MotoTextMain),
                            border = BorderStroke(1.dp, MotoBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                viewModel.showToast("Base de datos sincronizada: SQLite")
                                showBaasDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoAccent, contentColor = MotoDarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 3. ADD BIKE SCREEN
// --------------------------------------------------------------------------
@Composable
fun AddBikeScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: MotoTrackerViewModel
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2022") }
    var odo by remember { mutableStateOf("") }

    val customStyles by viewModel.customStylesList.collectAsState()

    val defaultStyles = listOf(
        "sport" to "Deportiva / Naked",
        "scooter" to "Scooter / Urbana",
        "adventure" to "Doble Propósito",
        "cruiser" to "Chopper / Cruiser"
    )

    // Merge default and custom styles dynamically!
    val allStyles = remember(customStyles) {
        val list = mutableListOf<Pair<String, String>>()
        list.addAll(defaultStyles)
        customStyles.forEach {
            list.add(it.id to it.name)
        }
        list
    }

    var selectedStyle by remember { mutableStateOf("sport") }
    var photoUrl by remember { mutableStateOf("") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
            photoUrl = uri.toString()
        }
    }

    var lastKm by remember { mutableStateOf("") }
    var lastDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    // Dialog State for creating a custom motorcycle type
    var showCreateStyleDialog by remember { mutableStateOf(false) }
    var customStyleName by remember { mutableStateOf("") }
    var customStyleIcon by remember { mutableStateOf("scrambler") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MotoDarkBg,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_to_garage_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MotoTextMain)
                    }
                    Text(
                        text = "Nueva Moto",
                        color = MotoTextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(40.dp))
                }
                HorizontalDivider(color = MotoBorder)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Details section
            Text(
                text = "DETALLES DE LA MOTO",
                color = MotoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca") },
                        placeholder = { Text("Ej: Yamaha, Honda, Suzuki") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bike_brand")
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ej: MT-07, CB500X") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bike_model")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) year = input
                            },
                            label = { Text("Año") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoPrimary,
                                unfocusedBorderColor = MotoBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bike_year")
                        )

                        OutlinedTextField(
                            value = odo,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) odo = input
                            },
                            label = { Text("Odómetro actual (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoPrimary,
                                unfocusedBorderColor = MotoBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("bike_odometer")
                        )
                    }
                }
            }

            // Photo section
            Text(
                text = "FOTO DE LA MOTO",
                color = MotoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth().testTag("bike_photo_section_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (photoUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Vista previa de la moto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoUrl = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoPrimary.copy(alpha = 0.15f),
                            contentColor = MotoPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_from_gallery_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Seleccionar desde la Galería", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text("Enlace de la Foto (Opcional - URL)") },
                        placeholder = { Text("Pega un URL de imagen (https://...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bike_photo_url_input")
                    )

                    Text(
                        text = "O selecciona una foto de muestra preestablecida:",
                        color = MotoTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Curated high quality presets matching the motorcycle profiles!
                    val presets = listOf(
                        Triple("https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=500&auto=format&fit=crop&q=60", "Scrambler / Retro", "scrambler"),
                        Triple("https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?w=500&auto=format&fit=crop&q=60", "Sport / Naked", "sport"),
                        Triple("https://images.unsplash.com/photo-1515777315835-281b94c9589f?w=500&auto=format&fit=crop&q=60", "Aventura / ADV", "adventure"),
                        Triple("https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60", "Cruiser / Chopper", "cruiser"),
                        Triple("https://images.unsplash.com/photo-1563272658-00569766ee14?w=500&auto=format&fit=crop&q=60", "Scooter / Urban", "scooter"),
                        Triple("https://images.unsplash.com/photo-1599819811279-d5ad9cccf838?w=500&auto=format&fit=crop&q=60", "Cafe Racer", "cafe_racer"),
                        Triple("https://images.unsplash.com/photo-1471440671318-55dddde7a76a?w=500&auto=format&fit=crop&q=60", "Tracker style", "tracker")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (presetUrl, label, styleKey) ->
                            val isPresetSelected = photoUrl == presetUrl
                            Surface(
                                color = if (isPresetSelected) MotoPrimary.copy(alpha = 0.12f) else MotoSecondaryBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPresetSelected) MotoPrimary else Color.Transparent
                                ),
                                onClick = {
                                    photoUrl = presetUrl
                                    // Automatically pre-select the appropriate visual template as well!
                                    val match = allStyles.find { it.first == styleKey || it.first.lowercase() == styleKey.lowercase() }
                                    if (match != null) {
                                        selectedStyle = match.first
                                    } else {
                                        selectedStyle = styleKey
                                    }
                                },
                                modifier = Modifier.width(135.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray)
                                    ) {
                                        AsyncImage(
                                            model = presetUrl,
                                            contentDescription = label,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = MotoTextMain,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Style section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTILO DE LA MOTO",
                    color = MotoTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                TextButton(
                    onClick = { showCreateStyleDialog = true },
                    modifier = Modifier.testTag("open_create_style_dialog")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear Tipo de Moto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                allStyles.forEach { (styleKey, styleName) ->
                    val isActive = selectedStyle == styleKey
                    Surface(
                        color = if (isActive) MotoPrimary.copy(alpha = 0.08f) else MotoCardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) MotoPrimary else MotoBorder
                        ),
                        onClick = { selectedStyle = styleKey },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_option_$styleKey")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val resolvedStyleIcon = getStyleDetails(styleKey, customStyles).second
                            MotorcycleDrawing(
                                style = resolvedStyleIcon,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = styleName,
                                color = if (isActive) MotoPrimary else MotoTextMain,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Baseline section
            Text(
                text = "PUNTO DE PARTIDA INICIAL",
                color = MotoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Surface(
                color = MotoAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MotoAccent.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MotoAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Para evitar que se generen alertas de todos los servicios vencidos inmediatamente, indica cuándo fue su último servicio global aproximado.",
                        color = MotoTextMain,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = lastKm,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) lastKm = input
                        },
                        label = { Text("Último servicio (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("last_service_km")
                    )

                    OutlinedTextField(
                        value = lastDate,
                        onValueChange = { lastDate = it },
                        label = { Text("Fecha de servicio") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("last_service_date")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val odoVal = odo.trim().toIntOrNull() ?: 0
                    val lastKmVal = if (lastKm.trim().isEmpty()) odoVal else (lastKm.trim().toIntOrNull() ?: 0)
                    val yearVal = year.trim().toIntOrNull() ?: 2022

                    if (brand.trim().isEmpty()) {
                        viewModel.showToast("La marca de la moto es obligatoria", isError = true)
                    } else if (model.trim().isEmpty()) {
                        viewModel.showToast("El modelo de la moto es obligatorio", isError = true)
                    } else if (year.trim().isEmpty()) {
                        viewModel.showToast("El año es obligatorio", isError = true)
                    } else if (yearVal < 1900 || yearVal > 2100) {
                        viewModel.showToast("El año debe estar entre 1900 y 2100", isError = true)
                    } else if (odo.trim().isEmpty()) {
                        viewModel.showToast("El kilometraje / odómetro actual es obligatorio", isError = true)
                    } else if (odo.trim().toLongOrNull() ?: 0L > 2000000L) {
                        viewModel.showToast("El odómetro actual no puede exceder los 2,000,000 km", isError = true)
                    } else if (lastKm.trim().isNotEmpty() && (lastKm.trim().toLongOrNull() ?: 0L > 2000000L)) {
                        viewModel.showToast("El kilometraje de último servicio no puede exceder los 2,000,000 km", isError = true)
                    } else if (lastKmVal > odoVal) {
                        viewModel.showToast("El último servicio no puede ser mayor al odómetro actual", isError = true)
                    } else if (lastDate.trim().isEmpty()) {
                        viewModel.showToast("La fecha de servicio es obligatoria", isError = true)
                    } else {
                        viewModel.addBike(
                            brand = brand,
                            model = model,
                            year = yearVal,
                            style = selectedStyle,
                            photoUrl = photoUrl,
                            currentMileage = odoVal,
                            lastServiceKm = lastKmVal,
                            lastServiceDate = lastDate,
                            onSuccess = onSuccess
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MotoPrimary,
                    contentColor = MotoDarkBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bike_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Guardar Motocicleta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Modal Dialog to create a custom motorcycle style template
    if (showCreateStyleDialog) {
        Dialog(onDismissRequest = { showCreateStyleDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoSecondaryBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("create_custom_style_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Crear Tipo de Moto Custom",
                        color = MotoTextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customStyleName,
                        onValueChange = { customStyleName = it },
                        label = { Text("Nombre del Tipo") },
                        placeholder = { Text("Ej: Scrambler Retro, Tracker Urbano") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_style_name_input")
                    )

                    Text(
                        text = "Selecciona una Plantilla de Ícono:",
                        color = MotoTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val iconTemplates = listOf(
                        "scrambler" to "Scrambler Classic",
                        "cafe_racer" to "Cafe Racer Hump",
                        "tracker" to "Flat Tracker Plate",
                        "sport" to "Sport Naked",
                        "scooter" to "Urban Scooter",
                        "adventure" to "Adventure ADV",
                        "cruiser" to "Low Cruiser"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        iconTemplates.forEach { (key, label) ->
                            val isChosen = customStyleIcon == key
                            Surface(
                                color = if (isChosen) MotoPrimary.copy(alpha = 0.12f) else MotoCardBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChosen) MotoPrimary else MotoBorder),
                                onClick = { customStyleIcon = key },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MotorcycleDrawing(
                                        style = key,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = MotoTextMain,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreateStyleDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MotoTextMain
                            ),
                            border = BorderStroke(1.dp, MotoBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (customStyleName.trim().isEmpty()) {
                                    viewModel.showToast("El nombre del tipo es obligatorio", isError = true)
                                } else {
                                    viewModel.addCustomStyle(customStyleName, customStyleIcon) {
                                        showCreateStyleDialog = false
                                        selectedStyle = customStyleName.trim()
                                        customStyleName = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MotoPrimary,
                                contentColor = MotoDarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 4. MAIN TRACKER ACTIVE DASHBOARD SCREEN
// --------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit,
    viewModel: MotoTrackerViewModel
) {
    val activeBike by viewModel.activeBike.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val customStyles by viewModel.customStylesList.collectAsState()

    var showQuickOdoDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MotoDarkBg,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("dashboard_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MotoTextMain)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeBike?.let { "${it.brand} ${it.model}" } ?: "Cargando...",
                            color = MotoTextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val styleDetails = activeBike?.let { getStyleDetails(it.style, customStyles) }
                        Text(
                            text = activeBike?.let { "${it.year} • ${styleDetails?.first?.uppercase()}" } ?: "",
                            color = MotoTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(12.dp))
                                .testTag("dashboard_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Cambiar tema",
                                tint = MotoTextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showQuickOdoDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotoSecondaryBg)
                                .border(1.dp, MotoBorder, RoundedCornerShape(12.dp))
                                .testTag("quick_mileage_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Configurar odómetro",
                                tint = MotoAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MotoBorder)
            }
        },
        bottomBar = {
            // Adaptive Navigation Bottom Bar
            Surface(
                color = MotoSecondaryBg.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(72.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("inicio", "Inicio", Icons.Default.Home),
                        Triple("historial", "Historial", Icons.Default.History),
                        Triple("combustible", "Combustible", Icons.Default.LocalGasStation),
                        Triple("servicio", "Servicio", Icons.Default.AddCircle),
                        Triple("ajustes", "Ajustes", Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, label, icon) ->
                        val isActive = activeTab == tab
                        IconButton(
                            onClick = { viewModel.setActiveTab(tab) },
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .testTag("nav_tab_$tab")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isActive) MotoPrimary else MotoTextMuted,
                                    modifier = Modifier.size(if (isActive) 24.dp else 21.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = if (isActive) MotoPrimary else MotoTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                "inicio" -> DashboardInicioTab(viewModel = viewModel, onUpdateMileageClick = { showQuickOdoDialog = true })
                "historial" -> DashboardHistorialTab(viewModel = viewModel)
                "combustible" -> DashboardCombustibleTab(viewModel = viewModel)
                "servicio" -> DashboardServicioTab(viewModel = viewModel)
                "ajustes" -> DashboardAjustesTab(viewModel = viewModel, onDeleted = onBackClick)
            }
        }
    }

    // Quick Update Odometer Dialog
    if (showQuickOdoDialog && activeBike != null) {
        var newOdoStr by remember { mutableStateOf(activeBike?.currentMileage?.toString() ?: "") }
        Dialog(onDismissRequest = { showQuickOdoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoSecondaryBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Actualizar Odómetro",
                        color = MotoTextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Ingresa el kilometraje actual para ${activeBike?.brand} ${activeBike?.model}:",
                        color = MotoTextMuted,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = newOdoStr,
                        onValueChange = { newOdoStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder,
                            focusedContainerColor = MotoInputBg,
                            unfocusedContainerColor = MotoInputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_odo_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showQuickOdoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MotoTextMain),
                            border = BorderStroke(1.dp, MotoBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val intVal = newOdoStr.toIntOrNull() ?: 0
                                if (intVal >= (activeBike?.currentMileage ?: 0)) {
                                    viewModel.updateOdometer(activeBike!!.id, intVal)
                                    showQuickOdoDialog = false
                                } else {
                                    viewModel.showToast("El kilometraje no puede ser menor al actual", isError = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoPrimary, contentColor = MotoDarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            enabled = newOdoStr.toIntOrNull() ?: 0 >= (activeBike?.currentMileage ?: 0)
                        ) {
                            Text("Actualizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// DASHBOARD TABS IMPLEMENTATIONS
// --------------------------------------------------------------------------

@Composable
fun DashboardInicioTab(
    viewModel: MotoTrackerViewModel,
    onUpdateMileageClick: () -> Unit
) {
    val activeBike by viewModel.activeBike.collectAsState()
    val intervals by viewModel.activeBikeIntervals.collectAsState()
    val history by viewModel.activeBikeHistory.collectAsState()
    val viewMode by viewModel.dashboardViewMode.collectAsState()

    // Persistent preference localization states
    val odometerUnit by viewModel.odometerUnit.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val dailyAverageKm by viewModel.dailyAverageKm.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Mileage Card (Unit-aware and dynamically converted)
        Surface(
            color = MotoCardBg,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Accent left border effect drawn safely using drawBehind
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawLine(
                                color = MotoPrimary,
                                start = Offset(0f, 0f),
                                end = Offset(0f, this.size.height),
                                strokeWidth = 12f
                            )
                        }
                )

                Row(
                    modifier = Modifier.padding(20.dp).padding(start = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ODÓMETRO ACTUAL",
                            color = MotoTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val displayMil = activeBike?.currentMileage?.toMilOrKm(odometerUnit) ?: 0
                            Text(
                                text = displayMil.toLocaleString(),
                                color = MotoTextMain,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = odometerUnit,
                                color = MotoTextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onUpdateMileageClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoPrimary.copy(alpha = 0.1f),
                            contentColor = MotoPrimary
                        ),
                        border = BorderStroke(1.dp, MotoPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Actualizar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Expense, Cost/Km, Fuel Economy, and Predictions Analytics Card (New Premium Feature!)
        var isAnalyticsExpanded by remember { mutableStateOf(false) }
        val fuelLogs by viewModel.activeBikeFuelLogs.collectAsState()

        val totalFuelCost = fuelLogs.sumOf { it.cost }
        val totalMaintCost = history.sumOf { it.cost }
        val totalExpenses = totalFuelCost + totalMaintCost

        // Distance Traveled Calculation
        val currentMileage = activeBike?.currentMileage ?: 0
        val listMileages = (history.map { it.mileage } + fuelLogs.map { it.mileage }).filter { it > 0 }
        val minLogMileage = listMileages.minOrNull() ?: currentMileage
        val deltaDistance = (currentMileage - minLogMileage).coerceAtLeast(0)

        val costPerUnit = if (deltaDistance > 0) {
            totalExpenses / deltaDistance.toMilOrKm(odometerUnit)
        } else {
            if (currentMileage > 0) totalExpenses / currentMileage.toMilOrKm(odometerUnit) else 0.0
        }

        // Fuel economy calculation: average km/L or mi/gal
        val sortedLogs = remember(fuelLogs) { fuelLogs.sortedBy { it.mileage } }
        val averageFuelEconomyStr = if (sortedLogs.size >= 2) {
            val minOdoVal = sortedLogs.first().mileage
            val maxOdoVal = sortedLogs.last().mileage
            val deltaKm = (maxOdoVal - minOdoVal).toDouble()
            val litersSumInInterval = sortedLogs.drop(1).sumOf { it.liters }

            if (litersSumInInterval > 0.0 && deltaKm > 0.0) {
                if (odometerUnit == "mi") {
                    val miles = deltaKm * 0.621371
                    val gallons = litersSumInInterval * 0.264172
                    String.format(Locale.getDefault(), "%,.1f mpg", miles / gallons)
                } else {
                    String.format(Locale.getDefault(), "%,.1f km/L", deltaKm / litersSumInInterval)
                }
            } else {
                "Espera otra carga..."
            }
        } else {
            "Faltan registros"
        }

        CustomAnalyticsCard(
            totalExpenses = totalExpenses,
            totalFuelCost = totalFuelCost,
            totalMaintCost = totalMaintCost,
            costPerUnit = costPerUnit,
            odoUnitStr = odometerUnit,
            currencySymbol = currencySymbol,
            averageFuelEconomyStr = averageFuelEconomyStr,
            isExpanded = isAnalyticsExpanded,
            onExpandClick = { isAnalyticsExpanded = !isAnalyticsExpanded },
            onExportReportClick = {
                val reportContent = buildString {
                    append("===== MOTO TRACKER - REPORTE DE MOTOCICLETA =====\n")
                    append("Generado el: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
                    append("Vehículo: ${activeBike?.brand?.uppercase()} ${activeBike?.model?.uppercase()} (${activeBike?.year})\n")
                    append("Odómetro Actual: ${activeBike?.currentMileage?.toMilOrKm(odometerUnit)?.toLocaleString()} $odometerUnit\n")
                    append("Unidad de Medida: ${odometerUnit.uppercase()}\n")
                    append("Moneda de Registro: $currencySymbol\n")
                    append("-------------------------------------------------\n")
                    append("RESUMEN FINANCIERO & RENDIMIENTO:\n")
                    append("  - Gasto Total en Combustible: ${totalFuelCost.toFormattedCurrency(currencySymbol)}\n")
                    append("  - Gasto Total en Mantenimiento: ${totalMaintCost.toFormattedCurrency(currencySymbol)}\n")
                    append("  - Gasto Global: ${totalExpenses.toFormattedCurrency(currencySymbol)}\n")
                    append("  - Rendimiento Promedio: $averageFuelEconomyStr\n")
                    append("  - Costo Promedio por $odometerUnit: ${costPerUnit.toFormattedCurrency(currencySymbol)}/$odometerUnit\n")
                    append("-------------------------------------------------\n\n")

                    append("HISTORIAL DE INTERVENCIONES:\n")
                    if (history.isEmpty()) {
                        append("Sin registros de servicio de mantenimiento.\n")
                    } else {
                        append(String.format("%-12s | %-16s | %-24s | %-12s | %s\n", "Fecha", "Odo ($odometerUnit)", "Servicio", "Costo", "Notas"))
                        history.forEach { log ->
                            append(String.format(
                                "%-12s | %-16s | %-24s | %-12s | %s\n",
                                log.date,
                                "${log.mileage.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                log.typeName,
                                log.cost.toFormattedCurrency(currencySymbol),
                                log.notes
                            ))
                        }
                    }
                    append("\n")

                    append("HISTORIAL DE COMBUSTIBLE:\n")
                    if (fuelLogs.isEmpty()) {
                        append("Sin registros de combustible.\n")
                    } else {
                        append(String.format("%-12s | %-16s | %-12s | %-12s | %s\n", "Fecha", "Odo ($odometerUnit)", "Volumen", "Costo", "Notas"))
                        fuelLogs.forEach { log ->
                            append(String.format(
                                "%-12s | %-16s | %-12s | %-12s | %s\n",
                                log.date,
                                "${log.mileage.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                log.liters.toFormattedLiters(odometerUnit),
                                log.cost.toFormattedCurrency(currencySymbol),
                                log.notes
                            ))
                        }
                    }
                }

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, reportContent)
                    putExtra(Intent.EXTRA_SUBJECT, "MotoTracker Reporte: ${activeBike?.brand} ${activeBike?.model}")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Exportar Reporte de Moto")
                context.startActivity(shareIntent)
            }
        )

        // Section header with toggle views
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mantenimientos",
                color = MotoTextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = MotoSecondaryBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MotoBorder)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    IconButton(
                        onClick = { viewModel.setDashboardViewMode("list") },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewMode == "list") MotoCardBg else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Vista Listado",
                            tint = if (viewMode == "list") MotoPrimary else MotoTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setDashboardViewMode("rpm") },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewMode == "rpm") MotoCardBg else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Vista Relojes",
                            tint = if (viewMode == "rpm") MotoPrimary else MotoTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Draw cards based on mode
        if (viewMode == "rpm") {
            // Cut experimental FlowRow out, use standard divided Rows instead (Fast, completely stable, beautiful)
            val chunkedIntervals = intervals.chunked(2)
            chunkedIntervals.forEach { rowPairs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPairs.forEach { interval ->
                        val lastLog = history.find { it.typeId == interval.intervalId }
                        val lastKm = lastLog?.mileage ?: 0
                        val currentMileage = activeBike?.currentMileage ?: 0
                        val remaining = (lastKm + interval.kmInterval) - currentMileage
                        val consumedRatio = (currentMileage - lastKm).toFloat() / interval.kmInterval.toFloat()
                        val consumedPercent = (consumedRatio * 100).toInt().coerceIn(0, 110)

                        val isOverdue = remaining < 0
                        val color = when {
                            isOverdue -> MotoDanger
                            remaining <= 500 -> MotoAccent
                            else -> MotoSuccess
                        }

                        val statusMsg = when {
                            isOverdue -> "Vencido"
                            remaining <= 500 -> "Próximo"
                            else -> "Al Día"
                        }

                        Surface(
                            color = MotoCardBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MotoBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(205.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Dial Canvas
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Blinking alarm light when overdue
                                    var lightBlink by remember { mutableStateOf(false) }
                                    if (isOverdue) {
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                lightBlink = !lightBlink
                                                delay(400)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (lightBlink) MotoDanger else MotoDanger.copy(alpha = 0.2f))
                                        )
                                    }

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Track circle: 240 deg sweep
                                        drawArc(
                                            color = MotoBorder,
                                            startAngle = 150f,
                                            sweepAngle = 240f,
                                            useCenter = false,
                                            style = Stroke(width = 12f, cap = StrokeCap.Round)
                                        )

                                        // Colored sweep health arc
                                        val activeSweep = (240f * (consumedPercent.toFloat() / 100f).coerceAtMost(1f))
                                        drawArc(
                                            color = color,
                                            startAngle = 150f,
                                            sweepAngle = activeSweep,
                                            useCenter = false,
                                            style = Stroke(width = 12f, cap = StrokeCap.Round)
                                        )

                                        // Dial Needle
                                        val needleAngleDeg = 150f + (240f * (consumedPercent.toFloat() / 100f).coerceAtMost(1.1f))
                                        val angleRad = Math.toRadians(needleAngleDeg.toDouble())
                                        val rx = (size.width / 2) + (size.width / 2.8f) * Math.cos(angleRad).toFloat()
                                        val ry = (size.height / 2) + (size.height / 2.8f) * Math.sin(angleRad).toFloat()

                                        drawLine(
                                            color = MotoDanger,
                                            start = Offset(size.width / 2, size.height / 2),
                                            end = Offset(rx, ry),
                                            strokeWidth = 5f,
                                            cap = StrokeCap.Round
                                        )
                                        drawCircle(
                                            color = MotoDarkBg,
                                            radius = 10f,
                                            center = Offset(size.width / 2, size.height / 2)
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.7f),
                                            radius = 5f,
                                            center = Offset(size.width / 2, size.height / 2)
                                        )
                                    }

                                    // Display center Text label
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.offset(y = 24.dp)
                                    ) {
                                        val displayRemaining = remaining.toMilOrKm(odometerUnit)
                                        Text(
                                            text = if (displayRemaining >= 0) displayRemaining.toLocaleString() else "-${Math.abs(displayRemaining).toLocaleString()}",
                                            color = MotoTextMain,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = odometerUnit,
                                            color = MotoTextMuted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = interval.name,
                                        color = MotoTextMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = statusMsg,
                                        color = color,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Último: ${lastKm.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                        color = MotoTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                    if (rowPairs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Standard List mode view of intervals
            intervals.forEach { interval ->
                val lastLog = history.find { it.typeId == interval.intervalId }
                val lastKm = lastLog?.mileage ?: 0
                val remaining = (lastKm + interval.kmInterval) - (activeBike?.currentMileage ?: 0)
                
                val progressPercent = if (remaining > 0) {
                    (remaining.toFloat() / interval.kmInterval.toFloat() * 100).coerceAtMost(100f)
                } else 0f

                val isOverdue = remaining < 0
                val color = when {
                    isOverdue -> MotoDanger
                    remaining <= 500 -> MotoAccent
                    else -> MotoSuccess
                }

                val remainingConv = remaining.toMilOrKm(odometerUnit)
                val statusText = when {
                    isOverdue -> "Vencido por ${Math.abs(remainingConv).toLocaleString()} $odometerUnit"
                    remaining <= 500 -> "Próximo en ${remainingConv.toLocaleString()} $odometerUnit"
                    else -> "Faltan ${remainingConv.toLocaleString()} $odometerUnit"
                }

                // Dynamic prediction date based on daily average kilometrage
                val predictionText = if (isOverdue) {
                    "¡Intervención requerida ya!"
                } else {
                    val days = (remaining.toFloat() / dailyAverageKm).toInt().coerceAtLeast(1)
                    val estTimeMs = System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000)
                    val formattedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(estTimeMs))
                    "Estimado: $formattedDate (en $days d.)"
                }

                Surface(
                    color = MotoCardBg,
                    border = BorderStroke(1.dp, MotoBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = interval.name,
                                color = MotoTextMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = statusText.uppercase(),
                                color = color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Custom progress indicator track
                        Surface(
                            color = MotoSecondaryBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressPercent / 100f)
                                    .background(color)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isOverdue) MotoDanger else MotoTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = predictionText,
                                    color = if (isOverdue) MotoDanger else MotoTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Frecuencia: ${interval.kmInterval.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                    color = MotoTextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Último: ${lastKm.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                    color = MotoTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// HELPER FUNCTION: RESOLVE CUSTOM MOTO STYLE DETAILS
// --------------------------------------------------------------------------
fun getStyleDetails(styleKey: String, customStyles: List<com.example.data.CustomBikeStyleEntity>): Pair<String, String> {
    // If styleKey itself matches a default lowercase style ID, return its default mapping
    val defaultMap = mapOf(
        "sport" to "Deportiva / Naked",
        "scooter" to "Scooter / Urbana",
        "adventure" to "Doble Propósito",
        "cruiser" to "Chopper / Cruiser"
    )
    if (defaultMap.containsKey(styleKey.lowercase())) {
        return defaultMap[styleKey.lowercase()]!! to styleKey.lowercase()
    }

    // Try finding in database custom styles (lowercase match or raw match)
    val foundCustom = customStyles.find { 
        it.id == styleKey || it.name.lowercase() == styleKey.lowercase() || it.id.lowercase() == styleKey.lowercase() 
    }
    if (foundCustom != null) {
        return foundCustom.name to foundCustom.iconKey
    }

    // Fallback split words or raw string capitalizing
    val formattedName = styleKey.lowercase()
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    // Map keywords to suitable canvas vectors
    val lowerStyle = styleKey.lowercase()
    val fallbackIcon = when {
        lowerStyle.contains("scrambler") -> "scrambler"
        lowerStyle.contains("racer") || lowerStyle.contains("cafe") -> "cafe_racer"
        lowerStyle.contains("track") -> "tracker"
        lowerStyle.contains("sport") || lowerStyle.contains("naked") -> "sport"
        lowerStyle.contains("scooter") || lowerStyle.contains("urban") || lowerStyle.contains("vespa") -> "scooter"
        lowerStyle.contains("adv") || lowerStyle.contains("prop") || lowerStyle.contains("trail") -> "adventure"
        lowerStyle.contains("chopper") || lowerStyle.contains("cruise") -> "cruiser"
        else -> "sport"
    }

    return formattedName to fallbackIcon
}

// --------------------------------------------------------------------------
// 4.3 FUEL LOG CONTROL TAB (DASHBOARD)
// --------------------------------------------------------------------------
@Composable
fun DashboardCombustibleTab(viewModel: MotoTrackerViewModel) {
    val activeBike by viewModel.activeBike.collectAsState()
    val fuelLogs by viewModel.activeBikeFuelLogs.collectAsState()

    // Preferences
    val odometerUnit by viewModel.odometerUnit.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var litrosStr by remember { mutableStateOf("") }
    var precioLitroStr by remember { mutableStateOf("") }
    var odoRefuelStr by remember { mutableStateOf("") }

    // Pre-populate odometer on refuel input with current bike mileage
    LaunchedEffect(activeBike, odometerUnit) {
        if (odoRefuelStr.isEmpty() && activeBike != null) {
            val convertedOdo = activeBike?.currentMileage?.toMilOrKm(odometerUnit) ?: 0
            odoRefuelStr = convertedOdo.toString()
        }
    }

    val litrosDouble = litrosStr.toDoubleOrNull() ?: 0.0
    val precioDouble = precioLitroStr.toDoubleOrNull() ?: 0.0
    val totalCalculated = if (litrosDouble > 0 && precioDouble > 0) {
        String.format(Locale.US, "%.2f", litrosDouble * precioDouble)
    } else {
        "0.00"
    }

    // Calculate core Fuel statistics in local views
    // display unit labels:
    val volumeLabel = if (odometerUnit == "mi") "gal" else "L"
    val volumeFieldLabel = if (odometerUnit == "mi") "Galones (gal)" else "Litros (L)"
    val priceFieldLabel = if (odometerUnit == "mi") "Precio/Galón ($currencySymbol)" else "Precio/Litro ($currencySymbol)"
    val odometerFieldLabel = if (odometerUnit == "mi") "Odómetro (mi)" else "Odómetro (km)"

    val totalVolumeInLocal = fuelLogs.sumOf { 
        if (odometerUnit == "mi") it.liters * 0.264172 else it.liters 
    }
    val totalDinero = fuelLogs.sumOf { it.cost }
    
    // For mpg / consumption statistics, sort logs by engine odometer reading
    val sortedLogs = remember(fuelLogs) { fuelLogs.sortedBy { it.mileage } }
    
    val calculatedConsumo = remember(sortedLogs, odometerUnit) {
        if (sortedLogs.size >= 2) {
            val minOdo = sortedLogs.first().mileage
            val maxOdo = sortedLogs.last().mileage
            val deltaKm = (maxOdo - minOdo).toDouble()
            
            // System: liters refueled following first fillup
            val litersSumInInterval = sortedLogs.drop(1).sumOf { it.liters }
            if (litersSumInInterval > 0.0 && deltaKm > 0.0) {
                if (odometerUnit == "mi") {
                    val miles = deltaKm * 0.621371
                    val gallons = litersSumInInterval * 0.264172
                    String.format(Locale.getDefault(), "%,.2f mpg", miles / gallons)
                } else {
                    val kmPerLiter = deltaKm / litersSumInInterval
                    String.format(Locale.getDefault(), "%,.2f km/L", kmPerLiter)
                }
            } else {
                "Espera otra carga..."
            }
        } else {
            "Faltan más registros"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Stats Banner
        Text(
            text = "RENDIMIENTO Y CONSUMO",
            color = MotoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = MotoPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Consumo Medio", color = MotoTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = calculatedConsumo,
                        color = MotoTextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MotoSuccess, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Inversión Total", color = MotoTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = totalDinero.toFormattedCurrency(currencySymbol),
                        color = MotoTextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Stats detail banner auxiliary
        Card(
            colors = CardDefaults.cardColors(containerColor = MotoSecondaryBg),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MotoBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Volumen Total", color = MotoTextMuted, fontSize = 11.sp)
                    Text("${String.format(Locale.US, "%.1f", totalVolumeInLocal)} $volumeLabel", color = MotoTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MotoBorder))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cargas", color = MotoTextMuted, fontSize = 11.sp)
                    Text("${fuelLogs.size} cargas", color = MotoTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Add Log card Form
        Text(
            text = "REGISTRAR CARGA DE COMBUSTIBLE",
            color = MotoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MotoCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth().testTag("fuel_input_form")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val placeHolderVal = if (odometerUnit == "mi") "Ej: 3.5" else "Ej: 10.5"
                    OutlinedTextField(
                        value = litrosStr,
                        onValueChange = { litrosStr = it },
                        label = { Text(volumeFieldLabel) },
                        placeholder = { Text(placeHolderVal) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fuel_liters_input")
                    )

                    OutlinedTextField(
                        value = precioLitroStr,
                        onValueChange = { precioLitroStr = it },
                        label = { Text(priceFieldLabel) },
                        placeholder = { Text("Ej: 1.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fuel_price_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = odoRefuelStr,
                        onValueChange = { odoRefuelStr = it },
                        label = { Text(odometerFieldLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("fuel_odo_input")
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("Costo aprox.", color = MotoTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text((totalCalculated.toDoubleOrNull() ?: 0.0).toFormattedCurrency(currencySymbol), color = MotoSuccess, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Button(
                    onClick = {
                        val active = activeBike
                        val volumeVal = litrosStr.toDoubleOrNull()
                        val priceVal = precioLitroStr.toDoubleOrNull()
                        val odoEntered = odoRefuelStr.toIntOrNull()

                        if (active == null) {
                            viewModel.showToast("Cargando motocicleta pasito a pasito", isError = true)
                        } else if (volumeVal == null || volumeVal <= 0.0) {
                            viewModel.showToast("Indica litros/galones válidos mayores a cero", isError = true)
                        } else if (priceVal == null || priceVal <= 0.0) {
                            viewModel.showToast("Indica precio por unidad válido", isError = true)
                        } else if (odoEntered == null || odoEntered <= 0) {
                            viewModel.showToast("Indica kilometraje de odómetro válido", isError = true)
                        } else {
                            // Convert entered odometer back to metrics KM if unit preference is 'mi'
                            val odoInKmMetric = odoEntered.fromMilOrKm(odometerUnit)

                            // Convert entered fuel volume back to metric Liters if unit preference is 'mi' (gallons)
                            val volumeInLitMetric = if (odometerUnit == "mi") volumeVal * 3.78541 else volumeVal

                            viewModel.addFuelLog(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                mileage = odoInKmMetric,
                                liters = volumeInLitMetric,
                                cost = volumeVal * priceVal,
                                notes = "Carga de combustible"
                            ) {
                                litrosStr = ""
                                precioLitroStr = ""
                                // Toast is handled securely via ViewModel
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotoPrimary,
                        contentColor = MotoDarkBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_fuel_log_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EvStation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Registrar Combustible", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Historial de Cargas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HISTORIAL DE CARGAS",
                color = MotoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text("${fuelLogs.size} cargas registradas", color = MotoTextMuted, fontSize = 11.sp)
        }

        if (fuelLogs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = MotoBorder, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay cargas registradas todavía", color = MotoTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Registra arriba tu carga para calcular rendimiento", color = MotoTextMuted.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            // Reverse chronological order for listing logs
            val displayLogs = remember(fuelLogs) { fuelLogs.sortedByDescending { it.mileage } }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayLogs.forEach { log ->
                    Surface(
                        color = MotoCardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MotoBorder),
                        modifier = Modifier.fillMaxWidth().testTag("fuel_log_item_${log.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MotoSecondaryBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EvStation, contentDescription = null, tint = MotoPrimary, modifier = Modifier.size(18.dp))
                                }

                                Column {
                                    val volLocal = if (odometerUnit == "mi") log.liters * 0.264172 else log.liters
                                    val formattedVolStr = String.format(Locale.getDefault(), "%,.1f %s", volLocal, if (odometerUnit == "mi") "gal" else "L")
                                    Text(
                                        text = "$formattedVolStr Cargados",
                                        color = MotoTextMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val pricePerLocalVol = if (volLocal > 0.0) log.cost / volLocal else 0.0
                                    val formattedPrice = pricePerLocalVol.toFormattedCurrency(currencySymbol)
                                    val displayOdoLocal = log.mileage.toMilOrKm(odometerUnit)
                                    Text(
                                        text = "${displayOdoLocal.toLocaleString()} $odometerUnit • $formattedPrice/${if (odometerUnit == "mi") "gal" else "L"}",
                                        color = MotoTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.cost.toFormattedCurrency(currencySymbol),
                                    color = MotoSuccess,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                IconButton(
                                    onClick = { viewModel.deleteFuelLog(log.id) },
                                    modifier = Modifier.size(32.dp).testTag("delete_fuel_log_${log.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar registro",
                                        tint = MotoDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHistorialTab(viewModel: MotoTrackerViewModel) {
    val history by viewModel.activeBikeHistory.collectAsState()
    val odometerUnit by viewModel.odometerUnit.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de Servicios",
                color = MotoTextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = MotoSecondaryBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MotoBorder)
            ) {
                Text(
                    text = "${history.size} registros",
                    color = MotoTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay registros de servicio aún.",
                    color = MotoTextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { log ->
                    Surface(
                        color = MotoCardBg,
                        border = BorderStroke(1.dp, MotoBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.date,
                                    color = MotoTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = log.cost.toFormattedCurrency(currencySymbol),
                                    color = MotoSuccess,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = log.typeName,
                                color = MotoTextMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Realizado a los ${log.mileage.toMilOrKm(odometerUnit).toLocaleString()} $odometerUnit",
                                color = MotoPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (log.notes.isNotEmpty()) {
                                HorizontalDivider(color = MotoBorder)
                                Text(
                                    text = log.notes,
                                    color = MotoTextMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardServicioTab(viewModel: MotoTrackerViewModel) {
    val activeBike by viewModel.activeBike.collectAsState()
    val intervals by viewModel.activeBikeIntervals.collectAsState()

    val odometerUnit by viewModel.odometerUnit.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var selectedIntervalId by remember { mutableStateOf("") }
    var mileageStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var costStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }

    // Prefill category index when intervals load
    LaunchedEffect(intervals) {
        if (intervals.isNotEmpty()) {
            selectedIntervalId = intervals.first().intervalId
        }
    }

    // Prefill odometer based on stored bike preferences and dynamic unit system
    LaunchedEffect(activeBike, odometerUnit) {
        if (mileageStr.isEmpty() && activeBike != null) {
            val convertedOdo = activeBike?.currentMileage?.toMilOrKm(odometerUnit) ?: 0
            mileageStr = convertedOdo.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Registrar Mantenimiento",
            color = MotoTextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MotoCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dropdown choosing interval type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TIPO DE SERVICIO",
                        color = MotoTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val currentSelectionName = intervals.find { it.intervalId == selectedIntervalId }?.name ?: "Seleccione"
                    Surface(
                        color = MotoInputBg,
                        border = BorderStroke(1.dp, MotoBorder),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentSelectionName, color = MotoTextMain, fontSize = 14.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MotoTextMuted)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MotoSecondaryBg)
                        ) {
                            intervals.forEach { interval ->
                                DropdownMenuItem(
                                    text = { Text(interval.name, color = MotoTextMain) },
                                    onClick = {
                                        selectedIntervalId = interval.intervalId
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = mileageStr,
                    onValueChange = { mileageStr = it },
                    label = { Text("Kilometraje al realizarlo ($odometerUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoPrimary,
                        unfocusedBorderColor = MotoBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_mileage_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Fecha") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("service_date_input")
                    )

                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Costo ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("service_cost_input")
                    )
                }

                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Notas / Repuestos") },
                    placeholder = { Text("Aceite, filtro K&N, guayas, etc...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoPrimary,
                        unfocusedBorderColor = MotoBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("service_notes_input")
                )
            }
        }

        Button(
            onClick = {
                val odoEntered = mileageStr.toIntOrNull() ?: 0
                val cost = costStr.toDoubleOrNull() ?: 0.0
                val catName = intervals.find { it.intervalId == selectedIntervalId }?.name ?: "Servicio"

                if (selectedIntervalId.isEmpty()) {
                    viewModel.showToast("Seleccione una categoría", isError = true)
                } else if (odoEntered <= 0) {
                    viewModel.showToast("Kilometraje inválido", isError = true)
                } else {
                    // Convert entered mileage back to metric km if user runs on miles system
                    val mileageInKmMetric = odoEntered.fromMilOrKm(odometerUnit)

                    viewModel.saveHistoryEntry(
                        bikeId = activeBike!!.id,
                        date = dateStr,
                        typeId = selectedIntervalId,
                        typeName = catName,
                        mileage = mileageInKmMetric,
                        cost = cost,
                        notes = notesStr,
                        onSuccess = {
                            costStr = ""
                            notesStr = ""
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MotoAccent, contentColor = MotoDarkBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_service_button")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Guardar Registro", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardAjustesTab(
    viewModel: MotoTrackerViewModel,
    onDeleted: () -> Unit
) {
    val activeBike by viewModel.activeBike.collectAsState()
    val intervals by viewModel.activeBikeIntervals.collectAsState()
    val odometerUnit by viewModel.odometerUnit.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val dailyAverageKm by viewModel.dailyAverageKm.collectAsState()

    val customStyles by viewModel.customStylesList.collectAsState()
    val defaultStyles = listOf(
        "sport" to "Deportiva / Naked",
        "scooter" to "Scooter / Urbana",
        "adventure" to "Doble Propósito",
        "cruiser" to "Chopper / Cruiser"
    )
    val allStyles = remember(customStyles) {
        val list = mutableListOf<Pair<String, String>>()
        list.addAll(defaultStyles)
        customStyles.forEach {
            list.add(it.id to it.name)
        }
        list
    }

    // Edit Profile State
    var isEditSectionExpanded by remember { mutableStateOf(false) }
    var editBrand by remember(activeBike) { mutableStateOf(activeBike?.brand ?: "") }
    var editModel by remember(activeBike) { mutableStateOf(activeBike?.model ?: "") }
    var editYear by remember(activeBike) { mutableStateOf(activeBike?.year?.toString() ?: "") }
    var editStyle by remember(activeBike) { mutableStateOf(activeBike?.style ?: "sport") }
    var editPhotoUrl by remember(activeBike) { mutableStateOf(activeBike?.photoUrl ?: "") }

    var showCreateStyleInAjustes by remember { mutableStateOf(false) }
    var customStyleName by remember { mutableStateOf("") }
    var customStyleIcon by remember { mutableStateOf("scrambler") }

    val context = LocalContext.current
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
            editPhotoUrl = uri.toString()
        }
    }

    // Threshold state holders
    var localIntervals by remember { mutableStateOf<List<ServiceIntervalEntity>>(emptyList()) }

    LaunchedEffect(intervals) {
        localIntervals = intervals
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (activeBike != null) {
            Text(
                text = "Mi Motocicleta Personal",
                color = MotoTextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MotoCardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier.fillMaxWidth().testTag("edit_bike_profile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditSectionExpanded = !isEditSectionExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = null,
                                tint = MotoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Editar Perfil de la Moto",
                                    color = MotoTextMain,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Actualiza marca, modelo, año, estilo o imagen personal",
                                    color = MotoTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isEditSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MotoTextMuted
                        )
                    }

                    AnimatedVisibility(
                        visible = isEditSectionExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HorizontalDivider(color = MotoBorder)

                            OutlinedTextField(
                                value = editBrand,
                                onValueChange = { editBrand = it },
                                label = { Text("Marca") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_brand_input")
                            )

                            OutlinedTextField(
                                value = editModel,
                                onValueChange = { editModel = it },
                                label = { Text("Modelo") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_model_input")
                            )

                            OutlinedTextField(
                                value = editYear,
                                onValueChange = { input -> 
                                    if (input.all { it.isDigit() }) editYear = input
                                },
                                label = { Text("Año") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_year_input")
                            )

                            // Photo editor section
                            Text("Imagen Personal de la Moto", color = MotoTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            if (editPhotoUrl.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MotoSecondaryBg)
                                        .border(1.dp, MotoBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = editPhotoUrl,
                                        contentDescription = "Foto de la moto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { editPhotoUrl = "" },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Button(
                                onClick = { editImagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MotoPrimary.copy(alpha = 0.15f),
                                    contentColor = MotoPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_pick_from_gallery_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Elegir de la Galería", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            OutlinedTextField(
                                value = editPhotoUrl,
                                onValueChange = { editPhotoUrl = it },
                                label = { Text("O introduce un Enlace URL (Opcional)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_photo_url_input")
                            )

                            // Style selector list
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Categoría / Estilo de Moto", color = MotoTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { showCreateStyleInAjustes = true },
                                    modifier = Modifier.testTag("create_style_button_ajustes")
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crear Categoría", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allStyles.forEach { (styleKey, styleName) ->
                                    val isActive = editStyle == styleKey
                                    Surface(
                                        color = if (isActive) MotoPrimary.copy(alpha = 0.08f) else MotoSecondaryBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isActive) MotoPrimary else MotoBorder
                                        ),
                                        onClick = { editStyle = styleKey },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_style_option_$styleKey")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val resolvedStyleIcon = getStyleDetails(styleKey, customStyles).second
                                            MotorcycleDrawing(
                                                style = resolvedStyleIcon,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = styleName,
                                                color = if (isActive) MotoPrimary else MotoTextMain,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val yearVal = editYear.toIntOrNull() ?: 2022
                                    if (editBrand.trim().isEmpty()) {
                                        viewModel.showToast("La marca es obligatoria", isError = true)
                                    } else if (editModel.trim().isEmpty()) {
                                        viewModel.showToast("El modelo es obligatorio", isError = true)
                                    } else if (yearVal < 1900 || yearVal > 2100) {
                                        viewModel.showToast("El año debe estar entre 1900 y 2100", isError = true)
                                    } else if (activeBike != null) {
                                        viewModel.updateBikeDetails(
                                            bikeId = activeBike!!.id,
                                            brand = editBrand,
                                            model = editModel,
                                            year = yearVal,
                                            style = editStyle,
                                            photoUrl = editPhotoUrl
                                        )
                                        isEditSectionExpanded = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotoPrimary, contentColor = MotoDarkBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("save_edit_bike_button")
                            ) {
                                Text("Guardar Cambios de la Moto", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MotoBorder)
        }

        Text(
            text = "Frecuencias de Mantenimiento",
            color = MotoTextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ajusta las frecuencias recomendadas para realizar el servicio a tu motocicleta.",
            color = MotoTextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MotoCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                localIntervals.forEachIndexed { index, interval ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(interval.name, color = MotoTextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Recomendado", color = MotoTextMuted, fontSize = 11.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayVal = interval.kmInterval.toMilOrKm(odometerUnit)
                            OutlinedTextField(
                                value = displayVal.toString(),
                                onValueChange = { updatedVal ->
                                    val num = updatedVal.toIntOrNull() ?: 100
                                    val convertedVal = num.fromMilOrKm(odometerUnit)
                                    localIntervals = localIntervals.mapIndexed { idx, entity ->
                                        if (idx == index) entity.copy(kmInterval = convertedVal) else entity
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoPrimary,
                                    unfocusedBorderColor = MotoBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .width(100.dp)
                                    .testTag("interval_input_${interval.intervalId}")
                             )
                            Text(odometerUnit, color = MotoTextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.updateBikeIntervals(activeBike!!.id, localIntervals)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MotoPrimary, contentColor = MotoDarkBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Frecuencias", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = MotoBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Preferencias de Localización (Unidades, Monedas, etc.)
        Text(
            text = "Configuración y Preferencias",
            color = MotoTextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Personaliza las unidades de medida, la divisa global y la estimación de uso diario de la app.",
            color = MotoTextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MotoCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unidad de distancia selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unidad del Odómetro", color = MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("km" to "Kilómetros (km)", "mi" to "Millas (mi)").forEach { (value, label) ->
                            val isSelected = odometerUnit == value
                            Surface(
                                color = if (isSelected) MotoPrimary.copy(alpha = 0.15f) else MotoSecondaryBg,
                                border = BorderStroke(1.dp, if (isSelected) MotoPrimary else MotoBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateOdometerUnit(value) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) MotoPrimary else MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Símbolo de Moneda selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Símbolo de Divisa", color = MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("$", "€", "£", "S/.", "MXN$").forEach { symbol ->
                            val isSelected = currencySymbol == symbol
                            Surface(
                                color = if (isSelected) MotoPrimary.copy(alpha = 0.15f) else MotoSecondaryBg,
                                border = BorderStroke(1.dp, if (isSelected) MotoPrimary else MotoBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateCurrencySymbol(symbol) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(symbol, color = if (isSelected) MotoPrimary else MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Daily Average Mileage
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Promedio de Recorrido Diario", color = MotoTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${dailyAverageKm.toInt()} $odometerUnit/día", color = MotoPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    var sliderValue by remember(dailyAverageKm) { mutableStateOf(dailyAverageKm) }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { viewModel.updateDailyAverageKm(sliderValue) },
                        valueRange = 5f..150f,
                        colors = SliderDefaults.colors(
                            thumbColor = MotoPrimary,
                            activeTrackColor = MotoPrimary,
                            inactiveTrackColor = MotoBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Esta estimación calcula con precisión las fechas para los recordatorios de tus próximos mantenimientos.",
                        color = MotoTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        HorizontalDivider(color = MotoBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Create Custom Service Category
        Text(
            text = "Crear Servicio Personalizado",
            color = MotoTextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        var customName by remember { mutableStateOf("") }
        var customFreq by remember { mutableStateOf("5000") }
        var customLastKm by remember { mutableStateOf("") }

        Card(
            colors = CardDefaults.cardColors(containerColor = MotoCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MotoBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Nombre del servicio") },
                    placeholder = { Text("Ej: Pastillas de freno, Guayas") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoPrimary,
                        unfocusedBorderColor = MotoBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_service_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customFreq,
                        onValueChange = { customFreq = it },
                        label = { Text("Frecuencia ($odometerUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_service_freq_input")
                    )

                    OutlinedTextField(
                        value = customLastKm,
                        onValueChange = { customLastKm = it },
                        label = { Text("Último hecho ($odometerUnit)") },
                        placeholder = { Text("Opcional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("custom_service_last_km_input")
                    )
                }
            }
        }

        Button(
            onClick = {
                val inputFreq = customFreq.toIntOrNull() ?: 5000
                val inputLastKm = customLastKm.toIntOrNull() ?: (activeBike?.currentMileage?.toMilOrKm(odometerUnit) ?: 0)
                
                val freqNum = inputFreq.fromMilOrKm(odometerUnit)
                val lastKmNum = inputLastKm.fromMilOrKm(odometerUnit)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                viewModel.addCustomInterval(
                    bikeId = activeBike!!.id,
                    name = customName,
                    frequency = freqNum,
                    lastKm = lastKmNum,
                    lastDate = todayStr,
                    onSuccess = {
                        customName = ""
                        customLastKm = ""
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MotoAccent, contentColor = MotoDarkBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Servicio", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = MotoBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Danger Delete zone
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Zona de Peligro",
                color = MotoDanger,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    viewModel.deleteBike(activeBike!!.id, onDeleted)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MotoDanger
                ),
                border = BorderStroke(1.dp, MotoDanger),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_bike_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Eliminar esta Motocicleta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCreateStyleInAjustes) {
        Dialog(onDismissRequest = { showCreateStyleInAjustes = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoSecondaryBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MotoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("create_custom_style_dialog_ajustes")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Nueva Categoría",
                        color = MotoTextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Crea un tipo o estilo de motocicleta personalizado.",
                        color = MotoTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = customStyleName,
                        onValueChange = { customStyleName = it },
                        label = { Text("Nombre de la Categoría") },
                        placeholder = { Text("Ej: Cruiser, Custom, Trial, Enduro") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoPrimary,
                            unfocusedBorderColor = MotoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_style_name_input_ajustes")
                    )

                    Text(
                        text = "Selecciona una Plantilla de Ícono:",
                        color = MotoTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val iconTemplates = listOf(
                        "scrambler" to "Scrambler Classic",
                        "cafe_racer" to "Cafe Racer Hump",
                        "tracker" to "Flat Tracker Plate",
                        "sport" to "Sport Naked",
                        "scooter" to "Urban Scooter",
                        "adventure" to "Adventure ADV",
                        "cruiser" to "Low Cruiser"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        iconTemplates.forEach { (key, label) ->
                            val isChosen = customStyleIcon == key
                            Surface(
                                color = if (isChosen) MotoPrimary.copy(alpha = 0.12f) else MotoCardBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChosen) MotoPrimary else MotoBorder),
                                onClick = { customStyleIcon = key },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MotorcycleDrawing(
                                        style = key,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = MotoTextMain,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreateStyleInAjustes = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MotoTextMain
                            ),
                            border = BorderStroke(1.dp, MotoBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (customStyleName.trim().isEmpty()) {
                                    viewModel.showToast("El nombre del tipo es obligatorio", isError = true)
                                } else {
                                    viewModel.addCustomStyle(customStyleName, customStyleIcon) {
                                        showCreateStyleInAjustes = false
                                        editStyle = customStyleName.trim()
                                        customStyleName = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MotoPrimary,
                                contentColor = MotoDarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Locale-aware formatting helper
fun Int.toLocaleString(): String {
    return String.format("%,d", this).replace(',', '.')
}

// Distance, Volume and Currency formatting helpers
fun Int.toMilOrKm(unit: String): Int {
    return if (unit == "mi") (this * 0.621371).toInt() else this
}

fun Double.toMilOrKm(unit: String): Double {
    return if (unit == "mi") this * 0.621371 else this
}

fun Int.fromMilOrKm(unit: String): Int {
    return if (unit == "mi") (this / 0.621371).toInt() else this
}

fun Double.toFormattedCurrency(symbol: String): String {
    return String.format(Locale.getDefault(), "%s %,.2f", symbol, this)
}

fun Double.toFormattedLiters(unit: String): String {
    return if (unit == "mi") {
        // Liters to Gallons conversion: 1 Liter = 0.264172 Gallons
        val gallons = this * 0.264172
        String.format(Locale.getDefault(), "%,.1f gal", gallons)
    } else {
        String.format(Locale.getDefault(), "%,.1f L", this)
    }
}

@Composable
fun CustomAnalyticsCard(
    totalExpenses: Double,
    totalFuelCost: Double,
    totalMaintCost: Double,
    costPerUnit: Double,
    odoUnitStr: String,
    currencySymbol: String,
    averageFuelEconomyStr: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onExportReportClick: () -> Unit
) {
    Surface(
        color = MotoCardBg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MotoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MotoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Inversión y Estadísticas",
                            color = MotoTextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Historial acumulado de costos y eficiencia",
                            color = MotoTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MotoTextMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MotoBorder)

                    // Stats row 1: Mantenimiento y Nafta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MotoSecondaryBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MANTENIMIENTO", color = MotoTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(totalMaintCost.toFormattedCurrency(currencySymbol), color = MotoTextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = MotoSecondaryBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("COMBUSTIBLE", color = MotoTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(totalFuelCost.toFormattedCurrency(currencySymbol), color = MotoTextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Stats row 2: Gasto Total e Inserción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MotoSecondaryBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("COSTO GLOBAL", color = MotoPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(totalExpenses.toFormattedCurrency(currencySymbol), color = MotoTextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Surface(
                            color = MotoSecondaryBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("COSTO POR ${odoUnitStr.uppercase()}", color = MotoTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${costPerUnit.toFormattedCurrency(currencySymbol)}/$odoUnitStr", color = MotoTextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Rendimiento promedio de combustible
                    Surface(
                        color = MotoSecondaryBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("RENDIMIENTO COMBUSTIBLE", color = MotoSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(averageFuelEconomyStr, color = MotoTextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            // Share Action Button
                            Button(
                                onClick = onExportReportClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MotoPrimary.copy(alpha = 0.15f),
                                    contentColor = MotoPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
