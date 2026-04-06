[主助手回报]
轮次：12
本轮范围：保存端口成功后的首页明确提示
通过的改动：

- 新增 `UiNoticeStore`，用于保存/消费一次性提示。
- 设置页保存成功时写入“端口已切换”提示。
- 首页 `onResume` 消费并弹出一次提示，避免重复。
- 构建与回归通过：`./gradlew :app:assembleDebug`、`./gradlew :app:testDebugUnitTest`。
  暂缓的改动：
- 提示样式升级为 Snackbar（本轮先用 Toast）。
  结论：通过
  严重问题：
- 无
  下一轮入口：
- 把端口相关提示统一到一个轻提示组件（后续可切 Snackbar）。
- 评估补一条 UI 自动化用例覆盖“保存后首页提示只出现一次”。
