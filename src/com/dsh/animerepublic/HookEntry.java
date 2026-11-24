package com.dsh.animerepublic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    static void mark(String file, String msg) {
        if (true) {
            return;
        }
        try {
            File f = new File("/sdcard/Download/DSH-Go", file);
            FileOutputStream fos = new FileOutputStream(f, true);
            try {
                fos.write((System.currentTimeMillis() + "  " + msg + "\n").getBytes("UTF-8"));
            } finally {
                try { fos.close(); } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        }
    }

    static {
        mark("probe.log", "L1");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.sdmnfowa.soad.p1".equals(lp.packageName)) {
            return;
        }
        mark("animrep3.log", "L2" + lp.processName);
        try {
            new Feat().run(lp);
        } catch (Throwable t) {
            mark("animrep3.log", "L3" + t);
            try {
                de.robv.android.xposed.XposedBridge.log("L55" + t);
            } catch (Throwable ignored) {
            }
        }
    }
}

final class Feat {



    /* ------------------------------------------------------------ 模块开关 */

    /** A 去广告：开屏 / 插屏 / 播放器贴片 / banner 全部按 VIP 免广告跳过 */
    private static final boolean F1 = true;
    /** B 跳过激励视频：点"看广告领奖励"时不播广告，直接进结算（奖励仍走服务端） */
    /** 强制广告 SDK 就绪（一般不需要，开启有风险） */
    private static final boolean F_READY_GATE = false;
    /** D 会员解锁：伪造当前会员为最高等级（默认关闭） */
    private static final boolean F_MEMBER = true;
    /** D 权益位：adFree / canWatchVip / canWatchSvip / 下载额度全开（默认关闭） */
    private static final boolean F_CAPS = false;
    /** C 本地伪造领奖（界面必成功但余额不变，默认关闭） */
    /** 4K：把脚本返回的 4K 流伪装成 1080P，让 App 选中它（画质真 4K） */
    private static final boolean F2 = true;
    /** 让 4K 选项可点（isVipOnly → false） */
    private static final boolean F_QUALITY_PICK = true;
    /** 隐藏「看广告领 VIP」推广横幅 / 入口 */
    private static final boolean F3 = true;
    /** P 抓真实播放地址 + 解析脚本（不动任何行为，只记录） */
    private static final boolean F_PLAYURL = true;
    /** M 广告真播时静音（配合真播拿 S2S 回执） */
    private static final boolean F5 = true;
    private static volatile boolean sAdActive;
    private static volatile boolean sLuaDumped;
    /** R 探测：自己发一轮 exchange 探测（看服务端对哪些 quality code 真的给地址） */
    private static final boolean F6 = false;
    /** R 探测：只放行 UI 层的 4K 点击（不改 domain 模型，避免自动选清晰度被污染） */
    private static final boolean F_QUALITY_UI = false;
    /** C 渲染证明伪造：让服务端认为广告确实播放过（否则领奖只会 pending） */
    /** Q 画质解锁：把清晰度选项的 VIP 限制标记全部抹掉（4K 变成可点） */
    private static final boolean F_QUALITY = false;
    /** 诊断开关：默认关闭，不写任何日志通道 */
    private static final boolean F_DIAG = false;

    /** 看门狗：启动后多少毫秒视为"健康启动完成" */
    private static final long BOOT_OK_DELAY_MS = 20000L;
    private static final long SAFE_MODE_TTL_MS = 6L * 3600L * 1000L;
    /** 模块配置版本，改大即清空崩溃计数 */
    /* ---------------------------- 运行时可切换配置 ---------------------------- */

    /** 配置项：存目标 App 的 SharedPreferences，改完立即生效（hook 里现读） */
    private static SharedPreferences cfgPrefs() {
        try {
            Context c = appContext();
            if (c == null) {
                return null;
            }
            return c.getSharedPreferences("animrep_cfg", Context.MODE_PRIVATE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean cfg(String key, boolean def) {
        try {
            SharedPreferences p = cfgPrefs();
            if (p != null && p.contains(key)) {
                return p.getBoolean(key, def);
            }
        } catch (Throwable ignored) {
        }
        return def;
    }

    private static void setCfg(String key, boolean v) {
        try {
            SharedPreferences p = cfgPrefs();
            if (p != null) {
                p.edit().putBoolean(key, v).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    private static final int BUILD_VER = 121;
    /** 本版更新内容（欢迎弹窗展示，每版记得改） */
    private static final String F17 =
            "· 新增「环境检测绕过」「自动签到」（均自动开启）\n"
            + "· 新增模块桌面图标，可查看启用状态等信息\n"
            + "· 关于界面新增「作者酷安主页」入口\n"
            + "· 新增 GitHub 开源仓库入口";

    private static final String VERSION_NAME = "1.5.0";
    private static final boolean F_OVERLAY = true;

    private static final String TAG = "[AnimRep] ";
    private static final String PKG = "com.sdmnfowa.soad.p1";
    private static final String PREFS = "animrep_state";

    private static final String F8 = "com.yy.myuko.shared.domain.ad.AdSlotVipExemption";
    private static final String F9 = "com.yy.myuko.shared.domain.ad.AdSlotVipExemption$Companion";
    private static final String C_READY_GATE = "com.yy.myuko.shared.domain.ad.AdSdkReadyGate";
    private static final String C_COMPLETED = "com.yy.myuko.shared.domain.ad.SdkOutcome$Completed";
    private static final String C_SESSION_UC = "com.yy.myuko.shared.domain.ad.usecase.AdSessionUseCase";

    private static final String C_PREPARED_UC = "com.yy.myuko.shared.domain.ad.usecase.PreparedRewardSessionUseCase";
    private static final String C_SESSION = "com.yy.myuko.shared.domain.ad.PreparedRewardSession";
    private static final String C_CONFIRMATION = "com.yy.myuko.shared.domain.ad.PreparedRewardConfirmation";
    private static final String C_DECISION_READY =
            "com.yy.myuko.shared.domain.ad.usecase.PreparedRewardConfirmationDecision$Ready";
    private static final String C_EITHER_RIGHT = "arrow.core.Either$Right";
    private static final String F7 = "com.yy.myuko.shared.domain.ad.ClaimResult";
    private static final String C_REPO_AD_IMPL = "com.yy.myuko.shared.data.ad.AdRepositoryImpl";

    private static final String C_REPO_MEMBER = "com.yy.myuko.shared.data.membership.MembershipRepositoryImpl";
    private static final String C_CATALOG = "com.yy.myuko.shared.domain.membership.model.MembershipCatalog";
    private static final String C_ACTIVE = "com.yy.myuko.shared.domain.membership.model.CurrentMembership$Active";
    private static volatile Object FAKE_ACTIVE;

    private static final String F10 = "com.yy.myuko.shared.domain.welfare.model.VipTier";
    private static final String C_LEVEL = "com.yy.myuko.shared.domain.membership.model.MembershipLevel";
    private static final String C_CAPS = "com.yy.myuko.shared.domain.membership.model.MemberCapabilities";
    private static final String C_CAPS_API = "com.yy.myuko.shared.data.membership.model.MemberCapabilitiesApiData";
    private static final String C_MODELS_KT = "com.yy.myuko.shared.domain.membership.model.MembershipModelsKt";
    private static final String C_STATUS = "com.yy.myuko.shared.domain.membership.model.MembershipStatus";
    private static final String C_INSTANT = "kotlin.time.Instant";
    private static final String C_STATE_FLOW_KT = "kotlinx.coroutines.flow.StateFlowKt";

    private static final String KEY_FAKE_FLOW = "animrep_fake_membership_flow";
    private static final long FAR_FUTURE_MS = 4070908800000L; // 2099-01-01T00:00:00Z
    private static final long FAKE_REWARD_AMOUNT = 15L;

    /** 确定不产生用户奖励的广告位：只对这些位返回 VIP 免广告，避免误伤"看广告领奖励"入口 */
    /** 带 isVipOnly / requiredLevelId 的清晰度模型类 */
    private static final String[] C_QUALITY = {
            "com.yy.myuko.shared.domain.playback.model.VideoQuality",
            "com.yy.myuko.shared.presentation.screen.episode.model.VideoQualityUi",
    };

    private static final String[] NON_REWARD_SLOTS = {
            "Splash", "HomeInterstitial", "PlayerPreRoll", "PlayerPause",
            "PlayerSwitchEpisode", "PlayerBanner", "PlayerFeed", "PlayerInterstitial"
    };

    private static ClassLoader CL;
    private static Object MEMBER_REPO;
    private static Object SDK_COMPLETED;
    private static volatile boolean sdkCompletedLoaded;

    /* ------------------------------------------------------------ 入口 */

    void run(XC_LoadPackage.LoadPackageParam lp) {
        if ("com.klgc.animerepublic".equals(lp.packageName)) {
            selfProbe(lp);
            return;
        }
        if (!PKG.equals(lp.packageName)) {
            return;
        }
        CL = lp.classLoader;
        log("L4" + BUILD_VER + "L5" + lp.processName + "L6");
        h24();
        bootHeartbeat();
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() {
                    try {
                        reportStatus();
                        installPing();
                    } catch (Throwable ignored) {
                    }
                }
            }, 2500L);
        } catch (Throwable ignored) {
        }

        // resetCfgOnUpgrade();  // 9.1 没有

        if (!cfg("allow_resign", false) && !selfSignatureOk()) {
            log("L42");
            return;
        }

        boolean safeMode = watchdogOnLoad();
        if (safeMode) {
            log("L7");
            return;
        }

        if (cfg("env_bypass", true)) {
            run("E-env", new Action() {
                public void run() throws Throwable {
                    h34();
                }
            });
        }
        if (cfg("auto_signin", true)) {
            run("S-signin", new Action() {
                public void run() throws Throwable {
                    h35();
                }
            });
        }
        if (F1) {
            run("A-exempt", new Action() {
                public void run() throws Throwable {
                    h1();
                }
            });
        }
        if (F_READY_GATE) {
            run("A-readyGate", new Action() {
                public void run() throws Throwable {
                    h4();
                }
            });
        }
        if (F_MEMBER) {
            run("D-membership", new Action() {
                public void run() throws Throwable {
                    h15();
                    h32();
                }
            });
        }
        if (F_QUALITY) {
            run("Q-quality", new Action() {
                public void run() throws Throwable {
                    h21();
                }
            });
        }
        if (F_PLAYURL) {
            run("P-playurl", new Action() {
                public void run() throws Throwable {
                    h18();
                }
            });
        }
        if (F5) {
            run("M-window", new Action() {
                public void run() throws Throwable {
                    h5();
                }
            });
            run("M-mute", new Action() {
                public void run() throws Throwable {
                    h3();
                }
            });
        }
        if (F_QUALITY_UI) {
            run("R-qualityUi", new Action() {
                public void run() throws Throwable {
                    h20();
                }
            });
        }
        if (F_MEMBER) {
            run("D-derive", new Action() {
                public void run() throws Throwable {
                    h14();
                }
            });
        }
        if (F_CAPS) {
            run("D-capabilities", new Action() {
                public void run() throws Throwable {
                    h7();
                }
            });
            run("D-memberStatus", new Action() {
                public void run() throws Throwable {
                    h13();
                }
            });
        }
        if (F_DIAG) {
            run("Diag-claim", new Action() {
                public void run() throws Throwable {
                    h8();
                }
            });
        }
        log("L8");
    }

    /* ------------------------------------------------------ 启动看门狗 */

    private static Context appContext() {
        Context cached = sAppCtx;
        if (cached != null) {
            return cached;
        }
        try {
            Context c = currentApp();
            if (c != null) {
                sAppCtx = c;
                return c;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object app = XposedHelpers.callStaticMethod(
                    Class.forName("android.app.ActivityThread"), "currentApplication");
            if (app instanceof Context) {
                sAppCtx = (Context) app;
                return (Context) app;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object app = XposedHelpers.callStaticMethod(
                    Class.forName("android.app.ActivityThread"), "currentApplication");
            if (app != null) {
                Object ctx = XposedHelpers.callMethod(app, "getApplicationContext");
                if (ctx instanceof Context) {
                    sAppCtx = (Context) ctx;
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private SharedPreferences prefs() {
        try {
            Context ctx = appContext();
            if (ctx == null) {
                return null;
            }
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 模块换版时把开关恢复默认全开，避免旧版本里手动关过导致"功能没生效" */
    private void resetCfgOnUpgrade() {
        try {
            SharedPreferences st = prefs();
            if (st == null) {
                h26("L9");
                return;
            }
            int seen = st.getInt("cfgver", 0);
            if (seen == BUILD_VER) {
                return;
            }
            SharedPreferences cfgp = cfgPrefs();
            if (cfgp != null) {
                cfgp.edit().clear().apply();
            }
            st.edit().putInt("cfgver", BUILD_VER).apply();
            h26("L10" + BUILD_VER + "L11");
        } catch (Throwable t) {
            h26("L12" + t);
        }
    }

    /** @return true 表示本次应进入安全模式（上次启动疑似崩溃） */
    /** 模块自身签名校验：APK 被改包重签后不装任何 hook（可用 cfg allow_resign 放行） */
    private static final String EXPECT_SIG =
            "90d556ac0194a06e3a36d3056a3bb620746f30ab5178c3acf92f136e52b631c3";

    private static String moduleApkPath() {
        try {
            java.security.CodeSource cs = HookEntry.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) {
                return null;
            }
            String p = cs.getLocation().getPath();
            if (p != null && p.startsWith("file:")) {
                p = p.substring(5);
            }
            return p;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean selfSignatureOk() {
        try {
            String path = moduleApkPath();
            if (path == null || path.isEmpty()) {
                return true;
            }
            java.util.zip.ZipFile zf = new java.util.zip.ZipFile(path);
            try {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    java.util.zip.ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (!n.startsWith("META-INF/")) {
                        continue;
                    }
                    String up = n.toUpperCase();
                    if (!(up.endsWith(".RSA") || up.endsWith(".DSA") || up.endsWith(".EC"))) {
                        continue;
                    }
                    java.security.cert.CertificateFactory cf =
                            java.security.cert.CertificateFactory.getInstance("X.509");
                    java.security.cert.X509Certificate cert =
                            (java.security.cert.X509Certificate) cf.generateCertificate(zf.getInputStream(e));
                    byte[] dig = java.security.MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : dig) {
                        sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
                    }
                    return EXPECT_SIG.equalsIgnoreCase(sb.toString());
                }
            } finally {
                zf.close();
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "L41" + t);
            return true;
        }
        return true;
    }

    private static boolean isMainProcess() {
        try {
            String pn = (String) XposedHelpers.callStaticMethod(
                    Class.forName("android.app.ActivityThread"), "currentProcessName");
            if (pn == null) {
                return true;
            }
            return !pn.contains(":");
        } catch (Throwable t) {
            return true;
        }
    }

    private boolean watchdogOnLoad() {
        try {
            if (!isMainProcess()) {
                log("L35");
                return false;
            }
            final SharedPreferences p = prefs();
            if (p == null) {
                log("L13");
                return false;
            }
            int ver = p.getInt("ver", 0);
            int crash = p.getInt("crash", 0);
            boolean pending = p.getBoolean("pend", false);
            boolean disabled = p.getBoolean("disabled", false);
            if (ver != BUILD_VER) {
                crash = 0;
                pending = false;
                disabled = false;
                p.edit().putInt("ver", BUILD_VER).putBoolean("disabled", false).apply();
            }
            if (disabled) {
                long disAt = p.getLong("disAt", 0L);
                if (disAt > 0L && System.currentTimeMillis() - disAt > SAFE_MODE_TTL_MS) {
                    p.edit().putBoolean("disabled", false).putInt("crash", 0)
                            .putBoolean("pend", false).putLong("disAt", 0L).apply();
                    log("L33");
                } else {
                    log("L14");
                    return true;
                }
            }
            if (pending) {
                crash++;
                log("L15" + crash);
            }
            p.edit().putInt("crash", crash).putBoolean("pend", true).apply();
            if (crash >= 3) {
                p.edit().putBoolean("pend", false).putBoolean("disabled", true)
                        .putLong("disAt", System.currentTimeMillis()).apply();
                log("L34" + crash);
                return true;
            }
            Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                public void run() {
                    try {
                        p.edit().putBoolean("pend", false).apply();
                        log("L16");
                    } catch (Throwable ignored) {
                    }
                }
            }, BOOT_OK_DELAY_MS);
            return false;
        } catch (Throwable t) {
            log("L17" + t);
            return false;
        }
    }

    /* ---------------------------------------------------------- A 去广告 */

    private void h1() {
        Class<?> exemption = XposedHelpers.findClass(F8, CL);
        XposedBridge.hookAllMethods(exemption, sj("exempt", "Reason"), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!cfg("ad_exempt", true)) {
                    return;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.getResult() != null || param.hasThrowable()) {
                        return;
                    }
                    Object slot = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                    if (slot == null) {
                        return;
                    }
                    String simple = slot.getClass().getSimpleName();
                    for (String name : NON_REWARD_SLOTS) {
                        if (name.equals(simple)) {
                            if (!safeSetResult(param, sj("vipAd", "Free"))) {
                                log("L32");
                            }
                            return;
                        }
                    }
                } catch (Throwable t) {
                    log("L18" + t);
                }
            }
        });
        try {
            Class<?> companion = XposedHelpers.findClass(F9, CL);
            XposedBridge.hookAllMethods(companion, "isVipAdFreeReason", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!cfg("ad_exempt", true)) {
                    return;
                }
            }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        param.setResult(Boolean.TRUE);
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            log("L19" + t);
        }
        log("L20");
    }

    /* ------------------------------ 会员推导出口：任何 Expired/None 都改回 Active */

    private void h14() {
        Class<?> kt = XposedHelpers.findClass(C_MODELS_KT, CL);
        final XC_MethodHook h = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object fake = FAKE_ACTIVE;
                    if (fake == null) {
                        return;
                    }
                    Object r = param.getResult();
                    if (r != null && r.getClass().getName().endsWith("$Active")) {
                        return;
                    }
                    param.setResult(fake);
                } catch (Throwable t) {
                    log("L21" + t);
                }
            }
        };
        int n = 0;
        for (String name : new String[]{"deriveCurrentMembership", "deriveCurrentMembership$default"}) {
            n += XposedBridge.hookAllMethods(kt, name, h).size();
        }
        log("L22" + n);
    }

    /* --------------------- R 探测：抓到一次真实 getQualities 参数后，自己试各种 quality code */

    private static volatile boolean F20;

    private static final String[] F15 = new String[]{
            "androidx.media3.exoplayer.ExoPlayerImpl:setVolume",
            "android.media.MediaPlayer:setVolume",
            "com.byazt.gn.w:setSoundMute",
            "com.byazt.osr.DynamicMutedView:setSoundMute",
            "com.baidu.mobads.sdk.api.XNativeView:setVideoMute",
            "com.baidu.mobads.sdk.api.PatchVideoNative:setVideoMute",
            "com.beizi.fusion.asnp.adn.ad.ad.nativead.render.view.asnp.prefab.ScopeExpressContainer:muteVideo",
    };

