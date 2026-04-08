# SmsForwarder 短信专用版改造执行档

## 1. 文档定位

本文档不是泛泛而谈的方案说明，而是当前 `SmsForwarder` 短信专用版改造的长期执行档。

它必须同时承担 4 个用途：

- 说明为什么要做短信专用版
- 记录已经完成的改动和明确决策
- 约束未来继续裁切时不能踩的坑
- 在后续追踪上游改动时，作为“差异面地图”和“回补准则”

目标要求是：

- 以后只凭这份文档，就能知道当前版本的设计目标、已改内容、保留边界、禁用边界和后续该怎么继续做
- 后续跟随上游同步时，可以基于本文档快速判断哪些改动必须保留，哪些改动要重新补回

## 2. 项目基线

### 2.1 上游基线

- 上游仓库：`https://github.com/pppscn/SmsForwarder`
- 分析起点分支：`main`
- 起始分析提交：`523b29a1bb7eb1a79ce156524287db1e85b86b65`
- 起始分析提交时间：`2026-03-02 21:38:56 +0800`

### 2.2 当前派生仓库

- 当前派生仓库名：`SmsForwarderLite`
- 当前 GitHub 仓库地址：`git@github.com:I-Nd/SmsForwarderLite.git`
- 当前应用显示名：`ForwarderLite`
- 当前 Android 安装包名：`ik.loong.forwarder`
- 当前前台通知默认文案：`服务正在运行中`

### 2.3 上游原始功能范围

上游并不是单一短信工具，而是“监听 + 分发 + 远控 + 自动化 + 保活”的综合工具，至少包含：

- 短信监听与转发
- 通话/来电监听与转发
- APP 通知监听与转发
- 发送器系统
- 规则系统
- HTTP 服务端与客户端
- FRP
- 自动任务
- 蓝牙/定位/网络/锁屏/电量等触发器
- Cactus、前台服务、开机启动、电池优化等保活能力

## 3. 本项目的目标

### 3.1 目标形态

当前目标不是“做一个全新 App”，而是把上游项目裁成一个“短信专版”。

专版保留：

- 短信输入
- 短信规则
- 发送器
- 日志
- 必要设置
- 强保活能力

### 3.2 专版必须禁用的能力

以下能力必须视为下线：

- 通话转发
- APP 通知转发
- 短信指令
- 远程控制服务端
- 远程控制客户端
- HTTP API
- FRP
- 自动任务
- 纯客户端模式
- 纯任务模式
- 蓝牙
- 定位
- 电量/网络/锁屏触发类自动化
- 应用列表加载
- 非短信类型规则

### 3.3 “下线”的判定标准

本项目中，“下线”不要求把所有旧代码删除，而是要求满足：

- UI 中不可见
- 用户无法开启
- 进程启动后不会自动注册/自动拉起
- Manifest 不再暴露相关组件时，系统无法直接唤起
- 即使数据库里残留旧配置，也不会真正生效

## 4. 非目标

当前阶段明确不做：

- 不做大规模架构重写
- 不清理所有无用类、无用表、无用资源、无用依赖
- 不追求“最优雅”的工程结构
- 不优先补测试
- 不优先删除抽屉菜单相关适配器和旧 Fragment 文件

当前优先级始终是：

1. 功能边界正确
2. 用户界面不混乱
3. 后台不会误活
4. 保活不能被误伤
5. 差异面尽量小，便于跟上游

## 5. 核心设计判断

### 5.1 为什么这个项目适合裁切

因为它的“输入源”和“转发核心”是分离的。

- 短信入口：`receiver/SmsReceiver.kt`
- 统一分发：`workers/SendWorker.kt`
- 规则匹配：`database/entity/Rule.kt`
- 发送器调度：`utils/SendUtils.kt`

这意味着：

- 不需要重写短信转发核心
- 主要工作是切掉其他入口、隐藏其他 UI、阻断其他后台逻辑

### 5.2 为什么保活必须保留

当前产品目标不是“极简短信 App”，而是“短信专版 + 强保活”。

所以这条要求是硬约束：

- `Cactus`
- 前台服务
- 静音音乐
- 一像素
- 开机启动
- 电池优化引导

这些保活能力不能因为裁短信专版而被误删或误关闭。

这条约束是当前文档里最重要的风险控制项之一。

