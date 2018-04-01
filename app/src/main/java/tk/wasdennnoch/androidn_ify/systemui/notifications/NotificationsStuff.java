package tk.wasdennnoch.androidn_ify.systemui.notifications;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Pools;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformableView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationChildrenContainerHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationGroupManagerHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.getChildMeasureSpec;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.*;

public class NotificationsStuff {
    private static final String TAG = "NotificationsStuff";

    public static final boolean ENABLE_CHILD_NOTIFICATIONS = ConfigUtils.notifications().enable_bundled_notifications;
    public static final boolean DEBUG_CONTENT_TRANSFORMATIONS = false; //RED = headsUp, GREEN = expanded, BLUE = contracted
    public static final int NUMBER_OF_CHILDREN_WHEN_COLLAPSED = ConfigUtils.notifications().bundled_notifications_collapsed_children;
    public static final int NUMBER_OF_CHILDREN_WHEN_SYSTEM_EXPANDED = 5;
    public static final int NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED = ConfigUtils.notifications().bundled_notifications_expanded_children;


    public static final int VISIBLE_TYPE_CONTRACTED = 0;
    public static final int VISIBLE_TYPE_EXPANDED = 1;
    public static final int VISIBLE_TYPE_HEADSUP = 2;
    public static final int VISIBLE_TYPE_SINGLELINE = 3;
    public static final int VISIBLE_TYPE_AMBIENT = 4;
    public static final int VISIBLE_TYPE_AMBIENT_SINGLELINE = 5;
    public static final int UNDEFINED = -1;

    private static Object mGroupManager;

    public static void hook() {

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

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "addChildNotification", SystemUI.ExpandableNotificationRow, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(param.args[0]);
                helper.onChildrenCountChanged();
                rowHelper.setIsChildInGroup(true, (FrameLayout) param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "removeChildNotification", SystemUI.ExpandableNotificationRow, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(param.args[0]);
                helper.onChildrenCountChanged();
                rowHelper.setIsChildInGroup(false, null);
            }
        });
        XposedBridge.hookAllMethods(SystemUI.ExpandableNotificationRow, "setGroupManager", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                helper.setGroupManager(param.args[0]);
            }
        });
//        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "onMeasure", int.class, int.class, XOnMeasureExpandableView); //TODO: causes weird issues with clipping on Xperia
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setStatusBarNotification", StatusBarNotification.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "applyExpansionToLayout", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateExpandButton", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateExpandButtonAppearance", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "canHaveBottomDecor", XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "isChildInvisible", View.class, XC_MethodReplacement.returnConstant(false));

