package com.dsh.animerepublic;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** libxposed API 102 入口：转调 legacy 风格的 HookEntry */
public class LibEntry extends XposedModule {

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        XposedBridge.MODULE = this;
        try {
            log(4, "klgc", "L192" + getFrameworkName() + "L194" + getApiVersion());
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        try {
            writeStatus();
        } catch (Throwable ignored) {
        }
    }

    /** 应用对象可能还没创建，带重试地写入状态。 */
    private void writeStatusRetry(final int left) {
        try {
            writeStatus();
        } catch (Throwable ignored) {
        }
        if (left <= 0) {
            return;
        }
        try {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                public void run() {
                    writeStatusRetry(left - 1);
                }
            }, 700L);
        } catch (Throwable ignored) {
        }
    }

    /** 把框架运行信息写进本应用自己的 SharedPreferences，供状态页读取。 */
    private void writeStatus() {
        try {
            Object app = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (!(app instanceof android.content.Context)) {
                return;
            }
            android.content.Context ctx = (android.content.Context) app;
            if (!ctx.getPackageName().equals("com.klgc.animerepublic")) {
                return;
            }
            android.content.SharedPreferences sp = ctx.getSharedPreferences("klgc_rt", 0);
            sp.edit()
                    .putString("fw", getFrameworkName())
                    .putString("fv", getFrameworkVersion())
                    .putLong("fvc", getFrameworkVersionCode())
                    .putInt("api", getApiVersion())
                    .putLong("props", getFrameworkProperties())
                    .putInt("pid", android.os.Process.myPid())
                    .putLong("stamp", System.currentTimeMillis())
                    .putLong("ts", System.currentTimeMillis())
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        XposedBridge.MODULE = this;
        try {
            if ("com.klgc.animerepublic".equals(param.getPackageName())) {
                writeStatusRetry(4);
            }
        } catch (Throwable ignored) {
        }
        try {
            XC_LoadPackage.LoadPackageParam lp = new XC_LoadPackage.LoadPackageParam();
            lp.packageName = param.getPackageName();
            lp.processName = param.getPackageName();
            lp.classLoader = param.getDefaultClassLoader();
            lp.appInfo = param.getApplicationInfo();
            lp.isFirstApplication = param.isFirstPackage();
            new HookEntry().handleLoadPackage(lp);
        } catch (Throwable t) {
            try { log(6, "klgc", "L193" + t, t); } catch (Throwable ignored) {}
        }
    }
}
