package com.immersive.app.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import com.immersive.app.overlay.ImmersiveSidebarOverlay;

import java.util.List;

public class ImmersiveAccessibilityService extends AccessibilityService {

    private ImmersiveSidebarOverlay sidebarOverlay;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sidebarOverlay = new ImmersiveSidebarOverlay(this);
        sidebarOverlay.attach();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || sidebarOverlay == null) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                        sidebarOverlay.collapseForKeyboard();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        if (sidebarOverlay != null) {
            sidebarOverlay.collapseForKeyboard();
        }
    }

    @Override
    public void onDestroy() {
        if (sidebarOverlay != null) {
            sidebarOverlay.detach();
            sidebarOverlay = null;
        }
        super.onDestroy();
    }
}
