package com.example.note.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image // 导入图片图标
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dev.jeziellago.compose.markdown.Markdown // 导入 Markdown Composable
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddEditNoteScreen(
    noteId: Int?,
    onNavigateBack: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(
            (LocalContext.current.applicationContext as android.app.Application)
        )
    )
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val isEditing = noteId != null

    LaunchedEffect(key1 = noteId) {
        if (isEditing) {
            noteViewModel.getNoteById(noteId!!).collectLatest { note ->
                if (note != null) {
                    title = note.title
                    content = note.content
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑笔记" else "添加笔记") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentTimestamp = System.currentTimeMillis()
                        val noteToSave = if (isEditing) {
                            Note(id = noteId!!, title = title, content = content, timestamp = currentTimestamp)
                        } else {
                            Note(title = title, content = content, timestamp = currentTimestamp)
                        }

                        if (isEditing) {
                            noteViewModel.update(noteToSave)
                        } else {
                            noteViewModel.insert(noteToSave)
                        }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.Done, contentDescription = "保存笔记")
                    }
                }
            )
        }
    ) { paddingValues -> 
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()) // 使 Column 可滚动
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
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- 内容编辑与预览 ---
            Text("内容 (Markdown):", style = MaterialTheme.typography.labelMedium)
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .defaultMinSize(minHeight = 150.dp), // 给内容区一个最小高度
                placeholder = { Text("内容", style = MaterialTheme.typography.bodyLarge) },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Markdown 预览
            Text("预览:", style = MaterialTheme.typography.labelMedium)
            Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray).padding(8.dp)) {
                Markdown(content = content)
            }
            // --- 结束内容编辑与预览 ---

            // --- 绘图叠加区域 ---
            // TODO: 实现绘图叠加功能
            // 这需要一个复杂的自定义 Composable，能够将 Canvas 叠加在文本/Markdown 渲染器上，
            // 并处理触摸事件、路径存储和渲染。
            // 简单的 Canvas 占位符已移除，因为它无法实现所需功能。
            Spacer(modifier = Modifier.height(16.dp))
            Text("绘图功能待实现", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            // --- 结束绘图叠加区域 ---

            // --- 图片插入按钮 ---
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: 实现图片选择逻辑 */ }) {
                Icon(Icons.Default.Image, contentDescription = "插入图片")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("插入图片 (待实现)")
            }
            // --- 结束图片插入按钮 ---

        }
    }
}
