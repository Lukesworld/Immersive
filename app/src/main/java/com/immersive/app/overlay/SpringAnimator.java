package com.immersive.app.overlay;

import android.view.Choreographer;
import android.view.View;

/**
 * Zero-dependency Hooke's Law spring physics engine synchronized to VSYNC via Choreographer.
 * Eliminates androidx.dynamicanimation AAR dependency for fast Termux javac/d8 builds.
 */
public final class SpringAnimator implements Choreographer.FrameCallback {

    private final View targetView;
    private final float stiffness;
    private final float dampingRatio;

    private float targetX;
    private float currentX;
    private float velocityX;
    private long lastFrameNanos;
    private boolean running;

    public SpringAnimator(View targetView, float stiffness, float dampingRatio) {
        this.targetView = targetView;
        this.stiffness = stiffness;
        this.dampingRatio = dampingRatio;
    }

    public void animateTo(float finalPositionX, float startVelocityPxPerSec) {
        cancel();
        this.targetX = finalPositionX;
        this.currentX = targetView.getTranslationX();
        this.velocityX = startVelocityPxPerSec;
        this.lastFrameNanos = 0L;
        this.running = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void cancel() {
        if (running) {
            running = false;
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;

        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        // Clamp delta time to avoid physics explosion on frame drops
        float dt = Math.min((frameTimeNanos - lastFrameNanos) / 1_000_000_000f, 0.032f);
        lastFrameNanos = frameTimeNanos;

        float displacement = currentX - targetX;
        float damping = (float) (2.0 * dampingRatio * Math.sqrt(stiffness));
        float acceleration = (-stiffness * displacement) - (damping * velocityX);

        velocityX += acceleration * dt;
        currentX += velocityX * dt;

        targetView.setTranslationX(currentX);

        if (Math.abs(currentX - targetX) < 0.5f && Math.abs(velocityX) < 5.0f) {
            targetView.setTranslationX(targetX);
            running = false;
        } else {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }
}
