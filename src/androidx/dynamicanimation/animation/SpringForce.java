package androidx.dynamicanimation.animation;

public class SpringForce {
    private float finalPosition = 0f;
    private float stiffness = 1500f;
    private float damping = 0.5f;

    public SpringForce() {}
    public SpringForce(float finalPosition) { this.finalPosition = finalPosition; }
    public SpringForce setFinalPosition(float f) { this.finalPosition = f; return this; }
    public SpringForce setStiffness(float f) { this.stiffness = f; return this; }
    public SpringForce setDampingRatio(float f) { this.damping = f; return this; }
    public float getFinalPosition() { return finalPosition; }
    public float getStiffness() { return stiffness; }
    public float getDampingRatio() { return damping; }
}
