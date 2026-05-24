package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
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
                        val histories by viewModel.activeBikeHistory.collectAsState()
                        // Filter for this specific bike
                        val bikeHistories = histories.filter { it.bikeId == bike.id }
                        
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
                                    // Custom avatar motor drawing
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MotoSecondaryBg)
                                            .border(1.dp, MotoBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MotorcycleDrawing(
                                            style = bike.style,
                                            modifier = Modifier.size(60.dp)
                                        )
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
                                        Text(
                                            text = "${bike.year} • Ref: ${bike.style.uppercase()}",
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

    val styles = listOf(
        "sport" to "Deportiva / Naked",
        "scooter" to "Scooter / Urbana",
        "adventure" to "Doble Propósito",
        "cruiser" to "Chopper / Cruiser"
    )
    var selectedStyle by remember { mutableStateOf("sport") }
    var photoUrl by remember { mutableStateOf("") }

    var lastKm by remember { mutableStateOf("") }
    var lastDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

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
                            onValueChange = { year = it },
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
                            onValueChange = { odo = it },
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

            // Style section
            Text(
                text = "ESTILO DE LA MOTO",
                color = MotoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                styles.forEach { (style, name) ->
                    val isActive = selectedStyle == style
                    Surface(
                        color = if (isActive) MotoPrimary.copy(alpha = 0.08f) else MotoCardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) MotoPrimary else MotoBorder
                        ),
                        onClick = { selectedStyle = style },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_option_$style")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MotorcycleDrawing(
                                style = style,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = name,
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
                        onValueChange = { lastKm = it },
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
                    val odoVal = odo.toIntOrNull() ?: 0
                    val lastKmVal = lastKm.toIntOrNull() ?: odoVal
                    val yearVal = year.toIntOrNull() ?: 2022

                    if (brand.trim().isEmpty() || model.trim().isEmpty()) {
                        viewModel.showToast("Marca y Modelo son obligatorios", isError = true)
                    } else if (lastKmVal > odoVal) {
                        viewModel.showToast("El último servicio no puede ser mayor al odómetro actual", isError = true)
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
                        Text(
                            text = activeBike?.let { "${it.year} • ${it.style.uppercase()}" } ?: "",
                            color = MotoTextMuted,
                            fontSize = 11.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Mileage Card
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
                            Text(
                                text = activeBike?.currentMileage?.toLocaleString() ?: "0",
                                color = MotoTextMain,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "km",
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
                                        Text(
                                            text = if (remaining >= 0) remaining.toLocaleString() else "-${Math.abs(remaining).toLocaleString()}",
                                            color = MotoTextMain,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "km",
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
                                        text = "Último: ${lastKm.toLocaleString()} km",
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
            // Standard List mode view
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

                val statusText = when {
                    isOverdue -> "Vencido por ${Math.abs(remaining).toLocaleString()} km"
                    remaining <= 500 -> "Próximo en ${remaining.toLocaleString()} km"
                    else -> "Faltan ${remaining.toLocaleString()} km"
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
                            Text(
                                text = "Último: ${lastKm.toLocaleString()} km",
                                color = MotoTextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Frecuencia: ${interval.kmInterval.toLocaleString()} km",
                                color = MotoTextMuted,
                                fontSize = 12.sp
                            )
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
                                    text = "$${log.cost.toInt().toLocaleString()}",
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
                                text = "Realizado a los ${log.mileage.toLocaleString()} km",
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

    var selectedIntervalId by remember { mutableStateOf("") }
    var mileageStr by remember { mutableStateOf(activeBike?.currentMileage?.toString() ?: "") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var costStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }

    // Prefill category index when intervals load
    LaunchedEffect(intervals) {
        if (intervals.isNotEmpty()) {
            selectedIntervalId = intervals.first().intervalId
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
                    label = { Text("Kilometraje al realizarlo (km)") },
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
                        label = { Text("Costo ($)") },
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
                val mil = mileageStr.toIntOrNull() ?: 0
                val cost = costStr.toDoubleOrNull() ?: 0.0
                val catName = intervals.find { it.intervalId == selectedIntervalId }?.name ?: "Servicio"

                if (selectedIntervalId.isEmpty()) {
                    viewModel.showToast("Seleccione una categoría", isError = true)
                } else {
                    viewModel.saveHistoryEntry(
                        bikeId = activeBike!!.id,
                        date = dateStr,
                        typeId = selectedIntervalId,
                        typeName = catName,
                        mileage = mil,
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
        Text(
            text = "Frecuencias de Mantenimiento",
            color = MotoTextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ajusta cada cuántos kilómetros debes realizar el servicio para esta moto.",
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
                            OutlinedTextField(
                                value = interval.kmInterval.toString(),
                                onValueChange = { updatedVal ->
                                    val num = updatedVal.toIntOrNull() ?: 100
                                    localIntervals = localIntervals.mapIndexed { idx, entity ->
                                        if (idx == index) entity.copy(kmInterval = num) else entity
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
                                    .width(90.dp)
                                    .testTag("interval_input_${interval.intervalId}")
                            )
                            Text("km", color = MotoTextMuted, fontSize = 13.sp)
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
                        label = { Text("Frecuencia (km)") },
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
                        label = { Text("Último hecho (km)") },
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
                val freqNum = customFreq.toIntOrNull() ?: 5000
                val lastKmNum = customLastKm.toIntOrNull() ?: (activeBike?.currentMileage ?: 0)
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
}

// Locale-aware formatting helper
fun Int.toLocaleString(): String {
    return String.format("%,d", this).replace(',', '.')
}
