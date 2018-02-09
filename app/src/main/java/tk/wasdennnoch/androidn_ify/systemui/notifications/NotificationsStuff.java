package tk.wasdennnoch.androidn_ify.systemui.notifications;


import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformState;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformableView;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.getChildMeasureSpec;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.*;

public class NotificationsStuff {
    private static final String TAG = "NotificationsStuff";

    public static final int VISIBLE_TYPE_CONTRACTED = 0;
    public static final int VISIBLE_TYPE_EXPANDED = 1;
    public static final int VISIBLE_TYPE_HEADSUP = 2;
    public static final int VISIBLE_TYPE_SINGLELINE = 3;
    public static final int VISIBLE_TYPE_AMBIENT = 4;
    public static final int VISIBLE_TYPE_AMBIENT_SINGLELINE = 5;
    public static final int UNDEFINED = -1;

    public static Field fieldClipTopOptimization;

    public static Field fieldIsHeadsUp;
    public static Field fieldIsPinned;
    public static Field fieldShowingPublic;
    public static Field fieldSensitive;
    public static Field fieldHideSensitiveForIntrinsicHeight;
    public static Field fieldHeadsUpHeight;
    public static Field fieldMaxExpandHeight;
    public static Field fieldChildrenExpanded;
    public static Field fieldChildrenContainer;

    public static Field fieldDark;
    public static Field fieldAnimate;
    public static Field fieldClipTopAmount;
    public static Field fieldContractedChild;
    public static Field fieldExpandedChild;
    public static Field fieldHeadsUpChild;
    public static Field fieldClipBounds;

