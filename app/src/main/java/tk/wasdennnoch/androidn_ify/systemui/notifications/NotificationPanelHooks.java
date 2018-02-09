package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSFooter;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSContainerHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationPanelView.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationPanelView.*;

@SuppressLint("StaticFieldLeak")
public class NotificationPanelHooks {

    private static final String TAG = "NotificationPanelHooks";

    private static Field fieldStatusBarState;

    public static final int STATE_KEYGUARD = 1;

    private static boolean mAnimate = true;

    private static int mQsPeekHeight;

    private static ViewGroup mNotificationPanelView;
    private static ViewGroup mQSContainer;
    private static ViewGroup mHeader;
    private static ViewGroup mQSPanel;
    private static QSFooter mQsFooter;
    private static ExpandableIndicator mExpandIndicator;
    private static QSCustomizer mQsCustomizer;
    private static QSContainerHelper mQsContainerHelper;

    private static final List<BarStateCallback> mBarStateCallbacks = new ArrayList<>();

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            Context context = mNotificationPanelView.getContext();

//            mNotificationPanelView.setClipChildren(false);
//            mNotificationPanelView.setClipToPadding(false);
            mHeader = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mHeader");
            mQSContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQSPanel = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsPanel");
            mQsFooter = mQSContainer.findViewById(R.id.qs_footer);
            mHeader.setOnClickListener(null);
            mExpandIndicator = mQsFooter.findViewById(R.id.statusbar_header_expand_indicator);
            mExpandIndicator.setOnClickListener(mExpandIndicatorListener);

            /*if (!ConfigUtils.qs().keep_qs_panel_background) {
                View mQSContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mQSContainer");
                try {
                    //noinspection deprecation
                    mQSContainer.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't change QS container background color", t);
                }
            }*/

