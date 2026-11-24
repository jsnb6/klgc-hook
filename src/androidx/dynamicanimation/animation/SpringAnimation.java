package androidx.dynamicanimation.animation;

import android.view.View;

public class SpringAnimation extends DynamicAnimation<SpringAnimation> {
    private final View view;
    private final int prop;
    private SpringForce spring = new SpringForce();

    public SpringAnimation(View view, int prop) {
        this.view = view;
        this.prop = prop;
    }

    public SpringAnimation setSpring(SpringForce s) {
        if (s != null) {
            this.spring = s;
        }
        return this;
    }

    @Override
    public void animateToFinalPosition(float value) {
        try {
            if (view == null) {
                return;
            }
            if (prop == SCALE_X) {
                view.animate().scaleX(value).setDuration(180).start();
            } else if (prop == SCALE_Y) {
                view.animate().scaleY(value).setDuration(180).start();
            } else if (prop == ROTATION_X) {
                view.animate().rotationX(value).setDuration(180).start();
            } else if (prop == ROTATION_Y) {
                view.animate().rotationY(value).setDuration(180).start();
            } else if (prop == TRANSLATION_X) {
                view.animate().translationX(value).setDuration(180).start();
            } else if (prop == TRANSLATION_Y) {
                view.animate().translationY(value).setDuration(180).start();
            }
        } catch (Throwable ignored) {
        }
    }
}
