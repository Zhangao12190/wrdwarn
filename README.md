# welding Android MVP

这是一个合规激励广告安卓 MVP，用于验证“小游戏 + 广告确认奖励”的简化产品流程。

## 当前功能

- 原生 Android Java 项目
- 接入 Google AdMob 激励视频广告 SDK
- 默认使用 Google 官方测试广告位，避免开发阶段产生无效流量
- 应用名称：welding
- 扁平 UI，使用蓝紫/灰白配色
- 首页聚焦核心流程：开始尖刺蛇、游戏结束、观看广告领取奖励
- 小游戏中心原型，当前主推“尖刺蛇”
- 用户完成小游戏后生成待领取金币
- 用户主动观看激励广告后，待领取金币才计入账户
- 本地金币余额与流水记录
- 本地游客账户 ID，用于演示用户账户余额
- 支付宝提现申请表单和“审核中”记录演示
- 基础合规说明入口

## 合规边界

广告变现必须遵守广告平台和应用商店政策：

- 不允许自动播放广告、刷广告、诱导点击或承诺虚假收益
- 用户必须先完成小游戏，再主动点击按钮观看激励广告
- 只有广告 SDK 回调确认奖励后，才把待领取游戏奖励计入账户
- 支付宝提现当前只创建申请并冻结本地金币；真实自动打款必须由后端审核后调用支付宝企业转账接口
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

如果 GitHub 下载慢或经常失败，可以优先使用下面几个国内/香港加速代理。已完整校验 `gh.llkk.cc` 链接，下载文件与仓库 APK 的 SHA-256 一致。

推荐国内代理：

```text
https://gh.llkk.cc/https://raw.githubusercontent.com/Zhangao12190/wrdwarn/7e38d7c1d16370dbc17a7b12a44940ca038f538a/releases/wrdwarn-reward-ads-debug.apk
```

备用代理：

```text
https://ghproxy.net/https://raw.githubusercontent.com/Zhangao12190/wrdwarn/7e38d7c1d16370dbc17a7b12a44940ca038f538a/releases/wrdwarn-reward-ads-debug.apk
```

```text
https://ghfast.top/https://raw.githubusercontent.com/Zhangao12190/wrdwarn/7e38d7c1d16370dbc17a7b12a44940ca038f538a/releases/wrdwarn-reward-ads-debug.apk
```

国际 CDN 镜像：

```text
https://cdn.jsdelivr.net/gh/Zhangao12190/wrdwarn@7e38d7c1d16370dbc17a7b12a44940ca038f538a/releases/wrdwarn-reward-ads-debug.apk
```

也可以使用 GitHub Raw 原始下载：

```text
https://raw.githubusercontent.com/Zhangao12190/wrdwarn/7e38d7c1d16370dbc17a7b12a44940ca038f538a/releases/wrdwarn-reward-ads-debug.apk
```

当前 APK SHA-256：

```text
f4666e3845570c9a8827d2fab18e582fed5829c332c0f7b45640ab7d7baa03e1
```

## 下一步建议

1. 替换包名、应用名与品牌资源
2. 在 AdMob 创建应用与激励广告单元，替换 Manifest 和 `MainActivity` 中的测试 ID
3. 增加更多小游戏，并把小游戏定义抽象为可配置列表
4. 增加后端服务：用户账户、金币账本、广告回调验证、支付宝提现审核、风控
5. 准备支付宝企业商家/开放平台转账能力，后端安全保存证书和私钥
6. 增加隐私政策、用户协议和提现规则页面
7. 接入 Google User Messaging Platform 或目标市场要求的同意管理
