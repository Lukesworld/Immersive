package com.immersive.app.overlay;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.immersive.app.R;

import java.util.function.Consumer;

public class ImmersiveSidebarOverlay {

    private final AccessibilityService service;
    private final WindowManager windowManager;

    private LinearLayout sidebarContainer;
    private WindowManager.LayoutParams layoutParams;
    private SpringAnimator springAnimator;
    private VelocityTracker velocityTracker;

    private boolean isExpanded = false;
    private boolean thresholdCrossed = false;
    private float initialTouchX = 0f;
    private int sidebarWidthPx = 0;
    private int handlePeekWidthPx = 0;

    private final Consumer<Boolean> blurEnabledListener = this::applyGlassStyling;

    public ImmersiveSidebarOverlay(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attach() {
        float density = service.getResources().getDisplayMetrics().density;
        sidebarWidthPx = (int) (220 * density);
        handlePeekWidthPx = (int) (48 * density); // WCAG 48dp accessible touch target
        int collapsedOffset = sidebarWidthPx - (int) (18 * density);

        sidebarContainer = new LinearLayout(service);
        sidebarContainer.setOrientation(LinearLayout.VERTICAL);
        sidebarContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (16 * density);
        sidebarContainer.setPadding(pad, pad, pad, pad);
        sidebarContainer.setTranslationX(collapsedOffset);
        sidebarContainer.setContentDescription(service.getString(R.string.cd_immersive_sidebar));

        // Populate accessible 48dp+ edge action buttons
        addActionPill("BACK", density, v ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK));
        addActionPill("HOME", density, v ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME));
        addActionPill("RECENTS", density, v ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS));
        addActionPill("HIDE", density, v -> collapseForKeyboard());

        springAnimator = new SpringAnimator(sidebarContainer, 220f, 0.72f);

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        }

        layoutParams = new WindowManager.LayoutParams(
                sidebarWidthPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layoutParams.setBlurBehindRadius((int) (24 * density));
            applyGlassStyling(windowManager.isCrossWindowBlurEnabled());
            windowManager.addCrossWindowBlurEnabledListener(blurEnabledListener);
        } else {
            applyGlassStyling(false);
        }

        sidebarContainer.setOnTouchListener((view, event) ->
                handleElasticGesture(view, event, collapsedOffset));

        windowManager.addView(sidebarContainer, layoutParams);
    }

    private void addActionPill(String label, float density, View.OnClickListener listener) {
        TextView btn = new TextView(service);
        btn.setText(label);
        btn.setTextColor(service.getColor(R.color.immersive_text_primary));
        btn.setTextSize(14f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setMinimumHeight((int) (48 * density));
        btn.setClickable(true);
        btn.setOnClickListener(v -> {
            triggerHapticCheckpoint(v, true);
            listener.onClick(v);
        });

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(14 * density);
        pillBg.setColor(Color.parseColor("#2610B981"));
        pillBg.setStroke((int) density, service.getColor(R.color.immersive_glass_stroke));
        btn.setBackground(pillBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * density)
        );
        lp.topMargin = (int) (8 * density);
        lp.bottomMargin = (int) (8 * density);
        sidebarContainer.addView(btn, lp);
    }

    private void applyGlassStyling(boolean blurSupported) {
        float density = service.getResources().getDisplayMetrics().density;
        int bgColor = service.getColor(
                blurSupported ? R.color.immersive_glass_scrim : R.color.immersive_glass_fallback
        );
        int strokeColor = service.getColor(R.color.immersive_glass_stroke);

        GradientDrawable glassDrawable = new GradientDrawable();
        glassDrawable.setShape(GradientDrawable.RECTANGLE);
        glassDrawable.setCornerRadii(new float[]{
                24 * density, 24 * density,
                0f, 0f,
                0f, 0f,
                24 * density, 24 * density
        });
        glassDrawable.setColor(bgColor);
        glassDrawable.setStroke((int) Math.max(1f, 1.5f * density), strokeColor);

        sidebarContainer.setBackground(glassDrawable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sidebarContainer.isAttachedToWindow()) {
            layoutParams.setBlurBehindRadius(blurSupported ? (int) (24 * density) : 0);
            windowManager.updateViewLayout(sidebarContainer, layoutParams);
        }
    }

    private boolean handleElasticGesture(View view, MotionEvent event, int collapsedOffset) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                springAnimator.cancel();
                if (velocityTracker != null) velocityTracker.recycle();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                initialTouchX = event.getRawX();
                thresholdCrossed = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(event);
                float deltaX = event.getRawX() - initialTouchX;
                float rawTarget = isExpanded ? deltaX : (collapsedOffset + deltaX);
                float clampedX = Math.max(0f, Math.min(collapsedOffset, rawTarget));
                view.setTranslationX(clampedX);

                boolean crossedNow = Math.abs(deltaX) > handlePeekWidthPx;
                if (crossedNow != thresholdCrossed) {
                    thresholdCrossed = crossedNow;
                    triggerHapticCheckpoint(view, crossedNow);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float xVelocity = 0f;
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    xVelocity = velocityTracker.getXVelocity();
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                boolean shouldExpand;
                if (xVelocity < -600f) {
                    shouldExpand = true;
                } else if (xVelocity > 600f) {
                    shouldExpand = false;
                } else {
                    shouldExpand = view.getTranslationX() < (collapsedOffset * 0.5f);
                }

                isExpanded = shouldExpand;
                springAnimator.animateTo(shouldExpand ? 0f : collapsedOffset, xVelocity);
                return true;
        }
        return false;
    }

    private void triggerHapticCheckpoint(View view, boolean activated) {
        int constant;
        if (Build.VERSION.SDK_INT >= 34) {
            constant = activated
                    ? HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                    : HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE;
        } else {
            constant = HapticFeedbackConstants.CLOCK_TICK;
        }
        view.performHapticFeedback(constant);
    }

    public void collapseForKeyboard() {
        if (isExpanded && sidebarContainer != null && springAnimator != null) {
            float density = service.getResources().getDisplayMetrics().density;
            int collapsedOffset = sidebarWidthPx - (int) (18 * density);
            isExpanded = false;
            springAnimator.animateTo(collapsedOffset, 1200f);
        }
    }

    public void detach() {
        if (springAnimator != null) springAnimator.cancel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            windowManager.removeCrossWindowBlurEnabledListener(blurEnabledListener);
        }
        if (sidebarContainer != null && sidebarContainer.isAttachedToWindow()) {
            windowManager.removeView(sidebarContainer);
        }
    }
}
