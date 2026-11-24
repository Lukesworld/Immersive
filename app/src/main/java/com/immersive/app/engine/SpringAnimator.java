package com.immersive.app.engine;

import android.view.Choreographer;
import android.view.View;

public class SpringAnimator implements Choreographer.FrameCallback {
    public enum Axis { TRANSLATION_X, TRANSLATION_Y, ALPHA, SCALE }

    private final View target;
    private final Axis axis;
    private final float finalPosition;
    private final float stiffness;
    private final float damping;
    private float position;
    private float velocity;
    private long lastFrameNanos = -1L;
    private boolean running = false;
    private Runnable endAction;

    public SpringAnimator(View target, Axis axis, float finalPosition, float startVelocity,
                          float stiffness, float dampingRatio) {
        this.target = target;
        this.axis = axis;
        this.finalPosition = finalPosition;
        this.velocity = startVelocity;
        this.stiffness = stiffness;
        this.damping = (float) (2.0 * dampingRatio * Math.sqrt(stiffness));
        this.position = readCurrent();
    }

    public SpringAnimator withEndAction(Runnable action) {
        this.endAction = action;
        return this;
    }

    private float readCurrent() {
        switch (axis) {
            case TRANSLATION_X: return target.getTranslationX();
            case TRANSLATION_Y: return target.getTranslationY();
            case ALPHA: return target.getAlpha();
            case SCALE: return target.getScaleX();
            default: return 0f;
        }
    }

    private void applyCurrent(float val) {
        switch (axis) {
            case TRANSLATION_X: target.setTranslationX(val); break;
            case TRANSLATION_Y: target.setTranslationY(val); break;
            case ALPHA: target.setAlpha(Math.max(0f, Math.min(1f, val))); break;
            case SCALE:
                target.setScaleX(val);
                target.setScaleY(val);
                break;
        }
    }

    public void start() {
        if (running) return;
        running = true;
        lastFrameNanos = -1L;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void cancel() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (lastFrameNanos < 0L) {
            lastFrameNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        float dt = Math.min((frameTimeNanos - lastFrameNanos) / 1_000_000_000f, 0.032f);
        lastFrameNanos = frameTimeNanos;

        float displacement = position - finalPosition;
        float springForce = -stiffness * displacement;
        float dampingForce = -damping * velocity;
        float acceleration = springForce + dampingForce;

        velocity += acceleration * dt;
        position += velocity * dt;

        if (Math.abs(position - finalPosition) < 0.35f && Math.abs(velocity) < 8f) {
            applyCurrent(finalPosition);
            running = false;
            if (endAction != null) endAction.run();
            return;
        }

        applyCurrent(position);
        Choreographer.getInstance().postFrameCallback(this);
    }
}
