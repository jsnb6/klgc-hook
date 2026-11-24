package androidx.annotation;
public @interface RequiresApi {
    int value() default 0;
    int api() default 0;
}
