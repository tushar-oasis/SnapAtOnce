package com.oasis.snapatonce;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by tushar on 17-03-2017.
 */

public class Utils {
    public static int getDeviceHeightInPx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    public static int getDeviceWidthInPx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }
}
