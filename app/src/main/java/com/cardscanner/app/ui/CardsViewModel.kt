package com.cardscanner.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardscanner.app.CardScannerApp
import com.cardscanner.app.data.Card
import com.cardscanner.app.data.Project
import com.cardscanner.app.data.ProjectWithCount
import com.cardscanner.app.ocr.CardParser
import com.cardscanner.app.ocr.CardTextRecognizer
import com.cardscanner.app.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Application.repository get() = (this as CardScannerApp).repository

/** Reads the text on a card photo and returns [card] with the recognized fields filled in. */
private suspend fun readCard(app: Application, card: Card, photo: File): Pair<Card, Int> {
    val lines = CardTextRecognizer.recognize(app, photo)
    val parsed = CardParser.parse(lines)
    return card.copy(
        name = parsed.name,
        jobTitle = parsed.jobTitle,
        company = parsed.company,
        phones = parsed.phones,
        emails = parsed.emails,
        website = parsed.website,
        address = parsed.address,
        rawText = parsed.rawText,
    ) to lines.size
}

class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    val projects: StateFlow<List<ProjectWithCount>?> = app.repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(getApplication<Application>().repository.createProject(name)) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { getApplication<Application>().repository.renameProject(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { getApplication<Application>().repository.deleteProject(id) }
    }
}

/** Progress of a multi-photo import: [done] of [total] photos read. */
data class ImportProgress(val done: Int, val total: Int)

class ProjectViewModel(app: Application, private val projectId: Long) : AndroidViewModel(app) {
    val project: StateFlow<Project?> = app.repository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val cards: StateFlow<List<Card>> = app.repository.observeCards(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()

    /** Reads and saves every picked photo as a card in this project; the user can edit them afterwards. */
    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty() || _importProgress.value != null) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            _importProgress.value = ImportProgress(0, uris.size)
            uris.forEachIndexed { index, uri ->
                try {
                    val photo = withContext(Dispatchers.IO) { ImageStore.importFromUri(app, uri) }
                    val blank = Card(projectId = projectId, imagePath = photo.absolutePath)
                    val card = try {
                        readCard(app, blank, photo).first
                    } catch (e: Exception) {
                        blank.copy(notes = "Text could not be read from this photo")
                    }
                    app.repository.save(card)
                } catch (e: Exception) {
                    // A photo that can't be opened is skipped; the rest still import.
                }
                _importProgress.value = ImportProgress(index + 1, uris.size)
            }
            _importProgress.value = null
        }
    }

    fun deleteProject(onDone: () -> Unit) {
        viewModelScope.launch {
            getApplication<Application>().repository.deleteProject(projectId)
            onDone()
        }
    }
}

class CardDetailViewModel(app: Application, id: Long) : AndroidViewModel(app) {
    val card: StateFlow<Card?> = app.repository.observe(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        val current = card.value ?: return
        viewModelScope.launch {
            getApplication<Application>().repository.delete(current)
            onDone()
        }
    }
}

data class EditState(
    val card: Card = Card(),
    val scanning: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
)

/** Loads an existing card ([cardId] > 0) or reads a freshly captured [imagePath] into [projectId]. */
class EditCardViewModel(
    app: Application,
    private val imagePath: String?,
    private val cardId: Long,
    private val projectId: Long,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (cardId > 0) {
                val existing = app.repository.get(cardId)
                _state.value = EditState(card = existing ?: Card(), loaded = true)
            } else {
                _state.value = EditState(card = Card(projectId = projectId, imagePath = imagePath), loaded = true)
                if (imagePath != null) scan()
            }
        }
    }

    /** Runs text recognition on the card photo and fills in the fields. */
    fun scan() {
        val path = _state.value.card.imagePath ?: return
        viewModelScope.launch {
            _state.update { it.copy(scanning = true, error = null) }
            try {
                val (card, lineCount) = readCard(getApplication(), _state.value.card, File(path))
                _state.update {
                    it.copy(
                        scanning = false,
                        error = if (lineCount == 0) "No text found. Try again with better light." else null,
                        card = card.copy(notes = it.card.notes),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(scanning = false, error = "Could not read the card: ${e.message}") }
            }
        }
    }

    /** Throws away the photo of a new card the user backed out of. */
    fun discard() {
        if (cardId == 0L) imagePath?.let { File(it).delete() }
    }

    fun edit(transform: (Card) -> Card) = _state.update { it.copy(card = transform(it.card)) }

    fun save(onSaved: (Long) -> Unit) {
        val card = _state.value.card.let { c ->
            c.copy(
                name = c.name.trim(),
                jobTitle = c.jobTitle.trim(),
                company = c.company.trim(),
                phones = c.phones.map { it.trim() }.filter { it.isNotEmpty() },
                emails = c.emails.map { it.trim() }.filter { it.isNotEmpty() },
                website = c.website.trim(),
                address = c.address.trim(),
                notes = c.notes.trim(),
            )
        }
        viewModelScope.launch { onSaved(getApplication<Application>().repository.save(card)) }
    }
}
