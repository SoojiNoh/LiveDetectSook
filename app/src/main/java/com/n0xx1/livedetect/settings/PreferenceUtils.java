package com.n0xx1.livedetect.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.gms.common.images.Size;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.CameraSizePair;

public class PreferenceUtils {
    public static boolean isAutoSearchEnabled(Context context) {
        return getBooleanPref(context, R.string.pref_key_enable_auto_search, true);
    }

    public static boolean isMultipleObjectsMode(Context context) {
        return getBooleanPref(
                context, R.string.pref_key_object_detector_enable_multiple_objects, false);
    }

    public static boolean isClassificationEnabled(Context context) {
        return getBooleanPref(
                context, R.string.pref_key_object_detector_enable_classification, false);
    }

    private static boolean getBooleanPref(
            Context context, @StringRes int prefKeyId, boolean defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        return sharedPreferences.getBoolean(prefKey, defaultValue);
    }

    @Nullable
    public static CameraSizePair getUserSpecifiedPreviewSize(Context context) {
        try {
            String previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size);
            String pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return new CameraSizePair(
                    Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                    Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveStringPreference(
            Context context, @StringRes int prefKeyId, @Nullable String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(prefKeyId), value)
                .apply();
    }


    public static int getConfirmationTimeMs(Context context) {
        if (isMultipleObjectsMode(context)) {
            return 300;
        } else if (isAutoSearchEnabled(context)) {
            return getIntPref(context, R.string.pref_key_confirmation_time_in_auto_search, 1500);
        } else {
            return getIntPref(context, R.string.pref_key_confirmation_time_in_manual_search, 500);
        }
    }

    private static int getIntPref(Context context, @StringRes int prefKeyId, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        return sharedPreferences.getInt(prefKey, defaultValue);
    }

}
