package net.crowdventures.storypop.viewmodels

import androidx.lifecycle.ViewModel
import net.crowdventures.storypop.models.ArticleType

class AIWizardViewModel : ViewModel() {

    // Step 2: Topic and article type
    var topic: String = ""
    var selectedArticleType: ArticleType = ArticleType.RESEARCH

    // Generated content
    var generatedTitle: String = ""
    var generatedContent: String = ""

    // Existing content (if any)
    var existingTitle: String = ""
    var existingContent: String = ""

}