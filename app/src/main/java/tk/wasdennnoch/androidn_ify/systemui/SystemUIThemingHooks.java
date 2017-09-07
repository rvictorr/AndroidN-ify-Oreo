package tk.wasdennnoch.androidn_ify.systemui;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;

public class SystemUIThemingHooks {

    private static final String TAG = "SystemUIThemingHooks";

    private static final String CLASS_SYSTEMUI_APPLICATION = "com.android.systemui.SystemUIApplication";

    public static void hook(ClassLoader classLoader) {

        XposedHelpers.findAndHookMethod(CLASS_SYSTEMUI_APPLICATION, classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Application app = (Application) param.thisObject;

                //app.setTheme(android.R.style.Theme_DeviceDefault_Light);
                //int theme = ResourceUtils.getInstance(app.getApplicationContext()).getResources().getIdentifier("Theme.DeviceDefault.QuickSettings", "style", XposedHook.PACKAGE_OWN);
                //app.setTheme(theme); //TODO finish theming
            }
        });

        XposedHelpers.findAndHookMethod(QSTile.CLASS_RESOURCE_ICON, classLoader, "getDrawable", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Drawable icon = (Drawable) param.getResult();
                icon.setTint(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimaryInverse));
                icon.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        });
        XposedHelpers.findAndHookMethod(QSTile.CLASS_ANIMATION_ICON, classLoader, "getDrawable", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Drawable icon = (Drawable) param.getResult();
                icon.setTint(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimaryInverse));
                icon.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        });
    }
}
