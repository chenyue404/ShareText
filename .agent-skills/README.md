# ShareText2 多助手执行包

这套文档是给你直接拿来分工用的。

## 有哪些文档

- `main-orchestrator/SKILL.md`：总负责人（主助手）
- `subagent-a-baseline/SKILL.md`：先把“现在能做什么”查清楚
- `subagent-b-service-core/SKILL.md`：服务端部分（启动、停止、端口、接口）
- `subagent-c-android-host/SKILL.md`：安卓界面和生命周期
- `subagent-d-web-assets/SKILL.md`：网页文件和前端交互
- `subagent-e-build-governance/SKILL.md`：构建和依赖
- `subagent-f-release-guard/SKILL.md`：上线前把关和回滚方案

## 怎么用（最短路径）

1. 先让主助手执行 `main-orchestrator/SKILL.md`，先出本轮任务分配。
2. 再让 6 个子助手并行执行各自 `SKILL.md`。
3. 子助手都按各自模板回报结果。
4. 主助手统一给结论：本轮能不能过。
