package com.dsh.animerepublic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 桌面入口：一屏看完的模块状态页。 */
public class SettingsActivity extends Activity {

    private static final String[] TARGETS = {"com.sdmnfowa.soad.p1"};
    private static android.content.Context sCtx;

    private static String hostPkg() {
        for (int i = 0; i < TARGETS.length; i++) {
            try {
                sCtx.getPackageManager().getPackageInfo(TARGETS[i], 0);
                return TARGETS[i];
            } catch (Throwable ignored) {
            }
        }
        return TARGETS[0];
    }
    private static final String SELF_PKG = "com.klgc.animerepublic";
    private static final int C_TITLE = 0xFF16181D;
    private static final int C_SUB = 0xFF6E7681;
    private static final int C_OK = 0xFF12B76A;
    private static final int C_BAD = 0xFFE5484D;
    private static final long PROP_RT_API_PROTECTION = 4L;
    private static volatile io.github.libxposed.service.XposedService sSvc = null;
    private static volatile String sProbe = null;
    private static volatile boolean sPinging = false;

    /**
     * 探针：模块激活时，LSPosed 会把本方法 hook 掉并返回框架信息；
     * 未激活时返回空串。返回值跨 ClassLoader 传递，不依赖静态变量。
     */
    public static String probeStatus() {
        return "";
    }