## 6. 已实施的改造

本节记录当前已经落地的改造，不是计划，而是现状。

### 6.1 增加短信专版模式总开关

新增文件：

- `app/src/main/kotlin/cn/ppps/forwarder/utils/SmsOnlyMode.kt`

作用：

- 通过 `SmsOnlyMode.isEnabled = true` 标识当前为短信专版
- 在应用启动时强制写死非短信功能相关开关

当前固定关闭的功能包括：

- `enablePhone`
- `enableCallType1..6`
- `enableAppNotify`
- `enableSmsCommand`
- `enableCloseToEarpieceTurnOffScreen`
- `enableCancelAppNotify`
- `enableNotUserPresent`
- `enableLoadAppList`
- `enableLoadUserAppList`
- `enableLoadSystemAppList`
- `enablePureClientMode`
- `enablePureTaskMode`
- `enableLocation`
- `enableBluetooth`
- `enableServerAutorun`
- 全部 `HttpServerUtils.enableApi*`

注意：

- `enableCactus`
- `enablePlaySilenceMusic`
- `enableOnePixelActivity`

这三个**不能**在短信专版中被强行写成 `false`，因为保活必须保留。

这条是实际踩过的坑，后续不能再犯。

### 6.2 App 启动入口裁切

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/App.kt`

已做事项：

- 在 `initLibs()` 后立即调用 `SmsOnlyMode.enforceLockedConfig()`
- 仍然初始化 `WorkManager`
- 仍然启动 `ForegroundService`
- 在短信专版模式下，阻断以下启动链：
  - `HttpServerService`
  - `LocationService`
  - `BatteryReceiver`
  - `BluetoothReceiver`
  - `BluetoothScanService`
  - `NetworkChangeReceiver`
  - `LockScreenReceiver`
  - `ProximitySensorScreenHelper`
  - FRPC 动态库加载

保留事项：

- `Cactus` 初始化链仍然保留
- `CactusReceiver` 注册仍然保留
- 静音音乐 / 一像素逻辑仍然保留

结论：

- 当前版本是“短信专版 + 保活保留”
- 不是“短信极简版”

### 6.3 前台服务裁切

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/service/ForegroundService.kt`

已做事项：

- `startForegroundService()` 启动前台通知后，如果 `SmsOnlyMode.isEnabled`，直接返回

这意味着在短信专版下：

- 仍有前台服务本身
- 但不再继续执行这些非短信职责：
  - 通知监听服务切换
  - cron 调度
  - 应用列表加载
  - FRPC 自启动
  - 自动任务相关联动

保留事项：

- 前台常驻通知
- 保活基础结构

### 6.4 自动任务断根

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/workers/SendWorker.kt`

已做事项：

- 在短信专版模式下，不再执行 `autoTaskProcess(...)`

这是强制要求，不允许只隐藏任务页而让后台继续跑。

### 6.5 Manifest 裁切

修改文件：

- `app/src/main/AndroidManifest.xml`

已移除声明的组件：

- `ClientActivity`
- `TaskActivity`
- `BluetoothScanService`
- `HttpServerService`
- `LocationService`
- `NotificationService`
- `BatteryReceiver`
- `BluetoothReceiver`
- `CallReceiver`
- `LockScreenReceiver`
- `NetworkChangeReceiver`
- `SimStateReceiver`

当前仍保留的关键组件：

- `SplashActivity`
- `MainActivity`
- `ForegroundService`
- `BootCompletedReceiver`
- `SmsReceiver`

结果：

- 系统层面不会再直接拉起通话、通知、远控、定位、蓝牙等入口

### 6.6 Rules 页面短信化

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/fragment/RulesFragment.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/RulesEditFragment.kt`

已做事项：

- `RulesFragment` 在短信专版模式下隐藏类型 tab
- 不再允许在界面上切换 `sms / call / app`
- `RulesEditFragment` 在 `initArgs()` 后强制 `ruleType = "sms"`

结果：

- 规则系统仍保留
- 但 UI 上只允许短信规则

### 6.7 Logs 页面短信化

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/fragment/LogsFragment.kt`

已做事项：

- 在短信专版模式下隐藏日志类型 tab

结果：

- 日志页默认只围绕短信链路工作
- 不再暴露 `call` / `app` 入口

### 6.8 主界面改为纯 4-tab 导航

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/activity/MainActivity.kt`

