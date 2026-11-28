package com.dsh.xshim;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** 反射工具兼容层 */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static Class<?> findClass(String name, ClassLoader cl) {
        if (cl != null) {
            try { return Class.forName(name, false, cl); } catch (Throwable ignored) {}
        }
        try { return Class.forName(name); } catch (Throwable ignored) {}
        try { return Class.forName(name, false, ClassLoader.getSystemClassLoader()); } catch (Throwable ignored) {}
        throw new NoClassDefFoundError(name);
    }

    public static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldError(clazz.getName() + "#" + name);
    }

    private static Method findMethodExact(Class<?> clazz, String name, Object[] args) {
        int n = args == null ? 0 : args.length;
        Class<?> c = clazz;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != n) continue;
                m.setAccessible(true);
                return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object invoke(Method m, Object target, Object[] args) {
        try {
            return m.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object callMethod(Object objOrClass, String name, Object... args) {
        Class<?> c = (objOrClass instanceof Class) ? (Class<?>) objOrClass : objOrClass.getClass();
        Method m = findMethodExact(c, name, args);
        if (m == null) throw new NoSuchMethodError(c.getName() + "#" + name);
        return invoke(m, (objOrClass instanceof Class) ? null : objOrClass, args);
    }

    public static Object callStaticMethod(Class<?> c, String name, Object... args) {
        Method m = findMethodExact(c, name, args);
        if (m == null) throw new NoSuchMethodError(c.getName() + "#" + name);
        return invoke(m, null, args);
    }

    public static Object getObjectField(Object obj, String name) {
        try { return findField(obj.getClass(), name).get(obj); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Object getStaticObjectField(Class<?> c, String name) {
        try { return findField(c, name).get(null); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void setBooleanField(Object obj, String name, boolean v) {
        try { findField(obj.getClass(), name).setBoolean(obj, v); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void setIntField(Object obj, String name, int v) {
        try { findField(obj.getClass(), name).setInt(obj, v); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void setObjectField(Object obj, String name, Object v) {
        try { findField(obj.getClass(), name).set(obj, v); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Object newInstance(Class<?> c, Object... args) {
        for (java.lang.reflect.Constructor<?> ctor : c.getDeclaredConstructors()) {
            if (ctor.getParameterTypes().length != (args == null ? 0 : args.length)) continue;
            try {
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            } catch (Throwable ignored) {}
        }
        throw new RuntimeException("no ctor for " + c.getName());
    }
}