            FrameLayout.LayoutParams qsCustomizerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            qsCustomizerLp.gravity = Gravity.CENTER_HORIZONTAL;
            QSCustomizer qsCustomizer = new QSCustomizer(context);
            qsCustomizer.setElevation(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.qs_container_elevation));
            mNotificationPanelView.addView(qsCustomizer, qsCustomizerLp);

            mQsCustomizer = qsCustomizer;

            if (ConfigUtils.qs().fix_header_space) {
                mQsContainerHelper = new QSContainerHelper(mNotificationPanelView, mQSContainer, mHeader,
                        mQSPanel, mQsFooter);

                mNotificationPanelView.requestLayout();
            }
        }
    };

    private static final XC_MethodHook setHeightOverrideHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int height = (int) param.args[0];
            height = height - mQsContainerHelper.getGutterHeight();
            param.args[0] = height;
        }
    };

    private static final XC_MethodHook setBarStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "setBarStateHook: Setting state to " + (int) param.args[0]);
            //StatusBarHeaderHooks.onSetBarState((int) param.args[0]);
            for (BarStateCallback callback : mBarStateCallbacks) {
                callback.onStateChanged();
            }
            setKeyguardShowing(getBoolean(mKeyguardShowing, param.thisObject));
        }
    };

    private static XC_MethodHook setVerticalPanelTranslationHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mQsCustomizer != null)
                mQsCustomizer.setTranslationX((float) param.args[0]);
        }
    };

    private static final View.OnClickListener mExpandIndicatorListener = new SafeOnClickListener() {
        @Override
        public void onClickSafe(View v) {
            // Fixes an issue with the indicator having two backgrounds when layer type is hardware
            mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
            flingSettings(!mExpandIndicator.isExpanded());
        }
    };

    private static Runnable mRunAfterInstantCollapse;
    private static Runnable mInstantCollapseRunnable = new Runnable() {
        @Override
        public void run() {
            instantCollapse();
            if (mRunAfterInstantCollapse != null)
                mNotificationPanelView.post(mRunAfterInstantCollapse);
        }
    };

    private static void setKeyguardShowing(boolean keyguardShowing) {
        XposedHook.logD(TAG, "setKeyguardShowing " + keyguardShowing);
        //QSContainerHelper.setKeyguardShowing(keyguardShowing);//FIXME this screws things up

        if (StatusBarHeaderHooks.getQsAnimator() != null) {
            StatusBarHeaderHooks.getQsAnimator().setOnKeyguard(keyguardShowing);
        }

        mQsFooter.setKeyguardShowing(keyguardShowing);
        invoke(updateQsState, mNotificationPanelView);
    }

    public static void expandWithQs() {
        try {
            if (ConfigUtils.M) {
                XposedHelpers.callMethod(mNotificationPanelView, "expandWithQs");
            } else {
                XposedHelpers.callMethod(mNotificationPanelView, "expand");
            }
        } catch (Throwable ignore) {

        }
    }

    public static boolean isExpanded() {
        return (mExpandIndicator != null && mExpandIndicator.isExpanded());
    }

    public static boolean isCollapsed() {
        return (mExpandIndicator != null && !mExpandIndicator.isExpanded());
    }

    public static void expandIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (!mExpandIndicator.isExpanded()) {
                flingSettings(true);
            }
        }
    }

    public static void collapseIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (mExpandIndicator.isExpanded()) {
                flingSettings(false);
            }
        }
    }

    private static void flingSettings(boolean expanded) {
        try {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", new Class[]{float.class, boolean.class, Runnable.class, boolean.class}, 0, expanded, null, true);
        } catch (Throwable t) {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, expanded);
        }
    }

    public static boolean isOnKeyguard() {
        return getStatusBarState() == NotificationPanelHooks.STATE_KEYGUARD;
    }

    public static int getStatusBarState() {
        if (mNotificationPanelView == null) {
            return 0;
        }
        int state = 0;
        state = getInt(fieldStatusBarState, mNotificationPanelView);
        return state;
    }

    public static void setNoVisibleNotifications(boolean noNotifications) {
        if (mQsContainerHelper != null) {
            mQsContainerHelper.setGutterEnabled(!noNotifications);
        }
    }

    public static QSContainerHelper getQsContainerHelper() {
        return mQsContainerHelper;
    }

    public static void hook() {
        try {
            if (ConfigUtils.M) {
                try {
                    XposedBridge.hookAllMethods(Classes.SystemUI.PanelView, "expand", instantExpand);
                    XposedBridge.hookAllMethods(Classes.SystemUI.PanelView, "instantExpand", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            mAnimate = false;
                        }
                    });
                    XposedBridge.hookAllMethods(Classes.SystemUI.PanelView, "instantExpand", instantExpand);
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Error in PanelView hooks", t);
                }
            }

            if (ConfigUtils.qs().header) { // Although this is the notification panel everything here is header-related (mainly QS editor)

                fieldStatusBarState = XposedHelpers.findField(Classes.SystemUI.NotificationPanelView, "mStatusBarState");

                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "setBarState", int.class, boolean.class, boolean.class, setBarStateHook);
                XposedHelpers.findAndHookMethod(Classes.SystemUI.QSContainer, "setHeightOverride", int.class, setHeightOverrideHook);
                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "loadDimens", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mQsPeekHeight = XposedHelpers.getIntField(param.thisObject, "mQsPeekHeight");
                    }
                });
                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "setQsExpansionEnabled", boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        boolean qsExpansionEnabled = (boolean) param.args[0];
                        XposedHelpers.setBooleanField(mNotificationPanelView, "mQsExpansionEnabled", qsExpansionEnabled);
                        mQsFooter.getExpandView().setClickable(qsExpansionEnabled);
                        mHeader.setClickable(false);
                        return null;
                    }
                });

                if (ConfigUtils.M)
                    XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslationHook);

                XposedHelpers.findAndHookMethod(Classes.SystemUI.PanelView, "schedulePeek", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(mNotificationPanelView, "setListening", true);
                    }
                });

                XC_MethodHook returnIfCustomizing = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mQsCustomizer != null && mQsCustomizer.isCustomizing())
                            param.setResult(false);
                    }
                };

                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "onInterceptTouchEvent", MotionEvent.class, returnIfCustomizing);
                XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "onTouchEvent", MotionEvent.class, returnIfCustomizing);

                if (ConfigUtils.qs().fix_header_space) {
                    XC_MethodReplacement updateQsTranslation = new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.setQsExpansion((float) invoke(getQsExpansionFraction, param.thisObject),
                                        (float) invoke(getHeaderTranslation,param.thisObject));
                            return null;
                        }
                    };

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "setQsTranslation", float.class, updateQsTranslation);

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.QSContainer, "getDesiredHeight", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                return mQsContainerHelper.getDesiredHeight();
                            return 0;
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.QSContainer, "updateBottom", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.updateBottom();
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "isQsDetailShowing", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                return mQsContainerHelper.getQSDetail().isShowingDetail();
                            return null;
                        }
                    });

                    hookOnLayout();

                    if (ConfigUtils.M) {
                        XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "animateHeaderSlidingIn", new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                if (mQsContainerHelper != null)
                                    mQsContainerHelper.animateHeaderSlidingIn();
                                return null;
                            }
                        });

                        XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "animateHeaderSlidingOut", new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                if (mQsContainerHelper != null)
                                    mQsContainerHelper.animateHeaderSlidingOut();
                                return null;
                            }
                        });
                    }

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "updateHeaderShade", updateQsTranslation);
                }
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    private static final XC_MethodReplacement instantExpand = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final FrameLayout panelView = (FrameLayout) param.thisObject;
            final Object statusBar = get(mStatusBar, panelView);

            Method cancelPeek = XposedHelpers.findMethodBestMatch(Classes.SystemUI.PanelView, "cancelPeek");
            final Method notifyExpandingStarted = XposedHelpers.findMethodBestMatch(Classes.SystemUI.PanelView, "notifyExpandingStarted");
            final Method fling = XposedHelpers.findMethodBestMatch(Classes.SystemUI.PanelView, "fling", int.class, boolean.class);
            boolean isFullyCollapsed = (boolean) XposedHelpers.callMethod(panelView, "isFullyCollapsed");
            boolean isCollapsing = (boolean) XposedHelpers.callMethod(panelView, "isCollapsing");

            if (!isFullyCollapsed && !isCollapsing) {
                return null;
            }
            XposedHelpers.setBooleanField(panelView, "mInstantExpanding", true);
            set(mUpdateFlingOnLayout, panelView, false);
            invoke(abortAnimations, panelView);
            invoke(cancelPeek, panelView);
            if (XposedHelpers.getBooleanField(panelView, "mTracking")) {
                XposedHelpers.callMethod(panelView, "onTrackingStopped", true /* expands */); // The panel is expanded after this call.
            }
            if (XposedHelpers.getBooleanField(panelView, "mExpanding")) {
                XposedHelpers.callMethod(panelView, "notifyExpandingFinished");
            }
            XposedHelpers.callMethod(panelView, "notifyBarPanelExpansionChanged");

            // Wait for window manager to pickup the change, so we know the maximum height of the panel
            // then.
            panelView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout(){
                            try {
                                if (!XposedHelpers.getBooleanField(panelView, "mInstantExpanding")) {
                                    panelView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    return;
                                }
                                View statusBarWindow = (View) XposedHelpers.callMethod(statusBar, "getStatusBarWindow");
                                if (statusBarWindow.getHeight()
                                        != (int) XposedHelpers.callMethod(statusBar, "getStatusBarHeight")) {
                                    panelView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    if (mAnimate) {
                                        invoke(notifyExpandingStarted, panelView);
                                        invoke(fling, panelView, 0, true /* expand */);
                                    } else {
                                        XposedHelpers.callMethod(panelView, "setExpandedFraction", 1f);
                                    }
                                    XposedHelpers.setBooleanField(panelView, "mInstantExpanding", false);
                                }
                            } catch (Throwable ignore) {}
                        }
                    });

            // Make sure a layout really happens.
            panelView.requestLayout();
            mAnimate = true;
            return null;
        }
    };

    private static void hookOnLayout() {
        XposedHelpers.findAndHookMethod(Classes.SystemUI.NotificationPanelView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                if (mQsContainerHelper != null)
                    mQsContainerHelper.notificationPanelViewOnLayout(param);
                return null;
            }
        });
    }

    public static void postInstantCollapse(Runnable after) {
        mRunAfterInstantCollapse = after;
        mNotificationPanelView.post(mInstantCollapseRunnable);
    }

    private static void instantCollapse() {
        XposedHelpers.callMethod(mNotificationPanelView, "instantCollapse");
    }

    public static void addBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.add(callback);
    }

    public static void removeBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.remove(callback);
    }

    static QSCustomizer getQsCustomizer() {
        return mQsCustomizer;
    }

    public interface BarStateCallback {
        void onStateChanged();
    }

    public static void invalidateTileAdapter() {
        if (mQsCustomizer != null)
            mQsCustomizer.invalidateTileAdapter();
    }

    public static void handleStateChanged(Object qsTile, Object state) {
        if (mQsCustomizer != null)
            mQsCustomizer.handleStateChanged(qsTile, state);
    }

    public static void showQsCustomizer(ArrayList<Object> records, boolean animated) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, animated);
    }

    public static void showQsCustomizer(ArrayList<Object> records, int x, int y) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, x, y);
    }

    private static boolean canShowCustomizer(ArrayList<Object> records) {
        if (records == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mRecords == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mRecords == null", null);
            return false;
        }
        if (mQsCustomizer == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mQsCustomizer == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mQsCustomizer == null", null);
            return false;
        }
        return true;
    }
}