    public static void hook() {

        fieldClipTopOptimization = XposedHelpers.findField(SystemUI.ExpandableView, "mClipTopOptimization");

        fieldIsPinned = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mIsPinned");
        fieldIsHeadsUp = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mIsHeadsUp");
        fieldShowingPublic = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mShowingPublic");
        fieldSensitive = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mSensitive");
        fieldHideSensitiveForIntrinsicHeight = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mHideSensitiveForIntrinsicHeight");
        fieldHeadsUpHeight = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mHeadsUpHeight");
        fieldMaxExpandHeight = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mMaxExpandHeight");
        fieldChildrenExpanded = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mChildrenExpanded");
        fieldChildrenContainer = XposedHelpers.findField(SystemUI.ExpandableNotificationRow, "mChildrenContainer");

        fieldClipTopAmount = XposedHelpers.findField(SystemUI.NotificationContentView, "mClipTopAmount");
        fieldDark = XposedHelpers.findField(SystemUI.NotificationContentView, "mDark");
        fieldAnimate = XposedHelpers.findField(SystemUI.NotificationContentView, "mAnimate");
        fieldContractedChild = XposedHelpers.findField(SystemUI.NotificationContentView, "mContractedChild");
        fieldExpandedChild = XposedHelpers.findField(SystemUI.NotificationContentView, "mExpandedChild");
        fieldHeadsUpChild = XposedHelpers.findField(SystemUI.NotificationContentView, "mHeadsUpChild");
        fieldClipBounds = XposedHelpers.findField(SystemUI.NotificationContentView, "mClipBounds");

        NotificationContentHelper.initFields();
        ExpandableNotificationRowHelper.initFields();
        TransformState.initFields();

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                helper.onFinishInflate();
            }
        });
        XposedHelpers.findAndHookConstructor(SystemUI.NotificationContentView, Context.class, AttributeSet.class, constructorHook);

        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setContractedChild", View.class, XSetContractedChild);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setExpandedChild", View.class, XSetExpandedChild);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setHeadsUpChild", View.class, XSetHeadsUpChild);

        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "reset", boolean.class, XReset);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "selectLayout", boolean.class, boolean.class, XSelectLayout);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "onMeasure", int.class, int.class, XOnMeasure);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setVisible", boolean.class, XSetVisible);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setContentHeight", int.class, XSetContentHeight);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "updateClipping", XUpdateClipping);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "updateViewVisibilities", int.class, XUpdateViewVisibilities);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "runSwitchAnimation", int.class, XAnimateToVisibleType);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "calculateVisibleType", XCalculateVisibleType);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setDark", boolean.class, boolean.class, long.class, XSetDark);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setHeadsUp", boolean.class, setHeadsUpHook);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "setShowingLegacyBackground", boolean.class, setShowingLegacyBackgroundHook);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "notifyContentUpdated", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "updateRoundRectClipping", XC_MethodReplacement.DO_NOTHING);

        //XposedHelpers.findAndHookMethod(ExpandableView, "onMeasure", int.class, int.class, onMeasureExpandableView); //TODO: causes weird issues with clipping on Xperia
        //XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setStatusBarNotification", StatusBarNotification.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "applyExpansionToLayout", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateExpandButton", XC_MethodReplacement.DO_NOTHING);
        //XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "updateExpandButtonAppearance", XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "setCollapseClickListener", View.OnClickListener.class, XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "setClipTopAmount", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                invoke(ExpandableView.updateClipping, param.thisObject);
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "updateClipping", XUpdateClippingExpandableView);

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "getBoundsOnScreen", Rect.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                Rect outRect = (Rect) param.args[0];
                if (view.getTop() + view.getTranslationY() < 0) {
                    // We got clipped to the parent here - make sure we undo that.
                    outRect.top += view.getTop() + view.getTranslationY();
                }
                outRect.top += (int) invoke(ExpandableView.getClipTopAmount, param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "notifyHeightChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (SystemUI.ExpandableNotificationRow.isInstance(param.thisObject))
                    ExpandableNotificationRowHelper.getInstance(param.thisObject).notifyHeightChanged((boolean) param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationContentView, "getMinHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return NotificationContentHelper.getInstance(param.thisObject).getMinHeight();
            }
        });

        XposedHelpers.findAndHookConstructor(SystemUI.ExpandableNotificationRow, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                //mFalsingManager = FalsingManager.getInstance(context);
                XposedHelpers.setObjectField(param.thisObject, "mExpandClickListener", null);
                helper.initDimens();
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setUserLocked", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean userLocked = (boolean) param.args[0];
                ExpandableNotificationRowHelper.getInstance(param.thisObject).mPrivateHelper.setUserExpanding(userLocked);
                /*if (mIsSummaryWithChildren) {
                    mChildrenContainer.setUserLocked(userLocked);
                    if (userLocked || !isGroupExpanded()) {
                        updateBackgroundForGroupState();
                    }
                }*/
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setPinned", boolean.class, new XC_MethodHook() {
            /*@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "intrinsicHeight", XposedHelpers.callMethod(param.thisObject, "getIntrinsicHeight"));
            }*/

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean pinned = (boolean) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                //int intrinsicHeight = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "intrinsicHeight");
                fieldIsPinned.setBoolean(param.thisObject, pinned);
//                if (intrinsicHeight != (int) XposedHelpers.callMethod(param.thisObject, "getIntrinsicHeight")) {
//                    invoke(ExpandableNotificationRow.notifyHeightChanged, param.thisObject, false /* needsAnimation */); //TODO fix notification flashing
//                }
                if (pinned) {
                    XposedHelpers.callMethod(param.thisObject, "setIconAnimationRunning", true);
                    helper.mExpandedWhenPinned = false;
                } else if (helper.mExpandedWhenPinned) {
                    invoke(ExpandableNotificationRow.setUserExpanded, param.thisObject, true);
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getHeadsUpHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return ExpandableNotificationRowHelper.getInstance(param.thisObject).getPinnedHeadsUpHeight(true);
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getIntrinsicHeight", XGetIntrinsicHeight);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getMinHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return ExpandableNotificationRowHelper.getInstance(param.thisObject).getMinHeight();
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setExpandable", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).mPrivateHelper.updateExpandButtons((boolean) invoke(ExpandableNotificationRow.isExpandable, param.thisObject));
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setHideSensitive", boolean.class, boolean.class, long.class, long.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).mPrivateHelper.updateExpandButtons((boolean) invoke(ExpandableNotificationRow.isExpandable, param.thisObject));
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "reset", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).resetTranslation();
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "resetHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                View expandableRow = (View) param.thisObject;
                XposedHelpers.callMethod(expandableRow, "onHeightReset");
                expandableRow.requestLayout();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateChildrenVisibility", boolean.class, XUpdateChildrenVisibility);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setChildrenExpanded", boolean.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                set(fieldChildrenExpanded, param.thisObject, param.args[0]);
                ExpandableNotificationRowHelper.getInstance(param.thisObject).updateClickAndFocus();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateMaxHeights", XUpdateMaxHeights);
    }

    private static final XC_MethodReplacement XUpdateMaxHeights = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);

            int intrinsicBefore = invoke(ExpandableNotificationRow.getIntrinsicHeight, param.thisObject);
            View expandedChild = invoke(NotificationContentView.getExpandedChild, helper.mPrivateLayout);
            if (expandedChild == null) {
                expandedChild = invoke(NotificationContentView.getContractedChild, helper.mPrivateLayout);
            }
            set(fieldMaxExpandHeight, param.thisObject, expandedChild.getHeight());
            View headsUpChild = invoke(NotificationContentView.getHeadsUpChild, helper.mPrivateLayout);
            if (headsUpChild == null) {
                headsUpChild = invoke(NotificationContentView.getContractedChild, helper.mPrivateLayout);
            }
            set(fieldHeadsUpHeight, param.thisObject, headsUpChild.getHeight());
            if (intrinsicBefore != (int) invoke(ExpandableNotificationRow.getIntrinsicHeight, param.thisObject)) {
                invoke(ExpandableNotificationRow.notifyHeightChanged, param.thisObject, true /*needsAnimation*/);
            }
            return null;
        }
    };

    private static final XC_MethodReplacement XUpdateChildrenVisibility = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            View mChildrenContainer = get(fieldChildrenContainer, param.thisObject);
            boolean mShowingPublic = getBoolean(fieldShowingPublic, param.thisObject);
            helper.mPrivateLayout.setVisibility(!mShowingPublic/* && !helper.mIsSummaryWithChildren*/ ? VISIBLE
                    : INVISIBLE);
                /*if (mChildrenContainer != null) {
                    mChildrenContainer.setVisibility(!mShowingPublic && mIsSummaryWithChildren ? VISIBLE
                            : INVISIBLE);
                    mChildrenContainer.updateHeaderVisibility(!mShowingPublic && mIsSummaryWithChildren
                            ? VISIBLE
                            : INVISIBLE);
                }*/
            mChildrenContainer.setVisibility(GONE);
            // The limits might have changed if the view suddenly became a group or vice versa
            helper.updateLimits();
            return null;
        }
    };

    private static final XC_MethodReplacement XGetIntrinsicHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (invoke(ExpandableNotificationRow.isUserLocked, param.thisObject)) {
                return invoke(ExpandableNotificationRow.getActualHeight, param.thisObject);
            }
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            int mHeadsUpHeight = getInt(fieldHeadsUpHeight, param.thisObject);
                /*if (mGuts != null && mGuts.areGutsExposed()) {
                    return mGuts.getHeight();
                } else if ((isChildInGroup() && !isGroupExpanded())) {
                    return mPrivateLayout.getMinHeight();
                } else */if (getBoolean(fieldSensitive, param.thisObject) && getBoolean(fieldHideSensitiveForIntrinsicHeight, param.thisObject)) {
                return helper.getMinHeight();
                /*} else if (mIsSummaryWithChildren && !mOnKeyguard) {
                    return mChildrenContainer.getIntrinsicHeight();*/
            } else if (fieldIsHeadsUp.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                if (fieldIsPinned.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                    return helper.getPinnedHeadsUpHeight(true  /*atLeastMinHeight*/ );
                } else if (invoke(ExpandableNotificationRow.isExpanded, param.thisObject)) {
                    return Math.max((int) invoke(ExpandableNotificationRow.getMaxExpandHeight, param.thisObject), mHeadsUpHeight);
                } else {
                    return Math.max(helper.getCollapsedHeight(), mHeadsUpHeight);
                }
            } else if (invoke(ExpandableNotificationRow.isExpanded, param.thisObject)) {
                return invoke(ExpandableNotificationRow.getMaxExpandHeight, param.thisObject);
            } else {
                return helper.getCollapsedHeight();
            }
        }
    };

    private static final XC_MethodReplacement XUpdateClippingExpandableView = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            Rect mClipRect = (Rect) XposedHelpers.getStaticObjectField(SystemUI.ExpandableView, "mClipRect");
            View view = (View) param.thisObject;
            if (helper.mClipToActualHeight) {
                //int top = (int) XposedHelpers.callMethod(view, "getClipTopAmount"); //TODO see why it doesn't work properly
                int top = getInt(fieldClipTopOptimization, view);
                int actualHeight = invoke(ExpandableView.getActualHeight, view);
                if (top >= actualHeight) {
                    top = actualHeight - 1;
                }
                mClipRect.set(0, top, view.getWidth(), actualHeight/* + getExtraBottomPadding()*/); //TODO implement
                view.setClipBounds(mClipRect);
            } else {
                view.setClipBounds(null);
            }
            return null;
        }
    };

    private static final XC_MethodReplacement XOnMeasureExpandableView = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            ViewGroup expandableView = (ViewGroup) param.thisObject;
            ArrayList<View> mMatchParentViews = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mMatchParentViews");
            int widthMeasureSpec = (int) param.args[0];
            int heightMeasureSpec = (int) param.args[1];
            final int givenSize = View.MeasureSpec.getSize(heightMeasureSpec);
            int ownMaxHeight = Integer.MAX_VALUE;
            int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            if (heightMode != View.MeasureSpec.UNSPECIFIED && givenSize != 0) {
                ownMaxHeight = Math.min(givenSize, ownMaxHeight);
            }
            int newHeightSpec = View.MeasureSpec.makeMeasureSpec(ownMaxHeight, View.MeasureSpec.AT_MOST);
            int maxChildHeight = 0;
            int childCount = expandableView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = expandableView.getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                int childHeightSpec = newHeightSpec;
                ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
                if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    if (layoutParams.height >= 0) {
                        // An actual height is set
                        childHeightSpec = layoutParams.height > ownMaxHeight
                                ? View.MeasureSpec.makeMeasureSpec(ownMaxHeight, View.MeasureSpec.EXACTLY)
                                : View.MeasureSpec.makeMeasureSpec(layoutParams.height, View.MeasureSpec.EXACTLY);
                    }
                    child.measure(
                            getChildMeasureSpec(widthMeasureSpec, 0 /* padding */, layoutParams.width),
                            childHeightSpec);
                    int childHeight = child.getMeasuredHeight();
                    maxChildHeight = Math.max(maxChildHeight, childHeight);
                } else {
                    mMatchParentViews.add(child);
                }
            }
            int ownHeight = heightMode == View.MeasureSpec.EXACTLY
                    ? givenSize : Math.min(ownMaxHeight, maxChildHeight);
            newHeightSpec = View.MeasureSpec.makeMeasureSpec(ownHeight, View.MeasureSpec.EXACTLY);
            for (View child : mMatchParentViews) {
                child.measure(getChildMeasureSpec(
                        widthMeasureSpec, 0 /* padding */, child.getLayoutParams().width),
                        newHeightSpec);
            }
            mMatchParentViews.clear();
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            XposedHelpers.callMethod(param.thisObject, "setMeasuredDimension", width, ownHeight);
            return null;
        }
    };

    private static XC_MethodReplacement XReset = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            FrameLayout contentView = (FrameLayout) param.thisObject;
            NotificationContentHelper helper = NotificationContentHelper.getInstance(contentView);

            View mContractedChild = helper.mContractedChild;
            View mExpandedChild = helper.mExpandedChild;
            View mHeadsUpChild = helper.mHeadsUpChild;
            XposedHook.logD(TAG, "reset called");

            if (mContractedChild != null) {
                mContractedChild.animate().cancel();
                contentView.removeView(mContractedChild);
            }
            helper.mPreviousExpandedRemoteInputIntent = null;
            XposedHook.logD(TAG, "contentView: " + helper.getContentView());
            XposedHook.logD(TAG, "helper.mExpandedRemoteInput != null: " + (helper.mExpandedRemoteInput != null));
            if (helper.mExpandedRemoteInput != null) {
                helper.mExpandedRemoteInput.onNotificationUpdateOrReset();
                XposedHook.logD(TAG, "helper.mExpandedRemoteInput.isActive(): " + helper.mExpandedRemoteInput.isActive());
                if (helper.mExpandedRemoteInput.isActive()) {
                    XposedHook.logD(TAG, "helper.mExpandedRemoteInput.isActive()");
                    helper.mPreviousExpandedRemoteInputIntent = helper.mExpandedRemoteInput.getPendingIntent();
                    helper.mCachedExpandedRemoteInput = helper.mExpandedRemoteInput;
                    helper.mExpandedRemoteInput.dispatchStartTemporaryDetach();
                    ((ViewGroup)helper.mExpandedRemoteInput.getParent()).removeView(helper.mExpandedRemoteInput);
                }
            }
            if (mExpandedChild != null) {
                mExpandedChild.animate().cancel();
                contentView.removeView(mExpandedChild);
                helper.mExpandedRemoteInput = null;
            }
            helper.mPreviousHeadsUpRemoteInputIntent = null;
            if (helper.mHeadsUpRemoteInput != null) {
                helper.mHeadsUpRemoteInput.onNotificationUpdateOrReset();
                if (helper.mHeadsUpRemoteInput.isActive()) {
                    helper.mPreviousHeadsUpRemoteInputIntent = helper.mHeadsUpRemoteInput.getPendingIntent();
                    helper.mCachedHeadsUpRemoteInput = helper.mHeadsUpRemoteInput;
                    helper.mHeadsUpRemoteInput.dispatchStartTemporaryDetach();
                    ((ViewGroup)helper.mHeadsUpRemoteInput.getParent()).removeView(helper.mHeadsUpRemoteInput);
                }
            }
            if (mHeadsUpChild != null) {
                mHeadsUpChild.animate().cancel();
                contentView.removeView(mHeadsUpChild);
                helper.mHeadsUpRemoteInput = null;
            }
            set(fieldContractedChild, contentView, null);
            helper.mContractedChild = null;
            set(fieldExpandedChild, contentView, null);
            helper.mExpandedChild = null;
            set(fieldHeadsUpChild, contentView, null);
            helper.mHeadsUpChild = null;
            return null;
        }
    };

    public static void setChangingPosition(Object view, boolean changingPosition) {
        XposedHelpers.setAdditionalInstanceField(view, "mChangingPosition", changingPosition);
    }

    public static boolean isChangingPosition(Object view) {
        Object changingPosition = XposedHelpers.getAdditionalInstanceField(view, "mChangingPosition");
        return changingPosition != null && (boolean) changingPosition;
    }

    private static XC_MethodHook constructorHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            helper.init();
            helper.getContentView().setOutlineProvider(null);
        }
    };

    private static XC_MethodReplacement XAnimateToVisibleType = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(final MethodHookParam param) throws Throwable {
            int visibleType = (int) param.args[0];
            final NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            final TransformableView shownView = helper.getTransformableViewForVisibleType(visibleType);
            final TransformableView hiddenView = helper.getTransformableViewForVisibleType(helper.getVisibleType());
            if (shownView == hiddenView || hiddenView == null) {
                shownView.setVisible(true);
                return null;
            }
            helper.mAnimationStartVisibleType = helper.getVisibleType();
            shownView.transformFrom(hiddenView);
            helper.getViewForVisibleType(visibleType).setVisibility(VISIBLE);
            hiddenView.transformTo(shownView, new Runnable() {
                @Override
                public void run() {
                    if (hiddenView != helper.getTransformableViewForVisibleType(helper.getVisibleType())) {
                        hiddenView.setVisible(false);
                    }
                    helper.mAnimationStartVisibleType = UNDEFINED;
                }
            });
            //helper.fireExpandedVisibleListenerIfVisible();
            return null;
        }
    };

    private static XC_MethodReplacement XSetContractedChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            if (helper.mContractedChild != null) {
                helper.mContractedChild.animate().cancel();
                helper.getContentView().removeView(helper.mContractedChild);
            }
            helper.getContentView().addView(child);
            set(fieldContractedChild, param.thisObject, child);
            helper.mContractedChild = child;
            helper.mContractedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);
            helper.mContractedWrapper.setDark(getBoolean(fieldDark, param.thisObject), false  /*animate*/ , 0  /*delay*/ );
            return null;
        }
    };

    private static XC_MethodReplacement XSetExpandedChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);

            if (helper.mExpandedChild != null) {
                helper.mExpandedChild.animate().cancel();
                helper.getContentView().removeView(helper.mExpandedChild);
            }
            helper.getContentView().addView(child);
            set(fieldExpandedChild, param.thisObject, child);
            helper.mExpandedChild = child;
            helper.mExpandedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);
            /*if (helper.mExpandedChild != null) {
                helper.mPreviousExpandedRemoteInputIntent = null;
                if (helper.mExpandedRemoteInput != null) {
                    helper.mExpandedRemoteInput.onNotificationUpdateOrReset();
                    if (helper.mExpandedRemoteInput.isActive()) {
                        helper.mPreviousExpandedRemoteInputIntent = helper.mExpandedRemoteInput.getPendingIntent();
                        helper.mCachedExpandedRemoteInput = helper.mExpandedRemoteInput;
                        helper.mExpandedRemoteInput.dispatchStartTemporaryDetach();
                        ((ViewGroup)helper.mExpandedRemoteInput.getParent()).removeView(helper.mExpandedRemoteInput);
                    }
                }
                helper.mExpandedChild.animate().cancel();
                helper.getContentView().removeView(helper.mExpandedChild);
                helper.mExpandedRemoteInput = null;
            }
            if (child == null) {
                helper.mExpandedChild = null;
                helper.mExpandedWrapper = null;
                if (XposedHelpers.getIntField(helper.getContentView(), "mVisibleType") == NotificationContentHelper.VISIBLE_TYPE_EXPANDED) {
                    XposedHelpers.setIntField(helper.getContentView(), "mVisibleType", NotificationContentHelper.VISIBLE_TYPE_CONTRACTED);
                }
                if (helper.mTransformationStartVisibleType == NotificationContentHelper.VISIBLE_TYPE_EXPANDED) {
                    helper.mTransformationStartVisibleType = UNDEFINED;
                }
                return null;
            }
            helper.getContentView().addView(child);
            helper.mExpandedChild = child;
            helper.mExpandedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);*/
            return null;
        }
    };

    private static XC_MethodReplacement XSetHeadsUpChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);

            if (helper.mHeadsUpChild != null) {
                helper.mHeadsUpChild.animate().cancel();
                helper.getContentView().removeView(helper.mHeadsUpChild);
            }
            helper.getContentView().addView(child);
            set(fieldHeadsUpChild, param.thisObject, child);
            helper.mHeadsUpChild = child;
            helper.mHeadsUpWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);
            /*if (helper.mHeadsUpChild != null) {
                helper.mPreviousHeadsUpRemoteInputIntent = null;
                if (helper.mHeadsUpRemoteInput != null) {
                    helper.mHeadsUpRemoteInput.onNotificationUpdateOrReset();
                    if (helper.mHeadsUpRemoteInput.isActive()) {
                        helper.mPreviousHeadsUpRemoteInputIntent = helper.mHeadsUpRemoteInput.getPendingIntent();
                        helper.mCachedHeadsUpRemoteInput = helper.mHeadsUpRemoteInput;
                        helper.mHeadsUpRemoteInput.dispatchStartTemporaryDetach(); //TODO see what to do with this
                        ((ViewGroup)helper.mHeadsUpRemoteInput.getParent()).removeView(helper.mHeadsUpRemoteInput);
                    }
                }
                helper.mHeadsUpChild.animate().cancel();
                helper.getContentView().removeView(helper.mHeadsUpChild);
                helper.mHeadsUpRemoteInput = null;
            }
            if (child == null) {
                helper.mHeadsUpChild = null;
                helper.mHeadsUpWrapper = null;
                if (XposedHelpers.getIntField(helper.getContentView(), "mVisibleType") == NotificationContentHelper.VISIBLE_TYPE_HEADSUP) {
                    XposedHelpers.setIntField(helper.getContentView(), "mVisibleType", NotificationContentHelper.VISIBLE_TYPE_CONTRACTED);
                }
                if (helper.mTransformationStartVisibleType == NotificationContentHelper.VISIBLE_TYPE_HEADSUP) {
                    helper.mTransformationStartVisibleType = UNDEFINED;
                }
                return null;
            }
            helper.getContentView().addView(child);
            helper.mHeadsUpChild = child;
            helper.mHeadsUpWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);*/
            return null;
        }
    };

    private static XC_MethodReplacement XSetVisible = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            boolean isVisible = (boolean) param.args[0];
            if (isVisible) {
                // This call can happen multiple times, but removing only removes a single one.
                // We therefore need to remove the old one.
                helper.getContentView().getViewTreeObserver().removeOnPreDrawListener(helper.mEnableAnimationPredrawListener);
                // We only animate if we are drawn at least once, otherwise the view might animate when
                // it's shown the first time
                helper.getContentView().getViewTreeObserver().addOnPreDrawListener(helper.mEnableAnimationPredrawListener);
            } else {
                helper.getContentView().getViewTreeObserver().removeOnPreDrawListener(helper.mEnableAnimationPredrawListener);
                set(fieldAnimate, helper.getContentView(), false);
            }
            return null;
        }
    };

    private static XC_MethodReplacement XOnMeasure = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int widthMeasureSpec = (int) param.args[0];
            int heightMeasureSpec = (int) param.args[1];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            boolean hasFixedHeight = heightMode == View.MeasureSpec.EXACTLY;
            boolean isHeightLimited = heightMode == View.MeasureSpec.AT_MOST;
            int maxSize = Integer.MAX_VALUE;
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            if (hasFixedHeight || isHeightLimited) {
                maxSize = View.MeasureSpec.getSize(heightMeasureSpec);
            }
            int maxChildHeight = 0;
            if (helper.mExpandedChild != null) {
                int size = Math.min(maxSize, helper.mNotificationMaxHeight);
                ViewGroup.LayoutParams layoutParams = helper.mExpandedChild.getLayoutParams();
                boolean useExactly = false;
                if (layoutParams.height >= 0) {
                    // An actual height is set
                    size = Math.min(maxSize, layoutParams.height);
                    useExactly = true;
                }
                int spec = size == Integer.MAX_VALUE
                        ? View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        : View.MeasureSpec.makeMeasureSpec(size, useExactly
                        ? View.MeasureSpec.EXACTLY
                        : View.MeasureSpec.AT_MOST);
                helper.mExpandedChild.measure(widthMeasureSpec, spec);
                maxChildHeight = Math.max(maxChildHeight, helper.mExpandedChild.getMeasuredHeight());
            }
            if (helper.mContractedChild != null) {
                int heightSpec;
                int size = Math.min(maxSize, helper.mSmallHeight);
                if (helper.shouldContractedBeFixedSize()) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
                } else {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST);
                }
                helper.mContractedChild.measure(widthMeasureSpec, heightSpec);
                int measuredHeight = helper.mContractedChild.getMeasuredHeight();
                if (measuredHeight < helper.mMinContractedHeight) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(helper.mMinContractedHeight, View.MeasureSpec.EXACTLY);
                    helper.mContractedChild.measure(widthMeasureSpec, heightSpec);
                }
                maxChildHeight = Math.max(maxChildHeight, measuredHeight);
                if (helper.updateContractedHeaderWidth()) {
                    helper.mContractedChild.measure(widthMeasureSpec, heightSpec);
                }
                if (helper.mExpandedChild != null
                        && helper.mContractedChild.getMeasuredHeight() > helper.mExpandedChild.getMeasuredHeight()) {
                    // the Expanded child is smaller then the collapsed. Let's remeasure it.
                    heightSpec = View.MeasureSpec.makeMeasureSpec(helper.mContractedChild.getMeasuredHeight(),
                            View.MeasureSpec.EXACTLY);
                    helper.mExpandedChild.measure(widthMeasureSpec, heightSpec);
                }
            }
            if (helper.mHeadsUpChild != null) {
                int size = Math.min(maxSize, helper.mHeadsUpHeight);
                ViewGroup.LayoutParams layoutParams = helper.mHeadsUpChild.getLayoutParams();
                boolean useExactly = false;
                if (layoutParams.height >= 0) {
                    // An actual height is set
                    size = Math.min(size, layoutParams.height);
                    useExactly = true;
                }
                helper.mHeadsUpChild.measure(widthMeasureSpec,
                        View.MeasureSpec.makeMeasureSpec(size, useExactly ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST));
                maxChildHeight = Math.max(maxChildHeight, helper.mHeadsUpChild.getMeasuredHeight());
            }
            if (helper.mSingleLineView != null) {
                int singleLineWidthSpec = widthMeasureSpec;
                if (helper.mSingleLineWidthIndention != 0
                        && View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED) {
                    singleLineWidthSpec = View.MeasureSpec.makeMeasureSpec(
                            width - helper.mSingleLineWidthIndention + helper.mSingleLineView.getPaddingEnd(),
                            View.MeasureSpec.EXACTLY);
                }
                helper.mSingleLineView.measure(singleLineWidthSpec,
                        View.MeasureSpec.makeMeasureSpec(maxSize, View.MeasureSpec.AT_MOST));
                maxChildHeight = Math.max(maxChildHeight, helper.mSingleLineView.getMeasuredHeight());
            }
            /*if (helper.mAmbientChild != null) {
                int size = Math.min(maxSize, helper.mNotificationAmbientHeight);
                ViewGroup.LayoutParams layoutParams = helper.mAmbientChild.getLayoutParams();
                boolean useExactly = false;
                if (layoutParams.height >= 0) {
                    // An actual height is set
                    size = Math.min(size, layoutParams.height);
                    useExactly = true;
                }
                helper.mAmbientChild.measure(widthMeasureSpec,
                        View.MeasureSpec.makeMeasureSpec(size, useExactly ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST));
                maxChildHeight = Math.max(maxChildHeight, helper.mAmbientChild.getMeasuredHeight());
            }
            if (helper.mAmbientSingleLineChild != null) {
                int size = Math.min(maxSize, helper.mNotificationAmbientHeight);
                ViewGroup.LayoutParams layoutParams = helper.mAmbientSingleLineChild.getLayoutParams();
                boolean useExactly = false;
                if (layoutParams.height >= 0) {
                    // An actual height is set
                    size = Math.min(size, layoutParams.height);
                    useExactly = true;
                }
                int ambientSingleLineWidthSpec = widthMeasureSpec;
                if (helper.mSingleLineWidthIndention != 0
                        && View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED) {
                    ambientSingleLineWidthSpec = View.MeasureSpec.makeMeasureSpec(
                            width - helper.mSingleLineWidthIndention + helper.mAmbientSingleLineChild.getPaddingEnd(),
                            View.MeasureSpec.EXACTLY);
                }
                helper.mAmbientSingleLineChild.measure(ambientSingleLineWidthSpec,
                        View.MeasureSpec.makeMeasureSpec(size, useExactly ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST));
                maxChildHeight = Math.max(maxChildHeight, helper.mAmbientSingleLineChild.getMeasuredHeight());
            }*/
            int ownHeight = Math.min(maxChildHeight, maxSize);

            XposedHelpers.callMethod(helper.getContentView(), "setMeasuredDimension", width, ownHeight);
            return null;
        }
    };

    private static XC_MethodHook onLayoutHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int previousHeight = 0;
            if (helper.mExpandedChild != null) {
                previousHeight = helper.mExpandedChild.getHeight();
            }
            helper.previousHeight = previousHeight;
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int previousHeight = helper.previousHeight;

            if (previousHeight != 0 && helper.mExpandedChild.getHeight() != previousHeight) {
                helper.mContentHeightAtAnimationStart = previousHeight;
            }
            invoke(NotificationContentView.selectLayout, helper.getContentView(), false  /*animate*/, helper.mForceSelectNextLayout  /*force*/ );
            helper.mForceSelectNextLayout = false;
            helper.updateExpandButtons(helper.mExpandable);
        }
    };

    private static XC_MethodReplacement XSetContentHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int contentHeight = (int) param.args[0];
            set(NotificationContentHelper.fieldContentHeight, helper.getContentView(), Math.max(Math.min(contentHeight, helper.getContentView().getHeight()), helper.getMinHeight()));

            invoke(NotificationContentView.selectLayout, helper.getContentView(),
                    getBoolean(fieldAnimate, helper.getContentView()) /*animate*/ , false  /*force*/ );

            int minHeightHint = helper.getMinContentHeightHint();

            NotificationViewWrapper wrapper = helper.getVisibleWrapper(helper.getVisibleType());
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            wrapper = helper.getVisibleWrapper(helper.mTransformationStartVisibleType);
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            invoke(NotificationContentView.updateClipping, helper.getContentView());
            helper.getContentView().invalidateOutline();
            return null;
        }
    };

    private static XC_MethodReplacement XSelectLayout = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean animate = (boolean) param.args[0];
            boolean force = (boolean) param.args[1];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            if (helper.mContractedChild == null) {
                return null;
            }
            if (helper.mUserExpanding) {
                helper.updateContentTransformation();
            } else {
                int visibleType = invoke(NotificationContentView.calculateVisibleType, helper.getContentView());
                boolean changedType = visibleType != helper.getVisibleType();
                if (changedType || force) {
                    View visibleView = helper.getViewForVisibleType(visibleType);
                    if (visibleView != null) {
                        visibleView.setVisibility(VISIBLE);
                        helper.transferRemoteInputFocus(visibleType);
                    }

                    if (animate && ((visibleType == VISIBLE_TYPE_EXPANDED && helper.mExpandedChild != null)
                            || (visibleType == VISIBLE_TYPE_HEADSUP && helper.mHeadsUpChild != null)
                            || (visibleType == VISIBLE_TYPE_SINGLELINE && helper.mSingleLineView != null)
                            || visibleType == VISIBLE_TYPE_CONTRACTED)) {
                        invoke(NotificationContentView.runSwitchAnimation, helper.getContentView(), visibleType);
                    } else {
                        invoke(NotificationContentView.updateViewVisibilities, helper.getContentView(), visibleType);
                    }
                    set(NotificationContentHelper.fieldVisibleType, helper.getContentView(), visibleType);
                    if (changedType) {
                        helper.focusExpandButtonIfNecessary();
                    }
                    NotificationViewWrapper visibleWrapper = helper.getVisibleWrapper(visibleType);
                    if (visibleWrapper != null) {
                        visibleWrapper.setContentHeight(helper.getContentHeight(), helper.getMinContentHeightHint());
                    }
                    helper.updateBackgroundColor(animate);
                }
            }
            return null;
        }
    };

    private static XC_MethodReplacement XUpdateViewVisibilities = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int visibleType = (int) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);

            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_CONTRACTED,
                    helper.mContractedChild, helper.mContractedWrapper);
            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_EXPANDED,
                    helper.mExpandedChild, helper.mExpandedWrapper);
            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_HEADSUP,
                    helper.mHeadsUpChild, helper.mHeadsUpWrapper);
            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_SINGLELINE,
                    helper.mSingleLineView, helper.mSingleLineView);
