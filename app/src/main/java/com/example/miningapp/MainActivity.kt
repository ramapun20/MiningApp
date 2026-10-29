package com.example.miningapp

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.example.miningapp.data.repository.DemoKanzRepository
import com.example.miningapp.domain.model.ChatMessage
import com.example.miningapp.domain.model.MapPlace
import com.example.miningapp.domain.model.MapPlaceType
import com.example.miningapp.domain.model.Product
import com.example.miningapp.domain.model.ProductCategory
import com.example.miningapp.domain.usecase.GetMapPlaces
import com.example.miningapp.domain.usecase.GetProducts
import com.example.miningapp.domain.usecase.PhoneInputValidation
import com.example.miningapp.data.auth.FirebasePhoneAuthRepository
import com.example.miningapp.data.auth.loadCurrentUserProfile
import com.example.miningapp.data.auth.listenToAdminChat
import com.example.miningapp.data.auth.deleteAdminChatMessage
import com.example.miningapp.data.auth.saveCurrentUserToFirestore
import com.example.miningapp.data.auth.sendAdminChatMessage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import com.example.miningapp.ui.theme.MiningAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val Teal = Color(0xFF009999)
private val Ink = Color(0xFF202627)
private val Muted = Color(0xFF737C7D)
private val KanzDuotone = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0.18f, 0.35f, 0.07f, 0f, 0f,
            0.18f, 0.35f, 0.07f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.isDeviceLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

private fun phoneAuthFailureMessage(error: Exception): String {
    if (error is FirebaseNetworkException) {
        return "Firebase could not connect. Check your internet connection, then try again."
    }

    val errorCode = (error as? FirebaseAuthException)?.errorCode
    val errorDetails = generateSequence(error as Throwable?) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
    if ("BILLING_NOT_ENABLED" in errorDetails) {
        return "Firebase billing is not enabled for this project. Enable billing for the Firebase project to send real verification SMS."
    }

    val detail = errorDetails
        ?.replace(Regex("\\+?[0-9][0-9\\s().-]{6,}"), "[number]")
        ?.replace(Regex("\\s+"), " ")
        ?.take(180)
    return when (errorCode) {
        "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER" -> "Enter a valid phone number."
        "ERROR_INVALID_VERIFICATION_CODE" -> "That code is incorrect. Check the SMS and try again."
        "ERROR_SESSION_EXPIRED" -> "That code has expired. Request a new one."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Firebase could not connect. Check your internet connection, then try again."
        "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" ->
            "Too many verification attempts or the Firebase SMS quota was reached. Wait and check Firebase Authentication quotas."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Phone sign-in is not enabled in Firebase."
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_MISSING_CLIENT_IDENTIFIER" ->
            "Firebase rejected this app. Add this app's SHA-1 and SHA-256 signing fingerprints in Firebase Project settings, then reinstall the app."
        "ERROR_CAPTCHA_CHECK_FAILED", "ERROR_INVALID_APP_CREDENTIAL" ->
            "Firebase app verification failed. Check the SHA fingerprints and Google Play services, then try on a physical device."
        else -> if (errorCode != null) {
            "Firebase phone verification failed ($errorCode)${detail?.let { ": $it" } ?: ""}. Check Phone sign-in, SMS region policy, SHA fingerprints, and quotas."
        } else {
            "Firebase phone verification failed (${error.javaClass.simpleName})${detail?.let { ": $it" } ?: ""}. Check network access, Google Play services, and Firebase Phone sign-in settings."
        }
    }
}

private fun productImage(product: Product): Int = when (product.imageName) {
    "metal_detector" -> R.drawable.metal_detector
    "jack_hammer" -> R.drawable.jack_hammer
    "gold_wash_plant" -> R.drawable.gold_wash_plant
    "car_icon" -> R.drawable.car_icon
    "jobs_icon" -> R.drawable.jobs_icon
    else -> R.drawable.kanz_logo
}

private enum class Page {
   Welcome, IdentityIntro, Phone, Verification, Location, Consent,
   Marketplace, ProductDetail, Map, Navigation, Profile, Category, ChatThread
}

private fun chatFailureMessage(action: String, error: Exception): String {
    val detail = error.localizedMessage?.takeIf(String::isNotBlank)
    return when ((error as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore denied the attempt to $action this chat. This app stores messages at `supportChats/{userId}/messages`; publish rules for that exact path, including the owner-only delete rule, then retry. ${detail.orEmpty()}"
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Your sign-in expired. Sign in again to use admin chat. ${detail.orEmpty()}"
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "Firestore is unavailable right now. Check internet access and try again. ${detail.orEmpty()}"
        else ->
            "Unable to $action your admin chat${detail?.let { ": $it" } ?: ". Check Firebase setup and internet access."}"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiningAppTheme {
                MiningApp()
            }
        }
    }
}

