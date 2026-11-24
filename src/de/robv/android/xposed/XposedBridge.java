package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** 把 legacy hook 调用翻译成 libxposed 102 的 hook()/intercept() */
public final class XposedBridge {
    private XposedBridge() {}

    public static volatile XposedModule MODULE;

    public static void log(String msg) {
        try {
            XposedModule m = MODULE;
            if (m != null) m.log(4, "AnimRep", msg);
            else android.util.Log.i("AnimRep", msg);
        } catch (Throwable ignored) {}
    }

    public static void log(Throwable t) {
        try {
            XposedModule m = MODULE;
            if (m != null) m.log(6, "AnimRep", String.valueOf(t), t);
            else android.util.Log.e("AnimRep", String.valueOf(t), t);
        } catch (Throwable ignored) {}
    }

    private static final class Bridge implements XposedInterface.Hooker {
        private final XC_MethodHook cb;
        Bridge(XC_MethodHook cb) { this.cb = cb; }

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            XC_MethodHook.MethodHookParam p = new XC_MethodHook.MethodHookParam();
            p.method = chain.getExecutable();
            p.thisObject = chain.getThisObject();
            List<Object> args = chain.getArgs();
            p.args = args == null ? new Object[0] : args.toArray();
            cb.beforeHookedMethod(p);
            if (p.hasThrowable()) throw p.getThrowable();
            if (p.resultSet) {
                // legacy 语义：before 里 setResult 就跳过原方法（也不回调 after）
                return p.getResult();
            }
            Object ret = chain.proceed(p.args);
            p.setResult(ret);
            cb.afterHookedMethod(p);
            if (p.hasThrowable()) throw p.getThrowable();
            return p.getResult();
        }
    }

    private static int installed(Executable e, XC_MethodHook cb) {
        try {
            XposedModule m = MODULE;
            if (m == null) return 0;
            m.hook(e)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(new Bridge(cb));
            return 1;
        } catch (Throwable t) {
            log("hook 失败 " + e + " : " + t);
            return 0;
        }
    }

    private static Set<XC_MethodHook.Unhook> sized(int n) {
        java.util.LinkedHashSet<XC_MethodHook.Unhook> s = new java.util.LinkedHashSet<>();
        for (int i = 0; i < n; i++) s.add(new XC_MethodHook.Unhook());
        return s;
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> c, String name, XC_MethodHook cb) {
        int n = 0;
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name)) n += installed(m, cb);
        }
        return sized(n);
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> c, XC_MethodHook cb) {
        int n = 0;
        for (Constructor<?> ctor : c.getDeclaredConstructors()) n += installed(ctor, cb);
        return sized(n);
    }

    public static XC_MethodHook.Unhook hookMethod(java.lang.reflect.Member m, XC_MethodHook cb) {
        if (m instanceof Executable) installed((Executable) m, cb);
        return new XC_MethodHook.Unhook();
    }

}
