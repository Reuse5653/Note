package com.example.note.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid // 导入 LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells // 导入 StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items // 导入 staggered grid items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.IosShare // 导入导出图标
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.note.data.Note
import com.example.note.ui.theme.NoteTheme
import com.example.note.viewmodel.NoteViewModel
import com.example.note.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement // 导入 Arrangement
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.draw.clip // 导入 clip
import androidx.compose.material.icons.filled.Redo // 导入 Redo 图标
import androidx.compose.material.icons.filled.Undo // 导入 Undo 图标
import androidx.compose.material.icons.filled.Brush // 导入 Brush 图标
import androidx.compose.material.icons.filled.Rectangle // 导入 Eraser (用 Rectangle 替代) 图标
import com.example.note.data.ContentBlock // Import ContentBlock
import com.example.note.data.contentJson // Import contentJson
import kotlinx.serialization.decodeFromString // Ensure decodeFromString is imported

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NoteListScreen(
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(
            (LocalContext.current.applicationContext as Application)
        )
    )
) {
    val notes by noteViewModel.allNotes.collectAsState()
    var isInSelectionMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { mutableStateListOf<Int>() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() } // 用于显示消息

    // State to hold IDs for the export triggered
    var idsToExportState by remember { mutableStateOf<List<Int>?>(null) }
    // --- State for export options ---
    var showExportDialog by remember { mutableStateOf(false) }
    var includeImagesExport by remember { mutableStateOf(false) }
    var includeDrawingsExport by remember { mutableStateOf(false) }
    var includeRecordingsExport by remember { mutableStateOf(false) }
    // --- End State for export options ---

    // --- ActivityResultLaunchers ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { targetUri ->
                val ids = idsToExportState
                coroutineScope.launch {
                    try {
                        // --- Get export options from state ---
                        val includeImages = includeImagesExport
                        val includeDrawings = includeDrawingsExport
                        val includeRecordings = includeRecordingsExport
                        // --- End Get export options ---

                        val jsonString = noteViewModel.exportNotesToJsonString(
                            idsToExport = ids,
                            // --- Pass flags to ViewModel ---
                            includeImages = includeImages,
                            includeDrawings = includeDrawings,
                            includeRecordings = includeRecordings
                            // --- End Pass flags ---
                        )
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                                outputStream.write(jsonString.toByteArray())
                            } ?: throw Exception("无法打开输出流")
                        }
                        val count = ids?.size ?: notes.size // Get count based on IDs or all notes
                        val message = if (ids.isNullOrEmpty()) "所有 $count 条笔记已导出" else "$count 条笔记已导出"
                        snackbarHostState.showSnackbar(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                    } finally {
                        idsToExportState = null // Reset state after handling
                        // --- Reset export option states ---
                        showExportDialog = false
                        includeImagesExport = false
                        includeDrawingsExport = false
                        includeRecordingsExport = false
                        // --- End Reset export option states ---
                    }
                }
            } ?: run { idsToExportState = null } // Reset state if user cancels
        }
    )

    // Function to show export dialog and then trigger export
    fun showExportOptionsAndTrigger(ids: List<Int>? = null) {
        idsToExportState = ids // Set the IDs to export
        // Reset options before showing dialog
        includeImagesExport = false
        includeDrawingsExport = false
        includeRecordingsExport = false
        showExportDialog = true // Show the dialog
    }

    // --- Export Options Dialog ---
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出选项") },
            text = {
                Column {
                    // TODO: Add actual image/drawing/recording fields to Note model
                    //       before enabling these checkboxes.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeImagesExport,
                            onCheckedChange = { includeImagesExport = it },
                            enabled = false // Enable when feature is implemented
                        )
                        Text("包含图片 (待实现)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeDrawingsExport,
                            onCheckedChange = { includeDrawingsExport = it },
                            enabled = false // Enable when feature is implemented
                        )
                        Text("包含绘图 (待实现)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeRecordingsExport,
                            onCheckedChange = { includeRecordingsExport = it },
                            enabled = false // Enable when feature is implemented
                        )
                        Text("包含录音 (待实现)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false // Close dialog
                        // Trigger the actual export launcher
                        val exportAll = idsToExportState.isNullOrEmpty()
                        val baseFileName = if (exportAll) "notes_export_all" else "notes_export_selected"
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val suggestedName = "${baseFileName}_$timestamp.json"
                        exportLauncher.launch(suggestedName)
                    }
                ) {
                    Text("导出")
                }
            },
            dismissButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    // --- End Export Options Dialog ---

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { sourceUri ->
                coroutineScope.launch {
                    var importedIds: List<Int>? = null // Store imported IDs for potential undo
                    try {
                        val jsonString = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                                BufferedReader(InputStreamReader(inputStream)).readText()
                            } ?: throw Exception("无法打开输入流")
                        }
                        // Call ViewModel to import, get list of new IDs
                        importedIds = noteViewModel.importNotesFromJsonString(jsonString)

                        if (importedIds != null) {
                            // Show Snackbar with Undo action
                            val result = snackbarHostState.showSnackbar(
                                message = "成功导入 ${importedIds.size} 条笔记",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Long // Give user time to react
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // If Undo is clicked, delete the imported notes
                                noteViewModel.deleteNotesByIds(importedIds)
                                snackbarHostState.showSnackbar("导入已撤销")
                            }
                        } else {
                            snackbarHostState.showSnackbar("导入失败：文件格式错误或内容无效")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("导入失败: ${e.message}")
                        // Ensure importedIds is cleared if error occurs before Snackbar
                        if (importedIds != null) {
                           // Optional: Could try to delete partially imported notes if needed, but complex
                        }
                    }
                }
            }
        }
    )
    // --- End ActivityResultLaunchers ---


    fun clearSelection() {
        selectedNoteIds.clear()
        isInSelectionMode = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // 添加 SnackbarHost
        topBar = {
            if (isInSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedNoteIds.size,
                    onCancelClick = { clearSelection() },
                    onSelectAllClick = {
                        if (selectedNoteIds.size == notes.size) {
                            selectedNoteIds.clear()
                        } else {
                            selectedNoteIds.clear()
                            selectedNoteIds.addAll(notes.map { it.id })
                        }
                    },
                    onDeleteClick = {
                        coroutineScope.launch {
                            noteViewModel.deleteNotesByIds(selectedNoteIds.toList())
                            clearSelection()
                        }
                    },
                    onExportSelectedClick = { // Show dialog before exporting selected
                        showExportOptionsAndTrigger(selectedNoteIds.toList())
                    }
                )
            } else {
                // 普通顶部栏，添加菜单
                NormalTopAppBar(
                    onExportAllClick = { // Show dialog before exporting all
                        showExportOptionsAndTrigger(null)
                    },
                    onImportClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(onClick = onAddNoteClick) {
                    Icon(Icons.Filled.Add, contentDescription = "添加笔记")
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalItemSpacing = 8.dp, // 设置项目之间的垂直间距
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 设置项目之间的水平间距
        ) {
            items(notes, key = { it.id }) { note ->
                val isSelected = selectedNoteIds.contains(note.id)
                NoteItem(
                    note = note,
                    isSelected = isSelected,
                    modifier = Modifier
                        .fillMaxWidth() // 让 NoteItem 填满网格单元宽度
                        .combinedClickable(
                            onClick = {
                                if (isInSelectionMode) {
                                    if (isSelected) selectedNoteIds.remove(note.id) else selectedNoteIds.add(note.id)
                                    if (selectedNoteIds.isEmpty()) isInSelectionMode = false
                                } else {
                                    onNoteClick(note.id)
                                }
                            },
                            onLongClick = {
                                if (!isInSelectionMode) {
                                    isInSelectionMode = true
                                    selectedNoteIds.add(note.id)
                                }
                            }
                        )
                )
            }
        }
    }
}

// 修改：普通模式下的 TopAppBar
@Composable
fun NormalTopAppBar(
    onExportAllClick: () -> Unit, // Renamed for clarity
    onImportClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("我的笔记") },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多选项"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("导出全部笔记") },
                    onClick = {
                        showMenu = false
                        onExportAllClick() // Call the renamed handler
                    }
                )
                DropdownMenuItem(
                    text = { Text("导入笔记") },
                    onClick = {
                        showMenu = false
                        onImportClick()
                    }
                )
            }
        }
    )
}


