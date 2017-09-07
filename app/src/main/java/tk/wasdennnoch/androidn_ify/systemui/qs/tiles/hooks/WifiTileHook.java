package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Intent;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class WifiTileHook extends QSTileHook {

    private static final String CLASS_WIFI_TILE = "com.android.systemui.qs.tiles.WifiTile";

    private Object mController;

    public WifiTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_WIFI_TILE);
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        mController = getObjectField("mController");
    }

    @Override
    public void handleClick() {
        Object mState = getState();
        boolean enabled = XposedHelpers.getBooleanField(mState, "enabled");
        XposedHelpers.callMethod(mState, "copyTo", getObjectField("mStateBeforeClick"));
        if (ConfigUtils.M) {
            MetricsLogger.action(mContext, MetricsLogger.QS_WIFI, !enabled);
        }
        XposedHelpers.callMethod(mController, "setWifiEnabled", !enabled);
    }

    @Override
    protected Intent getSettingsIntent() {
        return new Intent(Settings.ACTION_WIFI_SETTINGS);
    }

}
