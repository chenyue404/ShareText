# 第9轮任务卡

日期：2026-04-06
目标：

1. 启动失败时提示可用端口建议。
2. 增加自动回归，覆盖端口建议逻辑。

范围：

- app/src/main/java/com/chenyue404/sharetext/PortConfig.kt
- app/src/main/java/com/chenyue404/sharetext/WebService.kt
- app/src/main/java/com/chenyue404/sharetext/SettingsActivity.kt
- app/build.gradle.kts
- app/src/test/java/com/chenyue404/sharetext/PortConfigTest.kt

验收：

- 端口占用时，错误信息带“建议改为 xxx”。
- 单元测试通过。
- assembleDebug 通过。
