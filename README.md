# 智聊空间 · Zhiliao Space

基于 **Spring Boot、Spring AI 与 Ollama** 的本地 AI 应用中心，集成文字与多媒体聊天、哄哄模拟器、演示课程预约和 PDF 文档问答。

适合个人学习、课程展示与单机演示。项目目前没有多用户登录与访问隔离，课程预约为本地演示流程。

![智聊空间首页](docs/images/home.png)

## 功能介绍

| 模块 | 已实现功能 |
| --- | --- |
| AI 聊天 | 中文流式回复、多轮上下文、历史记录恢复、图片识别、语音转写问答、视频关键帧与音轨分析 |
| 哄哄模拟器 | 自定义生气原因、AI 角色回复、服务端计分、10 轮上限、胜负判定与重新开始 |
| 课程咨询（演示） | 演示课程介绍，逐步选择课程、校区、称呼和联系电话；确认摘要后生成本地演示预约编号，可取消预约 |
| ChatPDF | 文字型 PDF 上传、逐页图片预览、原文件取回、相关片段检索问答、检索参考页与历史记录 |

所有模型推理在本机完成。正常使用时，聊天内容与附件只传给本机服务；首次安装依赖与下载模型需要联网。

### 模型与工具

| 用途 | 模型 / 工具 | 文件体积说明 |
| --- | --- | --- |
| 文字聊天、课程咨询、游戏角色回复 | `qwen2.5:1.5b` | 约 986MB |
| 图片与视频画面理解 | `qwen3-vl:2b-instruct` | 约 1.9GB |
| 语音转写 | 本地 Whisper base / faster-whisper | 模型约 145MB，另需运行依赖 |
| 音视频解码、抽帧 | PyAV | Python 依赖，包含所需 FFmpeg 库 |
| PDF 解析与逐页预览 | Apache PDFBox | Java 依赖 |

上述体积是模型文件体积，运行时内存与显存占用更高。需要支持 Qwen3-VL 的 Ollama 版本。

## 技术栈与架构

- Java 17、Spring Boot 3.5.5、Spring AI 1.0.1。
- Ollama 提供本地文字与视觉模型接口。
- Python 独立环境处理语音转写、音视频与图片。
- Vue 打包后的静态资源随 Spring Boot 一起发布，也可交给 Nginx 提供。
- 会话、预约和 PDF 存放于本地 `data/`，按模块隔离，不依赖 MySQL。当前 `pom.xml` 中保留了原工程的 MySQL 驱动依赖，但业务数据没有接入数据库。

```text
浏览器
  ├─ 8080：Spring Boot 内置前端 + /ai 接口
  └─ 5173：Nginx 静态页面 + /api → 8080 代理
                         │
                    Ollama 本地模型
                         │
             Python 媒体处理 / PDFBox 文档处理
                         │
                    data/ 本地存储
```

> **前端源码说明：** 本仓库包含可运行的前端打包资源，原始 Vue 工程未提供。不能直接在这里运行 `npm install && npm run build`。现有页面修复由 `tools/prepare_frontend.py` 维护；后续较大的前端开发建议补充原始 Vue 源码。

## 环境准备

当前启动与停止脚本面向 Windows，媒体环境以 Python 3.13 验证。需要先自行安装：

1. **JDK 17**，建议设置 `JAVA_HOME` 指向 JDK 目录。
2. **Python 3.13**，确保 `python` 命令可用。
3. **Ollama**，并启动本机服务（默认 `http://localhost:11434`）。
4. **Maven**，也可使用仓库中的 Maven Wrapper。
5. **Nginx（可选）**：只使用 8080 入口时不需要安装。

### 1. 获取代码

```powershell
git clone https://github.com/Trappedqueens/zhiliao-space.git
cd zhiliao-space
```

