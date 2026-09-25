package com.cardscanner.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardscanner.app.CardScannerApp
import com.cardscanner.app.data.Card
import com.cardscanner.app.ocr.CardParser
import com.cardscanner.app.ocr.CardTextRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private val Application.repository get() = (this as CardScannerApp).repository

class CardsViewModel(app: Application) : AndroidViewModel(app) {
    val cards: StateFlow<List<Card>> = app.repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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

/** Loads an existing card ([cardId] > 0) or reads a freshly captured [imagePath]. */
class EditCardViewModel(app: Application, private val imagePath: String?, private val cardId: Long) :
    AndroidViewModel(app) {

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (cardId > 0) {
                val existing = app.repository.get(cardId)
                _state.value = EditState(card = existing ?: Card(), loaded = true)
            } else {
                _state.value = EditState(card = Card(imagePath = imagePath), loaded = true)
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
                val lines = CardTextRecognizer.recognize(getApplication(), File(path))
                val parsed = CardParser.parse(lines)
                _state.update {
                    it.copy(
                        scanning = false,
                        error = if (lines.isEmpty()) "No text found. Try again with better light." else null,
                        card = it.card.copy(
                            name = parsed.name,
                            jobTitle = parsed.jobTitle,
                            company = parsed.company,
                            phones = parsed.phones,
                            emails = parsed.emails,
                            website = parsed.website,
                            address = parsed.address,
                            rawText = parsed.rawText,
                        ),
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
