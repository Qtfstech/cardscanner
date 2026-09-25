package com.cardscanner.app.ui.edit

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cardscanner.app.ui.EditCardViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardScreen(
    imagePath: String?,
    cardId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val context = LocalContext.current
    val vm: EditCardViewModel = viewModel {
        EditCardViewModel(context.applicationContext as Application, imagePath, cardId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val card = state.card
    val leave = { vm.discard(); onBack() }
    BackHandler(onBack = leave)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cardId > 0) "Edit card" else "New card") },
                navigationIcon = {
                    IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { vm.save(onSaved) }, enabled = state.loaded && !state.scanning) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            card.imagePath?.let(::File)?.takeIf { it.exists() }?.let { file ->
                Box {
                    AsyncImage(
                        model = file,
                        contentDescription = "Card photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(12.dp)),
                    )
                }
                if (state.scanning) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Reading card…", style = MaterialTheme.typography.bodySmall)
                } else {
                    TextButton(onClick = vm::scan) {
                        Icon(Icons.Default.DocumentScanner, null)
                        Text("  Read text again")
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (!state.loaded) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                return@Column
            }

            Field("Name", card.name) { v -> vm.edit { it.copy(name = v) } }
            Field("Job title", card.jobTitle) { v -> vm.edit { it.copy(jobTitle = v) } }
            Field("Company", card.company) { v -> vm.edit { it.copy(company = v) } }
            ListField("Phone", card.phones, KeyboardType.Phone) { v -> vm.edit { it.copy(phones = v) } }
            ListField("Email", card.emails, KeyboardType.Email) { v -> vm.edit { it.copy(emails = v) } }
            Field("Website", card.website, KeyboardType.Uri) { v -> vm.edit { it.copy(website = v) } }
            Field("Address", card.address, singleLine = false) { v -> vm.edit { it.copy(address = v) } }
            Field("Notes", card.notes, singleLine = false) { v -> vm.edit { it.copy(notes = v) } }

            Button(
                onClick = { vm.save(onSaved) },
                enabled = !state.scanning,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Save card") }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Editable list of values (several phones or emails) with add and remove buttons. */
@Composable
private fun ListField(label: String, values: List<String>, keyboardType: KeyboardType, onChange: (List<String>) -> Unit) {
    val rows = values.ifEmpty { listOf("") }
    rows.forEachIndexed { index, value ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { v -> onChange(rows.toMutableList().also { it[index] = v }) },
                label = { Text(if (rows.size > 1) "$label ${index + 1}" else label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
            )
            if (index == rows.lastIndex) {
                IconButton(onClick = { onChange(rows + "") }) { Icon(Icons.Default.Add, "Add $label") }
            } else {
                IconButton(onClick = { onChange(rows.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Default.Close, "Remove $label")
                }
            }
        }
    }
}