@Composable
private fun MiningApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val authRepository = remember(context) {
        FirebaseApp.getApps(context)
            .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?.let { app -> FirebasePhoneAuthRepository(FirebaseAuth.getInstance(app)) }
    }
    val repository = remember { DemoKanzRepository() }
    val products = remember(repository) { GetProducts(repository)() }
    val mapPlaces = remember(repository) { GetMapPlaces(repository)() }
    var page by rememberSaveable {
        mutableStateOf(Page.Welcome)
    }
    var chatReturnPage by rememberSaveable { mutableStateOf(Page.Marketplace) }
    var isCreatingAccount by rememberSaveable { mutableStateOf(false) }
    var previousPage by rememberSaveable { mutableStateOf(Page.Marketplace) }
    var selectedListing by rememberSaveable { mutableIntStateOf(0) }
    var selectedCategory by rememberSaveable { mutableStateOf("Marketplace") }
    var selectedMarketCategory by rememberSaveable { mutableStateOf("All") }
    var selectedMapFilter by rememberSaveable { mutableStateOf("All") }
    var chatMessages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var phone by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var profileImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCountryCode by rememberSaveable { mutableStateOf("+977") }
    var code by rememberSaveable { mutableStateOf("") }
    var verificationCodeSent by rememberSaveable { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authLoading by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }
    var isDeviceLocationEnabled by remember {
        mutableStateOf(context.isDeviceLocationEnabled())
    }
    var locationMessage by remember {
        mutableStateOf<String?>(
            if (context.hasLocationPermission()) "Waiting for your current location..."
            else "Enable location access to show your live position."
        )
    }
    var returnToMapAfterPermission by rememberSaveable { mutableStateOf(false) }
    val signedInUserId = authRepository?.currentUserId

    DisposableEffect(page, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDeviceLocationEnabled = context.isDeviceLocationEnabled()
                hasLocationPermission = context.hasLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(page, signedInUserId) {
        if (page != Page.ChatThread) {
            onDispose { }
        } else if (signedInUserId == null) {
            chatMessages = emptyList()
            chatError = "Sign in to your account to chat with the admin."
            onDispose { }
        } else {
            chatMessages = emptyList()
            chatError = null
            val registration = try {
                listenToAdminChat(
                    userId = signedInUserId,
                    onMessages = {
                        chatMessages = it
                        chatError = null
                    },
                    onFailure = { error ->
                        Log.e("MiningAppChat", "Unable to load the account's admin chat.", error)
                        chatError = chatFailureMessage("load", error)
                    }
                )
            } catch (error: Exception) {
                Log.e("MiningAppChat", "Unable to open the account's admin chat.", error)
                chatError = "Unable to open your admin chat. Please sign in again."
                null
            }
            onDispose { registration?.remove() }
        }
    }

    fun completePhoneSignIn() {
        authLoading = false
        page = Page.Marketplace
        if (isCreatingAccount) {
            fullName = fullName.trim()
            email = email.trim()
        } else {
            fullName = ""
            email = ""
            loadCurrentUserProfile(
                onSuccess = { savedName, savedEmail ->
                    fullName = savedName.orEmpty()
                    email = savedEmail.orEmpty()
                },
                onFailure = { error ->
                    Log.e("MiningAppAuth", "Failed to load the signed-in user's profile.", error)
                    Toast.makeText(
                        context,
                        "Signed in, but your profile details could not be loaded. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
        saveCurrentUserToFirestore(
            fullName = fullName.takeIf { isCreatingAccount },
            email = email.takeIf { isCreatingAccount },
            onSuccess = {
                Log.d("MiningAppAuth", "User profile saved to Firestore.")
            },
            onFailure = { error ->
                Log.e("MiningAppAuth", "Failed to save user profile to Firestore.", error)
                Toast.makeText(
                    context,
                    "Signed in, but your profile could not be saved. Check your connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    fun finishPhoneSignIn(credential: com.google.firebase.auth.PhoneAuthCredential) {
        val auth = authRepository ?: run {
            authError = "Firebase phone authentication is not configured."
            return
        }
        authLoading = true
        authError = null
        auth.signInWithCredential(
            credential = credential,
            onSuccess = { _ ->
                completePhoneSignIn()
            },
            onFailure = { error ->
                authLoading = false
                authError = phoneAuthFailureMessage(error)
            }
        )
    }

    fun requestPhoneCode(forceResend: Boolean = false) {
        if (isCreatingAccount) {
            if (fullName.isBlank()) {
                authError = "Enter your full name."
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                authError = "Enter a valid email address."
                return
            }
        }
        val phoneInput = phone.trim()
        val phoneWithCountryCode = if (phoneInput.startsWith("+")) {
            phoneInput
        } else {
            selectedCountryCode + phoneInput.filter(Char::isDigit)
        }
        val normalizedPhone = PhoneInputValidation.normalizePhoneNumber(phoneWithCountryCode)
        if (normalizedPhone == null) {
            authError = "Enter a valid phone number."
            return
        }
        val auth = authRepository
        if (auth == null) {
            authError = "Firebase phone authentication is not configured. Add a matching google-services.json."
            return
        }
        val currentActivity = activity
        if (currentActivity == null) {
            authError = "Unable to start phone verification right now."
            return
        }

        phone = normalizedPhone
        authLoading = true
        authError = null
        auth.requestCode(
            activity = currentActivity,
            phoneNumber = normalizedPhone,
            forceResend = forceResend,
            onCodeSent = {
                authLoading = false
                code = ""
                verificationCodeSent = true
                if (!isCreatingAccount && !forceResend) page = Page.Verification
            },
            onAutoVerified = ::finishPhoneSignIn,
            onFailure = { error ->
                authLoading = false
                authError = phoneAuthFailureMessage(error)
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = context.hasLocationPermission()
        if (hasLocationPermission) {
            locationMessage = "Waiting for your current location..."
            page = if (returnToMapAfterPermission) {
                returnToMapAfterPermission = false
                isDeviceLocationEnabled = context.isDeviceLocationEnabled()
                if (isDeviceLocationEnabled) Page.Map else Page.Location
            } else {
                Page.Consent
            }
        } else {
            locationMessage = if (permissions.values.any { it }) {
                "Location permission is required to use this feature."
            } else {
                "Location permission denied. Enable it in app settings to use live location."
            }
        }
    }

    val shouldTrackLocation = page == Page.Map || page == Page.Navigation
    DisposableEffect(hasLocationPermission, shouldTrackLocation, context, lifecycleOwner) {
        if (!hasLocationPermission || !shouldTrackLocation) {
            onDispose { }
        } else {
            val client = LocationServices.getFusedLocationProviderClient(context)
            var trackingActive = false
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (!trackingActive) return
                    if (!context.hasLocationPermission()) {
                        hasLocationPermission = false
                        currentLocation = null
                        locationMessage = "Location permission is required to use this feature."
                        return
                    }
                    result.lastLocation?.let {
                        currentLocation = it
                        locationMessage = null
                    }
                }
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateDistanceMeters(3f)
                .build()
            var updatesStarted = false

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (!context.hasLocationPermission()) {
                            trackingActive = false
                            hasLocationPermission = false
                            currentLocation = null
                            locationMessage = "Location permission is required to use this feature."
                        } else if (!updatesStarted) {
                            try {
                                trackingActive = true
                                client.lastLocation
                                    .addOnSuccessListener {
                                        if (trackingActive && context.hasLocationPermission()) {
                                            it?.let { fix -> currentLocation = fix }
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        if (trackingActive) {
                                            locationMessage = "Unable to get last location: ${error.localizedMessage ?: "location service error"}"
                                        }
                                    }
                                client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
                                    .addOnSuccessListener {
                                        if (
                                            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                                            (page == Page.Map || page == Page.Navigation)
                                        ) {
                                            updatesStarted = true
                                            if (currentLocation == null) {
                                                locationMessage = "Waiting for your current location..."
                                            }
                                        } else {
                                            trackingActive = false
                                            client.removeLocationUpdates(callback)
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        client.removeLocationUpdates(callback)
                                        trackingActive = false
                                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                            locationMessage = "Unable to start location updates: ${error.localizedMessage ?: "location service error"}"
                                        }
                                    }
                            } catch (_: SecurityException) {
                                trackingActive = false
                                hasLocationPermission = false
                                currentLocation = null
                                locationMessage = "Location permission is required to use this feature."
                            }
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        trackingActive = false
                        client.removeLocationUpdates(callback)
                        updatesStarted = false
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                observer.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START)
            }
            onDispose {
                trackingActive = false
                lifecycleOwner.lifecycle.removeObserver(observer)
                client.removeLocationUpdates(callback)
            }
        }
    }

    val mainPage = page == Page.Marketplace || page == Page.Map || page == Page.Navigation ||
        page == Page.Profile || page == Page.Category || page == Page.ChatThread
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (mainPage) {
                BottomNavigation(
                    selected = when (page) {
                        Page.Map -> "Maps"
                        Page.Profile -> "Profile"
                        Page.Navigation -> "Maps"
                        Page.ChatThread -> "Chats"
                        else -> "Home"
                    },
                    onSelect = {
                        page = when (it) {
                            "Maps" -> {
                                returnToMapAfterPermission = true
                                isDeviceLocationEnabled = context.isDeviceLocationEnabled()
                                Page.Location
                            }
                            "Profile" -> Page.Profile
                            "Chats" -> if (authRepository?.isSignedIn == true) {
                                chatReturnPage = page.takeUnless { it == Page.ChatThread } ?: Page.Marketplace
                                Page.ChatThread
                            } else {
                                isCreatingAccount = false
                                authError = "Sign in to your account to chat with the admin."
                                Page.Phone
                            }
                            "Legal" -> {
                                previousPage = page
                                selectedCategory = it
                                Page.Category
                            }
                            else -> Page.Marketplace
                        }
                    }
                )
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            when (page) {
                Page.Welcome -> WelcomeScreen(
                    onGetStarted = { countryCode ->
                        selectedCountryCode = countryCode
                        authError = null
                        fullName = ""
                        email = ""
                        phone = ""
                        code = ""
                        verificationCodeSent = false
                        isCreatingAccount = true
                        page = Page.Phone
                    },
                    onLogin = { countryCode ->
                        selectedCountryCode = countryCode
                        authError = null
                        fullName = ""
                        email = ""
                        isCreatingAccount = false
                        page = Page.IdentityIntro
                    }
                )
                Page.IdentityIntro -> IdentityIntroScreen(
                    onBack = { page = Page.Welcome },
                    onContinue = { page = Page.Phone }
                )
                Page.Phone -> if (isCreatingAccount) {
                    CreateAccountScreen(
                            fullName = fullName,
                            onFullNameChange = {
                                fullName = it
                                authError = null
                            },
                            email = email,
                            onEmailChange = {
                                email = it
                                authError = null
                            },
                            phone = phone,
                            phoneCountryCode = selectedCountryCode,
                            onPhoneChange = {
                                phone = it
                                code = ""
                                verificationCodeSent = false
                                authError = null
                            },
                            code = code,
                            onCodeChange = {
                                code = it.filter(Char::isDigit).take(6)
                                authError = null
                            },
                            verificationCodeSent = verificationCodeSent,
                            errorText = authError,
                            isLoading = authLoading,
                            onBack = { page = Page.Welcome },
                            onRequestCode = { requestPhoneCode() },
                            onResendCode = { requestPhoneCode(forceResend = true) },
                            onCreateAccount = {
                                authError = PhoneInputValidation.verificationCodeError(code)
                                if (authError == null) {
                                    val auth = authRepository
                                    if (auth == null) {
                                        authError = "Firebase phone authentication is not configured."
                                    } else {
                                        authLoading = true
                                        auth.verifyCode(
                                            code = code,
                                            onSuccess = { _ -> completePhoneSignIn() },
                                            onFailure = { error ->
                                                authLoading = false
                                                authError = phoneAuthFailureMessage(error)
                                            }
                                        )
                                    }
                                }
                            }
                    )
                } else FormScreen(
                    title = "Sign in",
                    question = "Enter your phone number",
                    hint = "Phone number",
                    value = phone,
                    onValueChange = {
                        phone = it
                        authError = null
                    },
                    keyboardType = KeyboardType.Phone,
                    onBack = { page = Page.Welcome },
                    onContinue = { requestPhoneCode() },
                    supportingText = null,
                    errorText = authError,
                    isLoading = authLoading,
                    continueLabel = "Send verification code"
                )
                Page.Verification -> FormScreen(
                    title = "Verify phone",
                    question = "Enter your verification code",
                    hint = "6-digit code",
                    value = code,
                    onValueChange = {
                        code = it.filter { character -> character in '0'..'9' }.take(6)
                        authError = null
                    },
                    keyboardType = KeyboardType.Number,
                    onBack = { page = Page.Phone },
                    onContinue = {
                        authError = PhoneInputValidation.verificationCodeError(code)
                        if (authError == null) {
                            val auth = authRepository
                            if (auth == null) {
                                authError = "Firebase phone authentication is not configured."
                            } else {
                                authLoading = true
                                auth.verifyCode(
                                    code = code,
                                    onSuccess = { _ ->
                                        completePhoneSignIn()
                                    },
                                    onFailure = { error ->
                                        authLoading = false
                                        authError = phoneAuthFailureMessage(error)
                                    }
                                )
                            }
                        }
                    },
                    supportingText = "Enter the SMS code sent to $phone.",
                    errorText = authError,
                    isLoading = authLoading,
                    onResend = { requestPhoneCode(forceResend = true) },
                    continueLabel = "Verify"
                )
                Page.Location -> LocationScreen(
                    onBack = {
                        val backPage = if (returnToMapAfterPermission) Page.Marketplace else Page.Welcome
                        returnToMapAfterPermission = false
                        page = backPage
                    },
                    hasLocationPermission = hasLocationPermission,
                    isDeviceLocationEnabled = isDeviceLocationEnabled,
                    isMapRequest = returnToMapAfterPermission,
                    onEnableLocation = {
                        if (!context.hasLocationPermission()) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenLocationSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (error: ActivityNotFoundException) {
                            Log.e("MiningAppLocation", "Unable to open device location settings.", error)
                            locationMessage = "Open your device Settings and turn on Location."
                        }
                    },
                    onContinue = {
                        if (context.hasLocationPermission()) {
                            hasLocationPermission = true
                            isDeviceLocationEnabled = context.isDeviceLocationEnabled()
                            locationMessage = "Waiting for your current location..."
                            if (returnToMapAfterPermission && isDeviceLocationEnabled) {
                                returnToMapAfterPermission = false
                                page = Page.Map
                            } else if (returnToMapAfterPermission) {
                                locationMessage = "Turn on your device's Location setting to show your location on the map."
                            } else {
                                page = Page.Consent
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    location = currentLocation,
                    locationMessage = locationMessage
                )
                Page.Consent -> ConsentScreen(
                    onConfirm = { page = Page.Marketplace },
                    onDismiss = { page = Page.Location },
                    onEnableLocation = {
                        returnToMapAfterPermission = false
                        if (context.hasLocationPermission()) {
                            hasLocationPermission = true
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
                Page.Marketplace -> MarketplaceScreen(
                    products = products,
                    selectedCategory = selectedMarketCategory,
                    onOpenListing = {
                        previousPage = Page.Marketplace
                        selectedListing = it
                        page = Page.ProductDetail
                    },
                    onOpenNavigation = { page = Page.Navigation },
                    onCategory = { selectedMarketCategory = it }
                )
                Page.ProductDetail -> ProductDetailScreen(
                    product = products[selectedListing],
                    onBack = { page = previousPage },
                    onInquire = {
                        val userId = signedInUserId
                        if (userId != null) {
                            chatError = null
                            chatReturnPage = Page.ProductDetail
                            page = Page.ChatThread
                            sendAdminChatMessage(
                                userId = userId,
                                senderName = fullName,
                                text = "I'm interested in ${products[selectedListing].title} (${products[selectedListing].detail}), listed at ${products[selectedListing].price}. Please share more information.",
                                onSuccess = {},
                                onFailure = { error ->
                                    Log.e("MiningAppChat", "Unable to send the product inquiry to the admin.", error)
                                    chatError = "Your product inquiry could not be sent. Check your connection and try again."
                                }
                            )
                        } else {
                            isCreatingAccount = false
                            authError = "Sign in to your account to chat with the admin."
                            page = Page.Phone
                        }
                    }
                )
                Page.Map -> MapScreen(
                    onBack = { page = Page.Marketplace },
                    onOpenNavigation = { page = Page.Navigation },
                    location = currentLocation,
                    locationMessage = locationMessage,
                    hasLocationPermission = hasLocationPermission,
                    places = mapPlaces,
                    selectedFilter = selectedMapFilter,
                    onRequestLocation = {
                        if (context.hasLocationPermission()) {
                            hasLocationPermission = true
                        } else {
                            returnToMapAfterPermission = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onCategory = { selectedMapFilter = it }
                )
                Page.Profile -> ProfileScreen(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    profileImageUri = profileImageUri,
                    onProfileImageSelected = { profileImageUri = it },
                    onSignOut = {
                        authRepository?.signOut()
                        phone = ""
                        fullName = ""
                        email = ""
                        code = ""
                        authError = null
                        page = Page.Welcome
                    },
                    onCategory = {
                        previousPage = Page.Profile
                        selectedCategory = it
                        page = Page.Category
                    }
                )
                Page.Navigation -> NavigationScreen(
                    onBack = { page = Page.Map },
                    location = currentLocation,
                    locationMessage = locationMessage,
                    hasLocationPermission = hasLocationPermission,
                    places = mapPlaces
                )
                Page.Category -> CategoryScreen(
                    title = selectedCategory,
                    onBack = { page = previousPage },
                    onOpenNavigation = { page = Page.Navigation },
                    onCategory = { selectedCategory = it },
                    onOpenListing = {
                        previousPage = Page.Category
                        selectedListing = it
                        page = Page.ProductDetail
                    },
                    products = products
                )
                Page.ChatThread -> ChatThreadScreen(
                    messages = chatMessages,
                    errorText = chatError,
                    onBack = { page = chatReturnPage },
                    onSend = { text, clearDraft ->
                        val userId = signedInUserId
                        if (userId == null) {
                            chatError = "Sign in to your account to chat with the admin."
                        } else {
                            sendAdminChatMessage(
                                userId = userId,
                                senderName = fullName,
                                text = text,
                                onSuccess = {
                                    chatError = null
                                    clearDraft()
                                },
                                onFailure = { error ->
                                    Log.e("MiningAppChat", "Unable to send the admin chat message.", error)
                                    chatError = chatFailureMessage("send", error)
                                }
                            )
                        }
                    },
                    onDeleteMessage = { message ->
                        val userId = signedInUserId
                        val messageId = message.documentId
                        if (userId == null || messageId == null) {
                            chatError = "This message cannot be deleted. Sign in again and retry."
                        } else {
                            deleteAdminChatMessage(
                                userId = userId,
                                messageId = messageId,
                                onSuccess = { chatError = null },
                                onFailure = { error ->
                                    Log.e("MiningAppChat", "Unable to delete the chat message.", error)
                                    chatError = chatFailureMessage("delete", error)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    onGetStarted: (String) -> Unit,
    onLogin: (String) -> Unit
) {
    val countries = listOf(
        CountryOption("Nepal", "🇳🇵", "+977"),
        CountryOption("Mauritania", "🇲🇷", "+222"),
        CountryOption("South Korea", "🇰🇷", "+82"),
        CountryOption("United States", "🇺🇸", "+1"),
        CountryOption("India", "🇮🇳", "+91"),
        CountryOption("United Arab Emirates", "🇦🇪", "+971")
    )
    var selectedCountry by rememberSaveable { mutableStateOf(countries.first().name) }
    var selectedCountryCode by rememberSaveable { mutableStateOf("+977") }
    var countryMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.kanz_logo),
            contentDescription = "The Mining logo",
            modifier = Modifier.size(150.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            colorFilter = KanzDuotone
        )
        Spacer(Modifier.height(8.dp))
        Text("Start Mining Smarter", fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color.Black)
        Text(
            "Your all in one Mining App",
            fontSize = 14.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Box {
            Text(
                text = "${countries.first { it.name == selectedCountry }.flag}  $selectedCountry   ⌄",
                modifier = Modifier.clickable { countryMenuExpanded = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Ink
            )
            DropdownMenu(
                expanded = countryMenuExpanded,
                onDismissRequest = { countryMenuExpanded = false }
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.flag}  ${country.name} (${country.phoneCode})") },
                        onClick = {
                            selectedCountry = country.name
                            selectedCountryCode = country.phoneCode
                            countryMenuExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Get Started", { onGetStarted(selectedCountryCode) }, modifier = Modifier.width(190.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Have an account? ", color = Muted, fontSize = 14.sp)
            Text(
                "Log in",
                color = Teal,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onLogin(selectedCountryCode) }.padding(vertical = 10.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

private data class CountryOption(
    val name: String,
    val flag: String,
    val phoneCode: String
)

@Composable
private fun IdentityIntroScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        TopLine(title = "Sign in", onBack = onBack)
        Spacer(Modifier.height(30.dp))
        Text(
            "Verify your identity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Verify your phone number to securely access your account and the marketplace.",
            fontSize = 15.sp,
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.kanz_logo),
            contentDescription = "Mining identity verification",
            modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f).clip(CircleShape),
            contentScale = ContentScale.Crop,
            colorFilter = KanzDuotone
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue", onContinue)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LocationScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    hasLocationPermission: Boolean,
    isDeviceLocationEnabled: Boolean,
    isMapRequest: Boolean,
    onEnableLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    location: Location?,
    locationMessage: String?
) {
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        TopLine(title = if (isMapRequest) "" else "Location access", onBack = onBack)
        if (!isMapRequest) {
            Spacer(Modifier.height(42.dp))
            Text(
                "Enable your location",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Allow location access to see your position on the map.",
                fontSize = 15.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            if (isMapRequest && hasLocationPermission && !isDeviceLocationEnabled) {
                Text(
                    "Your phone's Location is turned off. Turn it on to show your location on the map.",
                    fontSize = 15.sp,
                    color = Color(0xFFB3261E),
                    textAlign = TextAlign.Center
                )
            } else {
                LiveLocationStatus(if (hasLocationPermission) location else null, locationMessage)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            label = when {
                !hasLocationPermission && isMapRequest -> "Enable your location"
                !hasLocationPermission -> "Enable location access"
                isMapRequest && !isDeviceLocationEnabled -> "Turn on Location"
                isMapRequest -> "Open map"
                else -> "Continue"
            },
            onClick = when {
                !hasLocationPermission -> onEnableLocation
                isMapRequest && !isDeviceLocationEnabled -> onOpenLocationSettings
                else -> onContinue
            }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ConsentScreen(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onEnableLocation: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 34.sp, color = Ink, modifier = Modifier.clickable(onClick = onDismiss))
                Spacer(Modifier.width(10.dp))
                Text("Search by area", modifier = Modifier.weight(1f).background(Color(0xFFE7E7E7)).padding(8.dp), textAlign = TextAlign.Center, color = Muted)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton("⌖   Enable Location", onEnableLocation)
        }
        Column(
            Modifier.fillMaxWidth().fillMaxSize(0.79f).align(Alignment.Center)
                .padding(horizontal = 34.dp).clip(RoundedCornerShape(52.dp))
                .background(Color.White).padding(horizontal = 18.dp, vertical = 48.dp)
        ) {
            Text("Consent is required to use Mining App", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(14.dp))
            Text(
                "Mining App collects basic information\nthat helps maintain integrity.\nUsers must be 18 years or older.",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Ink
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Confirm", onConfirm)
        }
    }
}

@Composable
private fun CreateAccountScreen(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    phoneCountryCode: String,
    onPhoneChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    verificationCodeSent: Boolean,
    errorText: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRequestCode: () -> Unit,
    onResendCode: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        TopLine(title = "Create account", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        Text("Enter your details", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Full name") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email address") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Phone number ($phoneCountryCode)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Verification code") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (verificationCodeSent) {
            Spacer(Modifier.height(8.dp))
            Text("Enter the SMS code sent to your phone.", color = Teal, fontSize = 14.sp)
            TextButton(
                onClick = onResendCode,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Text("Resend code", color = Teal)
            }
        }
        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorText, color = Color(0xFFB3261E), fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            if (isLoading) "Please wait..." else "Send verification code",
            onRequestCode,
            enabled = !isLoading && !verificationCodeSent
        )
        if (verificationCodeSent) {
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                if (isLoading) "Please wait..." else "Create account",
                onCreateAccount,
                enabled = !isLoading
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun FormScreen(
    title: String,
    question: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    supportingText: String?,
    errorText: String?,
    isLoading: Boolean,
    continueLabel: String,
    onResend: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        TopLine(title = title, onBack = onBack)
        Spacer(Modifier.height(40.dp))
        Text(question, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, color = Color(0xFF9AA3A3)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        if (supportingText != null) {
            Spacer(Modifier.height(12.dp))
            Text(supportingText, color = Teal, fontSize = 14.sp)
        }
        if (onResend != null) {
            TextButton(
                onClick = onResend,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Text("Resend code", color = Teal)
            }
        }
        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorText, color = Color(0xFFB3261E), fontSize = 14.sp)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            if (isLoading) "Please wait..." else continueLabel,
            onContinue,
            enabled = !isLoading
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MarketplaceScreen(
    products: List<Product>,
    selectedCategory: String,
    onOpenListing: (Int) -> Unit,
    onOpenNavigation: () -> Unit,
    onCategory: (String) -> Unit
) {
    val categories = listOf("All") + ProductCategory.values().map(ProductCategory::label)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        HeaderActions(onMenu = onOpenNavigation)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { label ->
                Surface(
                    modifier = Modifier.clickable { onCategory(label) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (selectedCategory == label) Color.Black else Color(0xFFE8E8E8)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = if (selectedCategory == label) Color.White else Ink,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)
        ) {
            val filteredProducts = if (selectedCategory == "All") products else {
                products.filter { it.category.label == selectedCategory }
            }
            if (filteredProducts.isEmpty()) {
                Text(
                    "No listings in this category yet.",
                    modifier = Modifier.padding(20.dp),
                    color = Muted
                )
            }
            filteredProducts.forEach { product ->
                ListingCard(product = product, onClick = { onOpenListing(products.indexOf(product)) })
            }
        }
    }
}

@Composable
private fun ListingCard(product: Product, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(170.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(productImage(product)),
            contentDescription = product.title,
            modifier = Modifier.width(188.dp).height(150.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("• ${product.title}", fontSize = 15.sp, color = Ink)
            Text("• ${product.detail}", fontSize = 14.sp, color = Color(0xFFD0D3D5))
            Text("• ${product.price}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, onBack: () -> Unit, onInquire: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TitleHeader(title = "Product details", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(productImage(product)),
                contentDescription = product.title,
                modifier = Modifier.fillMaxWidth().height(285.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.padding(horizontal = 28.dp)) {
                Text("• ${product.detail} ${product.title}", fontSize = 15.sp, color = Color.Black)
                Spacer(Modifier.height(16.dp))
                Text(product.price, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Teal)
                product.specifications.forEach { specification ->
                    Spacer(Modifier.height(10.dp))
                    Text("• $specification", fontSize = 14.sp, color = Color.Black)
                }
            }
        }
        PrimaryButton("Buy / Inquire", onInquire, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

@Composable
private fun ChatThreadScreen(
    messages: List<ChatMessage>,
    errorText: String?,
    onBack: () -> Unit,
    onSend: (String, () -> Unit) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    val messageListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) messageListState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F8F7))) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 34.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            Text("Messages", color = Ink, fontWeight = FontWeight.Bold)
        }
        if (errorText != null) {
            Text(
                errorText,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE)).padding(10.dp),
                color = Color(0xFFB3261E),
                fontSize = 13.sp
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = messageListState,
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && errorText == null) {
                item {
                    Text(
                        "Start a private support conversation.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            }
            items(messages, key = { it.documentId ?: it.id.toString() }) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier.clickable(enabled = message.isOutgoing) {
                            selectedMessageId =
                                if (selectedMessageId == message.documentId) null else message.documentId
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.isOutgoing) Color(0xFFD8F2E9) else Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(Modifier.widthIn(max = 280.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                message.sender,
                                color = Teal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(message.text, color = Color(0xFF202627), fontSize = 15.sp)
                            if (message.timestamp > 0L) {
                                Text(
                                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp)),
                                    color = Muted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp)
                                )
                            }
                            if (message.isOutgoing && selectedMessageId == message.documentId) {
                                TextButton(
                                    onClick = {
                                        onDeleteMessage(message)
                                        selectedMessageId = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                ) {
                                    Text("Delete", color = Color(0xFFB3261E), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft) { draft = "" }
                    }
                },
                enabled = draft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MapScreen(
    onBack: () -> Unit,
    onOpenNavigation: () -> Unit,
    location: Location?,
    locationMessage: String?,
    hasLocationPermission: Boolean,
    places: List<MapPlace>,
    selectedFilter: String,
    onRequestLocation: () -> Unit,
    onCategory: (String) -> Unit
) {
    var recenterRequest by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TitleHeader(title = "Maps", onBack = onBack, onMenu = onOpenNavigation)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Mines", "Markets").forEachIndexed { index, label ->
                CategoryChip(label, selected = selectedFilter == label, onClick = { onCategory(label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LiveMap(
                location = location,
                hasLocationPermission = hasLocationPermission,
                selectedFilter = selectedFilter,
                recenterRequest = recenterRequest,
                places = places
            )
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp)
                    .clickable(onClick = onRequestLocation),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Mine and market locations",
                        color = Ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⛏ Mine locations     🛒 Market areas",
                        fontSize = 13.sp,
                        color = Muted
                    )
                    Spacer(Modifier.height(4.dp))
                    LiveLocationStatus(
                        if (hasLocationPermission) location else null,
                        locationMessage
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .size(52.dp).clickable {
                        if (!hasLocationPermission || location == null) {
                            onRequestLocation()
                        } else {
                            recenterRequest++
                        }
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "◎",
                        color = Color(0xFF1686F8),
                        fontSize = 29.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMap(
    location: Location?,
    hasLocationPermission: Boolean,
    selectedFilter: String,
    recenterRequest: Int,
    places: List<MapPlace>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val placeMarkers = remember { mutableMapOf<String, Marker>() }
    val liveMarker = remember { arrayOfNulls<Marker>(1) }
    var hasCenteredOnLocation by remember { mutableStateOf(false) }
    var lastCenteredLocation by remember { mutableStateOf<Location?>(null) }
    var lastHandledRecenterRequest by remember { mutableIntStateOf(0) }
    DisposableEffect(mapViewState.value, lifecycleOwner) {
        val mapView = mapViewState.value
        if (mapView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mapView.onResume()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onPause()
                mapView.onDetach()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView<View>(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                try {
                    Configuration.getInstance().load(
                        viewContext.applicationContext,
                        viewContext.applicationContext.getSharedPreferences(
                            "osmdroid",
                            Context.MODE_PRIVATE
                        )
                    )
                    Configuration.getInstance().userAgentValue = viewContext.packageName
                    MapView(viewContext).apply {
                        setBackgroundColor(AndroidColor.rgb(232, 234, 236))
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(20.25, -9.24))
                        mapViewState.value = this
                    }
                } catch (error: Exception) {
                    android.util.Log.e("KanzMap", "Unable to initialize OpenStreetMap", error)
                    mapFallbackView(viewContext)
                }
            },
            update = { view ->
                val mapView = view as? MapView ?: return@AndroidView
                try {
                val visiblePlaces = places.filter {
                    (selectedFilter == "All" || it.type.label == selectedFilter) &&
                        it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                        it.longitude.isFinite() && it.longitude in -180.0..180.0
                }
                val visibleIds = visiblePlaces.mapTo(mutableSetOf()) { it.id }
                (placeMarkers.keys - visibleIds).forEach { id ->
                    placeMarkers.remove(id)?.let(mapView.overlays::remove)
                }
                visiblePlaces.forEach { place ->
                    val marker = placeMarkers.getOrPut(place.id) {
                        Marker(mapView).apply {
                            title = place.name
                            snippet = place.description
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { clickedMarker, _ ->
                                clickedMarker.showInfoWindow()
                                true
                            }
                            icon = mapMarkerIcon(
                                context,
                                if (place.type == MapPlaceType.MINE) {
                                    AndroidColor.rgb(181, 120, 36)
                                } else {
                                    AndroidColor.rgb(22, 134, 91)
                                }
                            )
                            mapView.overlays.add(this)
                        }
                    }
                    marker.position = GeoPoint(place.latitude, place.longitude)
                }

                if (hasLocationPermission && context.hasLocationPermission() &&
                    location != null &&
                    location.latitude in -90.0..90.0 &&
                    location.longitude in -180.0..180.0
                ) {
                    val livePosition = GeoPoint(location.latitude, location.longitude)
                    val marker = liveMarker[0] ?: Marker(mapView).also {
                        it.title = "Your live location"
                        it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        it.setOnMarkerClickListener { clickedMarker, _ ->
                            clickedMarker.showInfoWindow()
                            true
                        }
                        it.icon = mapMarkerIcon(context, AndroidColor.rgb(22, 134, 248))
                        mapView.overlays.add(it)
                        liveMarker[0] = it
                    }
                    marker.position = livePosition
                    marker.snippet = String.format(
                        Locale.getDefault(),
                        "Lat %.5f, Long %.5f · Accuracy %.0f m",
                        location.latitude,
                        location.longitude,
                        location.accuracy
                    )

                    val locationChanged = lastCenteredLocation?.let {
                        it.time != location.time ||
                            it.latitude != location.latitude ||
                            it.longitude != location.longitude
                    } ?: true
                    if (
                        !hasCenteredOnLocation ||
                        locationChanged ||
                        recenterRequest != lastHandledRecenterRequest
                    ) {
                        mapView.controller.setZoom(15.0)
                        mapView.controller.animateTo(livePosition)
                        hasCenteredOnLocation = true
                        lastCenteredLocation = Location(location)
                        lastHandledRecenterRequest = recenterRequest
                    }
                } else {
                    liveMarker[0]?.let(mapView.overlays::remove)
                    liveMarker[0] = null
                    hasCenteredOnLocation = false
                    lastCenteredLocation = null
                }
                mapView.invalidate()
                } catch (error: RuntimeException) {
                    android.util.Log.e("KanzMap", "Unable to update OpenStreetMap overlays", error)
                }
            }
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            color = Color.White.copy(alpha = 0.88f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "© OpenStreetMap contributors",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                fontSize = 10.sp,
                color = Muted
            )
        }
    }
}

private fun mapFallbackView(context: Context): View = object : View(context) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(210, 213, 216)
        strokeWidth = context.resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val spacing = 48f * context.resources.displayMetrics.density
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += spacing
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += spacing
        }
    }
}.apply {
    setBackgroundColor(AndroidColor.rgb(232, 234, 236))
}

private fun mapMarkerIcon(context: Context, color: Int): GradientDrawable {
    val density = context.resources.displayMetrics.density
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke((2 * density).toInt(), AndroidColor.WHITE)
        setSize((22 * density).toInt(), (22 * density).toInt())
    }
}

@Composable
private fun LiveLocationStatus(location: Location?, message: String?) {
    val status = when {
        location != null -> String.format(
            Locale.getDefault(),
            "Lat %.5f, Long %.5f  •  Accuracy %.0f m",
            location.latitude,
            location.longitude,
            location.accuracy
        ) + "  •  Updated ${DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(location.time))}"
        message != null -> message
        else -> "Waiting for your current location..."
    }
    Text(status, fontSize = 13.sp, color = if (location == null) Muted else Ink)
}

@Composable
private fun NavigationScreen(
    onBack: () -> Unit,
    location: Location?,
    locationMessage: String?,
    hasLocationPermission: Boolean,
    places: List<MapPlace>
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TitleHeader(title = "Navigation", onBack = onBack, onMenu = onBack)
        Box(Modifier.fillMaxSize()) {
            LiveMap(location, hasLocationPermission, "All", 0, places)
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Your live location", color = Ink, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    LiveLocationStatus(location, locationMessage)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    fullName: String,
    email: String,
    phone: String,
    profileImageUri: String?,
    onProfileImageSelected: (String) -> Unit,
    onSignOut: () -> Unit,
    onCategory: (String) -> Unit
) {
    val context = LocalContext.current
    var profileDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var profileImage by remember(profileImageUri) { mutableStateOf<Bitmap?>(null) }
    var profileImageError by remember { mutableStateOf<String?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                profileImageError = null
                onProfileImageSelected(uri.toString())
            } catch (error: SecurityException) {
                Log.e("MiningAppProfile", "Unable to keep access to the selected profile photo.", error)
                profileImageError = "Could not access that photo. Please choose another one."
            }
        }
    }

    LaunchedEffect(profileImageUri) {
        profileImage = null
        profileImageError = null
        val uri = profileImageUri?.let(Uri::parse) ?: return@LaunchedEffect
        try {
            profileImage = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            if (profileImage == null) {
                profileImageError = "This image could not be opened. Please choose another one."
            }
        } catch (error: IOException) {
            Log.e("MiningAppProfile", "Unable to load the selected profile photo.", error)
            profileImageError = "This image could not be opened. Please choose another one."
        } catch (error: SecurityException) {
            Log.e("MiningAppProfile", "Access to the selected profile photo was denied.", error)
            profileImageError = "Access to this image expired. Please choose it again."
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF1F1F1)).padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (profileImage != null) {
                    Image(
                        bitmap = profileImage!!.asImageBitmap(),
                        contentDescription = "Choose profile photo",
                        modifier = Modifier.size(76.dp).clip(CircleShape)
                            .clickable { galleryLauncher.launch(arrayOf("image/*")) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = "Choose profile photo",
                        modifier = Modifier.size(76.dp).clip(CircleShape)
                            .clickable { galleryLauncher.launch(arrayOf("image/*")) },
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        fullName.ifBlank { "Profile" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    if (email.isNotBlank()) {
                        Text(email, fontSize = 12.sp, color = Muted)
                    }
                }
                Text(
                    if (profileDetailsExpanded) "⌃" else "⌄",
                    modifier = Modifier.padding(start = 8.dp).clickable {
                        profileDetailsExpanded = !profileDetailsExpanded
                    },
                    color = Teal,
                    fontSize = 26.sp
                )
            }
            if (profileDetailsExpanded) {
                Spacer(Modifier.height(12.dp))
                Text("Profile details", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                ProfileDetailRow("Name", fullName.ifBlank { "Not provided" })
                ProfileDetailRow("Email", email.ifBlank { "Not provided" })
                ProfileDetailRow("Phone", phone.ifBlank { "Not provided" })
                TextButton(
                    onClick = onSignOut,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Sign out", color = Teal)
                }
                profileImageError?.let { error ->
                    Text(
                        error,
                        modifier = Modifier.padding(bottom = 4.dp),
                        color = Color(0xFFB3261E),
                        fontSize = 13.sp
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(54.dp))
                .background(Color(0xFFF1F1F1)).padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text("Services", fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(start = 10.dp, bottom = 8.dp))
            listOf(
                listOf("Marketplace" to R.drawable.marketplace_icon, "Jobs" to R.drawable.jobs_icon),
                listOf("Car rentals" to R.drawable.car_icon, "Maps" to R.drawable.legal_icon),
                listOf("Legal" to R.drawable.legal_icon, "Prospecting" to R.drawable.prospecting_icon)
            ).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, icon) ->
                        Row(
                            Modifier.weight(1f).clickable { onCategory(label) }.padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Color.Black, fontSize = 15.sp)
    }
}

@Composable
private fun CategoryScreen(
    title: String,
    onBack: () -> Unit,
    onOpenNavigation: () -> Unit,
    onCategory: (String) -> Unit,
    onOpenListing: (Int) -> Unit,
    products: List<Product>
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        HeaderActions(onMenu = onOpenNavigation)
        TitleHeader(title = title, onBack = onBack)
        if (title == "Services" || title == "Marketplace") {
            Text(
                "Category",
                modifier = Modifier.padding(start = 30.dp, top = 14.dp, bottom = 4.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            CategoryGrid(onCategory = onCategory)
        } else {
        val matchingProducts = products.filter {
            it.category.label.equals(title, ignoreCase = true) ||
                (title == "Electronic" && it.category == ProductCategory.ELECTRONIC) ||
                (title == "Electric" && it.category == ProductCategory.ELECTRIC)
            }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            matchingProducts.forEach { product ->
                ListingCard(product) { onOpenListing(products.indexOf(product)) }
            }
            if (matchingProducts.isEmpty()) Text("No listings in this category yet.", Modifier.padding(20.dp), color = Muted)
        }
        }
    }
}

@Composable
private fun CategoryGrid(onCategory: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf(
            Triple("Electronic", R.drawable.electronics_icon, "Electronic"),
            Triple("Electric", R.drawable.services_icon, "Electric"),
            Triple("Wanted", R.drawable.wanted_icon, "Wanted"),
            Triple("Drilling", R.drawable.drilling_icon, "Drilling"),
            Triple("Car rentals", R.drawable.car_icon, "Car rentals")
        ).chunked(3).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, icon, route) ->
                    Column(
                        Modifier.weight(1f).clickable { onCategory(route) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(58.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = Color.Black, fontSize = 13.sp)
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeaderActions(onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Text("☰", color = Color.Black, fontSize = 25.sp, modifier = Modifier.clickable(onClick = onMenu).padding(horizontal = 8.dp))
        Text("⌕", color = Color.Black, fontSize = 28.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Text("🔔", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
private fun TitleHeader(title: String, onBack: () -> Unit, onMenu: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", fontSize = 34.sp, color = Color.Black, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
        if (onMenu != null) {
            Text("☰", color = Color.Black, fontSize = 25.sp, modifier = Modifier.clickable(onClick = onMenu).padding(horizontal = 7.dp))
            Text("⌕", color = Color.Black, fontSize = 27.sp, modifier = Modifier.padding(horizontal = 7.dp))
            Text("🔔", fontSize = 19.sp, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
private fun TopLine(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 34.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack))
        if (title.isNotEmpty()) {
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = if (selected) Color.Black else Color(0xFFE8E8E8)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 9.dp),
            color = if (selected) Color.White else Color.Black,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun BottomNavigation(selected: String, onSelect: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "Home" to "⌂",
                "Legal" to "⚖",
                "Maps" to "⌖",
                "Chats" to "☏",
                "Profile" to "○"
            ).forEach { (label, icon) ->
                Column(
                    Modifier.weight(1f).clickable { onSelect(label) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, color = Color.Black, fontSize = 21.sp)
                    Text(label, color = Color.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
