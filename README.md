# klgc hook

面向 Android 的 Xposed 模块（libxposed **API 102**），用于给「动漫共和国」客户端补齐播放体验：解锁最高清晰度、跳过非激励广告、本地会员态、界面精简等。

> 本项目仅用于**个人学习与安全研究**（Hook 框架、Compose 应用结构、播放链路分析）。请勿用于任何商业用途；使用前请确认你所在地区的法律与目标应用的用户协议。作者不对任何账号、设备或数据后果负责。

<p align="left">
  <img src="icon/ic_launcher.png" width="96" alt="icon">
</p>

## 功能

| 分组 | 开关 | 说明 |
| --- | --- | --- |
| 常用 | 4K 画质解锁 | 解锁最高清晰度播放 |
| 常用 | 非激励广告豁免 | 开屏与贴片广告自动跳过 |
| 常用 | 广告静音 | 广告播放期间自动静音 |
| 常用 | 修改本地会员 | 本地显示会员权益（仅界面层） |
| 常用 | 隐藏看广告领 VIP | 移除相关横幅与推广弹窗 |
| 常用 | 屏蔽系统公告弹窗 | 启动时不再弹出系统公告 |
| 界面 | 任务页去除 | 隐藏底部任务入口 |
| 界面 | 底部透明 | 底栏透明显示 |
| 界面 | 环境检测绕过 | 让宿主认为设备环境干净 |
| 界面 | 自动签到 | 进入主界面后自动完成签到 |

全部开关可在模块内面板中即时切换；切换「底部透明」会自动重建界面。

## 环境要求

- Android 8.0（API 26）及以上
- LSPosed 1.9.3+（支持 libxposed API 102）或同等实现（如 Vector）
- 作用域：`com.sdmnfowa.soad.p1`

## 安装

1. 安装模块 APK；
2. 在 LSPosed 管理器中启用模块，并勾选作用域内的宿主应用；
3. 强制停止宿主应用后重新打开；
4. 进入「我的」页面，**长按左上角头像约 0.5 秒**唤出开关面板；
5. 桌面图标可直接打开模块状态页，查看启用状态、框架版本、API 版本与作用域。

## 构建

```bash
# 1) 依赖：JDK 17+、Android SDK platform-33、R8、build-tools 33+
export ANDROID_JAR=$ANDROID_HOME/platforms/android-33/android.jar
export R8_JAR=$HOME/.m2/repository/com/android/tools/r8/8.2.42/r8-8.2.42.jar
export BUILD_TOOLS=$ANDROID_HOME/build-tools/33.0.2

# 2) 拉取 libxposed 客户端库（api / interface / service，Apache-2.0）
bash scripts/fetch-libs.sh

# 3) 构建并签名（首次会自动生成 animrep.keystore）
bash build.sh
# 产物：out/animrep-module.apk
```

> 如果你用自己的签名，请同步修改 `HookEntry.java` 中的 `EXPECT_SIG`（模块启动时的签名自校验基准），否则模块会拒绝加载。该常量的作用是防止 APK 被二次打包分发。

### 可选：R8 混淆版

```bash
bash build_obf.sh    # 产物 out/animrep-obf.apk
```

## 项目结构

```
src/com/dsh/animerepublic/
  LibEntry.java        libxposed 入口（XposedModule）
  HookEntry.java       兼容层入口 + 全部 Hook 与 UI 实现
  SettingsActivity.java 桌面状态页
  StatusProvider.java   宿主进程状态回写
  LiquidRt.java         AGSL 折射着色器（底栏玻璃）
src/de/robv/android/xposed/   legacy Xposed API 垫片（转调 libxposed）
src/com/qmdeve/liquidglass/   第三方液态玻璃组件（MIT，见 NOTICE）
src/META-INF/xposed/          module.prop / scope.list / java_init.list
```

## 已知限制

- 会员相关能力只作用于**客户端展示层**；服务端的发奖、片源授权仍由官方服务器裁决。
- 激励视频奖励依赖广告平台的服务端回调，跳过播放不会产生回调，因此奖励不会到账。

## 贡献

见 [CONTRIBUTING.md](CONTRIBUTING.md)。提交前请确保 `bash build.sh` 能通过，且不包含任何第三方受版权保护的素材、目标应用 APK 或其反编译产物。

## 许可

代码以 [GPL-3.0](LICENSE) 发布。第三方组件许可见 [NOTICE](NOTICE)。
