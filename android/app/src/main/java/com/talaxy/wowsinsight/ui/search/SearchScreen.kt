package com.talaxy.wowsinsight.ui.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.ui.components.EmptySearchState
import com.talaxy.wowsinsight.ui.components.EmptyState
import com.talaxy.wowsinsight.ui.components.ErrorState
import com.talaxy.wowsinsight.ui.components.ListHeader
import com.talaxy.wowsinsight.ui.components.LoadingState
import com.talaxy.wowsinsight.ui.components.PlayerIdentity
import com.talaxy.wowsinsight.ui.components.PlayerRowCard
import com.talaxy.wowsinsight.ui.components.PlayerRowTrailing
import com.talaxy.wowsinsight.ui.components.SelectedMenuItem
import com.talaxy.wowsinsight.ui.util.UiText
import com.talaxy.wowsinsight.ui.util.labelRes
import com.talaxy.wowsinsight.ui.util.resolve
import kotlinx.coroutines.flow.Flow

private const val EXPORT_FILE_NAME = "WoWs Insight Favorites.json"

/**
 * Search / favorites home.
 *
 * The search field stays fixed below the top app bar. When the trimmed query is
 * shorter than the minimum it shows the locally saved favorites, exactly like a
 * search field that has been cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    favorites: List<FavoritePlayer>,
    messages: Flow<UiText>,
    onQueryChange: (String) -> Unit,
    onServerChange: (GameServer) -> Unit,
    onSearchNow: () -> Unit,
    onClearQuery: () -> Unit,
    onOpenPlayer: (PlayerSummary, GameServer) -> Unit,
    onToggleFavorite: (PlayerSummary, GameServer) -> Unit,
    onRemoveFavorite: (FavoritePlayer) -> Unit,
    onMoveFavorite: (Int, Int) -> Unit,
    onMoveFavoriteBy: (FavoritePlayer, Int) -> Unit,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit,
    onOpenAbout: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExport) }

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        ServerMenu(
                            server = uiState.server,
                            onSelect = {
                                onServerChange(it)
                                hideKeyboard()
                            },
                        )
                        FavoritesMenu(
                            favoritesEmpty = favorites.isEmpty(),
                            onImport = {
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            onExport = { exportLauncher.launch(EXPORT_FILE_NAME) },
                            onOpenAbout = onOpenAbout,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text(stringResource(R.string.search_prompt)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onClearQuery()
                                    hideKeyboard()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_clear_query),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchNow()
                            hideKeyboard()
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            when {
                !uiState.hasValidQuery -> FavoritesList(
                    favorites = favorites,
                    onOpen = { favorite ->
                        hideKeyboard()
                        onOpenPlayer(favorite.summary, favorite.server)
                    },
                    onRemove = onRemoveFavorite,
                    onMove = onMoveFavorite,
                    onMoveBy = onMoveFavoriteBy,
                )

                uiState.error != null -> ErrorState(
                    title = stringResource(R.string.search_error_title),
                    message = uiState.error,
                    onRetry = onSearchNow,
                )

                uiState.isSearching -> LoadingState(title = stringResource(R.string.search_searching))

                uiState.results.isEmpty() -> EmptySearchState(query = uiState.trimmedQuery)

                else -> SearchResults(
                    results = uiState.results,
                    server = uiState.server,
                    favorites = favorites,
                    onOpen = { player ->
                        hideKeyboard()
                        onOpenPlayer(player, uiState.server)
                    },
                    onToggleFavorite = { player ->
                        onToggleFavorite(player, uiState.server)
                    },
                )
            }

            if (uiState.isSearching && uiState.hasValidQuery) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<PlayerSummary>,
    server: GameServer,
    favorites: List<FavoritePlayer>,
    onOpen: (PlayerSummary) -> Unit,
    onToggleFavorite: (PlayerSummary) -> Unit,
) {
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "results-header") {
            ListHeader(
                title = stringResource(R.string.search_results_header),
                count = results.size,
            )
        }
        items(results, key = { it.accountId }) { player ->
            PlayerRowCard(
                player = player,
                server = server,
                isFavorite = FavoritePlayer.idOf(server, player.accountId) in favoriteIds,
                onClick = { onOpen(player) },
                onToggleFavorite = { onToggleFavorite(player) },
            )
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoritePlayer>,
    onOpen: (FavoritePlayer) -> Unit,
    onRemove: (FavoritePlayer) -> Unit,
    onMove: (Int, Int) -> Unit,
    onMoveBy: (FavoritePlayer, Int) -> Unit,
) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.search_empty_title),
            message = stringResource(R.string.search_empty_message),
        )
        return
    }

    var draggingId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "favorites-header") {
            ListHeader(
                title = stringResource(R.string.search_favorites_header),
                count = favorites.size,
            )
        }
        itemsIndexed(
            items = favorites,
            key = { _, favorite -> favorite.id },
        ) { index, favorite ->
            FavoriteRow(
                favorite = favorite,
                index = index,
                lastIndex = favorites.lastIndex,
                isDragging = draggingId == favorite.id,
                onOpen = { onOpen(favorite) },
                onRemove = { onRemove(favorite) },
                onMove = onMove,
                onMoveBy = onMoveBy,
                onDragStart = { draggingId = favorite.id },
                onDragEnd = { draggingId = null },
            )
        }
    }
}

@Composable
private fun FavoriteRow(
    favorite: FavoritePlayer,
    index: Int,
    lastIndex: Int,
    isDragging: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onMoveBy: (FavoritePlayer, Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val moveUpLabel = stringResource(R.string.search_favorites_move_up)
    val moveDownLabel = stringResource(R.string.search_favorites_move_down)
    val reorderLabel = stringResource(R.string.search_favorites_reorder)
    val density = LocalDensity.current
    val threshold = with(density) { 48.dp.toPx() }

    val latestIndex by rememberUpdatedState(index)
    val latestLastIndex by rememberUpdatedState(lastIndex)
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    var workingIndex = index

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) {
                        onMoveBy(favorite, -1)
                        true
                    },
                    CustomAccessibilityAction(moveDownLabel) {
                        onMoveBy(favorite, 1)
                        true
                    },
                )
            },
        shape = MaterialTheme.shapes.large,
        color = if (isDragging) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 70.dp)
                    .width(44.dp)
                    .pointerInput(favorite.id) {
                        var accumulated = 0f
                        detectDragGestures(
                            onDragStart = {
                                workingIndex = latestIndex
                                accumulated = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnDragStart()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount.y
                                if (accumulated > threshold && workingIndex < latestLastIndex) {
                                    latestOnMove(workingIndex, workingIndex + 1)
                                    workingIndex += 1
                                    accumulated -= threshold
                                } else if (accumulated < -threshold && workingIndex > 0) {
                                    latestOnMove(workingIndex, workingIndex - 1)
                                    workingIndex -= 1
                                    accumulated += threshold
                                }
                            },
                            onDragEnd = { latestOnDragEnd() },
                            onDragCancel = { latestOnDragEnd() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = reorderLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            PlayerIdentity(
                nickname = favorite.nickname,
                server = favorite.server,
                accountId = favorite.accountId,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
            PlayerRowTrailing(isFavorite = true, onToggleFavorite = onRemove)
        }
    }
}

@Composable
private fun ServerMenu(
    server: GameServer,
    onSelect: (GameServer) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.widthIn(max = 128.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.search_server_selector),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(server.labelRes()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GameServer.entries.forEach { option ->
                SelectedMenuItem(
                    text = stringResource(option.labelRes()),
                    selected = option == server,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun FavoritesMenu(
    favoritesEmpty: Boolean,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.search_transfer_menu),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_import)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onImport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_export)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                    )
                },
                enabled = !favoritesEmpty,
                onClick = {
                    expanded = false
                    onExport()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.about_title)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenAbout()
                },
            )
        }
    }
}