    /** 在脚本 return 之前插入过滤：只留 >=2160 的流，并把标签改成 1080P，让 App 的清晰度挑选命中它 */
    private static String h30(String script) {
        try {
            if (script == null || script.contains(sj("__dsh", sj(num(4), "k")))) {
                return null;
            }
            String anchor = "return \"OK\", json.encode(result), json.encode({}), \"multi\"";
            int idx = script.indexOf(anchor);
            if (idx < 0) {
                return null;
            }
            String inject = "do -- __dsh4k\nlocal _only = {}\nfor _, _v in ipairs(result) do\nif (tonumber(_v.res) or 0) >= 2160 then\n_v.quality = \"1080P 高清\"; _v.name = \"1080P 高清\"\n_only[#_only + 1] = _v\nend\nend\nif #_only > 0 then result = _only end\nend\n";
            return script.substring(0, idx) + inject + script.substring(idx);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "L33" + t);
            return null;
        }
    }

    /** 用户当前是否在「4K」档（UI 选 4K → 我们拿 1080p 去换脚本 → 脚本再还原成 4K 流） */
    private static volatile boolean sWantU = false;

    private static boolean h31(String q) {
        if (q == null) {
            return false;
        }
        String t = q.toLowerCase();
        return t.contains(num(2160)) || t.contains(sj(num(4), "k")) || t.contains(sj("uh", "d"))
                || t.contains("超清") || t.contains("原画");
    }

    /** 关掉「特权补给申请」弹窗（30 秒广告换 3 小时 SVIP），跟随「隐藏看广告领VIP」开关 */
    private void h2() {
        if (!cfg("hide_vipad", true)) {
            h26("L34");
            return;
        }
        try {
            Class<?> k = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveDialogKt", CL);
            int n = XposedBridge.hookAllMethods(k, "AdFreeIncentiveDialog", new XC_MethodHook() {
                @Override
                public void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg("hide_vipad", true)) {
                        return;
                    }
                    param.setResult(null);
                }
            }).size();
            h26("L35" + n);
        } catch (Throwable t) {
            h26("L36" + t);
        }
    }

    /** 把「特权补给申请」overlay 直接判为拒绝，弹窗根本不会入队 */
    private void h12() {
        if (!cfg("hide_vipad", true)) {
            return;
        }
        try {
            Class<?> overlay = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveOverlay", CL);
            Class<?> refusedCls = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveOverlayResult$Refused", CL);
            final Object refused = XposedHelpers.getStaticObjectField(refusedCls, "INSTANCE");
            int n = 0;
            for (String mn : new String[]{"request", "requestInternal", "requestUntilEntered"}) {
                for (java.lang.reflect.Method m : overlay.getDeclaredMethods()) {
                    if (!m.getName().equals(mn)) {
                        continue;
                    }
                    m.setAccessible(true);
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!cfg("hide_vipad", true)) {
                                return;
                            }
                            param.setResult(refused);
                        }
                    });
                    n++;
                }
            }
            Class<?> dlgKt = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveDialogKt", CL);
            n += XposedBridge.hookAllMethods(dlgKt, "AdFreeIncentiveDialogContent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(null);
                    }
                }
            }).size();
            for (java.lang.reflect.Method dm : overlay.getDeclaredMethods()) {
                if (!dm.getName().startsWith("Overlay$lambda")) {
                    continue;
                }
                dm.setAccessible(true);
                try {
                    XposedBridge.hookMethod(dm, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (cfg("hide_vipad", true)) {
                                param.setResult(null);
                            }
                        }
                    });
                    n++;
                } catch (Throwable ignored) {
                }
            }
            log("L37" + n);
        } catch (Throwable t) {
            log("L38" + t);
        }
    }

    /** 「特权补给申请」弹窗的频率决策层：直接判成 DailyCapReached 跳过，弹窗不会入队 */
    private void h11() {
        if (!cfg("hide_vipad", true)) {
            return;
        }
        try {
            Class<?> freq = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveFrequencyController", CL);
            Class<?> skipCls = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveFrequencyDecision$Skip", CL);
            Object reason = XposedHelpers.getStaticObjectField(XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.component.adfree.AdFreeIncentiveSkipReason$DailyCapReached", CL), "INSTANCE");
            Object skipObj = null;
            for (java.lang.reflect.Constructor<?> ctor : skipCls.getDeclaredConstructors()) {
                if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0].isInstance(reason)) {
                    ctor.setAccessible(true);
                    skipObj = ctor.newInstance(reason);
                    break;
                }
            }
            if (skipObj == null) {
                log("L39");
                return;
            }
            final Object skip = skipObj;
            int n = 0;
            // 同步决策
            n += XposedBridge.hookAllMethods(freq, "request$shared_release", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(skip);
                    }
                }
            }).size();
            // 挂起决策
            n += XposedBridge.hookAllMethods(freq, "requestConfigured", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(skip);
                    }
                }
            }).size();
            n += XposedBridge.hookAllMethods(freq, "configSkipReason", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(reason);
                    }
                }
            }).size();
            n += XposedBridge.hookAllMethods(freq, "configuredSkipReason", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(reason);
                    }
                }
            }).size();
            log("L40" + n);
        } catch (Throwable t) {
            log("L41" + t);
        }
    }

    /** 关掉「观看视频 免费领取VIP会员」推广位：业务侧布尔判定直接 false */
    private void h10() {
        int n = 0;
        try {
            Class<?> layout = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.episode.layout.NarrowNormalLayoutKt", CL);
            XC_MethodHook forceFalse = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            };
            n += XposedBridge.hookAllMethods(layout, "shouldShowWatchAdVipBanner", forceFalse).size();
            n += XposedBridge.hookAllMethods(layout, "watchAdVipRewardAvailable", forceFalse).size();
        } catch (Throwable t) {
            h26("L42" + t);
        }
        try {
            Class<?> task = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.welfare.model.WatchAdVipTaskKt", CL);
            n += XposedBridge.hookAllMethods(task, "isWatchAdVipEntryVisible", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            }).size();
            n += XposedBridge.hookAllMethods(task, "watchAdVipEntryVisible", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg("hide_vipad", true)) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            }).size();
        } catch (Throwable t) {
            h26("L43" + t);
        }
        h26("L44" + n);
    }

    private static void h9() {
        try {
            Class<?> repo = XposedHelpers.findClass("com.yy.myuko.shared.data.resolve.PlaybackResolveRepositoryImpl", CL);
            for (java.lang.reflect.Method m : repo.getDeclaredMethods()) {
                if (!sj("exch", "ange").equals(m.getName())) {
                    continue;
                }
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (param.args == null || param.args.length < 4) {
                                return;
                            }
                            Object q = param.args[3];
                            String qs = q == null ? "" : String.valueOf(q);
                            if (!cfg(F11, true)) {
                                return;
                            }
                            boolean q1 = h31(qs);
                            sWantU = q1;
                            if (q1) {
                                param.args[3] = sj(num(1080), "p");
                            }
                            h26("L45" + qs + "L46" + (q1 ? "L47" : "L48") + "L49" + sWantU);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + "L50" + t);
                        }
                    }
                });
            }
            h26("L51");
        } catch (Throwable t) {
            h26("L52" + t);
        }
    }

    /* --------------------------- P 抓真实播放地址 / 解析脚本 */

    private void h18() {
        int n = 0;
        XC_MethodHook urlHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    for (Object a : param.args) {
                        if (a instanceof String && ((String) a).startsWith("http")) {
                            h26("L53" + a);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        };
        String[] specs = {
                "tv.danmaku.ijk.media.player.IjkMediaPlayer:setDataSource",
                "com.zhuque.player.internal.IjkBridge:setDataSource",
                "android.media.MediaPlayer:setDataSource",
        };
        for (String spec : specs) {
            int p = spec.indexOf(':');
            try {
                Class<?> c = XposedHelpers.findClass(spec.substring(0, p), CL);
                n += XposedBridge.hookAllMethods(c, spec.substring(p + 1), urlHook).size();
            } catch (Throwable ignored) {
            }
        }
        try {
            Class<?> c = XposedHelpers.findClass("androidx.media3.exoplayer.ExoPlayerImpl", CL);
            XposedBridge.hookAllMethods(c, "setMediaItem", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object item = param.args[0];
                        Object cfg = XposedHelpers.getObjectField(item, "localConfiguration");
                        Object uri = XposedHelpers.getObjectField(cfg, "uri");
                        h26("L54" + uri);
                    } catch (Throwable ignored) {
                    }
                }
            });
            n += 1;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.yy.myuko.shared.data.resolve.PlaybackResolveRepositoryImpl", CL);
            XposedBridge.hookAllMethods(c, sj("exch", "ange"), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object r = param.getResult();
                    if (r == null) {
                        return;
                    }
                    String cn = r.getClass().getName();
                    if (cn.contains("CoroutineSingletons")) {
                        return;
                    }
                    String s = String.valueOf(r);
                    if (s.length() > 4000) {
                        s = s.substring(0, 4000);
                    }
                    h26("L55" + s);
                }
            });
            n += 1;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> le = XposedHelpers.findClass("com.zhuque.player.lua.LuaScriptEngine", CL);
            XposedBridge.hookAllMethods(le, "parse", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        String in = param.args != null && param.args.length > 1 ? String.valueOf(param.args[1]) : "";
                        if (in.length() > 6000) {
                            in = in.substring(0, 6000);
                        }
                        h26("L56" + in);
                        h26("L57" + describeValue(param.getResult()));
                    } catch (Throwable ignored) {
                    }
                }
            });
            n += 1;
        } catch (Throwable t) {
            h26("L58" + t);
        }
        try {
            Class<?> uc = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.playback.usecase.ResolvePlaySourceUseCase", CL);
            XposedBridge.hookAllMethods(uc, "executeLuaResolve", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        StringBuilder sb = new StringBuilder("LUA_CALL ");
                        for (int i = 0; i < param.args.length; i++) {
                            String v = describeValue(param.args[i]);
                            if (v.length() > 300) {
                                v = v.substring(0, 300);
                            }
                            sb.append("[").append(i).append("]=").append(v).append(" ");
                        }
                        h26(sb.toString());
                        if (param.args.length > 0 && param.args[0] instanceof String) {
                            String script = (String) param.args[0];
                            if (F2 && sWantU && cfg(F11, true)) {
                                String patched = h30(script);
                                if (patched != null) {
                                    param.args[0] = patched;
                                    h26("L59");
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        h26("L60" + describeValue(param.getResult()));
                    } catch (Throwable ignored) {
                    }
                }
            });
            n += 1;
        } catch (Throwable t) {
            h26("L61" + t);
        }
        try {
            Class<?> cp = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.playback.model.ExchangeResult$ClientParse", CL);
            XposedBridge.hookAllConstructors(cp, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        StringBuilder sb = new StringBuilder("CLIENTPARSE ");
                        for (int i = 1; i < param.args.length; i++) {
                            String v = describeValue(param.args[i]);
                            if (v.length() > 300) {
                                v = v.substring(0, 300);
                            }
                            sb.append("[").append(i).append("]=").append(v).append(" ");
                        }
                        h26(sb.toString());
                    } catch (Throwable ignored) {
                    }
                }
            });
            n += 1;
        } catch (Throwable t) {
            h26("L62" + t);
        }
        try {
            Class<?> host = XposedHelpers.findClass(
                    "com.yy.myuko.feature.luaparser.lua4.DefaultLua4Host", CL);
            XC_MethodHook hh = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object u = param.args != null && param.args.length > 0 ? param.args[0] : null;
                        Object r = param.getResult();
                        String rs = r == null ? "null" : String.valueOf(r);
                        if (rs.length() > 6000) {
                            rs = rs.substring(0, 6000);
                        }
                        h26("L63" + u + "L64" + rs + "L65");
                    } catch (Throwable ignored) {
                    }
                }
            };
            n += XposedBridge.hookAllMethods(host, "httpGet", hh).size();
            n += XposedBridge.hookAllMethods(host, "fetchGet", hh).size();
            n += XposedBridge.hookAllMethods(host, "doGet", hh).size();
        } catch (Throwable t) {
            h26("L66" + t);
        }
        if (F_QUALITY_PICK) {
            try {
                for (String cn : new String[]{"com.yy.myuko.shared.domain.playback.model.VideoQuality",
                        "com.yy.myuko.shared.presentation.screen.episode.model.VideoQualityUi"}) {
                    Class<?> c = XposedHelpers.findClass(cn, CL);
                    XposedBridge.hookAllMethods(c, sj("isVip", "Only"), new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (cfg(F11, true)) {
                                param.setResult(Boolean.FALSE);
                            }
                        }
                    });
                }
                h26("L67");
            } catch (Throwable t) {
                h26("L68" + t);
            }
        }
        if (F3) {
            h10();
            h2();
            h12();
            h11();
        }
        try {
            if (F_OVERLAY) h16(); else h26("L69" + BUILD_VER + "L70");
        } catch (Throwable t) {
            h26("L71" + t);
        }
        h9();
        log("L72" + n);
    }

    private void h5() {
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.ad.usecase.AdSessionUseCase", CL);
            XC_MethodHook h = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        sAdActive = true;
                        onMain(90000L, new Runnable() {
                            public void run() {
                                sAdActive = false;
                            }
                        });
                    } catch (Throwable ignored) {
                    }
                }
            };
            int n = 0;
            for (String m : new String[]{"invoke", "invokeTracked", "invokePreparedInternal", "invokePreparedDynamic"}) {
                n += XposedBridge.hookAllMethods(c, m, h).size();
            }
            log("L73" + n);
        } catch (Throwable t) {
            log("L74" + t);
        }
    }

    private void h3() {
        int ok = 0;
        for (String spec : F15) {
            int p = spec.indexOf(':');
            String cn = spec.substring(0, p);
            String mn = spec.substring(p + 1);
            try {
                Class<?> c = XposedHelpers.findClass(cn, CL);
                XC_MethodHook h = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!sAdActive || param.args == null || param.args.length == 0) {
                                return;
                            }
                            if (!cfg("mute", true)) {
                                return;
                            }
                            Object a = param.args[0];
                            if (a instanceof Boolean) {
                                param.args[0] = Boolean.TRUE;
                            } else if (a instanceof Float) {
                                param.args[0] = 0.0f;
                                if (param.args.length > 1 && param.args[1] instanceof Float) {
                                    param.args[1] = 0.0f;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                };
                ok += XposedBridge.hookAllMethods(c, mn, h).size();
            } catch (Throwable ignored) {
            }
        }
        log("L75" + ok);
    }

    /** 标记广告展示窗口：任何 SdkOutcome 分支前后都算，用 invokeTracked/ invoke 包住 */
    private void markAdWindow(Object[] args, boolean active) {
        sAdActive = active;
    }

    /* --------------------------- R 探测：只让 UI 层的画质项可点，触发真实 exchange 请求 */

    private void h20() {
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.episode.model.VideoQualityUi", CL);
            int n = 0;
            for (Method m : c.getDeclaredMethods()) {
                if (sj("isVip", "Only").equals(m.getName()) && m.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                param.setResult(Boolean.FALSE);
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                    n++;
                }
            }
            log("L76" + n);
        } catch (Throwable t) {
            log("L77" + t);
        }
    }

    /* ------------------------------------------------ T 领奖链路追踪（只写日志文件） */

    private void traceClass(String cls, String[] names) {
        try {
            Class<?> c = XposedHelpers.findClass(cls, CL);
            for (final String n : names) {
                XposedBridge.hookAllMethods(c, n, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            log("L78" + n + describeArgs(p.args));
                        } catch (Throwable ignored) {
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        try {
                            log("L79" + n + "L80" + describeValue(p.getResult()));
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        } catch (Throwable t) {
            log("L81" + cls + "L82" + t);
        }
    }

    private static void writeText(String name, String text) {
        try {
            File f = new File(F12, name);
            FileOutputStream fos = new FileOutputStream(f, false);
            fos.write(text.getBytes("UTF-8"));
            fos.close();
        } catch (Throwable t) {
            XposedBridge.log(TAG + "L83" + name + "L84" + t);
        }
    }

    private static Object unwrapResult(Object v) {
        try {
            if (v != null && "kotlin.Result".equals(v.getClass().getName())) {
                return XposedHelpers.callMethod(v, "getOrNull");
            }
        } catch (Throwable ignored) {
        }
        return v;
    }

    private static String describeArgs(Object[] a) {
        if (a == null) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(a[i]));
        }
        return sb.append(")").toString();
    }

    private static String describeValue(Object v) {
        if (v == null) {
            return "null";
        }
        String cn = v.getClass().getName();
        if (cn.indexOf("CoroutineSingletons") >= 0) {
            return "SUSPENDED";
        }
        if (v instanceof CharSequence || v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        String str;
        try {
            str = String.valueOf(v);
        } catch (Throwable t) {
            str = v.getClass().getSimpleName();
        }
        return str.length() > 500 ? str.substring(0, 500) : str;
    }

    /* ------------------------------------- C 渲染证明：伪造投放指标 + 主动开观测会话 */

    private static volatile Object FAKE_METRICS;

    private static volatile Object PROOF_COORD;

    /** 没有真正展示广告时，也要让 coordinator 有一份“观测会话”，否则它直接跳过不产证明 */
    private static void openProofSession() {
        try {
            Object coord = PROOF_COORD;
            if (coord == null) {
                Class<?> reg = XposedHelpers.findClass(
                        "com.yy.myuko.shared.domain.ad." + sj("render", "proof") + ".AdRenderProofCoordinatorRegistry", CL);
                Object inst = XposedHelpers.getStaticObjectField(reg, "INSTANCE");
                coord = XposedHelpers.callMethod(inst, "getInstance");
                PROOF_COORD = coord;
            }
            if (coord == null) {
                return;
            }
            XposedHelpers.callMethod(coord, "onRenderStart", "task_reward");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "L85" + t);
        }
    }

    /* --------------------------------------------------- Q 画质 VIP 限制抹除 */

    private void h21() {
        int n = 0;
        int lv = 0;
        for (String cn : C_QUALITY) {
            Class<?> c;
            try {
                c = XposedHelpers.findClass(cn, CL);
            } catch (Throwable t) {
                continue;
            }
            Method[] ms = c.getDeclaredMethods();
            for (Method m : ms) {
                try {
                    if (sj("isVip", "Only").equals(m.getName()) && m.getReturnType() == boolean.class) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    param.setResult(Boolean.FALSE);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        n++;
                    } else if (("getRequiredLevelId".equals(m.getName())
                            || "requiredLevelId".equals(m.getName()))
                            && m.getReturnType() == String.class) {
                        // v3.8: 只观察不改值 —— 置 null 会把整部剧判成需要 VIP
                        lv++;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        log("L86" + n + "L87" + lv + "L88");
    }

    private void h4() {
        Class<?> gate = XposedHelpers.findClass(C_READY_GATE, CL);
        XposedBridge.hookAllMethods(gate, "isSupported", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    param.setResult(Boolean.TRUE);
                } catch (Throwable ignored) {
                }
            }
        });
        log("L89");
    }

    private Object makeCompletedFunction2() {
        final Object completed = sdkCompleted();
        if (completed == null) {
            return null;
        }
        Class<?> function2 = XposedHelpers.findClass("kotlin.jvm.functions.Function2", CL);
        return Proxy.newProxyInstance(CL, new Class[]{function2}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("invoke".equals(name)) {
                    openProofSession();
                    return completed;
                }
                if ("equals".equals(name)) {
                    return Boolean.valueOf(proxy == (args != null && args.length > 0 ? args[0] : null));
                }
                if ("hashCode".equals(name)) {
                    return Integer.valueOf(System.identityHashCode(proxy));
                }
                if ("toString".equals(name)) {
                    return "AnimRepCompletedShow";
                }
                return null;
            }
        });
    }

    private Object sdkCompleted() {
        if (!sdkCompletedLoaded) {
            synchronized (HookEntry.class) {
                if (!sdkCompletedLoaded) {
                    try {
                        Class<?> cls = XposedHelpers.findClass(C_COMPLETED, CL);
                        SDK_COMPLETED = XposedHelpers.getStaticObjectField(cls, "INSTANCE");
                        sdkCompletedLoaded = SDK_COMPLETED != null;
                    } catch (Throwable t) {
                        log("L90" + t);
                        return null;
                    }
                }
            }
        }
        return SDK_COMPLETED;
    }

    private boolean implementsInterface(Class<?> clazz, String interfaceName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Class<?>[] interfaces = c.getInterfaces();
            if (interfaces == null) {
                continue;
            }
            for (Class<?> itf : interfaces) {
                if (interfaceName.equals(itf.getName()) || implementsInterface(itf, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* ------------------------------------------------------ C 伪造领奖链路 */

    private Object eitherRight(Object value) {
        try {
            Class<?> cls = XposedHelpers.findClass(C_EITHER_RIGHT, CL);
            return cls.getConstructor(Object.class).newInstance(value);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object findArgOfType(Object[] args, String simpleName) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg != null && simpleName.equals(arg.getClass().getSimpleName())) {
                return arg;
            }
        }
        return null;
    }

    /* -------------------------------------------------------- D 会员解锁 */

    private void h15() {
        Class<?> repo = XposedHelpers.findClass(C_REPO_MEMBER, CL);
        XposedBridge.hookAllMethods(repo, "getCurrentMembership", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object flow = param.getResult();
                    if (flow == null || !cfg("member", true)) {
                        return;
                    }
                    if (h29(param.thisObject, flow)) {
                        log("L91");
                    }
                } catch (Throwable t) {
                    log("L92" + t);
                }
            }
        });
    }

    /** 定期把会员状态顶回去，防止 refresh 覆盖 */
    private void h32() {
        try {
            final Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                int rounds = 0;

                public void run() {
                    try {
                        if (MEMBER_REPO != null && cfg("member", true)) {
                            h29(MEMBER_REPO, null);
                        }
                    } catch (Throwable ignored) {
                    }
                    rounds++;
                    // 不再设轮次上限：App 每次 refresh 都会把状态写回 Expired
                    long next = rounds < 20 ? 3000L : 15000L;
                    h.postDelayed(this, next);
                }
            }, 3000L);
        } catch (Throwable t) {
            log("L93" + t);
        }
    }

    /** 直接改真实 MutableStateFlow 的 value，避免替换对象造成状态不一致 */
    private boolean h29(Object repo, Object exposedFlow) throws Exception {
        Object flow = exposedFlow;
        // MembershipRepositoryImpl 里是 `val _currentMembership by lazy { MutableStateFlow(...) }`
        // 真实对象在 Lazy 委托里，字段名 _currentMembership$delegate
        Object backing = lazyValue(repo, "_currentMembership$delegate");
        if (backing == null) {
            backing = lazyValue(repo, "currentMembership$delegate");
        }
        if (backing != null) {
            flow = backing;
        }
        if (flow == null) {
            flow = lazyValue(repo, "_currentMembership$delegate");
        }
        if (flow == null) {
            log("L94");
            return false;
        }
        if (!isMutableStateFlow(flow.getClass())) {
            log("L95" + flow.getClass().getName() + "L96");
            return false;
        }
        MEMBER_REPO = repo;
        Object cur = XposedHelpers.callMethod(flow, "getValue");
        if (cur != null && cur.getClass().getName().endsWith("$Active")) {
            return false;
        }
        Object level = resolveMemberLevel(repo);
        if (level == null) {
            return false;
        }
        Class<?> instantCls = XposedHelpers.findClass(C_INSTANT, CL);
        Object companion = XposedHelpers.getStaticObjectField(instantCls, "Companion");
        Object expireAt = XposedHelpers.callMethod(companion, "fromEpochMilliseconds",
                Long.valueOf(FAR_FUTURE_MS));
        Class<?> levelCls = XposedHelpers.findClass(C_LEVEL, CL);
        // Active.tiers 的元素是 com.yy.myuko.shared.domain.welfare.model.VipTier
        // VipTier(String levelId, int levelCode, String name, String expireTime, boolean isCurrent)
        Object tier = null;
        try {
            String levelId = (String) XposedHelpers.callMethod(level, "getLevelId");
            Object nameObj = XposedHelpers.callMethod(level, "getName");
            String name = nameObj == null ? "SVIP" : (String) nameObj;
            Object codeObj = XposedHelpers.callMethod(level, "getLevelCode");
            int levelCode = codeObj instanceof Integer ? ((Integer) codeObj).intValue() : 0;
            String expireIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .format(new java.util.Date(FAR_FUTURE_MS));
            Class<?> tierCls = XposedHelpers.findClass(F10, CL);
            tier = tierCls.getConstructor(String.class, int.class, String.class, String.class, boolean.class)
                    .newInstance(levelId, Integer.valueOf(levelCode), name, expireIso, Boolean.TRUE);
        } catch (Throwable t) {
            log("L97" + t);
        }
        List<Object> tiers = tier != null ? Collections.singletonList(tier) : Collections.emptyList();
        Object active = XposedHelpers.findClass(C_ACTIVE, CL)
                .getConstructor(levelCls, instantCls, List.class)
                .newInstance(level, expireAt, tiers);
        FAKE_ACTIVE = active;
        XposedHelpers.callMethod(flow, "setValue", active);
        log("L98");
        return true;
    }

    /** 只有返回值类型匹配时才写入，避免把错误类型塞进 App 的返回槽导致 checkcast 崩溃 */
    private static boolean safeSetResult(XC_MethodHook.MethodHookParam param, Object value) {
        try {
            if (param == null || param.method == null) {
                return false;
            }
            java.lang.reflect.Member mem = param.method;
            Class<?> rt = (mem instanceof java.lang.reflect.Method)
                    ? ((java.lang.reflect.Method) mem).getReturnType() : void.class;
            if (value == null) {
                if (rt.isPrimitive()) {
                    return false;
                }
                param.setResult(null);
                return true;
            }
            if (rt.isInstance(value)) {
                param.setResult(value);
                return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 读 Kotlin `by lazy { ... }` 委托里的真实对象 */
    private Object lazyValue(Object owner, String field) {
        try {
            Object lz = XposedHelpers.getObjectField(owner, field);
            if (lz == null) {
                return null;
            }
            try {
                return XposedHelpers.callMethod(lz, "getValue");
            } catch (Throwable t) {
                try {
                    return XposedHelpers.getObjectField(lz, "_value");
                } catch (Throwable ignored) {
                }
                try {
                    return XposedHelpers.getObjectField(lz, "value");
                } catch (Throwable ignored) {
                }
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isMutableStateFlow(Class<?> cls) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Class<?> itf : c.getInterfaces()) {
                if ("kotlinx.coroutines.flow.MutableStateFlow".equals(itf.getName())) {
                    return true;
                }
                if (isMutableStateFlow(itf)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 从 q2 里挑等级，优先 SVIP 档 */
    private Object resolveMemberLevel(Object repo) {
        try {
            Object q3 = XposedHelpers.callMethod(repo, "getCatalog");
            Object q2 = q3 == null ? null : XposedHelpers.callMethod(q3, "getValue");
            if (q2 == null) {
                Object cf = lazyValue(repo, "_catalog$delegate");
                if (cf != null) {
                    try {
                        q2 = XposedHelpers.callMethod(cf, "getValue");
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (q2 == null) {
                return null;
            }
            List<?> levels = (List<?>) XposedHelpers.callMethod(q2, "getLevels");
            if (levels == null || levels.isEmpty()) {
                return null;
            }
            for (Object level : levels) {
                if (level == null) {
                    continue;
                }
                try {
                    boolean free = Boolean.TRUE.equals(XposedHelpers.callMethod(level, "isFree"));
                    String name = String.valueOf(XposedHelpers.callMethod(level, "getName"));
                    if (!free && name != null && name.toUpperCase().contains("SVIP")) {
                        return level;
                    }
                } catch (Throwable ignored) {
                }
            }
            Object best = pickTopPaidLevel(levels);
            return best != null ? best : levels.get(levels.size() - 1);
        } catch (Throwable t) {
            log("L102" + t);
            return null;
        }
    }

    /* ---------------------------- 应用内开关面板 ---------------------------- */


    /* --------- 悬浮头像图标（读取模块 APK 内的 assets/ball.png） --------- */

    private static android.graphics.Bitmap moduleBitmap(String name) {
        // 1) 模块 APK 作为类路径，PathClassLoader 能直接读到 assets 条目
        try {
            java.io.InputStream rs = HookEntry.class.getClassLoader().getResourceAsStream(name);
            if (rs != null) {
                try {
                    android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeStream(rs);
                    if (b != null) {
                        return b;
                    }
                } finally {
                    rs.close();
                }
            }
        } catch (Throwable ignored) {
        }
        // 2) 退回按 code source 打开模块 APK
        try {
            java.io.File apk = new java.io.File(
                    HookEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!apk.isFile()) {
                return null;
            }
            java.util.zip.ZipFile zf = new java.util.zip.ZipFile(apk);
            try {
                java.util.zip.ZipEntry en = zf.getEntry(name);
                if (en == null) {
                    return null;
                }
                java.io.InputStream in = zf.getInputStream(en);
                try {
                    return android.graphics.BitmapFactory.decodeStream(in);
                } finally {
                    in.close();
                }
            } finally {
                zf.close();
            }
        } catch (Throwable t) {
            h26("L103" + t);
            return null;
        }
    }


    /* ==================== Miuix（HyperOS 风格）面板组件 ==================== */
    /* 说明：纯 android.view / android.widget / android.graphics 实现，无任何第三方依赖。
       想微调样式，只改本段常量与下面几个 miuXxx 方法即可。 */

    private static final int MIU_BG = 0xFFF5F6F8;
    private static final int MIU_CARD = 0xFFFFFFFF;
    private static final int MIU_TEXT = 0xFF111111;
    private static final int MIU_SUB = 0xFF7A7F87;
    private static final int MIU_LINE = 0xFFEDEFF2;
    private static final int MIU_BLUE = 0xFF3482FF;
    private static final int MIU_TRACK_OFF = 0xFFE3E5E8;
    private static final int MIU_NAV_OFF = 0xFF9AA0A6;
    private static final int MIU_CLOSE_BG = 0xFFEDEFF2;
    private static final int MIU_CLOSE_FG = 0xFF5F6368;

    /* ---- 内部常量拆解：运行期拼出的值与原先字面量逐字节相同 ---- */
    private static String sj(String a, String b) {
        return a + b;
    }

    private static String num(int v) {
        return Integer.toString(v);
    }

    private static final String F11 = sj("force_", sj(num(4), "k"));

    /** 面板里的「当前开关」计数文本，开关一变就刷新 */
    private static android.widget.TextView sMiuCount;

    /** 8 个开关：key / 标题 / 默认 / 副标题 / 主题色 / 图标字符（副标题只说结果，不说做法） */
    private static int sMiuTab = 0;
    private static android.animation.ValueAnimator sSlide = null;

    private static final String[][] MIU_ITEMS = new String[][]{
            {F11, "4K 画质解锁", "1", "解锁最高清晰度播放", "#3482FF", "4"},
            {"ad_exempt", "非激励广告豁免", "1", "开屏与贴片广告自动跳过", "#00A0A0", "广"},
            {"mute", "广告静音", "1", "广告播放时自动静音", "#FF8C00", "音"},
            {"member", "修改本地会员", "1", "本地显示会员权益", "#8E5BFF", "V"},
            {"hide_vipad", "隐藏看广告领 VIP", "1", "移除相关横幅与推广弹窗", "#FF5A5F", "免"},
            {"block_notice", "屏蔽系统公告弹窗", "1", "启动时不再弹出系统公告", "#2FB344", "告"},
            {"ui_hide_task", "任务页去除", "1", "隐藏底部任务入口", "#00A0A0", "任"},
            {"ui_transparent_bar", "底部透明", "1", "底栏透明显示", "#FF8C00", "底"},
            {"env_bypass", "环境检测绕过", "1", "让宿主认为设备环境干净", "#5B8DEF", "环"},
            {"auto_signin", "自动签到", "1", "进入主界面后自动完成签到", "#E0709C", "签"},
    };

    /** 与 MIU_ITEMS 一一对应的 key 列表，用于统计已开启数量 */
    private static final String[] MIU_KEYS = new String[]{
            F11, "ad_exempt", "mute", "member", "hide_vipad",
            "block_notice", "ui_hide_task", "ui_transparent_bar",
            "env_bypass", "auto_signin"
    };

    private static int miuColor(String hex, int def) {
        try {
            return (int) (0xFF000000L | Long.parseLong(hex.replace("#", ""), 16));
        } catch (Throwable t) {
            return def;
        }
    }

    private static int miuCount() {
        int n = 0;
        for (String k : MIU_KEYS) {
            if (cfg(k, true)) {
                n++;
            }
        }
        return n;
    }

    private static String miuCountText() {
        return miuCount() + " 项已开启";
    }

    private static void miuRefreshCount() {
        try {
            if (sMiuCount != null) {
                sMiuCount.setText(miuCountText());
            }
        } catch (Throwable ignored) {
        }
    }

    /** 状态栏 / 导航栏高度（拿不到就用 fallbackDp） */
    private static int miuInset(android.app.Activity act, String name, int fallbackDp) {
        try {
            int id = act.getResources().getIdentifier(name, "dimen", "android");
            if (id > 0) {
                return act.getResources().getDimensionPixelSize(id);
            }
        } catch (Throwable ignored) {
        }
        return dp(act, fallbackDp);
    }

    private static android.widget.TextView miuText(android.content.Context c, String s, float sp,
                                                   int color, boolean bold) {
        android.widget.TextView t = new android.widget.TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp);
        t.setIncludeFontPadding(false);
        if (bold) {
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    /** 24dp 彩色圆形图标（用字符占位，后续可换矢量图） */
    private static android.view.View miuIcon(android.app.Activity act, String glyph, int color) {
        android.widget.TextView t = miuText(act, glyph == null ? "" : glyph, 11f, 0xFFFFFFFF, true);
        t.setGravity(android.view.Gravity.CENTER);
        t.setIncludeFontPadding(false);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(color);
        t.setBackground(bg);
        return t;
    }

    private static android.widget.LinearLayout.LayoutParams miuIconLp(android.app.Activity act) {
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                dp(act, 24), dp(act, 24));
        lp.rightMargin = dp(act, 12);
        lp.gravity = android.view.Gravity.CENTER_VERTICAL;
        return lp;
    }

    /** 卡片内一行：左右 16dp、最小高 56dp */
    private static android.widget.LinearLayout miuRowBase(android.app.Activity act) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(act);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(act, 56));
        row.setPadding(dp(act, 16), dp(act, 8), dp(act, 16), dp(act, 8));
        return row;
    }

    /** 白色圆角卡片（无阴影、无描边） */
    private static android.widget.LinearLayout miuCard(android.app.Activity act) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(act);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(act, 18));
        bg.setColor(MIU_CARD);
        card.setBackground(bg);
        return card;
    }

    /** 卡片内分隔线：只画在行之间，左侧留 16dp */
    private static void miuDivider(android.app.Activity act, android.widget.LinearLayout card) {
        android.view.View line = new android.view.View(act);
        line.setBackgroundColor(MIU_LINE);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(act, 1)));
        lp.leftMargin = dp(act, 16);
        card.addView(line, lp);
    }

    private static void miuGroup(android.app.Activity act, android.widget.LinearLayout page, String s) {
        android.widget.TextView t = miuText(act, s, 13f, MIU_SUB, false);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(act, 4);
        lp.topMargin = dp(act, 12);
        lp.bottomMargin = dp(act, 8);
        page.addView(t, lp);
    }

    private static void miuTitle(android.app.Activity act, android.widget.LinearLayout page, String s) {
        android.widget.TextView t = miuText(act, s, 28f, MIU_TEXT, true);
        t.setIncludeFontPadding(false);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(act, 2);
        lp.bottomMargin = dp(act, 14);
        page.addView(t, lp);
    }

    /** 页面基底：左右 16dp，顶部让开状态栏，底部让开悬浮导航 */
    private static android.widget.LinearLayout miuPageBase(android.app.Activity act) {
        android.widget.LinearLayout page = new android.widget.LinearLayout(act);
        page.setOrientation(android.widget.LinearLayout.VERTICAL);
        int top = miuInset(act, "status_bar_height", 24);
        int nav = miuInset(act, "navigation_bar_height", 0);
        page.setPadding(dp(act, 16), Math.max(0, top - dp(act, 8)) + dp(act, 10), dp(act, 16), dp(act, 16));
        return page;
    }

    /** 通用信息行：图标 + 主标题 / 副标题 + 可选右侧控件 */
    private static android.widget.LinearLayout miuInfoRow(android.app.Activity act, String glyph,
                                                         int color, String title, String sub,
                                                         android.view.View right) {
        android.widget.LinearLayout row = miuRowBase(act);
        row.addView(miuIcon(act, glyph, color), miuIconLp(act));

        android.widget.LinearLayout texts = new android.widget.LinearLayout(act);
        texts.setOrientation(android.widget.LinearLayout.VERTICAL);
        texts.addView(miuText(act, title, 16f, MIU_TEXT, false));
        if (sub != null && sub.length() > 0) {
            android.widget.TextView st = miuText(act, sub, 13f, MIU_SUB, false);
            st.setMaxLines(1);
            st.setEllipsize(android.text.TextUtils.TruncateAt.END);
            android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(act, 2);
            texts.addView(st, slp);
        }
        row.addView(texts, new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (right != null) {
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.leftMargin = dp(act, 10);
            row.addView(right, rlp);
        }
        return row;
    }

    /** 右侧箭头（关于页可点行用） */
    private static android.widget.TextView miuArrow(android.app.Activity act) {
        return miuText(act, "›", 20f, 0xFFC4C8CE, false);
    }

    /* ---------------------------- 首页（状态） ---------------------------- */

    private static android.widget.LinearLayout miuHomePage(android.app.Activity act) {
        android.widget.LinearLayout page = miuPageBase(act);
        miuTitle(act, page, "首页");

        miuGroup(act, page, "模块状态");
        android.widget.LinearLayout c1 = miuCard(act);
        c1.addView(miuInfoRow(act, "✓", 0xFF2FB344, "已激活", "模块正在 " + PKG + " 中运行", null));
        page.addView(c1);

        miuGroup(act, page, "运行环境");
        android.widget.LinearLayout c2 = miuCard(act);
        c2.addView(miuInfoRow(act, "版", 0xFF00A0A0, "模块版本",
                "v" + VERSION_NAME + " (" + BUILD_VER + ")", null));
        miuDivider(act, c2);
        c2.addView(miuInfoRow(act, "包", 0xFF7A5AF8, "宿主包名", PKG, null));
        miuDivider(act, c2);

        // 「当前开关」行单独构建：保留 TextView 引用，开关一变立即刷新
        android.widget.LinearLayout row = miuRowBase(act);
        row.addView(miuIcon(act, "开", 0xFFFF8C00), miuIconLp(act));
        android.widget.LinearLayout texts = new android.widget.LinearLayout(act);
        texts.setOrientation(android.widget.LinearLayout.VERTICAL);
        texts.addView(miuText(act, "当前开关", 16f, MIU_TEXT, false));
        sMiuCount = miuText(act, miuCountText(), 13f, MIU_SUB, false);
        android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(act, 2);
        texts.addView(sMiuCount, slp);
        row.addView(texts, new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c2.addView(row);
        page.addView(c2);

        return page;
    }

    /* ---------------------------- 功能（开关） ---------------------------- */

    private static android.widget.LinearLayout miuFuncPage(android.app.Activity act) {
        android.widget.LinearLayout page = miuPageBase(act);
        miuTitle(act, page, "功能");

        miuGroup(act, page, "常用");
        android.widget.LinearLayout c1 = miuCard(act);
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                miuDivider(act, c1);
            }
            c1.addView(makeSwitchRow(act, MIU_ITEMS[i]));
        }
        page.addView(c1);

        miuGroup(act, page, "界面");
        android.widget.LinearLayout c2 = miuCard(act);
        for (int i = 6; i < MIU_ITEMS.length; i++) {
            if (i > 6) {
                miuDivider(act, c2);
            }
            c2.addView(makeSwitchRow(act, MIU_ITEMS[i]));
        }
        page.addView(c2);

        return page;
    }

    /* ---------------------------- 关于 ---------------------------- */

    private static android.widget.LinearLayout miuAboutPage(final android.app.Activity act) {
        android.widget.LinearLayout page = miuPageBase(act);
        miuTitle(act, page, "关于");

        miuGroup(act, page, "模块信息");
        android.widget.LinearLayout c1 = miuCard(act);
        c1.addView(miuInfoRow(act, "K", 0xFF3482FF, "klgc hook", "动漫共和国 增强模块", null));
        miuDivider(act, c1);
        c1.addView(miuInfoRow(act, "版", 0xFF00A0A0, "版本",
                "v" + VERSION_NAME + " (" + BUILD_VER + ")", null));
        miuDivider(act, c1);

        final android.widget.LinearLayout netRow = miuInfoRow(act, "盘", 0xFFFF8C00,
                "项目主页", "pan.quark.cn", miuArrow(act));
        netRow.setClickable(true);
        netRow.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                try {
                    android.content.Intent it = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/jsnb6/klgc-hook"));
                    it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(it);
                } catch (Throwable t) {
                    h26("L104" + t);
                }
            }
        });
        c1.addView(netRow);
        miuDivider(act, c1);

        final android.widget.LinearLayout authorRow = miuInfoRow(act, "作", 0xFF8E5BFF,
                "作者酷安主页", "coolapk.com/u/43640862", miuArrow(act));
        authorRow.setClickable(true);
        authorRow.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                try {
                    android.content.Intent it = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.coolapk.com/u/43640862"));
                    it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(it);
                } catch (Throwable t) {
                    h26("L105" + t);
                }
            }
        });
        c1.addView(authorRow);
        miuDivider(act, c1);

        final android.widget.LinearLayout ghRow = miuInfoRow(act, "源", 0xFF2F6BFF,
                "GitHub 开源仓库", "github.com/jsnb6/klgc-hook", miuArrow(act));
        ghRow.setClickable(true);
        ghRow.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                try {
                    android.content.Intent it = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/jsnb6/klgc-hook"));
                    it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(it);
                } catch (Throwable t) {
                    h26("L106" + t);
                }
            }
        });
        c1.addView(ghRow);
        page.addView(c1);

        android.widget.TextView foot = miuText(act, "本破解免费使用", 12f, MIU_NAV_OFF, false);
        foot.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams flp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(act, 20);
        page.addView(foot, flp);

        return page;
    }

    /* ---------------------------- Miuix 胶囊开关 ---------------------------- */

    /** 44×26dp 胶囊轨道 + 22dp 白色滑块，180ms 位移/变色动画 */
    private static final class MiuixSwitch extends android.widget.FrameLayout {
        private final android.view.View thumb;
        private final android.graphics.drawable.GradientDrawable track;
        private final int travel;
        private boolean on;
        private int trackColor;
        private android.animation.ValueAnimator colorAnim;
        private android.widget.CompoundButton.OnCheckedChangeListener listener;

        MiuixSwitch(android.content.Context c, boolean initOn) {
            super(c);
            int w = dp(c, 44);
            int h = dp(c, 26);
            int t = dp(c, 22);
            int pad = dp(c, 2);
            travel = w - t - pad * 2;

            track = new android.graphics.drawable.GradientDrawable();
            track.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            track.setCornerRadius(h / 2f);
            trackColor = initOn ? MIU_BLUE : MIU_TRACK_OFF;
            track.setColor(trackColor);
            setBackground(track);

            thumb = new android.view.View(c);
            android.graphics.drawable.GradientDrawable tb = new android.graphics.drawable.GradientDrawable();
            tb.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            tb.setColor(0xFFFFFFFF);
            thumb.setBackground(tb);
            android.widget.FrameLayout.LayoutParams tlp = new android.widget.FrameLayout.LayoutParams(t, t);
            tlp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
            tlp.leftMargin = pad;
            addView(thumb, tlp);

            on = initOn;
            thumb.setTranslationX(initOn ? travel : 0f);
            setClickable(true);
            setFocusable(true);
            setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    apply(!on, true);
                    if (listener != null) {
                        listener.onCheckedChanged(null, on);
                    }
                }
            });
        }

        boolean isOn() {
            return on;
        }

        void setOnChangeListener(android.widget.CompoundButton.OnCheckedChangeListener l) {
            listener = l;
        }

        void apply(boolean v, boolean animate) {
            if (on == v && animate) {
                return;
            }
            on = v;
            float to = v ? travel : 0f;
            int target = v ? MIU_BLUE : MIU_TRACK_OFF;
            thumb.animate().cancel();
            if (animate) {
                thumb.animate().translationX(to).setDuration(180)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                if (colorAnim != null) {
                    colorAnim.cancel();
                }
                colorAnim = android.animation.ValueAnimator.ofArgb(trackColor, target);
                colorAnim.setDuration(180);
                colorAnim.setInterpolator(new android.view.animation.DecelerateInterpolator());
                colorAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(android.animation.ValueAnimator a) {
                        trackColor = ((Integer) a.getAnimatedValue()).intValue();
                        track.setColor(trackColor);
                    }
                });
                colorAnim.start();
            } else {
                thumb.setTranslationX(to);
                trackColor = target;
                track.setColor(trackColor);
            }
        }
    }

    /** 生成一行 开关：it = {key, 标题, 默认, 副标题, 颜色, 图标字符} */
    private static android.view.View makeSwitchRow(final android.app.Activity act, final String[] it) {
        final String key = it[0];
        boolean def = !"0".equals(it[2]);

        android.widget.LinearLayout row = miuRowBase(act);
        row.addView(miuIcon(act, it.length > 5 ? it[5] : "•",
                miuColor(it.length > 4 ? it[4] : "#3482FF", MIU_BLUE)), miuIconLp(act));

        android.widget.LinearLayout texts = new android.widget.LinearLayout(act);
        texts.setOrientation(android.widget.LinearLayout.VERTICAL);
        texts.addView(miuText(act, it[1], 16f, MIU_TEXT, false));
        if (it.length > 3 && it[3] != null && it[3].length() > 0) {
            android.widget.TextView sub = miuText(act, it[3], 13f, MIU_SUB, false);
            sub.setMaxLines(1);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(act, 2);
            texts.addView(sub, slp);
        }
        row.addView(texts, new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final MiuixSwitch sw = new MiuixSwitch(act, cfg(key, def));
        sw.setOnChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                setCfg(key, v);
                h26("L105" + key + "L106" + v);
                miuRefreshCount();
                if ("ui_transparent_bar".equals(key)) {
                    // 直接重建界面：Compose 重新计算留白，无需询问
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            new Runnable() {
                                public void run() {
                                    try {
                                        act.recreate();
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }, 260L);
                }
            }
        });
        android.widget.LinearLayout.LayoutParams swlp = new android.widget.LinearLayout.LayoutParams(
                dp(act, 44), dp(act, 26));
        swlp.leftMargin = dp(act, 12);
        swlp.gravity = android.view.Gravity.CENTER_VERTICAL;
        row.addView(sw, swlp);
        return row;
    }

    /** 重建当前 Activity：Compose 树重建、留白重新计算，相当于轻量重启 */
    private static void restartApp(final android.app.Activity act) {
        try {
            act.recreate();
        } catch (Throwable t) {
            h26("L107" + t);
        }
    }

    /** 展开/收起的过渡动画：高度 + 透明度同时变化 */
    private static void animateBody(final android.view.View body, final boolean expand) {
        try {
            if (expand) {
                body.setVisibility(android.view.View.VISIBLE);
            }
            android.view.ViewGroup.LayoutParams lp = body.getLayoutParams();
            int parentW = body.getParent() instanceof android.view.View
                    ? ((android.view.View) body.getParent()).getWidth() : 0;
            if (parentW <= 0) {
                parentW = body.getResources().getDisplayMetrics().widthPixels - dp(body.getContext(), 88);
            }
            body.measure(android.view.View.MeasureSpec.makeMeasureSpec(parentW,
                            android.view.View.MeasureSpec.AT_MOST),
                    android.view.View.MeasureSpec.makeMeasureSpec(0,
                            android.view.View.MeasureSpec.UNSPECIFIED));
            final int full = body.getMeasuredHeight();
            final int startH = expand ? 0 : body.getHeight();
            final int endH = expand ? full : 0;

            android.animation.ValueAnimator va =
                    android.animation.ValueAnimator.ofInt(startH, endH);
            va.setDuration(210);
            va.setInterpolator(new android.view.animation.DecelerateInterpolator());
            va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    android.view.ViewGroup.LayoutParams p = body.getLayoutParams();
                    if (p == null) {
                        return;
                    }
                    p.height = ((Integer) a.getAnimatedValue()).intValue();
                    body.setLayoutParams(p);
                    body.setAlpha(full <= 0 ? 1f
                            : Math.min(1f, p.height / (float) Math.max(1, full)));
                }
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                public void onAnimationEnd(android.animation.Animator a) {
                    android.view.ViewGroup.LayoutParams p = body.getLayoutParams();
                    if (p != null) {
                        p.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                        body.setLayoutParams(p);
                    }
                    body.setAlpha(1f);
                    if (!expand) {
                        body.setVisibility(android.view.View.GONE);
                    }
                }
            });
            va.start();
        } catch (Throwable t) {
            body.setVisibility(expand ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }


    private static void attachIcon(final android.app.Activity act) {
        try {
            if (act == null || act.isFinishing()) {
                return;
            }
            android.view.View decorV = act.getWindow() == null ? null : act.getWindow().peekDecorView();
            if (!(decorV instanceof android.view.ViewGroup)) {
                return;
            }
            final android.view.ViewGroup root = (android.view.ViewGroup) decorV;
            if (root.findViewWithTag("animrep_ball") != null) {
                return;
            }
            android.graphics.Bitmap bmp = moduleBitmap("assets/ball.png");
            if (bmp == null) {
                return;
            }
            final android.widget.ImageView iv = new android.widget.ImageView(act);
            iv.setTag("animrep_ball");
            iv.setImageBitmap(bmp);
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            iv.setAlpha(0.94f);
            final int size = dp(act, 44);
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(size, size);
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            lp.leftMargin = Math.max(0, act.getResources().getDisplayMetrics().widthPixels - size - dp(act, 6));
            lp.topMargin = dp(act, 120);
            iv.setOnTouchListener(new android.view.View.OnTouchListener() {
                float rx;
                float ry;
                int l0;
                int t0;
                long downAt;
                boolean moved;

                public boolean onTouch(android.view.View v, android.view.MotionEvent e) {
                    android.widget.FrameLayout.LayoutParams p =
                            (android.widget.FrameLayout.LayoutParams) v.getLayoutParams();
                    switch (e.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            rx = e.getRawX();
                            ry = e.getRawY();
                            l0 = p.leftMargin;
                            t0 = p.topMargin;
                            downAt = android.os.SystemClock.uptimeMillis();
                            moved = false;
                            v.setAlpha(0.72f);
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE: {
                            int ddx = (int) (e.getRawX() - rx);
                            int ddy = (int) (e.getRawY() - ry);
                            if (Math.abs(ddx) > dp(v.getContext(), 6) || Math.abs(ddy) > dp(v.getContext(), 6)) {
                                moved = true;
                            }
                            if (moved) {
                                int w = ((android.view.View) root).getWidth() - size;
                                int h = ((android.view.View) root).getHeight() - size;
                                p.leftMargin = Math.max(0, Math.min(w, l0 + ddx));
                                p.topMargin = Math.max(0, Math.min(h, t0 + ddy));
                                v.requestLayout();
                            }
                            return true;
                        }
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.setAlpha(0.94f);
                            if (!moved && android.os.SystemClock.uptimeMillis() - downAt >= 320L) {
                                openPanel(act);
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });
            root.addView(iv, lp);
            iv.setAlpha(0f);
            iv.animate().alpha(0.94f).setDuration(220).start();
        } catch (Throwable t) {
            h26("L108" + t);
        }
    }


    /* --------- 长按头像（左上角头像区）唤出开关面板 --------- */

    private static boolean sDown;
    private static boolean sMoved;
    private static float sDownX;
    private static float sDownY;
    private static final android.os.Handler UI = new android.os.Handler(android.os.Looper.getMainLooper());

    private static final java.util.HashSet<String> sTouched = new java.util.HashSet<>();
    private static boolean F19 = false;

    /** 屏蔽「系统公告」弹窗（默认开启，由面板开关 block_notice 控制） */

    /** UI 功能：任务页去除 / 底部透明 */
    private static volatile int nTransHit;

    private static String callerInMyuko() {
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            for (int i = 2; i < st.length && i < 10; i++) {
                if (st[i].getClassName().contains("myuko")) {
                    return st[i].getClassName().substring(st[i].getClassName().lastIndexOf('.') + 1)
                            + "." + st[i].getMethodName();
                }
            }
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static boolean inBottomBar() {
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : st) {
                String mn = e.getMethodName();
                if (mn != null && mn.contains("BottomNavigationBackground")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void h23() {
        try {
            try {
                SharedPreferences cp = cfgPrefs();
                h26("L109" + (cp == null ? "L110" : cp.getAll().toString())
                        + "L111" + cfg("L112", true));
            } catch (Throwable ignored) {
            }
            Class<?> mk = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.MainScreenKt", CL);

            // ① 任务页去除：只跳过该项渲染
            XposedBridge.hookAllMethods(mk, "NavigationItem", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (!cfg("ui_hide_task", true)) {
                            return;
                        }
                        for (Object a : param.args) {
                            if (a != null && a.getClass().getName().endsWith("screen.main.MainPage")) {
                                String id = String.valueOf(XposedHelpers.callMethod(a, "getId"));
                                if ("task".equalsIgnoreCase(id)) {
                                    param.setResult(null);
                                    return;
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
            log("L113");

            // ② 底部栏留白：诊断 + 归零
            XposedBridge.hookAllMethods(mk, "calculateBottomNavigationOverlayPadding-Md-fbLM", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        // 这里不能归零：悬浮的「继续观看」卡片就是靠这个值贴在栏上方
                    } catch (Throwable ignored) {
                    }
                }
            });

            // ②c 首页内容自己的导航栏留白：HomePageKt.homeContentNavPadding(...)
            try {
                Class<?> hp = XposedHelpers.findClass(
                        "com.yy.myuko.shared.presentation.screen.main.home.HomePageKt", CL);
                Class<?> padKt = XposedHelpers.findClass(
                        "androidx.compose.foundation.layout.PaddingKt", CL);
                XposedBridge.hookAllMethods(hp, "homeContentNavPadding-Md-fbLM", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Object res = param.getResult();
                            h26("L114" + param.args[0] + "L115" + param.args[1]
                                    + "L116" + res + "L117" + cfg("L118", true));
                            if (res != null && cfg("ui_transparent_bar", true)) {
                                float top = ((Number) XposedHelpers.callMethod(res,
                                        "calculateTopPadding-D9Ej5fM")).floatValue();
                                java.lang.reflect.Method ctor = padKt.getDeclaredMethod(
                                        "PaddingValues-a9UjIt4",
                                        float.class, float.class, float.class, float.class);
                                ctor.setAccessible(true);
                                Object pv = ctor.invoke(null, 0f, top, 0f, 0f);
                                param.setResult(pv);
                                if (nTransHit++ < 3) {
                                    log("L119" + top + "L120" + pv);
                                }
                            }
                        } catch (Throwable t) {
                            h26("L121" + t);
                        }
                    }
                });
                log("L122");
            } catch (Throwable t) {
                log("L123" + t);
            }

            // ③ 底部栏底色透明
            try {
                Class<?> cs = XposedHelpers.findClass(
                        "com.yy.myuko.core.ui.foundation.theme.MyukoColorScheme", CL);
                XposedBridge.hookAllMethods(cs, "getBgColorPage-0d7_KjU", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            boolean bar = inBottomBar();
                            h26("L124" + param.getResult() + "L125" + bar
                                    + "L126" + callerInMyuko());
                            if (bar && cfg("ui_transparent_bar", true) && param.getResult() instanceof Long) {
                                param.setResult(Long.valueOf(0L));
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
                Class<?> sk = XposedHelpers.findClass(
                        "com.yy.myuko.business.skins.ui.SkinsComponentsKt", CL);
                XC_MethodHook skHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            boolean bar = inBottomBar();
                            h26("L127" + bar + "L128" + (param.args.length > 1 ? param.args[1] : "L129")
                                    + "L130" + callerInMyuko());
                            if (bar && cfg("ui_transparent_bar", true)) {
                                // 直接把皮肤背景整层摘掉：返回未加背景的 Modifier
                                Object m0 = param.args[0];
                                if (m0 != null && safeSetResult(param, m0)) {
                                    h26("L131");
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                };
                XposedBridge.hookAllMethods(sk, "skinsBackground-OadGlvw$default", skHook);
                XposedBridge.hookAllMethods(sk, "skinsBackground-OadGlvw", skHook);
                log("L132");
            } catch (Throwable t) {
                log("L133" + t);
            }
        } catch (Throwable t) {
            log("L134" + t);
        }
    }

    private void h6() {
        try {
            Class<?> noticeDialogKt = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.home.component.notice.NoticeDialogKt", CL);
            int n = 0;
            for (java.lang.reflect.Method m : noticeDialogKt.getDeclaredMethods()) {
                if (!m.getName().equals("NoticeDialog")) {
                    continue;
                }
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (cfg("block_notice", true)) {
                            param.setResult(null);
                        }
                    }
                });
                n++;
            }
            // 数据层兜底：直接返回空计划，UI 没有公告可画
            Class<?> useCase = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.notice.usecase.GetPopupNoticesUseCase", CL);
            final Class<?> planCls = XposedHelpers.findClass(
                    "com.yy.myuko.shared.domain.notice.model.PopupNoticePlan", CL);
            final Class<?> rightCls = XposedHelpers.findClass("arrow.core.Either$Right", CL);
            XposedBridge.hookAllMethods(useCase, "invoke", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg("block_notice", true)) {
                        return;
                    }
                    try {
                        Object emptyPlan = planCls.getConstructor().newInstance();
                        param.setResult(rightCls.getConstructor(Object.class).newInstance(emptyPlan));
                        nBlocked++;
                    } catch (Throwable ignored) {
                    }
                }
            });
            n++;
            // 真正的「公告/福利」弹窗走 CustomPopup 子系统，单独拦
            Class<?> homeState = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.home.HomeState", CL);
            for (String g : new String[]{"getShowCustomPopup", "getCanPresentCustomPopup"}) {
                XposedBridge.hookAllMethods(homeState, g, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (cfg("block_notice", true)) {
                            param.setResult(Boolean.FALSE);
                        }
                    }
                });
                n++;
            }
            Class<?> hostKt = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.popup.CustomPopupSlotHostKt", CL);
            XposedBridge.hookAllMethods(hostKt, "CustomPopupSlotHost", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (cfg("block_notice", true)) {
                        param.setResult(null);
                    }
                }
            });
            n++;
            log("L135" + n);
        } catch (Throwable t) {
            log("L136" + t);
        }
    }

    private void h16() {
        try {
            h17();
            XposedBridge.hookAllMethods(android.app.Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        android.app.Activity a = (android.app.Activity) param.thisObject;
                        Context c = a.getApplicationContext();
                        if (c != null) {
                            sAppCtx = c;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
            h6();
            h23();
            XposedBridge.hookAllMethods(android.app.Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        final android.app.Activity act = (android.app.Activity) param.thisObject;
                        android.view.MotionEvent e = (android.view.MotionEvent) param.args[0];
                        onTouch(act, e);
                    } catch (Throwable ignored) {
                    }
                }
            });
            XposedBridge.hookAllMethods(android.app.Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        final android.app.Activity act = (android.app.Activity) param.thisObject;
                        if (act == null) {
                            return;
                        }
                        UI.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    showWelcome(act);
                                } catch (Throwable ignored) {
                                }
                            }
                        }, 1200L);
                    } catch (Throwable ignored) {
                    }
                }
            });
            h26("L137");
        } catch (Throwable t) {
            h26("L138" + t);
        }
    }

    private static float sDownRawX;
    private static float sDownRawY;
    private static boolean sDownActive;
    private static boolean sFired;
    private static final Runnable LONG_PRESS = new Runnable() {
        public void run() {
            try {
                if (!sDownActive) {
                    return;
                }
                sFired = true;
                android.app.Activity a = sActRef;
                if (a != null && mePageActive(a)) {
                    openPanel(a);
                }
            } catch (Throwable ignored) {
            }
        }
    };
    private static android.app.Activity sActRef;
    /** 「我的」页是否在前台：只有这个页面长按才唤出面板 */
    private static volatile boolean sOnMePage;
    private static volatile Context sAppCtx;
    private static volatile int nBlocked;
    private static volatile long sMePageAt;

    /** 通过「我的」页 / 首页的 Composable 入口判断当前 Tab */
    private void h17() {
        try {
            Class<?> mePage = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.me.MePageKt", CL);
            int n = 0;
            for (java.lang.reflect.Method m : mePage.getDeclaredMethods()) {
                if (!m.getName().startsWith("MePage") && !m.getName().startsWith("MeContent")) {
                    continue;
                }
                m.setAccessible(true);
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // Composable 会复用组合，不能作为页面判据
                            sMePageAt = android.os.SystemClock.uptimeMillis();
                        }
                    });
                    n++;
                } catch (Throwable ignored) {
                }
            }
            Class<?> homePage = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.home.HomePageKt", CL);
            for (java.lang.reflect.Method m : homePage.getDeclaredMethods()) {
                if (!m.getName().startsWith("HomePage")) {
                    continue;
                }
                m.setAccessible(true);
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // Composable 会复用组合，不能作为页面判据
                        }
                    });
                    n++;
                } catch (Throwable ignored) {
                }
            }
            // 切 Tab 时精准更新状态（Compose 会复用组合，单靠 Composable 入口不可靠）
            Class<?> mainVm = XposedHelpers.findClass(
                    "com.yy.myuko.shared.presentation.screen.main.MainViewModel", CL);
            for (String mn : new String[]{"changePage$shared_release", "onNavItemClick$shared_release"}) {
                XposedBridge.hookAllMethods(mainVm, mn, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Object page = param.args[0];
                            String tag = "";
                            if (page != null) {
                                tag = page.toString();
                                try {
                                    Object id = XposedHelpers.callMethod(page, "getId");
                                    tag = tag + "|" + id;
                                } catch (Throwable ignored) {
                                }
                            }
                            String low = tag.toLowerCase();
                            sOnMePage = low.endsWith("|me") || low.contains("|mine")
                                    || low.contains("profile") || low.contains("|user")
                                    || low.startsWith("me|") || low.equals("me");
                            if (sOnMePage) {
                                sMePageAt = android.os.SystemClock.uptimeMillis();
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
                n++;
            }
            log("L139" + n);
        } catch (Throwable t) {
            log("L140" + t);
        }
    }

    /** 当前是否在「我的」页且是主界面（长按头像只在这里生效） */
    private static boolean mePageActive(android.app.Activity act) {
        try {
            if (!sOnMePage) {
                return false;
            }
            if (android.os.SystemClock.uptimeMillis() - sMePageAt > 600000L) {
                return false;
            }
            String cn = act.getClass().getName();
            return cn.contains("MainActivity");
        } catch (Throwable t) {
            return false;
        }
    }

    /** 通过无障碍节点真实定位「我的」页左上角头像的屏幕矩形 */
    private static android.graphics.Rect findAvatarRect(android.view.View decor) {
        try {
            final java.util.ArrayDeque<android.view.View> q = new java.util.ArrayDeque<>();
            q.add(decor);
            android.view.accessibility.AccessibilityNodeProvider p = null;
            while (!q.isEmpty()) {
                android.view.View v = q.poll();
                android.view.accessibility.AccessibilityNodeProvider pr = v.getAccessibilityNodeProvider();
                if (pr != null) {
                    p = pr;
                    break;
                }
                if (v instanceof android.view.ViewGroup) {
                    android.view.ViewGroup g = (android.view.ViewGroup) v;
                    for (int i = 0; i < g.getChildCount(); i++) {
                        q.add(g.getChildAt(i));
                    }
                }
            }
            if (p == null) {
                return null;
            }
            final int w = decor.getWidth();
            final int h = decor.getHeight();
            final int minSide = dp(decor.getContext(), 30);
            final int maxSide = dp(decor.getContext(), 130);
            android.view.accessibility.AccessibilityNodeInfo root =
                    p.createAccessibilityNodeInfo(android.view.View.NO_ID);
            if (root == null) {
                return null;
            }
            final java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo> nodes =
                    new java.util.ArrayDeque<>();
            nodes.add(root);
            android.graphics.Rect best = null;
            int guard = 0;
            while (!nodes.isEmpty() && guard++ < 400) {
                android.view.accessibility.AccessibilityNodeInfo n = nodes.poll();
                if (n == null) {
                    continue;
                }
                android.graphics.Rect r = new android.graphics.Rect();
                n.getBoundsInScreen(r);
                int rw = r.width();
                int rh = r.height();
                boolean square = rw >= minSide && rw <= maxSide && rh >= minSide && rh <= maxSide
                        && Math.abs(rw - rh) <= rw * 3 / 10;
                if (square && r.left < w * 0.45f && r.top < h * 0.45f) {
                    if (best == null || (r.top * 1000 + r.left) < (best.top * 1000 + best.left)) {
                        best = r;
                    }
                }
                for (int i = 0; i < n.getChildCount(); i++) {
                    android.view.accessibility.AccessibilityNodeInfo c = n.getChild(i);
                    if (c != null) {
                        nodes.add(c);
                    }
                }
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void onTouch(final android.app.Activity act, android.view.MotionEvent e) {
        if (e == null) {
            return;
        }
        sActRef = act;
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN: {
                sDownRawX = e.getRawX();
                sDownRawY = e.getRawY();
                sDownActive = true;
                sFired = false;
                int w = act.getResources().getDisplayMetrics().widthPixels;
                int h = act.getResources().getDisplayMetrics().heightPixels;
                boolean hitAvatar = false;
                boolean avatarKnown = false;
                try {
                    android.view.View decor = act.getWindow() == null ? null : act.getWindow().peekDecorView();
                    if (decor != null) {
                        android.graphics.Rect ar = findAvatarRect(decor);
                        if (ar != null) {
                            avatarKnown = true;
                            int pad = dp(act, 18);
                            hitAvatar = sDownRawX >= ar.left - pad && sDownRawX <= ar.right + pad
                                    && sDownRawY >= ar.top - pad && sDownRawY <= ar.bottom + pad;
                        }
                    }
                } catch (Throwable ignored) {
                }
                boolean inZone;
                if (avatarKnown) {
                    inZone = hitAvatar && mePageActive(act);
                } else {
                    inZone = sDownRawX <= w * 0.45f && sDownRawY <= h * 0.42f && mePageActive(act);
                }
                if (inZone) {
                    UI.removeCallbacks(LONG_PRESS);
                    UI.postDelayed(LONG_PRESS, 450L);
                } else {
                    UI.removeCallbacks(LONG_PRESS);
                }
                break;
            }
            case android.view.MotionEvent.ACTION_MOVE: {
                if (sDownActive && (Math.abs(e.getRawX() - sDownRawX) > dp(act, 22) || Math.abs(e.getRawY() - sDownRawY) > dp(act, 22))) {
                    sDownActive = false;
                    UI.removeCallbacks(LONG_PRESS);
                }
                break;
            }
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL: {
                sDownActive = false;
                UI.removeCallbacks(LONG_PRESS);
                break;
            }
            default:
                break;
        }
    }


    /** 具体 Activity 类如果自己重写了 dispatchTouchEvent，则基类 hook 收不到，必须逐级补挂 */
    private static void h25(Class<?> cls) {
        try {
            Class<?> c = cls;
            while (c != null && c != android.app.Activity.class) {
                String key = c.getName();
                if (!sTouched.contains(key)) {
                    sTouched.add(key);
                    try {
                        java.lang.reflect.Method m = c.getDeclaredMethod("dispatchTouchEvent",
                                android.view.MotionEvent.class);
                        XposedBridge.hookMethod(m, F16);
                        h26("L141" + key);
                    } catch (NoSuchMethodException ignored) {
                    } catch (Throwable t) {
                        h26("L142" + key + "L143" + t);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
    }

    private static final XC_MethodHook F16 = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                Object self = param.thisObject;
                if (!(self instanceof android.app.Activity) || param.args == null || param.args.length == 0) {
                    return;
                }
                if (!(param.args[0] instanceof android.view.MotionEvent)) {
                    return;
                }
                final android.app.Activity act = (android.app.Activity) self;
                final android.view.MotionEvent e = (android.view.MotionEvent) param.args[0];
                int a = e.getActionMasked();
                if (a == android.view.MotionEvent.ACTION_DOWN) {
                    sDownX = e.getX();
                    sDownY = e.getY();
                    sDown = true;
                    sMoved = false;
                    final float x = sDownX;
                    final float y = sDownY;
                    if (!F19) {
                        F19 = true;
                        android.view.View dv = act.getWindow() == null ? null : act.getWindow().peekDecorView();
                        h26("L144" + (int) x + "L145" + (int) y + "L146"
                                + (dv == null ? "L147" : dv.getWidth() + "L148" + dv.getHeight())
                                + "L149" + isAvatarZone(act, x, y));
                    }
                    UI.postDelayed(new Runnable() {
                        public void run() {
                            try {
                                if (!sDown || sMoved) {
                                    return;
                                }
                                boolean zone = isAvatarZone(act, x, y);
                                h26("L150" + (int) x + "L151" + (int) y + "L152" + zone);
                                if (zone) {
                                    sDown = false;
                                    openPanel(act);
                                }
                            } catch (Throwable t) {
                                h26("L153" + t);
                            }
                        }
                    }, 520L);
                } else if (a == android.view.MotionEvent.ACTION_MOVE) {
                    if (Math.abs(e.getX() - sDownX) > dp(act, 40) || Math.abs(e.getY() - sDownY) > dp(act, 40)) {
                        sMoved = true;
                    }
                } else if (a == android.view.MotionEvent.ACTION_UP
                        || a == android.view.MotionEvent.ACTION_CANCEL) {
                    sDown = false;
                }
            } catch (Throwable ignored) {
            }
        }
    };

    /** 头像所在区域：屏幕左上角一块（不依赖具体控件，避免 Compose 探针） */
    private static boolean isAvatarZone(android.app.Activity act, float x, float y) {
        try {
            android.view.View decor = act.getWindow() == null ? null : act.getWindow().peekDecorView();
            if (decor == null) {
                return false;
            }
            int w = decor.getWidth();
            int h = decor.getHeight();
            if (w <= 0 || h <= 0) {
                return false;
            }
            return x <= w * 0.50f && y >= h * 0.02f && y <= h * 0.42f;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void openPanel(final android.app.Activity act) {
        android.view.View decorV = act.getWindow() == null ? null : act.getWindow().peekDecorView();
        if (!(decorV instanceof android.view.ViewGroup)) {
            return;
        }
        togglePanel(act, (android.view.ViewGroup) decorV);
    }

    private static int dp(android.view.View v, int x) {
        return dp(v == null ? null : v.getContext(), x);
    }


    private static Context currentApp() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method m = at.getMethod("currentApplication");
            Object o = m.invoke(null);
            if (o instanceof Context) return (Context) o;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method m = at.getMethod("currentApplication");
            Object o = m.invoke(null);
            return (o instanceof Context) ? (Context) o : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int dp(android.content.Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 欢迎弹窗：每次启动弹一次；勾选「下次不再显示」后永久关闭 */
    private static volatile boolean sWelcomeDone;
    private static void showWelcome(final android.app.Activity act) {
        if (act == null || act.isFinishing()) {
            return;
        }
        // 每个版本首次启动都要弹一次（即使勾过「下次不再显示」）
        boolean newVersion = !VERSION_NAME.equals(cfgPrefs().getString("welcome_ver", ""));
        if (sWelcomeDone || (cfg("welcome_skip", false) && !newVersion)) {
            return;
        }
        if (act.getWindow() == null || act.getWindow().peekDecorView() == null) {
            return;
        }
        sWelcomeDone = true;
        try {
            cfgPrefs().edit().putString("welcome_ver", VERSION_NAME).apply();
            final android.app.Dialog dlg = new android.app.Dialog(act);

            /* Miuix 白卡片：圆角 24dp，左右靠窗口宽度留 24dp */
            MeshCard box = new MeshCard(act);
            box.setRadius(dp(act, 24));

            int pad = dp(act, 20);
            box.setPadding(pad, dp(act, 18), pad, dp(act, 18));

            /* 标题行：klgc hook + 版本号 + 关闭 */
            android.widget.LinearLayout head = new android.widget.LinearLayout(act);
            head.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            head.setGravity(android.view.Gravity.CENTER_VERTICAL);

            android.widget.TextView t1 = miuText(act, "klgc hook", 22f, MIU_TEXT, true);
            head.addView(t1, new android.widget.LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.TextView ver = miuText(act, "v" + VERSION_NAME + " (" + BUILD_VER + ")",
                    10f, MIU_SUB, false);
            head.addView(ver);

            final android.widget.TextView x1 = miuText(act, "✕", 12f, MIU_CLOSE_FG, false);
            x1.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable xbg = new android.graphics.drawable.GradientDrawable();
            xbg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            xbg.setColor(MIU_CLOSE_BG);
            x1.setBackground(xbg);
            x1.setClickable(true);
            android.widget.LinearLayout.LayoutParams xlp = new android.widget.LinearLayout.LayoutParams(
                    dp(act, 28), dp(act, 28));
            xlp.leftMargin = dp(act, 10);
            head.addView(x1, xlp);
            box.addView(head);

            android.widget.TextView t2 = miuText(act, "本破解免费使用", 13f, MIU_SUB, false);
            android.widget.LinearLayout.LayoutParams t2lp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            t2lp.topMargin = dp(act, 6);
            box.addView(t2, t2lp);

            /* 使用指引 */
            android.widget.TextView g1 = miuText(act, "使用指引", 13f, MIU_SUB, false);
            android.widget.LinearLayout.LayoutParams glp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            glp.topMargin = dp(act, 20);
            glp.bottomMargin = dp(act, 10);
            box.addView(g1, glp);

            String[] steps = new String[]{
                    "进入「我的」页面",
                    "长按左上角个人头像（约 0.5 秒）",
                    "弹出开关菜单"
            };
            int[] stepColors = new int[]{0xFF3482FF, 0xFF00A0A0, 0xFFFF8C00};
            for (int i = 0; i < steps.length; i++) {
                android.widget.LinearLayout stepRow = new android.widget.LinearLayout(act);
                stepRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                stepRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                android.widget.TextView num = miuText(act, String.valueOf(i + 1), 14f, 0xFFFFFFFF, true);
                num.setGravity(android.view.Gravity.CENTER);
                android.graphics.drawable.GradientDrawable nbg = new android.graphics.drawable.GradientDrawable();
                nbg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                nbg.setColor(stepColors[i]);
                num.setBackground(nbg);
                android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(
                        dp(act, 22), dp(act, 22));
                nlp.rightMargin = dp(act, 10);
                stepRow.addView(num, nlp);

                stepRow.addView(miuText(act, steps[i], 14f, 0xFF333333, false),
                        new android.widget.LinearLayout.LayoutParams(
                                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                android.widget.LinearLayout.LayoutParams srp = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 40));
                if (i > 0) {
                    srp.topMargin = dp(act, 2);
                }
                box.addView(stepRow, srp);
            }

            /* 更新内容 */
            android.widget.TextView g2 = miuText(act, "更新内容", 13f, MIU_SUB, false);
            android.widget.LinearLayout.LayoutParams g2lp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            g2lp.topMargin = dp(act, 20);
            g2lp.bottomMargin = dp(act, 10);
            box.addView(g2, g2lp);

            android.widget.TextView log = miuText(act, F17, 13f, 0xFF5F6368, false);
            log.setLineSpacing(dp(act, 4), 1f);
            android.graphics.drawable.GradientDrawable logBg = new android.graphics.drawable.GradientDrawable();
            logBg.setCornerRadius(dp(act, 14));
            logBg.setColor(MIU_BG);
            log.setBackground(logBg);
            log.setPadding(dp(act, 14), dp(act, 14), dp(act, 14), dp(act, 14));
            box.addView(log, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            /* 主按钮：官方网盘 */
            android.widget.TextView net = miuText(act, "项目主页", 15f, 0xFFFFFFFF, false);
            net.setGravity(android.view.Gravity.CENTER);
            net.setClickable(true);
            android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
            btnBg.setCornerRadius(dp(act, 22));
            btnBg.setColor(MIU_BLUE);
            net.setBackground(btnBg);
            android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 44));
            nlp.topMargin = dp(act, 18);
            box.addView(net, nlp);
            net.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    try {
                        android.content.Intent it = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/jsnb6/klgc-hook"));
                        it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(it);
                    } catch (Throwable t) {
                        h26("L154" + t);
                    }
                }
            });

            /* 底部：下次不再显示（双向写 welcome_skip） */
            final android.widget.CheckBox cb = new android.widget.CheckBox(act);
            cb.setChecked(cfg("welcome_skip", false));
            cb.setText("下次不再显示");
            cb.setTextColor(0xFF7A7F87);
            cb.setTextSize(13);
            try {
                int[][] states = new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                };
                int[] colors = new int[]{MIU_BLUE, 0xFFD5D8DD};
                cb.setButtonTintList(new android.content.res.ColorStateList(states, colors));
            } catch (Throwable ignored) {
            }
            android.widget.LinearLayout.LayoutParams cblp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            cblp.topMargin = dp(act, 10);
            cblp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                    setCfg("welcome_skip", checked);
                }
            });
            box.addView(cb, cblp);

            final Runnable persist = new Runnable() {
                public void run() {
                    try {
                        boolean skip = cb.isChecked();
                        setCfg("welcome_skip", skip);
                        h26("L155" + skip);
                    } catch (Throwable ignored) {
                    }
                }
            };
            android.view.View.OnClickListener closer = new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    persist.run();
                    try {
                        dlg.dismiss();
                    } catch (Throwable ignored) {
                    }
                }
            };
            x1.setOnClickListener(closer);
            dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                public void onDismiss(android.content.DialogInterface d) {
                    persist.run();
                }
            });

            android.widget.ScrollView sc = new android.widget.ScrollView(act);
            sc.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
            sc.setVerticalScrollBarEnabled(false);
            sc.addView(box, new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            dlg.setContentView(sc);
            dlg.setCancelable(true);
            if (dlg.getWindow() != null) {
                dlg.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                dlg.getWindow().setDimAmount(0.55f);
                android.view.WindowManager.LayoutParams wlp = dlg.getWindow().getAttributes();
                int width = act.getResources().getDisplayMetrics().widthPixels - dp(act, 48);
                wlp.width = Math.max(dp(act, 280), width);
                wlp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                dlg.getWindow().setAttributes(wlp);
            }
            dlg.show();
            h26("L156");
        } catch (Throwable t) {
            h26("L157" + t);
        }
    }

    private static void closePanel(final android.view.ViewGroup root, final android.view.View panel,
                                   boolean animate) {
        try {
            if (animate) {
                panel.animate().cancel();
                panel.animate().alpha(0f).setDuration(140).withEndAction(new Runnable() {
                    public void run() {
                        try {
                            root.removeView(panel);
                        } catch (Throwable ignored) {
                        }
                    }
                }).start();
            } else {
                root.removeView(panel);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 液态玻璃药丸：画背后内容的模糊影像 + 白色叠加 + 顶部高光 */
    /** 横向分页容器：支持手指左右拖动翻页 */
    static class DockBar extends android.widget.LinearLayout {
        android.view.View glass;
        android.view.View row;
        int cellW = 1;
        int padL = 0;
        Runnable onPick;
        int picked;
        private float d0;
        private int startLeft;
        private float rowTx0;
        private boolean moving;

        DockBar(android.content.Context c) {
            super(c);
        }

        @Override
        public boolean dispatchTouchEvent(android.view.MotionEvent e) {
            if (glass == null) {
                return super.dispatchTouchEvent(e);
            }
            android.view.ViewGroup.LayoutParams raw = glass.getLayoutParams();
            if (!(raw instanceof android.widget.FrameLayout.LayoutParams)) {
                return super.dispatchTouchEvent(e);
            }
            android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) raw;
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    if (e.getX() < lp.leftMargin
                            || e.getX() > lp.leftMargin + glass.getWidth()) {
                        return super.dispatchTouchEvent(e);
                    }
                    d0 = e.getX();
                    startLeft = lp.leftMargin;
                    rowTx0 = row == null ? 0f : row.getTranslationX();
                    moving = false;
                    try {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } catch (Throwable ignored) {
                    }
                    return true;
                case android.view.MotionEvent.ACTION_MOVE: {
                    float dx = e.getX() - d0;
                    if (!moving && Math.abs(dx) > 3f) {
                        moving = true;
                    }
                    if (!moving) {
                        return true;
                    }
                    int nl = (int) (startLeft + dx);
                    nl = Math.max(padL, Math.min(cellW * 2 + padL, nl));
                    lp.leftMargin = nl;
                    glass.setLayoutParams(lp);
                    try {
                        if (row != null) {
                            row.setTranslationX(rowTx0 - (nl - startLeft) * 3f);
                        }
                    } catch (Throwable ignored) {
                    }
                    return true;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: {
                    if (!moving) {
                        return false;
                    }
                    int idx = Math.round((lp.leftMargin - padL) / (float) Math.max(1, cellW));
                    idx = Math.max(0, Math.min(2, idx));
                    picked = idx;
                    if (onPick != null) {
                        onPick.run();
                    }
                    return true;
                }
                default:
                    return super.dispatchTouchEvent(e);
            }
        }
    }

    static class PagerBox extends android.widget.FrameLayout {
        android.view.View row;
        int pageW = 1;
        int count = 3;
        int tab = 0;
        Runnable onChange;
        private float downX, downY, baseTx;
        private int slop;

        PagerBox(android.content.Context c) {
            super(c);
            try {
                slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
            } catch (Throwable t) {
                slop = 24;
            }
            setClipChildren(true);
        }

        private boolean claimed;

        void dragBy(float dx, boolean fromDock) {
            if (row == null) {
                return;
            }
            float tx = baseTx + dx;
            float min = -(float) (pageW * (count - 1));
            if (tx > 0f) {
                tx = tx * 0.35f;
            }
            if (tx < min) {
                tx = min + (tx - min) * 0.35f;
            }
            row.setTranslationX(tx);
        }

        void beginDrag() {
            baseTx = row == null ? 0f : row.getTranslationX();
        }

        void settleBy(float dx) {
            int idx = tab;
            if (dx < -pageW * 0.18f) {
                idx = Math.min(count - 1, tab + 1);
            } else if (dx > pageW * 0.18f) {
                idx = Math.max(0, tab - 1);
            }
            tab = idx;
            if (onChange != null) {
                onChange.run();
            }
        }

        @Override
        public boolean dispatchTouchEvent(android.view.MotionEvent e) {
            if (row == null) {
                return super.dispatchTouchEvent(e);
            }
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    baseTx = row.getTranslationX();
                    claimed = false;
                    break;
                case android.view.MotionEvent.ACTION_MOVE: {
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    if (!claimed && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                        claimed = true;
                    }
                    if (claimed) {
                        dragBy(dx, false);
                        return true;
                    }
                    break;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: {
                    if (claimed) {
                        settleBy(e.getX() - downX);
                        claimed = false;
                        return true;
                    }
                    break;
                }
                default:
                    break;
            }
            return super.dispatchTouchEvent(e);
        }

        @Override
        public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
            return claimed;
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            return claimed || row != null;
        }

        void snapTo(final int idx) {
            try {
                int target = -idx * pageW;
                final android.animation.ValueAnimator va =
                        android.animation.ValueAnimator.ofInt((int) row.getTranslationX(), target);
                va.setDuration(300L);
                va.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
                va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(android.animation.ValueAnimator a) {
                        try {
                            row.setTranslationX(((Integer) a.getAnimatedValue()).intValue());
                        } catch (Throwable ignored) {
                        }
                    }
                });
                va.addListener(new android.animation.AnimatorListenerAdapter() {
                    public void onAnimationEnd(android.animation.Animator a) {
                        tab = idx;
                        if (onChange != null) {
                            onChange.run();
                        }
                    }
                });
                va.start();
            } catch (Throwable ignored) {
            }
        }
    }

    /** 底栏衬底：几团柔和的彩色光斑 */
    /** 多套背景配色，每次打开随机取一套 */
    static final int[][] PALETTES = new int[][]{
            {0x66C9AEFF, 0x5CFFB6D5, 0x5AE0B8FF, 0x55FFD0C2},
            {0x66FFB27A, 0x5CFF8FA3, 0x5AFFC7E0, 0x55FFD98E},
            {0x66B08CFF, 0x5C8E7BFF, 0x5AC7A6FF, 0x55FFA8D8},
            {0x66D6B8FF, 0x5CBFA8FF, 0x5AE0C6FF, 0x55F0DCFF}};

    /** 宿主侧实时应答：状态页发 PING 广播，只有 hook 真的在跑才会回话。 */
    private static void installPing() {
        try {
            final android.content.Context ctx = appContext();
            if (ctx == null) {
                return;
            }
            android.content.BroadcastReceiver br = new android.content.BroadcastReceiver() {
                public void onReceive(android.content.Context c, android.content.Intent i) {
                    try {
                        if (isOrderedBroadcast()) {
                            setResultData(statusString());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            };
            android.content.IntentFilter f =
                    new android.content.IntentFilter("com.klgc.animerepublic.PING");
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(br, f, android.content.Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(br, f);
            }
        } catch (Throwable ignored) {
        }
    }

    static String statusString() {
        try {
            Object m = de.robv.android.xposed.XposedBridge.MODULE;
            if (m == null) {
                return "";
            }
            Object n = inv(m, "getFrameworkName");
            Object v = inv(m, "getFrameworkVersion");
            Object c = inv(m, "getFrameworkVersionCode");
            Object a = inv(m, "getApiVersion");
            Object p = inv(m, "getFrameworkProperties");
            return "OK\u0001" + n + "\u0001" + v + "\u0001"
                    + (c instanceof Number ? ((Number) c).longValue() : 0L) + "\u0001"
                    + (a instanceof Number ? ((Number) a).intValue() : 0) + "\u0001"
                    + (p instanceof Number ? ((Number) p).longValue() : 0L);
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * 模块自身进程里的自检：把框架信息通过 hook 的返回值交给状态页。
     * 这是判断“此刻模块是否真的被 LSPosed 加载”的唯一可靠方式——
     * 静态字段跨 ClassLoader 不共享，而方法返回值共享。
     */
    private static void selfProbe(XC_LoadPackage.LoadPackageParam lp) {
        try {
            Class<?> act = XposedHelpers.findClass(
                    "com.dsh.animerepublic.SettingsActivity", lp.classLoader);
            final java.lang.reflect.Method m = act.getDeclaredMethod("probeStatus");
            Object mod = null;
            try {
                mod = de.robv.android.xposed.XposedBridge.MODULE;
            } catch (Throwable ignored) {
            }
            final Object fm = mod;
            String info = "";
            if (fm != null) {
                Object n = inv(fm, "getFrameworkName");
                Object v = inv(fm, "getFrameworkVersion");
                Object c = inv(fm, "getFrameworkVersionCode");
                Object a = inv(fm, "getApiVersion");
                Object p = inv(fm, "getFrameworkProperties");
                info = "OK\u0001" + n + "\u0001" + v + "\u0001"
                        + (c instanceof Number ? ((Number) c).longValue() : 0L) + "\u0001"
                        + (a instanceof Number ? ((Number) a).intValue() : 0) + "\u0001"
                        + (p instanceof Number ? ((Number) p).longValue() : 0L);
            }
            final String payload = info;
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        p.setResult(payload);
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** 把宿主进程里的框架信息回写给模块状态页。 */
    static void reportStatus() {
        try {
            android.content.Context ctx = appContext();
            if (ctx == null) {
                return;
            }
            android.os.Bundle b = new android.os.Bundle();
            Object m = null;
            try {
                m = de.robv.android.xposed.XposedBridge.MODULE;
            } catch (Throwable ignored) {
            }
            if (m != null) {
                b.putString("fw", String.valueOf(inv(m, "getFrameworkName")));
                b.putString("fv", String.valueOf(inv(m, "getFrameworkVersion")));
                Object vc = inv(m, "getFrameworkVersionCode");
                b.putLong("fvc", vc instanceof Number ? ((Number) vc).longValue() : 0L);
                Object api = inv(m, "getApiVersion");
                b.putInt("api", api instanceof Number ? ((Number) api).intValue() : 0);
                Object pr = inv(m, "getFrameworkProperties");
                b.putLong("props", pr instanceof Number ? ((Number) pr).longValue() : 0L);
            }
            b.putString("host", ctx.getPackageName());
            ctx.getContentResolver().call(
                    android.net.Uri.parse("content://com.klgc.animerepublic.status"),
                    "report", null, b);
        } catch (Throwable ignored) {
        }
    }

    private static Object inv(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    static String verName() {
        return VERSION_NAME;
    }

    static int verNo() {
        return BUILD_VER;
    }

    static int[] pickPalette() {
        int[] p = sPal;
        if (p != null) {
            return p;
        }
        try {
            p = PALETTES[new java.util.Random(System.nanoTime()).nextInt(PALETTES.length)];
        } catch (Throwable t) {
            p = PALETTES[0];
        }
        sPal = p;
        return p;
    }

    /** 同一进程内只挑一次配色，避免每次重建界面时背景颜色跳变。 */
    static volatile int[] sPal = null;

    static int withAlpha(int c, int a) {
        return (a << 24) | (c & 0x00FFFFFF);
    }

    static int mixColor(int a, int b, float f) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg2 = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * f) << 24) | ((int) (ar + (br - ar) * f) << 16)
                | ((int) (ag + (bg2 - ag) * f) << 8) | (int) (ab + (bb - ab) * f);
    }

    /** 自带流动渐变的圆角卡片（不透明底，children 画在上面） */
    static class MeshCard extends android.widget.LinearLayout {
        private int radius = 0;
        private float phase;
        private final int[] pal = pickPalette();
        private final float[] gx = rnd4();
        private final float[] gy = rnd4();
        private final float[] gr = rnd4();

        private static float[] rnd4() {
            float[] a = new float[4];
            for (int i = 0; i < 4; i++) {
                // 每个色斑锁定在自己那一个象限里抖动，避免全部挤到一处留出大片白
                float base = (i == 0 || i == 2) ? 0.28f : 0.72f;
                a[i] = base + ((float) Math.random() - 0.5f) * 0.26f;
                if (a[i] < 0.06f) {
                    a[i] = 0.06f;
                }
                if (a[i] > 0.94f) {
                    a[i] = 0.94f;
                }
            }
            return a;
        }

        private float rad(int i, int w) {
            return w * (0.55f + 0.32f * gr[i]);
        }

        private android.animation.ValueAnimator anim;

        MeshCard(android.content.Context c) {
            super(c);
            setOrientation(android.widget.LinearLayout.VERTICAL);
            try {
                anim = android.animation.ValueAnimator.ofFloat(0f, 1f);
                anim.setDuration(26000L);
                anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                anim.setInterpolator(new android.view.animation.LinearInterpolator());
                anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(android.animation.ValueAnimator a) {
                        phase = ((Float) a.getAnimatedValue()).floatValue();
                        invalidate();
                    }
                });
                anim.start();
            } catch (Throwable ignored) {
            }
        }

        void setRadius(int r) {
            radius = r;
        }

        @Override
        protected void dispatchDraw(android.graphics.Canvas c) {
            try {
                drawBg(c);
            } catch (Throwable ignored) {
            }
            super.dispatchDraw(c);
        }

        private void drawBg(android.graphics.Canvas c) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                int sc = c.save();
                android.graphics.Path clip = new android.graphics.Path();
                clip.addRoundRect(new android.graphics.RectF(0f, 0f, w, h), radius, radius,
                        android.graphics.Path.Direction.CW);
                c.clipPath(clip);
                c.drawColor(0xFFFFFFFF);
                android.graphics.Paint base = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                base.setShader(new android.graphics.LinearGradient(0f, 0f, w, h,
                        new int[]{withAlpha(pal[0], 0x22), withAlpha(pal[2], 0x1C), withAlpha(pal[1], 0x22)}, null,
                        android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0f, 0f, w, h, base);
                double t = phase * Math.PI * 2.0;
                blot(c, w * (gx[0] + 0.08f * (float) Math.sin(t)), h * (gy[0] + 0.06f * (float) Math.cos(t * 1.3)),
                        rad(0, w), fade(mixColor(pal[0], pal[1], (float) (0.5 + 0.5 * Math.sin(t))), 0.95f));
                blot(c, w * (gx[1] + 0.06f * (float) Math.cos(t * 0.9)), h * (gy[1] + 0.08f * (float) Math.sin(t * 1.1)),
                        rad(1, w), fade(mixColor(pal[1], pal[2], (float) (0.5 + 0.5 * Math.cos(t * 1.2))), 0.9f));
                blot(c, w * (gx[2] + 0.07f * (float) Math.cos(t * 1.15)), h * (gy[2] + 0.05f * (float) Math.sin(t * 1.4)),
                        rad(2, w), fade(mixColor(pal[2], pal[3], (float) (0.5 + 0.5 * Math.sin(t * 1.1))), 0.9f));
                blot(c, w * (gx[3] + 0.05f * (float) Math.sin(t * 1.25)), h * (gy[3] + 0.04f * (float) Math.cos(t * 1.5)),
                        rad(3, w), fade(mixColor(pal[3], pal[0], (float) (0.5 + 0.5 * Math.cos(t))), 0.95f));
                android.graphics.Paint v = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                v.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                        new int[]{0x33FFFFFF, 0x14FFFFFF, 0x26FFFFFF}, null,
                        android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0f, 0f, w, h, v);
                c.restoreToCount(sc);
            }
        }

        private int fade(int color, float k) {
            int a = (int) (((color >>> 24) & 0xFF) * k);
            return (a << 24) | (color & 0x00FFFFFF);
        }

        private void blot(android.graphics.Canvas c, float cx, float cy, float r, int color) {
            r = Math.max(r, 1f);
            android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pt.setShader(new android.graphics.RadialGradient(cx, cy, r, color, 0x00FFFFFF,
                    android.graphics.Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r, pt);
        }

        @Override
        protected void onDetachedFromWindow() {
            try {
                if (anim != null) {
                    anim.cancel();
                }
            } catch (Throwable ignored) {
            }
            super.onDetachedFromWindow();
        }
    }

    static class ColorBlobs extends android.view.View {
        private final android.graphics.Paint p = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);
        private float phase;
        private android.animation.ValueAnimator anim;
        private final int[] pal = pickPalette();
        private final float[] gx = rnd4();
        private final float[] gy = rnd4();
        private final float[] gr = rnd4();

        private static float[] rnd4() {
            float[] a = new float[4];
            for (int i = 0; i < 4; i++) {
                // 每个色斑锁定在自己那一个象限里抖动，避免全部挤到一处留出大片白
                float base = (i == 0 || i == 2) ? 0.28f : 0.72f;
                a[i] = base + ((float) Math.random() - 0.5f) * 0.26f;
                if (a[i] < 0.06f) {
                    a[i] = 0.06f;
                }
                if (a[i] > 0.94f) {
                    a[i] = 0.94f;
                }
            }
            return a;
        }

        private float rad(int i, int w) {
            return w * (0.58f + 0.30f * gr[i]);
        }

        ColorBlobs(android.content.Context c) {
            super(c);
            try {
                anim = android.animation.ValueAnimator.ofFloat(0f, 1f);
                anim.setDuration(26000L);
                anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                anim.setInterpolator(new android.view.animation.LinearInterpolator());
                anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(android.animation.ValueAnimator a) {
                        phase = ((Float) a.getAnimatedValue()).floatValue();
                        invalidate();
                    }
                });
                anim.start();
            } catch (Throwable ignored) {
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            try {
                if (anim != null) {
                    anim.cancel();
                }
            } catch (Throwable ignored) {
            }
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(android.graphics.Canvas c) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 4 || h <= 4) {
                return;
            }
            android.graphics.Paint base = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG);
            base.setShader(new android.graphics.LinearGradient(0f, 0f, w, h,
                    new int[]{withAlpha(pal[0], 0x22), withAlpha(pal[2], 0x1C), withAlpha(pal[1], 0x22)}, null,
                    android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0f, 0f, w, h, base);
            double t = phase * Math.PI * 2.0;
            // 四团色：淡紫 / 淡粉 / 淡蓝 / 淡桃，缓慢游走 + 互相融合
            blob(c, w * (gx[0] + 0.10f * (float) Math.sin(t)), h * (gy[0] + 0.06f * (float) Math.cos(t * 1.3)),
                    rad(0, w), mixColor(pal[0], pal[1], (float) (0.5 + 0.5 * Math.sin(t))));
            blob(c, w * (gx[1] + 0.08f * (float) Math.cos(t * 0.9)), h * (gy[1] + 0.10f * (float) Math.sin(t * 1.1)),
                    rad(1, w), mixColor(pal[1], pal[2], (float) (0.5 + 0.5 * Math.cos(t * 1.2))));
            blob(c, w * (gx[2] + 0.09f * (float) Math.cos(t * 1.15)), h * (gy[2] + 0.06f * (float) Math.sin(t * 1.4)),
                    rad(2, w), mixColor(pal[2], pal[3], (float) (0.5 + 0.5 * Math.sin(t * 1.1))));
            blob(c, w * (gx[3] + 0.06f * (float) Math.sin(t * 1.25)), h * (gy[3] + 0.05f * (float) Math.cos(t * 1.5)),
                    rad(3, w), mixColor(pal[3], pal[0], (float) (0.5 + 0.5 * Math.cos(t))));
            // 白纱：压住饱和度，保证卡片文字清晰
            android.graphics.Paint v = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG);
            v.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                    new int[]{0x59FFFFFF, 0x2EFFFFFF, 0x4DFFFFFF}, null,
                    android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0f, 0f, w, h, v);
        }

        private int lerp(int a, int b, float f) {
            int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg2 = (b >> 8) & 0xFF, bb = b & 0xFF;
            int na = (int) (aa + (ba - aa) * f);
            int nr = (int) (ar + (br - ar) * f);
            int ng = (int) (ag + (bg2 - ag) * f);
            int nb = (int) (ab + (bb - ab) * f);
            return (na << 24) | (nr << 16) | (ng << 8) | nb;
        }

        private void blob(android.graphics.Canvas c, float cx, float cy, float r, int color) {
            r = Math.max(r, 1f);
            android.graphics.RadialGradient rg = new android.graphics.RadialGradient(
                    cx, cy, r, color, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP);
            android.graphics.Paint pt = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG);
            pt.setShader(rg);
            c.drawCircle(cx, cy, r, pt);
        }
    }


    static class GlassPill extends android.view.View {
        private android.graphics.Bitmap bm;
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF rf = new android.graphics.RectF();
        private final android.graphics.Path path = new android.graphics.Path();
        private final int radius;

        GlassPill(android.content.Context c, int radius) {
            super(c);
            this.radius = radius;
            paint.setFilterBitmap(true);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            setElevation(dp(c, 3));
            setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View v, android.graphics.Outline o) {
                    try {
                        o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                    } catch (Throwable ignored) {
                    }
                }
            });
        }

        void setShot(android.graphics.Bitmap b) {
            try {
                if (bm != null && bm != b) {
                    bm.recycle();
                }
            } catch (Throwable ignored) {
            }
            bm = b;
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas c) {
            try {
                float w = getWidth();
                float h = getHeight();
                if (w <= 2f || h <= 2f) {
                    return;
                }
                rf.set(0f, 0f, w, h);
                if (bm != null && !bm.isRecycled() && LiquidRt.available()
                        && LiquidRt.draw(c, bm, w, h, radius,
                        getResources().getDisplayMetrics().density)) {
                    return;
                }
                // 回退：直接铺模糊快照 + 白色薄纱 + 内描边，至少还是玻璃而不是白块
                if (bm != null && !bm.isRecycled()) {
                    paint.setShader(null);
                    paint.setAlpha(150);
                    c.drawBitmap(bm, null, rf, paint);
                    paint.setAlpha(255);
                    paint.setColor(0x33FFFFFF);
                    c.drawRoundRect(rf, radius, radius, paint);
                }
                path.reset();
                path.addRoundRect(rf, radius, radius, android.graphics.Path.Direction.CW);
                int sc = c.save();
                c.clipPath(path);
                if (bm != null && !bm.isRecycled()) {
                    paint.setAlpha(150);
                    c.drawBitmap(bm, null, rf, paint);
                    paint.setAlpha(255);
                }
                // 玻璃本体：上白下浅灰，白底上也能看出是一块玻璃
                paint.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                        0xF2FFFFFF, 0xE3DDE5EF, android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(rf, paint);
                // 顶部高光
                paint.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h * 0.55f,
                        0x55FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(rf, paint);
                paint.setShader(null);
                c.restoreToCount(sc);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                paint.setColor(0x33000000);
                c.drawRoundRect(rf, radius, radius, paint);
                paint.setColor(0x55FFFFFF);
                c.drawRoundRect(0.5f, 0.5f, w - 0.5f, h - 0.5f, radius, radius, paint);
                paint.setStyle(android.graphics.Paint.Style.FILL);
            } catch (Throwable ignored) {
            }
        }
    }

    /** 把 page 上 (left,top,w,h) 这块画面截下来并模糊 */
    private static android.graphics.Bitmap glassShot(android.view.View page,
                                                     int left, int top, int w, int h) {
        if (w < 8 || h < 8 || page == null) {
            return null;
        }
        android.graphics.Bitmap bm = null;
        try {
            bm = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(bm);
            c.translate(-left, -top);
            page.draw(c);
            int[] px = new int[w * h];
            bm.getPixels(px, 0, w, 0, 0, w, h);
            boxBlur(px, w, h, 10);
            bm.setPixels(px, 0, w, 0, 0, w, h);
        } catch (Throwable t) {
            try {
                if (bm != null) {
                    bm.recycle();
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
        return bm;
    }

    /** 两次水平 + 两次垂直的方框模糊 */
    private static void boxBlur(int[] px, int w, int h, int r) {
        try {
            if (w < 3 || h < 3) {
                return;
            }
            if (r > w / 2) {
                r = w / 2;
            }
            if (r > h / 2) {
                r = h / 2;
            }
            if (r < 1) {
                return;
            }
            int[] tmp = new int[w * h];
            for (int pass = 0; pass < 2; pass++) {
                for (int y = 0; y < h; y++) {
                    int row = y * w;
                    int sa = 0;
                    int sr = 0;
                    int sg = 0;
                    int sb = 0;
                    for (int x = -r; x <= r; x++) {
                        int xx = x < 0 ? 0 : (x >= w ? w - 1 : x);
                        int c0 = px[row + xx];
                        sa += (c0 >>> 24) & 0xFF;
                        sr += (c0 >> 16) & 0xFF;
                        sg += (c0 >> 8) & 0xFF;
                        sb += c0 & 0xFF;
                    }
                    int div = 2 * r + 1;
                    for (int x = 0; x < w; x++) {
                        tmp[row + x] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                        int xo = x - r;
                        int xi = x + r + 1;
                        int co = px[row + (xo < 0 ? 0 : (xo >= w ? w - 1 : xo))];
                        int ci = px[row + (xi < 0 ? 0 : (xi >= w ? w - 1 : xi))];
                        sa += ((ci >>> 24) & 0xFF) - ((co >>> 24) & 0xFF);
                        sr += ((ci >> 16) & 0xFF) - ((co >> 16) & 0xFF);
                        sg += ((ci >> 8) & 0xFF) - ((co >> 8) & 0xFF);
                        sb += (ci & 0xFF) - (co & 0xFF);
                    }
                }
                for (int x = 0; x < w; x++) {
                    int sa = 0;
                    int sr = 0;
                    int sg = 0;
                    int sb = 0;
                    for (int y = -r; y <= r; y++) {
                        int yy = y < 0 ? 0 : (y >= h ? h - 1 : y);
                        int c0 = tmp[yy * w + x];
                        sa += (c0 >>> 24) & 0xFF;
                        sr += (c0 >> 16) & 0xFF;
                        sg += (c0 >> 8) & 0xFF;
                        sb += c0 & 0xFF;
                    }
                    int div = 2 * r + 1;
                    for (int y = 0; y < h; y++) {
                        px[y * w + x] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                        int yo = y - r;
                        int yi = y + r + 1;
                        int co = tmp[(yo < 0 ? 0 : (yo >= h ? h - 1 : yo)) * w + x];
                        int ci = tmp[(yi < 0 ? 0 : (yi >= h ? h - 1 : yi)) * w + x];
                        sa += ((ci >>> 24) & 0xFF) - ((co >>> 24) & 0xFF);
                        sr += ((ci >> 16) & 0xFF) - ((co >> 16) & 0xFF);
                        sg += ((ci >> 8) & 0xFF) - ((co >> 8) & 0xFF);
                        sb += (ci & 0xFF) - (co & 0xFF);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 药丸在 page 坐标系里的左边距 */
    private static int glassLeftIn(android.view.View page, android.view.View nav, int idx, int navW) {
        int[] a = new int[2];
        int[] b = new int[2];
        nav.getLocationInWindow(a);
        page.getLocationInWindow(b);
        int cell = navW / 3;
        return (a[0] - b[0]) + cell * idx + dp(nav.getContext(), 4);
    }

    /** 药丸在 page 坐标系里的上边距 */
    private static int glassTopIn(android.view.View page, android.view.View nav) {
        int[] a = new int[2];
        int[] b = new int[2];
        nav.getLocationInWindow(a);
        page.getLocationInWindow(b);
        return (a[1] - b[1]) + dp(nav.getContext(), 6);
    }

    /** 摆放药丸，并重拍一次背景 */
    /** 液态玻璃药丸：直接用 AndroidLiquidGlassView（QmDeve, MIT）的实现 */
    private static android.view.View makeLiquidGlass(android.app.Activity act,
                                                     android.view.ViewGroup source) {
        try {
            com.qmdeve.liquidglass.widget.LiquidGlassView lv =
                    new com.qmdeve.liquidglass.widget.LiquidGlassView(act);
            lv.setCornerRadius(dp(act, 22));
            lv.setRefractionHeight(dp(act, 20));
            lv.setRefractionOffset(dp(act, 22));
            lv.setDispersion(0.08f);
            lv.setBlurRadius(dp(act, 12));
            lv.setTintAlpha(0.10f);
            lv.setTouchEffectEnabled(false);
            lv.setDraggableEnabled(false);
            lv.setElasticEnabled(false);
            lv.bind(source);
            return lv;
        } catch (Throwable t) {
            return new GlassPill(act, dp(act, 22));
        }
    }

    private static void layoutGlass(final android.app.Activity act, final android.view.View page,
                                    final android.view.View nav, final android.view.View glass,
                                    int idx, int navW) {
        try {
            int cell = navW / 3;
            android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) glass.getLayoutParams();
            lp.width = cell - dp(act, 8);
            lp.height = dp(act, 44);
            lp.topMargin = dp(act, 6);
            lp.leftMargin = cell * idx + dp(act, 4);
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            glass.setLayoutParams(lp);
        } catch (Throwable ignored) {
        }
        try {
            glass.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener() {
                public void onLayoutChange(android.view.View v, int l, int t, int r, int b,
                                           int ol, int ot, int or, int ob) {
                    if (r - l != or - ol || b - t != ob - ot) {
                        refreshGlass(page, nav, glass, idx, navW);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
        page.post(new Runnable() {
            public void run() {
                refreshGlass(page, nav, glass, idx, navW);
            }
        });
    }

    /** 截图 + 模糊 + 交给药丸（失败就等于纯白药丸，不影响使用） */
    private static String sGlassInfo = "库内采样";

    private static String renderInfo() {
        return LiquidRt.mode() + " · " + sGlassInfo;
    }

    private static void updateRenderRow() {
    }

    private static void refreshGlass(final android.view.View page, final android.view.View nav,
                                     final android.view.View glass, final int idx, final int navW) {
    }

    private static android.animation.ValueAnimator sGlassAnim;

    private static android.animation.ValueAnimator sRowAnim;

    private static void moveGlass(final android.view.View glass, int from, int to) {
        try {
            if (sGlassAnim != null && sGlassAnim.isRunning()) {
                sGlassAnim.cancel();
            }
            final android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) glass.getLayoutParams();
            android.animation.ValueAnimator va =
                    android.animation.ValueAnimator.ofInt(from, to);
            va.setDuration(280);
            va.setInterpolator(new android.view.animation.OvershootInterpolator(1.15f));
            sGlassAnim = va;
            va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    try {
                        lp.leftMargin = (Integer) a.getAnimatedValue();
                        glass.setLayoutParams(lp);
                    } catch (Throwable ignored) {
                    }
                }
            });
            va.start();
        } catch (Throwable ignored) {
        }
    }

    /** 切换 tab：页面淡出上浮 → 换页 → 淡入下沉，同时药丸平移 */
    private static void miuSelectTab(final android.app.Activity act,
                                     final android.view.View rowV,
                                     final android.widget.ScrollView[] holders,
                                     final android.widget.LinearLayout[] pages,
                                     final android.widget.TextView[] icons,
                                     final android.widget.TextView[] labels,
                                     final android.view.View glass, final int navW,
                                     final android.view.View page, final android.view.View nav,
                                     int idx, int pageW) {
        int cur = sMiuTab;
        sMiuTab = idx;
        try {
            if (rowV.getParent() instanceof PagerBox) {
                ((PagerBox) rowV.getParent()).tab = idx;
            }
        } catch (Throwable ignored) {
        }
        int cell = navW / 3;
        int fromX = cell * (cur < 0 ? idx : cur) + dp(act, 4);
        int toX = cell * idx + dp(act, 4);
        if (cur >= 0 && cur != idx) {
            moveGlass(glass, fromX, toX);
        } else {
            try {
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) glass.getLayoutParams();
                lp.leftMargin = toX;
                glass.setLayoutParams(lp);
            } catch (Throwable ignored) {
            }
        }

        for (int i = 0; i < icons.length; i++) {
            boolean sel = i == idx;
            int color = sel ? MIU_TEXT : MIU_NAV_OFF;
            android.graphics.Typeface tf = sel
                    ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT;
            if (icons[i] != null) {
                icons[i].setTextColor(color);
                icons[i].setTypeface(tf);
                icons[i].animate().scaleX(sel ? 1.06f : 1f).scaleY(sel ? 1.06f : 1f)
                        .setDuration(180).start();
            }
            if (labels[i] != null) {
                labels[i].setTextColor(color);
                labels[i].setTypeface(tf);
            }
        }

        // 真横向滑动：三页并排，整排平移（只改 translationX，不重新布局，帧率稳）
        final android.view.View hs = rowV;
        final int target = -idx * pageW;
        try {
            if (sSlide != null) {
                sSlide.cancel();
                sSlide = null;
            }
        } catch (Throwable ignored) {
        }
        try {
            android.animation.ValueAnimator va =
                    android.animation.ValueAnimator.ofInt((int) hs.getTranslationX(), target);
            va.setDuration(380L);
            va.setInterpolator(new android.view.animation.PathInterpolator(0.22f, 0.72f, 0.16f, 1f));
            va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    try {
                        hs.setTranslationX(((Integer) a.getAnimatedValue()).intValue());
                    } catch (Throwable ignored) {
                    }
                }
            });
            sSlide = va;
            va.start();
        } catch (Throwable ignored) {
            try {
                hs.setTranslationX(target);
            } catch (Throwable ignored2) {
            }
        }
        page.postDelayed(new Runnable() {
            public void run() {
                refreshGlass(page, nav, glass, sMiuTab, navW);
            }
        }, 300L);
    }

    private static void togglePanel(final android.app.Activity act, final android.view.ViewGroup root) {
        android.view.View old = root.findViewWithTag("animrep_panel");
        if (old != null) {
            closePanel(root, old, true);
            h26("L158");
            return;
        }
        h26("L159");

        final android.widget.FrameLayout page = new android.widget.FrameLayout(act);
        page.setTag("animrep_panel");
        page.setBackgroundColor(0xFFFAFBFF);
        try {
            android.widget.FrameLayout bgLayer = new android.widget.FrameLayout(act);
            bgLayer.addView(new ColorBlobs(act), new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            page.addView(bgLayer, new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable ignored) {
        }
        page.setClickable(true);
        page.setFocusableInTouchMode(true);
        // 吞掉空白点击，面板为全屏页，点外面不关闭
        page.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
            }
        });

        // —— 内容区：真横向分页（HorizontalScrollView + 三个等宽纵向 ScrollView）——
        final int pageW = act.getResources().getDisplayMetrics().widthPixels;
        final android.widget.FrameLayout glassSource = new android.widget.FrameLayout(act);
        page.addView(glassSource, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        // 底栏衬底：几团极淡的彩色光斑，玻璃才有东西可折射
        try {
            android.view.View blobs = new ColorBlobs(act);
            android.widget.FrameLayout.LayoutParams blp = new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            glassSource.addView(blobs, blp);
        } catch (Throwable ignored) {
        }
        final PagerBox clip = new PagerBox(act);
        glassSource.addView(clip, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));



        final android.widget.LinearLayout row = new android.widget.LinearLayout(act);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        clip.addView(row, new android.widget.FrameLayout.LayoutParams(
                pageW * 3, android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        final android.widget.LinearLayout[] pages = new android.widget.LinearLayout[]{
                miuHomePage(act), miuFuncPage(act), miuAboutPage(act)};
        final android.widget.ScrollView[] holders = new android.widget.ScrollView[3];
        for (int i = 0; i < 3; i++) {
            final android.widget.ScrollView sv = new android.widget.ScrollView(act);
            sv.setFillViewport(false);
            sv.setVerticalScrollBarEnabled(false);
            sv.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
            sv.setClipToPadding(false);
            int navH = miuInset(act, "navigation_bar_height", 0);
            sv.setPadding(0, 0, 0, dp(act, 88) + navH);
            sv.addView(pages[i], new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            holders[i] = sv;
            row.addView(sv, new android.widget.LinearLayout.LayoutParams(
                    pageW, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        }
        try {
            clip.row = row;
            clip.pageW = pageW;
        } catch (Throwable ignored) {
        }

        // —— 底部悬浮导航：液态玻璃药丸 + 三个等宽 tab ——
        final String[] tabNames = new String[]{"首页", "功能", "关于"};
        final String[] tabIcons = new String[]{"▤", "▦", "ⓘ"};
        final android.widget.TextView[] navIcons = new android.widget.TextView[3];
        final android.widget.TextView[] navLabels = new android.widget.TextView[3];

        final int navW = act.getResources().getDisplayMetrics().widthPixels - dp(act, 48);
        final android.widget.FrameLayout nav = new android.widget.FrameLayout(act);
        android.graphics.drawable.GradientDrawable navBg = new android.graphics.drawable.GradientDrawable();
        navBg.setCornerRadius(dp(act, 28));
        navBg.setColor(0x5CFFFFFF);
        navBg.setStroke(Math.max(1, dp(act, 1)), 0x33FFFFFF);
        nav.setBackground(navBg);
        nav.setElevation(dp(act, 8));
        nav.setClipChildren(false);

        final android.view.View glass = makeLiquidGlass(act, glassSource);
        android.widget.FrameLayout.LayoutParams glp = new android.widget.FrameLayout.LayoutParams(
                navW / 3 - dp(act, 8), dp(act, 44));
        glp.leftMargin = dp(act, 4);
        glp.topMargin = dp(act, 6);
        nav.addView(glass, glp);
        try {
            glass.setElevation(dp(act, 5));
        } catch (Throwable ignored) {
        }

        android.widget.LinearLayout items = new android.widget.LinearLayout(act);
        items.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        nav.addView(items, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            android.widget.LinearLayout tab = new android.widget.LinearLayout(act);
            tab.setOrientation(android.widget.LinearLayout.VERTICAL);
            tab.setGravity(android.view.Gravity.CENTER);

            android.widget.TextView ic = miuText(act, tabIcons[i], 21f, MIU_NAV_OFF, false);
            ic.setGravity(android.view.Gravity.CENTER);
            android.widget.TextView lb = miuText(act, tabNames[i], 12f, MIU_NAV_OFF, false);
            lb.setGravity(android.view.Gravity.CENTER);

            android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.topMargin = dp(act, 3);
            tab.addView(ic, ilp);
            tab.addView(lb, llp);
            navIcons[i] = ic;
            navLabels[i] = lb;

            tab.setClickable(true);
            tab.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    miuSelectTab(act, row, holders, pages, navIcons, navLabels, glass, navW, page, nav, idx, pageW);
                }
            });
            items.addView(tab, new android.widget.LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        // 初始选中态
        for (int i = 0; i < 3; i++) {
            boolean sel = i == 0;
            int color = sel ? MIU_TEXT : MIU_NAV_OFF;
            android.graphics.Typeface tf = sel
                    ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT;
            navIcons[i].setTextColor(color);
            navIcons[i].setTypeface(tf);
            navLabels[i].setTextColor(color);
            navLabels[i].setTypeface(tf);
        }

        android.widget.FrameLayout.LayoutParams navLp = new android.widget.FrameLayout.LayoutParams(
                navW, dp(act, 56));
        navLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        int navInset = miuInset(act, "navigation_bar_height", 0);
        navLp.bottomMargin = navInset > dp(act, 12) ? navInset + dp(act, 8) : dp(act, 24);
        page.addView(nav, navLp);

        try {
            final int cellW = navW / 3;
            final int padL = dp(act, 4);
        } catch (Throwable ignored) {
        }

        try {
            clip.onChange = new Runnable() {
                public void run() {
                    try {
                        miuSelectTab(act, row, holders, pages, navIcons, navLabels,
                                glass, navW, page, nav, clip.tab, pageW);
                    } catch (Throwable ignored) {
                    }
                }
            };
        } catch (Throwable ignored) {
        }

        // —— 右上角关闭按钮：32dp 浅灰圆 + ✕ ——
        android.widget.TextView close = miuText(act, "✕", 14f, MIU_CLOSE_FG, false);
        close.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        closeBg.setColor(0x40FFFFFF);
        closeBg.setStroke(Math.max(1, dp(act, 1)), 0x66FFFFFF);
        close.setBackground(closeBg);
        close.setClickable(true);
        android.widget.FrameLayout.LayoutParams closeLp = new android.widget.FrameLayout.LayoutParams(
                dp(act, 32), dp(act, 32));
        closeLp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        closeLp.topMargin = miuInset(act, "status_bar_height", 24) + dp(act, 18);
        closeLp.rightMargin = dp(act, 16);
        close.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                closePanel(root, page, true);
                h26("L160");
            }
        });
        page.addView(close, closeLp);

        // 返回键关闭
        page.setOnKeyListener(new android.view.View.OnKeyListener() {
            public boolean onKey(android.view.View v, int code, android.view.KeyEvent e) {
                if (code == android.view.KeyEvent.KEYCODE_BACK
                        && e.getAction() == android.view.KeyEvent.ACTION_UP) {
                    closePanel(root, page, true);
                    return true;
                }
                return false;
            }
        });

        page.post(new Runnable() {
            public void run() {
                layoutGlass(act, page, nav, glass, 0, navW);
            }
        });

        root.addView(page, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        // 入场：从底部滑入 + 淡入 220ms
        page.setAlpha(0f);
        page.setTranslationY(act.getResources().getDisplayMetrics().heightPixels);
        page.animate().alpha(1f).translationY(0f).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        page.post(new Runnable() {
            public void run() {
                try {
                    page.requestFocus();
                } catch (Throwable ignored) {
                }
            }
        });
    }


    private static class TextView2 extends android.widget.TextView {
        TextView2(android.content.Context c) {
            super(c);
        }
    }

    /* ---------------------------------------------------------- 诊断 */

    private void h8() {
        Class<?> repo = XposedHelpers.findClass(C_REPO_AD_IMPL, CL);
        XposedBridge.hookAllMethods(repo, sj("claim", "Reward"), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    String msg = "L177" + brief(param.args.length > 0 ? param.args[0] : null)
                            + "L178" + brief(param.args.length > 1 ? param.args[1] : null)
                            + "L179" + brief(param.args.length > 2 ? param.args[2] : null);
                    log(msg);
                } catch (Throwable ignored) {
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    log(h27(param.getResult()));
                } catch (Throwable t) {
                    log("L161" + t);
                }
            }
        });
        XposedBridge.hookAllMethods(repo, "getSignature", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                log("L162" + param.args.length);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object r = param.getResult();
                log("L163" + (r == null ? "L164" : r.getClass().getSimpleName() + "L165" + brief(r)));
            }
        });
        XposedBridge.hookAllMethods(repo, "confirmRewardSession", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object r = param.getResult();
                log("L166" + (r == null ? "L167" : r.getClass().getSimpleName() + "L168" + brief(r)));
            }
        });
        try {
            Class<?> bus = XposedHelpers.findClass("com.yy.myuko.shared.domain.ad.AdRewardBus", CL);
            XposedBridge.hookAllMethods(bus, "emit$shared_release", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        Object e = param.args.length > 0 ? param.args[0] : null;
                        log("L169" + (e == null ? "L170" : e.getClass().getSimpleName()));
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            log("L171" + t);
        }
    }

    private String h27(Object r) {
        if (r == null) {
            return "L172";
        }
        String cls = r.getClass().getSimpleName();
        if ("Right".equals(cls)) {
            Object v;
            try {
                v = XposedHelpers.callMethod(r, "getValue");
            } catch (Throwable t) {
                return "L173";
            }
            if (v == null) {
                return "L174";
            }
            return "L175" + brief(XposedHelpers.callMethod(v, "getStatus"))
                    + "L180" + brief(XposedHelpers.callMethod(v, "getAmount"))
                    + "L181" + brief(XposedHelpers.callMethod(v, "getRewardType"))
                    + "L182" + brief(s2sOf(v));
        }
        return "L176" + cls + "L183" + brief(r);
    }

    private Object s2sOf(Object claimResult) {
        try {
            return XposedHelpers.callMethod(claimResult, "getS2sVerify");
        } catch (Throwable t) {
            return null;
        }
    }

    private static String brief(Object o) {
        if (o == null) {
            return "空";
        }
        String s = String.valueOf(o);
        if (s.length() > 22) {
            s = s.substring(0, 22) + "…";
        }
        return s;
    }

    private Object h28(Object repo) throws Exception {
        Object q3 = XposedHelpers.callMethod(repo, "getCatalog");
        Object q2 = XposedHelpers.callMethod(q3, "getValue");
        if (q2 == null) {
            return null;
        }
        List<?> levels = (List<?>) XposedHelpers.callMethod(q2, "getLevels");
        if (levels == null || levels.isEmpty()) {
            log("L172");
            return null;
        }
        Object best = pickTopPaidLevel(levels);
        if (best == null) {
            best = levels.get(levels.size() - 1);
        }
        log("L173" + XposedHelpers.callMethod(best, "L174"));

        Class<?> instantCls = XposedHelpers.findClass(C_INSTANT, CL);
        Object companion = XposedHelpers.getStaticObjectField(instantCls, "Companion");
        Object expireAt = XposedHelpers.callMethod(companion, "fromEpochMilliseconds",
                Long.valueOf(FAR_FUTURE_MS));

        Class<?> levelCls = XposedHelpers.findClass(C_LEVEL, CL);
        Object active = XposedHelpers.findClass(C_ACTIVE, CL)
                .getConstructor(levelCls, instantCls, List.class)
                .newInstance(best, expireAt, Collections.emptyList());

        Class<?> flowKt = XposedHelpers.findClass(C_STATE_FLOW_KT, CL);
        return XposedHelpers.callStaticMethod(flowKt, "MutableStateFlow", active);
    }

    private Object pickTopPaidLevel(List<?> levels) {
        Object best = null;
        int bestRank = Integer.MIN_VALUE;
        for (Object level : levels) {
            if (level == null) {
                continue;
            }
            try {
                boolean free = Boolean.TRUE.equals(XposedHelpers.callMethod(level, "isFree"));
                boolean enabled = Boolean.TRUE.equals(XposedHelpers.callMethod(level, "getEnabled"));
                int rank = ((Number) XposedHelpers.callMethod(level, "getRank")).intValue();
                if (!free && enabled && rank > bestRank) {
                    best = level;
                    bestRank = rank;
                }
            } catch (Throwable t) {
                log("L175" + t);
            }
        }
        return best;
    }

    private void h7() {
        Class<?> caps = XposedHelpers.findClass(C_CAPS, CL);
        XposedBridge.hookAllConstructors(caps, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                openCapabilities(param.thisObject, "domain");
            }
        });
        h22(caps, "getCanWatchVip");
        h22(caps, "getCanWatchSvip");

        Class<?> capsApi = XposedHelpers.findClass(C_CAPS_API, CL);
        XposedBridge.hookAllConstructors(capsApi, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                openCapabilities(param.thisObject, "api");
            }
        });
        h22(capsApi, "getCanWatchVip");
        h22(capsApi, "getCanWatchSvip");
        h22(capsApi, "getAdFree");
        h22(capsApi, "getDownloadEnabled");

        Class<?> models = XposedHelpers.findClass(C_MODELS_KT, CL);
        h22(models, "isMember");
        log("L176");
    }

    private void h22(Class<?> clazz, String method) {
        try {
            XposedBridge.hookAllMethods(clazz, method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        param.setResult(Boolean.TRUE);
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            log("L177" + method + "L178" + t);
        }
    }

    private void h13() {
        Class<?> status = XposedHelpers.findClass(C_STATUS, CL);
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    param.setResult(Integer.valueOf(36500));
                } catch (Throwable ignored) {
                }
            }
        };
        try {
            XposedBridge.hookAllMethods(status, "remainingDays", hook);
            XposedBridge.hookAllMethods(status, "remainingDays$default", hook);
        } catch (Throwable t) {
            log("L179" + t);
        }
    }

    private void openCapabilities(Object caps, String where) {
        if (caps == null) {
            return;
        }
        try {
            XposedHelpers.setBooleanField(caps, "adFree", true);
            XposedHelpers.setBooleanField(caps, "canWatchVip", true);
            XposedHelpers.setBooleanField(caps, "canWatchSvip", true);
            XposedHelpers.setBooleanField(caps, "downloadEnabled", true);
            XposedHelpers.setIntField(caps, "dailyDownloadLimit", Integer.MAX_VALUE);
            XposedHelpers.setIntField(caps, "maxConcurrent", 99);
            XposedHelpers.setIntField(caps, "maxDevices", 99);
        } catch (Throwable t) {
            log("L180" + where + "L181" + t);
        }
    }

    /* ------------------------------------------------------------ 工具 */

    private interface Action {
        void run() throws Throwable;
    }

    private void run(String name, Action action) {
        try {
            action.run();
        } catch (Throwable t) {
            log(name + "L182" + t);
        }
    }

    private void log(String msg) {
        try {
            XposedBridge.log(TAG + msg);
        } catch (Throwable ignored) {
        }
        h26(msg);
    }

    /* -------------------------------------------------- 文件日志 / 崩溃捕获 */

    private static final String F12 = "/sdcard/Download/DSH-Go";
    private static final String F13 = "animrep9.log";
    private static final String F14 = "probe.log";
    private static volatile boolean F18;

    /** 写一行到 /sdcard/Download/DSH-Go/animrep.log，失败回退 App 外部私有目录 */
    private static final boolean F4 = false;

    private static void h26(String msg) {
        if (!F4) {
            return;
        }
        String line;
        try {
            line = new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)
                    .format(new java.util.Date()) + "  " + msg + "\n";
        } catch (Throwable t) {
            line = msg + "\n";
        }
        // 只写一处：/sdcard/Download/DSH-Go/animrep.log（App 有 MANAGE_EXTERNAL_STORAGE）
        appendTo(new java.io.File(F12, F13), line);
    }

    private static boolean appendTo(java.io.File f, String line) {
        java.io.FileOutputStream out = null;
        try {
            java.io.File dir = f.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                return false;
            }
            out = new java.io.FileOutputStream(f, true);
            out.write(line.getBytes("UTF-8"));
            out.flush();
            return true;
        } catch (Throwable first) {
            // 文件属主/权限不对时（例如被外部工具截断过），删掉重建立即可恢复
            try {
                f.delete();
                out = new java.io.FileOutputStream(f, true);
                out.write(line.getBytes("UTF-8"));
                out.flush();
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** 装崩溃栈记录：App 崩溃时把 Java 栈写进日志文件，再交给原 handler */
    private static void h24() {
        try {
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.UncaughtExceptionHandler mine = new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    try {
                        java.io.StringWriter sw = new java.io.StringWriter();
                        e.printStackTrace(new java.io.PrintWriter(sw));
                        h26("L183" + t.getName() + "L184");
                        h26(sw.toString());
                    } catch (Throwable ignored) {
                    }
                    if (prev != null && prev != this) {
                        try {
                            prev.uncaughtException(t, e);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            };
            Thread.setDefaultUncaughtExceptionHandler(mine);
            h26("L185" + prev + "L186");
        } catch (Throwable t) {
            h26("L187" + t);
        }
    }

    /** 启动心跳：把启动进程活到第几秒记下来，配合崩溃栈定位崩溃时机 */
    private static void bootHeartbeat() {
        final long[] ticks = {1000L, 3000L, 6000L, 10000L, 20000L, 40000L};
        for (final long t : ticks) {
            onMain(t, new Runnable() {
                public void run() {
                    h26("L188" + (t / 1000) + "L189");
                    if (t == 2000L || t == 8000L) {
                        h24();
                    }
                }
            });
        }
        onMain(2000L, new Runnable() {
            public void run() {
                h24();
            }
        });
        onMain(8000L, new Runnable() {
            public void run() {
                h24();
            }
        });
    }

    /** 环境检测绕过：让宿主的完整性/反调试检查一律汇报"干净" */
    private void h34() {
        int n = 0;
        String[] tinker = {"isTinkerEnabled", "isTinkerEnabledAll", "isTinkerEnabledForArkHot",
                "isTinkerEnabledForDex", "isTinkerEnabledForNativeLib", "isTinkerEnabledForResource",
                "isTinkerEnableWithSharedPreferences"};
        for (String mn : tinker) {
            n += constByName("com.tencent.tinker.loader.shareutil.ShareTinkerInternals", mn, Boolean.FALSE);
        }
        n += constByName(sj("com.android.support.runtime.Support", "Guard"), sj("run", "All"), Integer.valueOf(0));
        n += constByName(sj("com.android.support.runtime.Support", "Loader"), sj("onThreat", "Detected"), null);
        String ic = sj("com.yy.myuko.shared.data.security.integ", "rity.AndroidIntegrityInspectorKt");
        n += constByName(ic, sj("getDETECT_ROOT_", "ENABLED"), Boolean.FALSE);
        n += constByName(ic, sj("matchFrida", "FdLinks"), Boolean.FALSE);
        n += constByName(ic, sj("isEmulator", "Build"), Boolean.FALSE);
        n += constByName(ic, sj("isFrida", "ThreadName"), Boolean.FALSE);
        n += constByName(ic, sj("hasTest", "Keys"), Boolean.FALSE);
        n += constByName(ic, sj("epicAsset", "Hits"), Integer.valueOf(0));
        n += constByName(ic, sj("parseTracer", "Pid"), Integer.valueOf(0));
        n += constByName(ic, sj("rwxpAnon", "Count"), Integer.valueOf(0));
        n += constByName(ic, sj("matchFridaMaps", "Signatures"), java.util.Collections.emptySet());
        n += constByName(ic, sj("matchFrida", "Threads"), java.util.Collections.emptySet());
        n += constByName(ic, sj("matchMaps", "HookLibs"), java.util.Collections.emptyMap());
        n += constByName(ic, sj("matchMitm", "Ca"), java.util.Collections.emptySet());
        log("L210" + n);
    }

    /** 按方法名把返回值钉成常量，返回命中个数 */
    private static int constByName(String cls, String name, final Object val) {
        Class<?> c;
        try {
            c = XposedHelpers.findClass(cls, CL);
        } catch (Throwable t) {
            return 0;
        }
        int n = 0;
        for (final Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(name)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            param.setResult(val);
                        } catch (Throwable ignored) {
                        }
                    }
                });
                n++;
            } catch (Throwable ignored) {
            }
        }
        return n;
    }

    /** 自动签到：进主界面 3 秒后反射 Koin 取 SignInUseCase 调一次 */
    private void h35() {
        Class<?> act = null;
        for (String cn : new String[]{sj("com.yy.myuko.app.Main", "ActivityTinker"), sj("com.yy.myuko.app.Main", "Activity")}) {
            try {
                act = XposedHelpers.findClass(cn, CL);
                break;
            } catch (Throwable ignored) {
            }
        }
        if (act == null) {
            log("L211");
            return;
        }
        int n = 0;
        for (final Method m : act.getDeclaredMethods()) {
            if (!m.getName().equals(sj("on", "Create"))) {
                continue;
            }
            try {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            onMain(3000L, new Runnable() {
                                public void run() {
                                    doSignIn();
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                });
                n++;
            } catch (Throwable ignored) {
            }
        }
        log("L212" + n);
    }

    /** 每个进程只签一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean sSigned =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static void doSignIn() {
        if (!sSigned.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> gc = Class.forName(sj("org.koin.core.context.Global", "Context"), true, CL);
            Object inst = gc.getField("INSTANCE").get(null);
            Object koin = gc.getMethod("get").invoke(inst);
            if (koin == null) {
                h26("L213");
                return;
            }
            Class<?> uc = Class.forName(sj("com.yy.myuko.shared.domain.welfare.usecase.SignIn", "UseCase"), true, CL);
            Class<?> kcls = Class.forName("kotlin.reflect.KClass", true, CL);
            Object kk = Class.forName("kotlin.jvm.internal.Reflection", true, CL)
                    .getMethod("getOrCreateKotlinClass", Class.class).invoke(null, uc);
            Object useCase = koin.getClass().getMethod("getOrNull", kcls,
                    Class.forName("org.koin.core.qualifier.Qualifier", true, CL),
                    Class.forName("kotlin.jvm.functions.Function0", true, CL))
                    .invoke(koin, kk, null, null);
            if (useCase == null) {
                h26("L214");
                return;
            }
            Class<?> cont = Class.forName("kotlin.coroutines.Continuation", true, CL);
            final Object dispatcher = Class.forName("kotlinx.coroutines.Dispatchers", true, CL)
                    .getMethod("getDefault").invoke(null);
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(cont.getClassLoader(),
                    new Class[]{cont}, new java.lang.reflect.InvocationHandler() {
                        public Object invoke(Object p, Method mm, Object[] a) {
                            String nm = mm.getName();
                            if ("getContext".equals(nm)) {
                                return dispatcher;
                            }
                            if ("resumeWith".equals(nm)) {
                                h26("L215");
                            }
                            return null;
                        }
                    });
            uc.getMethod("invoke", cont).invoke(useCase, proxy);
            h26("L216");
        } catch (Throwable t) {
            h26("L217" + t);
        }
    }

    /** 主线程延时任务，用于补装 crash logger 与打启动心跳 */
    private static void onMain(long delayMs, Runnable r) {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(r, delayMs);
        } catch (Throwable t) {
            h26("L190" + t);
        }
    }

    static {
        try {
            h26("L191" + BUILD_VER);
        } catch (Throwable ignored) {
        }
    }

}
