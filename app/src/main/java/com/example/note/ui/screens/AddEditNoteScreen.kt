package com.example.note.ui.screens

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.note.MainActivity // 假设 formatTimestamp 在这里
import com.example.note.data.Note
import com.example.note.data.ContentBlock // Import from data package
import com.example.note.data.contentJson // Import from data package
import com.example.note.viewmodel.NoteViewModel
import com.example.note.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.snapshots.SnapshotStateMap // Import SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf // Import mutableStateMapOf

// --- 时间戳格式化函数 (如果不在 MainActivity，移到这里或工具类) ---
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
// --- End 时间戳格式化函数 ---

// --- 比较函数移到外部 ---
private fun areBlocksMeaningfullyEqual(blocks1: List<ContentBlock>, blocks2: List<ContentBlock>): Boolean {
    // 过滤掉空的文本块进行比较
    val meaningfulBlocks1 = blocks1.filterNot { it is ContentBlock.TextBlock && it.text.isBlank() }
    val meaningfulBlocks2 = blocks2.filterNot { it is ContentBlock.TextBlock && it.text.isBlank() } // Corrected variable name and 'it'

    if (meaningfulBlocks1.size != meaningfulBlocks2.size) return false // Corrected size access
    for (i in meaningfulBlocks1.indices) {
        val b1 = meaningfulBlocks1[i]
        val b2 = meaningfulBlocks2[i]
        when {
            // 文本块比较时忽略首尾空白
            b1 is ContentBlock.TextBlock && b2 is ContentBlock.TextBlock -> {
                if (b1.text.trim() != b2.text.trim()) return false
            }
            b1 is ContentBlock.ImageBlock && b2 is ContentBlock.ImageBlock -> {
                if (b1.uri != b2.uri) return false
            }
            else -> return false // 类型不同
        }
    }
    return true
}
// --- End 比较函数 ---


