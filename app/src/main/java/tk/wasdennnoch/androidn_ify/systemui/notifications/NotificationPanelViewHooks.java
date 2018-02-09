package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSFooter;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Methods;

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.getStatusBarState;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationPanelView.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.QSContainer.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationPanelView.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationStackScrollLayout.*;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;

@SuppressLint("StaticFieldLeak")
public class NotificationPanelViewHooks {

    private static final String TAG = "NotificationPanelViewHooks";

    private static final int STATE_SHADE = 0;
    private static final int STATE_KEYGUARD = 1;
    private static final int STATE_SHADE_LOCKED = 2;

    private static ViewGroup mNotificationPanelView;
    private static ViewGroup mScrollView;
    private static ViewGroup mQsContainer;
    private static QSFooter mQsFooter;
    private static ViewGroup mHeader;
    private static ViewGroup mQSPanel;
    private static ViewGroup mStackScroller;

    private static Method methodCancelQsAnimation;
    private static Method methodCancelHeightAnimator;
    private static Method methodSetQsExpansion;
    private static Method methodOnQsExpansionStarted;
    private static Method methodSetQsExpanded;
    private static Method methodGetMaxPanelHeight;

    private static Method methodGetNotGoneChildCount;
    private static Method methodGetFirstChildNotGone;

    private static Method methodHasPinnedHeadsUp;
    private static Method methodGetTopHeadsUpHeight;
    private static Method methodGetIntrinsicHeight;
    private static Method methodGetPositionInLinearLayout;
    private static Method methodClampScrollPosition;
    private static Method methodIsScrolledToBottom;

    private static Field fieldQsExpandImmediate;
    private static Field fieldQsExpandedWhenExpandingStarted;
    private static Field fieldStackScrollerOverscrolling;
    private static Field fieldHeaderAnimating;
    private static Field fieldQsExpansionFromOverscroll;
    private static Field fieldQsScrimEnabled;
    private static Field fieldIsExpanding;
    private static Field fieldExpandedHeight;
    private static Field fieldTopPaddingOverflow;
    private static Field fieldTopPadding;
    private static Field fieldTopPaddingAdjustment;
    private static Field fieldTrackingHeadsUp;
    private static Field fieldHeadsUpManager;
    private static Field fieldQsExpansionEnabled;
    private static Field fieldHeader;
    private static Field fieldClockPositionResult;
    private static Field fieldScrollYOverride;

    private static Field fieldBottomStackSlowDownHeight;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldMaxLayoutHeight;
    private static Field fieldIntrinsicPadding;
    private static Field fieldPaddingBetweenElements;
    private static Field fieldOwnScrollY;
    private static Field fieldCollapsedSize;
    private static Field fieldInterceptDelegateEnabled;
    private static Field fieldOnlyScrollingInThisMotion;
    private static Field fieldDelegateToScrollView;
    private static Field fieldChildrenToAddAnimated;


