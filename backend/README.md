# 智聊空间后端

Java 17 / Spring Boot 3.5.5 / Spring AI 1.0.1 的 API 服务。完整配置与模型安装见仓库根 README。

从仓库根目录构建与启动：

```shell
mvn -f backend/pom.xml package
java -jar backend/target/AI-0.0.1-SNAPSHOT.jar
```

默认端口8080，仅提供API；网页由 frontend 的5173端口提供。启动时工作目录建议为仓库根，以便 tools/media.py、.venv、data 默认路径正确。

从其他目录、IDE或Linux启动时，可通过 APP_DATA_DIR、APP_MEDIA_PYTHON、APP_MEDIA_SCRIPT、APP_WHISPER_MODEL_DIR 指定绝对路径。仅使用文字、游戏和演示预约时不调用 Python 媒体工具。

后端测试：`mvn -f backend/pom.xml test`。原 static 构建资源已移出，源码只维护在 frontend/；无需向Java项目复制网页。
