package com.immersive.app.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.Choreographer;
import android.view.View;
import com.immersive.app.data.ImmersivePrefs;

public class EdgePerimeterLightingView extends View implements Choreographer.FrameCallback {
    private final ImmersivePrefs prefs;
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF perimeterRect = new RectF();

    private boolean isBinaryActive = false;
    private float binaryBlend = 0f;
    private float sweepAngle = 0f;
    private float touchFocusX = -1f;
    private float touchFocusY = -1f;
    private float shockRadius = 0f;
    private boolean animating = false;

    public EdgePerimeterLightingView(Context context, ImmersivePrefs prefs) {
        super(context);
        this.prefs = prefs;
        glowPaint.setStyle(Paint.Style.STROKE);
        corePaint.setStyle(Paint.Style.STROKE);
        shockPaint.setStyle(Paint.Style.STROKE);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void startLoop() {
        if (!animating) {
            Choreographer.getInstance().postFrameCallback(this);
            animating = true;
        }
    }

    public void stopLoop() {
        animating = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    public void triggerBinaryGestureState(boolean active, float rawX, float rawY) {
        this.isBinaryActive = active;
        if (rawX >= 0f && rawY >= 0f) {
            this.touchFocusX = rawX;
            this.touchFocusY = rawY;
            if (active) {
                this.shockRadius = 12f;
            }
        }
        invalidate();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!animating) return;

        float targetBlend = isBinaryActive ? 1f : 0f;
        binaryBlend += (targetBlend - binaryBlend) * 0.22f;

        if (prefs.isPulseAnimationEnabled()) {
            sweepAngle = (sweepAngle + (isBinaryActive ? 4.5f : 1.4f)) % 360f;
        }

        if (shockRadius > 0f) {
            shockRadius += 28f;
            if (shockRadius > Math.max(getWidth(), getHeight()) * 0.65f) {
                shockRadius = 0f;
            }
        }

        if (prefs.isPulseAnimationEnabled() || Math.abs(targetBlend - binaryBlend) > 0.01f || shockRadius > 0f) {
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!prefs.isEdgeLightingEnabled() || !prefs.isMasterEnabled()) return;

        float density = getResources().getDisplayMetrics().density;
        float strokePx = prefs.getEdgeThicknessDp() * density;
        float cornerPx = prefs.getEdgeCornerDp() * density;

        int c1 = prefs.getPrimaryColor();
        int c2 = prefs.getSecondaryColor();
        int activeColor = blendColors(c1, c2, binaryBlend);
        int secondaryAccent = blendColors(c2, c1, binaryBlend);

        float halfStroke = strokePx * 0.5f;
        perimeterRect.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);

        // Outer diffused OLED halo
        glowPaint.setStrokeWidth(strokePx * 2.6f);
        glowPaint.setShader(new SweepGradient(
                getWidth() * 0.5f,
                getHeight() * 0.5f,
                new int[]{
                        withAlpha(activeColor, 95),
                        withAlpha(secondaryAccent, 145),
                        withAlpha(activeColor, 95)
                },
                null
        ));

        canvas.save();
        canvas.rotate(sweepAngle, getWidth() * 0.5f, getHeight() * 0.5f);
        canvas.drawRoundRect(perimeterRect, cornerPx, cornerPx, glowPaint);
        canvas.restore();

        // Crisp inner perimeter filament
        corePaint.setStrokeWidth(strokePx);
        corePaint.setShader(new LinearGradient(
                0f, 0f, getWidth(), getHeight(),
                activeColor, secondaryAccent, Shader.TileMode.MIRROR
        ));
        canvas.drawRoundRect(perimeterRect, cornerPx, cornerPx, corePaint);

        // Gesture shockwave burst along edge contact point
        if (shockRadius > 0f && touchFocusX >= 0f) {
            int alpha = (int) (220 * Math.max(0f, 1f - (shockRadius / (getHeight() * 0.65f))));
            shockPaint.setStrokeWidth(strokePx * 1.8f);
            shockPaint.setColor(withAlpha(c2, alpha));
            canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            canvas.drawCircle(touchFocusX, touchFocusY, shockRadius, shockPaint);
            canvas.restore();
        }
    }

    private int blendColors(int from, int to, float ratio) {
        float inv = 1f - ratio;
        int a = (int) (Color.alpha(from) * inv + Color.alpha(to) * ratio);
        int r = (int) (Color.red(from) * inv + Color.red(to) * ratio);
        int g = (int) (Color.green(from) * inv + Color.green(to) * ratio);
        int b = (int) (Color.blue(from) * inv + Color.blue(to) * ratio);
        return Color.argb(a, r, g, b);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }
}
