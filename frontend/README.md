# 智聊空间前端

这是独立的 **Vue 3 + Vue Router + Vite** 源码工程，可以修改 .vue 页面、运行开发服务并重新构建。

本工程依据原 Nginx 网页的功能和现有后端接口重新实现。原 Nginx 目录只有打包文件，没有原始 .vue 源码，因此这里不是原开发工程的恢复副本。

## 开发与构建

准备 Node.js 22.12+（或20.19+），先启动 8080 后端：

```shell
npm ci
npm run dev
```

访问 http://localhost:5173。正式构建使用 `npm run build`，结果在 `dist/`。本机预览使用 `npm run preview`。前端检查使用 `npm test`，源码格式检查使用 `npm run format:check`，统一格式使用 `npm run format`。

## 修改位置

| 文件 | 用途 |
| --- | --- |
| src/views/HomeView.vue | 应用中心的卡片、文案和入口 |
| src/views/ChatView.vue | 四种应用的会话、附件和文档界面 |
| src/composables/useConversation.js | 会话切换、历史恢复、发送、停止和上传 |
| src/lib/api.js | 接口路径、分块解码、错误处理和文件校验 |
| src/router.js | 路由、各应用标题、提示语和示例问题 |
| src/style.css | 页面样式、配色和移动布局 |
| src/components/AppIcon.vue | 项目自己的 SVG 图标 |
| public/favicon.svg / index.html | 网页图标与初始标题 |

响应内容作为文本显示，不把模型或文档内容作为 HTML 执行。文件时长、内容与格式由后端再次校验。

## 接口与部署

浏览器向同源 /api 请求；Vite 开发和预览服务器去掉 /api 前缀并代理至 BACKEND_URL，默认 http://127.0.0.1:8080。

参考 .env.example。如果复制为 .env.local，请不要提交个人配置。VITE_ 开头的变量会公开进入浏览器，不可填写密钥。

Nginx 指向 frontend/dist，并配置 /api 代理和页面路由回退。详见根 README 与 deploy/ 中的模板。前端构建不会复制进 backend/static。

## 已有功能与限制

支持流式文本、多轮历史、同类型媒体附件、角色游戏、演示预约、PDF 上传预览/恢复和问答。历史存在本机后端，界面不提供删除；新建会话不会覆盖旧数据。没有账户隔离，演示环境不适合直接公开部署。
