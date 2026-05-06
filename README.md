# WrdWarn Reward Ads MVP

这是一个合规激励广告安卓 MVP，用于验证“用户主动观看激励广告并获得金币”的产品流程。

## 当前功能

- 原生 Android Java 项目
- 接入 Google AdMob 激励视频广告 SDK
- 默认使用 Google 官方测试广告位，避免开发阶段产生无效流量
- 用户完成激励广告观看后增加金币
- 本地金币余额与流水记录
- 提现申请演示入口
- 基础合规说明入口

## 合规边界

广告变现必须遵守广告平台和应用商店政策：

- 不允许自动播放广告、刷广告、诱导点击或承诺虚假收益
- 用户必须主动点击按钮观看激励广告
- 只有广告 SDK 回调确认奖励后才发放金币
- 上线前必须完善隐私政策、用户协议、提现规则、风控与审核
- 真实广告位上线前应在 AdMob 后台配置测试设备并完成政策审核

## 本地构建

需要 Android SDK 与 JDK 17+。仓库包含 Gradle Wrapper：

```bash
./gradlew assembleDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 直接下载测试 APK

仓库中保留了一份 debug 测试包：

```text
releases/wrdwarn-reward-ads-debug.apk
```

这是开发测试版本，使用 Google 官方测试广告位，不适合直接上线发布。

## 下一步建议

1. 替换包名、应用名与品牌资源
2. 在 AdMob 创建应用与激励广告单元，替换 Manifest 和 `MainActivity` 中的测试 ID
3. 增加后端服务：用户账户、金币账本、广告回调验证、提现审核、风控
4. 增加隐私政策、用户协议和提现规则页面
5. 接入 Google User Messaging Platform 或目标市场要求的同意管理
