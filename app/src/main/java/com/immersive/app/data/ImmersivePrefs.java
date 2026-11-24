package com.immersive.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class ImmersivePrefs {
    private static final String PREF_NAME = "immersive_admin_prefs";
    public static final String KEY_MASTER_ENABLED = "master_enabled";
    public static final String KEY_FULLSCREEN_MASK = "fullscreen_mask";
    public static final String KEY_EDGE_LIGHTING = "edge_lighting";
    public static final String KEY_BOTTOM_NAV_BAR = "bottom_nav_bar";
    public static final String KEY_SIDEBAR_ENABLED = "sidebar_enabled";
    public static final String KEY_TOP_NOTIF_GESTURE = "top_notif_gesture";
    public static final String KEY_HAPTICS_ENABLED = "haptics_enabled";
    public static final String KEY_PULSE_ANIMATION = "pulse_animation";
    public static final String KEY_PRIMARY_COLOR = "binary_primary_color";
    public static final String KEY_SECONDARY_COLOR = "binary_secondary_color";
    public static final String KEY_EDGE_THICKNESS_DP = "edge_thickness_dp";
    public static final String KEY_EDGE_CORNER_DP = "edge_corner_dp";
    public static final String KEY_GESTURE_SENSITIVITY_DP = "gesture_sensitivity_dp";
    public static final String KEY_HAPTIC_INTENSITY = "haptic_intensity";
    public static final String KEY_EXCLUDED_PACKAGES = "excluded_packages";

    private final SharedPreferences prefs;

    public ImmersivePrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isMasterEnabled() { return prefs.getBoolean(KEY_MASTER_ENABLED, true); }
    public void setMasterEnabled(boolean v) { prefs.edit().putBoolean(KEY_MASTER_ENABLED, v).apply(); }

    public boolean isFullscreenMaskEnabled() { return prefs.getBoolean(KEY_FULLSCREEN_MASK, true); }
    public void setFullscreenMaskEnabled(boolean v) { prefs.edit().putBoolean(KEY_FULLSCREEN_MASK, v).apply(); }

    public boolean isEdgeLightingEnabled() { return prefs.getBoolean(KEY_EDGE_LIGHTING, true); }
    public void setEdgeLightingEnabled(boolean v) { prefs.edit().putBoolean(KEY_EDGE_LIGHTING, v).apply(); }

    public boolean isBottomNavBarEnabled() { return prefs.getBoolean(KEY_BOTTOM_NAV_BAR, true); }
    public void setBottomNavBarEnabled(boolean v) { prefs.edit().putBoolean(KEY_BOTTOM_NAV_BAR, v).apply(); }

    public boolean isSidebarEnabled() { return prefs.getBoolean(KEY_SIDEBAR_ENABLED, true); }
    public void setSidebarEnabled(boolean v) { prefs.edit().putBoolean(KEY_SIDEBAR_ENABLED, v).apply(); }

    public boolean isTopNotifGestureEnabled() { return prefs.getBoolean(KEY_TOP_NOTIF_GESTURE, true); }
    public void setTopNotifGestureEnabled(boolean v) { prefs.edit().putBoolean(KEY_TOP_NOTIF_GESTURE, v).apply(); }

    public boolean isHapticsEnabled() { return prefs.getBoolean(KEY_HAPTICS_ENABLED, true); }
    public void setHapticsEnabled(boolean v) { prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, v).apply(); }

    public boolean isPulseAnimationEnabled() { return prefs.getBoolean(KEY_PULSE_ANIMATION, true); }
    public void setPulseAnimationEnabled(boolean v) { prefs.edit().putBoolean(KEY_PULSE_ANIMATION, v).apply(); }

    public int getPrimaryColor() { return prefs.getInt(KEY_PRIMARY_COLOR, 0xFF10B981); }
    public void setPrimaryColor(int color) { prefs.edit().putInt(KEY_PRIMARY_COLOR, color).apply(); }

    public int getSecondaryColor() { return prefs.getInt(KEY_SECONDARY_COLOR, 0xFF00E5FF); }
    public void setSecondaryColor(int color) { prefs.edit().putInt(KEY_SECONDARY_COLOR, color).apply(); }

    public int getEdgeThicknessDp() { return prefs.getInt(KEY_EDGE_THICKNESS_DP, 3); }
    public void setEdgeThicknessDp(int dp) { prefs.edit().putInt(KEY_EDGE_THICKNESS_DP, dp).apply(); }

    public int getEdgeCornerDp() { return prefs.getInt(KEY_EDGE_CORNER_DP, 28); }
    public void setEdgeCornerDp(int dp) { prefs.edit().putInt(KEY_EDGE_CORNER_DP, dp).apply(); }

    public int getGestureSensitivityDp() { return prefs.getInt(KEY_GESTURE_SENSITIVITY_DP, 48); }
    public void setGestureSensitivityDp(int dp) { prefs.edit().putInt(KEY_GESTURE_SENSITIVITY_DP, dp).apply(); }

    public int getHapticIntensity() { return prefs.getInt(KEY_HAPTIC_INTENSITY, 2); }
    public void setHapticIntensity(int level) { prefs.edit().putInt(KEY_HAPTIC_INTENSITY, level).apply(); }

    public Set<String> getExcludedPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_EXCLUDED_PACKAGES, new HashSet<String>()));
    }

    public void setExcludedPackages(Set<String> pkgs) {
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, new HashSet<>(pkgs)).apply();
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