//            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_AMBIENT,
//                    helper.mAmbientChild, helper.mAmbientWrapper);
//            helper.updateViewVisibility(visibleType, VISIBLE_TYPE_AMBIENT_SINGLELINE,
//                    helper.mAmbientSingleLineChild, helper.mAmbientSingleLineChild);
            //helper.fireExpandedVisibleListenerIfVisible();
            // updateViewVisibilities cancels outstanding animations without updating the
            // mAnimationStartVisibleType. Do so here instead.
            helper.mAnimationStartVisibleType = UNDEFINED;
            return null;
        }
    };

    private static XC_MethodReplacement XCalculateVisibleType = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            /*if (helper.mContainingNotification.isShowingAmbient()) {
                if (helper.mIsChildInGroup && helper.mAmbientSingleLineChild != null) {
                    return NotificationContentHelper.VISIBLE_TYPE_AMBIENT_SINGLELINE;
                } else if (helper.mAmbientChild != null) {
                    return NotificationContentHelper.VISIBLE_TYPE_AMBIENT;
                } else {
                    return NotificationContentHelper.VISIBLE_TYPE_CONTRACTED;
                }
            }*/
            if (helper.mUserExpanding) {
                int height = !helper.mIsChildInGroup || helper.isGroupExpanded()
                        || (boolean) invoke(ExpandableNotificationRow.isExpanded, helper.mContainingNotification/*, (true  *//*allowOnKeyguard*//* */) //TODO finish implementing
                        ? (int) invoke(ExpandableNotificationRow.getMaxContentHeight, helper.mContainingNotification)
                        : helper.getContainingHelper().getShowingHelper().getMinHeight();
                if (height == 0) {
                    height = helper.getContentHeight();
                }
                int expandedVisualType = helper.getVisualTypeForHeight(height);
                int collapsedVisualType = helper.mIsChildInGroup && !helper.isGroupExpanded()
                        ? VISIBLE_TYPE_SINGLELINE
                        : helper.getVisualTypeForHeight((helper.getContainingHelper().getCollapsedHeight()));
                return helper.mTransformationStartVisibleType == collapsedVisualType
                        ? expandedVisualType
                        : collapsedVisualType;
            }
            int intrinsicHeight = invoke(ExpandableNotificationRow.getIntrinsicHeight, helper.mContainingNotification);
            int viewHeight = helper.getContentHeight();
            if (intrinsicHeight != 0) {
                // the intrinsicHeight might be 0 because it was just reset.
                viewHeight = Math.min(helper.getContentHeight(), intrinsicHeight);
            }
            return helper.getVisualTypeForHeight(viewHeight);
        }
    };

    private static XC_MethodReplacement XIsContentExpandable = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return NotificationContentHelper.getInstance(param.thisObject).mIsContentExpandable;
        }
    };

    private static XC_MethodReplacement XSetDark = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean dark = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            long delay = (long) param.args[2];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int visibleType = helper.getVisibleType();
            if (helper.mContractedChild == null) {
                return null;
            }
            set(fieldDark, helper.getContentView(), dark);
            if (visibleType == VISIBLE_TYPE_CONTRACTED || !dark) {
                helper.mContractedWrapper.setDark(dark, fade, delay);
            }
            if (visibleType == VISIBLE_TYPE_EXPANDED || (helper.mExpandedChild != null && !dark)) {
                helper.mExpandedWrapper.setDark(dark, fade, delay);
            }
            if (visibleType == VISIBLE_TYPE_HEADSUP || (helper.mHeadsUpChild != null && !dark)) {
                helper.mHeadsUpWrapper.setDark(dark, fade, delay);
            }
            if (helper.mSingleLineView != null && (visibleType == VISIBLE_TYPE_SINGLELINE || !dark)) {
                helper.mSingleLineView.setDark(dark, fade, delay);
            }
            //XposedHelpers.callMethod(helper.getContentView(), "selectLayout", !dark && fade  /*animate*/ , false  /*force*/ );
            return null;
        }
    };

    private static XC_MethodHook setHeadsUpHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            helper.updateExpandButtons(helper.mExpandable);
        }
    };

    private static XC_MethodReplacement XUpdateClipping = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            if (helper.mClipToActualHeight) {
                int top = getInt(fieldClipTopAmount, helper.getContentView());
                int bottom = helper.getContentHeight();
                ((Rect) get(fieldClipBounds, helper.getContentView())).set(0, top, helper.getContentView().getWidth(), bottom);
                helper.getContentView().setClipBounds((Rect) get(fieldClipBounds, helper.getContentView()));
            } else {
                helper.getContentView().setClipBounds(null);
            }
            return null;
        }
    };

    private static XC_MethodHook setShowingLegacyBackgroundHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper.getInstance(param.thisObject).updateShowingLegacyBackground();
        }
    };

    public static void setRemoteInputActive(Object headsUpManager, Object entry, boolean remoteInputActive) {
        HashMap<String, Object> mHeadsUpEntries = (HashMap<String, Object>) XposedHelpers.getObjectField(headsUpManager, "mHeadsUpEntries");
        Object headsUpEntry = mHeadsUpEntries.get(XposedHelpers.getObjectField(entry, "key"));
        if (headsUpEntry != null) {
            Object isRemoteInputActive = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "remoteInputActive");
            boolean headsUpRemoteInputActive = isRemoteInputActive != null && (boolean) isRemoteInputActive;
            if (headsUpRemoteInputActive != remoteInputActive) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "remoteInputActive", remoteInputActive);
                if (remoteInputActive) {
                    XposedHelpers.callMethod(headsUpEntry, "removeAutoRemovalCallbacks");
                } else {
                    updateEntry(headsUpManager, headsUpEntry, false /* updatePostTime */);
                }
            }
        }
    }

    /**
     * Set an entry to be expanded and therefore stick in the heads up area if it's pinned
     * until it's collapsed again.
     */
    public static void setExpanded(Object headsUpManager, Object entry, boolean expanded) {
        HashMap<String, Object> mHeadsUpEntries = (HashMap<String, Object>) XposedHelpers.getObjectField(headsUpManager, "mHeadsUpEntries");
        Object headsUpEntry = mHeadsUpEntries.get(XposedHelpers.getObjectField(entry, "key"));
        if (headsUpEntry != null) {
            Object isExpanded = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "expanded");
            boolean headsUpExpanded = isExpanded != null && (boolean) isExpanded;
            if (headsUpExpanded != expanded) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "expanded", expanded);
                if (expanded) {
                    XposedHelpers.callMethod(headsUpEntry, "removeAutoRemovalCallbacks");
                } else {
                    updateEntry(headsUpManager, headsUpEntry, false /* updatePostTime */);
                }
            }
        }
    }

    public static void updateEntry(Object headsUpManager, Object entry, boolean updatePostTime) {
        Object mClock = XposedHelpers.getObjectField(headsUpManager, "mClock");
        HashSet mEntriesToRemoveAfterExpand = (HashSet) XposedHelpers.getObjectField(headsUpManager, "mEntriesToRemoveAfterExpand");
        Handler mHandler = (Handler) XposedHelpers.getObjectField(headsUpManager, "mHandler");
        int mMinimumDisplayTime = XposedHelpers.getIntField(headsUpManager, "mMinimumDisplayTime");
        long postTime = XposedHelpers.getLongField(entry, "postTime");
        long currentTime = (long) XposedHelpers.callMethod(mClock, "currentTimeMillis");
        XposedHelpers.setLongField(entry, "earliestRemovaltime", currentTime + mMinimumDisplayTime);
        if (updatePostTime) {
            XposedHelpers.setLongField(entry, "postTime", Math.max(postTime, currentTime));
            postTime = Math.max(postTime, currentTime);
        }
        XposedHelpers.callMethod(entry, "removeAutoRemovalCallbacks");
        if (mEntriesToRemoveAfterExpand.contains(entry)) {
            mEntriesToRemoveAfterExpand.remove(entry);
        }
        if (!isSticky(headsUpManager, entry)) {
            long finishTime = postTime + XposedHelpers.getIntField(headsUpManager, "mHeadsUpNotificationDecay");
            long removeDelay = Math.max(finishTime - currentTime, mMinimumDisplayTime);
            mHandler.postDelayed((Runnable) XposedHelpers.getObjectField(entry, "mRemoveHeadsUpRunnable"), removeDelay);
        }
    }

    private static boolean isSticky(Object headsUpManager, Object headsUpEntry) {
        Object entry = XposedHelpers.getObjectField(headsUpEntry, "entry");
        Object isExpanded = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "expanded");
        boolean expanded = isExpanded != null && (boolean) isExpanded;
        Object isRemoteInputActive = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "remoteInputActive");
        boolean remoteInputActive = isRemoteInputActive != null && (boolean) isRemoteInputActive;
        boolean isPinned = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(entry, "row"), "isPinned");

        return (isPinned && expanded)
                || remoteInputActive || (boolean) XposedHelpers.callMethod(headsUpManager, "hasFullScreenIntent", entry);
    }

}