    public static void hook() {

        try {

            fieldBottomStackSlowDownHeight = XposedHelpers.findField(NotificationStackScrollLayout, "mBottomStackSlowDownHeight");
            fieldBottomStackPeekSize = XposedHelpers.findField(NotificationStackScrollLayout, "mBottomStackPeekSize");
            fieldMaxLayoutHeight = XposedHelpers.findField(NotificationStackScrollLayout, "mMaxLayoutHeight");
            fieldIntrinsicPadding = XposedHelpers.findField(NotificationStackScrollLayout, "mIntrinsicPadding");
            fieldOwnScrollY = XposedHelpers.findField(NotificationStackScrollLayout, "mOwnScrollY");
            fieldCollapsedSize = XposedHelpers.findField(NotificationStackScrollLayout, "mCollapsedSize");
            fieldHeader = XposedHelpers.findField(NotificationPanelView, "mHeader");

            methodGetFirstChildNotGone = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getFirstChildNotGone");

            XposedHelpers.findAndHookMethod(NotificationPanelView, "onFinishInflate", onFinishInflateHook);
            
            if (ConfigUtils.qs().reconfigure_notification_panel) {

                methodCancelQsAnimation = XposedHelpers.findMethodBestMatch(NotificationPanelView, "cancelQsAnimation");
                methodCancelHeightAnimator = XposedHelpers.findMethodBestMatch(NotificationPanelView, "cancelHeightAnimator");
                methodSetQsExpansion = XposedHelpers.findMethodBestMatch(NotificationPanelView, "setQsExpansion", float.class);
                methodOnQsExpansionStarted = XposedHelpers.findMethodBestMatch(NotificationPanelView, "onQsExpansionStarted");
                methodSetQsExpanded = XposedHelpers.findMethodBestMatch(NotificationPanelView, "setQsExpanded", boolean.class);
                methodGetMaxPanelHeight = XposedHelpers.findMethodBestMatch(NotificationPanelView, "getMaxPanelHeight");

                methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getIntrinsicHeight", View.class);
                methodGetPositionInLinearLayout = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getPositionInLinearLayout", View.class);
                methodClampScrollPosition = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "clampScrollPosition");
                methodGetNotGoneChildCount = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getNotGoneChildCount");
                methodIsScrolledToBottom = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "isScrolledToBottom");

                methodHasPinnedHeadsUp = XposedHelpers.findMethodBestMatch(HeadsUpManager, "hasPinnedHeadsUp");
                methodGetTopHeadsUpHeight = XposedHelpers.findMethodBestMatch(HeadsUpManager, "getTopHeadsUpHeight");
                
                fieldQsExpandImmediate = XposedHelpers.findField(NotificationPanelView, "mQsExpandImmediate");
                fieldQsExpandedWhenExpandingStarted = XposedHelpers.findField(NotificationPanelView, "mQsExpandedWhenExpandingStarted");
                fieldStackScrollerOverscrolling = XposedHelpers.findField(NotificationPanelView, "mStackScrollerOverscrolling");
                fieldHeaderAnimating = XposedHelpers.findField(NotificationPanelView, "mHeaderAnimating");
                fieldQsExpansionFromOverscroll = XposedHelpers.findField(NotificationPanelView, "mQsExpansionFromOverscroll");
                fieldQsScrimEnabled = XposedHelpers.findField(NotificationPanelView, "mQsScrimEnabled");
                fieldIsExpanding = XposedHelpers.findField(NotificationPanelView, "mIsExpanding");
                fieldExpandedHeight = XposedHelpers.findField(NotificationPanelView, "mExpandedHeight");
                fieldTopPaddingAdjustment = XposedHelpers.findField(NotificationPanelView, "mTopPaddingAdjustment");
                fieldQsExpansionEnabled = XposedHelpers.findField(NotificationPanelView, "mQsExpansionEnabled");
                fieldClockPositionResult = XposedHelpers.findField(NotificationPanelView, "mClockPositionResult");
                fieldScrollYOverride = XposedHelpers.findField(NotificationPanelView, "mScrollYOverride");

                fieldTopPaddingOverflow = XposedHelpers.findField(NotificationStackScrollLayout, "mTopPaddingOverflow");
                fieldTopPadding = XposedHelpers.findField(NotificationStackScrollLayout, "mTopPadding");
                fieldTrackingHeadsUp = XposedHelpers.findField(NotificationStackScrollLayout, "mTrackingHeadsUp");
                fieldHeadsUpManager = XposedHelpers.findField(NotificationStackScrollLayout, "mHeadsUpManager");
                fieldPaddingBetweenElements = XposedHelpers.findField(NotificationStackScrollLayout, "mPaddingBetweenElements");
                fieldInterceptDelegateEnabled = XposedHelpers.findField(NotificationStackScrollLayout, "mInterceptDelegateEnabled");
                fieldOnlyScrollingInThisMotion = XposedHelpers.findField(NotificationStackScrollLayout, "mOnlyScrollingInThisMotion");
                fieldDelegateToScrollView = XposedHelpers.findField(NotificationStackScrollLayout, "mDelegateToScrollView");
                fieldChildrenToAddAnimated = XposedHelpers.findField(NotificationStackScrollLayout, "mChildrenToAddAnimated");

                XposedHelpers.findAndHookMethod(NotificationPanelView, "onOverscrolled", float.class, float.class, int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getTempQsMaxExpansion", getTempQsMaxExpansionHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onExpandingStarted", onExpandingStartedHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "setListening", boolean.class, setListening);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onClick", View.class, onClick);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslation);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onQsExpansionStarted", int.class, onQsExpansionStarted);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "updateQsState", updateQsState);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "isScrolledToBottom", isScrolledToBottom);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getHeaderTranslation", XGetHeaderTranslation);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getPeekHeight", getPeekHeightHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getScrollViewScrollY", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onExpandingFinished", onExpandingFinishedHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onScrollChanged", onScrollChanged);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "calculatePanelHeightQsExpanded", XCalculatePanelHeightQsExpanded);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getMaxPanelHeight", getMaxPanelHeight);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getFadeoutAlpha", getFadeoutAlpha);
                /*XposedHelpers.findAndHookMethod(NotificationPanelView, "animateHeaderSlidingIn", XC_MethodReplacement.DO_NOTHING); //TODO make this work
                XposedHelpers.findAndHookMethod(NotificationPanelView, "animateHeaderSlidingOut", XC_MethodReplacement.DO_NOTHING);*/
                XposedHelpers.findAndHookMethod(NotificationPanelView, "updateHeaderShade", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "updateHeader", updateHeader);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "positionClockAndNotifications", positionClockAndNotifications);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "setQsExpansion", float.class, setQsExpansion);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "setQsTranslation", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "notifyVisibleChildrenChanged", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "calculateQsTopPadding", calculateQsTopPaddingHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onQsTouch", MotionEvent.class, onQsTouchHook);
                XposedBridge.hookAllMethods(NotificationPanelView, "flingSettings", flingSettingsHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "shouldQuickSettingsIntercept", float.class, float.class, float.class, shouldQuickSettingsInterceptHook);

                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "setScrollView", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(NotificationStackScrollLayout, "setInterceptDelegateEnabled", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "onTouchEvent", MotionEvent.class, onTouchEventHook);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, onInterceptTouchEventHook);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, onScrollTouchHook);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "getPeekHeight", getPeekHeightStackScroller);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "getEmptyBottomMargin", getEmptyBottomMargin);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "getMinStackHeight", getMinStackHeight);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "getDismissViewHeight", getDismissViewHeight);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "updateChildren", updateChildrenHook);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "updateSpeedBumpIndex", int.class, updateSpeedBumpIndex);
                XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "setStackHeight", float.class, setStackHeight);

                XposedBridge.hookAllMethods(ObservableScrollView, "overScrollBy", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(ObservableScrollView, "fling", int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(ObservableScrollView, "getMaxScrollY", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(ObservableScrollView, "isScrolledToBottom", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(ObservableScrollView, "setBlockFlinging", boolean.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(ObservableScrollView, "onTouchEvent", MotionEvent.class, XC_MethodReplacement.returnConstant(false));

                XposedHelpers.findAndHookMethod(StackScrollAlgorithm, "notifyChildrenChanged", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking NotificationPanelView ", t);
        }
    }

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            mHeader = get(fieldHeader, mNotificationPanelView);
            mScrollView = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView");
            mQsContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQsFooter = mNotificationPanelView.findViewById(R.id.qs_footer);
            mQSPanel = get(mQsPanel, mNotificationPanelView);
            mStackScroller = get(mNotificationStackScroller, mNotificationPanelView);
        }
    };

    private static final XC_MethodReplacement onQsExpansionStarted = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            invoke(methodCancelQsAnimation, mNotificationPanelView);
            invoke(methodCancelHeightAnimator, mNotificationPanelView);
            float height = getFloat(mQsExpansionHeight, mNotificationPanelView) - (int) param.args[0];
            invoke(methodSetQsExpansion, mNotificationPanelView, height);
            invoke(requestPanelHeightUpdate, mNotificationPanelView);
            return null;
        }
    };

    private static final XC_MethodReplacement setVerticalPanelTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float translation = (float) param.args[0];
            mStackScroller.setTranslationX(translation);
            mScrollView.setTranslationX(translation);
            return null;
        }
    };

    private static final XC_MethodHook getTempQsMaxExpansionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.setResult(getInt(mQsMaxExpansionHeight, mNotificationPanelView));
        }
    };

    private static final XC_MethodHook onExpandingStartedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callMethod(mHeader, "setListening", true);
        }
    };

    private static final XC_MethodReplacement setListening = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mKeyguardStatusBar = XposedHelpers.getObjectField(param.thisObject, "mKeyguardStatusBar");
            try {
                XposedHelpers.callMethod(mKeyguardStatusBar, "setListening", param.args[0]);
            } catch (NoSuchMethodError e) { //LOS
                setListening(mKeyguardStatusBar, (boolean) param.args[0]);
            }
            XposedHelpers.callMethod(mQSPanel, "setListening", param.args[0]);
            return null;
        }
    };

    private static final XC_MethodReplacement onClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (((View) param.args[0]).getId() == R.id.statusbar_header_expand_indicator) {
                invoke(methodOnQsExpansionStarted, mNotificationPanelView);
                if (getBoolean(mQsExpanded, mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, false, null, true);
                } else if (getBoolean(fieldQsExpansionEnabled, mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, true, null, true);
                }
            }
            return null;
        }
    };

    private static final XC_MethodHook shouldQuickSettingsInterceptHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            float x = (float) param.args[0];
            float y = (float) param.args[1];
            float yDiff = (float) param.args[2];
            if (!getBoolean(fieldQsExpansionEnabled, mNotificationPanelView) || XposedHelpers.getBooleanField(mNotificationPanelView, "mCollapsedOnDown")) {
                param.setResult(false);
            }
            View header = getBoolean(mKeyguardShowing, mNotificationPanelView)
                    ? (View) XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusBar")
                    : (View) get(fieldHeader, mNotificationPanelView);
            View mScrollView = (View) (XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView"));
            boolean onHeader = x >= mScrollView.getX()
                    && x <= mScrollView.getX() + mScrollView.getWidth()
                    && y >= header.getTop() && y <= header.getBottom();
            if (getBoolean(mQsExpanded, mNotificationPanelView)) {
                param.setResult(onHeader || (yDiff < 0 && (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isInQsArea", x, y)));
            } else {
                param.setResult(onHeader);
            }
        }
    };

    private static final XC_MethodReplacement updateQsState = new XC_MethodReplacement() { //TODO: optimize everything
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean qsExpanded = getBoolean(mQsExpanded, mNotificationPanelView);
            boolean mStackScrollerOverscrolling = getBoolean(fieldStackScrollerOverscrolling, mNotificationPanelView);
            boolean mHeaderAnimating = getBoolean(fieldHeaderAnimating, mNotificationPanelView);
            boolean keyguardShowing = getBoolean(mKeyguardShowing, mNotificationPanelView);
            boolean mQsExpansionFromOverscroll = getBoolean(fieldQsExpansionFromOverscroll, mNotificationPanelView);
            boolean mQsScrimEnabled = getBoolean(fieldQsScrimEnabled, mNotificationPanelView);
            Object mKeyguardUserSwitcher = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardUserSwitcher");
            int mStatusBarState = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
            View mQsNavbarScrim = (View) XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean expandVisually = qsExpanded || mStackScrollerOverscrolling || mHeaderAnimating;
            XposedHelpers.callMethod(mQSPanel, "setExpanded", qsExpanded);
            mHeader.setVisibility((qsExpanded || !keyguardShowing || mHeaderAnimating)
                    ? View.VISIBLE
                    : View.INVISIBLE);
            XposedHelpers.callMethod(mHeader, "setExpanded", ((keyguardShowing && !mHeaderAnimating)
                    || (qsExpanded && !mStackScrollerOverscrolling)));
            mQsFooter.setVisibility((qsExpanded || !keyguardShowing || mHeaderAnimating)
                    ? View.VISIBLE
                    : View.INVISIBLE);
            mQsFooter.setExpanded((keyguardShowing && !mHeaderAnimating)
                    || (qsExpanded && !mStackScrollerOverscrolling));
            mQSPanel.setVisibility((expandVisually ? View.VISIBLE : View.INVISIBLE));
            invoke(setScrollingEnabled, mStackScroller, (
                    mStatusBarState != STATE_KEYGUARD && (!qsExpanded
                            || mQsExpansionFromOverscroll)));
            XposedHelpers.callMethod(mNotificationPanelView, "updateEmptyShadeView");
            mQsNavbarScrim.setVisibility(mStatusBarState == STATE_SHADE && qsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled
                    ? View.VISIBLE
                    : View.INVISIBLE);
            if (mKeyguardUserSwitcher != null && qsExpanded && !mStackScrollerOverscrolling) {
                XposedHelpers.callMethod(mKeyguardUserSwitcher, "hideIfNotSimple", true /* animate */);
            }
            return null;
        }
    };

    private static final XC_MethodHook onTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldDelegateToScrollView, mStackScroller, false);
            set(fieldOnlyScrollingInThisMotion, mStackScroller, false);
        }
    };

    private static final XC_MethodHook onInterceptTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldInterceptDelegateEnabled, mStackScroller, false);
            set(fieldOnlyScrollingInThisMotion, mStackScroller, false);
        }
    };

    private static final XC_MethodReplacement isScrolledToBottom = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object statusBar = get(mStatusBar, mNotificationPanelView);
            int getBarState = (int) XposedHelpers.callMethod(statusBar, "getBarState");
            boolean isScrolledToBottom = invoke(methodIsScrolledToBottom, mStackScroller);
            boolean isInSettings = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isInSettings");
            if (!isInSettings) {
                return (getBarState == STATE_KEYGUARD)
                        || isScrolledToBottom;
            } else {
                return true;
            }
        }
    };

    private static final XC_MethodHook onScrollTouchHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            MotionEvent ev = (MotionEvent) param.args[0];
            boolean mIsBeingDragged = XposedHelpers.getBooleanField(mStackScroller, "mIsBeingDragged");
            if (ev.getY() < ((int) XposedHelpers.callMethod(mQsContainer, "getBottom")) && !mIsBeingDragged) {
                param.setResult(false);
            }
        }
    };

    private static final XC_MethodReplacement XGetHeaderTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int qsMinExpansionHeight = getInt(mQsMinExpansionHeight, mNotificationPanelView);
            float mExpandedHeight = getFloat(fieldExpandedHeight, mNotificationPanelView);
            if (getStatusBarState() == STATE_KEYGUARD) {
                return 0;
            }
            float translation = NotificationUtils.interpolate(-qsMinExpansionHeight, 0, getAppearFraction(mExpandedHeight));
            return Math.min(0, translation);
        }
    };

    private static final XC_MethodReplacement getMaxPanelHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object statusBar = get(mStatusBar, mNotificationPanelView);
            int min = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarMinHeight");
            int qsMinExpansionHeight = getInt(mQsMinExpansionHeight, mNotificationPanelView);
            int panelHeightQsExpanded = invoke(calculatePanelHeightQsExpanded, mNotificationPanelView);
            boolean mQsExpandImmediate = getBoolean(fieldQsExpandImmediate, mNotificationPanelView);
            boolean qsExpanded = getBoolean(mQsExpanded, mNotificationPanelView);
            boolean mIsExpanding = getBoolean(fieldIsExpanding, mNotificationPanelView);
            boolean qsExpandedWhenExpandingStarted = getBoolean(fieldQsExpandedWhenExpandingStarted, mNotificationPanelView);
            if ((int) XposedHelpers.callMethod(statusBar, "getBarState") != STATE_KEYGUARD
                    && (int) invoke(methodGetNotGoneChildCount, mStackScroller) == 0) {
                int minHeight = (int) ((qsMinExpansionHeight + (float) invoke(getOverExpansionAmount, mNotificationPanelView)));
                min = Math.max(min, minHeight);
            }
            int maxHeight;
            if (mQsExpandImmediate || qsExpanded || mIsExpanding && qsExpandedWhenExpandingStarted) {
                maxHeight = panelHeightQsExpanded;
            } else {
                maxHeight = invoke(calculatePanelHeightShade, mNotificationPanelView);
            }
            maxHeight = Math.max(maxHeight, min);
            return maxHeight;
        }
    };

    private static final XC_MethodHook getPeekHeightHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int qsMinExpansionHeight = getInt(mQsMinExpansionHeight, mNotificationPanelView);
            if (!((int) invoke(methodGetNotGoneChildCount, mStackScroller) > 0))
                param.setResult(qsMinExpansionHeight);
        }
    };

    private static final XC_MethodReplacement setStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float height = (float) param.args[0];
            XposedHelpers.setFloatField(mStackScroller, "mLastSetStackHeight", height);
            invoke(setIsExpanded, mStackScroller, height > 0.0f);
            int stackHeight;
            int mCurrentStackHeight = XposedHelpers.getIntField(mStackScroller, "mCurrentStackHeight");

            float translationY;
            float appearEndPosition = getAppearEndPosition();
            float appearStartPosition = getAppearStartPosition();
            if (height >= appearEndPosition) {
                translationY = getFloat(fieldTopPaddingOverflow, mStackScroller);
                stackHeight = (int) height;
            } else {
                float appearFraction = getAppearFraction(height);
                if (appearFraction >= 0) {
                    translationY = NotificationUtils.interpolate(getExpandTranslationStart(), 0,
                            appearFraction);
                } else {
                    // This may happen when pushing up a heads up. We linearly push it up from the
                    // start
                    translationY = height - appearStartPosition + getExpandTranslationStart();
                }
                stackHeight = (int) (height - translationY);
            }
            if (stackHeight != mCurrentStackHeight) {
                XposedHelpers.setIntField(mStackScroller, "mCurrentStackHeight", stackHeight);
                invoke(updateAlgorithmHeightAndPadding, mStackScroller);
                invoke(requestChildrenUpdate, mStackScroller);
            }
            invoke(setStackTranslation, mStackScroller, translationY);
            return null;
        }
    };

    private static final XC_MethodReplacement getPeekHeightStackScroller = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object firstChild = invoke(methodGetFirstChildNotGone, mStackScroller);
            final int firstChildMinHeight = firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight")
                    : getInt(fieldCollapsedSize, mStackScroller);
            int mIntrinsicPadding = getInt(fieldIntrinsicPadding, mStackScroller);
            int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
            int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
            return mIntrinsicPadding + firstChildMinHeight + mBottomStackPeekSize
                    + mBottomStackSlowDownHeight;
        }
    };

    private static final XC_MethodHook onExpandingFinishedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldScrollYOverride, mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook onQsTouchHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldScrollYOverride, mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook flingSettingsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldScrollYOverride, mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodReplacement onScrollChanged = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean qsExpanded = getBoolean(mQsExpanded, mNotificationPanelView);
            boolean qsFullyExpanded = getBoolean(mQsFullyExpanded, mNotificationPanelView);
            set(mQsMaxExpansionHeight, mNotificationPanelView, invoke(getDesiredHeight, mQsContainer));
            if (qsExpanded && qsFullyExpanded) {
                set(mQsExpansionHeight, mNotificationPanelView, get(mQsMaxExpansionHeight, mNotificationPanelView));
                invoke(requestScrollerTopPaddingUpdate, mNotificationPanelView, false /* animate */);
                invoke(requestPanelHeightUpdate, mNotificationPanelView);
            }
            return null;
        }
    };

    private static final XC_MethodReplacement XCalculatePanelHeightQsExpanded = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mShadeEmpty = XposedHelpers.getBooleanField(mNotificationPanelView, "mShadeEmpty");
            int qsMaxExpansionHeight = getInt(mQsMaxExpansionHeight, mNotificationPanelView);
            int mTopPaddingAdjustment = getInt(fieldTopPaddingAdjustment, mNotificationPanelView);
            Object qsSizeChangeAnimator = get(mQsSizeChangeAnimator, mNotificationPanelView);
            Object mClockPositionResult = get(fieldClockPositionResult, mNotificationPanelView);
            float notificationHeight = mStackScroller.getHeight()
                    - (int) XposedHelpers.callMethod(mStackScroller, "getEmptyBottomMargin")
                    - (int) XposedHelpers.callMethod(mStackScroller, "getTopPadding");

            // When only empty shade view is visible in QS collapsed state, simulate that we would have
            // it in expanded QS state as well so we don't run into troubles when fading the view in/out
            // and expanding/collapsing the whole panel from/to quick settings.
            if ((int) invoke(methodGetNotGoneChildCount, mStackScroller) == 0
                    && mShadeEmpty) {
                notificationHeight = (int) XposedHelpers.callMethod(mStackScroller, "getEmptyShadeViewHeight")
                        + (int) XposedHelpers.callMethod(mStackScroller, "getBottomStackPeekSize")
                        + getInt(fieldBottomStackSlowDownHeight, mStackScroller);
            }
            int maxQsHeight = qsMaxExpansionHeight;

            // If an animation is changing the size of the QS panel, take the animated value.
            if (qsSizeChangeAnimator != null) {
                maxQsHeight = (int) XposedHelpers.callMethod(qsSizeChangeAnimator, "getAnimatedValue");
            }
            float totalHeight = Math.max(
                    maxQsHeight, getStatusBarState() == STATE_KEYGUARD
                            ? XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding") - mTopPaddingAdjustment
                            : 0)
                    + notificationHeight + (float) XposedHelpers.callMethod(mStackScroller, "getTopPaddingOverflow");

            if (totalHeight > mStackScroller.getHeight()) {
                float fullyCollapsedHeight = maxQsHeight
                        + getLayoutMinHeight();
                totalHeight = Math.max(fullyCollapsedHeight, mStackScroller.getHeight());
            }
            return (int) totalHeight;
        }
    };

    private static final XC_MethodReplacement getFadeoutAlpha = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int qsMinExpansionHeight = getInt(mQsMinExpansionHeight, mNotificationPanelView);
            float alpha = ((float) XposedHelpers.callMethod(mNotificationPanelView, "getNotificationsTopY") + getFirstItemMinHeight())
                    / (qsMinExpansionHeight + (int) XposedHelpers.callMethod(mStackScroller, "getBottomStackPeekSize")
                    - getInt(fieldBottomStackSlowDownHeight, mStackScroller));
            alpha = Math.max(0, Math.min(alpha, 1));
            alpha = (float) Math.pow(alpha, 0.75);
            return alpha;
        }
    };

    private static final XC_MethodReplacement getMinStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
            int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
            int mMaxLayoutHeight = getInt(fieldMaxLayoutHeight, mStackScroller);
            int mIntrinsicPadding = getInt(fieldIntrinsicPadding, mStackScroller);
            int firstChildMinHeight = getFirstChildIntrinsicHeight();
            return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                    mMaxLayoutHeight - mIntrinsicPadding);
        }
    };

    private static final XC_MethodReplacement getEmptyBottomMargin = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mMaxLayoutHeight = getInt(fieldMaxLayoutHeight, mStackScroller);
            int mContentHeight = XposedHelpers.getIntField(mStackScroller, "mContentHeight"); //TODO: optimize
            int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
            int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
            int emptyMargin = mMaxLayoutHeight - mContentHeight - mBottomStackPeekSize
                    - mBottomStackSlowDownHeight;
            return Math.max(emptyMargin, 0);
        }
    };

    private static final XC_MethodReplacement getDismissViewHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View mDismissView = (View) XposedHelpers.getObjectField(mStackScroller, "mDismissView");
            int mPaddingBetweenElements = getInt(fieldPaddingBetweenElements, mStackScroller);
            return mDismissView.getHeight() + mPaddingBetweenElements;
        }
    };

    private static final XC_MethodReplacement positionClockAndNotifications = new XC_MethodReplacement() { //TODO: optimize this as it's called very often.
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

            Object clockPositionAlgorithm = get(mClockPositionAlgorithm, mNotificationPanelView);
            Object statusBar = get(mStatusBar, mNotificationPanelView);
            View keyguardStatusView = get(mKeyguardStatusView, mNotificationPanelView);
            Object mClockPositionResult = get(fieldClockPositionResult, mNotificationPanelView);
            Object mClockAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mClockAnimator");
            boolean animate = invoke(isAddOrRemoveAnimationPending, mStackScroller);
            int stackScrollerPadding;
            if (getStatusBarState() != STATE_KEYGUARD) {
                stackScrollerPadding = mHeader.getHeight() + XposedHelpers.getIntField(mNotificationPanelView, "mQsPeekHeight");
                set(fieldTopPaddingAdjustment, mNotificationPanelView, 0);
            } else {
                try {
                    XposedHelpers.callMethod(clockPositionAlgorithm, "setup",
                            NotificationHooks.getMaxKeyguardNotifications(statusBar, false),
                            invoke(methodGetMaxPanelHeight, mNotificationPanelView),
                            invoke(getExpandedHeight, mNotificationPanelView),
                            invoke(methodGetNotGoneChildCount, mStackScroller),
                            mNotificationPanelView.getHeight(),
                            keyguardStatusView.getHeight(),
                            XposedHelpers.getFloatField(mNotificationPanelView, "mEmptyDragAmount"));
                    XposedHelpers.callMethod(clockPositionAlgorithm, "run", mClockPositionResult);
                } catch (NoSuchMethodError e) {//Xperia
                    XposedHelpers.callMethod(mNotificationPanelView, "positionKeyguardClockAndResize");
                }
                if (animate || mClockAnimator != null) {
                    XposedHelpers.callMethod(mNotificationPanelView, "startClockAnimation", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                } else {
                    keyguardStatusView.setY(XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                }
                XposedHelpers.callMethod(mNotificationPanelView, "updateClock", XposedHelpers.getFloatField(mClockPositionResult, "clockAlpha"), XposedHelpers.getFloatField(mClockPositionResult, "clockScale"));
                stackScrollerPadding = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding");
                set(fieldTopPaddingAdjustment, mNotificationPanelView, XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPaddingAdjustment"));
            }
            XposedHelpers.callMethod(mStackScroller, "setIntrinsicPadding", stackScrollerPadding);
            invoke(requestScrollerTopPaddingUpdate, mNotificationPanelView, animate);
            return null;
        }
    };

    private static final XC_MethodReplacement setQsExpansion = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int qsMinExpansionHeight = getInt(mQsMinExpansionHeight, mNotificationPanelView);
            int qsMaxExpansionHeight = getInt(mQsMaxExpansionHeight, mNotificationPanelView);
            Object mQsNavbarScrim = XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean qsExpanded = getBoolean(mQsExpanded, mNotificationPanelView);
            boolean mStackScrollerOverscrolling = getBoolean(fieldStackScrollerOverscrolling, mNotificationPanelView);
            boolean mLastAnnouncementWasQuickSettings = XposedHelpers.getBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings");
            boolean mTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mTracking");
            boolean isCollapsing = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isCollapsing");
            boolean mQsScrimEnabled = getBoolean(fieldQsScrimEnabled, mNotificationPanelView);
            float height = (float) param.args[0];
            height = Math.min(Math.max(height, qsMinExpansionHeight), qsMaxExpansionHeight);
            set(mQsFullyExpanded, mNotificationPanelView, height == qsMaxExpansionHeight && qsMaxExpansionHeight != 0);
            if (height > qsMinExpansionHeight && !qsExpanded && !mStackScrollerOverscrolling) {
                invoke(methodSetQsExpanded, mNotificationPanelView, true);
            } else if (height <= qsMinExpansionHeight && qsExpanded) {
                invoke(methodSetQsExpanded, mNotificationPanelView, false);
                if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing) {
                    XposedHelpers.callMethod(mNotificationPanelView, "announceForAccessibility", (boolean) XposedHelpers.callMethod(mNotificationPanelView, "getKeyguardOrLockScreenString"));
                    XposedHelpers.setBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings", false);
                }
            }
            set(mQsExpansionHeight, mNotificationPanelView, height);
            updateQsExpansion();

            invoke(requestScrollerTopPaddingUpdate, mNotificationPanelView, false /* animate */);

            if (isOnKeyguard()) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");

            }
            if (getStatusBarState() == STATE_SHADE_LOCKED
                    || getStatusBarState() == STATE_KEYGUARD) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateKeyguardBottomAreaAlpha");
            }
            if (getStatusBarState() == STATE_SHADE && qsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled) {
                XposedHelpers.callMethod(mQsNavbarScrim, "setAlpha", invoke(getQsExpansionFraction, mNotificationPanelView));
            }
            return null;
        }
    };

    private static final XC_MethodReplacement updateHeader = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (getStatusBarState() == STATE_KEYGUARD)
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");
            updateQsExpansion();
            return null;
        }
    };

    private static final XC_MethodHook calculateQsTopPaddingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            set(fieldScrollYOverride, mNotificationPanelView, -1);
        }
    };

    private static final XC_MethodHook updateChildrenHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            updateScrollStateForAddedChildren();
        }
    };

    private static final XC_MethodReplacement updateSpeedBumpIndex = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int newIndex = (int) param.args[0];
            Object mAmbientState = XposedHelpers.getObjectField(mStackScroller, "mAmbientState");
            XposedHelpers.callMethod(mAmbientState, "setSpeedBumpIndex", newIndex);
            return null;
        }
    };

    private static void setListening(Object keyguardStatusBar, boolean listening) {
        Object mBatteryListening = XposedHelpers.getAdditionalInstanceField(keyguardStatusBar, "mBatteryListening");
        if (mBatteryListening == null || listening == (boolean) mBatteryListening) {
            return;
        }
        Object mBatteryController = XposedHelpers.getObjectField(keyguardStatusBar, "mBatteryController");
        XposedHelpers.setAdditionalInstanceField(keyguardStatusBar, "mBatteryListening", listening);
        if ((boolean) mBatteryListening) {
            XposedHelpers.callMethod(mBatteryController, "addStateChangedCallback", keyguardStatusBar);
        } else {
            XposedHelpers.callMethod(mBatteryController, "removeStateChangedCallback", keyguardStatusBar);
        }
    }

    private static void updateQsExpansion() {
        NotificationPanelHooks.getQsContainerHelper().setQsExpansion((float) invoke(getQsExpansionFraction, mNotificationPanelView), (float) invoke(getHeaderTranslation, mNotificationPanelView));
    }

    private static int getFirstItemMinHeight() {
        final Object firstChild = invoke(methodGetFirstChildNotGone, mStackScroller);
        int mCollapsedSize = getInt(fieldCollapsedSize, mStackScroller);
        return firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight") : mCollapsedSize;
    }

    private static float getExpandTranslationStart() {
        Object mHeadsUpManager = get(fieldHeadsUpManager, mStackScroller);
        int mMaxLayoutHeight = getInt(fieldMaxLayoutHeight, mStackScroller);
        int mIntrinsicPadding = getInt(fieldIntrinsicPadding, mStackScroller);
        int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
        int mTopPadding = getInt(fieldTopPadding, mStackScroller);
        int startPosition = 0;
        if (!getBoolean(fieldTrackingHeadsUp, mStackScroller) && !(boolean) invoke(methodHasPinnedHeadsUp, mHeadsUpManager)) {
            startPosition = -Math.min(getFirstChildIntrinsicHeight(),
                    mMaxLayoutHeight - mIntrinsicPadding - mBottomStackSlowDownHeight
                            - mBottomStackPeekSize);
        }
        return startPosition - mTopPadding;
    }

    /**
     * @return the position from where the appear transition starts when expanding.
     * Measured in absolute height.
     */
    private static float getAppearStartPosition() {
        Object mHeadsUpManager = get(fieldHeadsUpManager, mStackScroller);
        boolean trackingHeadsUp = getBoolean(fieldTrackingHeadsUp, mStackScroller) || (boolean) invoke(methodHasPinnedHeadsUp, mHeadsUpManager);
        return trackingHeadsUp
                ? (int) invoke(methodGetTopHeadsUpHeight, mHeadsUpManager)
                : 0;
    }

    /**
     * @return the position from where the appear transition ends when expanding.
     * Measured in absolute height.
     */
    private static float getAppearEndPosition()  {
        Object mHeadsUpManager = get(fieldHeadsUpManager, mStackScroller);
        boolean trackingHeadsUp = getBoolean(fieldTrackingHeadsUp, mStackScroller) || (boolean) invoke(methodHasPinnedHeadsUp, mHeadsUpManager);
        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
        int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
        int mTopPadding = getInt(fieldTopPadding, mStackScroller);
        int mIntrinsicPadding = getInt(fieldIntrinsicPadding, mStackScroller);
        int firstItemHeight = trackingHeadsUp
                ? (int) invoke(methodGetTopHeadsUpHeight, mHeadsUpManager) + mBottomStackPeekSize
                + mBottomStackSlowDownHeight
                : getLayoutMinHeight();
        return firstItemHeight + (isOnKeyguard() ? mTopPadding : mIntrinsicPadding);
    }

    public static int getLayoutMinHeight() {
        int firstChildMinHeight = getFirstChildIntrinsicHeight();
        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScroller);
        int mBottomStackSlowDownHeight = getInt(fieldBottomStackSlowDownHeight, mStackScroller);
        int mIntrinsicPadding = getInt(fieldIntrinsicPadding, mStackScroller);
        int mMaxLayoutHeight = getInt(fieldMaxLayoutHeight, mStackScroller);
        return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                mMaxLayoutHeight - mIntrinsicPadding);
    }

    private static int getFirstChildIntrinsicHeight() {
        final Object firstChild = invoke(methodGetFirstChildNotGone, mStackScroller);
        final Object mEmptyShadeView = XposedHelpers.getObjectField(mStackScroller, "mEmptyShadeView");
        int mCollapsedSize = getInt(fieldCollapsedSize, mStackScroller);
        int mOwnScrollY = getInt(fieldOwnScrollY, mStackScroller);
        int firstChildMinHeight = firstChild != null
                ? (int) invoke(Methods.SystemUI.ExpandableView.getIntrinsicHeight, firstChild)
                : mEmptyShadeView != null
                ? (int) XposedHelpers.callMethod(mEmptyShadeView, "getMinHeight")
                : mCollapsedSize;
        if (mOwnScrollY > 0) {
            firstChildMinHeight = Math.max(firstChildMinHeight - mOwnScrollY, mCollapsedSize);
        }
        return firstChildMinHeight;
    }

    /**
     * @param height the height of the panel
     * @return the fraction of the appear animation that has been performed
     */
    private static float getAppearFraction(float height) {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (height - appearStartPosition)
                / (appearEndPosition - appearStartPosition);
    }

    private static void updateScrollStateForAddedChildren() {
        int mPaddingBetweenElements = getInt(fieldPaddingBetweenElements, mStackScroller);
        int mOwnScrollY = getInt(fieldOwnScrollY, mStackScroller);
        ArrayList<View> mChildrenToAddAnimated = get(fieldChildrenToAddAnimated, mStackScroller);
        if (mChildrenToAddAnimated.isEmpty()) {
            return;
        }
        for (int i = 0; i < mStackScroller.getChildCount(); i++) {
            View child = mStackScroller.getChildAt(i);
            if (mChildrenToAddAnimated.contains(child)) {
                int startingPosition = invoke(methodGetPositionInLinearLayout, mStackScroller, child);
                int padding = mPaddingBetweenElements;
                int childHeight = (int) invoke(methodGetIntrinsicHeight, mStackScroller, child) + padding;
                if (startingPosition < mOwnScrollY) {
                    // This child starts off screen, so let's keep it offscreen to keep the others visible
                    set(fieldOwnScrollY, mStackScroller, mOwnScrollY + childHeight);
                }
            }
        }
        invoke(methodClampScrollPosition, mStackScroller);
    }

    /**
     * @param maximum the maximum to return at most
     * @return the maximum keyguard notifications that can fit on the screen
     */
    public static int computeMaxKeyguardNotifications(View notificationPanel, int maximum) {
        Object clockPositionAlgorithm = get(mClockPositionAlgorithm, notificationPanel); //TODO: switch to global variables once they're set up(Fields.java)
        Object statusBar = get(mStatusBar, notificationPanel);
        View keyguardStatusView = get(mKeyguardStatusView, notificationPanel);
        Resources res = notificationPanel.getContext().getResources();
        float minPadding = getMinStackScrollerPadding(clockPositionAlgorithm, notificationPanel.getHeight(),
                keyguardStatusView.getHeight());
        int notificationPadding = Math.max(1, res.getDimensionPixelSize(
                res.getIdentifier("notification_divider_height", "dimen", XposedHook.PACKAGE_SYSTEMUI)));
        final int overflowheight = res.getDimensionPixelSize(
                res.getIdentifier("notification_summary_height", "dimen", XposedHook.PACKAGE_SYSTEMUI));
        float bottomStackSize = getKeyguardBottomStackSize(mStackScroller);
        float availableSpace = mStackScroller.getHeight() - minPadding - overflowheight
                - bottomStackSize;
        int count = 0;
        for (int i = 0; i < mStackScroller.getChildCount(); i++) {
            View  child = mStackScroller.getChildAt(i);
            if (!(Classes.SystemUI.ExpandableNotificationRow.isInstance(child))) {
                continue;
            }
            View row = child;
            Object notification = invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, row);
//            boolean suppressedSummary = mGroupManager.isSummaryOfSuppressedGroup(
//                    notification);
//            if (suppressedSummary) {
//                continue;
//            }
            boolean shouldShow;
            try {
                shouldShow = invoke(Methods.SystemUI.BaseStatusBar.shouldShowOnKeyguard, statusBar, notification);
            } catch (UncheckedIllegalArgumentException e) { //Xperia
                boolean show = XposedHelpers.getBooleanField(statusBar, "mShowLockscreenNotifications");
                if (XposedHelpers.getBooleanField(statusBar, "mIsDisableSecureNotificationsByDpm"))
                    show = false;
                shouldShow = invoke(Methods.SystemUI.BaseStatusBar.shouldShowOnKeyguard, statusBar, notification, show);
            }
            if (!shouldShow) {
                continue;
            }
//            if ((boolean) XposedHelpers.callMethod(row, "isRemoved")) {
//                continue;
//            }
            availableSpace -= (int) XposedHelpers.callMethod(child, "getMinHeight") + notificationPadding;
            if (availableSpace >= 0 && count < maximum) {
                count++;
            } else {
                return count;
            }
        }
        return count;
    }

    public static float getMinStackScrollerPadding(Object algorithm, int height, int keyguardStatusHeight) {
        return XposedHelpers.getFloatField(algorithm, "mClockYFractionMin") * height + keyguardStatusHeight / 2
                + XposedHelpers.getIntField(algorithm, "mClockNotificationsMarginMin");
    }

    public static float getKeyguardBottomStackSize(View stackScroller) {
        Resources res = stackScroller.getContext().getResources();
        return getInt(fieldBottomStackPeekSize, stackScroller) + res.getDimensionPixelSize(
                res.getIdentifier("bottom_stack_slow_down_length", "dimen", XposedHook.PACKAGE_SYSTEMUI));
    }
}