// 修改：选择模式下的 TopAppBar
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    onCancelClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportSelectedClick: () -> Unit // Added export selected callback
) {
    TopAppBar(
        title = { Text("$selectedCount 已选择") },
        navigationIcon = {
            IconButton(onClick = onCancelClick) {
                Icon(Icons.Filled.Cancel, contentDescription = "取消选择")
            }
        },
        actions = {
            // Added Export Selected button
            IconButton(onClick = onExportSelectedClick, enabled = selectedCount > 0) {
                Icon(Icons.Default.IosShare, contentDescription = "导出选中")
            }
            IconButton(onClick = onSelectAllClick) {
                Icon(Icons.Filled.SelectAll, contentDescription = "全选/取消全选")
            }
            IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) {
                Icon(Icons.Filled.Delete, contentDescription = "删除所选")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    modifier: Modifier = Modifier // Keep the modifier parameter
) {
    // --- 解析内容以生成预览文本 ---
    val contentPreview = remember(note.content) {
        try {
            val blocks = contentJson.decodeFromString<List<ContentBlock>>(note.content)
            blocks.filterIsInstance<ContentBlock.TextBlock>()
                .joinToString("\n") { it.text.trim() } // 连接所有文本块的文本，用换行符分隔
                .take(150) // 限制预览长度
        } catch (e: Exception) {
            // 解析失败，可能是旧的纯文本格式，直接显示
            note.content.take(150) // 同样限制长度
        }
    }
    // --- End 解析内容 ---

    Card(
        // --- Remove the inner combinedClickable modifier ---
        modifier = modifier // Apply the modifier passed from the outside
            .fillMaxWidth() // Ensure Card fills width if modifier doesn't specify
            .padding(vertical = 4.dp, horizontal = 8.dp), // Keep padding or adjust as needed
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // ... Column with Text elements ...
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = note.title.ifBlank { "无标题" }, // 显示标题，空则显示 "无标题"
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // --- 显示解析后的预览文本 ---
            Text(
                text = contentPreview.ifBlank { "无内容" }, // 显示内容预览，空则显示 "无内容"
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, // 限制预览行数
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // --- End 显示预览文本 ---
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTimestamp(note.timestamp), // 显示时间戳
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Preview(showBackground = true, name = "Note Item Preview")
@Composable
fun NoteItemPreview() {
    NoteTheme {
        Column {
            NoteItem(
                note = Note(id = 1, title = "未选中笔记", content = "内容...", timestamp = System.currentTimeMillis()),
                isSelected = false,
                modifier = Modifier.padding(8.dp)
            )
            NoteItem(
                note = Note(id = 2, title = "选中笔记", content = "内容...", timestamp = System.currentTimeMillis()),
                isSelected = true,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Note List Screen Preview")
@Composable
fun NoteListScreenPreview() {
    NoteTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("我的笔记") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Add, contentDescription = "添加笔记")
                }
            }
        ) { innerPadding ->
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2), // 使用 StaggeredGridCells
                modifier = Modifier.padding(innerPadding),
                verticalItemSpacing = 8.dp, // 设置项目之间的垂直间距
                horizontalArrangement = Arrangement.spacedBy(8.dp) // 设置项目之间的水平间距
            ) {
                items(
                    listOf(
                        Note(id = 1, title = "示例笔记 1", content = "内容...", timestamp = System.currentTimeMillis()),
                        Note(id = 2, title = "示例笔记 2", content = "内容...", timestamp = System.currentTimeMillis())
                    )
                ) { note ->
                    NoteItem(note = note, isSelected = false, modifier = Modifier)
                }
            }
        }
    }
}
