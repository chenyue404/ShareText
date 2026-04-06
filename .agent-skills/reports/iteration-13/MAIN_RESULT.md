[主助手回报]
轮次：13
本轮范围：端口相关提示统一
通过的改动：

- `PortAdvice` 新增：保存失败提示、保存成功提示、端口切换提示。
- `SettingsActivity` 和 `UiNoticeStore` 改为统一调用 `PortAdvice`。
- `PortAdviceTest` 增补对应测试用例。
- 验收通过：`./gradlew :app:assembleDebug`、`./gradlew :app:testDebugUnitTest`。
  暂缓的改动：
- 提示组件升级为 Snackbar（本轮不改交互形态）。
  结论：通过
  严重问题：
- 无
  下一轮入口：
- 继续做“提示展示方式统一”（例如 Toast 与页面错误提示的一致性）。
