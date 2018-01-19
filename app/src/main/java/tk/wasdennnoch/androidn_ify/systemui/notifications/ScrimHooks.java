package tk.wasdennnoch.androidn_ify.systemui.notifications;


import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;

public class ScrimHooks {

    private static final String TAG = "ScrimHooks";

    public static void hook() {
        if (!ConfigUtils.notifications().enable_notifications_background)
            return;
        try {
            XposedHelpers.findAndHookMethod(ScrimView, "onDraw", Canvas.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    ScrimHelper helper = ScrimHelper.getInstance(param.thisObject);
                    helper.onDraw((Canvas) param.args[0]);
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(ScrimView, "setDrawAsSrc", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ScrimHelper helper = ScrimHelper.getInstance(param.thisObject);
                    helper.setDrawAsSrc((boolean) param.args[0]);
                }
            });

            XposedHelpers.findAndHookMethod(ScrimView, "setScrimColor", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    int color = (int) param.args[0];
                    View scrimView = (View) param.thisObject;
                    ScrimHelper helper = ScrimHelper.getInstance(scrimView);
                    if (color != XposedHelpers.getIntField(scrimView, "mScrimColor")) {
                        XposedHelpers.setBooleanField(scrimView, "mIsEmpty", Color.alpha(color) == 0);
                        XposedHelpers.setIntField(scrimView, "mScrimColor", color);
                        scrimView.invalidate();
                        if (helper.mChangeRunnable != null) {
                            helper.mChangeRunnable.run();
                        }
                    }
                    return null;
                }
            });

            XposedBridge.hookAllMethods(ScrimController, "updateScrimBehindDrawingMode", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View mBackDropView = (View) XposedHelpers.getObjectField(param.thisObject, "mBackDropView");
                    boolean asSrc = mBackDropView.getVisibility() != View.VISIBLE && XposedHelpers.getBooleanField(param.thisObject, "mScrimSrcEnabled");
                    NotificationStackScrollLayoutHooks.setDrawBackgroundAsSrc(asSrc);
                }
            });

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error", t);
        }
    }
}

