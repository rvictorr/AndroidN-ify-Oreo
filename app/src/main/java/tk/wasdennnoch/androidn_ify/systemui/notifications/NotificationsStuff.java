package tk.wasdennnoch.androidn_ify.systemui.notifications;


import android.app.Notification;
import android.content.Context;
import android.graphics.Rect;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformState;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformableView;
import tk.wasdennnoch.androidn_ify.utils.Classes;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.getChildMeasureSpec;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;

public class NotificationsStuff {
    private static final String TAG = "NotificationsStuff";

    public static final int VISIBLE_TYPE_CONTRACTED = 0;
    public static final int VISIBLE_TYPE_EXPANDED = 1;
    public static final int VISIBLE_TYPE_HEADSUP = 2;
    public static final int VISIBLE_TYPE_SINGLELINE = 3;
    public static final int VISIBLE_TYPE_AMBIENT = 4;
    public static final int VISIBLE_TYPE_AMBIENT_SINGLELINE = 5;
    public static final int UNDEFINED = -1;

    public static Field fieldIsHeadsUp;
    public static Field fieldIsPinned;

    public static Method methodGetClipTopAmount;
    public static Method methodUpdateClipping;
    public static Method methodGetActualHeight;

    public static Method methodGetExpandedChild;
    public static Method methodGetContractedChild;
    public static Method methodGetHeadsUpChild;
    public static Method methodUpdateContentClipping;
    public static Method methodRunSwitchAnimation;

    public static Method methodIsUserLocked;
    public static Method methodIsExpanded;
    public static Method methodGetIntrinsicHeight;
    public static Method methodGetMaxContentHeight;
    public static Method methodGetMaxExpandHeight;
    public static Method methodGetActualHeightRow;
    public static Method methodNotifyHeightChanged;
    public static Method methodGetStatusBarNotification;

