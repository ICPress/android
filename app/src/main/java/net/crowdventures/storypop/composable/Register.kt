package net.crowdventures.storypop.composable

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Config
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.composable.ComposableUtil.Companion.HandleImeShown
import net.crowdventures.storypop.composable.ComposableUtil.Companion.ShowProgressDialog
import net.crowdventures.storypop.dto.AccountInfo
import net.crowdventures.storypop.util.BottomBarManagerCallback
import net.crowdventures.storypop.util.EndpointAccountHandler
import net.crowdventures.storypop.util.EndpointServerHandler
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.viewmodels.RegisterViewModel
import java.util.Locale
import java.util.UUID

public class Register {
    companion object {
        @Composable
        fun ApiEndpointSelector(
            selectedEndpoint: MutableState<String>,
            isServerReachable: MutableState<Boolean>,
            isCheckingServer: MutableState<Boolean>,
            onEndpointSelected: (String) -> Unit,
            onCustomEndpointChange: (String) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }
            var customEndpointVisible by remember { mutableStateOf(false) }
            var customEndpointText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Endpoint Label
                Text(
                    text = "API Endpoint",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Endpoint Selector Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Status Indicator
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCheckingServer.value -> Color(0xFFFFA500)
                                            isServerReachable.value -> Color(0xFF28A745)
                                            else -> Color(0xFFE53E3E)
                                        }
                                    )
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = Config.apiEndpoints.find { it.url == selectedEndpoint.value }?.name
                                        ?: if (customEndpointVisible) "Custom" else "Select Endpoint",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onBackground
                                )
                                Text(
                                    text = selectedEndpoint.value.ifEmpty { "Choose API endpoint" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colors.secondary
                        )
                    }
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colors.surface)
                ) {
                    Config.apiEndpoints.forEach { endpoint ->
                        DropdownMenuItem(
                            onClick = {
                                if (endpoint.name == "Custom") {
                                    customEndpointVisible = true
                                    selectedEndpoint.value = ""
                                } else {
                                    customEndpointVisible = false
                                    selectedEndpoint.value = endpoint.url
                                    onEndpointSelected(endpoint.url)
                                }
                                expanded = false
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = endpoint.name,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                if (endpoint.url == selectedEndpoint.value && !customEndpointVisible) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF28A745),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (endpoint.isDefault) {
                                    Text(
                                        text = "Default",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colors.secondary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Endpoint Input
                AnimatedVisibility(
                    visible = customEndpointVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = customEndpointText,
                            onValueChange = {
                                customEndpointText = it
                                selectedEndpoint.value = it
                                onCustomEndpointChange(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "https://your-server.com",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            label = { Text("Custom Endpoint URL") },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.secondary,
                                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                cursorColor = MaterialTheme.colors.secondary,
                                textColor = MaterialTheme.colors.onBackground
                            )
                        )
                        Text(
                            text = "Enter the full URL including https://",
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        @Composable
        fun VerifyEmail() {
            Column(
                modifier = Modifier
                    .padding(0.dp, 15.dp, 0.dp, 0.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    Modifier
                        .width(100.dp)
                        .height(100.dp).align(Alignment.CenterHorizontally)
                ) {
                    Profile.StoryPopIcon(80.dp)
                }
                Column {
                    Text(
                        text = "Check your email inbox for the verification link!",
                        Modifier
                            .padding(40.dp, 40.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun RegisterView(
            context: Context,
            registerViewModel: RegisterViewModel,
            bottomBarVisible: MutableState<Boolean>
        ) {
            val showSignIn = remember { mutableStateOf(registerViewModel.isRequestingSignOnLink) }
            val sharedPrefs = remember { SharedPreferenceManager(context) }
            val savedEndpoint = remember { mutableStateOf(sharedPrefs.getApiEndpoint() ?: Config.APP_ENDPOINT) }
            val isServerReachable = remember { mutableStateOf(true) }
            val isCheckingServer = remember { mutableStateOf(false) }

            // Update API endpoint when changed
            LaunchedEffect(savedEndpoint.value) {
                if (!savedEndpoint.value.contains("http://") && !savedEndpoint.value.contains("https://"))
                    savedEndpoint.value = "https://"+ savedEndpoint.value.ifEmpty { "api.example.com" }
                if (savedEndpoint.value.length == "http://".length || savedEndpoint.value.length == "https://".length) return@LaunchedEffect
                sharedPrefs.setApiEndpoint(savedEndpoint.value)
                Config.APP_ENDPOINT = savedEndpoint.value
                isCheckingServer.value = true

                // Check server reachability
                kotlinx.coroutines.delay(500)
                val reachable = EndpointServerHandler.checkServerReachability(context)
                isServerReachable.value = reachable
                isCheckingServer.value = false
            }

            HandleImeShown(object : BottomBarManagerCallback {
                override fun hide() {
                    bottomBarVisible.value = true
                }

                override fun show() {
                    bottomBarVisible.value = false
                }
            })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                // Server Status Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        isCheckingServer.value -> Color(0xFFFFA500).copy(alpha = 0.1f)
                        isServerReachable.value -> Color(0xFF28A745).copy(alpha = 0.1f)
                        else -> Color(0xFFE53E3E).copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCheckingServer.value -> Color(0xFFFFA500)
                                        isServerReachable.value -> Color(0xFF28A745)
                                        else -> Color(0xFFE53E3E)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isCheckingServer.value -> "Checking server connection..."
                                isServerReachable.value -> "Connected to ${savedEndpoint.value}"
                                else -> "Unable to connect to server. Check your endpoint."
                            },
                            fontSize = 12.sp,
                            color = when {
                                isCheckingServer.value -> Color(0xFFFFA500)
                                isServerReachable.value -> Color(0xFF28A745)
                                else -> Color(0xFFE53E3E)
                            }
                        )
                    }
                }

                if (!showSignIn.value) {
                    RegisterAccountView(
                        context = context,
                        registerViewModel = registerViewModel,
                        showSignIn = showSignIn,
                        selectedEndpoint = savedEndpoint,
                        isServerReachable = isServerReachable,
                        isCheckingServer = isCheckingServer,
                        onEndpointSelected = { endpoint ->
                            savedEndpoint.value = endpoint
                        }
                    )
                } else {
                    SignInView(
                        context = context,
                        registerViewModel = registerViewModel,
                        showSignIn = showSignIn,
                        selectedEndpoint = savedEndpoint,
                        isServerReachable = isServerReachable,
                        isCheckingServer = isCheckingServer,
                        onEndpointSelected = { endpoint ->
                            savedEndpoint.value = endpoint
                        }
                    )
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun SignInView(
            context: Context,
            registerViewModel: RegisterViewModel,
            showSignIn: MutableState<Boolean>,
            selectedEndpoint: MutableState<String>,
            isServerReachable: MutableState<Boolean>,
            isCheckingServer: MutableState<Boolean>,
            onEndpointSelected: (String) -> Unit
        ) {
            var showDialog by remember { mutableStateOf(false) }
            var textStateEmail by remember { mutableStateOf(TextFieldValue(registerViewModel.email)) }
            var textStateEmailInvalid by remember { mutableStateOf(false) }
            var errorText by remember { mutableStateOf("") }
            val iSource = remember { MutableInteractionSource() }
            val coroutineScope = rememberCoroutineScope()
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Section
                Card(
                    modifier = Modifier
                        .size(80.dp),
                    shape = CircleShape,
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )

                Text(
                    text = "Enter your email to receive a sign-in link",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // API Endpoint Selector
                ApiEndpointSelector(
                    selectedEndpoint = selectedEndpoint,
                    isServerReachable = isServerReachable,
                    isCheckingServer = isCheckingServer,
                    onEndpointSelected = onEndpointSelected,
                    onCustomEndpointChange = { endpoint ->
                        onEndpointSelected(endpoint)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Input
                OutlinedTextField(
                    value = textStateEmail,
                    onValueChange = {
                        textStateEmail = it
                        registerViewModel.email = it.text.trim()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Email address") },
                    isError = textStateEmailInvalid,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.secondary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colors.secondary,
                        textColor = MaterialTheme.colors.onBackground
                    ),
                    singleLine = true
                )

                if (textStateEmailInvalid) {
                    Text(
                        text = "Please enter a valid email address",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back Button
                    OutlinedButton(
                        onClick = {
                            registerViewModel.isRequestingSignOnLink = false
                            showSignIn.value = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.secondary
                        )
                    ) {
                        Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Sign In Button
                    Button(
                        onClick = {
                            textStateEmailInvalid = !Config.isValidEmail(textStateEmail.text.trim())
                            if (!textStateEmailInvalid && isServerReachable.value) {
                                showDialog = true
                                registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable + StoryUtil.coroutineExceptionHandler) {
                                    val responseText = EndpointAccountHandler.sendSignInLink(
                                        context,
                                        textStateEmail.text.trim()
                                    )
                                    withContext(Dispatchers.Main) {
                                        showDialog = false
                                        if (responseText == "") {
                                            registerViewModel.sentRegisterRequest.value = true
                                        } else {
                                            errorText = responseText
                                        }
                                    }
                                }
                            } else if (!isServerReachable.value) {
                                errorText = "Cannot connect to server. Please check your endpoint."
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = Color.White
                        ),
                        enabled = isServerReachable.value
                    ) {
                        Text("Send Sign-in Link", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color(0xFFE53E3E),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    )
                }
            }

            if (showDialog) ShowProgressDialog()
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun RegisterAccountView(
            context: Context,
            registerViewModel: RegisterViewModel,
            showSignIn: MutableState<Boolean>,
            selectedEndpoint: MutableState<String>,
            isServerReachable: MutableState<Boolean>,
            isCheckingServer: MutableState<Boolean>,
            onEndpointSelected: (String) -> Unit
        ) {
            val scrollState = rememberScrollState()
            var showDialog by remember { mutableStateOf(false) }
            var otherErrorText by remember { mutableStateOf<String?>(null) }

            var textStateUsername by remember { mutableStateOf(TextFieldValue(registerViewModel.userName)) }
            var textStateEmail by remember { mutableStateOf(TextFieldValue(registerViewModel.email)) }
            var textStateEmailConfirm by remember { mutableStateOf(TextFieldValue()) }
            var textStateUsernameInvalid by remember { mutableStateOf(false) }
            var textStateUsernameTaken by remember { mutableStateOf(false) }
            var textStateEmailInvalid by remember { mutableStateOf(false) }
            var textStateEmailRegistered by remember { mutableStateOf(false) }
            var textStateEmailConfirmInvalid by remember { mutableStateOf(false) }
            val bringIntoViewRequesterUsername = remember { BringIntoViewRequester() }
            val bringIntoViewRequesterEmail = remember { BringIntoViewRequester() }
            val bringIntoViewRequesterEmailConfirm = remember { BringIntoViewRequester() }
            val bringIntoViewRegisterButton = remember { BringIntoViewRequester() }

            val focusManager = LocalFocusManager.current
            val uriHandler = LocalUriHandler.current
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Section
                Card(
                    modifier = Modifier
                        .size(80.dp),
                    shape = CircleShape,
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )

                Text(
                    text = "Join ICPress to read and publish articles",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // API Endpoint Selector
                ApiEndpointSelector(
                    selectedEndpoint = selectedEndpoint,
                    isServerReachable = isServerReachable,
                    isCheckingServer = isCheckingServer,
                    onEndpointSelected = onEndpointSelected,
                    onCustomEndpointChange = { endpoint ->
                        onEndpointSelected(endpoint)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username Field
                OutlinedTextField(
                    value = textStateUsername,
                    onValueChange = { newValue ->
                        val cleaned = newValue.text
                            .replace(Regex("[^._|A-Za-z0-9\\[\\]]"), "")
                            .lowercase(Locale.ROOT)
                            .take(Config.MAX_USERNAME_LENGTH)

                        if (cleaned != newValue.text) {
                            // Invalid characters were removed, update with cleaned version
                            textStateUsername = TextFieldValue(
                                text = cleaned,
                                selection = TextRange(cleaned.length)
                            )
                        } else {
                            textStateUsername = newValue
                        }
                        registerViewModel.userName = cleaned
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                textStateUsernameInvalid = textStateUsername.text.length < 4
                                if (!textStateUsernameInvalid) {
                                    registerViewModel.viewModelScope.launch(Dispatchers.IO) {
                                        textStateUsernameTaken = !EndpointAccountHandler.verifyUsernameOK(
                                            context, textStateUsername.text
                                        )
                                    }
                                }
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequesterUsername),
                    placeholder = { Text("Username") },
                    isError = textStateUsernameInvalid || textStateUsernameTaken,
                    leadingIcon = {
                        Text(
                            "@",
                            color = MaterialTheme.colors.secondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.secondary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colors.secondary,
                        textColor = MaterialTheme.colors.onBackground
                    ),
                    singleLine = true
                )
                if (textStateUsernameInvalid) {
                    Text(
                        text = "Username must be at least 4 characters",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                if (textStateUsernameTaken) {
                    Text(
                        text = "Username already taken",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Email Field
                OutlinedTextField(
                    value = textStateEmail,
                    onValueChange = {
                        textStateEmail = it
                        registerViewModel.email = it.text.trim()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                textStateEmailInvalid = !Config.isValidEmail(textStateEmail.text.trim())
                                if (!textStateEmailInvalid) {
                                    registerViewModel.viewModelScope.launch(Dispatchers.IO) {
                                        textStateEmailRegistered = !EndpointAccountHandler.verifyEmailOK(
                                            context, textStateEmail.text.trim()
                                        )
                                    }
                                }
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequesterEmail),
                    placeholder = { Text("Email address") },
                    isError = textStateEmailInvalid || textStateEmailRegistered,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.secondary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colors.secondary,
                        textColor = MaterialTheme.colors.onBackground
                    ),
                    singleLine = true
                )

                if (textStateEmailInvalid) {
                    Text(
                        text = "Please enter a valid email address",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                if (textStateEmailRegistered) {
                    Text(
                        text = "An account already exists with this email",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Email Field
                OutlinedTextField(
                    value = textStateEmailConfirm,
                    onValueChange = { textStateEmailConfirm = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                textStateEmailConfirmInvalid = textStateEmailConfirm.text.trim()
                                    .lowercase() != textStateEmail.text.trim().lowercase()
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequesterEmailConfirm),
                    placeholder = { Text("Confirm email address") },
                    isError = textStateEmailConfirmInvalid,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.secondary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colors.secondary,
                        textColor = MaterialTheme.colors.onBackground
                    ),
                    singleLine = true
                )

                if (textStateEmailConfirmInvalid) {
                    Text(
                        text = otherErrorText ?: "Email addresses do not match",
                        color = Color(0xFFE53E3E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sign In Link
                    TextButton(
                        onClick = { showSignIn.value = true },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "Existing user? Sign in",
                            color = MaterialTheme.colors.secondary,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 13.sp
                        )
                    }

                    // Register Button
                    Button(
                        onClick = {
                            textStateUsernameInvalid = textStateUsername.text.length < 4
                            textStateEmailInvalid = !Config.isValidEmail(textStateEmail.text.trim())
                            textStateEmailConfirmInvalid = textStateEmailConfirm.text.trim()
                                .lowercase() != textStateEmail.text.trim().lowercase()

                            if (!textStateUsernameInvalid && !textStateEmailInvalid &&
                                !textStateEmailConfirmInvalid && !textStateUsernameTaken &&
                                !textStateEmailRegistered && isServerReachable.value
                            ) {
                                val userUUID = UUID.randomUUID()
                                val accountInfo = AccountInfo(
                                    userUUID.toString(),
                                    registerViewModel.email,
                                    registerViewModel.userName
                                )
                                showDialog = true
                                registerViewModel.viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                                    if (registerViewModel.sentRegisterRequest.value != true) {
                                        val requestResult = EndpointAccountHandler.createAccount(
                                            context, accountInfo
                                        )
                                        withContext(Dispatchers.Main) {
                                            when (requestResult) {
                                                201 -> {
                                                    Config.firebaseAnalytics.logEvent("register_verify_email", null)
                                                    registerViewModel.sentRegisterRequest.value = true
                                                }
                                                409 -> {
                                                    otherErrorText = "An account already exists with this email."
                                                }
                                                0 -> {
                                                    otherErrorText = "Connection issue. Please check your internet."
                                                }
                                                else -> {
                                                    otherErrorText = "Server error. Please try again later."
                                                }
                                            }
                                            showDialog = false
                                        }
                                    }
                                }
                            } else if (!isServerReachable.value) {
                                otherErrorText = "Cannot connect to server. Please check your endpoint."
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bringIntoViewRequester(bringIntoViewRegisterButton),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = Color.White
                        ),
                        enabled = isServerReachable.value
                    ) {
                        Text("Create Account", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Policy Link
                TextButton(
                    onClick = {
                        Config.firebaseAnalytics.logEvent("privacy_policy_open", null)
                        uriHandler.openUri("https://icpress.org/privacy-policy")
                    }
                ) {
                    Text(
                        text = "Privacy Policy",
                        color = MaterialTheme.colors.secondary,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 12.sp
                    )
                }
            }

            if (showDialog) ShowProgressDialog()
        }
    }
}