package tk.wasdennnoch.androidn_ify.systemui;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.android.pm.PackageManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SystemUIThemingHooks {

    private static final String TAG = "SystemUIThemingHooks";

    //private static Application app;
    private static final String CLASS_SYSTEMUI_APPLICATION = "com.android.systemui.SystemUIApplication";
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    //private static final Context mContext;

    public static void hook(final ClassLoader classLoader) {

        if (!ConfigUtils.qs().enable_theming)
            return;

        XposedHelpers.findAndHookMethod(CLASS_SYSTEMUI_APPLICATION, classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Application app = (Application) param.thisObject;
                Context context = app.getApplicationContext();

                //app.setTheme(android.R.style.Theme_Material_Light);
                Context themeContext = ResourceUtils.createOwnContext(context);

                android.content.pm.PackageManager pm = app.getPackageManager();
                Resources res = pm.getResourcesForApplication(XposedHook.PACKAGE_OWN);

                themeContext.setTheme(res.getIdentifier("LightSystemUITheme", "style", XposedHook.PACKAGE_OWN));
                app.getTheme().setTo(themeContext.getTheme());
            }
        });

        XposedHelpers.findAndHookMethod(QSTile.CLASS_RESOURCE_ICON, classLoader, "getDrawable", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Drawable icon = (Drawable) param.getResult();
                icon.setTint(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
                icon.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        });
        XposedHelpers.findAndHookMethod(QSTile.CLASS_ANIMATION_ICON, classLoader, "getDrawable", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Drawable icon = (Drawable) param.getResult();
                icon.setTint(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
                icon.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        });
    }

    public static void hookRes(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        if (!ConfigUtils.qs().enable_theming)
            return;

        XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
        resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_detail_item", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ViewGroup layout = (ViewGroup) liparam.view;
                Context context = layout.getContext();
                ImageView icon = layout.findViewById(android.R.id.icon);
                ImageView icon2 = layout.findViewById(android.R.id.icon2);
                TextView title = layout.findViewById(android.R.id.title);
                icon.setColorFilter(ColorUtils.getColorAttr(layout.getContext(), android.R.attr.textColorPrimary));
                icon2.setColorFilter(ColorUtils.getColorAttr(layout.getContext(), android.R.attr.textColorPrimary));
                title.setTextColor(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
            }
        });

        resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "volume_dialog_row", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ViewGroup layout = (ViewGroup) liparam.view;
                Context context = layout.getContext();
            }
        });

        resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "volume_dialog", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ViewGroup layout = (ViewGroup) liparam.view;
                Context context = layout.getContext();
            }
        });

        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "system_primary_color", modRes.fwd(R.color.primary_material_settings_light));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_brightness_thumb", modRes.fwd(R.drawable.ic_brightness_thumb));

        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_alarm", modRes.fwd(R.drawable.ic_volume_alarm));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_alarm_mute", modRes.fwd(R.drawable.ic_volume_alarm_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_bt_sco", modRes.fwd(R.drawable.ic_volume_bt_sco));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_media", modRes.fwd(R.drawable.ic_volume_media));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_media_bt", modRes.fwd(R.drawable.ic_volume_media_bt));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_media_bt_mute", modRes.fwd(R.drawable.ic_volume_media_bt_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_media_mute", modRes.fwd(R.drawable.ic_volume_media_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_remote", modRes.fwd(R.drawable.ic_volume_remote));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_remote_mute", modRes.fwd(R.drawable.ic_volume_remote_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_ringer", modRes.fwd(R.drawable.ic_volume_ringer));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_ringer_mute", modRes.fwd(R.drawable.ic_volume_ringer_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_ringer_vibrate", modRes.fwd(R.drawable.ic_volume_ringer_vibrate));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_system", modRes.fwd(R.drawable.ic_volume_system));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_system_mute", modRes.fwd(R.drawable.ic_volume_system_mute));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_expand", modRes.fwd(R.drawable.ic_volume_expand));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_collapse", modRes.fwd(R.drawable.ic_volume_collapse));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_expand_animation", modRes.fwd(R.drawable.ic_volume_expand_animation));
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "ic_volume_collapse_animation", modRes.fwd(R.drawable.ic_volume_collapse_animation));

        //resparam.res.setReplacement(XposedHook.PACKAGE_SYSTEMUI, "color", "qs_text", ColorUtils.getColorAttr(AndroidAppHelper.currentApplication(), android.R.attr.textColorPrimary));
    }
}