//        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "setCollapseClickListener", View.OnClickListener.class, XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "setClipTopAmount", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                invoke(Methods.SystemUI.ExpandableView.updateClipping, param.thisObject);
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
                outRect.top += (int) invoke(Methods.SystemUI.ExpandableView.getClipTopAmount, param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "notifyHeightChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (SystemUI.ExpandableNotificationRow.isInstance(param.thisObject))
                    ExpandableNotificationRowHelper.getInstance(param.thisObject).notifyHeightChanged((boolean) param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "setActualHeight", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                set(Fields.SystemUI.ExpandableView.mActualHeightInitialized, param.thisObject, false);
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableView, "getBottomDecorHeight", XC_MethodReplacement.returnConstant(0));
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

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "isUserLocked", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return getBoolean(Fields.SystemUI.ExpandableNotificationRow.mUserLocked, param.thisObject) && !ExpandableNotificationRowHelper.getInstance(param.thisObject).mForceUnlocked;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setUserLocked", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean userLocked = (boolean) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                helper.getPrivateHelper().setUserExpanding(userLocked);
                if (helper.mIsSummaryWithChildren) {
                    helper.mChildrenContainerHelper.setUserLocked(userLocked);
                    if (userLocked || !helper.isGroupExpanded()) {
                        helper.updateBackgroundForGroupState();
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setIconAnimationRunning", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean running = (boolean) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (helper.mIsSummaryWithChildren) {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.setIconAnimationRunningForChild, param.thisObject, running, helper.mChildrenContainerHelper.getHeaderView());
                    List notificationChildren =
                            invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, helper.mChildrenContainer);
                    for (int i = 0; i < notificationChildren.size(); i++) {
                        Object child = notificationChildren.get(i);
                        invoke(Methods.SystemUI.ExpandableNotificationRow.setIconAnimationRunning, child, running);
                    }
                }
                helper.mIconAnimationRunning = running;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setPinned", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean pinned = (boolean) param.args[0];
                int intrinsicHeight = invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, param.thisObject);
                if (intrinsicHeight != (int) invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, param.thisObject)) {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.notifyHeightChanged, param.thisObject, false /* needsAnimation */);
                    invoke(Methods.SystemUI.NotificationContentView.selectLayout,
                            invoke(Methods.SystemUI.ExpandableNotificationRow.getShowingLayout, param.thisObject),
                            invoke(Methods.SystemUI.ExpandableNotificationRow.isUserLocked, param.thisObject),
                            false);
                }
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (pinned) {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.setIconAnimationRunning, param.thisObject, true);
                    helper.mExpandedWhenPinned = false;
                } else if (helper.mExpandedWhenPinned) {
                    helper.setUserExpanded(true);
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
                ExpandableNotificationRowHelper.getInstance(param.thisObject).getPrivateHelper()
                        .updateExpandButtons((boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isExpandable, param.thisObject));
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setHideSensitive", boolean.class, boolean.class, long.class, long.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                boolean hideSensitive = (boolean) param.args[0];
                boolean animated = (boolean) param.args[1];
                long delay = (long) param.args[2];
                long duration = (long) param.args[3];

                boolean oldShowingPublic = getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject);
                set(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject, getBoolean(Fields.SystemUI.ExpandableNotificationRow.mSensitive, param.thisObject) && hideSensitive);
                if (getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublicInitialized, param.thisObject) &&
                        getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject) == oldShowingPublic) {
                    return null;
                }

                // bail out if no public version
                if (((ViewGroup) get(Fields.SystemUI.ExpandableNotificationRow.mPublicLayout, param.thisObject)).getChildCount() == 0)
                    return null;

                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (!animated) {
                    helper.mPublicLayout.animate().cancel();
                    helper.mPrivateLayout.animate().cancel();
                    if (helper.mChildrenContainer != null) {
                        helper.mChildrenContainer.animate().cancel();
                        helper.mChildrenContainer.setAlpha(1f);
                    }
                    helper.mPublicLayout.setAlpha(1f);
                    helper.mPrivateLayout.setAlpha(1f);
                    helper.mPublicLayout.setVisibility(getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject) ? View.VISIBLE : View.INVISIBLE);
                    helper.updateChildrenVisibility();
                } else {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.animateShowingPublic, param.thisObject, delay, duration);
                }
                helper.getShowingHelper().updateBackgroundColor(animated);
                helper.getPrivateHelper().updateExpandButtons((boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isExpandable, param.thisObject));
                set(Fields.SystemUI.ExpandableNotificationRow.mShowingPublicInitialized, param.thisObject, true);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "animateShowingPublic", long.class, long.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                long delay = (long) param.args[0];
                long duration = (long) param.args[1];

                View[] privateViews = isSummaryWithChildren(param.thisObject)
                        ? new View[]{get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, param.thisObject)}
                        : new View[]{get(Fields.SystemUI.ExpandableNotificationRow.mPrivateLayout, param.thisObject)};
                View[] publicViews = new View[]{get(Fields.SystemUI.ExpandableNotificationRow.mPublicLayout, param.thisObject)};
                View[] hiddenChildren = getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject) ? privateViews : publicViews;
                View[] shownChildren = getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject) ? publicViews : privateViews;
                for (final View hiddenView : hiddenChildren) {
                    hiddenView.setVisibility(View.VISIBLE);
                    hiddenView.animate().cancel();
                    hiddenView.animate()
                            .alpha(0f)
                            .setStartDelay(delay)
                            .setDuration(duration)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    hiddenView.setVisibility(View.INVISIBLE);
                                }
                            });
                }
                for (View showView : shownChildren) {
                    showView.setVisibility(View.VISIBLE);
                    showView.setAlpha(0f);
                    showView.animate().cancel();
                    showView.animate()
                            .alpha(1f)
                            .setStartDelay(delay)
                            .setDuration(duration);
                }
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getContentView", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isSummaryWithChildren(param.thisObject) && !getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject)) {
                    param.setResult(get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, param.thisObject));
                }
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
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateChildrenVisibility", boolean.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setChildrenExpanded", boolean.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                boolean expanded = (boolean) param.args[0];
                boolean animated = (boolean) param.args[1];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);

                set(Fields.SystemUI.ExpandableNotificationRow.mChildrenExpanded, param.thisObject, expanded);
                helper.setChildrenExpanded((boolean) param.args[0], (boolean) param.args[1]);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setHeadsUp", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (helper.mIsSummaryWithChildren) {
                    // The overflow might change since we allow more lines as HUN.
                    helper.mChildrenContainerHelper.updateGroupOverflow();
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setSystemExpanded", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                boolean expand = (boolean) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (expand != getBoolean(Fields.SystemUI.ExpandableNotificationRow.mIsSystemExpanded, param.thisObject)) {
                    if (helper.mIsSummaryWithChildren) {
                        helper.mChildrenContainerHelper.updateGroupOverflow();
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setExpansionDisabled", boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                boolean onKeyguard = (boolean) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (onKeyguard != getBoolean(Fields.SystemUI.ExpandableNotificationRow.mExpansionDisabled, param.thisObject)) {
                    final boolean wasExpanded = invoke(Methods.SystemUI.ExpandableNotificationRow.isExpanded, param.thisObject);
                    set(Fields.SystemUI.ExpandableNotificationRow.mExpansionDisabled, param.thisObject, onKeyguard);
                    XposedHelpers.callMethod(param.thisObject, "logExpansionEvent", false, wasExpanded);
                    if (wasExpanded != (boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isExpanded, param.thisObject)) {
                        if (helper.mIsSummaryWithChildren) {
                            helper.mChildrenContainerHelper.updateGroupOverflow();
                        }
                        invoke(Methods.SystemUI.ExpandableNotificationRow.notifyHeightChanged, param.thisObject, false  /* needsAnimation */);
                    }
                }
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getMaxContentHeight", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (helper.mIsSummaryWithChildren && !getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject)) {
                    param.setResult(helper.mChildrenContainerHelper.getMaxContentHeight());
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setActualHeight", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int height = (int) param.args[0];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (helper.mIsSummaryWithChildren) {
                    helper.mChildrenContainerHelper.setActualHeight(height);
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setDark", boolean.class, boolean.class, long.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean dark = (boolean) param.args[0];
                boolean fade = (boolean) param.args[1];
                long delay = (long) param.args[2];
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                if (helper.mIsSummaryWithChildren) {
                    helper.mChildrenContainerHelper.setDark(dark, fade, delay);
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "isExpandable", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isSummaryWithChildren(param.thisObject) && !getBoolean(Fields.SystemUI.ExpandableNotificationRow.mShowingPublic, param.thisObject)) {
                    param.setResult(!getBoolean(Fields.SystemUI.ExpandableNotificationRow.mChildrenExpanded, param.thisObject));
                }
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "updateMaxHeights", XUpdateMaxHeights);

        XposedHelpers.findAndHookMethod(SystemUI.PhoneStatusBar, "maybeEscalateHeadsUp", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object headsUpManager = get(Fields.SystemUI.BaseStatusBar.mHeadsUpManager, param.thisObject);
                Collection entries = getAllEntries(headsUpManager);
                for (Object entry : entries) {
                    Object notificationEntry = get(Fields.SystemUI.HeadsUpEntry.entry, entry);
                    final StatusBarNotification sbn = get(Fields.SystemUI.NotificationDataEntry.notification, notificationEntry);
                    final Notification notification = sbn.getNotification();
                    if (notification.fullScreenIntent != null) {
                        try {
                            EventLog.writeEvent(36003 /*EventLogTags.SYSUI_HEADS_UP_ESCALATION*/,
                                    sbn.getKey());
                            notification.fullScreenIntent.send();
                            XposedHelpers.callMethod(notificationEntry, "notifyFullScreenIntentLaunched"); //TODO: maybe optimize
                        } catch (PendingIntent.CanceledException e) {
                        }
                    }
                }
                invoke(Methods.SystemUI.HeadsUpManager.releaseAllImmediately, headsUpManager);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.PhoneStatusBar, "updateNotificationShade", XUpdateNotificationShade);
        XposedHelpers.findAndHookMethod(SystemUI.BaseStatusBar, "start", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(SystemUI.BaseStatusBar, "ENABLE_CHILD_NOTIFICATIONS", ENABLE_CHILD_NOTIFICATIONS);
            }
        });

        XposedHelpers.findAndHookMethod(Classes.SystemUI.BaseStatusBar, "updateNotificationViews", Classes.SystemUI.NotificationDataEntry, StatusBarNotification.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object entry = param.args[0];
                StatusBarNotification sbn = (StatusBarNotification) param.args[1];
                Object row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
                Context context = get(Fields.SystemUI.BaseStatusBar.mContext, param.thisObject);
                Object privateLayout = get(Fields.SystemUI.ExpandableNotificationRow.mPrivateLayout, row);
                View expandedChild = invoke(Methods.SystemUI.NotificationContentView.getExpandedChild, privateLayout);
                View headsUpChild = invoke(Methods.SystemUI.NotificationContentView.getHeadsUpChild, privateLayout);

                if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                    Notification.Action[] actions = sbn.getNotification().actions;
                    if (actions != null) {
                        NotificationHooks.addRemoteInput(context, expandedChild, actions);
                        NotificationHooks.addRemoteInput(context, headsUpChild, actions);
                    }
                }

                ExpandableNotificationRowHelper.getInstance(row).onNotificationUpdated(entry);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.PhoneStatusBar, "onHeadsUpStateChanged", SystemUI.NotificationDataEntry, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                NotificationGroupManagerHooks.onHeadsUpStateChanged(get(Fields.SystemUI.BaseStatusBar.mGroupManager, param.thisObject), param.args[0], (boolean) param.args[1]);
                NotificationPanelHooks.onHeadsUpStateChanged(get(Fields.SystemUI.BaseStatusBar.mHeadsUpManager, param.thisObject));
            }
        });
        XposedHelpers.findAndHookConstructor(SystemUI.NotificationChildrenContainer, Context.class, AttributeSet.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                NotificationChildrenContainerHelper.getInstance(param.thisObject).onConstructor();
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                NotificationChildrenContainerHelper.getInstance(param.thisObject).onLayout(false, 0, 0, 0, 0); //passing all zeroes since we don't use them anyway
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "onMeasure", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                NotificationChildrenContainerHelper.getInstance(param.thisObject).onMeasure((int) param.args[0], (int) param.args[1]);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "addNotification", SystemUI.ExpandableNotificationRow, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                NotificationChildrenContainerHelper.getInstance(param.thisObject).addNotification((FrameLayout) param.args[0], (int) param.args[1]);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "removeNotification", SystemUI.ExpandableNotificationRow, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                NotificationChildrenContainerHelper.getInstance(param.thisObject).removeNotification((FrameLayout) param.args[0]);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.NotificationChildrenContainer, "getIntrinsicHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return NotificationChildrenContainerHelper.getInstance(param.thisObject).getIntrinsicHeight();
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getChildrenStates", SystemUI.StackScrollState, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                Object resultState = param.args[0];
                if (helper.mIsSummaryWithChildren) {
                    Object parentState = invoke(Methods.SystemUI.StackScrollState.getViewStateForView, resultState, param.thisObject);
                    helper.mChildrenContainerHelper.getState(resultState, parentState);
                }
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "applyChildrenState", SystemUI.StackScrollState, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                Object state = param.args[0];
                if (helper.mIsSummaryWithChildren) {
                    helper.mChildrenContainerHelper.applyState(state);
                }
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "prepareExpansionChanged", SystemUI.StackScrollState, XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "startChildAnimation",
                SystemUI.StackScrollState, SystemUI.StackStateAnimator, boolean.class, long.class, long.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Object finalState = param.args[0];
                        Object stateAnimator = param.args[1];
                        long delay = (long) param.args[3];
                        long duration = (long) param.args[4];
                        ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
                        if (helper.mIsSummaryWithChildren) {
                            helper.mChildrenContainerHelper.startAnimationToState(finalState, stateAnimator, delay, duration);
                        }
                        return null;
                    }
                });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "getViewAtPosition", float.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isSummaryWithChildren(param.thisObject))
                    param.setResult(param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "calculateContentHeightFromActualHeight", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                int height = (int) param.args[0];
                return Math.max(ExpandableNotificationRowHelper.getInstance(param.thisObject).getMinHeight(), height);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.ExpandableNotificationRow, "setHideSensitiveForIntrinsicHeight", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean hideSensitive = (boolean) param.args[0];
                Object childrenContainer = get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, param.thisObject);
                if (isSummaryWithChildren(param.thisObject)) {
                    List notificationChildren =
                            invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, childrenContainer);
                    for (int i = 0; i < notificationChildren.size(); i++) {
                        Object child = notificationChildren.get(i);
                        invoke(Methods.SystemUI.ExpandableNotificationRow.setHideSensitiveForIntrinsicHeight, child, hideSensitive);
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "getTopHeadsUpHeight", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object topEntry = invoke(Methods.SystemUI.HeadsUpManager.getTopEntry, param.thisObject);
                if (topEntry == null || get(Fields.SystemUI.HeadsUpEntry.entry, topEntry) == null) {
                    return 0;
                }
                View row = get(Fields.SystemUI.NotificationDataEntry.row, get(Fields.SystemUI.HeadsUpEntry.entry, topEntry));
                ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);
                if (rowHelper.isChildInGroup()) {
                    final FrameLayout groupSummary
                            = invoke(Methods.SystemUI.NotificationGroupManager.getGroupSummary, mGroupManager, invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, row));
                    if (groupSummary != null) {
                        row = groupSummary;
                        rowHelper = ExpandableNotificationRowHelper.getInstance(row);
                    }
                }
                return rowHelper.getPinnedHeadsUpHeight(true /* atLeastMinHeight */);
            }
        });

        XposedBridge.hookAllMethods(SystemUI.HeadsUpManager, "onComputeInternalInsets", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object info = param.args[0];
                int[] mTmpTwoArray = get(Fields.SystemUI.HeadsUpManager.mTmpTwoArray, param.thisObject);
                if (getBoolean(Fields.SystemUI.HeadsUpManager.mIsExpanded, param.thisObject)
                        || (boolean) XposedHelpers.callMethod(get(Fields.SystemUI.HeadsUpManager.mBar, param.thisObject), "isBouncerShowing")) //TODO: maybe optimize
                    return null;
                if (getBoolean(Fields.SystemUI.HeadsUpManager.mHasPinnedNotification, param.thisObject)) {
                    Object topNotificationEntry = get(Fields.SystemUI.HeadsUpEntry.entry, invoke(Methods.SystemUI.HeadsUpManager.getTopEntry, param.thisObject));
                    View topEntry = get(Fields.SystemUI.NotificationDataEntry.row, topNotificationEntry);
                    if (ExpandableNotificationRowHelper.getInstance(topEntry).isChildInGroup()) {
                        final View groupSummary
                                = invoke(Methods.SystemUI.NotificationGroupManager.getGroupSummary,
                                mGroupManager, (StatusBarNotification) invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, topEntry));
                        if (groupSummary != null) {
                            topEntry = groupSummary;
                        }
                    }
                    topEntry.getLocationOnScreen(mTmpTwoArray);
                    int minX = mTmpTwoArray[0];
                    int maxX = mTmpTwoArray[0] + topEntry.getWidth();
                    int maxY = invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, topEntry);

                    XposedHelpers.callMethod(info, "setTouchableInsets", 3 /*TOUCHABLE_INSETS_REGION*/);
                    ((Region) XposedHelpers.getObjectField(info, "touchableRegion")).set(minX, 0, maxX, maxY);
                } else if (getBoolean(Fields.SystemUI.HeadsUpManager.mHeadsUpGoingAway, param.thisObject) || getBoolean(Fields.SystemUI.HeadsUpManager.mWaitingOnCollapseWhenGoingAway, param.thisObject)) {
                    XposedHelpers.callMethod(info, "setTouchableInsets", 3 /*TOUCHABLE_INSETS_REGION*/);
                    ((Region) XposedHelpers.getObjectField(info, "touchableRegion"))
                            .set(0, 0, ((View) get(Fields.SystemUI.HeadsUpManager.mStatusBarWindowView, param.thisObject)).getWidth(),
                                    getInt(Fields.SystemUI.HeadsUpManager.mStatusBarHeight, param.thisObject));
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "updateNotification", SystemUI.NotificationDataEntry, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object headsUp = param.args[0];
                boolean alert = (boolean) param.args[1];
                ((View) get(Fields.SystemUI.NotificationDataEntry.row, headsUp)).sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);

                if (alert) {
                    Object headsUpEntry = ((HashMap) get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, param.thisObject)).get(get(Fields.SystemUI.NotificationDataEntry.key, headsUp));
                    if (headsUpEntry == null) {
                        // the entry was released before this update (i.e by a listener) This can happen
                        // with the groupmanager
                        return null;
                    }
                    XposedHelpers.callMethod(headsUpEntry, "updateEntry");
                    invoke(Methods.SystemUI.HeadsUpManager.setEntryPinned, param.thisObject, headsUpEntry, invoke(Methods.SystemUI.HeadsUpManager.shouldHeadsUpBecomePinned, param.thisObject, headsUp));
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "getTopEntry", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                HashMap headsUpEntries = get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, param.thisObject);
                if (headsUpEntries.isEmpty()) {
                    return null;
                }
                Object topEntry = null;
                for (Object entry: headsUpEntries.values()) {
                    if (topEntry == null || (int) invoke(Methods.SystemUI.HeadsUpEntry.compareTo, entry, topEntry) == -1) {
                        topEntry = entry;
                    }
                }
                return topEntry;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "getSortedEntries", XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "removeHeadsUpEntry", SystemUI.NotificationDataEntry, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object entry = param.args[0];
                HashMap headsUpEntries = get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, param.thisObject);
                HashSet listeners = get(Fields.SystemUI.HeadsUpManager.mListeners, param.thisObject);
                View row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
                Object remove = headsUpEntries.remove(get(Fields.SystemUI.NotificationDataEntry.key, entry));
                Pools.Pool entryPool = get(Fields.SystemUI.HeadsUpManager.mEntryPool, param.thisObject);

                row.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                invoke(Methods.SystemUI.ExpandableNotificationRow.setHeadsUp, row, false);
                invoke(Methods.SystemUI.HeadsUpManager.setEntryPinned, param.thisObject, remove, false /* isPinned */);
                for (Object listener : listeners) {
                    invoke(Methods.SystemUI.HeadsUpManager.OnHeadsUpChangedListener.onHeadsUpStateChanged, listener, entry, false);
                }
                entryPool.release(remove);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "unpinAll", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                HashMap headsUpEntries = get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, param.thisObject);
                for (Object key : headsUpEntries.keySet()) {
                    Object entry = headsUpEntries.get(key);
                    invoke(Methods.SystemUI.HeadsUpManager.setEntryPinned, param.thisObject, entry, false /* isPinned */);
                    // maybe it got un sticky
                    updateEntry(entry, false);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpManager, "onExpandingFinished", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                HashSet entriesToRemoveAfterExpand = get(Fields.SystemUI.HeadsUpManager.mEntriesToRemoveAfterExpand, param.thisObject);
                if (getBoolean(Fields.SystemUI.HeadsUpManager.mReleaseOnExpandFinish, param.thisObject)) {
                    invoke(Methods.SystemUI.HeadsUpManager.releaseAllImmediately, param.thisObject);
                    set(Fields.SystemUI.HeadsUpManager.mReleaseOnExpandFinish, param.thisObject, false);
                } else {
                    for (Object entry : entriesToRemoveAfterExpand) {
                        if (invoke(Methods.SystemUI.HeadsUpManager.isHeadsUp, param.thisObject, get(Fields.SystemUI.NotificationDataEntry.key, entry))) {
                            // Maybe the heads-up was removed already
                            invoke(Methods.SystemUI.HeadsUpManager.removeHeadsUpEntry, param.thisObject, entry);
                        }
                    }
                }
                entriesToRemoveAfterExpand.clear();
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpEntry, "reset", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "expanded",  false);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "remoteInputActive", false);
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpEntry, "updateEntry", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                updateEntry(param.thisObject, true);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(SystemUI.HeadsUpEntry, "compareTo", SystemUI.HeadsUpEntry, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object entry = get(Fields.SystemUI.HeadsUpEntry.entry, param.thisObject);
                Object row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
                Object o = param.args[0];
                Object otherEntry = get(Fields.SystemUI.HeadsUpEntry.entry, o);
                Object otherRow = get(Fields.SystemUI.NotificationDataEntry.row, otherEntry);
                boolean isPinned = invoke(Methods.SystemUI.ExpandableNotificationRow.isPinned, row);
                boolean otherPinned = invoke(Methods.SystemUI.ExpandableNotificationRow.isPinned, otherRow);
                if (isPinned && !otherPinned) {
                    param.setResult(-1);
                } else if (!isPinned && otherPinned) {
                    param.setResult(1);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object o = param.args[0];
                Object temp = XposedHelpers.getAdditionalInstanceField(param.thisObject, "remoteInputActive");
                boolean remoteInputActive = temp != null && (boolean) temp;

                temp = XposedHelpers.getAdditionalInstanceField(o, "remoteInputActive");
                boolean otherRemoteInputActive = temp != null && (boolean) temp;
                if (remoteInputActive && !otherRemoteInputActive) {
                    param.setResult(-1);
                } else if (!remoteInputActive && otherRemoteInputActive) {
                    param.setResult(1);
                }
            }
        });
    }

    private static final XC_MethodReplacement XUpdateMaxHeights = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);

            int intrinsicBefore = invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, param.thisObject);
            View expandedChild = invoke(Methods.SystemUI.NotificationContentView.getExpandedChild, helper.mPrivateLayout);
            if (expandedChild == null) {
                expandedChild = invoke(Methods.SystemUI.NotificationContentView.getContractedChild, helper.mPrivateLayout);
            }
            set(Fields.SystemUI.ExpandableNotificationRow.mMaxExpandHeight, param.thisObject, expandedChild.getHeight());
            View headsUpChild = invoke(Methods.SystemUI.NotificationContentView.getHeadsUpChild, helper.mPrivateLayout);
            if (headsUpChild == null) {
                headsUpChild = invoke(Methods.SystemUI.NotificationContentView.getContractedChild, helper.mPrivateLayout);
            }
            set(Fields.SystemUI.ExpandableNotificationRow.mHeadsUpHeight, param.thisObject, headsUpChild.getHeight());
            if (intrinsicBefore != (int) invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, param.thisObject)) {
                invoke(Methods.SystemUI.ExpandableNotificationRow.notifyHeightChanged, param.thisObject, true /*needsAnimation*/);
            }
            return null;
        }
    };

    private static final XC_MethodReplacement XGetIntrinsicHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (invoke(Methods.SystemUI.ExpandableNotificationRow.isUserLocked, param.thisObject)) {
                return invoke(Methods.SystemUI.ExpandableNotificationRow.getActualHeight, param.thisObject);
            }
            ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            int mHeadsUpHeight = getInt(Fields.SystemUI.ExpandableNotificationRow.mHeadsUpHeight, param.thisObject);
                /*if (mGuts != null && mGuts.areGutsExposed()) {
                    return mGuts.getHeight();
                } else */if ((helper.isChildInGroup() && !helper.isGroupExpanded())) {
                    return helper.getPrivateHelper().getMinHeight();
                } else if (getBoolean(Fields.SystemUI.ExpandableNotificationRow.mSensitive, param.thisObject)
                    && getBoolean(Fields.SystemUI.ExpandableNotificationRow.mHideSensitiveForIntrinsicHeight, param.thisObject)) {
                return helper.getMinHeight();
                } else if (helper.mIsSummaryWithChildren && !helper.isOnKeyguard()) {
                    return helper.mChildrenContainerHelper.getIntrinsicHeight();
            } else if (Fields.SystemUI.ExpandableNotificationRow.mIsHeadsUp.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                if (Fields.SystemUI.ExpandableNotificationRow.mIsPinned.getBoolean(param.thisObject) || helper.mHeadsupDisappearRunning) {
                    return helper.getPinnedHeadsUpHeight(true  /*atLeastMinHeight*/ );
                } else if (invoke(Methods.SystemUI.ExpandableNotificationRow.isExpanded, param.thisObject)) {
                    return Math.max(getInt(Fields.SystemUI.ExpandableNotificationRow.mMaxExpandHeight, param.thisObject), mHeadsUpHeight);
                } else {
                    return Math.max(helper.getCollapsedHeight(), mHeadsUpHeight);
                }
            } else if (invoke(Methods.SystemUI.ExpandableNotificationRow.isExpanded, param.thisObject)) {
                return getInt(Fields.SystemUI.ExpandableNotificationRow.mMaxExpandHeight, param.thisObject);
            } else {
                return helper.getCollapsedHeight();
            }
        }
    };

    private static final XC_MethodReplacement XUpdateClippingExpandableView = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            Rect mClipRect = get(Fields.SystemUI.ExpandableView.mClipRect, null);
            View view = (View) param.thisObject;
            if (helper.mClipToActualHeight) {
                //int top = (int) XposedHelpers.callMethod(view, "getClipTopAmount"); //TODO see why it doesn't work properly
                int top = getInt(Fields.SystemUI.ExpandableView.mClipTopOptimization, view);
                int actualHeight = invoke(Methods.SystemUI.ExpandableView.getActualHeight, view);
                if (top >= actualHeight) {
                    top = actualHeight - 1;
                }
                int extraBottomPadding = SystemUI.ExpandableNotificationRow.isInstance(view) ? helper.getRowHelper().getExtraBottomPadding() : 0;
                mClipRect.set(0, top, view.getWidth(), actualHeight + extraBottomPadding);
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
            invoke(Methods.Android.View.setMeasuredDimension, param.thisObject, width, ownHeight);
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

            if (mContractedChild != null) {
                mContractedChild.animate().cancel();
                contentView.removeView(mContractedChild);
            }
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
            set(Fields.SystemUI.NotificationContentView.mContractedChild, contentView, null);
            helper.mContractedChild = null;
            set(Fields.SystemUI.NotificationContentView.mExpandedChild, contentView, null);
            helper.mExpandedChild = null;
            set(Fields.SystemUI.NotificationContentView.mHeadsUpChild, contentView, null);
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
//            helper.fireExpandedVisibleListenerIfVisible();
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
            set(Fields.SystemUI.NotificationContentView.mContractedChild, param.thisObject, child);
            helper.mContractedChild = child;
            helper.mContractedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);
            helper.mContractedWrapper.setDark(getBoolean(Fields.SystemUI.NotificationContentView.mDark, param.thisObject), false  /*animate*/ , 0  /*delay*/ );

            if (DEBUG_CONTENT_TRANSFORMATIONS)
                child.setBackgroundColor(0x990000FF);
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
            set(Fields.SystemUI.NotificationContentView.mExpandedChild, param.thisObject, child);
            helper.mExpandedChild = child;
            helper.mExpandedWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);

            if (DEBUG_CONTENT_TRANSFORMATIONS)
                child.setBackgroundColor(0x9900FF00);
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
            set(Fields.SystemUI.NotificationContentView.mHeadsUpChild, param.thisObject, child);
            helper.mHeadsUpChild = child;
            helper.mHeadsUpWrapper = NotificationViewWrapper.wrap(helper.getContentView().getContext(), child,
                    helper.mContainingNotification);

            if (DEBUG_CONTENT_TRANSFORMATIONS)
                child.setBackgroundColor(0x99FF0000);
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
                set(Fields.SystemUI.NotificationContentView.mAnimate, helper.getContentView(), false);
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

            invoke(Methods.Android.View.setMeasuredDimension, helper.getContentView(), width, ownHeight);
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
            invoke(Methods.SystemUI.NotificationContentView.selectLayout, helper.getContentView(), false  /*animate*/, helper.mForceSelectNextLayout  /*force*/ );
            helper.mForceSelectNextLayout = false;
            helper.updateExpandButtons(helper.mExpandable);
        }
    };

    private static XC_MethodReplacement XSetContentHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            NotificationContentHelper helper = NotificationContentHelper.getInstance(param.thisObject);
            View contentView = helper.getContentView();
            int contentHeight = (int) param.args[0];
            set(Fields.SystemUI.NotificationContentView.mContentHeight,
                    contentView, Math.max(Math.min(contentHeight, contentView.getHeight()), helper.getMinHeight()));

            invoke(Methods.SystemUI.NotificationContentView.selectLayout, contentView,
                    getBoolean(Fields.SystemUI.NotificationContentView.mAnimate, contentView) /*animate*/ , false  /*force*/ );

            int minHeightHint = helper.getMinContentHeightHint();

            NotificationViewWrapper wrapper = helper.getVisibleWrapper(helper.getVisibleType());
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            wrapper = helper.getVisibleWrapper(helper.mTransformationStartVisibleType);
            if (wrapper != null) {
                wrapper.setContentHeight(helper.getContentHeight(), minHeightHint);
            }

            invoke(Methods.SystemUI.NotificationContentView.updateClipping, contentView);
            contentView.invalidateOutline();
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
                int visibleType = invoke(Methods.SystemUI.NotificationContentView.calculateVisibleType, helper.getContentView());
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
                        invoke(Methods.SystemUI.NotificationContentView.runSwitchAnimation, helper.getContentView(), visibleType);
                    } else {
                        invoke(Methods.SystemUI.NotificationContentView.updateViewVisibilities, helper.getContentView(), visibleType);
                    }
                    set(Fields.SystemUI.NotificationContentView.mVisibleType, helper.getContentView(), visibleType);
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
//            helper.mAnimationStartVisibleType = UNDEFINED;
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
                        || ExpandableNotificationRowHelper.isExpanded(helper.mContainingNotification, true /*allowOnKeyguard*/)
                        ? (int) invoke(Methods.SystemUI.ExpandableNotificationRow.getMaxContentHeight, helper.mContainingNotification)
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
            int intrinsicHeight = invoke(Methods.SystemUI.ExpandableNotificationRow.getIntrinsicHeight, helper.mContainingNotification);
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
            set(Fields.SystemUI.NotificationContentView.mDark, helper.getContentView(), dark);
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
                int top = getInt(Fields.SystemUI.NotificationContentView.mClipTopAmount, helper.getContentView());
                int bottom = helper.getContentHeight();
                ((Rect) get(Fields.SystemUI.NotificationContentView.mClipBounds, helper.getContentView())).set(0, top, helper.getContentView().getWidth(), bottom);
                helper.getContentView().setClipBounds((Rect) get(Fields.SystemUI.NotificationContentView.mClipBounds, helper.getContentView()));
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

    private static XC_MethodReplacement XUpdateNotificationShade = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object phoneStatusBar = param.thisObject;
            ViewGroup mStackScroller = get(Fields.SystemUI.BaseStatusBar.mStackScroller, param.thisObject);
            HashMap mTmpChildOrderMap = get(Fields.SystemUI.PhoneStatusBar.mTmpChildOrderMap, param.thisObject);
            Object mNotificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, param.thisObject);
            Object mGroupManager = get(Fields.SystemUI.BaseStatusBar.mGroupManager, phoneStatusBar);

            if (mStackScroller == null) return null;

            // Do not modify the notifications during collapse.
            if (invoke(Methods.SystemUI.PhoneStatusBar.isCollapsing, phoneStatusBar)) {
                invoke(Methods.SystemUI.PhoneStatusBar.addPostCollapseAction, phoneStatusBar, new Runnable() {
                    @Override
                    public void run() {
                        invoke(Methods.SystemUI.PhoneStatusBar.updateNotificationShade, phoneStatusBar);
                    }
                });
                return null;
            }

            ArrayList activeNotifications = invoke(Methods.SystemUI.NotificationData.getActiveNotifications, mNotificationData);
            ArrayList<FrameLayout> toShow = new ArrayList<>(activeNotifications.size());
            final int N = activeNotifications.size();
            for (int i=0; i<N; i++) {
                Object ent = activeNotifications.get(i);
                StatusBarNotification notification = get(Fields.SystemUI.NotificationDataEntry.notification, ent);
                int vis = notification.getNotification().visibility;

                // Display public version of the notification if we need to redact.
                final boolean hideSensitive =
                        !(boolean) invoke(Methods.SystemUI.BaseStatusBar.userAllowsPrivateNotificationsInPublic, phoneStatusBar, notification.getUserId());
                boolean sensitiveNote = vis == Notification.VISIBILITY_PRIVATE;
                boolean sensitivePackage = invoke(Methods.SystemUI.PhoneStatusBar.packageHasVisibilityOverride, phoneStatusBar, notification.getKey());
                boolean sensitive = (sensitiveNote && hideSensitive) || sensitivePackage;
                boolean showingPublic = sensitive && (boolean) invoke(Methods.SystemUI.BaseStatusBar.isLockscreenPublicMode, phoneStatusBar);
//                if (showingPublic) { //TODO: implement maybe?
//                    updatePublicContentView(ent, ent.notification);
//                }
                FrameLayout row = get(Fields.SystemUI.NotificationDataEntry.row, ent);
                invoke(Methods.SystemUI.ExpandableNotificationRow.setSensitive, row, sensitive/*, hideSensitive*/);
                if (getBoolean(Fields.SystemUI.NotificationDataEntry.autoRedacted, ent) && getBoolean(Fields.SystemUI.NotificationDataEntry.legacy, ent)) {
                    // TODO: Also fade this? Or, maybe easier (and better), provide a dark redacted form
                    // for legacy auto redacted notifications.
                    if (showingPublic) {
                        invoke(Methods.SystemUI.ExpandableNotificationRow.setShowingLegacyBackground, row, false);
                    } else {
                        invoke(Methods.SystemUI.ExpandableNotificationRow.setShowingLegacyBackground, row, true);
                    }
                }
                if (invoke(Methods.SystemUI.NotificationGroupManager.isChildInGroupWithSummary, mGroupManager, invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, row))) {
                    FrameLayout summary = invoke(Methods.SystemUI.NotificationGroupManager.getGroupSummary, mGroupManager,
                            invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, row));
                    List<FrameLayout> orderedChildren =
                            (List) mTmpChildOrderMap.get(summary);
                    if (orderedChildren == null) {
                        orderedChildren = new ArrayList<>();
                        mTmpChildOrderMap.put(summary, orderedChildren);
                    }
                    orderedChildren.add(row);
                } else {
                    toShow.add(row);
                }

            }

            ArrayList<FrameLayout> toRemove = new ArrayList<>();
            for (int i=0; i< mStackScroller.getChildCount(); i++) {
                View child = mStackScroller.getChildAt(i);
                if (!toShow.contains(child) && SystemUI.ExpandableNotificationRow.isInstance(child)) {
                    toRemove.add((FrameLayout) child);
                }
            }

            for (FrameLayout remove : toRemove) {
                ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(remove);
                if (invoke(Methods.SystemUI.NotificationGroupManager.isChildInGroupWithSummary, mGroupManager, invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, remove))) {
                    // we are only transfering this notification to its parent, don't generate an animation
                    NotificationStackScrollLayoutHooks.setChildTransferInProgress(true);
                }
                if (helper.isSummaryWithChildren()) {
                    helper.removeAllChildren();
                }
                mStackScroller.removeView(remove);
                NotificationStackScrollLayoutHooks.setChildTransferInProgress(false);
            }

            removeNotificationChildren(phoneStatusBar);

            for (int i=0; i<toShow.size(); i++) {
                View v = toShow.get(i);
                if (v.getParent() == null) {
                    mStackScroller.addView(v);
                }
            }

            addNotificationChildrenAndSort(phoneStatusBar);

            // So after all this work notifications still aren't sorted correctly.
            // Let's do that now by advancing through toShow and mStackScroller in
            // lock-step, making sure mStackScroller matches what we see in toShow.
            int j = 0;
            for (int i = 0; i < mStackScroller.getChildCount(); i++) {
                View child = mStackScroller.getChildAt(i);
                if (!(SystemUI.ExpandableNotificationRow.isInstance(child))) {
                    // We don't care about non-notification views.
                    continue;
                }

                FrameLayout targetChild = toShow.get(j);
                if (child != targetChild) {
                    // Oops, wrong notification at this position. Put the right one
                    // here and advance both lists.
                    invoke(Methods.SystemUI.NotificationStackScrollLayout.changeViewPosition, mStackScroller, targetChild, i);
                }
                j++;

            }
            // clear the map again for the next usage
            mTmpChildOrderMap.clear();

            invoke(Methods.SystemUI.PhoneStatusBar.updateRowStates, phoneStatusBar);
            invoke(Methods.SystemUI.PhoneStatusBar.updateSpeedbump, phoneStatusBar);
            invoke(Methods.SystemUI.PhoneStatusBar.updateClearAll, phoneStatusBar);
            invoke(Methods.SystemUI.PhoneStatusBar.updateEmptyShadeView, phoneStatusBar);

            invoke(Methods.SystemUI.PhoneStatusBar.updateQsExpansionEnabled, phoneStatusBar);
            XposedHelpers.callMethod(get(Fields.SystemUI.PhoneStatusBar.mShadeUpdates, phoneStatusBar), "check");
            if (RomUtils.isXperia())
                XposedHelpers.callMethod(get(Fields.SystemUI.PhoneStatusBar.mIconController, phoneStatusBar) ,"updateNotificationIcons", mNotificationData);

            return null;
        }
    };

    public static boolean isTrackingHeadsUp(Object headsUpManager) {
        return getBoolean(Fields.SystemUI.HeadsUpManager.mTrackingHeadsUp, headsUpManager);
    }

    public static void setRemoteInputActive(Object headsUpManager, Object entry, boolean remoteInputActive) {
        HashMap<String, Object> mHeadsUpEntries = get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, headsUpManager);
        Object headsUpEntry = mHeadsUpEntries.get(get(Fields.SystemUI.NotificationDataEntry.key, entry));
        if (headsUpEntry != null) {
            Object isRemoteInputActive = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "remoteInputActive");
            boolean headsUpRemoteInputActive = isRemoteInputActive != null && (boolean) isRemoteInputActive;
            if (headsUpRemoteInputActive != remoteInputActive) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "remoteInputActive", remoteInputActive);
                if (remoteInputActive) {
                    invoke(Methods.SystemUI.HeadsUpEntry.removeAutoRemovalCallbacks, headsUpEntry);
                } else {
                    updateEntry(headsUpEntry, false /* updatePostTime */);
                }
            }
        }
    }

    /**
     * Set an entry to be expanded and therefore stick in the heads up area if it's pinned
     * until it's collapsed again.
     */
    public static void setExpanded(Object headsUpManager, Object entry, boolean expanded) {
        HashMap<String, Object> mHeadsUpEntries = get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, headsUpManager);
        Object headsUpEntry = mHeadsUpEntries.get(get(Fields.SystemUI.NotificationDataEntry.key, entry));
        if (headsUpEntry != null) {
            Object isExpanded = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "expanded");
            boolean headsUpExpanded = isExpanded != null && (boolean) isExpanded;
            if (headsUpExpanded != expanded) {
                XposedHelpers.setAdditionalInstanceField(headsUpEntry, "expanded", expanded);
                if (expanded) {
                    invoke(Methods.SystemUI.HeadsUpEntry.removeAutoRemovalCallbacks, headsUpEntry);
                } else {
                    updateEntry(headsUpEntry, false /* updatePostTime */);
                }
            }
        }
    }

    public static void updateEntry(Object headsUpEntry, boolean updatePostTime) {
        Object headsUpManager = XposedHelpers.getSurroundingThis(headsUpEntry);
        Object entry = get(Fields.SystemUI.HeadsUpEntry.entry, headsUpEntry);
        Object mClock = get(Fields.SystemUI.HeadsUpManager.mClock, headsUpManager);
        HashSet mEntriesToRemoveAfterExpand = get(Fields.SystemUI.HeadsUpManager.mEntriesToRemoveAfterExpand, headsUpManager);
        Handler mHandler = get(Fields.SystemUI.HeadsUpManager.mHandler, headsUpManager);
        int mMinimumDisplayTime = getInt(Fields.SystemUI.HeadsUpManager.mMinimumDisplayTime, headsUpManager);
        long postTime = getLong(Fields.SystemUI.HeadsUpEntry.postTime, headsUpEntry);
        long currentTime = (long) XposedHelpers.callMethod(mClock, "currentTimeMillis");
        set(Fields.SystemUI.HeadsUpEntry.earliestRemovaltime, headsUpEntry, currentTime + mMinimumDisplayTime);
        if (updatePostTime) {
            set(Fields.SystemUI.HeadsUpEntry.postTime, headsUpEntry, Math.max(postTime, currentTime));
            postTime = Math.max(postTime, currentTime);
        }
        invoke(Methods.SystemUI.HeadsUpEntry.removeAutoRemovalCallbacks, headsUpEntry);
        if (mEntriesToRemoveAfterExpand.contains(entry)) {
            mEntriesToRemoveAfterExpand.remove(entry);
        }
        if (!isSticky(headsUpEntry)) {
            long finishTime = postTime + getInt(Fields.SystemUI.HeadsUpManager.mHeadsUpNotificationDecay, headsUpManager);
            long removeDelay = Math.max(finishTime - currentTime, mMinimumDisplayTime);
            mHandler.postDelayed((Runnable) get(Fields.SystemUI.HeadsUpEntry.mRemoveHeadsUpRunnable, headsUpEntry), removeDelay);
        }
    }

    private static boolean isSticky(Object headsUpEntry) {
        Object headsUpManager = XposedHelpers.getSurroundingThis(headsUpEntry);
        Object entry = get(Fields.SystemUI.HeadsUpEntry.entry, headsUpEntry);
        Object isExpanded = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "expanded");
        boolean expanded = isExpanded != null && (boolean) isExpanded;
        Object isRemoteInputActive = XposedHelpers.getAdditionalInstanceField(headsUpEntry, "remoteInputActive");
        boolean remoteInputActive = isRemoteInputActive != null && (boolean) isRemoteInputActive;
        boolean isPinned = getBoolean(Fields.SystemUI.ExpandableNotificationRow.mIsPinned, get(Fields.SystemUI.NotificationDataEntry.row, entry));

        return (isPinned && expanded)
                || remoteInputActive || (boolean) XposedHelpers.callMethod(headsUpManager, "hasFullScreenIntent", entry);
    }

    public static Collection getAllEntries(Object headsUpManager) {
        return ((HashMap) get(Fields.SystemUI.HeadsUpManager.mHeadsUpEntries, headsUpManager)).values();
    }

    private static void addNotificationChildrenAndSort(Object phoneStatusBar) {
        ViewGroup mStackScroller = get(Fields.SystemUI.BaseStatusBar.mStackScroller, phoneStatusBar);
        HashMap mTmpChildOrderMap = get(Fields.SystemUI.PhoneStatusBar.mTmpChildOrderMap, phoneStatusBar);
        // Let's now add all notification children which are missing
        boolean orderChanged = false;
        for (int i = 0; i < mStackScroller.getChildCount(); i++) {
            View view = mStackScroller.getChildAt(i);
            if (!(SystemUI.ExpandableNotificationRow.isInstance(view))) {
                // We don't care about non-notification views.
                continue;
            }

            FrameLayout parent = (FrameLayout) view;
            List<FrameLayout> children = invoke(Methods.SystemUI.ExpandableNotificationRow.getNotificationChildren, parent);
            List<FrameLayout> orderedChildren = (List) mTmpChildOrderMap.get(parent);

            for (int childIndex = 0; orderedChildren != null && childIndex < orderedChildren.size();
                 childIndex++) {
                FrameLayout childView = orderedChildren.get(childIndex);
                if (children == null || !children.contains(childView)) {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.addChildNotification, parent, childView, childIndex);
                    invoke(Methods.SystemUI.NotificationStackScrollLayout.notifyGroupChildAdded, mStackScroller, childView);
                }
            }

            // Finally after removing and adding has been beformed we can apply the order.
            orderChanged |= (boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.applyChildOrder, parent, orderedChildren);
        }
        if (orderChanged) {
            invoke(Methods.SystemUI.NotificationStackScrollLayout.generateChildOrderChangedEvent, mStackScroller);
        }
    }

    private static void removeNotificationChildren(Object phoneStatusBar) {
        // First let's remove all children which don't belong in the parents
        ViewGroup mStackScroller = get(Fields.SystemUI.BaseStatusBar.mStackScroller, phoneStatusBar);
        HashMap mTmpChildOrderMap = get(Fields.SystemUI.PhoneStatusBar.mTmpChildOrderMap, phoneStatusBar);
        Object mNotificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, phoneStatusBar);
        ArrayList<FrameLayout> toRemove = new ArrayList<>();
        for (int i = 0; i < mStackScroller.getChildCount(); i++) {
            View view = mStackScroller.getChildAt(i);
            if (!(Classes.SystemUI.ExpandableNotificationRow.isInstance(view))) {
                // We don't care about non-notification views.
                continue;
            }

            FrameLayout parent = (FrameLayout) view;
            List<FrameLayout> children = invoke(Methods.SystemUI.ExpandableNotificationRow.getNotificationChildren, parent);
            List<FrameLayout> orderedChildren = (List) mTmpChildOrderMap.get(parent);

            if (children != null) {
                toRemove.clear();
                for (FrameLayout  childRow : children) {
                    ExpandableNotificationRowHelper childHelper = ExpandableNotificationRowHelper.getInstance(childRow);
                    if ((orderedChildren == null
                            || !orderedChildren.contains(childRow))
                            && !childHelper.keepInParent()) {
                        toRemove.add(childRow);
                    }
                }
                for (FrameLayout  remove : toRemove) {
                    invoke(Methods.SystemUI.ExpandableNotificationRow.removeChildNotification, parent, remove);
                    if (invoke(Methods.SystemUI.NotificationData.get, mNotificationData,
                            (((StatusBarNotification) invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, remove)).getKey())) == null) {
                        // We only want to add an animation if the view is completely removed
                        // otherwise it's just a transfer
                        XposedHelpers.callMethod(mStackScroller, "notifyGroupChildRemoved", remove/*, //TODO: implement the new method for this
                                get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, parent)*/);
                    }
                }
            }
        }
    }

    /**
     * Ensures that the group children are cancelled immediately when the group summary is cancelled
     * instead of waiting for the notification manager to send all cancels. Otherwise this could
     * lead to flickers.
     *
     * This also ensures that the animation looks nice and only consists of a single disappear
     * animation instead of multiple.
     *
     * @param key the key of the notification was removed
     * @param ranking the current ranking
     */
    public static void handleGroupSummaryRemoved(Object phoneStatusBar, String key,
                                           NotificationListenerService.RankingMap ranking) {
        Object mNotificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, phoneStatusBar);
        Object entry = invoke(Methods.SystemUI.NotificationData.get, mNotificationData, key);
        FrameLayout row = null;
        ExpandableNotificationRowHelper rowHelper = null;
        if (entry != null) {
            row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
            rowHelper = ExpandableNotificationRowHelper.getInstance(row);
        }
        if (entry != null && row != null
                && rowHelper.isSummaryWithChildren()) {
            if (/*((StatusBarNotification) XposedHelpers.getObjectField(entry, "notification")).getOverrideGroupKey() != null
                    && */!rowHelper.isDismissed()) {
                // We don't want to remove children for autobundled notifications as they are not
                // always cancelled. We only remove them if they were dismissed by the user.
                return;
            }
            List<FrameLayout> notificationChildren = invoke(Methods.SystemUI.ExpandableNotificationRow.getNotificationChildren, row);
            ArrayList<FrameLayout> toRemove = new ArrayList<>(notificationChildren);
            for (int i = 0; i < toRemove.size(); i++) {
                ExpandableNotificationRowHelper toRemoveHelper = ExpandableNotificationRowHelper.getInstance(toRemove.get(i));
                toRemoveHelper.setKeepInParent(true);
                // we need to set this state earlier as otherwise we might generate some weird
                // animations
                toRemoveHelper.setRemoved();
            }
            for (int i = 0; i < toRemove.size(); i++) {
                invoke(Methods.SystemUI.BaseStatusBar.removeNotification, phoneStatusBar,
                        ((StatusBarNotification) invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, toRemove.get(i))).getKey(), ranking);
                // we need to ensure that the view is actually properly removed from the viewstate
                // as this won't happen anymore when kept in the parent.
                NotificationStackScrollLayoutHooks.removeViewStateForView(toRemove.get(i));
            }
        }
    }

//    protected static void performRemoveNotification(StatusBarNotification n, boolean removeView) { //TODO: call in BaseStatusBar
//        Entry entry = mNotificationData.get(n.getKey());
//        if (mRemoteInputController.isRemoteInputActive(entry)) {
//            mRemoteInputController.removeRemoteInput(entry, null);
//        }
//        //super.performRemoveNotification(n, removeView);
//    }

    public static boolean isSummaryWithChildren(Object row) {
        if (!SystemUI.ExpandableNotificationRow.isInstance(row))
            return false;

        Object childrenContainer = get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, row);
        return ENABLE_CHILD_NOTIFICATIONS &&
                childrenContainer != null &&
                NotificationChildrenContainerHelper.getNotificationChildCount(childrenContainer) > 0;

    }

    public static void setGroupManager(Object groupManager) {
        mGroupManager = groupManager;
    }
}

