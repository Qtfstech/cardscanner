package com.cardscanner.app.ui.detail

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cardscanner.app.ui.CardDetailViewModel
import com.cardscanner.app.util.ContactExport
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(cardId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val vm: CardDetailViewModel = viewModel { CardDetailViewModel(context.applicationContext as Application, cardId) }
    val card by vm.card.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.displayName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    card?.let { c ->
                        IconButton(onClick = { ContactExport.shareVCard(context, listOf(c)) }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        IconButton(onClick = { onEdit(c.id) }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        val c = card ?: return@Scaffold
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            c.imagePath?.let(::File)?.takeIf { it.exists() }?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Card photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.padding(4.dp))
            }
            Text(c.displayName, style = MaterialTheme.typography.headlineSmall)
            if (c.jobTitle.isNotBlank()) Text(c.jobTitle, style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ContactExport.addToContacts(context, c) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to contacts")
                }
                c.phones.firstOrNull()?.let { phone ->
                    OutlinedButton(onClick = { ContactExport.dial(context, phone) }) {
                        Icon(Icons.Default.Phone, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call")
                    }
                }
            }
            HorizontalDivider()

            if (c.company.isNotBlank()) InfoRow(Icons.Default.Business, c.company)
            c.phones.forEach { p -> InfoRow(Icons.Default.Phone, p) { ContactExport.dial(context, p) } }
            c.emails.forEach { e -> InfoRow(Icons.Default.Email, e) { ContactExport.email(context, e) } }
            if (c.website.isNotBlank()) InfoRow(Icons.Default.Language, c.website) { ContactExport.openWebsite(context, c.website) }
            if (c.address.isNotBlank()) InfoRow(Icons.Default.Place, c.address) { ContactExport.openMap(context, c.address) }
            if (c.notes.isNotBlank()) InfoRow(Icons.AutoMirrored.Filled.Notes, c.notes)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this card?") },
            text = { Text("The card and its photo will be removed from this app.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
