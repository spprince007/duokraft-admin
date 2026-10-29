package se.duokraft.admin.ui.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.data.net.ContentTypeDto
import se.duokraft.admin.data.net.PostDto
import se.duokraft.admin.data.net.TermDto
import se.duokraft.admin.data.repo.ContentDraft
import se.duokraft.admin.ui.components.wpText
import java.time.LocalDateTime

class ContentEditViewModel(private val c: AppContainer, private val slug: String, initialId: Long?) : ViewModel() {
    var id by mutableStateOf(initialId); private set
    var type by mutableStateOf<ContentTypeDto?>(c.site.type(slug)); private set
    var loading by mutableStateOf(true); private set
    var saving by mutableStateOf(false); private set
    var loadError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null)
    var closeRequested by mutableStateOf(false); private set

    // Editable fields
    var title by mutableStateOf("")
    var content by mutableStateOf(TextFieldValue(""))
    var excerpt by mutableStateOf("")
    var status by mutableStateOf("draft"); private set
    var scheduledAt by mutableStateOf<LocalDateTime?>(null); private set
    var featuredId by mutableStateOf(0L)
    var featuredUrl by mutableStateOf<String?>(null)
    val selectedTerms = mutableStateMapOf<String, Set<Long>>()
    val termOptions = mutableStateMapOf<String, List<TermDto>>()

    var builder by mutableStateOf("editor"); private set
    var link by mutableStateOf<String?>(null); private set
    val isElementor get() = builder == "elementor"
    val isFrontPage get() = id != null && id == c.site.site.value?.frontPageId

    private var snapshot: String = ""
    val isDirty: Boolean get() = !loading && fingerprint() != snapshot

    init { load() }

    fun load() {
        loading = true; loadError = null
        viewModelScope.launch {
            if (type == null) { c.site.refreshSite(); type = c.site.type(slug) }
            val t = type ?: run { loadError = "This content type is not available."; loading = false; return@launch }

            val currentId = id
            if (currentId != null) {
                when (val r = c.content.get(t, currentId)) {
                    is ApiResult.Ok -> fill(r.value)
                    is ApiResult.Err -> { loadError = r.message; loading = false; return@launch }
                }
            }
            t.taxonomies.forEach { tax ->
                when (val r = c.content.terms(tax.restBase)) {
                    is ApiResult.Ok -> termOptions[tax.restBase] = r.value
                    is ApiResult.Err -> message = "Couldn't load ${tax.name.lowercase()}: ${r.message}"
                }
            }
            loading = false
            snapshot = fingerprint()
        }
    }

    private fun fill(p: PostDto) {
        id = p.id
        title = wpText(p.title.raw ?: p.title.rendered)
        content = TextFieldValue(p.content.raw ?: p.content.rendered)
        excerpt = p.excerpt.raw ?: wpText(p.excerpt.rendered)
        status = p.status
        scheduledAt = if (p.status == "future") p.date?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } else null
        featuredId = p.featuredMedia
        featuredUrl = p.embedded?.featuredMedia?.firstOrNull()?.thumbUrl
        builder = p.builder
        link = p.link.takeIf { it.isNotBlank() }
        type?.taxonomies?.forEach { tax ->
            selectedTerms[tax.restBase] = when (tax.restBase) {
                "categories" -> p.categories.toSet()
                "tags" -> p.tags.toSet()
                else -> selectedTerms[tax.restBase].orEmpty()
            }
        }
    }

    fun toggleTerm(base: String, termId: Long) {
        val current = selectedTerms[base].orEmpty()
        selectedTerms[base] = if (termId in current) current - termId else current + termId
    }

    fun addTerm(base: String, name: String) {
        if (name.isBlank()) return
        val existing = termOptions[base].orEmpty().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
        if (existing != null) { selectedTerms[base] = selectedTerms[base].orEmpty() + existing.id; return }
        viewModelScope.launch {
            when (val r = c.content.createTerm(base, name)) {
                is ApiResult.Ok -> {
                    termOptions[base] = termOptions[base].orEmpty() + r.value
                    selectedTerms[base] = selectedTerms[base].orEmpty() + r.value.id
                }
                is ApiResult.Err -> message = r.message
            }
        }
    }

    /** Saves with the given status: "draft", "publish", "pending" or "future" (needs [at]). */
    fun save(targetStatus: String, at: LocalDateTime? = null) {
        val t = type ?: return
        if (title.isBlank() && t.supports.title) { message = "Please add a title first."; return }
        if (targetStatus == "future" && (at == null || at.isBefore(LocalDateTime.now()))) {
            message = "Choose a date and time in the future to schedule."; return
        }
        saving = true
        val draft = ContentDraft(
            title = title.trim(),
            content = if (isElementor) null else content.text,
            excerpt = if (t.supports.excerpt) excerpt else null,
            status = if (targetStatus == "future") "publish" else targetStatus, // WordPress makes it "future" from the date
            dateLocal = when {
                targetStatus == "future" -> requireNotNull(at).withNano(0).toString().let { if (it.length == 16) "$it:00" else it }
                status == "future" && targetStatus != "future" -> LocalDateTime.now().withNano(0).toString()
                else -> null
            },
            featuredMedia = featuredId,
            terms = selectedTerms.toMap().mapValues { it.value.toList() },
        )
        viewModelScope.launch {
            when (val r = c.content.save(t, id, draft)) {
                is ApiResult.Ok -> {
                    fill(r.value)
                    snapshot = fingerprint()
                    message = when (r.value.status) {
                        "publish" -> "${t.singularName} published"
                        "future" -> "${t.singularName} scheduled"
                        "pending" -> "Submitted for review"
                        else -> "Draft saved"
                    }
                }
                is ApiResult.Err -> message = r.message
            }
            saving = false
        }
    }

    fun trash() {
        val t = type ?: return
        val currentId = id ?: return
        saving = true
        viewModelScope.launch {
            when (val r = c.content.trash(t, currentId)) {
                is ApiResult.Ok -> { message = "Moved to trash"; snapshot = fingerprint(); closeRequested = true }
                is ApiResult.Err -> message = r.message
            }
            saving = false
        }
    }

    private fun fingerprint() = listOf(
        title, content.text, excerpt, featuredId, selectedTerms.toSortedMap().toString(),
    ).joinToString("\u0001")
}