    public static void hook() {

        fieldIsPinned = XposedHelpers.findField(ExpandableNotificationRow, "mIsPinned");
        fieldIsHeadsUp = XposedHelpers.findField(ExpandableNotificationRow, "mIsHeadsUp");

        methodUpdateClipping = XposedHelpers.findMethodBestMatch(ExpandableView, "updateClipping");
        methodGetClipTopAmount = XposedHelpers.findMethodBestMatch(ExpandableView, "getClipTopAmount");
        methodGetActualHeight = XposedHelpers.findMethodBestMatch(ExpandableView, "getActualHeight");

        methodGetExpandedChild = XposedHelpers.findMethodBestMatch(NotificationContentView, "getExpandedChild");
        methodGetContractedChild = XposedHelpers.findMethodBestMatch(NotificationContentView, "getContractedChild");
        methodGetHeadsUpChild = XposedHelpers.findMethodBestMatch(NotificationContentView, "getHeadsUpChild");
        methodUpdateContentClipping = XposedHelpers.findMethodBestMatch(NotificationContentView, "updateClipping");
        methodRunSwitchAnimation = XposedHelpers.findMethodBestMatch(NotificationContentView, "runSwitchAnimation", int.class);

        methodIsUserLocked = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "isUserLocked");
        methodGetActualHeightRow = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getActualHeight");
        methodIsExpanded = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "isExpanded");
        methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getIntrinsicHeight");
        methodGetMaxContentHeight = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getMaxContentHeight");
        methodGetMaxExpandHeight = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getMaxExpandHeight");
        methodNotifyHeightChanged = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "notifyHeightChanged", boolean.class);
        methodGetStatusBarNotification = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getStatusBarNotification");

        NotificationContentHelper.initFields();
        ExpandableNotificationRowHelper.initFields();
        TransformState.initFields();

        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                helper.onFinishInflate();
            }
        });
        XposedHelpers.findAndHookConstructor(NotificationContentView, Context.class, AttributeSet.class, constructorHook);

        XposedHelpers.findAndHookMethod(NotificationContentView, "reset", boolean.class, reset);
        XposedHelpers.findAndHookMethod(NotificationContentView, "selectLayout", boolean.class, boolean.class, selectLayout);
        XposedHelpers.findAndHookMethod(NotificationContentView, "onMeasure", int.class, int.class, onMeasure);
        XposedHelpers.findAndHookMethod(NotificationContentView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setContractedChild", View.class, setContractedChild);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setExpandedChild", View.class, setExpandedChild);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setHeadsUpChild", View.class, setHeadsUpChild);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setVisible", boolean.class, setVisible);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setContentHeight", int.class, setContentHeight);
        XposedHelpers.findAndHookMethod(NotificationContentView, "updateClipping", updateClipping);
        XposedHelpers.findAndHookMethod(NotificationContentView, "updateViewVisibilities", int.class, updateViewVisibilities);
        XposedHelpers.findAndHookMethod(NotificationContentView, "runSwitchAnimation", int.class, animateToVisibleType);
        XposedHelpers.findAndHookMethod(NotificationContentView, "calculateVisibleType", calculateVisibleType);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setDark", boolean.class, boolean.class, long.class, setDark);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setHeadsUp", boolean.class, setHeadsUp);
        XposedHelpers.findAndHookMethod(NotificationContentView, "setShowingLegacyBackground", boolean.class, setShowingLegacyBackgroundHook);
        XposedHelpers.findAndHookMethod(NotificationContentView, "notifyContentUpdated", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(NotificationContentView, "updateRoundRectClipping", XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(ExpandableView, "onMeasure", int.class, int.class, onMeasureRow);
        //XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setStatusBarNotification", StatusBarNotification.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "applyExpansionToLayout", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "updateExpandButton", XC_MethodReplacement.DO_NOTHING);
        //XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "updateExpandButtonAppearance", XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(NotificationChildrenContainer, "setCollapseClickListener", View.OnClickListener.class, XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(ExpandableView, "setClipTopAmount", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                methodUpdateClipping.invoke(param.thisObject);
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableView, "updateClipping", updateClippingExpandableView);

        XposedHelpers.findAndHookMethod(ExpandableView, "getBoundsOnScreen", Rect.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                Rect outRect = (Rect) param.args[0];
                if (view.getTop() + view.getTranslationY() < 0) {
                    // We got clipped to the parent here - make sure we undo that.
                    outRect.top += view.getTop() + view.getTranslationY();
                }
                outRect.top += (int) invoke(methodGetClipTopAmount, param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(ExpandableView, "notifyHeightChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (ExpandableNotificationRow.isInstance(param.thisObject))
                    ExpandableNotificationRowHelper.getInstance(param.thisObject).notifyHeightChanged((boolean) param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod(NotificationContentView, "getMinHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return NotificationContentHelper.getInstance(param.thisObject).getMinHeight();
            }
        });

        XposedHelpers.findAndHookConstructor(ExpandableNotificationRow, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                //mFalsingManager = FalsingManager.getInstance(context);
                XposedHelpers.setObjectField(param.thisObject, "mExpandClickListener", null);
                helper.initDimens();
            }
        });

        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setUserLocked", boolean.class, new XC_MethodHook() {
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
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setPinned", boolean.class, new XC_MethodHook() {
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
                //if (intrinsicHeight != (int) XposedHelpers.callMethod(param.thisObject, "getIntrinsicHeight")) {
                    //XposedHelpers.callMethod(param.thisObject, "notifyHeightChanged", false  needsAnimation ); //TODO fix notification flashing
                //}
                if (pinned) {
                    XposedHelpers.callMethod(param.thisObject, "setIconAnimationRunning", true);
                    helper.mExpandedWhenPinned = false;
                } else if (helper.mExpandedWhenPinned) {
                    XposedHelpers.callMethod(param.thisObject, "setUserExpanded", true);
                }
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "getHeadsUpHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return ExpandableNotificationRowHelper.getInstance(param.thisObject).getPinnedHeadsUpHeight(true);
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "getIntrinsicHeight", getIntrinsicHeight);
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "getMinHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return ExpandableNotificationRowHelper.getInstance(param.thisObject).getMinHeight();
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setExpandable", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).mPrivateHelper.updateExpandButtons((boolean) XposedHelpers.callMethod(param.thisObject, "isExpandable"));
            }
        });

        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setHideSensitive", boolean.class, boolean.class, long.class, long.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).mPrivateHelper.updateExpandButtons((boolean) XposedHelpers.callMethod(param.thisObject, "isExpandable"));
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "reset", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper.getInstance(param.thisObject).resetTranslation();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "resetHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                View expandableRow = (View) param.thisObject;
                XposedHelpers.callMethod(expandableRow, "onHeightReset");
                expandableRow.requestLayout();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "updateChildrenVisibility", boolean.class, updateChildrenVisibility);
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "setChildrenExpanded", boolean.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "mChildrenExpanded", (boolean) param.args[0]);
                //ExpandableNotificationRowHelper.getInstance(param.thisObject).updateClickAndFocus(); //TODO do something about this
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(ExpandableNotificationRow, "updateMaxHeights", updateMaxHeights);
    }

    private static final XC_MethodReplacement updateMaxHeights = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);

            int intrinsicBefore = (int) methodGetIntrinsicHeight.invoke(param.thisObject);
            View expandedChild = (View) methodGetExpandedChild.invoke(helper.mPrivateLayout);
            if (expandedChild == null) {
                expandedChild = (View) methodGetContractedChild.invoke(helper.mPrivateLayout);
            }
            XposedHelpers.setIntField(param.thisObject, "mMaxExpandHeight", expandedChild.getHeight());
            View headsUpChild = (View) methodGetHeadsUpChild.invoke(helper.mPrivateLayout);
            if (headsUpChild == null) {
                headsUpChild = (View) methodGetContractedChild.invoke(helper.mPrivateLayout);
            }
            XposedHelpers.setIntField(param.thisObject, "mHeadsUpHeight", headsUpChild.getHeight());
            if (intrinsicBefore != (int) methodGetIntrinsicHeight.invoke(param.thisObject)) {
                methodNotifyHeightChanged.invoke(param.thisObject, true /*needsAnimation*/);
            }
            return null;
        }
    };

    private static final XC_MethodReplacement updateChildrenVisibility = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            View mChildrenContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mChildrenContainer");
            boolean mShowingPublic = XposedHelpers.getBooleanField(param.thisObject, "mShowingPublic");
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

    private static final XC_MethodReplacement getIntrinsicHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if ((boolean) methodIsUserLocked.invoke(param.thisObject)) {
                return methodGetActualHeightRow.invoke(param.thisObject);
            }
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            int mHeadsUpHeight = XposedHelpers.getIntField(param.thisObject, "mHeadsUpHeight");
                /*if (mGuts != null && mGuts.areGutsExposed()) {
                    return mGuts.getHeight();
                } else if ((isChildInGroup() && !isGroupExpanded())) {
                    return mPrivateLayout.getMinHeight();
                } else */if (XposedHelpers.getBooleanField(param.thisObject, "mSensitive") && XposedHelpers.getBooleanField(param.thisObject, "mHideSensitiveForIntrinsicHeight")) {
                return helper.getMinHeight();
                /*} else if (mIsSummaryWithChildren && !mOnKeyguard) {
                    return mChildrenContainer.getIntrinsicHeight();*/
            } else if (fieldIsHeadsUp.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                if (fieldIsPinned.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                    return helper.getPinnedHeadsUpHeight(true  /*atLeastMinHeight*/ );
                } else if ((boolean) methodIsExpanded.invoke(param.thisObject)) {
                    return Math.max((int) methodGetMaxExpandHeight.invoke(param.thisObject), mHeadsUpHeight);
                } else {
                    return Math.max(helper.getCollapsedHeight(), mHeadsUpHeight);
                }
            } else if ((boolean) methodIsExpanded.invoke(param.thisObject)) {
                return methodGetMaxExpandHeight.invoke(param.thisObject);
            } else {
                return helper.getCollapsedHeight();
            }
        }
    };

    private static final XC_MethodReplacement updateClippingExpandableView = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            Rect mClipRect = (Rect) XposedHelpers.getStaticObjectField(ExpandableView, "mClipRect");
            View view = (View) param.thisObject;
            if (helper.mClipToActualHeight) {
                //int top = (int) XposedHelpers.callMethod(view, "getClipTopAmount"); //TODO see why it doesn't work properly
                int top = XposedHelpers.getIntField(view, "mClipTopOptimization");
                int actualHeight = (int) methodGetActualHeight.invoke(view);
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

    private static final XC_MethodReplacement onMeasureRow = new XC_MethodReplacement() {
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

    private static XC_MethodReplacement reset = new XC_MethodReplacement() {
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
            XposedHelpers.setObjectField(contentView, "mContractedChild", null);
            helper.mContractedChild = null;
            XposedHelpers.setObjectField(contentView, "mExpandedChild", null);
            helper.mExpandedChild = null;
            XposedHelpers.setObjectField(contentView, "mHeadsUpChild", null);
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

    private static XC_MethodReplacement animateToVisibleType = new XC_MethodReplacement() {
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

    private static XC_MethodReplacement setContractedChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            if (helper.mContractedChild != null) {
                helper.mContractedChild.animate().cancel();
                helper.getContentView().removeView(helper.mContractedChild);
            }
            helper.getContentView().addView(child);
            XposedHelpers.setObjectField(param.thisObject, "mContractedChild", child);
            helper.mContractedChild = child;
            helper.mContractedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);
            helper.mContractedWrapper.setDark(XposedHelpers.getBooleanField(param.thisObject, "mDark"), false  /*animate*/ , 0  /*delay*/ );
            return null;
        }
    };

    private static XC_MethodReplacement setExpandedChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);

            if (helper.mExpandedChild != null) {
                helper.mExpandedChild.animate().cancel();
                helper.getContentView().removeView(helper.mExpandedChild);
            }
            helper.getContentView().addView(child);
            XposedHelpers.setObjectField(param.thisObject, "mExpandedChild", child);
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

    private static XC_MethodReplacement setHeadsUpChild = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View child = (View) param.args[0];
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);

            if (helper.mHeadsUpChild != null) {
                helper.mHeadsUpChild.animate().cancel();
                helper.getContentView().removeView(helper.mHeadsUpChild);
            }
            helper.getContentView().addView(child);
            XposedHelpers.setObjectField(param.thisObject, "mHeadsUpChild", child);
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

    private static XC_MethodReplacement setVisible = new XC_MethodReplacement() {
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
                XposedHelpers.setBooleanField(helper.getContentView(), "mAnimate", false);
            }
            return null;
        }
    };

    private static XC_MethodReplacement onMeasure = new XC_MethodReplacement() {
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
            XposedHelpers.setAdditionalInstanceField(param.thisObject, "previousHeight", previousHeight);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int previousHeight = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "previousHeight");

            if (previousHeight != 0 && helper.mExpandedChild.getHeight() != previousHeight) {
                helper.mContentHeightAtAnimationStart = previousHeight;
            }
            invoke(NotificationContentHelper.methodSelectLayout, helper.getContentView(), false  /*animate*/, helper.mForceSelectNextLayout  /*force*/ );
            helper.mForceSelectNextLayout = false;
            helper.updateExpandButtons(helper.mExpandable);
        }
    };

    private static XC_MethodReplacement setContentHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            int contentHeight = (int) param.args[0];
            set(NotificationContentHelper.fieldContentHeight, helper.getContentView(), Math.max(Math.min(contentHeight, helper.getContentView().getHeight()), helper.getMinHeight()));

            invoke(NotificationContentHelper.methodSelectLayout, helper.getContentView(),
                    XposedHelpers.getBooleanField(helper.getContentView(), "mAnimate")  /*animate*/ , false  /*force*/ );

            int minHeightHint = helper.getMinContentHeightHint();

            NotificationViewWrapper wrapper = helper.getVisibleWrapper(helper.getVisibleType());
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            wrapper = helper.getVisibleWrapper(helper.mTransformationStartVisibleType);
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            methodUpdateContentClipping.invoke(helper.getContentView());
            helper.getContentView().invalidateOutline();
            return null;
        }
    };

    private static XC_MethodReplacement selectLayout = new XC_MethodReplacement() {
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
                int visibleType = (int) invoke(NotificationContentHelper.methodCalculateVisibleType, helper.getContentView());
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
                        methodRunSwitchAnimation.invoke(helper.getContentView(), visibleType);
                        XposedHelpers.callMethod(helper.getContentView(), "runSwitchAnimation", visibleType);
                    } else {
                        invoke(NotificationContentHelper.methodUpdateViewVisibilities, helper.getContentView(), visibleType);
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

    private static XC_MethodReplacement updateViewVisibilities = new XC_MethodReplacement() {
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

    private static XC_MethodReplacement calculateVisibleType = new XC_MethodReplacement() {
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
                        || (boolean) methodIsExpanded.invoke(helper.mContainingNotification/*, (true  *//*allowOnKeyguard*//* */) //TODO finish implementing
                        ? (int) methodGetMaxContentHeight.invoke(helper.mContainingNotification)
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
            int intrinsicHeight = (int) methodGetIntrinsicHeight.invoke(helper.mContainingNotification);
            int viewHeight = helper.getContentHeight();
            if (intrinsicHeight != 0) {
                // the intrinsicHeight might be 0 because it was just reset.
                viewHeight = Math.min(helper.getContentHeight(), intrinsicHeight);
            }
            return helper.getVisualTypeForHeight(viewHeight);
        }
    };

    private static XC_MethodReplacement isContentExpandable = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return NotificationContentHelper.getInstance(param.thisObject).mIsContentExpandable;
        }
    };

    private static XC_MethodReplacement setDark = new XC_MethodReplacement() {
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
            XposedHelpers.setBooleanField(helper.getContentView(), "mDark", dark);
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

    private static XC_MethodHook setHeadsUp = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            helper.updateExpandButtons(helper.mExpandable);
        }
    };

    private static XC_MethodReplacement updateClipping = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            if (helper.mClipToActualHeight) {
                int top = XposedHelpers.getIntField(helper.getContentView(), "mClipTopAmount");
                int bottom = helper.getContentHeight();
                ((Rect) XposedHelpers.getObjectField(helper.getContentView(), "mClipBounds")).set(0, top, helper.getContentView().getWidth(), bottom);
                helper.getContentView().setClipBounds((Rect) XposedHelpers.getObjectField(helper.getContentView(), "mClipBounds"));
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
            Object headsUpRemoteInputActive = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "remoteInputActive");
            if (headsUpRemoteInputActive != null && (boolean) headsUpRemoteInputActive != remoteInputActive) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "remoteInputActive", remoteInputActive);
                if (remoteInputActive) {
                    XposedHook.logI(TAG, "remoteInputActive!");
                    XposedHelpers.callMethod(headsUpEntry, "removeAutoRemovalCallbacks");
                } else {
                    XposedHelpers.callMethod(headsUpEntry, "updateEntry", false /* updatePostTime */);
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
            Object headsUpExpanded = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "expanded");
            if (headsUpExpanded != null && (boolean) headsUpExpanded != expanded) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "expanded", expanded);
                if (expanded) {
                    XposedHook.logI(TAG, "expanded!");
                    XposedHelpers.callMethod(headsUpEntry, "removeAutoRemovalCallbacks");
                } else {
                    XposedHelpers.callMethod(headsUpEntry, "updateEntry", false /* updatePostTime */);
                }
            }
        }
    }
}

