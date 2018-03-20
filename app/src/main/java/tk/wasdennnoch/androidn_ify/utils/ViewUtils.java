package tk.wasdennnoch.androidn_ify.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.widget.TextView;

import java.util.Locale;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;

import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;

@SuppressWarnings("SameParameterValue")
public class ViewUtils {

    public static final float LARGE_TEXT_SCALE = 1.3f;

    public static int dpToPx(Resources res, int dp) {
        return (int) (res.getDisplayMetrics().density * dp);
    }

    public static void setHeight(View view, int height) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    public static void setWidth(View view, int width) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.width = width;
        view.setLayoutParams(layoutParams);
    }

    public static void setMarginStart(View view, int margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(margin);
        view.setLayoutParams(lp);
    }

    public static void setMarginEnd(View view, int margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.setMarginEnd(margin);
        view.setLayoutParams(lp);
    }

    public static void setMarginBottom(View view, int margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.bottomMargin = margin;
        view.setLayoutParams(lp);
    }

    public static void updateFontSize(TextView v, int dimensId) {
        if (v != null) {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    v.getResources().getDimensionPixelSize(dimensId));
        }
    }


    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void applyTheme(Activity activity, SharedPreferences prefs) {
        switch (prefs.getString("app_theme", "light")) {
            case "device":
                activity.setTheme(R.style.DeviceDefault);
                break;
            case "dark":
                activity.setTheme(R.style.DarkTheme);
            case "light":
                int colorPrimary = prefs.getInt("theme_colorPrimary", activity.getResources().getColor(R.color.colorPrimary));
                float[] hsv = new float[3];
                Color.colorToHSV(colorPrimary, hsv);
                hsv[2] *= 0.8f;
                int colorPrimaryDark = Color.HSVToColor(hsv);
                activity.getWindow().setStatusBarColor(colorPrimaryDark);
                activity.getActionBar().setBackgroundDrawable(new ColorDrawable(colorPrimary));
        }

        if (prefs.getBoolean("force_english", false)) {
            Configuration config = activity.getResources().getConfiguration();
            config.locale = Locale.ENGLISH;
            activity.getResources().updateConfiguration(config, null);
        }
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void applyTheme(Dialog dialog, Context context, SharedPreferences prefs) {
        if (prefs.getString("app_theme", "light").equals("device")) return;
        int colorPrimary = prefs.getInt("theme_colorPrimary", context.getResources().getColor(R.color.colorPrimary));
        float[] hsv = new float[3];
        Color.colorToHSV(colorPrimary, hsv);
        hsv[2] *= 0.8f;
        int colorPrimaryDark = Color.HSVToColor(hsv);
        dialog.getWindow().setStatusBarColor(colorPrimaryDark);
        try {
            dialog.getActionBar().setBackgroundDrawable(new ColorDrawable(colorPrimary));
        } catch (NullPointerException ignore) {
        }
    }

    public static void getRelativePosition(int[] loc1, View view, View parent) {
        loc1[0] = view.getWidth() / 2;
        loc1[1] = 0;
        getRelativePositionInt(loc1, view, parent);
    }

    private static void getRelativePositionInt(int[] loc1, View view, View parent) {
        if (view == parent || view == null) return;
        loc1[0] += view.getLeft();
        loc1[1] += view.getTop();
        if (!(view.getParent() instanceof ViewRootImpl))
            getRelativePositionInt(loc1, (View) view.getParent(), parent);
    }

    public static boolean isTemporarilyDetached(View view) {
        int mPrivateFlags3 = getInt(Fields.Android.View.mPrivateFlags3, view);
//        XposedHook.logI(ViewUtils.class.getSimpleName(), "isTemporarilyDetached called, result: " + ((mPrivateFlags3 & 0x2000000) != 0));
        return (mPrivateFlags3 & 0x2000000/* PFLAG3_TEMPORARY_DETACH*/) != 0;
    }

    public static boolean sameAs(Icon thisIcon, Icon otherIcon) {
        if (otherIcon == thisIcon) {
            return true;
        }
        if (get(Fields.Android.Icon.mType, thisIcon) != get(Fields.Android.Icon.mType, otherIcon)) {
            return false;
        }
        switch (getInt(Fields.Android.Icon.mType, thisIcon)) {
            case 1 /*TYPE_BITMAP*/:
                return invoke(Methods.Android.Icon.getBitmap, thisIcon) == invoke(Methods.Android.Icon.getBitmap, otherIcon);
            case 3 /*TYPE_DATA*/:
                return invoke(Methods.Android.Icon.getDataLength, thisIcon) == invoke(Methods.Android.Icon.getDataLength, otherIcon)
                        && invoke(Methods.Android.Icon.getDataOffset, thisIcon) == invoke(Methods.Android.Icon.getDataOffset, otherIcon)
                        && invoke(Methods.Android.Icon.getDataBytes, thisIcon) == invoke(Methods.Android.Icon.getDataBytes, otherIcon);
            case 2 /*TYPE_RESOURCE*/:
                return invoke(Methods.Android.Icon.getResId, thisIcon) == invoke(Methods.Android.Icon.getResId, otherIcon)
                        && Objects.equals(invoke(Methods.Android.Icon.getResPackage, thisIcon), invoke(Methods.Android.Icon.getResPackage, otherIcon));
            case 4 /*TYPE_URI*/:
                return Objects.equals(invoke(Methods.Android.Icon.getUriString, thisIcon), invoke(Methods.Android.Icon.getUriString, otherIcon));
        }
        return false;
    }
}