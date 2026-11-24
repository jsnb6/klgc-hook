package androidx.dynamicanimation.animation;

public abstract class DynamicAnimation<T> {
    public static final int SCALE_X = 0;
    public static final int SCALE_Y = 1;
    public static final int ROTATION_X = 2;
    public static final int ROTATION_Y = 3;
    public static final int TRANSLATION_X = 4;
    public static final int TRANSLATION_Y = 5;
    public abstract void animateToFinalPosition(float value);
}
