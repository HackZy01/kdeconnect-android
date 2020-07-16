/*
 * Copyright 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 * Copyright 2020 Sylvia van Os <sylvia@hackerchick.me>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License or (at your option) version 3 or any later version
 * accepted by the membership of KDE e.V. (or its successor approved
 * by the membership of KDE e.V.), which shall act as a proxy
 * defined in Section 14 of version 3 of the license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

package org.kde.kdeconnect.Plugins.BigscreenPlugin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;

import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.UserInterface.MainActivity;
import org.kde.kdeconnect.UserInterface.PermissionsAlertDialogFragment;
import org.kde.kdeconnect.UserInterface.ThemeUtil;
import org.kde.kdeconnect_tp.R;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class BigscreenActivity extends AppCompatActivity {

    private static final int REQUEST_SPEECH = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setUserPreferredTheme(this);

        setContentView(R.layout.activity_bigscreen);

        final String deviceId = getIntent().getStringExtra("deviceId");

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            findViewById(R.id.mic_button).setEnabled(false);
            findViewById(R.id.mic_button).setVisibility(View.INVISIBLE);
        }

        BackgroundService.RunWithPlugin(this, deviceId, BigscreenPlugin.class, plugin -> runOnUiThread(() -> {
            findViewById(R.id.left_button).setOnClickListener(v -> plugin.sendLeft());
            findViewById(R.id.right_button).setOnClickListener(v -> plugin.sendRight());
            findViewById(R.id.up_button).setOnClickListener(v -> plugin.sendUp());
            findViewById(R.id.down_button).setOnClickListener(v -> plugin.sendDown());
            findViewById(R.id.select_button).setOnClickListener(v -> plugin.sendSelect());
            findViewById(R.id.home_button).setOnClickListener(v -> plugin.sendHome());
            findViewById(R.id.mic_button).setOnClickListener(v -> {
                if (plugin.hasMicPermission()) {
                    activateSTT();
                } else {
                    new PermissionsAlertDialogFragment.Builder()
                            .setTitle(plugin.getDisplayName())
                            .setMessage(R.string.bigscreen_optional_permission_explanation)
                            .setPositiveButton(R.string.ok)
                            .setNegativeButton(R.string.cancel)
                            .setPermissions(new String[]{Manifest.permission.RECORD_AUDIO})
                            .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                            .create().show(getSupportFragmentManager(), null);
                }
            });
        }));
    }

    public void activateSTT() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.bigscreen_speech_extra_prompt);
        startActivityForResult(intent, REQUEST_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result.get(0) != null) {
                    final String deviceId = getIntent().getStringExtra("deviceId");
                    BackgroundService.RunWithPlugin(this, deviceId, BigscreenPlugin.class, plugin -> runOnUiThread(() -> {
                        plugin.sendSTT(result.get(0));
                    }));
                }
            }
        }
    }
}