    /** 向宿主进程发 PING，只有 hook 真的在跑才会回话（超时 900ms）。 */
    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        sCtx = this;
        bindFramework();
        try {
            int[] pal = pickSoftPalette();
            android.graphics.drawable.GradientDrawable win =
                    new android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{pastel(pal[0], 0.88f), pastel(pal[1], 0.80f), pastel(pal[2], 0.86f)});
            getWindow().setBackgroundDrawable(win);
            // 状态栏/导航栏全透明，让内容画到它们下面，消除顶部割裂
            getWindow().setStatusBarColor(0x00000000);
            getWindow().setNavigationBarColor(0x00000000);
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } catch (Throwable ignored) {
        }
        try {
            setContentView(build());
        } catch (Throwable t) {
            setContentView(fallback(t));
        }
    }

    /** 兜底界面：任何异常都不许让状态页变成空白/闪退。 */
    private View fallback(Throwable t) {
        ScrollView sc = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText("状态页加载失败\n" + t);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF333333);
        tv.setPadding(dp(20), dp(60), dp(20), dp(20));
        sc.addView(tv);
        return sc;
    }

    private final android.os.Handler ticker =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean alive = false;
    private volatile boolean sPaused = false;

    /** 每 1.5s 探一次框架服务：binder 还活着才算“已启用”。 */
    private final Runnable poll = new Runnable() {
        public void run() {
            if (sPaused) {
                return;
            }
            boolean now;
            try {
                io.github.libxposed.service.XposedService s = sSvc;
                // 多问一项：框架若已把本模块踢出（取消启用 / 撤回作用域），这里会抛异常
                now = s != null && s.getApiVersion() > 0 && s.getScope() != null;
            } catch (Throwable t) {
                sSvc = null;
                now = false;
            }
            if (now != alive) {
                alive = now;
                try {
                    setContentView(build());
                } catch (Throwable ignored) {
                }
            }
            ticker.postDelayed(this, 1500L);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        sPaused = true;
        ticker.removeCallbacks(poll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sPaused = false;
        if (sSvc == null) {
            checkAsync();
        }
        ticker.removeCallbacks(poll);
        ticker.postDelayed(poll, 1500L);
    }

    /** 向框架注册监听，拿到官方 XposedService（拿不到就走 PING 兜底）。 */
    private void bindFramework() {
        try {
            io.github.libxposed.service.XposedServiceHelper.registerListener(
                    new io.github.libxposed.service.XposedServiceHelper.OnServiceListener() {
                        public void onServiceBind(io.github.libxposed.service.XposedService s) {
                            boolean first = sSvc == null;
                            sSvc = s;
                            if (first) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            setContentView(build());
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                });
                            }
                        }

                        public void onServiceDied(io.github.libxposed.service.XposedService s) {
                            sSvc = null;
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** 后台发 PING（结果回调走主线程），避免主线程等待死锁。 */
    private void checkAsync() {
        if (sPinging) {
            return;
        }
        sPinging = true;
        new Thread(new Runnable() {
            public void run() {
                final String r = pingHost();
                sProbe = r == null ? "" : r;
                sPinging = false;
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            setContentView(build());
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        }).start();
    }

    /** 向宿主进程发有序广播，只有 hook 真在跑才会回话；900ms 超时按未运行处理。 */
    private String pingHost() {
        final String[] out = new String[1];
        try {
            final Object lock = new Object();
            BroadcastReceiver rv = new BroadcastReceiver() {
                public void onReceive(Context c, Intent i) {
                    out[0] = getResultData();
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            };
            Intent it = new Intent("com.klgc.animerepublic.PING");
            it.setPackage(hostPkg());
            sendOrderedBroadcast(it, null, rv, null, 0, null, null);
            synchronized (lock) {
                if (out[0] == null) {
                    lock.wait(900L);
                }
            }
            try {
                unregisterReceiver(rv);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return out[0];
    }

    private View build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x00000000);
        try {
            root.addView(new SoftBg(this), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable ignored) {
        }

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setVerticalScrollBarEnabled(false);
        root.addView(sc, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(28) + statusBarHeight(), dp(16), dp(20) + navBarHeight());
        sc.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 顶部：名字 + 版本
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(6), 0, dp(6), dp(14));
        TextView name = new TextView(this);
        name.setText("klgc hook");
        name.setTextSize(22f);
        name.setTextColor(C_TITLE);
        name.setIncludeFontPadding(false);
        head.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView ver = new TextView(this);
        ver.setText("v" + myVersion());
        ver.setTextSize(12f);
        ver.setTextColor(C_SUB);
        head.addView(ver);
        col.addView(head);

        // 第一张卡：状态（优先问框架服务；拿不到再用 PING 兜底）
        LinearLayout st = card();
        io.github.libxposed.service.XposedService svc = sSvc;
        String enabledText;
        int enabledColor;
        String fwText = managerVersion();
        int apiVal = 0;
        boolean prot = false;
        String scopeText = scopeCount() + " 个应用";
        if (svc != null) {
            enabledText = "已启用";
            enabledColor = C_OK;
            try {
                String fw = svc.getFrameworkName();
                String fv = svc.getFrameworkVersion();
                long fc = svc.getFrameworkVersionCode();
                fwText = join(fw, fv, fc);
                apiVal = svc.getApiVersion();
                prot = (svc.getFrameworkProperties() & 4L) != 0;
                java.util.List<String> scopeList = svc.getScope();
                boolean inScope = scopeList != null && scopeList.contains(hostPkg());
                scopeText = (scopeList == null ? 0 : scopeList.size()) + " 个应用" + (inScope ? "（含宿主）" : "（未含宿主）");
            } catch (Throwable ignored) {
            }
        } else {
            String probe = sProbe;
            boolean answered = probe != null && !probe.isEmpty();
            boolean active = answered && probe.startsWith("OK\u0001");
            enabledText = active ? "已启用（宿主运行中）" : "未启用";
            enabledColor = active ? C_OK : C_BAD;
            if (active) {
                try {
                    String[] p = probe.split("\u0001", -1);
                    fwText = join(p.length > 1 ? p[1] : "", p.length > 2 ? p[2] : "",
                            p.length > 3 ? Long.parseLong(p[3]) : 0L);
                    apiVal = p.length > 4 ? Integer.parseInt(p[4]) : 0;
                    prot = (Long.parseLong(p.length > 5 ? p[5] : "0") & 4L) != 0;
                } catch (Throwable ignored) {
                }
            }
        }
        st.addView(two("模块状态", enabledText, enabledColor));
        st.addView(two("框架", fwText, 0));
        st.addView(two("API 版本", apiVal > 0 ? "API " + apiVal : "—", 0));
        st.addView(two("API 调用保护", svc != null ? (prot ? "已开启" : "未开启") : "—",
                svc != null ? (prot ? C_OK : C_BAD) : 0));
        st.addView(two("作用域", scopeText, 0));
        col.addView(st, cardLp());

        // 第二张卡：怎么用
        LinearLayout gd = card();
        gd.addView(two("开关位置", "「我的」→ 长按左上角头像 0.5 秒", 0));
        col.addView(gd, cardLp());

        // 第三张卡：宿主列表（每行右侧一个「打开」小按钮）
        LinearLayout app = card();
        boolean firstRow = true;
        for (int hi = 0; hi < TARGETS.length; hi++) {
            if (!installed(TARGETS[hi])) {
                continue;
            }
            if (!firstRow) {
                app.addView(divider());
            }
            firstRow = false;
            app.addView(hostRow(TARGETS[hi]));
        }
        if (!firstRow) {
            col.addView(app, cardLp());
        }

        TextView foot = new TextView(this);
        foot.setText("本破解免费使用");
        foot.setTextSize(11f);
        foot.setTextColor(C_SUB);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(12);
        col.addView(foot, flp);
        return root;
    }

    private String ago(long ts) {
        long d = System.currentTimeMillis() - ts;
        if (d < 0) {
            return "刚刚";
        }
        long min = d / 60000L;
        if (min < 1) {
            return "刚刚";
        }
        if (min < 60) {
            return min + " 分钟前";
        }
        long h = min / 60L;
        if (h < 24) {
            return h + " 小时前";
        }
        return (h / 24L) + " 天前";
    }

    private String join(String fw, String fv, long code) {
        StringBuilder sb = new StringBuilder(fw == null ? "" : fw);
        if (fv != null && fv.length() > 0) {
            sb.append(" ").append(fv);
        }
        if (code > 0) {
            sb.append(" (").append(code).append(")");
        }
        return sb.toString().trim();
    }

    private String managerVersion() {
        try {
            return "LSPosed " + getPackageManager()
                    .getPackageInfo("org.lsposed.manager", 0).versionName;
        } catch (Throwable t) {
            return "未检测到 LSPosed";
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(6), dp(4), dp(6), dp(4));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xE6FFFFFF);
        g.setCornerRadius(dp(16));
        g.setStroke(dp(1), 0x22304050);
        c.setBackground(g);
        return c;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        return lp;
    }

    /** 一行两列：左边标签、右边值，紧凑排版。 */
    private LinearLayout two(String label, String value, int valueColor) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(9), dp(12), dp(9));
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13.5f);
        l.setTextColor(C_SUB);
        l.setIncludeFontPadding(false);
        r.addView(l, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13.5f);
        v.setTextColor(valueColor == 0 ? C_TITLE : valueColor);
        v.setGravity(Gravity.END);
        v.setIncludeFontPadding(false);
        r.addView(v);
        return r;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(0xFFEDEFF2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.leftMargin = dp(58);
        d.setLayoutParams(lp);
        return d;
    }

    private View hostRow(final String pkg) {
        LinearLayout r = (LinearLayout) appRow(pkg);
        TextView btn = new TextView(this);
        btn.setText("打开");
        btn.setTextSize(13f);
        btn.setTextColor(0xFF3482FF);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x1A3482FF);
        g.setCornerRadius(dp(14));
        btn.setBackground(g);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(62), dp(30));
        blp.rightMargin = dp(2);
        btn.setLayoutParams(blp);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent it = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (it != null) {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(it);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        r.addView(btn);
        return r;
    }

    private LinearLayout appRow(String pkg) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(10), dp(12), dp(10));

        ImageView iv = new ImageView(this);
        try {
            iv.setImageDrawable(getPackageManager().getApplicationIcon(pkg));
        } catch (Throwable ignored) {
            iv.setBackgroundColor(0x22304050);
        }
        r.addView(iv, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(10);
        r.addView(texts, tlp);

        TextView t1 = new TextView(this);
        t1.setText(appName(pkg));
        t1.setTextSize(15f);
        t1.setTextColor(C_TITLE);
        t1.setIncludeFontPadding(false);
        texts.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(pkg);
        t2.setTextSize(11f);
        t2.setTextColor(C_SUB);
        t2.setIncludeFontPadding(false);
        texts.addView(t2);
        return r;
    }

    private View openButton(final String pkg) {
        TextView btn = new TextView(this);
        btn.setText("打开应用");
        btn.setTextSize(14.5f);
        btn.setTextColor(0xFFFFFFFF);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF3482FF);
        g.setCornerRadius(dp(20));
        btn.setBackground(g);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent it = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (it != null) {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(it);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        FrameLayout holder = new FrameLayout(this);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        hlp.leftMargin = dp(6);
        hlp.rightMargin = dp(6);
        hlp.topMargin = dp(2);
        holder.setLayoutParams(hlp);
        holder.addView(btn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return holder;
    }

    private boolean installed(String pkg) {
        try {
            sCtx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String appName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Throwable t) {
            return "未安装";
        }
    }

    private int scopeCount() {
        ZipFile zf = null;
        try {
            zf = new ZipFile(getPackageCodePath());
            ZipEntry e = zf.getEntry("META-INF/xposed/scope.list");
            if (e == null) {
                return 0;
            }
            InputStream in = zf.getInputStream(e);
            byte[] buf = new byte[512];
            int n = in.read(buf);
            in.close();
            if (n <= 0) {
                return 0;
            }
            int c = 0;
            for (String line : new String(buf, 0, n, "UTF-8").split("\n")) {
                if (line.trim().length() > 0) {
                    c++;
                }
            }
            return c;
        } catch (Throwable t) {
            return 0;
        } finally {
            try {
                if (zf != null) {
                    zf.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** 与白色混合，做出低饱和的柔和底色（a=1 表示几乎全白）。 */
    static int pastel(int color, float white) {
        float k = 1f - Math.max(0f, Math.min(1f, white));
        int r = (int) (((color >> 16) & 0xFF) * k + 255 * (1 - k));
        int g = (int) (((color >> 8) & 0xFF) * k + 255 * (1 - k));
        int b = (int) ((color & 0xFF) * k + 255 * (1 - k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int statusBarHeight() {
        return dimen("status_bar_height");
    }

    private int navBarHeight() {
        return dimen("navigation_bar_height");
    }

    private int dimen(String name) {
        try {
            int id = getResources().getIdentifier(name, "dimen", "android");
            return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private String myVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "1.5.0";
        }
    }

    /** 自包含的柔和渐变背景（不引用模块主类的任何静态成员）。 */
    /** 状态页统一配色（窗口背景、内容渐变、状态栏、导航栏共用一套）。 */
    static int[] pickSoftPalette() {
        int[] p = sSoftPal;
        if (p != null) {
            return p;
        }
        int[][] all = {
                {0x66C9AEFF, 0x5CFFB6D5, 0x5AE0C6FF},
                {0x66FFB6A0, 0x5CFF9BB8, 0x5AD8B6FF},
                {0x66B49BFF, 0x5C9B8CFF, 0x5AD8C0FF},
                {0x66E6B4FF, 0x5CC7A8FF, 0x5AE0C0FF}
        };
        p = all[new java.util.Random(System.nanoTime()).nextInt(all.length)];
        sSoftPal = p;
        return p;
    }

    private static volatile int[] sSoftPal = null;

    static class SoftBg extends View {
        private final android.graphics.Paint p = new android.graphics.Paint();
        private final int[] pal;

        SoftBg(Context c) {
            super(c);
            pal = pickSoftPalette();
            p.setAntiAlias(true);
        }

        protected void onDraw(android.graphics.Canvas c) {
            try {
                int w = getWidth();
                int h = getHeight();
                // 先把整块铺满底色，避免顶部/角落露白
                p.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                        new int[]{0x40FFFFFF | (pal[0] & 0x00FFFFFF),
                                pal[1] & 0x00FFFFFF | 0x33FFFFFF,
                                pal[2] & 0x00FFFFFF | 0x2AFFFFFF}, null,
                        android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0f, 0f, w, h, p);
                float[][] pos = {{0.22f, 0.10f}, {0.82f, 0.30f}, {0.45f, 0.78f}};
                for (int i = 0; i < 3; i++) {
                    p.setShader(new android.graphics.RadialGradient(
                            w * pos[i][0], h * pos[i][1], w * 0.95f,
                            pal[i], 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
                    c.drawRect(0, 0, w, h, p);
                }
                p.setShader(null);
            } catch (Throwable ignored) {
            }
        }
    }
}
