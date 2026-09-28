package com.cardscanner.app.ui.list

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cardscanner.app.data.Card
import com.cardscanner.app.data.headline
import com.cardscanner.app.ui.ProjectViewModel
import com.cardscanner.app.ui.ProjectsViewModel
import com.cardscanner.app.ui.projects.DeleteProjectDialog
import com.cardscanner.app.ui.projects.ProjectNameDialog
import com.cardscanner.app.util.ContactExport
import java.io.File
import androidx.compose.material3.Card as MaterialCard

/** The cards in one project, with scanning, bulk photo upload and Excel export. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectId: Long,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onOpenCard: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: ProjectViewModel = viewModel { ProjectViewModel(app, projectId) }
    val projectsVm: ProjectsViewModel = viewModel { ProjectsViewModel(app) }
    val project by vm.project.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val progress by vm.importProgress.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val projectName = project?.name.orEmpty()

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        vm.importPhotos(uris)
    }
    val exportExcel = { ContactExport.shareExcel(context, cards, projectName.ifBlank { "Cards" }) }

    val filtered = remember(cards, query) { cards.filter { it.matches(query) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "All projects") }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export to Excel") },
                            leadingIcon = { Icon(Icons.Default.GridOn, null) },
                            enabled = cards.isNotEmpty(),
                            onClick = { menuOpen = false; exportExcel() },
                        )
                        DropdownMenuItem(
                            text = { Text("Export as vCard") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            enabled = cards.isNotEmpty(),
                            onClick = { menuOpen = false; ContactExport.shareVCard(context, cards) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Start new project") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = { menuOpen = false; creating = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename project") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; renaming = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete project") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; deleting = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (cards.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = exportExcel, modifier = Modifier.weight(1f), enabled = progress == null) {
                            Icon(Icons.Default.GridOn, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export to Excel")
                        }
                        OutlinedButton(onClick = { creating = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("New project")
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = {
                    if (progress == null) {
                        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }) { Icon(Icons.Default.PhotoLibrary, "Upload card photos") }
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(Icons.Default.CameraAlt, null) },
                    text = { Text("Scan card") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            progress?.let { p ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Reading photo ${minOf(p.done + 1, p.total)} of ${p.total}…", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { p.done.toFloat() / p.total }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (cards.isEmpty()) {
                if (progress == null) EmptyState()
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search company, name, phone, email") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                Text(
                    "${filtered.size} of ${cards.size} cards",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { card -> CardRow(card) { onOpenCard(card.id) } }
                }
            }
        }
    }

    if (creating) {
        ProjectNameDialog(
            title = "Start new project",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                projectsVm.create(name, onOpenProject)
            },
        )
    }
    if (renaming) {
        ProjectNameDialog(
            title = "Rename project",
            initial = projectName,
            confirmLabel = "Rename",
            onDismiss = { renaming = false },
            onConfirm = { name ->
                renaming = false
                projectsVm.rename(projectId, name)
            },
        )
    }
    if (deleting) {
        DeleteProjectDialog(
            name = projectName,
            cardCount = cards.size,
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                vm.deleteProject(onBack)
            },
        )
    }
}


private fun Card.matches(q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.trim().lowercase()
    val digits = needle.filter(Char::isDigit)
    return listOf(name, company, jobTitle, website, address, notes).any { it.lowercase().contains(needle) } ||
        emails.any { it.contains(needle) } ||
        (digits.length >= 3 && phones.any { it.filter(Char::isDigit).contains(digits) })
}

@Composable
private fun CardRow(card: Card, onClick: () -> Unit) {
    MaterialCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val image = card.imagePath?.let(::File)?.takeIf { it.exists() }
            Box(
                Modifier.width(96.dp).height(56.dp).clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Contacts, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.headline, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOf(card.name, card.jobTitle).filter { it.isNotBlank() && it != card.headline }
                    .joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                (card.phones.firstOrNull() ?: card.emails.firstOrNull())?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Contacts, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("No cards yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap Scan card to photograph a card, or upload several card photos at once from your gallery. " +
                "Details are read on your phone, offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