@Composable
private fun BottomAppBarCenterContent(
    isEditing: Boolean,
    currentTimestamp: Long,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    // AnimatedVisibility for Timestamp
    AnimatedVisibility(
        visible = !isEditing,
        enter = EnterTransition.None, // Or your desired transition
        exit = ExitTransition.None    // Or your desired transition
    ) {
        Text(
            text = "修改于 ${formatTimestamp(currentTimestamp)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // AnimatedVisibility for Undo/Redo buttons
    AnimatedVisibility(
        visible = isEditing,
        enter = EnterTransition.None, // Or your desired transition
        exit = ExitTransition.None    // Or your desired transition
    ) {
        Row {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class)
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
    // --- State ---
    var titleState by remember { mutableStateOf(TextFieldValue("")) }
    val contentBlocks = remember { mutableStateListOf<ContentBlock>() }
    // --- NEW: State map for TextFieldValues of TextBlocks ---
    val blockTextFieldStates = remember { mutableStateMapOf<String, TextFieldValue>() }
    var currentTimestamp by remember { mutableStateOf(0L) }
    var initialTimestamp by rememberSaveable { mutableStateOf(0L) }

    // --- History State ---
    // --- CHANGED: History now stores Triple with TextFieldValue states ---
    val history = remember { mutableStateListOf<Triple<TextFieldValue, List<ContentBlock>, Map<String, TextFieldValue>>>() }
    var historyIndex by rememberSaveable { mutableStateOf(-1) }

    // --- Focus Management ---
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() } // Block index to FocusRequester
    val titleFocusRequester = remember { FocusRequester() } // Focus for title

    // --- UI State ---
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // --- Derived State ---
    val isEditing by remember { derivedStateOf { historyIndex > 0 } } // True if changes have been made after load
    val canUndo by remember { derivedStateOf { historyIndex > 0 } }
    val canRedo by remember { derivedStateOf { historyIndex < history.size - 1 } }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- NEW: Helper function to save note to database ---
    fun saveNoteToDatabase(currentTitle: TextFieldValue, currentBlocks: List<ContentBlock>) {
        coroutineScope.launch {
            val contentJsonString = try {
                val blocksToSave = currentBlocks.dropLastWhile { it is ContentBlock.TextBlock && it.text.isBlank() }
                    .ifEmpty { listOf(ContentBlock.TextBlock("")) }
                contentJson.encodeToString(blocksToSave)
            } catch (e: Exception) {
                println("Error encoding content blocks during save: ${e.message}")
                currentBlocks.joinToString("\n") { if (it is ContentBlock.TextBlock) it.text else "[Image]" }
            }

            val noteToSave = Note(
                id = noteId ?: 0,
                title = currentTitle.text,
                content = contentJsonString,
                timestamp = System.currentTimeMillis(),
                imageUris = currentBlocks.filterIsInstance<ContentBlock.ImageBlock>().map { it.uri },
            )
            noteViewModel.saveNote(noteToSave)
            println("Note saved to database triggered by history add.")
        }
    }
    // --- End NEW Helper function ---


    // --- Helper function to add state to history ---
    fun addHistoryState(newTitle: TextFieldValue, newBlocks: List<ContentBlock>) {
        val currentStateTriple = history.getOrNull(historyIndex)
        val currentHistoryBlocks = currentStateTriple?.second
        // Compare based on title text and meaningful block content
        val isMeaningfullyDifferent = currentStateTriple == null ||
                newTitle.text != currentStateTriple.first.text ||
                !areBlocksMeaningfullyEqual(newBlocks, currentHistoryBlocks ?: emptyList())

        if (isMeaningfullyDifferent) {
            println("Adding history state at index ${historyIndex + 1}")
            if (historyIndex < history.size - 1) {
                history.removeRange(historyIndex + 1, history.size)
            }
            // --- Capture current TextFieldValue states for text blocks ---
            val currentTextFieldStates = blockTextFieldStates.filterKeys { key ->
                newBlocks.any { it.id == key && it is ContentBlock.TextBlock }
            }
            // --- Add Triple to history ---
            history.add(Triple(newTitle, newBlocks.toList(), currentTextFieldStates))
            historyIndex = history.lastIndex
            println("History size: ${history.size}, Index: $historyIndex")

            // --- Trigger database save ---
            saveNoteToDatabase(newTitle, newBlocks) // Call the save function

        } else {
            println("Skipping history add: No meaningful change detected.")
        }
    }
    // --- End Helper function ---

    // --- Undo/Redo Actions ---
    fun undo() {
        if (canUndo) {
            println("Undo: Current index $historyIndex")
            historyIndex--
            // --- Restore Triple ---
            val (prevTitle, prevBlocks, prevTextFieldStates) = history[historyIndex]
            titleState = prevTitle
            contentBlocks.clear()
            contentBlocks.addAll(prevBlocks)
            // --- Restore TextFieldValue states ---
            blockTextFieldStates.clear()
            blockTextFieldStates.putAll(prevTextFieldStates)
            // --- Restore timestamp for the initial state ---
            if (historyIndex == 0) {
                currentTimestamp = initialTimestamp
            }
            println("Undo: New index $historyIndex")
        }
    }

    fun redo() {
        if (canRedo) {
            println("Redo: Current index $historyIndex")
            historyIndex++
            // --- Restore Triple ---
            val (nextTitle, nextBlocks, nextTextFieldStates) = history[historyIndex]
            titleState = nextTitle
            contentBlocks.clear()
            contentBlocks.addAll(nextBlocks)
            // --- Restore TextFieldValue states ---
            blockTextFieldStates.clear()
            blockTextFieldStates.putAll(nextTextFieldStates)
            println("Redo: New index $historyIndex")
        }
    }
    // --- End Undo/Redo ---

    // --- Image Picker Launcher ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val copyJobs = uris.map { uri ->
                    async { noteViewModel.copyImageToInternalStorage(uri) }
                }
                val newInternalUris = copyJobs.awaitAll().filterNotNull()

                if (newInternalUris.isNotEmpty()) {
                    // --- 插入图片块和后续的空文本块 ---
                    val blocksToAdd = newInternalUris.flatMap { uri ->
                        // 创建 Block 时会自动生成 ID
                        listOf(ContentBlock.ImageBlock(uri = uri), ContentBlock.TextBlock(text = ""))
                    }
                    // 简单地在末尾添加
                    // TODO: 更复杂的逻辑可以根据当前焦点位置插入
                    contentBlocks.addAll(blocksToAdd)
                    // --- End 插入逻辑 ---

                    // 添加历史记录
                    addHistoryState(titleState, contentBlocks)
                    Toast.makeText(context, "已添加 ${newInternalUris.size} 张图片", Toast.LENGTH_SHORT).show()

                    // 插入后尝试聚焦到最后一个（新添加的）文本块
                    val lastTextIndex = contentBlocks.indexOfLast { it is ContentBlock.TextBlock }
                    if (lastTextIndex != -1) {
                         focusRequesters[lastTextIndex]?.requestFocus()
                    }

                } else {
                    Toast.makeText(context, "无法添加所选图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // --- End Image Picker Launcher ---

    // --- Load note data and initialize history ---
    LaunchedEffect(noteId) {
        var initialContentLoaded = false
        if (noteId != null && noteId != 0) {
            noteViewModel.getNoteById(noteId).collect { note ->
                if (note != null && !initialContentLoaded && historyIndex == -1) { // 确保只加载一次
                    titleState = TextFieldValue(note.title)
                    initialTimestamp = note.timestamp
                    currentTimestamp = note.timestamp
                    // 解析 content JSON
                    try {
                        // 尝试解码为 List<ContentBlock>
                        val loadedBlocks = contentJson.decodeFromString<List<ContentBlock>>(note.content)
                        contentBlocks.addAll(loadedBlocks)
                        // 确保至少有一个文本块，并且末尾是文本块以便输入
                        if (contentBlocks.isEmpty() || contentBlocks.last() !is ContentBlock.TextBlock) {
                            // 创建 Block 时会自动生成 ID
                            contentBlocks.add(ContentBlock.TextBlock(text = ""))
                        }
                    } catch (e: Exception) {
                        println("Error parsing content JSON or content is not JSON: ${e.message}")
                        // 如果解析失败（可能是旧格式的纯文本），则将其放入第一个文本块
                        // 创建 Block 时会自动生成 ID
                        contentBlocks.add(ContentBlock.TextBlock(text = note.content))
                        contentBlocks.add(ContentBlock.TextBlock(text = "")) // 确保末尾有空文本块
                    }

                    // --- Initialize blockTextFieldStates ---
                    blockTextFieldStates.clear()
                    contentBlocks.forEach { block ->
                        if (block is ContentBlock.TextBlock) {
                            blockTextFieldStates[block.id] = TextFieldValue(block.text, TextRange(block.text.length))
                        }
                    }

                    // --- Add initial state to history ---
                    val initialTextFieldStates = blockTextFieldStates.toMap() // Capture initial state
                    history.add(Triple(titleState, contentBlocks.toList(), initialTextFieldStates))
                    historyIndex = 0
                    initialContentLoaded = true
                    println("Loaded note, History size: ${history.size}, Index: $historyIndex")
                }
            }
        } else {
            // 新笔记初始化
            if (historyIndex == -1) { // 确保只初始化一次
                val now = System.currentTimeMillis()
                initialTimestamp = now
                currentTimestamp = now
                // 创建 Block 时会自动生成 ID
                val initialBlock = ContentBlock.TextBlock("")
                contentBlocks.add(initialBlock)

                // --- Initialize blockTextFieldStates for new note ---
                blockTextFieldStates.clear()
                blockTextFieldStates[initialBlock.id] = TextFieldValue("", TextRange(0))

                // --- Add initial state to history ---
                val initialTextFieldStates = blockTextFieldStates.toMap()
                history.add(Triple(TextFieldValue(""), contentBlocks.toList(), initialTextFieldStates))
                historyIndex = 0
                println("Initialized new note, History size: ${history.size}, Index: $historyIndex")
                // 请求标题的焦点
                titleFocusRequester.requestFocus()
            }
        }
    }
    // --- End Load note data ---

    // --- Debounced History Saving ---
    LaunchedEffect(Unit) {
        snapshotFlow { Pair(titleState, contentBlocks.toList()) } // 观察标题和内容块列表
            .debounce(1000L) // 1秒防抖
            .distinctUntilChanged { old, new ->
                // 使用更严格的比较来决定是否触发 collectLatest
                old.first.text == new.first.text && areBlocksMeaningfullyEqual(old.second, new.second)
            }
            .filter { historyIndex != -1 } // 确保初始加载不触发
            .collectLatest { (currentTitle, currentBlocks) ->
                println("Debounced change detected. Attempting to add history.")
                addHistoryState(currentTitle, currentBlocks)
            }
    }
    // --- End Debounced History Saving ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    IconButton(onClick = {
                        // --- 保存逻辑 ---
                        val finalContentBlocks = contentBlocks.toList() // 获取当前最终状态
                        // 序列化内容块为 JSON
                        val contentJsonString = try {
                            // 过滤掉末尾的空文本块再保存
                            val blocksToSave = finalContentBlocks.dropLastWhile { it is ContentBlock.TextBlock && it.text.isBlank() }
                                .ifEmpty { listOf(ContentBlock.TextBlock("")) } // 如果全空则保存一个空文本块
                            contentJson.encodeToString(blocksToSave)
                        } catch (e: Exception) {
                            println("Error encoding content blocks: ${e.message}")
                            // 极端情况下的回退：保存纯文本
                            finalContentBlocks.joinToString("\n") {
                                if (it is ContentBlock.TextBlock) it.text else "[Image]"
                            }
                        }

                        // 判断是否有实际内容
                        val hasMeaningfulContent = titleState.text.isNotBlank() ||
                                finalContentBlocks.any { block ->
                                    (block is ContentBlock.TextBlock && block.text.isNotBlank()) || block is ContentBlock.ImageBlock
                                }

                        // 需要保存的条件：历史记录被修改过，或者新笔记且有内容
                        val shouldSave = historyIndex > 0 || (noteId == null && hasMeaningfulContent)

                        if (shouldSave) {
                            coroutineScope.launch {
                                val noteToSave = Note(
                                    id = noteId ?: 0,
                                    title = titleState.text, // 使用当前标题状态
                                    content = contentJsonString, // 保存 JSON
                                    timestamp = System.currentTimeMillis(), // 更新时间戳
                                    // 从内容块中提取 imageUris (可选，如果 Note 实体仍需此字段)
                                    imageUris = finalContentBlocks.filterIsInstance<ContentBlock.ImageBlock>().map { it.uri },
                                    // drawingOverlayData = ...
                                )
                                noteViewModel.saveNote(noteToSave)
                                onNavigateBack()
                            }
                        } else {
                            onNavigateBack() // 无需保存，直接返回
                        }
                        // --- End 保存逻辑 ---
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = { /* No actions needed here */ }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(96.dp), // 使用你期望的高度
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp) // 使用你期望的边距
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Left Icons
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(
                                Icons.Outlined.AddBox,
                                contentDescription = "添加附件",
                                modifier = Modifier.size(28.dp) // 使用你期望的大小
                            )
                        }
                        IconButton(onClick = { /* TODO: Drawing */ }) {
                            Icon(
                                Icons.Default.Brush,
                                contentDescription = "绘图",
                                modifier = Modifier.size(28.dp) // 使用你期望的大小
                            )
                        }
                    }
                    // Center Content - 调用恢复后的函数
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        BottomAppBarCenterContent(
                            isEditing = isEditing,
                            currentTimestamp = currentTimestamp,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            onUndo = ::undo,
                            onRedo = ::redo
                        )
                    }
                    // Right Icon
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            // DropdownMenuItems 保持不变
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
                                        onNavigateBack() // 新笔记直接返回
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("制作副本") },
                                leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = "制作副本") },
                                onClick = {
                                    showMoreMenu = false
                                    coroutineScope.launch {
                                        val currentContentJson = contentJson.encodeToString(contentBlocks.toList())
                                        noteViewModel.duplicateNote(noteId, titleState.text, currentContentJson)
                                        onNavigateBack()
                                    }
                                },
                                enabled = (noteId != null && noteId != 0) || titleState.text.isNotBlank() || contentBlocks.any { it !is ContentBlock.TextBlock || it.text.isNotBlank() }
                            )
                            DropdownMenuItem(
                                text = { Text("发送") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "发送") },
                                onClick = {
                                    showMoreMenu = false
                                    val shareText = buildString {
                                        appendLine(titleState.text)
                                        appendLine()
                                        contentBlocks.forEach { block ->
                                            when (block) {
                                                is ContentBlock.TextBlock -> appendLine(block.text)
                                                is ContentBlock.ImageBlock -> appendLine("[图片]")
                                            }
                                        }
                                    }
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText.trim())
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
        // --- 编辑器主体 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 应用 Scaffold 的内边距
                .padding(horizontal = 16.dp) // 左右边距
                .verticalScroll(rememberScrollState()) // 使内容可滚动
                .pointerInput(Unit) { // 在 Column 背景上检测点击
                    detectTapGestures(onTap = {
                        // 点击空白处：聚焦到最后一个文本块
                        val lastTextIndex = contentBlocks.indexOfLast { it is ContentBlock.TextBlock }
                        if (lastTextIndex != -1) {
                            focusRequesters[lastTextIndex]?.requestFocus()
                        } else {
                            // 如果没有文本块（理论上不应发生），聚焦标题
                            titleFocusRequester.requestFocus()
                        }
                    })
                }
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // 顶部间距

            // --- 标题输入框 ---
            BasicTextField(
                value = titleState,
                onValueChange = { titleState = it /* 防抖处理历史 */ },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = LocalContentColor.current),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester), // 添加 focusRequester
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (titleState.text.isEmpty()) {
                            Text("标题", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp)) // 标题和内容间距

            // --- 渲染内容块 ---
            contentBlocks.forEachIndexed { index, block ->
                // --- 使用 block.id 作为 key ---
                key(block.id) {
                    when (block) {
                        is ContentBlock.TextBlock -> {
                            // --- Get TextFieldValue from the state map ---
                            val currentTextFieldValue = blockTextFieldStates[block.id]
                                ?: TextFieldValue(block.text, TextRange(block.text.length)) // Fallback

                            val focusRequester = focusRequesters.getOrPut(index) { FocusRequester() }

                            BasicTextField(
                                // --- Bind to the value from the state map ---
                                value = currentTextFieldValue,
                                onValueChange = { newValue ->
                                    // 1. Update the state map (source of truth for TextField)
                                    blockTextFieldStates[block.id] = newValue

                                    // 2. Update the underlying contentBlocks list's text property
                                    val blockIndex = contentBlocks.indexOfFirst { it.id == block.id }
                                    if (blockIndex != -1) {
                                        val currentListBlock = contentBlocks[blockIndex]
                                        if (currentListBlock is ContentBlock.TextBlock && currentListBlock.text != newValue.text) {
                                            // Update the list model silently; UI reads from blockTextFieldStates
                                            contentBlocks[blockIndex] = currentListBlock.copy(text = newValue.text)
                                        }
                                    }
                                    // Debounce mechanism observes contentBlocks changes for history/saving
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .padding(vertical = 4.dp),
                                decorationBox = { innerTextField ->
                                    // --- Use value from state map for placeholder ---
                                    if (index == 0 && currentTextFieldValue.text.isEmpty() && contentBlocks.size == 1) {
                                         Text("内容", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    innerTextField()
                                }
                            )
                            // --- REMOVED LaunchedEffect(block.text) ---
                        }
                        is ContentBlock.ImageBlock -> {
                            // ... Image 渲染代码保持不变 ...
                            val imageUri = remember { Uri.parse(block.uri) } // 解析 URI
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUri),
                                contentDescription = "笔记图片 $index",
                                modifier = Modifier
                                    .fillMaxWidth() // 图片占满宽度
                                    .heightIn(max = 400.dp) // 限制最大高度，避免过大图片撑爆屏幕
                                    .padding(vertical = 8.dp), // 图片块的垂直间距
                                contentScale = ContentScale.FillWidth // 填充宽度，高度自适应
                            )
                        }
                    }
                }
            }
            // --- End 渲染内容块 ---

            Spacer(modifier = Modifier.height(100.dp)) // 在底部留出一些空间，避免被键盘遮挡
        }
        // --- End 编辑器主体 ---
    }

    // --- ModalBottomSheet 保持不变 ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = "拍照") },
                    modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }
                )
                ListItem(
                    headlineContent = { Text("添加图片") },
                    leadingContent = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "添加图片") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                imagePickerLauncher.launch("image/*")
                                showBottomSheet = false
                            }
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("录音") },
                    leadingContent = { Icon(Icons.Default.Mic, contentDescription = "录音") },
                    modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }
                )
                ListItem(
                    headlineContent = { Text("复选框") },
                    leadingContent = { Icon(Icons.Default.CheckBox, contentDescription = "复选框") },
                    modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }
                )
            }
        }
    }
    // --- End ModalBottomSheet ---
}

// Helper function needed for TopAppBar save logic
private fun hasMeaningfulContent(titleState: TextFieldValue, contentBlocks: List<ContentBlock>): Boolean {
    return titleState.text.isNotBlank() ||
            contentBlocks.any { block ->
                (block is ContentBlock.TextBlock && block.text.isNotBlank()) || block is ContentBlock.ImageBlock
            }
}
