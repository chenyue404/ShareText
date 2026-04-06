[主助手回报]
轮次：10
本轮范围：端口占用提示统一 + 恢复链路自动回归测试补强
通过的改动：

- 新增 `PortAdvice`，统一生成“端口被占用”提示。
- `WebService`、`SettingsActivity` 接入统一提示，避免文案分叉。
- 新增 `PortAdviceTest`（建议/无建议两类）。
- `PortConfigTest` 增加真实占用场景：占用端口后可找到可用端口用于恢复。
- 构建与单测通过：`./gradlew :app:assembleDebug`、`./gradlew :app:testDebugUnitTest`。
  暂缓的改动：
- 端到端 UI 自动化（需要独立设备/仿真器链路，不在本轮范围）。
  结论：通过
  严重问题：
- 无
  下一轮入口：
- 增加“设置页一键采用建议端口”交互，减少手动输入。
- 视需要补一条仪表测试，覆盖“保存端口后自动运行中”的页面状态。