已做事项：

- 彻底移除 `SlidingRootNav`
- 彻底移除 `DrawerAdapter`
- 不再初始化侧滑抽屉
- 不再保留 `openMenu()` / `closeMenu()` / `isMenuOpen()`
- 不再保留抽屉里对应的导航逻辑
- 主界面现在只保留 4 个 tab：
  - Logs
  - Rules
  - Senders
  - Settings

这一步的设计理由：

- 原先左上角菜单和侧滑菜单在短信专版模式下已经退化成这 4 个 tab 的重复入口
- 保留抽屉只会制造冗余 UI 和后续维护负担

### 6.8.1 主界面默认落在设置页

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/activity/MainActivity.kt`

已做事项：

- 主界面首次显示时默认切到 `SettingsFragment`
- 底部 tab 默认选中第四个，即 `Settings`

原因：

- 当前短信专版更强调“先设置，再使用”
- 新用户进入后应直接看到短信相关与保活相关配置，而不是先落到日志页

后续约束：

- 不要再改回默认落到 `Logs`
- 如果后续 UI 重构，仍需保证首次进入默认落在设置页

### 6.9 主页面左上角菜单按钮去除

修改文件：

- `fragment/LogsFragment.kt`
- `fragment/RulesFragment.kt`
- `fragment/SendersFragment.kt`
- `fragment/SettingsFragment.kt`

已做事项：

- 标题栏统一改为 `disableLeftView()`
- 去掉左上角 `ic_action_menu`
- 去掉点击后打开侧滑菜单的逻辑

结果：

- 主界面四个主页面已经不会再出现左上角菜单按钮

### 6.10 Settings 页面短信专版隐藏策略

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/fragment/SettingsFragment.kt`

当前采用的是：

- **隐藏无关项**
- **不初始化无关项**

而不是“显示但灰掉”。

这样做的原因：

- 目标是减少干扰，不是保留无效痕迹
- 灰掉的无用项会误导用户，以为功能可恢复或权限没给

当前在短信专版模式下：

- 不再初始化：
  - `switchEnablePhone(...)`
  - `switchEnableAppNotify(...)`
  - `switchEnableBluetooth(...)`
  - `switchEnableLocation(...)`
  - `switchEnableSmsCommand(...)`
  - `switchEnableLoadAppList(...)`
  - `switchDirectlyToClient(...)`
  - `switchDirectlyToTask(...)`
  - `editExtraAppList(...)`
  - `initAppSpinner()`
  - `EVENT_LOAD_APP_LIST` observer

- 直接隐藏：
  - 通话转发整项
  - APP 通知整项
  - 蓝牙整项
  - 定位整项
  - 短信指令整项
  - 应用列表加载整项
  - 纯客户端模式
  - 纯任务模式
  - 靠近听筒关屏
  - 额外 APP 通知附属区域

在短信专版中仍保留并可编辑：

- 短信转发总开关
- 重复消息过滤
- 静默期
- 静默期日志
- 开机启动
- 电池优化
- 不在最近任务列表中显示
- `Cactus`
- 静音音乐
- 一像素
- 重试次数 / 延时 / 超时
- 设备备注
- SIM1 / SIM2 备注
- 前台通知内容
- 自定义短信模板
- 调试模式
- 导出日志
- 语言

### 6.11 Settings 页标题栏的额外约束

当前 `SettingsFragment` 中：

- 保留了帮助提示按钮
- 在短信专版模式下，不再显示 Clone / Restore 入口

原因：

- Clone/Restore 属于远控/客户端体系的一部分，不应再对用户暴露

### 6.12 首次启动不再显示隐私政策弹窗

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/activity/SplashActivity.kt`

已做事项：

- 去掉首次启动时的隐私政策弹窗逻辑
- `onSplashFinished()` 现在直接进入主界面

原因：

- 当前是个人定制裁剪版，不需要保留官方版的隐私政策弹框交互
- 该弹窗还带有外部链接文案，不符合“默认不访问外链”的目标

当前结论：

- 首次启动不再因为隐私协议阻断流程
- `CommonUtils.showPrivacyDialog()` 仍保留在代码里，但不再属于默认启动路径

### 6.13 更新检查与更新弹窗彻底禁用

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/utils/SmsOnlyMode.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/activity/MainActivity.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/utils/sdkinit/XUpdateInit.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/App.kt`

