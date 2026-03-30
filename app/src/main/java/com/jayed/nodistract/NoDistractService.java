package com.jayed.nodistract;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.util.Log;
import android.content.SharedPreferences;

public class NoDistractService extends AccessibilityService {

    private static final String TAG = "NoDistract Jayed";
    private long lastBlockTime = 0;

    public static boolean allowCurrentReel = false;
    public static long allowReelTime = 0;

    private WindowManager windowManager;
    private AudioManager audioManager;
    private View overlayView;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {};

    private static class UIState {
        boolean hasHomeTab = false;
        boolean hasReelsText = false;
        boolean hasLike = false;
        boolean hasComment = false;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "NoDistract Service Connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if the user paused protection in the main app
        SharedPreferences prefs = getSharedPreferences("NoDistractPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("isProtectionActive", true)) {
            return;
        }

        if (event == null || event.getPackageName() == null ||
                !event.getPackageName().toString().equals("com.facebook.katana")) {  // for now I only added facebook app
            return;
        }

        long currentTime = System.currentTimeMillis();

      // detect scroll but still unstable :(
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (allowCurrentReel && (currentTime - allowReelTime > 1500)) {
                Log.d(TAG, "swiped");
                allowCurrentReel = false;
            }
        }

        // debounce to save power
        if (currentTime - lastBlockTime < 1000) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        try {
            boolean isReelTab = isReelsTabSelected(rootNode);

            UIState state = new UIState();
            scanUI(rootNode, state);

            boolean isReelPlayer = false;

          //tab detection
            if (!state.hasHomeTab && state.hasReelsText && state.hasLike && state.hasComment) {
                isReelPlayer = true;
            }

            if (isReelTab || isReelPlayer) {
                if (!allowCurrentReel) {
                    Log.d(TAG, "Reel detected!");
                    lastBlockTime = currentTime;
                    pauseVideo();
                    showOverlay();
                }
            } else {

                if (state.hasHomeTab && !isReelTab) {
                    allowCurrentReel = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing accessibility nodes", e);
        } finally {
            rootNode.recycle();
        }
    }

    private void scanUI(AccessibilityNodeInfo node, UIState state) {
        if (node == null) return;

        CharSequence desc = node.getContentDescription();

        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if (d.equals("home") || d.contains("home, tab")) state.hasHomeTab = true;
            if (d.contains("reels")) state.hasReelsText = true;
            if (d.equals("like") || d.contains("like button")) state.hasLike = true;
            if (d.equals("comment") || d.contains("comment button")) state.hasComment = true;
        }

        if (state.hasHomeTab && state.hasReelsText && state.hasLike && state.hasComment) return;

        for (int i = 0; i < node.getChildCount(); i++) {
            scanUI(node.getChild(i), state);
        }
    }

    private boolean isReelsTabSelected(AccessibilityNodeInfo node) {
        if (node == null) return false;

        CharSequence desc = node.getContentDescription();
        CharSequence text = node.getText();

        boolean hasReelsText = (desc != null && desc.toString().toLowerCase().contains("reels")) ||
                (text != null && text.toString().toLowerCase().contains("reels"));

        if (hasReelsText && node.isSelected()) {
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (isReelsTabSelected(node.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    private void pauseVideo() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
        }
    }

    private void releaseAudio() {
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
    }

    private void showOverlay() {
        if (overlayView != null) return;

        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_Material3_Light_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(themedContext);

        overlayView = inflater.inflate(R.layout.block_overlay, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.6f;

        Button btnBack = overlayView.findViewById(R.id.btnBack);
        Button btnShowAnyway = overlayView.findViewById(R.id.btnShowAnyway);

        btnBack.setOnClickListener(v -> {
            performGlobalAction(GLOBAL_ACTION_BACK);
            removeOverlay();
        });

        btnShowAnyway.setOnClickListener(v -> {
            allowCurrentReel = true;
            allowReelTime = System.currentTimeMillis();
            removeOverlay();
        });

        windowManager.addView(overlayView, params);
    }

    private void removeOverlay() {
        releaseAudio();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @Override
    public void onInterrupt() {
        removeOverlay();
    }
}