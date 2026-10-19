package fr.perso.managingapps.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.managingapps.AppRow
import fr.perso.managingapps.MainViewModel
import fr.perso.managingapps.R
import fr.perso.managingapps.data.UpdateStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showToken by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = viewModel::checkAll) {
                        Icon(Icons.Default.Refresh, contentDescription = "Vérifier")
                    }
                    IconButton(onClick = { showToken = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Jeton GitHub")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Summary(rows) }
            items(rows, key = { it.app.packageName }) { row ->
                AppCard(row, onInstall = {
                    if (context.packageManager.canRequestPackageInstalls()) {
                        viewModel.install(row.app)
                    } else {
                        context.openInstallPermission()
                        scope.launch { snackbar.showSnackbar("Autorise « Mes applis » à installer des applis, puis reviens") }
                    }
                })
            }
        }
    }

    if (showToken) {
        TokenDialog(
            current = token,
            onSave = {
                viewModel.setToken(it)
                showToken = false
            },
            onDismiss = { showToken = false },
        )
    }
}

/** Bilan en tête de liste. */
@Composable
private fun Summary(rows: List<AppRow>) {
    val updates = rows.count { it.status == UpdateStatus.UPDATE_AVAILABLE }
    val text = when {
        rows.any { it.checking } -> "Vérification…"
        updates == 1 -> "1 mise à jour disponible"
        updates > 1 -> "$updates mises à jour disponibles"
        rows.any { it.error != null } -> "Vérification incomplète"
        else -> "Tout est à jour"
    }
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun AppCard(row: AppRow, onInstall: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(row)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(row.app.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    row.installed?.let { "Installée : ${it.versionName ?: "?"}" } ?: "Pas installée",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    row.error != null -> Text(
                        row.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    row.remote != null -> Text(
                        publishedLabel(LocalContext.current, row.remote.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                row.download?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Action(row, onInstall)
        }
    }
}

/** Bouton ou état, à droite de la carte. */
@Composable
private fun Action(row: AppRow, onInstall: () -> Unit) {
    when {
        row.download != null -> Text("${(row.download * 100).toInt()} %", style = MaterialTheme.typography.labelLarge)
        row.installing || (row.checking && row.remote == null) ->
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        row.status == UpdateStatus.UPDATE_AVAILABLE -> Button(onClick = onInstall) { Text("Mettre à jour") }
        row.status == UpdateStatus.NOT_INSTALLED -> OutlinedButton(onClick = onInstall) { Text("Installer") }
        row.status == UpdateStatus.UP_TO_DATE -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("À jour", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Icône de l'appli installée, sinon sa première lettre. */
@Composable
private fun AppIcon(row: AppRow) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val icon = remember(row.app.packageName, row.installed?.lastUpdateTime) {
        row.installed?.let {
            runCatching {
                context.packageManager.getApplicationIcon(row.app.packageName).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()
        }
    }
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(48.dp))
    } else {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.app.name.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun TokenDialog(current: String?, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jeton GitHub") },
        text = {
            Column {
                Text(
                    "Facultatif : nécessaire seulement pour suivre un dépôt privé. Sur github.com : Settings › " +
                        "Developer settings › Fine-grained tokens, avec l'accès « Contents » en lecture seule.",
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Jeton") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** « Publiée il y a 2 heures », ou « Publiée le 12 oct. » au-delà d'une semaine. */
private fun publishedLabel(context: Context, time: Long): String {
    val now = System.currentTimeMillis()
    return if (now - time < DateUtils.WEEK_IN_MILLIS) {
        // Android écrit « Il y a 2 heures », avec une majuscule.
        "Publiée " + DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()
            .replaceFirstChar { it.lowercase() }
    } else {
        "Publiée le " + DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }
}

/** Réglage « Installer des applis inconnues » de cette appli. */
private fun Context.openInstallPermission() {
    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
}
