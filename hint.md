# Note App - 开发状态与后续步骤指南

## 1. 项目概述

这是一个简单的笔记应用程序，旨在演示现代 Android 开发实践。用户可以创建、查看、编辑和删除笔记。

## 2. 技术栈

* **语言**: Kotlin
* **架构**: MVVM (Model-View-ViewModel)
* **UI**: Jetpack Compose with Material 3
* **数据持久化**: Room Persistence Library
* **异步处理**: Kotlin Coroutines (Flow, StateFlow, viewModelScope)
* **依赖注入 (手动)**: ViewModelProvider.Factory
* **导航**: Navigation Compose
* **图片加载**: Coil
* **序列化**: Kotlinx Serialization

## 3. 当前项目结构 (关键部分)

```
Note/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/note/
│   │       │   ├── data/                 # Model 层: 数据实体、DAO、数据库、仓库
│   │       │   │   ├── Note.kt           # 笔记数据实体
│   │       │   │   ├── ContentBlock.kt   # 定义笔记内容的结构块 (文本, 图片等) + JSON 配置
│   │       │   │   ├── NoteDao.kt
│   │       │   │   ├── NoteDatabase.kt   # Room 数据库定义
│   │       │   │   ├── NoteRepository.kt
│   │       │   │   └── Converters.kt     # Room 类型转换器
│   │       │   ├── ui/                   # View 层: Compose UI
│   │       │   │   ├── screens/
│   │       │   │   │   ├── NoteListScreen.kt
│   │       │   │   │   └── AddEditNoteScreen.kt # 添加/编辑屏幕
│   │       │   │   └── theme/            # Compose 主题
│   │       │   ├── viewmodel/            # ViewModel 层
│   │       │   │   └── NoteViewModel.kt
│   │       │   └── MainActivity.kt       # 应用入口, 设置 Compose UI 和导航
│   │       ├── res/
│   │       │   └── ...
│   │       └── AndroidManifest.xml
│   └── ...
├── gradle/
│   └── libs.versions.toml                # 依赖版本管理
└── ...
```

## 4. 数据结构详解

*   **`Note.kt`**: 这是笔记的核心数据类，映射到数据库的 `notes` 表。
    *   `id`: 笔记的唯一 ID (自动生成)。
    *   `title`: 笔记标题 (String)。
    *   `content`: **关键字段**。这是一个 **JSON 字符串**，它序列化了一个 `List<ContentBlock>` 对象。这使得笔记可以包含不同类型的内容块。
    *   `timestamp`: 最后修改时间戳 (Long)。
    *   `imageUris`: (已弃用/冗余) 以前用于存储图片 URI 列表，现在图片信息嵌入在 `content` 字段的 `ImageBlock` 中。
    *   `drawingOverlayData`: (可选) 用于存储绘图数据 (String?)。
*   **`ContentBlock.kt`**: 定义了笔记内容的基本构建块。
    *   这是一个 **密封类 (Sealed Class)**，意味着一个 `ContentBlock` 只能是预定义的几种类型之一。
    *   **`TextBlock`**: 包含一个 `text` 属性 (String)，代表一段文本。
    *   **`ImageBlock`**: 包含一个 `uri` 属性 (String)，代表一张图片。这个 URI 指向应用内部存储中的图片文件。
    *   **可扩展性**: 未来可以轻松添加新的块类型，如 `AudioBlock`, `TodoBlock` 等。
*   **序列化**: 使用 `kotlinx.serialization` 库 (通过 `contentJson` 实例) 将 `List<ContentBlock>` 转换为 JSON 字符串存入 `Note.content` 字段，以及从 `Note.content` 字段反序列化回 `List<ContentBlock>` 对象。

**总结**: 笔记不再是单一的纯文本，而是由一系列有序的 `ContentBlock` (文本块、图片块等) 组成。这些块的列表被序列化为 JSON 字符串存储在 `Note` 实体的 `content` 字段中。

## 5. 当前状态

* **数据层 (Model)**:
    * `Note.kt`, `ContentBlock.kt`: 定义了核心数据结构。
    * `Converters.kt`, `NoteDao.kt`, `NoteDatabase.kt`, `NoteRepository.kt`: Room 相关设置已完成，支持 `ContentBlock` JSON 的存储。
* **ViewModel 层**:
    * `NoteViewModel.kt`: 负责业务逻辑，包括加载、保存、复制、导出、导入笔记，处理 `ContentBlock` 的序列化/反序列化。
* **UI 层 (View)**:
    * `NoteListScreen.kt`:
        * 列表项现在能正确解析 `content` JSON 并显示文本预览。
        * 导出/导入功能支持包含图片数据（通过 Base64 编码）。
    * `AddEditNoteScreen.kt`:
        * 使用 `BasicTextField` 渲染 `TextBlock`。
        * 使用 `Image` Composable 渲染 `ImageBlock`。
        * **图片点击不再触发外部应用**。
        * 支持添加图片（从相册选择，复制到内部存储）。
        * 实现了基于 `contentBlocks` 变化的撤销/重做功能（通过 `history` 状态）。
* **导航**:
    * 基本导航流程已建立。
* **构建与依赖**:
    * Coil 用于加载图片。
    * Kotlinx Serialization 用于处理 JSON。

## 6. 下一步骤 (建议)

1.  **完善图片交互**:
    *   在 `AddEditNoteScreen` 中为图片块添加删除按钮。
    *   (可选) 实现图片块的重新排序。
    *   (可选) 考虑点击图片后的交互，例如全屏查看（在应用内）或编辑。
2.  **实现绘图功能**:
    *   在 `Note.kt` 添加 `drawingOverlayData: String?` (或类似字段)。
    *   创建 `DrawingScreen` 或在 `AddEditNoteScreen` 中添加 Canvas。
    *   实现绘图工具（画笔、橡皮擦、颜色选择）。
    *   将绘图结果（例如 Base64 编码的 PNG 或路径数据）保存到 `Note` 模型。
    *   在 `AddEditNoteScreen` 和 `NoteListScreen` 中渲染绘图预览。
    *   更新导出/导入以包含绘图数据。
3.  **实现其他底部菜单功能**:
    *   **拍照**: 使用 `TakePicture` Contract。
    *   **录音**: 实现录音逻辑，可能需要新的 `AudioBlock`。
    *   **复选框**: 修改 `Note` 模型和 `AddEditNoteScreen` 以支持待办事项列表，可能需要新的 `TodoBlock`。
4.  **实现顶部栏功能**:
    *   固定、提醒、归档。
5.  **优化和细节**:
    *   搜索、排序、UI 打磨、错误处理。

## 7. 给下一位 AI 的提示
* 核心数据结构是 `Note`，其 `content` 字段存储 `List<ContentBlock>` 的 JSON 字符串。
* `ContentBlock` 目前有 `TextBlock` 和 `ImageBlock` 两种类型。
* 图片存储在应用内部，`ImageBlock` 存储其 URI。
* 编辑界面 (`AddEditNoteScreen`) 可以显示和添加文本块与图片块。
* **图片点击行为已被移除**。
* **下一步可以专注于完善图片交互（如删除）或实现新的 `ContentBlock` 类型（如绘图、待办事项）**。

祝开发顺利！