已做事项：

- 在 `SmsOnlyMode.enforceLockedConfig()` 中强制 `SettingUtils.autoCheckUpdate = false`
- `MainActivity.initData()` 不再调用 `XUpdateInit.checkUpdate(...)`
- `XUpdateInit.init(...)` 在短信专版模式下直接 `return`
- `XUpdateInit.checkUpdate(...)` 在短信专版模式下直接 `return`
- `App.initLibs()` 中不再初始化 `XUpdateInit`

原因：

- 个人定制版永远不应该访问上游官方更新服务
- 个人定制版也不应该再出现任何官方更新检查或更新下载相关弹窗

当前结论：

- 启动后不会再自动检查更新
- 即使未来某处误调 `XUpdateInit.checkUpdate(...)`，短信专版模式下也不会真正执行

### 6.14 Tips 改为本地固定内容

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/widget/GuideTipsDialog.kt`

已做事项：

- 去掉通过 `XHttp.get(url_tips)` 远程拉取 tips JSON 的逻辑
- 改为本地直接构造固定 `TipInfo`

当前固定 tips 文案：

- 标题：`新用户必读`
- 内容：`开始设置之前，请您认真地看一遍 Wiki ！<br />\n遇到问题，请按照 常见问题 章节进行排查！`

原因：

- 个人定制版不应再默认请求官方 tips 远端地址
- tips 需要稳定、可控、离线可用

当前结论：

- `showTips()` / `showTipsForce()` 仍可用
- 但展示内容完全本地化，不再联网

### 6.14.1 启动时强制弹出本地 Tips

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/activity/MainActivity.kt`

已做事项：

- `MainActivity.initData()` 中改为直接显示本地 tips
- 不再依赖 `autoCheckUpdate`
- 不再依赖联网状态
- 不再伴随任何更新检查

当前结论：

- 启动主界面时，会弹出本地固定 tips
- tips 的展示与更新服务完全脱钩

后续约束：

- 不允许恢复“先联网拉取 tips 再弹窗”的逻辑
- 不允许恢复“tips 与更新检查绑定”的逻辑