### 2. 创建媒体处理环境

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r tools/media-requirements.lock.txt
```

依赖锁定文件使用与 faster-whisper 兼容的 `av==18.1.0`，避免 PyAV 19 与当前转写版本的接口兼容问题。

### 3. 下载本地模型

打开 Ollama 后运行：

```powershell
ollama pull qwen2.5:1.5b
ollama pull qwen3-vl:2b-instruct
.\.venv\Scripts\python.exe tools/media.py --download
```

第三条命令会将 Whisper base 下载至 `data/models/whisper-base/`。模型不包含在 Git 仓库中。

### 4. 启动：推荐先使用 8080

```powershell
.\start.ps1 -Build -BackendOnly
```

打开 [http://localhost:8080/](http://localhost:8080/)，即可使用前端页面与后端接口。

该模式无需 Nginx。脚本优先使用已安装的 Maven，没有 Maven 命令时使用 `mvnw.cmd`。也可以直接运行：

```powershell
.\mvnw.cmd spring-boot:run
```

直接通过 Maven 启动的进程，请在对应终端停止；它不会自动登记到启停脚本的 PID 文件中。

### 5. 使用 Nginx 的 5173 入口（可选）

先将 Nginx 解压到任意目录。若后端已运行，请先停止：

```powershell
.\stop.ps1
$env:NGINX_HOME = 'C:\tools\nginx'
.\start.ps1
```

或者：

```powershell
.\start.ps1 -NginxHome 'C:\tools\nginx'
```

打开 [http://localhost:5173/](http://localhost:5173/)。

启动脚本会根据项目目录与 Nginx 目录，从 `deploy/nginx.conf.template` 生成本地 `deploy/nginx.conf`，无需提交个人电脑的绝对路径。Nginx 使用项目独立的运行目录与配置；请避免同时启动另一个占用 5173 的默认实例。

### 停止与更新

```powershell
.\stop.ps1
# 修改代码后重新构建；需要 8080 模式时加上 -BackendOnly
.\start.ps1 -Build
```

脚本只停止 PID 文件指向且进程命令符合本项目路径的实例。Ollama 由用户单独管理。

## 使用方法

### AI 聊天

新建对话后输入文字即可。附件入口支持同一次最多 3 个同类型文件：

- 图片：每个最多 10MB。
- 音频：每个最多 10MB、180 秒；先本地转写，再由文字模型回答。
- 视频：每个最多 150MB、40 秒；抽取最多 4 个画面，并在存在音轨时转写语音。

对话完成后保存历史；模型或媒体处理失败时页面会显示错误。本地模型能力有限，复杂推理、精确 OCR 和媒体理解可能出错。视频抽样可能遗漏关键动作。

### 哄哄模拟器

填写生气原因并开始游戏。共 10 轮，倾听、道歉与具体行动会影响原谅值；游戏分数由程序维护，角色台词由模型生成，模型不可用时使用内置回复。

### 课程咨询与演示预约

输入 `预约试听`，按提示选择课程、校区，填写称呼与联系电话，最后回复 `确认预约`。可回复 `取消预约`。

**所有课程、价格、校区及预约均为演示数据**，不会向真实学校提交，不会发送短信或收取费用。可以使用演示号码 `13800000000`，不必提供真实个人资料。

### PDF 问答

上传可提取文字的 PDF，即可在左侧预览文档、在右侧提问。窄窗口会改为上下布局。

- 文件最多 20MB、200 页、100 万文字。
- 暂不支持扫描件与加密 PDF。
- 通过相关文字片段检索构建回答，末尾提供检索参考页。
- 参考页代表提供给模型的片段来源，不是对生成答案准确性的保证。
- PDF 预览由本机后端生成逐页图片，不要求浏览器安装 PDF 插件。
- 历史会话可恢复原文档和问答记录。

## 配置

主要配置位于 `src/main/resources/application.yml`：

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama 服务地址 |
| `spring.ai.ollama.chat.model` | `qwen2.5:1.5b` | 文字模型 |
| `app.vision-model` | `qwen3-vl:2b-instruct` | 视觉模型 |
| `app.media-python` | `.venv/Scripts/python.exe` | 媒体处理 Python |
| `app.data-dir` | `./data` | 本地数据目录 |

文字模型使用最近最多 6 轮历史作为上下文。完整历史保存在本地；PDF 和媒体提取结果参与当前问答。长消息和复杂文档仍受小模型上下文容量限制。

## 目录结构

```text
zhiliao-space/
├─ src/main/java/                 后端接口、会话、预约、文档和媒体服务
├─ src/main/resources/
│  ├─ application.yml             默认配置
│  └─ static/                     已打包前端与图标
├─ src/test/java/                 自动化测试
├─ tools/
│  ├─ media.py                    语音转写、图片与视频预处理
│  ├─ media-requirements.lock.txt  已验证的 Python 依赖
│  ├─ prepare_frontend.py         对现有前端资源做兼容修复
│  ├─ configure_nginx.ps1         生成当前电脑的 Nginx 配置
│  ├─ make_speech_fixture.ps1     生成合成测试语音
│  └─ smoke_test.py               本地端到端测试
├─ deploy/nginx.conf.template     不含个人目录的 Nginx 模板
├─ docs/images/home.png           首页截图
├─ start.ps1 / stop.ps1           Windows 启停脚本
└─ pom.xml
```

## 测试

### Java 测试

```powershell
.\mvnw.cmd test
```

当前有 **6 项测试**，覆盖应用上下文、会话持久化与模块隔离、预约确认及重复提交、游戏计分、PDF 检索、逐页预览和无效页码。测试不需要实际调用模型。

### 端到端测试

项目通过 5173 运行并安装全部模型后：

```powershell
.\.venv\Scripts\python.exe tools/smoke_test.py --fixtures
.\tools\make_speech_fixture.ps1
.\.venv\Scripts\python.exe tools/smoke_test.py --media
```

测试使用合成图片、语音、视频和 PDF，检查多轮记忆、历史隔离、游戏计分、完整预约流程、PDF 上传取回及问答、图片识别、语音问答与视频问答。

测试会在 `data/` 中生成 `smoke-` 开头的会话；结果在 `target/smoke-results.json`。小模型回答具有生成性，端到端测试对内容的判断可能随模型输出变化。上述测试不能替代完整的并发与安全验证。

## 数据、日志与限制

- `data/` 存放聊天、游戏状态、完整预约资料（包括完整电话）、上传 PDF 与 Whisper 模型。
- Java 日志位于 `target/app.log`、`target/app-error.log`；Nginx 日志位于 `target/nginx-access.log`、`target/nginx-error.log`。
- 当前没有登录、多用户权限或数据库事务，适合可信环境中的单机演示。
- 部分接口沿用 GET 形式传递问题，问题可能进入本地访问日志；请不要用它处理敏感资料。
- 不建议将当前版本直接暴露到公网。正式使用前需补齐身份认证、用户隔离、日志处理和数据管理。
- 仓库不包含模型、聊天记录、预约资料、上传 PDF、虚拟环境、构建产物、本机 Nginx 配置或备份。请通过 Git 上传项目，手动压缩整个目录不会应用 `.gitignore`。

## 常见问题

**提示视觉模型需要更新 Ollama？**
安装支持 Qwen3-VL 的 Ollama 版本，确认 `ollama list` 能看到视觉模型。

**语音转写报错或找不到 Python？**
检查 `.venv/Scripts/python.exe`、依赖安装和 `data/models/whisper-base/`；必要时重新按锁定文件安装依赖并下载模型。

**Nginx 找不到或路径不正确？**
设置 `NGINX_HOME` 为包含 `nginx.exe` 的目录，或在启动时传入 `-NginxHome`。配置会由模板重新生成。也可以使用 `-BackendOnly` 直接访问 8080。

**页面仍显示旧名称或旧脚本？**
强制刷新页面。前端兼容脚本为修改后的模块生成了新 URL，便于避开旧浏览器缓存。

**为什么没有原始 Vue 源码？**
当前工程最初只提供 Nginx 目录中的前端构建产物；本仓库在其基础上实现兼容修复与后端功能，保留了现有资源及第三方声明。
