package net.crowdventures.storypop.util

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.SharedPreferenceManager
import net.crowdventures.storypop.TextStyleManager
import net.crowdventures.storypop.adapters.ModelSpinnerAdapter
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.models.GroqModel

class AIArticleEditHelperUtil {
    companion object {
        fun handleAIRewritePressed(
            contentEditText: EditText,
            textStyleManager: TextStyleManager,
            viewModelScope: CoroutineScope,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            var selectionStart = contentEditText.selectionStart
            var selectionEnd = contentEditText.selectionEnd
            var selectionSize = selectionEnd - selectionStart

            if (selectionSize == 0 && contentEditText.text.length > 20) {
                selectionStart = 0
                selectionEnd = contentEditText.text.length - 1
                selectionSize = selectionEnd - selectionStart
            }

            val okToRephrase = selectionSize in 5..5000
            val li = LayoutInflater.from(contentEditText.context)

            if (!okToRephrase) {
                // Show error dialog
                showErrorDialog(contentEditText.context, li)
            } else {
                // Show AI rewrite prompt dialog
                showAIRewriteDialog(
                    context = contentEditText.context,
                    li = li,
                    selectedText = contentEditText.text.subSequence(selectionStart, selectionEnd)
                        .toString(),
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    contentEditText = contentEditText,
                    textStyleManager = textStyleManager,
                    viewModelScope = viewModelScope,
                    sharedPreferenceManager = sharedPreferenceManager
                )
            }
        }

        private fun showErrorDialog(context: Context, li: LayoutInflater) {
            val view = li.inflate(R.layout.error_dialog, null)
            val closeBtn = view.findViewById<ImageView>(R.id.close_btn)

            val alertDialog = AlertDialog.Builder(context)
                .setView(view)
                .create()

            val closeListener = View.OnClickListener { alertDialog.dismiss() }
            closeBtn.setOnClickListener(closeListener)

            val errorTextTv = view.findViewById<TextView>(R.id.error_text)
            errorTextTv.text = "Select between 5 to 5000 characters for AI to rewrite your text"

            val okBtn = view.findViewById<Button>(R.id.ok_btn)
            okBtn.setOnClickListener(closeListener)
            closeBtn.visibility = View.GONE

            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }

        private fun showAIRewriteDialog(
            context: Context,
            li: LayoutInflater,
            selectedText: String,
            selectionStart: Int,
            selectionEnd: Int,
            contentEditText: EditText,
            textStyleManager: TextStyleManager,
            viewModelScope: CoroutineScope,
            sharedPreferenceManager: SharedPreferenceManager
        ) {
            val view = li.inflate(R.layout.dialog_ai_rewrite_prompt, null)

            val alertDialog = AlertDialog.Builder(context)
                .setView(view)
                .create()

            // Initialize views
            val closeBtn = view.findViewById<ImageView>(R.id.close_btn)
            val selectedTextPreview = view.findViewById<TextView>(R.id.selected_text_preview)
            val promptEditText = view.findViewById<EditText>(R.id.prompt_edit_text)
            val modelSpinner = view.findViewById<Spinner>(R.id.model_spinner)
            val tokenWarningContainer =
                view.findViewById<LinearLayout>(R.id.token_warning_container)
            val tokenWarningText = view.findViewById<TextView>(R.id.token_warning_text)
            val totalChars = view.findViewById<TextView>(R.id.total_chars)
            val estimatedTokens = view.findViewById<TextView>(R.id.estimated_tokens)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancel_button)
            val rewriteBtn = view.findViewById<MaterialButton>(R.id.rewrite_btn)

            // Set up models spinner
            val models = GroqModelProvider.getModels()
            val adapter = ModelSpinnerAdapter(context, models)
            modelSpinner.adapter = adapter

            // Set default prompt
            promptEditText.setText(
                "Do not introduce new facts, assumptions, timelines, or actors that are not explicitly supported by the original text.\n" +
                        "Task\n" +
                        "1. Rewrite the following article using entirely new wording, sentence structure, and paragraph flow.\n" +
                        "2. Add background context ONLY where facts explicitly support it (e.g., named institutions, previously stated warnings, or known roles directly mentioned).\n" +
                        "3. Do NOT infer motivations, weapon types, operational scale, or political intent unless explicitly stated in the facts or original text.\n" +
                        "4. Preserve all verified details, including:\n" +
                        "   - Time and location of events\n" +
                        "   - Casualties\n" +
                        "   - Named officials and direct quotes\n" +
                        "   - Confirmed actions by authorities\n" +
                        "5. Do not generalize or speculate (e.g., “”heavily armed””, “”large-scale””, “”crucial role””) unless those descriptors appear verbatim in the facts or original text.\n" +
                        "\n" +
                        "Formatting Requirements\n" +
                        "- Output ONLY text with markdown\n" +
                        "- Do NOT include a conclusion or editorial commentary.\n"
            )

            // Display selected text preview
            val previewText = if (selectedText.length > 150) {
                selectedText.substring(0, 150) + "..."
            } else {
                selectedText
            }
            selectedTextPreview.text = previewText

            // Update model info when selection changes
            modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val model = models[position]
                    updateTokenWarning(
                        selectedText = selectedText,
                        promptText = promptEditText.text.toString(),
                        model = model,
                        tokenWarningContainer = tokenWarningContainer,
                        tokenWarningText = tokenWarningText,
                        totalChars = totalChars,
                        estimatedTokens = estimatedTokens
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Update warning when prompt changes
            promptEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val model = models[modelSpinner.selectedItemPosition]
                    updateTokenWarning(
                        selectedText = selectedText,
                        promptText = s?.toString() ?: "",
                        model = model,
                        tokenWarningContainer = tokenWarningContainer,
                        tokenWarningText = tokenWarningText,
                        totalChars = totalChars,
                        estimatedTokens = estimatedTokens
                    )
                }
            })

            // Close button listener
            val closeListener = View.OnClickListener { alertDialog.dismiss() }
            closeBtn.setOnClickListener(closeListener)
            cancelButton.setOnClickListener(closeListener)

            // Rewrite button listener
            rewriteBtn.setOnClickListener {
                val selectedModel = models[modelSpinner.selectedItemPosition]
                val customPrompt = promptEditText.text.toString()

                alertDialog.dismiss()

                val apiKey = sharedPreferenceManager.getGroqKey()
                if (apiKey == null) {
                    showEnterApiKeyDialog(
                        onSuccessCallback = object : SuccessCallback<String> {
                            override fun onSuccess(vararg param: String) {
                                val newApiKey = param.first()
                                sharedPreferenceManager.setGroqKey(newApiKey)
                                performRewriteShowLoading(
                                    newApiKey,
                                    model = selectedModel.id,
                                    promptText = customPrompt,
                                    selectionStart = selectionStart,
                                    selectionEnd = selectionEnd,
                                    li = li,
                                    view = view,
                                    contentEditText = contentEditText,
                                    textStyleManager = textStyleManager,
                                    viewModelScope = viewModelScope
                                )
                            }

                            override fun onFailure(reason: Any?) {
                                // Handle failure
                                Toast.makeText(
                                    context,
                                    "Failed to save API key",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        li = li,
                        context = context
                    )
                } else {
                    performRewriteShowLoading(
                        apiKey,
                        model = selectedModel.id,
                        promptText = customPrompt,
                        selectionStart = selectionStart,
                        selectionEnd = selectionEnd,
                        li = li,
                        view = view,
                        contentEditText = contentEditText,
                        textStyleManager = textStyleManager,
                        viewModelScope = viewModelScope
                    )
                }
            }

            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }

        private fun updateTokenWarning(
            selectedText: String,
            promptText: String,
            model: GroqModel,
            tokenWarningContainer: LinearLayout,
            tokenWarningText: TextView,
            totalChars: TextView,
            estimatedTokens: TextView
        ) {
            val totalCharsCount = selectedText.length + promptText.length + 3000 // Slack
            totalChars.text = totalCharsCount.toString()

            // Rough estimate: 4 characters ≈ 1 token (for English text)
            val estimatedTokenCount = totalCharsCount / 4
            estimatedTokens.text = estimatedTokenCount.toString()

            // Check if exceeds free tier limit (using TPM as reference)
            val exceedsLimit = estimatedTokenCount > model.tpm

            if (exceedsLimit) {
                tokenWarningContainer.visibility = View.VISIBLE
                tokenWarningText.text =
                    "Warning: Estimated token usage ($estimatedTokenCount tokens) may exceed the model's free tier limit of ${model.tpm} TPM. Consider shortening your text or prompt."
            } else {
                tokenWarningContainer.visibility = View.GONE
            }
        }

        fun showEnterApiKeyDialog(
            onSuccessCallback: SuccessCallback<String>,
            li: LayoutInflater,
            context: Context
        ) {

            val enterAPIkeyView = li.inflate(R.layout.ai_api_key_dialog, null)
            val alertDialogLoading: AlertDialog = context.let {
                val builder = AlertDialog.Builder(it)
                builder.setView(enterAPIkeyView)
                builder.setCancelable(true)
                builder.create()
            }
            val infoText = enterAPIkeyView.findViewById<TextView>(R.id.groq_link)
            infoText.movementMethod = LinkMovementMethod.getInstance();
            val closeBtn = enterAPIkeyView.findViewById<ImageView>(R.id.close_btn)
            closeBtn.setOnClickListener { alertDialogLoading.dismiss() }
            val cancelBtn = enterAPIkeyView.findViewById<Button>(R.id.cancel_btn)
            cancelBtn.setOnClickListener { alertDialogLoading.dismiss() }
            val api_key_et = enterAPIkeyView.findViewById<EditText>(R.id.api_key_et)
            // Handle connect
            val connect_btn = enterAPIkeyView.findViewById<Button>(R.id.connect_btn)
            connect_btn.setOnClickListener {
                val apiKey = api_key_et.text.toString()
                if (apiKey.isNotEmpty()) {
                    onSuccessCallback.onSuccess(apiKey)
                    alertDialogLoading.dismiss()
                }
            }
            alertDialogLoading.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialogLoading.show()
        }

        private fun showErrorDialog(context: Context, li: LayoutInflater, message: String) {
            val view = li.inflate(R.layout.error_dialog, null)
            val closeBtn = view.findViewById<ImageView>(R.id.close_btn)
            val errorTextTv = view.findViewById<TextView>(R.id.error_text)
            val okBtn = view.findViewById<Button>(R.id.ok_btn)

            val alertDialog = AlertDialog.Builder(context)
                .setView(view)
                .create()

            errorTextTv.text = message

            val closeListener = View.OnClickListener { alertDialog.dismiss() }
            closeBtn.setOnClickListener(closeListener)
            okBtn.setOnClickListener(closeListener)
            closeBtn.visibility = View.GONE

            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }

        private fun performRewriteShowLoading(
            apiKey: String,
            model: String,
            promptText: String,
            view: View,
            li: LayoutInflater,
            selectionStart: Int,
            selectionEnd: Int,
            contentEditText: EditText,
            textStyleManager: TextStyleManager,
            viewModelScope: CoroutineScope,
        ) {
            val textToRephrase = contentEditText.text.subSequence(
                selectionStart,
                selectionEnd.coerceAtMost(contentEditText.selectionStart + 1000)
            )
            val stylesToRemove = textStyleManager.getEnabledStylesSelection(
                selectionStart,
                selectionEnd, false
            )
            stylesToRemove.forEach { x -> textStyleManager.removeEnabledStyle(x) }
            val progressView = li.inflate(R.layout.ai_progress_dialog, null)
            val alertDialogLoading: AlertDialog = contentEditText.context.let {
                val builder = AlertDialog.Builder(it)
                builder.setView(progressView)
                builder.setCancelable(false)
                builder.create()
            }
            alertDialogLoading.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialogLoading.show()
            alertDialogLoading.window?.setLayout(300, 300)
            val suggestionCallbackHandler = object : SuccessCallback<List<String>> {
                override fun onSuccess(vararg param: List<String>) {
                    alertDialogLoading.dismiss()
                    val rawContent = param.first().first()
                    val (processedContent, markdownSpans) = MarkdownUtil.processMarkdownWithCorrectOffsets(
                        rawContent
                    )
                    val spanString = SpannableStringBuilder(processedContent)
                    // Apply all markdown styles
                    contentEditText.post {
                       val spans = StoryUtil.restoreSpannableFromStylingInfo(
                            view.context,
                            markdownSpans,
                            spanString,
                            false,
                            contentEditText,
                            Constants.loggedInUser!!
                        ).first
                        contentEditText.text.replace(
                            selectionStart,
                            selectionEnd,spanString)
                        textStyleManager.addEnabledStyles(spans)
                    }
                }

                override fun onFailure(reason: Any?) {
                    alertDialogLoading.dismiss()
                    showErrorDialog(
                        contentEditText.context,
                        li,
                        "Failed to rephrase your text, please try again later"
                    )
                }
            }
            AIRequestHandlerUtil.suggest(
                apiKey,
                model,
                promptText,
                contentEditText.context.applicationContext,
                viewModelScope,
                textToRephrase.toString(),
                suggestionCallbackHandler
            )
        }
    }
}