### 6.14.2 Tips 关闭方式被刻意改造

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/widget/GuideTipsDialog.kt`

已做事项：

- 在短信专版模式下，隐藏 tips 弹窗右上角关闭按钮
- 在短信专版模式下，隐藏“以后不再提示此类信息”复选框
- 在短信专版模式下，隐藏“上一条”
- 在短信专版模式下，禁止点击空白区域关闭
- 在短信专版模式下，唯一关闭方式改为连续点击“下一步”10次

原因：

- 当前个人定制版希望降低被他人直接上手使用的便利性
- tips 弹窗不再承担常规信息提示，而兼具一定的使用门槛作用

注意事项：

- 当前 tips 已经改为启动主界面时强制弹出本地内容
- 只要将来任何路径继续展示 tips，这个“10次点击关闭”的行为都必须保留
- 后续不要因为“交互不自然”而随手改回普通关闭方式，这是明确设计要求

### 6.15 友盟统计初始化移除

修改文件：

- `app/src/main/kotlin/cn/ppps/forwarder/App.kt`

已做事项：

- `App.initLibs()` 中不再调用 `UMengInit.init(this)`

原因：

- 友盟统计属于默认外连
- 个人定制版不应保留启动时对第三方统计服务的访问

当前结论：

- 当前默认启动链已不再包含友盟统计初始化

### 6.16 当前“默认外连”审计结论

当前目标不是完全删除所有网络能力，因为发送器本身就是网络通道的一部分。

本节的“默认外连”定义为：

- 用户未手动配置任何 sender
- 用户未主动进入隐藏页面
- App 正常首次启动 / 进入主界面 / 常规使用

在这个定义下，当前已经移除的默认外连包括：

- 官方更新检查
- 官方 tips 拉取
- 友盟统计初始化
- 首次启动隐私弹窗中的官方链接跳转

当前仍然存在但**不属于默认外连**的网络能力包括：

- 各 sender 的联网发送能力
- `WebhookUtils`
- `TelegramUtils`
- `WeworkAgentUtils`
- 其他 Bark / Email / Feishu / PushPlus / Gotify / 企业微信 / Telegram / Webhook 等发送器

这些属于产品功能，不是默认联网。

当前仍然存在但**默认 UI 已无入口**的外链代码包括：

- `AboutFragment` 中的 GitHub / Gitee / 捐赠 / 小程序图片等链接
- `ClientFragment` 中的小程序图片预览
- `UpdateTipDialog`
- `CommonUtils.showPrivacyDialog()`

它们可以暂时保留在代码中，但不属于当前默认用户路径。

## 6.17 当前默认提示策略

当前短信专版下，默认启动相关的提示策略应保持如下：

- 不弹隐私政策
- 不弹官方更新检查
- 不请求官方 tips JSON
- 仅保留本地固定 tips 内容
- 仅保留系统权限弹窗和本地业务提示

## 7. 未做但明确保留的内容

当前没有做、但明确决定暂时不动的内容：

- `adapter/menu/` 整个包
- `menu_left_drawer.xml`
- `ServerFragment`、`ClientFragment`、`TasksFragment`、`FrpcFragment` 等旧页面文件
- `server/` 包和 HTTP controller 代码
- `ActionWorker`、`ConditionUtils` 等自动任务实现文件
- `database/entity/Frpc.kt`、`Task.kt` 等数据库结构

理由：

- 这些都属于冗余代码，不是当前功能边界的主要问题
- 删除它们会加大和上游的差异面
- 只要入口、启动、Manifest 都已经切掉，它们暂留是可接受的

## 8. 本地构建与 CI 记录

### 8.1 本地构建结论

当前本地环境是 `Linux aarch64`。

已做过验证：

- 可以安装 Android SDK
- 可以配置 `local.properties`
- 但本地无法完成 APK 编译

失败原因不是项目代码，而是 Android 宿主构建工具的架构问题：

- `aapt2` 为 `x86_64` 宿主二进制
- 当前机器是 `aarch64`
- 因此 `:app:mergeDebugResources` 阶段启动 `aapt2` 失败

结论：

- **不要再把“本地编译 APK”当成当前机器的常规验证手段**
- 当前机器的正确用途是代码修改、静态检查、文档维护
- 真正 APK 验证应依赖 `x86_64` 环境，例如 GitHub Actions

### 8.2 GitHub Actions 迁移

新增文件：

- `.github/workflows/APK_Build.yml`
- `GITHUB_ACTIONS_APK_BUILD.md`

设计目标：

- 不依赖原作者私有 keystore 仓库
- 不依赖原作者 XUpdate 相关 secrets
- fork 或迁移到自己的仓库后可直接跑

当前工作流特点：

- `push main` 自动构建 `debug`
- `pull_request -> main` 自动构建 `debug`
- `workflow_dispatch` 支持手动选 `debug` / `release`
- `debug` 自动生成临时 debug keystore
- `release` 依赖 GitHub secrets

### 8.3 当前仓库与 Actions 相关结论

当前公开仓库：

- `SmsForwarderLite`

当前推荐的 APK 验证方式：

- 通过 GitHub Actions 的 `APK Build` 来验证

不要把以下工作流当作当前主线验证方式：

- `Release.yml`
- `Weekly_Build.yml`

它们属于上游原始发布链，不适合作为当前私有/派生仓库的主构建方式。

## 9. 后续继续改造时的操作准则

### 9.1 准则一：保活不能被误伤

以后如果改以下文件：

- `App.kt`
- `ForegroundService.kt`
- `SettingsFragment.kt`
- `AndroidManifest.xml`

必须优先检查是否误伤以下保活链：

- 前台服务
- 开机启动
- 电池优化
- `Cactus`
- 静音音乐
- 一像素

如果有任何代码会把这些开关写成 `false`，需要立即回退。

### 9.2 准则二：主界面必须保持 4-tab

短信专版当前主导航就是：

- Logs
- Rules
- Senders
- Settings

不应重新引入：

- 左上角菜单按钮
- 侧滑抽屉
- 重复导航入口

### 9.3 准则三：设置页优先“隐藏”，不是“灰掉”

当前明确决策：

- 无关项优先隐藏
- 不采用“保留显示但不可编辑”策略

理由：

- 灰掉仍然会造成认知干扰
- 会误导用户认为功能只是权限不足或暂时不可用

### 9.4 准则四：不要随便删旧代码

跟上游同步时，应该优先做：

- 重新应用 `SmsOnlyMode`
- 重新阻断非短信启动链
- 重新恢复 4-tab 主界面
- 重新隐藏设置页无关项

不应该一上来做：

- 大规模删文件
- 重写表结构
- 删大量旧 Fragment

## 10. 跟上游同步时的检查顺序

以后如果要把上游新版本同步进来，建议按下面顺序检查。

### 第一步：检查启动链

重点文件：

- `App.kt`
- `ForegroundService.kt`
- `SendWorker.kt`
- `AndroidManifest.xml`

检查项：

- `SmsOnlyMode.enforceLockedConfig()` 是否还在最早期执行
- 非短信 service / receiver 是否又被重新拉起
- `SendWorker.autoTaskProcess()` 是否又被重新启用
- 保活链是否被误删

### 第二步：检查主界面

重点文件：

- `MainActivity.kt`
- `LogsFragment.kt`
- `RulesFragment.kt`
- `SendersFragment.kt`
- `SettingsFragment.kt`

检查项：

- 左上角菜单是否又回来了
- 侧滑抽屉是否又回来了
- 4 个 tab 是否仍然稳定

### 第三步：检查设置页

重点文件：

- `SettingsFragment.kt`
- `fragment_settings.xml`

检查项：

- 无关项是否又显示了
- 保活项是否仍然保留
- Clone / Restore 是否又出现了

### 第四步：检查规则和日志页

重点文件：

- `RulesFragment.kt`
- `RulesEditFragment.kt`
- `LogsFragment.kt`

检查项：

- `call/app` tab 是否又出现
- `ruleType` 是否仍然被强制为 `sms`

### 第五步：检查 CI

重点文件：

- `.github/workflows/APK_Build.yml`
- `GITHUB_ACTIONS_APK_BUILD.md`

检查项：

- Actions 是否仍可直接构建 debug
- 签名路径逻辑是否被破坏
- `isNeedPackage=false` 的 debug 保护是否还在

## 11. 当前最重要的文件清单

这些文件属于短信专版的高优先级差异面，跟上游时要重点保留：

- `app/src/main/kotlin/cn/ppps/forwarder/utils/SmsOnlyMode.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/App.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/service/ForegroundService.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/workers/SendWorker.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/activity/MainActivity.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/LogsFragment.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/RulesFragment.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/RulesEditFragment.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/SendersFragment.kt`
- `app/src/main/kotlin/cn/ppps/forwarder/fragment/SettingsFragment.kt`
- `app/src/main/AndroidManifest.xml`
- `.github/workflows/APK_Build.yml`

## 12. 当前未提交到上游但已在本地执行的 UI 调整

以下事项是当前本地工作区已经做的方向，后续继续推进时应视为既定目标：

- 去除四个主页面左上角菜单按钮
- 抽屉菜单彻底移除
- 主界面纯 4-tab 化
- 设置页仅保留短信与保活相关项
- 启动默认落在设置页
- 启动必弹本地固定 tips
- tips 只能连点“下一步”10次关闭

如果后续提交前中断，只要基于本文档继续，就应优先完成这四项，而不是转去清理冗余代码。

## 13. 以后继续工作时的默认任务解释

以后如果把本文档交给 Codex，默认代表以下任务背景：

- 项目：`SmsForwarderLite`
- 本质：上游 `SmsForwarder` 的短信专版
- 目标：短信输入 + 规则 + 发送器 + 日志 + 强保活
- 必须隐藏：通话、通知、远控、任务、FRP、蓝牙、定位、纯客户端、纯任务等
- 必须保留：前台服务、开机启动、电池优化、`Cactus`、静音音乐、一像素
- 主导航：只允许 4-tab
- 默认首页：必须落在设置页
- 左上角菜单：不允许恢复
- 设置页策略：隐藏无关项，不用灰掉
- 启动提示：必须是本地固定 tips，不能联网拉取
- tips 关闭方式：只能连点“下一步”10次关闭
- 本地 APK 编译：当前机器不作为可靠手段
- APK 验证：优先走 GitHub Actions

只要遵守以上解释，就能继续正确地推进或回补该项目。
