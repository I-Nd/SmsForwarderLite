# GitHub Actions 自动构建 APK 操作说明

本文档说明如何在你自己的 GitHub 仓库里使用新的 GitHub Actions 工作流自动构建 `SmsForwarder` 的 APK。

对应工作流文件：

- `.github/workflows/APK_Build.yml`

## 1. 这条工作流做了什么

这条工作流和仓库现有的 `Release.yml`、`Weekly_Build.yml` 不同。

它的设计目标是：

- 不依赖原作者的私有 `keystore` 仓库
- 不依赖原作者的 `TOKEN`、`XUPDATE_TOKEN`、`X_TOKEN` 等私有 secrets
- 你 fork 到自己仓库后就能直接跑
- 默认直接构建 `debug APK`
- 需要时也可以手动构建 `release APK`

它的行为如下：

- `push main` 自动构建 `debug APK`
- `pull_request -> main` 自动构建 `debug APK`
- `workflow_dispatch` 支持手动选择 `debug` 或 `release`
- 构建产物会作为 GitHub Actions Artifact 上传

## 2. 使用前提

你需要先把代码推到自己的 GitHub 仓库。

最简单的做法：

1. 新建你自己的 GitHub 仓库
2. 把当前项目推上去
3. 确认以下文件已经提交：
   - `.github/workflows/APK_Build.yml`
   - 你的项目代码

## 3. 如何触发自动构建

### 3.1 自动构建 debug APK

只要你往 `main` 分支 push，这条工作流就会自动跑。

路径：

- GitHub 仓库
- `Actions`
- 选择 `APK Build`
- 查看最近一次运行

构建完成后：

- 打开这次 workflow run
- 在页面底部 `Artifacts` 区域下载 APK

### 3.2 手动构建

如果你想手动跑：

1. 进入 GitHub 仓库的 `Actions`
2. 选择 `APK Build`
3. 点击 `Run workflow`
4. 选择 `build_type`
   - `debug`
   - `release`
5. 点击运行

注意：

- GitHub 官方文档说明，`workflow_dispatch` 只有在 workflow 文件存在于默认分支时，才可以在 UI 里手动触发。

## 4. debug 和 release 的区别

### 4.1 debug 构建

`debug` 不要求你提前准备正式签名。

这条工作流会自动：

- 生成一个临时 debug keystore
- 写入 `keystore/keystore.properties`
- 构建 `assembleDebug`

适合：

- 验证代码能不能编过
- 下载 APK 自己安装测试
- 做功能联调

### 4.2 release 构建

`release` 要求你提供自己的签名信息。

这条工作流会：

- 从 GitHub Secrets 读取你的签名文件和密码
- 生成 `keystore/keystore.properties`
- 构建 `assembleRelease`

如果你没有配好 secrets，release 会直接失败。

## 5. release 构建需要配置哪些 Secrets

如果你要构建正式 `release APK`，需要在仓库里添加以下 secrets：

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

添加路径：

1. GitHub 仓库
2. `Settings`
3. `Secrets and variables`
4. `Actions`
5. `New repository secret`

### 5.1 `SIGNING_KEYSTORE_BASE64`

这是你的 keystore 文件做 base64 编码后的内容。

本地生成方式示例：

```bash
base64 -w 0 your-release.keystore
```

macOS 如果不支持 `-w 0`，可用：

```bash
base64 your-release.keystore | tr -d '\n'
```

把输出内容完整复制到 `SIGNING_KEYSTORE_BASE64`。

### 5.2 其它三个 secret

- `SIGNING_STORE_PASSWORD`：keystore 密码
- `SIGNING_KEY_ALIAS`：key alias
- `SIGNING_KEY_PASSWORD`：key 密码

## 6. 一次完整的 release 操作

假设你已经把 release keystore 配好了：

1. 把上述 4 个 secrets 配到 GitHub 仓库
2. 进入 `Actions`
3. 选择 `APK Build`
4. 点击 `Run workflow`
5. 选择 `build_type = release`
6. 等构建完成
7. 在 `Artifacts` 下载 release APK

## 7. APK 下载位置

构建完成后，产物会出现在当前 workflow run 的 `Artifacts` 区域。

artifact 名称格式大致为：

- `SmsForwarder-debug-<run_number>`
- `SmsForwarder-release-<run_number>`

里面会包含：

- `build/app/outputs/apk/**/*.apk`
- `output-metadata.json`

## 8. 和仓库现有工作流的关系

仓库当前已有：

- `.github/workflows/Release.yml`
- `.github/workflows/Weekly_Build.yml`

这两条工作流是原项目自己的发布体系，依赖：

- 私有 keystore 仓库
- 自定义 secrets
- XUpdate 上传逻辑

如果你只是想在自己的仓库稳定构建 APK：

- 优先使用 `APK_Build.yml`
- 原来的两个工作流可以先不动

如果你不想让旧工作流继续跑，有两个办法：

### 方案 A：在 GitHub UI 中禁用

进入：

- `Actions`
- 选择旧 workflow
- `...`
- `Disable workflow`

### 方案 B：直接删掉旧 workflow 文件

删除：

- `.github/workflows/Release.yml`
- `.github/workflows/Weekly_Build.yml`

如果你准备完全切到自己的构建体系，建议最终删掉旧 workflow，避免混淆。

## 9. 这条工作流内部做了什么

工作流的大致步骤：

1. checkout 代码
2. 用 Java 17 启动 Android SDK 工具
3. 安装：
   - `platform-tools`
   - `platforms;android-33`
   - `build-tools;33.0.1`
4. 生成 `local.properties`
5. 准备签名文件
   - debug：自动生成临时 keystore
   - release：读取 GitHub secrets
6. 切换到 Java 11 进行 Gradle 构建
7. 执行：
   - `assembleDebug`
   - 或 `assembleRelease`
8. 上传 APK artifact

## 10. 常见问题

### 10.1 为什么要先用 Java 17，再切回 Java 11

Android SDK 新版命令行工具通常要求更高版本的 Java。

但这个项目当前 Gradle / AGP / 插件组合更适合用 Java 11 构建。

所以工作流拆成两段：

- Java 17：给 `sdkmanager`
- Java 11：给 `gradlew`

### 10.2 为什么默认构建 debug

因为 debug 能做到：

- 不依赖你的正式签名
- fork 到自己仓库后就能直接试跑

这对迁移最稳。

### 10.3 release 为什么失败

最常见原因：

- 没配 4 个 signing secrets
- keystore 的 base64 不完整
- alias / password 填错

### 10.4 构建通过了，但找不到 APK

先看：

- `Actions`
- 对应的 workflow run
- 页面底部 `Artifacts`

如果 artifact 上传失败，先看 `Upload APK artifact` 这一步。

## 11. 推荐操作流程

如果你是第一次接手这个项目，建议这样做：

1. 先提交并启用 `APK_Build.yml`
2. 先在 GitHub 上跑一次 `debug`
3. 确认 APK 能产出并能安装
4. 再配置 release 签名 secrets
5. 再跑 `release`
6. 最后决定是否删除旧的 `Release.yml` / `Weekly_Build.yml`

## 12. 后续可继续做的增强

如果后面你希望继续完善 GitHub Actions，可以再加这些能力：

- tag push 自动发布 GitHub Release
- 自动上传多个 ABI APK
- 构建失败时自动保留更多日志
- PR 只做编译检查，不上传 artifact
- release 构建成功后自动创建 Release 并附加 APK

当前这版先解决最核心的问题：

- 在你自己的 GitHub 仓库里，稳定地自动构建 APK
