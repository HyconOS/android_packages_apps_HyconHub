/*
 * Copyright (C) 2020 Project-Awaken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hycon.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.SearchIndexableResource;

import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.logging.nano.MetricsProto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.settings.custom.preference.SystemSettingSwitchPreference;

import com.android.settings.custom.preference.CustomSeekBarPreference;

import com.android.settings.custom.colorpicker.ColorPickerPreference;

import com.android.internal.util.custom.CustomUtils;

@SearchIndexable
public class Lockscreen extends SettingsPreferenceFragment
              implements Preference.OnPreferenceChangeListener {

    private static final String AOD_SCHEDULE_KEY = "always_on_display_schedule";
    private static final String AMBIENT_ICONS_COLOR = "ambient_icons_color";
    private static final String AMBIENT_ICONS_LOCKSCREEN = "ambient_icons_lockscreen";
    private static final String NOTIFICATION_PULSE_COLOR = "ambient_notification_light_color";
    private static final String NOTIFICATION_PULSE_DURATION = "notification_pulse_duration";
    private static final String NOTIFICATION_PULSE_REPEATS = "notification_pulse_repeats";
    private static final String PULSE_COLOR_MODE_PREF = "ambient_notification_light_color_mode";
    private static final String KEY_AMBIENT = "ambient_notification_light_enabled";

    static final int MODE_DISABLED = 0;
    static final int MODE_NIGHT = 1;
    static final int MODE_TIME = 2;
    static final int MODE_MIXED_SUNSET = 3;
    static final int MODE_MIXED_SUNRISE = 4;

    private ContentResolver mResolver;
    private ColorPickerPreference mAmbientIconsColor;
    private SystemSettingSwitchPreference mAmbientIconsLockscreen;
    private ColorPickerPreference mEdgeLightColorPreference;
    private CustomSeekBarPreference mEdgeLightDurationPreference;
    private CustomSeekBarPreference mEdgeLightRepeatCountPreference;
    private ListPreference mColorMode;
    private SystemSettingSwitchPreference mAmbientPref;

    Preference mAODPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.lockscreen);
        PreferenceScreen prefSet = getPreferenceScreen();
        final Resources res = getResources();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mAODPref = findPreference(AOD_SCHEDULE_KEY);
        updateAlwaysOnSummary();
        mAmbientIconsLockscreen = (SystemSettingSwitchPreference) findPreference(AMBIENT_ICONS_LOCKSCREEN);
        mAmbientIconsLockscreen.setChecked((Settings.System.getInt(resolver,
                Settings.System.AMBIENT_ICONS_LOCKSCREEN, 0) == 1));
        mAmbientIconsLockscreen.setOnPreferenceChangeListener(this);

        // Ambient Icons Color
        mAmbientIconsColor = (ColorPickerPreference) findPreference(AMBIENT_ICONS_COLOR);
        int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_ICONS_COLOR, Color.WHITE);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mAmbientIconsColor.setNewPreviewColor(intColor);
        mAmbientIconsColor.setSummary(hexColor);
        mAmbientIconsColor.setOnPreferenceChangeListener(this);

        mAmbientPref = (SystemSettingSwitchPreference) findPreference(KEY_AMBIENT);
        boolean aodEnabled = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.DOZE_ALWAYS_ON, 0, UserHandle.USER_CURRENT) == 1;
        if (!aodEnabled) {
            mAmbientPref.setChecked(false);
            mAmbientPref.setEnabled(false);
            mAmbientPref.setSummary(R.string.aod_disabled);
        }

        mEdgeLightRepeatCountPreference = (CustomSeekBarPreference) findPreference(NOTIFICATION_PULSE_REPEATS);
        mEdgeLightRepeatCountPreference.setOnPreferenceChangeListener(this);
        int repeats = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_REPEATS, 0);
        mEdgeLightRepeatCountPreference.setValue(repeats);

        mEdgeLightDurationPreference = (CustomSeekBarPreference) findPreference(NOTIFICATION_PULSE_DURATION);
        mEdgeLightDurationPreference.setOnPreferenceChangeListener(this);
        int duration = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_DURATION, 2);
        mEdgeLightDurationPreference.setValue(duration);

        mColorMode = (ListPreference) findPreference(PULSE_COLOR_MODE_PREF);
        int value;
        boolean colorModeAutomatic = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 0) != 0;
        boolean colorModeAccent = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_ACCENT, 0) != 0;
        if (colorModeAutomatic) {
            value = 0;
        } else if (colorModeAccent) {
            value = 1;
        } else {
            value = 2;
        }

        mColorMode.setValue(Integer.toString(value));
        mColorMode.setSummary(mColorMode.getEntry());
        mColorMode.setOnPreferenceChangeListener(this);

        mEdgeLightColorPreference = (ColorPickerPreference) findPreference(NOTIFICATION_PULSE_COLOR);
        int edgeLightColor = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_COLOR, 0xFF3980FF);
        mEdgeLightColorPreference.setNewPreviewColor(edgeLightColor);
        mEdgeLightColorPreference.setAlphaSliderEnabled(false);
        String edgeLightColorHex = String.format("#%08x", (0xFF3980FF & edgeLightColor));
        if (edgeLightColorHex.equals("#ff1a73e8")) {
            mEdgeLightColorPreference.setSummary(R.string.color_default);
        } else {
            mEdgeLightColorPreference.setSummary(edgeLightColorHex);
        }
        mEdgeLightColorPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAlwaysOnSummary();
    }

    private void updateAlwaysOnSummary() {
        if (mAODPref == null) return;
        int mode = Settings.Secure.getIntForUser(getActivity().getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_MODE, 0, UserHandle.USER_CURRENT);
        switch (mode) {
            default:
            case MODE_DISABLED:
                break;
            case MODE_NIGHT:
                mAODPref.setSummary(R.string.night_display_auto_mode_twilight);
                break;
            case MODE_TIME:
                mAODPref.setSummary(R.string.night_display_auto_mode_custom);
                break;
            case MODE_MIXED_SUNSET:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunset);
                break;
            case MODE_MIXED_SUNRISE:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunrise);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mAmbientIconsLockscreen) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                    Settings.System.AMBIENT_ICONS_LOCKSCREEN, value ? 1 : 0);
            return true;
        } else if (preference == mAmbientIconsColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                .parseInt(String.valueOf(objValue)));
            mAmbientIconsColor.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.AMBIENT_ICONS_COLOR, intHex);
            return true;
        } else  if (preference == mEdgeLightColorPreference) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            if (hex.equals("#ff1a73e8")) {
                preference.setSummary(R.string.color_default);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_PULSE_COLOR, intHex);
            return true;
        } else if (preference == mEdgeLightRepeatCountPreference) {
                int value = (Integer) objValue;
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_REPEATS, value);
                return true;
        } else if (preference == mEdgeLightDurationPreference) {
            int value = (Integer) objValue;
                Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_PULSE_DURATION, value);
            return true;
        } else if (preference == mColorMode) {
             int value = Integer.valueOf((String) objValue);
            int index = mColorMode.findIndexOfValue((String) objValue);
            mColorMode.setSummary(mColorMode.getEntries()[index]);
            if (value == 0) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 1);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 0);
            } else if (value == 1) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 1);
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.lockscreen;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
