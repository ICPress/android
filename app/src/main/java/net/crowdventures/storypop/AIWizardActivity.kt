package net.crowdventures.storypop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.crowdventures.storypop.composable.AIWizardStep
import net.crowdventures.storypop.composable.ComposableUtil
import net.crowdventures.storypop.models.AIOption
import net.crowdventures.storypop.models.ArticleType
import net.crowdventures.storypop.models.GroqModel
import net.crowdventures.storypop.util.AIRequestHandlerUtil
import net.crowdventures.storypop.util.GroqModelProvider
import net.crowdventures.storypop.util.SuccessCallback
import net.crowdventures.storypop.viewmodels.AIWizardViewModel

class AIWizardActivity : ComponentActivity() {

    lateinit var viewModel: AIWizardViewModel

    companion object {
        const val EXISTING_TITLE_EXTRA = "EXISTING_TITLE_EXTRA"
        const val EXISTING_CONTENT_EXTRA = "EXISTING_CONTENT_EXTRA"
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[AIWizardViewModel::class.java]

        if (intent.hasExtra(EXISTING_TITLE_EXTRA)) {
            viewModel.existingTitle = intent.getStringExtra(EXISTING_TITLE_EXTRA) ?: ""
            viewModel.topic = viewModel.existingTitle
        }
        if (intent.hasExtra(EXISTING_CONTENT_EXTRA)) {
            viewModel.existingContent = intent.getStringExtra(EXISTING_CONTENT_EXTRA) ?: ""
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val sharedPreferenceManager = SharedPreferenceManager(this)
        val groqKey = sharedPreferenceManager.getGroqKey()

        if (groqKey == null) {
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colors = if (isSystemInDarkTheme()) ComposableUtil.GetDarkColors(this) else ComposableUtil.GetLightColors(
                    this
                )
            ) {
                AIWizardScreen(
                    groqKey = groqKey,
                    context = this.applicationContext,
                    viewModel = viewModel,
                    onFinish = { title, content ->
                        setResult(
                            Activity.RESULT_OK, Intent().putStringArrayListExtra(
                                ArticleContentEditActivity.AI_CONTENT_RESULT,
                                arrayListOf(title, content)
                            )
                        )
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun AIWizardScreen(
    groqKey: String,
    context: Context,
    viewModel: AIWizardViewModel,
    onFinish: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(AIWizardStep.SELECT_HELP_TYPE) }
    var selectedOption by remember { mutableStateOf<AIOption?>(null) }
    var selectedModel by remember { mutableStateOf(GroqModelProvider.getModels().first()) }
    var customPrompt by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var topic by remember { mutableStateOf(viewModel.topic) }
    var selectedArticleType by remember { mutableStateOf(viewModel.selectedArticleType) }

    val coroutineScope = rememberCoroutineScope()

    // Handle back button
    BackHandler {
        when (currentStep) {
            AIWizardStep.SELECT_HELP_TYPE -> showExitDialog = true
            AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL -> {
                currentStep = AIWizardStep.SELECT_HELP_TYPE
                selectedOption = null
            }

            AIWizardStep.PROCESSING -> {
                showExitDialog = true
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = onCancel,
            onDismiss = { showExitDialog = false }
        )
    }

    // Error dialog
    if (errorMessage != null) {
        ErrorDialog(
            message = errorMessage!!,
            onDismiss = { errorMessage = null }
        )
    }

    // Loading dialog
    if (showLoading) {
        LoadingDialog(
            message = when (currentStep) {
                AIWizardStep.PROCESSING -> "Generating content..."
                else -> "Processing your request..."
            }
        )
    }

    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // Top App Bar
        AIWizardTopBar(
            currentStep = currentStep,
            selectedOption = selectedOption,
            onBack = {
                when (currentStep) {
                    AIWizardStep.SELECT_HELP_TYPE -> showExitDialog = true
                    AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL -> {
                        currentStep = AIWizardStep.SELECT_HELP_TYPE
                        selectedOption = null
                    }

                    AIWizardStep.PROCESSING -> {
                        showExitDialog = true
                    }
                }
            },
            onClose = { showExitDialog = true }
        )

        // Step content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                AIWizardStep.SELECT_HELP_TYPE -> {
                    SelectHelpTypeStep(
                        selectedOption = selectedOption,
                        onOptionSelected = { option ->
                            selectedOption = option
                        },
                        onContinue = {
                            if (selectedOption != null) {
                                currentStep = AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL
                            }
                        }
                    )
                }

                AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL -> {
                    val titlePrompt = stringResource(
                        R.string.title_suggestion_prompt,
                        selectedArticleType.displayName,
                        viewModel.topic
                    )
                    CustomizePromptStep(
                        selectedOption = selectedOption!!,
                        topic = topic,
                        onTopicChange = { topic = it },
                        selectedArticleType = selectedArticleType,
                        onArticleTypeChange = { selectedArticleType = it },
                        customPrompt = customPrompt,
                        onPromptChange = { customPrompt = it },
                        selectedModel = selectedModel,
                        onModelChange = { selectedModel = it },
                        existingTitle = viewModel.existingTitle,
                        existingContent = viewModel.existingContent,
                        onGenerate = {

                            currentStep = AIWizardStep.PROCESSING
                            showLoading = true

                            // Save to ViewModel
                            viewModel.topic = topic
                            viewModel.selectedArticleType = selectedArticleType
                            if (selectedOption == AIOption.BothTitleAndContent || selectedOption == AIOption.ContentOnly)
                                AIRequestHandlerUtil.suggest(
                                    groqKey, selectedModel.id, customPrompt,
                                    context,
                                    viewModel.viewModelScope,
                                    viewModel.topic,
                                    object : SuccessCallback<List<String>> {
                                        override fun onSuccess(vararg paramContent: List<String>) {
                                            val content = paramContent.first().first()
                                            if (selectedOption == AIOption.BothTitleAndContent) {
                                                AIRequestHandlerUtil.suggest(
                                                    groqKey,
                                                    selectedModel.id,
                                                    titlePrompt,
                                                    context,
                                                    viewModel.viewModelScope,
                                                    viewModel.topic,
                                                    object : SuccessCallback<List<String>> {
                                                        override fun onSuccess(vararg paramTitle: List<String>) {
                                                            val title = paramTitle.first().first()
                                                            viewModel.generatedTitle = title
                                                            viewModel.generatedContent = content
                                                            onFinish(title, content)
                                                        }

                                                        override fun onFailure(reason: Any?) {
                                                            errorMessage =
                                                                "Failed to generate content. Please try again."
                                                            currentStep =
                                                                AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL
                                                            showLoading = false
                                                        }
                                                    })

                                            } else {
                                                viewModel.generatedContent = content
                                                onFinish(viewModel.generatedTitle, content)
                                            }
                                        }

                                        override fun onFailure(reason: Any?) {
                                            errorMessage =
                                                "Failed to generate content. Please try again."
                                            currentStep =
                                                AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL
                                            showLoading = false
                                        }
                                    }
                                ) else {
                                AIRequestHandlerUtil.suggest(
                                    groqKey,
                                    selectedModel.id,
                                    customPrompt,
                                    context,
                                    viewModel.viewModelScope,
                                    viewModel.topic,
                                    object : SuccessCallback<List<String>> {
                                        override fun onSuccess(vararg paramTitle: List<String>) {
                                            val title = paramTitle.first().first()
                                            viewModel.generatedTitle = title
                                            onFinish(title, viewModel.existingContent)
                                        }

                                        override fun onFailure(reason: Any?) {
                                            errorMessage =
                                                "Failed to generate content. Please try again."
                                            currentStep =
                                                AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL
                                            showLoading = false
                                        }
                                    })
                            }


                        },
                        onBack = {
                            currentStep = AIWizardStep.SELECT_HELP_TYPE
                            selectedOption = null
                        }
                    )
                }

                AIWizardStep.PROCESSING -> {
                    // Processing is handled by loading dialog
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun AIWizardTopBar(
    currentStep: AIWizardStep,
    selectedOption: AIOption?,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        color = colorResource(id = R.color.primaryColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(6.dp),
                tint = colorResource(id = R.color.textLight)
            )

            // Title
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (currentStep) {
                        AIWizardStep.SELECT_HELP_TYPE -> "AI Writing Assistant"
                        AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL ->
                            "Customize Your Article"

                        AIWizardStep.PROCESSING -> "Processing"
                    },
                    color = colorResource(id = R.color.textLight),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Step indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StepIndicator(
                        isActive = currentStep == AIWizardStep.SELECT_HELP_TYPE,
                        isCompleted = currentStep.ordinal > AIWizardStep.SELECT_HELP_TYPE.ordinal,
                        label = "Choose Help"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    StepIndicator(
                        isActive = currentStep == AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL,
                        isCompleted = currentStep.ordinal > AIWizardStep.CUSTOMIZE_PROMPT_AND_MODEL.ordinal,
                        label = "Describe & Customize"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    StepIndicator(
                        isActive = currentStep == AIWizardStep.PROCESSING,
                        isCompleted = false,
                        label = "Generate"
                    )
                }
            }

            // Close button
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
                    .padding(6.dp),
                tint = colorResource(id = R.color.textLight)
            )
        }
    }
}

@Composable
fun StepIndicator(
    isActive: Boolean,
    isCompleted: Boolean,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> colorResource(id = R.color.success)
                        isActive -> colorResource(id = R.color.textLight)
                        else -> colorResource(id = R.color.textLight).copy(alpha = 0.3f)
                    }
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isActive) colorResource(id = R.color.textLight)
            else colorResource(id = R.color.textLight).copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun SelectHelpTypeStep(
    selectedOption: AIOption?,
    onOptionSelected: (AIOption) -> Unit,
    onContinue: () -> Unit
) {
    val options = AIOption.getAllOptions()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "What would you like help with?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Select the type of assistance you need for your article",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Options
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                AIOptionCard(
                    option = option,
                    isSelected = selectedOption == option,
                    onSelect = { onOptionSelected(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            enabled = selectedOption != null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                disabledBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Continue",
                color = MaterialTheme.colors.onSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AIOptionCard(
    option: AIOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = if (isSelected) {
        if (isDarkTheme) {
            // For dark mode: use secondary color with 20% opacity (lighter appearance)
            MaterialTheme.colors.secondary.copy(alpha = 0.25f)
        } else {
            // For light mode: use secondaryColor_20 from resources (consistent blue)
            colorResource(id = R.color.secondaryColor_20)
        }
    } else {
        MaterialTheme.colors.surface
    }

    val iconBackgroundColor = if (isSelected) {
        MaterialTheme.colors.secondary
    } else {
        MaterialTheme.colors.primary.copy(alpha = 0.1f)
    }

    val iconTintColor = if (isSelected) {
        MaterialTheme.colors.onSecondary
    } else {
        MaterialTheme.colors.secondary
    }

    val titleColor = if (isSelected) {
        if (isDarkTheme) {
            // For dark mode selected: use white text for visibility
            MaterialTheme.colors.onSurface
        } else {
            // For light mode selected: use primary color (deep blue)
            MaterialTheme.colors.primary
        }
    } else {
        MaterialTheme.colors.onBackground
    }

    val descriptionColor = if (isSelected) {
        if (isDarkTheme) {
            // For dark mode selected: use lighter text with opacity
            MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        } else {
            // For light mode selected: use onSurface with opacity
            MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        }
    } else {
        MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        elevation = if (isSelected) 0.dp else 2.dp,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = option.icon),
                    contentDescription = option.title,
                    tint = iconTintColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                )
                Text(
                    text = option.description,
                    fontSize = 14.sp,
                    color = descriptionColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = colorResource(id = R.color.success),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@Composable
fun CustomizePromptStep(
    selectedOption: AIOption,
    topic: String,
    onTopicChange: (String) -> Unit,
    selectedArticleType: ArticleType,
    onArticleTypeChange: (ArticleType) -> Unit,
    customPrompt: String,
    onPromptChange: (String) -> Unit,
    selectedModel: GroqModel,
    onModelChange: (GroqModel) -> Unit,
    existingTitle: String,
    existingContent: String,
    onGenerate: () -> Unit,
    onBack: () -> Unit
) {
    val articleTypes = ArticleType.values()
    var showArticleTypeDropdown by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Help summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = selectedOption.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Getting help with: ${selectedOption.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Topic/Subject input
        Text(
            text = "What is your article about?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChange,
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = {
                Text(
                    text = "e.g., The impact of AI on modern journalism, Climate change solutions, etc.",
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
                )
            },
            label = {
                Text(
                    text = "Article Topic",
                    color = MaterialTheme.colors.onBackground
                )
            },
            maxLines = 3,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.secondary,
                unfocusedBorderColor = colorResource(id = R.color.border_color),
                focusedLabelColor = MaterialTheme.colors.secondary,
                cursorColor = MaterialTheme.colors.secondary,
                textColor = MaterialTheme.colors.onBackground,
                placeholderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
                backgroundColor = if (isSystemInDarkTheme()) Color.Transparent else Color.White
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.clearFocus()
                }
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Article Type selection
        Text(
            text = "What type of article are you writing?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            // Article type selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showArticleTypeDropdown = !showArticleTypeDropdown },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedArticleType.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onBackground
                        )
                        Text(
                            text = selectedArticleType.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select article type",
                        tint = MaterialTheme.colors.secondary
                    )
                }
            }

            // Popup for article type selection
            if (showArticleTypeDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = androidx.compose.ui.unit.IntOffset(
                        x = 0,
                        y = with(density) { 80.dp.roundToPx() }
                    ),
                    onDismissRequest = { showArticleTypeDropdown = false },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = 8.dp,
                        color = MaterialTheme.colors.surface
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(articleTypes.toList()) { type ->
                                val titlePrompt = stringResource(
                                    R.string.title_suggestion_prompt,
                                    type.displayName,
                                    topic
                                )
                                ArticleTypeDropdownItem(
                                    articleType = type,
                                    isSelected = type == selectedArticleType,
                                    onClick = {
                                        onArticleTypeChange(type)
                                        showArticleTypeDropdown = false
                                        // Generate default prompt based on selection
                                        onPromptChange(if (selectedOption == AIOption.TitleOnly) titlePrompt else "${type.systemPrompt} about $topic")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom prompt (editable)
        Text(
            text = "Customize your prompt",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))
        val titlePrompt = stringResource(
            R.string.title_suggestion_prompt,
            selectedArticleType.displayName,
            topic
        )
        OutlinedTextField(
            value = customPrompt.ifEmpty {
                if (selectedOption == AIOption.TitleOnly) titlePrompt else "${selectedArticleType.systemPrompt} about $topic"
            },
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "Enter instructions for the AI...",
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
                )
            },
            label = {
                Text(
                    text = "AI Instructions",
                    color = MaterialTheme.colors.onBackground
                )
            },
            maxLines = 5,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.secondary,
                unfocusedBorderColor = colorResource(id = R.color.border_color),
                focusedLabelColor = MaterialTheme.colors.secondary,
                cursorColor = MaterialTheme.colors.secondary,
                textColor = MaterialTheme.colors.onBackground,
                placeholderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
                backgroundColor = if (isSystemInDarkTheme()) Color.Transparent else Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "💡 You can edit this prompt to be more specific about tone, style, or requirements",
            fontSize = 12.sp,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Model selection
        Text(
            text = "Select AI Model",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            // Model selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showModelDropdown = !showModelDropdown },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedModel.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onBackground
                        )
                        Text(
                            text = selectedModel.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select model",
                        tint = MaterialTheme.colors.secondary
                    )
                }
            }

            // Popup for model selection
            if (showModelDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = androidx.compose.ui.unit.IntOffset(
                        x = 0,
                        y = with(density) { 80.dp.roundToPx() }
                    ),
                    onDismissRequest = { showModelDropdown = false },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = 8.dp,
                        color = MaterialTheme.colors.surface
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(GroqModelProvider.getModels()) { model ->
                                ModelDropdownItem(
                                    model = model,
                                    isSelected = model.id == selectedModel.id,
                                    onClick = {
                                        onModelChange(model)
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Model info card
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colorResource(id = R.color.secondaryColor_20)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Context window: ${selectedModel.contextWindow}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Existing content info if any
        if (existingTitle.isNotEmpty() || existingContent.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colorResource(id = R.color.warning_20)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colorResource(id = R.color.warning),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You have existing content. The AI will incorporate it into the generation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button - using surface background (already fixed)
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.secondary
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Text(
                    text = "Back",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Generate button - FIXED: enabled state uses solid secondary color
            Button(
                onClick = onGenerate,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                enabled = topic.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    // When enabled: use solid secondary color (bright blue)
                    backgroundColor = if (topic.isNotBlank())
                        MaterialTheme.colors.secondary
                    else
                        MaterialTheme.colors.secondary.copy(alpha = 0.3f),
                    // Text color: white on enabled, dimmed white on disabled
                    contentColor = if (topic.isNotBlank())
                        MaterialTheme.colors.onSecondary
                    else
                        MaterialTheme.colors.onSecondary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Generate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ArticleTypeDropdownItem(
    articleType: ArticleType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = if (isSelected)
            colorResource(id = R.color.secondaryColor_20)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = articleType.icon),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = articleType.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground
                )
                Text(
                    text = articleType.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = colorResource(id = R.color.success),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ModelDropdownItem(
    model: GroqModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = if (isSelected)
            colorResource(id = R.color.secondaryColor_20)
        else
            Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = colorResource(id = R.color.success),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = model.description,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colorResource(id = R.color.warning),
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = "Exit AI Assistant?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = "Your progress will be lost if you exit now.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Exit button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Exit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colorResource(id = R.color.error),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Error",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.error),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // OK button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.onSecondary
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = "OK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingDialog(
    message: String
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(200.dp)
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.secondary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "This may take a few seconds...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}