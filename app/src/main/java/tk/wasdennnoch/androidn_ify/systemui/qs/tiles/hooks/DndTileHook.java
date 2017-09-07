package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;


import android.content.Intent;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;


public class DndTileHook extends QSTileHook {

    private static final String CLASS_DND_TILE = "com.android.systemui.qs.tiles.DndTile";
    private static final String TAG = "QSTile.DndTile";
    private static final int ZEN_MODE_OFF = 0;
    private static final int ZEN_MODE_ALARMS = 3;

    private static Class mSysUiToastClass;
    private static Class mPrefsClass;

    private Object mController;

    public DndTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_DND_TILE);
        mSysUiToastClass = XposedHelpers.findClass("com.android.systemui.SysUIToast", classLoader);
        mPrefsClass = XposedHelpers.findClass("com.android.systemui.Prefs", classLoader);
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        mController = getObjectField("mController");
    }

    @Override
    protected Intent getSettingsIntent() {
        return (Intent) XposedHelpers.getObjectField(mThisObject, "ZEN_SETTINGS");
    }

    @Override
    protected void handleClick() {
        Object mState = getState();
        if (XposedHelpers.getBooleanField(mState, "value")) {
            XposedHelpers.callMethod(mController, "setZen", ZEN_MODE_OFF, null, TAG);
        } else {
            int zen = (int) XposedHelpers.callStaticMethod(mPrefsClass, "getInt", mContext, "DndFavoriteZen", ZEN_MODE_ALARMS);
            XposedHelpers.callMethod(mController, "setZen", zen, null, TAG);
        }
    }

    @Override
    protected void handleSecondaryClick() {
        Object mHost = XposedHelpers.getObjectField(mThisObject, "mHost");
        if ((boolean) XposedHelpers.callMethod(mController, "isVolumeRestricted")) {
            // Collapse the panels, so the user can see the toast.
            XposedHelpers.callMethod(mHost, "collapsePanels");
            ((Toast) XposedHelpers.callStaticMethod(mSysUiToastClass, "makeText", mContext, mContext.getString(
                    com.android.internal.R.string.error_message_change_not_allowed),
                    Toast.LENGTH_LONG)).show();
            return;
        }
        showDetail(true);
    }
}
