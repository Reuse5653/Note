package com.example.note.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.note.data.Note
import com.example.note.viewmodel.NoteViewModel
import com.example.note.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: Int?,
    onNavigateBack: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(
            (LocalContext.current.applicationContext as Application)
        )
    )
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Bottom Sheet State ---
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // --- Dropdown Menu State ---
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            noteViewModel.getNoteById(noteId).collectLatest { note ->
                if (note != null) {
                    title = note.title
                    content = note.content
                    timestamp = note.timestamp
                }
            }
        } else {
            timestamp = 0L
        }
    }

    // --- Main Screen Scaffold ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title needed */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val noteToSave = Note(
                                id = noteId ?: 0,
                                title = title,
                                content = content,
                                timestamp = System.currentTimeMillis()
                            )
                            coroutineScope.launch {
                                noteViewModel.saveNote(noteToSave)
                                onNavigateBack()
                            }
                        },
                        enabled = title.isNotBlank() || content.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            showBottomSheet = true
                        }
                    }) {
                        Icon(Icons.Default.AddBox, contentDescription = "添加附件")
                    }

                    val formattedTimestamp = remember(timestamp) {
                        if (timestamp > 0) {
                            val sdf = SimpleDateFormat("修改于 HH:mm:ss", Locale.getDefault())
                            sdf.format(Date(timestamp))
                        } else {
                            "正在编辑..."
                        }
                    }
                    Text(
                        text = formattedTimestamp,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "删除") },
                                onClick = {
                                    showMoreMenu = false
                                    if (noteId != null) {
                                        coroutineScope.launch {
                                            noteViewModel.deleteNotesByIds(listOf(noteId))
                                            onNavigateBack()
                                        }
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("制作副本") },
                                leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = "制作副本") },
                                onClick = {
                                    showMoreMenu = false
                                    coroutineScope.launch {
                                        noteViewModel.duplicateNote(noteId, title, content)
                                        onNavigateBack()
                                    }
                                },
                                enabled = noteId != null
                            )
                            DropdownMenuItem(
                                text = { Text("发送") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "发送") },
                                onClick = {
                                    showMoreMenu = false
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("标题", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 1
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .defaultMinSize(minHeight = 200.dp),
                placeholder = { Text("笔记内容", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = "拍照") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("添加图片") },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = "添加图片") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("绘图") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = "绘图") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("录音") },
                    leadingContent = { Icon(Icons.Default.Mic, contentDescription = "录音") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("复选框") },
                    leadingContent = { Icon(Icons.Default.CheckBox, contentDescription = "复选框") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
