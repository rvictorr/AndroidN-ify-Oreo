package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.getStatusBarState;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
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
    private static ViewGroup mQsPanel;
    private static ViewGroup mNotificationStackScroller;

    private static Method methodCancelQsAnimation;
    private static Method methodCancelHeightAnimator;
    private static Method methodSetQsExpansion;
    private static Method methodRequestPanelHeightUpdate;
    private static Method methodOnQsExpansionStarted;
    private static Method methodSetQsExpanded;
    private static Method methodGetMaxPanelHeight;
    private static Method methodGetExpandedHeight;
    private static Method methodGetQsExpansionFraction;
    private static Method methodGetHeaderTranslation;

    private static Method methodGetNotGoneChildCount;
    private static Method methodGetFirstChildNotGone;

    private static Method methodHasPinnedHeadsUp;
    private static Method methodGetTopHeadsUpHeight;
    private static Method methodGetIntrinsicHeight;
    private static Method methodGetPositionInLinearLayout;
    private static Method methodClampScrollPosition;
    private static Method methodIsScrolledToBottom;

    private static Field fieldQsExpansionHeight;
    private static Field fieldQsMinExpansionHeight;
    private static Field fieldQsMaxExpansionHeight;
    private static Field fieldQsExpanded;
    private static Field fieldQsExpandImmediate;
    private static Field fieldQsExpandedWhenExpandingStarted;
    private static Field fieldQsFullyExpanded;
    private static Field fieldStackScrollerOverscrolling;
    private static Field fieldHeaderAnimating;
    private static Field fieldKeyguardShowing;
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
    private static Field fieldStatusBar;
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
                methodRequestPanelHeightUpdate = XposedHelpers.findMethodBestMatch(NotificationPanelView, "requestPanelHeightUpdate");
                methodOnQsExpansionStarted = XposedHelpers.findMethodBestMatch(NotificationPanelView, "onQsExpansionStarted");
                methodSetQsExpanded = XposedHelpers.findMethodBestMatch(NotificationPanelView, "setQsExpanded", boolean.class);
                methodGetMaxPanelHeight = XposedHelpers.findMethodBestMatch(NotificationPanelView, "getMaxPanelHeight");
                methodGetExpandedHeight = XposedHelpers.findMethodBestMatch(NotificationPanelView, "getExpandedHeight");
                methodGetQsExpansionFraction = XposedHelpers.findMethodBestMatch(NotificationPanelView, "getQsExpansionFraction");
                methodGetHeaderTranslation = XposedHelpers.findMethodBestMatch(NotificationPanelView, "getHeaderTranslation");

                methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getIntrinsicHeight", View.class);
                methodGetPositionInLinearLayout = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getPositionInLinearLayout", View.class);
                methodClampScrollPosition = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "clampScrollPosition");
                methodGetNotGoneChildCount = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "getNotGoneChildCount");
                methodIsScrolledToBottom = XposedHelpers.findMethodBestMatch(NotificationStackScrollLayout, "isScrolledToBottom");

                methodHasPinnedHeadsUp = XposedHelpers.findMethodBestMatch(HeadsUpManager, "hasPinnedHeadsUp");
                methodGetTopHeadsUpHeight = XposedHelpers.findMethodBestMatch(HeadsUpManager, "getTopHeadsUpHeight");

                fieldQsExpansionHeight = XposedHelpers.findField(NotificationPanelView, "mQsExpansionHeight");
                fieldQsMinExpansionHeight = XposedHelpers.findField(NotificationPanelView, "mQsMinExpansionHeight");
                fieldQsMaxExpansionHeight = XposedHelpers.findField(NotificationPanelView, "mQsMaxExpansionHeight");
                fieldQsExpanded = XposedHelpers.findField(NotificationPanelView, "mQsExpanded");
                fieldQsExpandImmediate = XposedHelpers.findField(NotificationPanelView, "mQsExpandImmediate");
                fieldQsExpandedWhenExpandingStarted = XposedHelpers.findField(NotificationPanelView, "mQsExpandedWhenExpandingStarted");
                fieldQsFullyExpanded = XposedHelpers.findField(NotificationPanelView, "mQsFullyExpanded");
                fieldStackScrollerOverscrolling = XposedHelpers.findField(NotificationPanelView, "mStackScrollerOverscrolling");
                fieldHeaderAnimating = XposedHelpers.findField(NotificationPanelView, "mHeaderAnimating");
                fieldKeyguardShowing = XposedHelpers.findField(NotificationPanelView, "mKeyguardShowing");
                fieldQsExpansionFromOverscroll = XposedHelpers.findField(NotificationPanelView, "mQsExpansionFromOverscroll");
                fieldQsScrimEnabled = XposedHelpers.findField(NotificationPanelView, "mQsScrimEnabled");
                fieldIsExpanding = XposedHelpers.findField(NotificationPanelView, "mIsExpanding");
                fieldExpandedHeight = XposedHelpers.findField(NotificationPanelView, "mExpandedHeight");
                fieldTopPaddingAdjustment = XposedHelpers.findField(NotificationPanelView, "mTopPaddingAdjustment");
                fieldQsExpansionEnabled = XposedHelpers.findField(NotificationPanelView, "mQsExpansionEnabled");
                fieldClockPositionResult = XposedHelpers.findField(NotificationPanelView, "mClockPositionResult");
                fieldStatusBar = XposedHelpers.findField(NotificationPanelView, "mStatusBar");
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
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getHeaderTranslation", getHeaderTranslation);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getPeekHeight", getPeekHeightHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "getScrollViewScrollY", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onExpandingFinished", onExpandingFinishedHook);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onScrollChanged", onScrollChanged);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "onHeightUpdated", float.class, onHeightUpdated);
                XposedHelpers.findAndHookMethod(NotificationPanelView, "calculatePanelHeightQsExpanded", calculatePanelHeightQsExpanded);
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
            mHeader = (ViewGroup) fieldHeader.get(mNotificationPanelView);
            mScrollView = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView");
            mQsContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQsFooter = mNotificationPanelView.findViewById(R.id.qs_footer);
            mQsPanel = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsPanel");
            mNotificationStackScroller = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mNotificationStackScroller");
        }
    };

    private static final XC_MethodReplacement onQsExpansionStarted = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            methodCancelQsAnimation.invoke(mNotificationPanelView);
            methodCancelHeightAnimator.invoke(mNotificationPanelView);
            float height = fieldQsExpansionHeight.getFloat(mNotificationPanelView) - (int) param.args[0];
            methodSetQsExpansion.invoke(mNotificationPanelView, height);
            methodRequestPanelHeightUpdate.invoke(mNotificationPanelView);
            return null;
        }
    };

    private static final XC_MethodReplacement setVerticalPanelTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float translation = (float) param.args[0];
            mNotificationStackScroller.setTranslationX(translation);
            mScrollView.setTranslationX(translation);
            return null;
        }
    };

    private static final XC_MethodHook getTempQsMaxExpansionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.setResult(fieldQsMaxExpansionHeight.getInt(mNotificationPanelView));
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
            XposedHelpers.callMethod(mQsPanel, "setListening", param.args[0]);
            return null;
        }
    };

    private static final XC_MethodReplacement onClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (((View) param.args[0]).getId() == R.id.statusbar_header_expand_indicator) {
                methodOnQsExpansionStarted.invoke(mNotificationPanelView);
                if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, false, null, true);
                } else if (fieldQsExpansionEnabled.getBoolean(mNotificationPanelView)) {
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
            if (!fieldQsExpansionEnabled.getBoolean(mNotificationPanelView) || XposedHelpers.getBooleanField(mNotificationPanelView, "mCollapsedOnDown")) {
                param.setResult(false);
            }
            View header = fieldKeyguardShowing.getBoolean(mNotificationPanelView) ? (View) XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusBar") : (View) fieldHeader.get(mNotificationPanelView);
            View mScrollView = (View) (XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView"));
            boolean onHeader = x >= mScrollView.getX()
                    && x <= mScrollView.getX() + mScrollView.getWidth()
                    && y >= header.getTop() && y <= header.getBottom();
            if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                param.setResult(onHeader || (yDiff < 0 && (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isInQsArea", x, y)));
            } else {
                param.setResult(onHeader);
            }
        }
    };

    private static final XC_MethodReplacement updateQsState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mStackScrollerOverscrolling = fieldStackScrollerOverscrolling.getBoolean(mNotificationPanelView);
            boolean mHeaderAnimating = fieldHeaderAnimating.getBoolean(mNotificationPanelView);
            boolean mKeyguardShowing = fieldKeyguardShowing.getBoolean(mNotificationPanelView);
            boolean mQsExpansionFromOverscroll = fieldQsExpansionFromOverscroll.getBoolean(mNotificationPanelView);
            boolean mQsScrimEnabled = fieldQsScrimEnabled.getBoolean(mNotificationPanelView);
            Object mKeyguardUserSwitcher = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardUserSwitcher");
            int mStatusBarState = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
            View mQsNavbarScrim = (View) XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean expandVisually = mQsExpanded || mStackScrollerOverscrolling || mHeaderAnimating;
            XposedHelpers.callMethod(mQsPanel, "setExpanded", mQsExpanded);
            mHeader.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                    ? View.VISIBLE
                    : View.INVISIBLE);
            XposedHelpers.callMethod(mHeader, "setExpanded", ((mKeyguardShowing && !mHeaderAnimating)
                    || (mQsExpanded && !mStackScrollerOverscrolling)));
            mQsFooter.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                    ? View.VISIBLE
                    : View.INVISIBLE);
            mQsFooter.setExpanded((mKeyguardShowing && !mHeaderAnimating)
                    || (mQsExpanded && !mStackScrollerOverscrolling));
            XposedHelpers.callMethod(mQsPanel, "setVisibility", (expandVisually ? View.VISIBLE : View.INVISIBLE));
            XposedHelpers.callMethod(mNotificationStackScroller, "setScrollingEnabled", (
                    mStatusBarState != STATE_KEYGUARD && (!mQsExpanded
                            || mQsExpansionFromOverscroll)));
            XposedHelpers.callMethod(mNotificationPanelView, "updateEmptyShadeView");
            mQsNavbarScrim.setVisibility(mStatusBarState == STATE_SHADE && mQsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled
                    ? View.VISIBLE
                    : View.INVISIBLE);
            if (mKeyguardUserSwitcher != null && mQsExpanded && !mStackScrollerOverscrolling) {
                XposedHelpers.callMethod(mKeyguardUserSwitcher, "hideIfNotSimple", true /* animate */);
            }
            return null;
        }
    };

    private static final XC_MethodHook onTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            fieldDelegateToScrollView.setBoolean(mNotificationStackScroller, false);
            fieldOnlyScrollingInThisMotion.setBoolean(mNotificationStackScroller, false);
        }
    };

    private static final XC_MethodHook onInterceptTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            fieldInterceptDelegateEnabled.setBoolean(mNotificationStackScroller, false);
            fieldOnlyScrollingInThisMotion.setBoolean(mNotificationStackScroller, false);
        }
    };

    private static final XC_MethodReplacement isScrolledToBottom = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            int getBarState = (int) XposedHelpers.callMethod(mStatusBar, "getBarState");
            boolean isScrolledToBottom = (boolean) methodIsScrolledToBottom.invoke(mNotificationStackScroller);
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
            boolean mIsBeingDragged = XposedHelpers.getBooleanField(mNotificationStackScroller, "mIsBeingDragged");
            if (ev.getY() < ((int) XposedHelpers.callMethod(mQsContainer, "getBottom")) && !mIsBeingDragged) {
                param.setResult(false);
            }
        }
    };

    private static final XC_MethodReplacement getHeaderTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            float mExpandedHeight = fieldExpandedHeight.getFloat(mNotificationPanelView);
            if (getStatusBarState() == STATE_KEYGUARD) {
                return 0;
            }
            float translation = NotificationUtils.interpolate(-mQsMinExpansionHeight, 0, getAppearFraction(mExpandedHeight));
            return Math.min(0, translation);
        }
    };

    private static final XC_MethodReplacement getMaxPanelHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            int min = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarMinHeight");
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int panelHeightQsExpanded = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
            if ((int) XposedHelpers.callMethod(mStatusBar, "getBarState") != STATE_KEYGUARD
                    && (int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) == 0) {
                int minHeight = (int) ((mQsMinExpansionHeight + (float) XposedHelpers.callMethod(mNotificationPanelView, "getOverExpansionAmount")));
                min = Math.max(min, minHeight);
            }
            int maxHeight;
            if (mQsExpandImmediate || mQsExpanded || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                maxHeight = panelHeightQsExpanded;
            } else {
                maxHeight = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightShade");
            }
            maxHeight = Math.max(maxHeight, min);
            return maxHeight;
        }
    };

    private static final XC_MethodHook getPeekHeightHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            if (!((int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) > 0))
                param.setResult(mQsMinExpansionHeight);
        }
    };

    private static final XC_MethodReplacement setStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float height = (float) param.args[0];
            XposedHelpers.setFloatField(mNotificationStackScroller, "mLastSetStackHeight", height);
            XposedHelpers.callMethod(mNotificationStackScroller, "setIsExpanded", height > 0.0f);
            int stackHeight;
            int mCurrentStackHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mCurrentStackHeight");

            float translationY;
            float appearEndPosition = getAppearEndPosition();
            float appearStartPosition = getAppearStartPosition();
            if (height >= appearEndPosition) {
                translationY = fieldTopPaddingOverflow.getFloat(mNotificationStackScroller);
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
                XposedHelpers.setIntField(mNotificationStackScroller, "mCurrentStackHeight", stackHeight);
                XposedHelpers.callMethod(mNotificationStackScroller, "updateAlgorithmHeightAndPadding");
                XposedHelpers.callMethod(mNotificationStackScroller, "requestChildrenUpdate");
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setStackTranslation", translationY);
            return null;
        }
    };

    private static final XC_MethodReplacement getPeekHeightStackScroller = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
            final int firstChildMinHeight = firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight")
                    : fieldCollapsedSize.getInt(mNotificationStackScroller);
            int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            return mIntrinsicPadding + firstChildMinHeight + mBottomStackPeekSize
                    + mBottomStackSlowDownHeight;
        }
    };

    private static final XC_MethodHook onExpandingFinishedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook onQsTouchHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook flingSettingsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodReplacement onScrollChanged = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsFullyExpanded = fieldQsFullyExpanded.getBoolean(mNotificationPanelView);
            fieldQsMaxExpansionHeight.setInt(mNotificationPanelView, (int) XposedHelpers.callMethod(mQsContainer, "getDesiredHeight"));
            if (mQsExpanded && mQsFullyExpanded) {
                fieldQsExpansionHeight.setFloat(mNotificationPanelView, fieldQsMaxExpansionHeight.getInt(mNotificationPanelView));
                XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", false /* animate */);
                XposedHelpers.callMethod(mNotificationPanelView, "requestPanelHeightUpdate");
            }
            return null;
        }
    };

    private static final XC_MethodReplacement onHeightUpdated = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float expandedHeight = (float) param.args[0];
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
            boolean mQsTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsTracking");
            boolean mQsExpansionFromOverscroll = fieldQsExpansionFromOverscroll.getBoolean(mNotificationPanelView);
            boolean isFullyCollapsed = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isFullyCollapsed");
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            Object mQsExpansionAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsExpansionAnimator");

            if (!mQsExpanded || mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                XposedHelpers.callMethod(mNotificationPanelView, "positionClockAndNotifications");
            }
            if (mQsExpandImmediate || mQsExpanded && !mQsTracking && mQsExpansionAnimator == null
                    && !mQsExpansionFromOverscroll) {
                float t;
                if (isOnKeyguard()) {
                    // On Keyguard, interpolate the QS expansion linearly to the panel expansion
                    t = expandedHeight / (int) methodGetMaxPanelHeight.invoke(mNotificationPanelView);
                } else {
                    // In Shade, interpolate linearly such that QS is closed whenever panel height is
                    // minimum QS expansion + minStackHeight
                    float panelHeightQsCollapsed = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getIntrinsicPadding")
                            + getLayoutMinHeight();
                    float panelHeightQsExpanded = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
                    t = (expandedHeight - panelHeightQsCollapsed)
                            / (panelHeightQsExpanded - panelHeightQsCollapsed);
                }
                methodSetQsExpansion.invoke(mNotificationPanelView, mQsMinExpansionHeight
                        + t * ((int) XposedHelpers.callMethod(mNotificationPanelView, "getTempQsMaxExpansion") - mQsMinExpansionHeight));
            }
            XposedHelpers.callMethod(mNotificationPanelView, "updateStackHeight", expandedHeight);
            XposedHelpers.callMethod(mNotificationPanelView, "updateHeader");
            XposedHelpers.callMethod(mNotificationPanelView, "updateUnlockIcon");
            XposedHelpers.callMethod(mNotificationPanelView, "updateNotificationTranslucency");
            XposedHelpers.callMethod(mNotificationPanelView, "updatePanelExpanded");
            XposedHelpers.callMethod(mNotificationStackScroller, "setShadeExpanded", !isFullyCollapsed);
            return null;
        }
    };

    private static final XC_MethodReplacement calculatePanelHeightQsExpanded = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mShadeEmpty = XposedHelpers.getBooleanField(mNotificationPanelView, "mShadeEmpty");
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
            int mTopPaddingAdjustment = fieldTopPaddingAdjustment.getInt(mNotificationPanelView);
            Object mQsSizeChangeAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsSizeChangeAnimator");
            Object mClockPositionResult = fieldClockPositionResult.get(mNotificationPanelView);
            float notificationHeight = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getHeight")
                    - (int) XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyBottomMargin")
                    - (int) XposedHelpers.callMethod(mNotificationStackScroller, "getTopPadding");

            // When only empty shade view is visible in QS collapsed state, simulate that we would have
            // it in expanded QS state as well so we don't run into troubles when fading the view in/out
            // and expanding/collapsing the whole panel from/to quick settings.
            if ((int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) == 0
                    && mShadeEmpty) {
                notificationHeight = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyShadeViewHeight")
                        + (int) XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                        + fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            }
            int maxQsHeight = mQsMaxExpansionHeight;

            // If an animation is changing the size of the QS panel, take the animated value.
            if (mQsSizeChangeAnimator != null) {
                maxQsHeight = (int) XposedHelpers.callMethod(mQsSizeChangeAnimator, "getAnimatedValue");
            }
            float totalHeight = Math.max(
                    maxQsHeight, getStatusBarState() == STATE_KEYGUARD
                            ? XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding") - mTopPaddingAdjustment
                            : 0)
                    + notificationHeight + (float) XposedHelpers.callMethod(mNotificationStackScroller, "getTopPaddingOverflow");

            if (totalHeight > mNotificationStackScroller.getHeight()) {
                float fullyCollapsedHeight = maxQsHeight
                        + getLayoutMinHeight();
                totalHeight = Math.max(fullyCollapsedHeight, (int) XposedHelpers.callMethod(mNotificationStackScroller, "getHeight"));
            }
            return (int) totalHeight;
        }
    };

    private static final XC_MethodReplacement getFadeoutAlpha = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            float alpha = ((float) XposedHelpers.callMethod(mNotificationPanelView, "getNotificationsTopY") + getFirstItemMinHeight())
                    / (mQsMinExpansionHeight + (int) XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                    - fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller));
            alpha = Math.max(0, Math.min(alpha, 1));
            alpha = (float) Math.pow(alpha, 0.75);
            return alpha;
        }
    };

    private static final XC_MethodReplacement getMinStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
            int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
            int firstChildMinHeight = getFirstChildIntrinsicHeight();
            return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                    mMaxLayoutHeight - mIntrinsicPadding);
        }
    };

    private static final XC_MethodReplacement getEmptyBottomMargin = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
            int mContentHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mContentHeight");
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int emptyMargin = mMaxLayoutHeight - mContentHeight - mBottomStackPeekSize
                    - mBottomStackSlowDownHeight;
            return Math.max(emptyMargin, 0);
        }
    };

    private static final XC_MethodReplacement getDismissViewHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mDismissView = XposedHelpers.getObjectField(mNotificationStackScroller, "mDismissView");
            int mPaddingBetweenElements = fieldPaddingBetweenElements.getInt(mNotificationStackScroller);
            return (int) XposedHelpers.callMethod(mDismissView, "getHeight") + mPaddingBetweenElements;
        }
    };

    private static final XC_MethodReplacement positionClockAndNotifications = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

            Object mClockPositionAlgorithm = XposedHelpers.getObjectField(mNotificationPanelView, "mClockPositionAlgorithm");
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            Object mKeyguardStatusView = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusView");
            Object mClockPositionResult = fieldClockPositionResult.get(mNotificationPanelView);
            Object mClockAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mClockAnimator");
            boolean animate = (boolean) XposedHelpers.callMethod(mNotificationStackScroller, "isAddOrRemoveAnimationPending");
            int stackScrollerPadding;
            if (getStatusBarState() != STATE_KEYGUARD) {
                stackScrollerPadding = (int) XposedHelpers.callMethod(mHeader, "getCollapsedHeight") + XposedHelpers.getIntField(mNotificationPanelView, "mQsPeekHeight");
                fieldTopPaddingAdjustment.setInt(mNotificationPanelView, 0);
            } else {
                try {
                    XposedHelpers.callMethod(mClockPositionAlgorithm, "setup",
                            XposedHelpers.callMethod(mStatusBar, "getMaxKeyguardNotifications"),
                            methodGetMaxPanelHeight.invoke(mNotificationPanelView),
                            methodGetExpandedHeight.invoke(mNotificationPanelView),
                            methodGetNotGoneChildCount.invoke(mNotificationStackScroller),
                            XposedHelpers.callMethod(mNotificationPanelView, "getHeight"),
                            XposedHelpers.callMethod(mKeyguardStatusView, "getHeight"),
                            XposedHelpers.getFloatField(mNotificationPanelView, "mEmptyDragAmount"));
                    XposedHelpers.callMethod(mClockPositionAlgorithm, "run", mClockPositionResult);
                } catch (NoSuchMethodError e) {//Xperia
                    XposedHelpers.callMethod(mNotificationPanelView, "positionKeyguardClockAndResize");
                }
                if (animate || mClockAnimator != null) {
                    XposedHelpers.callMethod(mNotificationPanelView, "startClockAnimation", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                } else {
                    XposedHelpers.callMethod(mKeyguardStatusView, "setY", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                }
                XposedHelpers.callMethod(mNotificationPanelView, "updateClock", XposedHelpers.getFloatField(mClockPositionResult, "clockAlpha"), XposedHelpers.getFloatField(mClockPositionResult, "clockScale"));
                stackScrollerPadding = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding");
                fieldTopPaddingAdjustment.setInt(mNotificationPanelView, XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPaddingAdjustment"));
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setIntrinsicPadding", stackScrollerPadding);
            XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", animate);
            return null;
        }
    };

    private static final XC_MethodReplacement setQsExpansion = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
            Object mQsNavbarScrim = XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mStackScrollerOverscrolling = fieldStackScrollerOverscrolling.getBoolean(mNotificationPanelView);
            boolean mLastAnnouncementWasQuickSettings = XposedHelpers.getBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings");
            boolean mTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mTracking");
            boolean isCollapsing = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isCollapsing");
            boolean mQsScrimEnabled = fieldQsScrimEnabled.getBoolean(mNotificationPanelView);
            float height = (float) param.args[0];
            height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
            fieldQsFullyExpanded.setBoolean(mNotificationPanelView, height == mQsMaxExpansionHeight && mQsMaxExpansionHeight != 0);
            if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
                methodSetQsExpanded.invoke(mNotificationPanelView, true);
            } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
                methodSetQsExpanded.invoke(mNotificationPanelView, false);
                if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing) {
                    XposedHelpers.callMethod(mNotificationPanelView, "announceForAccessibility", (boolean) XposedHelpers.callMethod(mNotificationPanelView, "getKeyguardOrLockScreenString"));
                    XposedHelpers.setBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings", false);
                }
            }
            fieldQsExpansionHeight.setFloat(mNotificationPanelView, height);
            updateQsExpansion();

            XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", false /* animate */);

            if (isOnKeyguard()) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");

            }
            if (getStatusBarState() == STATE_SHADE_LOCKED
                    || getStatusBarState() == STATE_KEYGUARD) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateKeyguardBottomAreaAlpha");
            }
            if (getStatusBarState() == STATE_SHADE && mQsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled) {
                XposedHelpers.callMethod(mQsNavbarScrim, "setAlpha", methodGetQsExpansionFraction.invoke(mNotificationPanelView));
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
            fieldScrollYOverride.setInt(mNotificationPanelView, -1);
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
            Object mAmbientState = XposedHelpers.getObjectField(mNotificationStackScroller, "mAmbientState");
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

    private static void updateQsExpansion() throws IllegalAccessException, InvocationTargetException {
        NotificationPanelHooks.getQsContainerHelper().setQsExpansion((float) methodGetQsExpansionFraction.invoke(mNotificationPanelView), (float) methodGetHeaderTranslation.invoke(mNotificationPanelView));
    }

    private static int getFirstItemMinHeight() throws IllegalAccessException, InvocationTargetException {
        final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
        int mCollapsedSize = fieldCollapsedSize.getInt(mNotificationStackScroller);
        return firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight") : mCollapsedSize;
    }

    private static float getExpandTranslationStart() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mTopPadding = fieldTopPadding.getInt(mNotificationStackScroller);
        int startPosition = 0;
        if (!fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) && !(boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager)) {
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
    private static float getAppearStartPosition() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        boolean trackingHeadsUp = fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) || (boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager);
        return trackingHeadsUp
                ? (int) methodGetTopHeadsUpHeight.invoke(mHeadsUpManager)
                : 0;
    }

    /**
     * @return the position from where the appear transition ends when expanding.
     * Measured in absolute height.
     */
    private static float getAppearEndPosition() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        boolean trackingHeadsUp = fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) || (boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager);
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mTopPadding = fieldTopPadding.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int firstItemHeight = trackingHeadsUp
                ? (int) methodGetTopHeadsUpHeight.invoke(mHeadsUpManager) + mBottomStackPeekSize
                + mBottomStackSlowDownHeight
                : getLayoutMinHeight();
        return firstItemHeight + (isOnKeyguard() ? mTopPadding : mIntrinsicPadding);
    }

    public static int getLayoutMinHeight() throws IllegalAccessException, InvocationTargetException {
        int firstChildMinHeight = getFirstChildIntrinsicHeight();
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                mMaxLayoutHeight - mIntrinsicPadding);
    }

    private static int getFirstChildIntrinsicHeight() throws IllegalAccessException, InvocationTargetException {
        final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
        final Object mEmptyShadeView = XposedHelpers.getObjectField(mNotificationStackScroller, "mEmptyShadeView");
        int mCollapsedSize = fieldCollapsedSize.getInt(mNotificationStackScroller);
        int mOwnScrollY = fieldOwnScrollY.getInt(mNotificationStackScroller);
        int firstChildMinHeight = firstChild != null
                ? (int) XposedHelpers.callMethod(firstChild, "getIntrinsicHeight")
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
    private static float getAppearFraction(float height) throws IllegalAccessException, InvocationTargetException {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (height - appearStartPosition)
                / (appearEndPosition - appearStartPosition);
    }

    private static void updateScrollStateForAddedChildren() throws IllegalAccessException, InvocationTargetException {
        int mPaddingBetweenElements = fieldPaddingBetweenElements.getInt(mNotificationStackScroller);
        int mOwnScrollY = fieldOwnScrollY.getInt(mNotificationStackScroller);
        ArrayList<View> mChildrenToAddAnimated = get(fieldChildrenToAddAnimated, mNotificationStackScroller);
        if (mChildrenToAddAnimated.isEmpty()) {
            return;
        }
        for (int i = 0; i < mNotificationStackScroller.getChildCount(); i++) {
            View child = mNotificationStackScroller.getChildAt(i);
            if (mChildrenToAddAnimated.contains(child)) {
                int startingPosition = (int) methodGetPositionInLinearLayout.invoke(mNotificationStackScroller, child);
                int padding = mPaddingBetweenElements;
                int childHeight = (int) methodGetIntrinsicHeight.invoke(mNotificationStackScroller, child) + padding;
                if (startingPosition < mOwnScrollY) {
                    // This child starts off screen, so let's keep it offscreen to keep the others visible
                    fieldOwnScrollY.setInt(mNotificationStackScroller, mOwnScrollY + childHeight);
                }
            }
        }
        methodClampScrollPosition.invoke(mNotificationStackScroller);
    }
}
