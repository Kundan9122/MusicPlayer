package com.firekernel.musicplayer.utils;

import android.app.Activity;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Ashish on 5/18/2017.
 * Utility methods will go here
 */

public class FireUtils {
    private static long timestamp;

    public static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static void preventDoubleClick() {
        if (System.currentTimeMillis() - timestamp < 500) {
            return;
        }
        timestamp = System.currentTimeMillis();
    }

    public static String getFormattedDuration(int duration) {
        int minutes = duration / (60 * 1000);
        int seconds = duration % (60 * 1000);
        return minutes + ":" + (seconds + "").substring(0, 2);
    }

    public static void postponeTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.postponeEnterTransition();
        } else {
            ActivityCompat.postponeEnterTransition(activity);
        }
    }

    public static void startPostponedTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startPostponedEnterTransition();
        } else {
            ActivityCompat.startPostponedEnterTransition(activity);
        }
    }
}
