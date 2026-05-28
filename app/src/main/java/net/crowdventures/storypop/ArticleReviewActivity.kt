package net.crowdventures.storypop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import net.crowdventures.storypop.dto.ArticlePrivateSource
import net.crowdventures.storypop.dto.ArticlePublicSource
import net.crowdventures.storypop.viewmodels.StylingInfo
import net.crowdventures.storypop.dto.ArticleGuidelines
import net.crowdventures.storypop.dto.GuidelineCategory
import net.crowdventures.storypop.dto.GuidelineRule
import net.crowdventures.storypop.util.AIArticleEditHelperUtil
import net.crowdventures.storypop.util.AIRequestHandlerUtil
import net.crowdventures.storypop.util.GroqModelProvider
import net.crowdventures.storypop.util.GuidelinesProvider
import net.crowdventures.storypop.util.StoryUtil
import net.crowdventures.storypop.util.SuccessCallback
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ArticleReviewActivity : BaseActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_TAGS = "extra_tags"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_STYLING_INFO = "extra_styling_info"
        const val EXTRA_PUBLIC_SOURCES = "extra_public_sources"
        const val EXTRA_PRIVATE_SOURCES = "extra_private_sources"
        const val EXTRA_STORY_UUID = "extra_story_uuid"
        const val EXTRA_ORIGINAL_UUID = "extra_original_uuid"
        const val EXTRA_PUBLISHED_SLUG = "extra_published_slug"

        fun startForReview(
            context: Context,
            title: String,
            content: String,
            tags: List<String>,
            location: String,
            stylingInfo: StylingInfo,
            publicSources: Array<String>,
            privateSources: Array<ArticlePrivateSource>,
            storyUUID: UUID,
            originalUUID: UUID,
            publishedSlug: String? = null
        ): Intent {
            return Intent(context, ArticleReviewActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putStringArrayListExtra(EXTRA_TAGS, ArrayList(tags))
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_STYLING_INFO, stylingInfo as Parcelable)
                putExtra(EXTRA_PUBLIC_SOURCES, publicSources)
                putExtra(EXTRA_PRIVATE_SOURCES, privateSources)
                putExtra(EXTRA_STORY_UUID, storyUUID.toString())
                putExtra(EXTRA_ORIGINAL_UUID, originalUUID.toString())
                putExtra(EXTRA_PUBLISHED_SLUG, publishedSlug)
            }
        }
    }

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var guidelines: ArticleGuidelines

    // Views
    private lateinit var previewTitle: TextView
    private lateinit var previewContent: TextView
    private lateinit var previewTags: TextView
    private lateinit var previewSources: TextView
    private lateinit var guidelinesCheckbox: CheckBox
    private lateinit var runAIReviewBtn: MaterialButton
    private lateinit var submitBtn: MaterialButton
    private lateinit var backBtn: MaterialButton
    private lateinit var aiReviewCard: CardView
    private lateinit var aiReviewTitle: TextView
    private lateinit var aiReviewMessage: TextView
    private lateinit var aiSuggestionsList: LinearLayout
    private lateinit var scoreContainer: LinearLayout
    private lateinit var scoresGrid: LinearLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var reviewProgressContainer: LinearLayout
    private lateinit var reviewProgressBar: ProgressBar
    private lateinit var reviewProgressText: TextView
    private lateinit var reviewCancelBtn: TextView
    private var reviewJob: Job? = null

    // Data
    private var articleTitle: String = ""
    private var articleContent: String = ""
    private var articleTags: List<String> = emptyList()
    private var articleLocation: String = ""
    private var stylingInfo: StylingInfo? = null
    private var publicSources: Array<String> = arrayOf()
    private var privateSources: Array<ArticlePrivateSource> = arrayOf()
    private var storyUUID: UUID = UUID.randomUUID()
    private var originalUUID: UUID = UUID.randomUUID()
    private var publishedSlug: String? = null

    private var aiReviewPassed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_review)

        sharedPreferenceManager = SharedPreferenceManager(this)

        initViews()
        loadIntentData()
        setupToolbar()
        setupListeners()
        loadGuidelines()
    }

    private lateinit var guidelinesCard: CardView
    private lateinit var guidelinesContent: LinearLayout
    private lateinit var guidelinesLoading: LinearLayout
    private lateinit var guidelinesLoadingStatus: TextView
    private lateinit var guidelinesError: LinearLayout
    private lateinit var guidelinesErrorMessage: TextView
    private lateinit var retryGuidelinesBtn: MaterialButton

    private fun initViews() {
        // Existing views...
        previewTitle = findViewById(R.id.preview_title)
        previewContent = findViewById(R.id.preview_content)
        previewTags = findViewById(R.id.preview_tags)
        previewSources = findViewById(R.id.preview_sources)

        // Guidelines views
        guidelinesCard = findViewById(R.id.guidelines_card)
        guidelinesContent = findViewById(R.id.guidelines_content)
        guidelinesLoading = findViewById(R.id.guidelines_loading)
        guidelinesLoadingStatus = findViewById(R.id.guidelines_loading_status)
        guidelinesError = findViewById(R.id.guidelines_error)
        guidelinesErrorMessage = findViewById(R.id.guidelines_error_message)
        retryGuidelinesBtn = findViewById(R.id.retry_guidelines_btn)

        guidelinesCheckbox = findViewById(R.id.guidelines_checkbox)
        runAIReviewBtn = findViewById(R.id.run_ai_review_btn)
        reviewProgressContainer = findViewById(R.id.review_progress_container)
        reviewProgressBar = findViewById(R.id.review_progress_bar)
        reviewProgressText = findViewById(R.id.review_progress_text)
        reviewCancelBtn = findViewById(R.id.review_cancel_btn)
        submitBtn = findViewById(R.id.submit_btn)
        backBtn = findViewById(R.id.back_btn)
        aiReviewCard = findViewById(R.id.ai_review_card)
        aiReviewTitle = findViewById(R.id.ai_review_title)
        aiReviewMessage = findViewById(R.id.ai_review_message)
        aiSuggestionsList = findViewById(R.id.ai_suggestions_list)
        scoreContainer = findViewById(R.id.score_container)
        scoresGrid = findViewById(R.id.scores_grid)

        // Setup retry button
        retryGuidelinesBtn.setOnClickListener { loadGuidelines() }

        // Ensure progress bar is properly configured
        reviewProgressBar.apply {
            isIndeterminate = true
        }
    }

    private fun loadIntentData() {
        articleTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        articleContent = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        articleTags = intent.getStringArrayListExtra(EXTRA_TAGS) ?: emptyList()
        articleLocation = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        stylingInfo = intent.getParcelableExtra(EXTRA_STYLING_INFO)
        publicSources = intent.getStringArrayExtra(EXTRA_PUBLIC_SOURCES) ?: arrayOf()
        privateSources =  intent.getParcelableArrayExtra(EXTRA_PRIVATE_SOURCES)
            ?.filterIsInstance<ArticlePrivateSource>()
            ?.toTypedArray()
            ?: arrayOf()
        storyUUID = UUID.fromString(intent.getStringExtra(EXTRA_STORY_UUID) ?: UUID.randomUUID().toString())
        originalUUID = UUID.fromString(intent.getStringExtra(EXTRA_ORIGINAL_UUID) ?: UUID.randomUUID().toString())
        publishedSlug = intent.getStringExtra(EXTRA_PUBLISHED_SLUG)

        // Update preview
        previewTitle.text = articleTitle.ifEmpty { "Untitled Story" }
        previewContent.text = articleContent.take(200) + if (articleContent.length > 200) "..." else ""

        if (articleTags.isNotEmpty()) {
            previewTags.text = "Tags: ${articleTags.joinToString(", ")}"
            previewTags.visibility = View.VISIBLE
        } else {
            previewTags.visibility = View.GONE
        }

        if (publicSources.isNotEmpty() || privateSources.isNotEmpty()) {
            val sourceCount = publicSources.size + privateSources.size
            previewSources.text = "Sources: $sourceCount source${if (sourceCount > 1) "s" else ""} provided"
            previewSources.visibility = View.VISIBLE
        } else {
            previewSources.visibility = View.GONE
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadGuidelines() {
        // Show loading state
        guidelinesContent.visibility = View.GONE
        guidelinesLoading.visibility = View.VISIBLE
        guidelinesError.visibility = View.GONE
        runAIReviewBtn.isEnabled = false
        guidelinesCheckbox.isEnabled = false

        // Store the job reference
        var statusUpdaterJob: Runnable? = null

        // Animate status text with proper cleanup
        var statusIndex = 0
        val statusMessages = listOf(
            "Fetching from server...",
            "Verifying guidelines...",
            "Loading community standards...",
            "Almost ready..."
        )

        val statusUpdater = object : Runnable {
            override fun run() {
                // Only update if still in loading state
                if (guidelinesLoading.visibility == View.VISIBLE) {
                    guidelinesLoadingStatus.text = statusMessages[statusIndex % statusMessages.size]
                    statusIndex++
                    if (statusIndex < statusMessages.size * 2) {
                        guidelinesLoadingStatus.postDelayed(this, 600)
                    }
                }
            }
        }
        guidelinesLoadingStatus.postDelayed(statusUpdater, 600)

        // Cancel the updater when done
        val loadJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    GuidelinesProvider.fetchGuidelines(this@ArticleReviewActivity)
                }
                guidelines = result

                // Stop the status updater
                guidelinesLoadingStatus.removeCallbacks(statusUpdater)

                // IMPORTANT: Hide loading and show content
                guidelinesLoading.visibility = View.GONE
                guidelinesContent.visibility = View.VISIBLE
                guidelinesError.visibility = View.GONE

                // Update the guidelines display
                updateGuidelinesDisplay()

                // Enable UI elements
                runAIReviewBtn.isEnabled = true
                guidelinesCheckbox.isEnabled = true

                // Force a layout refresh
                guidelinesContent.requestLayout()
                guidelinesCard.requestLayout()

            } catch (e: Exception) {
                // Stop the status updater
                guidelinesLoadingStatus.removeCallbacks(statusUpdater)

                // Show error state
                guidelinesLoading.visibility = View.GONE
                guidelinesContent.visibility = View.GONE
                guidelinesError.visibility = View.VISIBLE
                guidelinesErrorMessage.text = e.message ?: "Failed to load guidelines. Please check your connection."

                runAIReviewBtn.isEnabled = false
                guidelinesCheckbox.isEnabled = false
            }
        }
    }

    private fun updateGuidelinesDisplay() {
        guidelinesContent.removeAllViews()

        // Header
        val headerText = TextView(this).apply {
            text = "Community Guidelines"
            setTextColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.primaryColor))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        guidelinesContent.addView(headerText)

        // Display each category
        guidelines.categories.forEach { category ->
            val categoryView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val categoryTitle = TextView(this).apply {
                text = "${getCategoryIcon(category.id)} ${category.name}"
                setTextColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.secondaryColor))
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            }
            categoryView.addView(categoryTitle)

            val categoryDesc = TextView(this).apply {
                text = category.description
                setTextColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.secondaryTextColor))
                textSize = 12f
                setPadding(0, 0, 0, 8)
            }
            categoryView.addView(categoryDesc)

            category.subRules.forEach { rule ->
                val ruleView = TextView(this).apply {
                    text = "• ${rule.text}"
                    setTextColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.primaryTextColor))
                    textSize = 12f
                    setPadding(16, 2, 0, 2)
                }
                categoryView.addView(ruleView)
            }

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.border_color))
                setPadding(0, 12, 0, 0)
            }
            categoryView.addView(divider)

            guidelinesContent.addView(categoryView)
        }
    }
    private fun getCategoryIcon(categoryId: String): String {
        return when (categoryId) {
            "accuracy" -> "🎯"
            "clarity" -> "📝"
            "sourcing" -> "🔗"
            "relevance" -> "📰"
            "bias" -> "⚖️"
            "originality" -> "✨"
            "grammar" -> "📚"
            "ethics" -> "🛡️"
            else -> "📌"
        }
    }

    private fun setupListeners() {

        runAIReviewBtn.setOnClickListener {
            startAIReview()
        }

        reviewCancelBtn.setOnClickListener {
            reviewJob?.cancel()
            hideReviewProgress()
            runAIReviewBtn.isEnabled = true
            Toast.makeText(this, "Review cancelled", Toast.LENGTH_SHORT).show()
        }

        submitBtn.setOnClickListener {
            if (guidelinesCheckbox.isChecked){
                this.setResult(Activity.RESULT_OK)
                finish()
            }else{
                StoryUtil.showShowWarningSnackbar(
                    guidelinesCheckbox.rootView,
                    null,
                    guidelinesCheckbox,
                    this,
                    "Confirm that you have read community guidelines",
                    submitBtn
                )
            }

        }

        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun showReviewProgress(message: String) {
        runAIReviewBtn.visibility = View.GONE
        reviewProgressContainer.visibility = View.VISIBLE
        reviewProgressText.text = message
        // Ensure progress bar is visible and animating
        reviewProgressBar.isIndeterminate = true
        reviewProgressBar.visibility = View.VISIBLE
        runAIReviewBtn.isEnabled = false
    }

    private fun hideReviewProgress() {
        runAIReviewBtn.visibility = View.VISIBLE
        reviewProgressContainer.visibility = View.GONE
        runAIReviewBtn.isEnabled = true
    }
    private fun startAIReview() {
        val groqKey = sharedPreferenceManager.getGroqKey()
        if (groqKey == null) {
            val onSuccess = object : SuccessCallback<String> {
                override fun onSuccess(vararg param: String) {
                    sharedPreferenceManager.setGroqKey(param.first())
                    performAIReview(groqKey = param.first())
                }

                override fun onFailure(reason: Any?) {
                    Toast.makeText(this@ArticleReviewActivity, "Failed to save API key", Toast.LENGTH_SHORT).show()
                }
            }
            AIArticleEditHelperUtil.showEnterApiKeyDialog(onSuccess, layoutInflater, this)
        } else {
            performAIReview(groqKey)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun performAIReview(groqKey: String) {
        showReviewProgress("Analyzing your article...")
        aiReviewCard.visibility = View.GONE
        scoreContainer.visibility = View.GONE

        reviewJob = CoroutineScope(Dispatchers.Main).launch {
            // Progress updates driven by a channel so they appear immediately
            // as each stage starts — no artificial delays
            val progressSteps = listOf(
                "Checking accuracy and sources...",
                "Evaluating clarity and structure...",
                "Analyzing bias and fairness...",
                "Reviewing grammar and style...",
                "Finalizing review..."
            )

            // Build the prompt on IO thread immediately
            val prompt = withContext(Dispatchers.IO) { buildReviewPrompt() }

            // Start a progress animation coroutine that cycles through steps
            // while the AI call is in flight — cancelled as soon as result arrives
            val progressJob = launch {
                for (step in progressSteps) {
                    reviewProgressText.text = step
                    delay(1200) // only delays between steps, not before first one
                }
                // If AI takes longer than all steps, loop back
                while (true) {
                    for (step in progressSteps) {
                        reviewProgressText.text = step
                        delay(1200)
                    }
                }
            }

            // Suspend until the AI result arrives via a coroutine-friendly wrapper
            val result = suspendCancellableCoroutine<String?> { continuation ->
                val models = GroqModelProvider.getModels()
                AIRequestHandlerUtil.suggest(
                    groqKey,
                    models.first().id,
                    prompt,
                    this@ArticleReviewActivity,
                    this,
                    "",
                    object : SuccessCallback<List<String>> {
                        override fun onSuccess(vararg param: List<String>) {
                            continuation.resume(param.firstOrNull()?.firstOrNull()) {
                                // AI call finished — cancel progress animation immediately
                                progressJob.cancel()
                                hideReviewProgress()
                            }
                        }
                        override fun onFailure(reason: Any?) {
                            continuation.resume(null) {
                                progressJob.cancel()
                                hideReviewProgress()
                            }
                        }
                    }
                )
            }


            if (result != null) {
                progressJob.cancel()
                hideReviewProgress()
                displayReviewResults(result)
            }else{
                // AI call finished — cancel progress animation immediately
                hideReviewProgress()
            }
        }
    }

    private fun buildReviewPrompt(): String {
        val criteriaBuilder = StringBuilder()
        guidelines.categories.forEach { category ->
            criteriaBuilder.append("\n${category.id.toUpperCase()}: ${category.name}")
            criteriaBuilder.append("\n   Description: ${category.description}")
            criteriaBuilder.append("\n   Requirements:")
            category.subRules.forEach { rule ->
                criteriaBuilder.append("\n   - ${rule.text}")
            }
            criteriaBuilder.append("\n")
        }

        return """
            You are an AI content moderator for a journalism platform. Please review the following article against these community guidelines. Assume articles are news articles unless labelled otherwise.
            
            SCORING CRITERIA (Rate each category 0-10):
            $criteriaBuilder
            
            PASSING THRESHOLD: Article PASSES if ALL categories score >= 7.0
            
            Article Title: $articleTitle
            
            Article Content:
            $articleContent
            
            Sources provided:
            Public Sources (${publicSources.size}): ${publicSources.map { it}.joinToString(", ")}
            Private Sources (${privateSources.size})
           
            Please respond with a detailed review in the following JSON format. Be objective and fair. Provide specific, actionable suggestions for improvement.
            
            {
                "passed": boolean,
                "overall_score": float,
                "category_scores": {
                    "accuracy": float,
                    "clarity": float,
                    "sourcing": float,
                    "relevance": float,
                    "bias": float,
                    "originality": float,
                    "grammar": float,
                    "ethics": float
                },
                "suggestions": ["suggestion1", "suggestion2", "suggestion3"],
                "explanation": "detailed explanation of the review"
            }
        """.trimIndent()
    }


    private fun displayReviewResults(result: String) {
        aiReviewCard.visibility = View.VISIBLE
        aiSuggestionsList.removeAllViews()
        scoreContainer.visibility = View.VISIBLE
        scoresGrid.removeAllViews()

        try {
            val json = JSONObject(result)
            val passed = json.optBoolean("passed", false)
            val overallScore = json.optDouble("overall_score", 0.0).toFloat()
            val explanation = json.optString("explanation", "")

            val categoryScores = mutableMapOf<String, Float>()
            val scoresJson = json.optJSONObject("category_scores")
            if (scoresJson != null) {
                guidelines.categories.forEach { category ->
                    val score = scoresJson.optDouble(category.id, 0.0).toFloat()
                    categoryScores[category.name] = score
                }
            }

            val suggestions = json.optJSONArray("suggestions")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()

            aiReviewPassed = passed

            // Set title with score
            val scoreEmoji = when {
                overallScore >= 9 -> "🏆"
                overallScore >= 7 -> "✅"
                overallScore >= 5 -> "⚠️"
                else -> "❌"
            }
            aiReviewTitle.text = if (passed) {
                "$scoreEmoji Review Passed! (Score: ${"%.1f".format(overallScore)}/10)"
            } else {
                "$scoreEmoji Review Failed (Score: ${"%.1f".format(overallScore)}/10)"
            }

            aiReviewMessage.text = explanation
            aiReviewMessage.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor))

            // Display category scores
            categoryScores.forEach { (name, score) ->
                val scoreRow = layoutInflater.inflate(R.layout.item_score_row, scoresGrid, false)
                val nameView = scoreRow.findViewById<TextView>(R.id.score_name)
                val barView = scoreRow.findViewById<View>(R.id.score_bar)
                val valueView = scoreRow.findViewById<TextView>(R.id.score_value)

                nameView.text = name
                valueView.text = "${"%.1f".format(score)}/10"

                val percentage = (score / 10f).coerceIn(0f, 1f)
                val maxBarWidth = 200 // dp equivalent
                val barWidth = (percentage * maxBarWidth).toInt()
                val barColor = when {
                    score >= 7 -> R.color.success
                    score >= 5 -> R.color.warning
                    else -> R.color.error
                }

                val layoutParams = barView.layoutParams
                layoutParams.width = barWidth.coerceAtLeast(4)
                barView.layoutParams = layoutParams
                barView.setBackgroundColor(ContextCompat.getColor(this, barColor))

                scoresGrid.addView(scoreRow)
            }

            // Display suggestions
            if (suggestions.isNotEmpty()) {
                suggestions.forEach { suggestion ->
                    val suggestionView = TextView(this).apply {
                        text = "• $suggestion"
                        setTextColor(ContextCompat.getColor(this@ArticleReviewActivity, R.color.primaryTextColor))
                        textSize = 13f
                        setPadding(0, 4, 0, 4)
                    }
                    aiSuggestionsList.addView(suggestionView)
                }
            }

        } catch (e: Exception) {
            aiReviewMessage.text = "Failed to parse review results: ${e.message}"
            aiReviewMessage.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }
}