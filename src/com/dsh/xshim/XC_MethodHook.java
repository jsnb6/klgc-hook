package com.dsh.xshim;

import java.lang.reflect.Member;

/** libxposed 102 兼容层：模拟 legacy XC_MethodHook */
public abstract class XC_MethodHook {
    public static final int PRIORITY_DEFAULT = 50;
    public static final int PRIORITY_LOWEST = -10000;
    public static final int PRIORITY_HIGHEST = 10000;

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public Unhook unhook() { return new Unhook(); }

    public static class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        Object result;
        Throwable throwable;
        /** 是否在 before 阶段就给了结果（给了就不再执行原方法） */
        public boolean resultSet;

        public Object getResult() { return result; }
        public void setResult(Object r) { result = r; throwable = null; resultSet = true; }
        public Throwable getThrowable() { return throwable; }
        public void setThrowable(Throwable t) { throwable = t; }
        public boolean hasThrowable() { return throwable != null; }
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
    }

    public static class Unhook {
        public void unhook() {}
    }
}
