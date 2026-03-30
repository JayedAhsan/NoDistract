package com.jayed.nodistract;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends Activity {

    private MaterialButton enableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        setContentView(R.layout.app_main);

        enableButton = findViewById(R.id.btnEnable);
        ImageButton infoButton = findViewById(R.id.btnInfo);
        ImageButton sourceButton = findViewById(R.id.btnSource);
        ImageButton developerButton = findViewById(R.id.btnDeveloper);


        enableButton.setOnClickListener(v -> {
            boolean isAccessibilityEnabled = isAccessibilityServiceEnabled(this);

            if (!isAccessibilityEnabled) {
                // ask for perm
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                Toast.makeText(this, "Please find 'NoDistract' and turn it on.", Toast.LENGTH_LONG).show();
            } else {
                // just toggle perm found
                SharedPreferences prefs = getSharedPreferences("NoDistractPrefs", MODE_PRIVATE);
                boolean isProtectionActive = prefs.getBoolean("isProtectionActive", true);
                boolean newState = !isProtectionActive;

                // save to shared
                prefs.edit().putBoolean("isProtectionActive", newState).apply();
                updateButtonState();

                Toast.makeText(this, newState ? "Protection Resumed" : "Protection Paused", Toast.LENGTH_SHORT).show();
            }
        });


        infoButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("App Information")
                    .setMessage("Name: NoDistract\n" +
                            "Version: 2026.3.30\n" +
                            "Developer: Jayed Ahsan Saad\n\n" +
                            "Licensed Under Apache 2.0")
                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        sourceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JayedAhsan"));
            startActivity(intent);
        });

        developerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://jayed.me"));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
    }

    private void updateButtonState() {
        if (enableButton == null) return;

        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled(this);
        SharedPreferences prefs = getSharedPreferences("NoDistractPrefs", MODE_PRIVATE);
        boolean isProtectionActive = prefs.getBoolean("isProtectionActive", true);

        if (isAccessibilityEnabled && isProtectionActive) {

            int primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimarySurface, android.graphics.Color.BLUE);
            int onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE);

            enableButton.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            enableButton.setIconTint(ColorStateList.valueOf(onPrimaryColor));
        } else {

            int surfaceVariantColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, android.graphics.Color.GRAY);
            int onSurfaceVariantColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.DKGRAY);

            enableButton.setBackgroundTintList(ColorStateList.valueOf(surfaceVariantColor));
            enableButton.setIconTint(ColorStateList.valueOf(onSurfaceVariantColor));
        }
    }


    private boolean isAccessibilityServiceEnabled(Context context) {
        ComponentName expectedComponentName = new ComponentName(context, NoDistractService.class);
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServicesSetting == null) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                return true;
            }
        }
        return false;
    }
}