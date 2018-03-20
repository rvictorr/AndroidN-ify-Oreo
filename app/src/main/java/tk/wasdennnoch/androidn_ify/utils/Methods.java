package tk.wasdennnoch.androidn_ify.utils;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.graphics.PointF;
import android.graphics.Rect;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telecom.Call;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

import static de.robv.android.xposed.XposedHelpers.findConstructorBestMatch;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

public final class Methods {

    private static final String TAG = Methods.class.getSimpleName();

    public static final class Android {

        public static void init() {
            Chronometer.init();
            Icon.init();
            View.init();
            ViewGroup.init();
            Notification.init();
        }

        public static final class Chronometer {
            private static final Class clazz = android.widget.Chronometer.class;

            public static Method setStarted;

            private static void init() {
                setStarted = findMethodBestMatch(clazz, "setStarted", boolean.class);
            }
        }

        public static final class Icon {
            private static final Class clazz = android.graphics.drawable.Icon.class;

            public static Method getBitmap;
            public static Method getDataLength;
            public static Method getDataOffset;
            public static Method getDataBytes;
            public static Method getResId;
            public static Method getResPackage;
            public static Method getUriString;

            public static void init() {
                getBitmap = XposedHelpers.findMethodBestMatch(clazz, "getBitmap");
                getDataLength = XposedHelpers.findMethodBestMatch(clazz, "getDataLength");
                getDataOffset = XposedHelpers.findMethodBestMatch(clazz, "getDataOffset");
                getDataBytes = XposedHelpers.findMethodBestMatch(clazz, "getDataBytes");
                getResId = XposedHelpers.findMethodBestMatch(clazz, "getResId");
                getResPackage = XposedHelpers.findMethodBestMatch(clazz, "getResPackage");
                getUriString = XposedHelpers.findMethodBestMatch(clazz, "getUriString");
            }
        }

        public static final class View {
            private static final Class clazz = android.view.View.class;

            public static Method isLayoutRtl;
            public static Method requestAccessibilityFocus;
            public static Method setMeasuredDimension;
            public static Method setTagInternal;
            public static Method getBoundsOnScreen;

            private static void init() {
                isLayoutRtl = findMethodBestMatch(clazz, "isLayoutRtl");
                requestAccessibilityFocus = findMethodBestMatch(clazz, "requestAccessibilityFocus");
                setMeasuredDimension = findMethodBestMatch(clazz, "setMeasuredDimension", int.class, int.class);
                setTagInternal = findMethodBestMatch(clazz, "setTagInternal", int.class, Object.class);
                getBoundsOnScreen = XposedHelpers.findMethodBestMatch(clazz, "getBoundsOnScreen", Rect.class, boolean.class);
            }
        }

        public static final class ViewGroup {
            private static final Class clazz = android.view.ViewGroup.class;

            public static Method addTransientView;
            public static Method removeTransientView;
            public static Method getTransientView;
            public static Method getTransientViewCount;

            private static void init() {
                addTransientView = findMethodBestMatch(clazz, "addTransientView", android.view.View.class, int.class);
                removeTransientView = findMethodBestMatch(clazz, "removeTransientView", android.view.View.class);
                getTransientView = findMethodBestMatch(clazz, "getTransientView", int.class);
                getTransientViewCount = findMethodBestMatch(clazz, "getTransientViewCount");
            }
        }

        public static final class Notification {
            private static final Class clazz = android.app.Notification.class;

            public static Method isGroupSummary;

            private static void init() {
                isGroupSummary = findMethodBestMatch(clazz, "isGroupSummary");

                BuilderRemoteViews.init();
            }

            public static final class BuilderRemoteViews {
                private static final Class clazz = Classes.Android.BuilderRemoteViews;

                public static Constructor constructor;

                private static void init() {
                    constructor = findConstructorBestMatch(clazz, ApplicationInfo.class, int.class);
                }
            }
        }
    }
    
    public static final class SystemUI {

        public static void init() {
            BaseStatusBar.init();
            PhoneStatusBar.init();
            StatusBarWindowManagerState.init();
            QSContainer.init();
            QSTile.init();
            QSTileView.init();
            StatusBarHeaderView.init();
            NotificationPanelView.init();
            NotificationStackScrollLayout.init();
            SwipeHelper.init();
            ExpandHelper.init();
            ViewScaler.init();
            StackScrollAlgorithm.init();
            StackStateAnimator.init();
            StackScrollState.init();
            AmbientState.init();
            ViewState.init();
            NotificationBackgroundView.init();
            ExpandableView.init();
            ActivatableNotificationView.init();
            NotificationContentView.init();
            ExpandableNotificationRow.init();
            NotificationChildrenContainer.init();
            NotificationGroupManager.init();
            HeadsUpManager.init();
            HeadsUpEntry.init();
            NotificationData.init();
        }

        public static final class BaseStatusBar {
            private static final Class clazz = Classes.SystemUI.BaseStatusBar;

            public static Method shouldShowOnKeyguard;
            public static Method userAllowsPrivateNotificationsInPublic;
            public static Method isLockscreenPublicMode;
            public static Method updateRowStates;
            public static Method updateNotification;
            public static Method addNotification;
            public static Method removeNotification;
            public static Method removeNotificationViews;

            private static void init() {
                try {
                    shouldShowOnKeyguard = findMethodBestMatch(clazz, "shouldShowOnKeyguard", StatusBarNotification.class);
                } catch (NoSuchMethodError ignore) { //Xperia
                    shouldShowOnKeyguard = findMethodBestMatch(clazz, "shouldShowOnKeyguard", StatusBarNotification.class, boolean.class);
                }
                userAllowsPrivateNotificationsInPublic = findMethodBestMatch(clazz, "userAllowsPrivateNotificationsInPublic", int.class);
                isLockscreenPublicMode = findMethodBestMatch(clazz, "isLockscreenPublicMode");
                updateRowStates = findMethodBestMatch(clazz, "updateRowStates");
                updateNotification = findMethodBestMatch(clazz, "updateNotification", StatusBarNotification.class, NotificationListenerService.RankingMap.class);
                addNotification = findMethodBestMatch(clazz, "addNotification", StatusBarNotification.class, NotificationListenerService.RankingMap.class, Classes.SystemUI.NotificationDataEntry);
                removeNotification = findMethodBestMatch(clazz, "removeNotification", String.class, NotificationListenerService.RankingMap.class);
                removeNotificationViews = findMethodBestMatch(clazz, "removeNotificationViews", String.class, NotificationListenerService.RankingMap.class);
            }
        }

        public static final class PhoneStatusBar {
            private static final Class clazz = Classes.SystemUI.PhoneStatusBar;

            public static Method isCollapsing;
            public static Method addPostCollapseAction;
            public static Method updateNotificationShade;
            public static Method packageHasVisibilityOverride;
            public static Method updateRowStates;
            public static Method updateSpeedbump;
            public static Method updateClearAll;
            public static Method updateEmptyShadeView;
            public static Method updateQsExpansionEnabled;
            public static Method updateNotifications;
            public static Method maybeEscalateHeadsUp;
            public static Method resetUserExpandedStates;
            public static Method getBarState;
            public static Method hasActiveNotifications;
            public static Method animateCollapsePanels;
            public static Method goToKeyguard;
            public static Method setAreThereNotifications;

            private static void init() {
                isCollapsing = findMethodBestMatch(clazz, "isCollapsing");
                addPostCollapseAction = findMethodBestMatch(clazz, "addPostCollapseAction", Runnable.class);
                updateNotificationShade = findMethodBestMatch(clazz, "updateNotificationShade");
                packageHasVisibilityOverride = findMethodBestMatch(clazz, "packageHasVisibilityOverride", String.class);
                updateRowStates = findMethodBestMatch(clazz, "updateRowStates");
                updateSpeedbump = findMethodBestMatch(clazz, "updateSpeedbump");
                updateClearAll = findMethodBestMatch(clazz, "updateClearAll");
                updateEmptyShadeView = findMethodBestMatch(clazz, "updateEmptyShadeView");
                updateQsExpansionEnabled = findMethodBestMatch(clazz, "updateQsExpansionEnabled");
                updateNotifications = findMethodBestMatch(clazz, "updateNotifications");
                maybeEscalateHeadsUp = findMethodBestMatch(clazz, "maybeEscalateHeadsUp");
                resetUserExpandedStates = findMethodBestMatch(clazz, "resetUserExpandedStates");
                getBarState = findMethodBestMatch(clazz, "getBarState");
                hasActiveNotifications = findMethodBestMatch(clazz, "hasActiveNotifications");
                animateCollapsePanels = findMethodBestMatch(clazz, "animateCollapsePanels");
                goToKeyguard = findMethodBestMatch(clazz, "goToKeyguard");
                setAreThereNotifications = findMethodBestMatch(clazz, "setAreThereNotifications");
            }
        }

        public static final class StatusBarWindowManagerState {
            private static final Class clazz = Classes.SystemUI.StatusBarWindowManagerState;

            public static Method isKeyguardShowingAndNotOccluded;

            private static void init() {
                isKeyguardShowingAndNotOccluded = findMethodBestMatch(clazz, "isKeyguardShowingAndNotOccluded");
            }
        }

        public static final class QSContainer {
            private static final Class clazz = Classes.SystemUI.QSContainer;

            public static Method setHeightOverride;
            public static Method getDesiredHeight;

            private static void init() {
                setHeightOverride = findMethodBestMatch(clazz, "setHeightOverride", int.class);
                getDesiredHeight = findMethodBestMatch(clazz, "getDesiredHeight");
            }
        }

        public static final class QSTile {
            private static final Class clazz = Classes.SystemUI.QSTile;

            public static Method supportsDualTargets;

            private static void init() {
                supportsDualTargets = findMethodBestMatch(clazz, "supportsDualTargets");
            }
        }

        public static final class QSTileView {
            private static final Class clazz = Classes.SystemUI.QSTileView;

            public static Method setDual;

            private static void init() {
                try {
                    setDual = findMethodBestMatch(clazz, "setDual", boolean.class);
                } catch (NoSuchMethodError ignore) {//LOS I guess
                    setDual = findMethodBestMatch(clazz, "setDual", boolean.class, boolean.class);
                }
            }
        }

        public static final class StatusBarHeaderView {
            private static final Class clazz = Classes.SystemUI.StatusBarHeaderView;

            public static Method setExpansion;
            public static Method setExpanded;

            private static void init() {
                setExpansion = findMethodBestMatch(clazz, "setExpansion", float.class);
                setExpanded = findMethodBestMatch(clazz, "setExpanded", boolean.class);
            }
        }

        public static final class NotificationPanelView {
            private static final Class clazz = Classes.SystemUI.NotificationPanelView;
            private static final Class classPanelView = Classes.SystemUI.PanelView;

            public static Method layoutChildren;
            public static Method requestPanelHeightUpdate;
            public static Method abortAnimations;
            public static Method fling;
            public static Method positionClockAndNotifications;
            public static Method requestScrollerTopPaddingUpdate;
            public static Method startQsSizeChangeAnimation;
            public static Method getQsExpansionFraction;
            public static Method getHeaderTranslation;
            public static Method updateStackHeight;
            public static Method getExpandedHeight;
            public static Method updateHeader;
            public static Method updateMaxHeadsUpTranslation;
            public static Method calculatePanelHeightQsExpanded;
            public static Method calculatePanelHeightShade;
            public static Method getOverExpansionAmount;
            public static Method updateQsState;
            public static Method updateClock;
            public static Method isTracking;
            public static Method isQsExpanded;

            private static void init() {
                layoutChildren = findMethodBestMatch(FrameLayout.class, "layoutChildren", int.class, int.class, int.class, int.class, boolean.class);
                requestPanelHeightUpdate = findMethodBestMatch(classPanelView, "requestPanelHeightUpdate");
                abortAnimations = findMethodBestMatch(classPanelView, "abortAnimations");
                fling = findMethodBestMatch(clazz, "fling", float.class, boolean.class);
                positionClockAndNotifications = findMethodBestMatch(clazz, "positionClockAndNotifications");
                requestScrollerTopPaddingUpdate = findMethodBestMatch(clazz, "requestScrollerTopPaddingUpdate", boolean.class);
                startQsSizeChangeAnimation = findMethodBestMatch(clazz, "startQsSizeChangeAnimation", int.class, int.class);
                getQsExpansionFraction = findMethodBestMatch(clazz, "getQsExpansionFraction");
                getHeaderTranslation = findMethodBestMatch(clazz, "getHeaderTranslation");
                updateStackHeight = findMethodBestMatch(clazz, "updateStackHeight", float.class);
                getExpandedHeight = findMethodBestMatch(clazz, "getExpandedHeight");
                updateHeader = findMethodBestMatch(clazz, "updateHeader");
                updateMaxHeadsUpTranslation = findMethodBestMatch(clazz, "updateMaxHeadsUpTranslation");
                calculatePanelHeightQsExpanded = findMethodBestMatch(clazz, "calculatePanelHeightQsExpanded");
                calculatePanelHeightShade = findMethodBestMatch(clazz, "calculatePanelHeightShade");
                getOverExpansionAmount = findMethodBestMatch(clazz, "getOverExpansionAmount");
                updateQsState = findMethodBestMatch(clazz, "updateQsState");
                updateClock = findMethodBestMatch(clazz, "updateClock", float.class, float.class);
                isTracking = findMethodBestMatch(clazz, "isTracking");
                isQsExpanded = findMethodBestMatch(clazz, "isQsExpanded");
            }
        }

        public static final class StackScrollAlgorithm {
            private static final Class clazz = Classes.SystemUI.StackScrollAlgorithm;

            public static Method handleDraggedViews;
            public static Method updateDimmedActivatedHideSensitive;
            public static Method updateSpeedBumpState;
            public static Method getNotificationChildrenStates;
            public static Method getStackScrollState;
            public static Method updateVisibleChildren;
            public static Method updateStateForChildTransitioningInBottom;
            public static Method updateStateForChildFullyInBottomStack;
            public static Method updateNotGoneIndex;

            private static void init() {
                handleDraggedViews = findMethodBestMatch(clazz, "handleDraggedViews",
                        Classes.SystemUI.AmbientState, Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateDimmedActivatedHideSensitive = findMethodBestMatch(clazz, "updateDimmedActivatedHideSensitive",
                        Classes.SystemUI.AmbientState, Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateSpeedBumpState = findMethodBestMatch(clazz, "updateSpeedBumpState",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState, int.class);
                getNotificationChildrenStates = findMethodBestMatch(clazz, "getNotificationChildrenStates",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                getStackScrollState = findMethodBestMatch(clazz, "getStackScrollState", Classes.SystemUI.AmbientState, Classes.SystemUI.StackScrollState);
                updateVisibleChildren = findMethodBestMatch(clazz, "updateVisibleChildren",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateStateForChildTransitioningInBottom = findMethodBestMatch(clazz, "updateStateForChildTransitioningInBottom",
                        Classes.SystemUI.StackScrollAlgorithmState, float.class, float.class, float.class, Classes.SystemUI.StackViewState, int.class);
                updateStateForChildFullyInBottomStack = findMethodBestMatch(clazz, "updateStateForChildFullyInBottomStack",
                        Classes.SystemUI.StackScrollAlgorithmState, float.class, Classes.SystemUI.StackViewState, int.class, Classes.SystemUI.AmbientState);
                updateNotGoneIndex = findMethodBestMatch(clazz, "updateNotGoneIndex",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState, int.class, Classes.SystemUI.ExpandableView);
            }
        }

        public static final class StackStateAnimator {
            private static final Class clazz = Classes.SystemUI.StackStateAnimator;

            public static Method getFinalActualHeight;
            public static Method startViewAnimations;
            public static Method startStackAnimations;
            public static Method cancelAnimatorAndGetNewDuration;
            public static Method getGlobalAnimationFinishedListener;
            public static Method startAnimator;

            private static void init() {
                getFinalActualHeight = findMethodBestMatch(clazz, "getFinalActualHeight", Classes.SystemUI.ExpandableView);
                startViewAnimations = findMethodBestMatch(clazz, "startViewAnimations", View.class, Classes.SystemUI.ViewState, long.class, long.class);
                startStackAnimations = findMethodBestMatch(clazz, "startStackAnimations",
                        Classes.SystemUI.ExpandableView, Classes.SystemUI.StackViewState, Classes.SystemUI.StackScrollState, int.class, long.class);
                cancelAnimatorAndGetNewDuration = findMethodBestMatch(clazz, "cancelAnimatorAndGetNewDuration", long.class, ValueAnimator.class);
                getGlobalAnimationFinishedListener = findMethodBestMatch(clazz, "getGlobalAnimationFinishedListener");
                startAnimator = findMethodBestMatch(clazz, "startAnimator", ValueAnimator.class);
            }
        }

        public static final class StackScrollState {
            private static final Class clazz = Classes.SystemUI.StackScrollState;

            public static Method getViewStateForView;
            public static Method removeViewStateForView;
            public static Method resetViewStates;
            public static Method resetViewState;
            public static Method applyState;
            public static Method applyViewState;
            public static Method getHostView;

            private static void init() {
                getViewStateForView = XposedHelpers.findMethodBestMatch(clazz, "getViewStateForView", View.class);
                removeViewStateForView = findMethodBestMatch(clazz, "removeViewStateForView", View.class);
                resetViewStates = XposedHelpers.findMethodBestMatch(clazz, "resetViewStates");
                resetViewState = findMethodBestMatch(clazz, "resetViewState", Classes.SystemUI.ExpandableView);
                applyState = XposedHelpers.findMethodBestMatch(clazz, "applyState", Classes.SystemUI.ExpandableView, Classes.SystemUI.StackViewState);
                applyViewState = XposedHelpers.findMethodBestMatch(clazz, "applyViewState", View.class, Classes.SystemUI.ViewState);
                getHostView = findMethodBestMatch(clazz, "getHostView");
            }
        }

        public static final class AmbientState {
            private static final Class clazz = Classes.SystemUI.AmbientState;

            public static Method setLayoutHeight;
            public static Method isDark;
            public static Method setDimmed;
            public static Method setDark;
            public static Method setSpeedBumpIndex;

            private static void init() {
                setLayoutHeight = findMethodBestMatch(clazz, "setLayoutHeight", int.class);
                isDark = findMethodBestMatch(clazz, "isDark");
                setDimmed = findMethodBestMatch(clazz, "setDimmed", boolean.class);
                setDark = findMethodBestMatch(clazz, "setDark", boolean.class);
                setSpeedBumpIndex = findMethodBestMatch(clazz, "setSpeedBumpIndex", int.class);
            }
        }

        public static final class ViewState {
            private static final Class clazz = Classes.SystemUI.ViewState;

            public static Constructor constructor;

            public static Method copyFrom;
            public static Method initFrom;

            private static void init() {
                constructor = XposedHelpers.findConstructorBestMatch(clazz);

                copyFrom = findMethodBestMatch(clazz, "copyFrom", clazz);
                initFrom = findMethodBestMatch(clazz, "initFrom", View.class);
            }
        }

        public static final class NotificationStackScrollLayout {
            private static final Class clazz = Classes.SystemUI.NotificationStackScrollLayout;

            public static Method setIsExpanded;
            public static Method updateAlgorithmHeightAndPadding;
            public static Method requestChildrenUpdate;
            public static Method setStackTranslation;
            public static Method setScrollingEnabled;
            public static Method isAddOrRemoveAnimationPending;
            public static Method setStackHeight;
            public static Method getPositionInLinearLayout;
            public static Method getScrollRange;
            public static Method getIntrinsicHeight;
            public static Method isChildInGroup;
            public static Method clampScrollPosition;
            public static Method getChildAtPosition;
            public static Method getFirstChildNotGone;
            public static Method notifyGroupChildAdded;
            public static Method generateChildHierarchyEvents;
            public static Method generateChildOrderChangedEvent;
            public static Method changeViewPosition;
            public static Method onHeightChanged;
            public static Method setIntrinsicPadding;
            public static Method generateAddAnimation;
            public static Method updateOverflowContainerVisibility;
            public static Method getEmptyBottomMargin;
            public static Method getTopPadding;
            public static Method getEmptyShadeViewHeight;
            public static Method getBottomStackPeekSize;
            public static Method getTopPaddingOverflow;
            public static Method getNotGoneChildCount;
            public static Method getContentHeight;
            public static Method isCurrentlyAnimating;
            public static Method findDarkAnimationOriginIndex;

            private static void init() {
                setIsExpanded = findMethodBestMatch(clazz, "setIsExpanded", boolean.class);
                updateAlgorithmHeightAndPadding = findMethodBestMatch(clazz, "updateAlgorithmHeightAndPadding");
                requestChildrenUpdate = findMethodBestMatch(clazz, "requestChildrenUpdate");
                setStackTranslation = findMethodBestMatch(clazz, "setStackTranslation", float.class);
                setScrollingEnabled = findMethodBestMatch(clazz, "setScrollingEnabled", boolean.class);
                isAddOrRemoveAnimationPending = findMethodBestMatch(clazz, "isAddOrRemoveAnimationPending");
                setStackHeight = findMethodBestMatch(clazz, "setStackHeight", float.class);
                getPositionInLinearLayout = findMethodBestMatch(clazz, "getPositionInLinearLayout", View.class);
                getScrollRange = findMethodBestMatch(clazz, "getScrollRange");
                getIntrinsicHeight = XposedHelpers.findMethodBestMatch(clazz, "getIntrinsicHeight", View.class);
                isChildInGroup = XposedHelpers.findMethodBestMatch(clazz, "isChildInGroup", View.class);
                clampScrollPosition = findMethodBestMatch(clazz, "clampScrollPosition");
                getChildAtPosition = findMethodBestMatch(clazz, "getChildAtPosition", float.class, float.class);
                getFirstChildNotGone = findMethodBestMatch(clazz, "getFirstChildNotGone");
                notifyGroupChildAdded = findMethodBestMatch(clazz, "notifyGroupChildAdded", View.class);
                generateChildHierarchyEvents = findMethodBestMatch(clazz, "generateChildHierarchyEvents");
                generateChildOrderChangedEvent = findMethodBestMatch(clazz, "generateChildOrderChangedEvent");
                changeViewPosition = findMethodBestMatch(clazz, "changeViewPosition", View.class, int.class);
                onHeightChanged = findMethodBestMatch(clazz, "onHeightChanged", Classes.SystemUI.ExpandableView, boolean.class);
                setIntrinsicPadding = findMethodBestMatch(clazz, "setIntrinsicPadding", int.class);
                generateAddAnimation = findMethodBestMatch(clazz, "generateAddAnimation", View.class, boolean.class);
                updateOverflowContainerVisibility = findMethodBestMatch(clazz, "updateOverflowContainerVisibility", boolean.class);
                getEmptyBottomMargin = findMethodBestMatch(clazz, "getEmptyBottomMargin");
                getTopPadding = findMethodBestMatch(clazz, "getTopPadding");
                getEmptyShadeViewHeight = findMethodBestMatch(clazz, "getEmptyShadeViewHeight");
                getBottomStackPeekSize = findMethodBestMatch(clazz, "getBottomStackPeekSize");
                getTopPaddingOverflow = findMethodBestMatch(clazz, "getTopPaddingOverflow");
                getNotGoneChildCount = findMethodBestMatch(clazz, "getNotGoneChildCount");
                getContentHeight = findMethodBestMatch(clazz, "getContentHeight");
                isCurrentlyAnimating = findMethodBestMatch(clazz, "isCurrentlyAnimating");
                findDarkAnimationOriginIndex = findMethodBestMatch(clazz, "findDarkAnimationOriginIndex", PointF.class);
            }
        }

        public static final class SwipeHelper {
            private static final Class clazz = Classes.SystemUI.SwipeHelper;

            public static Method getTranslation;
            public static Method getSize;
            public static Method updateSwipeProgressFromOffset;

            private static void init() {
                getTranslation = findMethodBestMatch(clazz, "getTranslation", View.class);
                getSize = findMethodBestMatch(clazz, "getSize", View.class);
                updateSwipeProgressFromOffset = findMethodBestMatch(clazz, "updateSwipeProgressFromOffset", View.class, boolean.class);

                Callback.init();
            }

            public static final class Callback {
                private static final Class clazz = Classes.SystemUI.SwipeHelperCallback;

                public static Method canChildBeDismissed;

                private static void init() {
                    canChildBeDismissed = findMethodBestMatch(clazz, "canChildBeDismissed", View.class);
                }
            }
        }

        public static final class ExpandHelper {
            private static final Class clazz = Classes.SystemUI.ExpandHelper;

            private static void init() {
                Callback.init();
            }

            public static final class Callback {
                private static final Class clazz = Classes.SystemUI.ExpandHelperCallback;

                public static Method setUserExpandedChild;
                public static Method setUserLockedChild;
                public static Method expansionStateChanged;
                public static Method canChildBeExpanded;

                private static void init() {
                    setUserExpandedChild = findMethodBestMatch(clazz, "setUserExpandedChild", View.class, boolean.class);
                    setUserLockedChild = findMethodBestMatch(clazz, "setUserLockedChild", View.class, boolean.class);
                    expansionStateChanged = findMethodBestMatch(clazz, "expansionStateChanged", boolean.class);
                    canChildBeExpanded = findMethodBestMatch(clazz, "canChildBeExpanded", View.class);
                }
            }
        }

        public static final class ViewScaler {
            private static final Class clazz = Classes.SystemUI.ViewScaler;

            public static Method setView;
            public static Method setHeight;
            public static Method getHeight;
            public static Method getNaturalHeight;


            private static void init() {
                setView = findMethodBestMatch(clazz, "setView", Classes.SystemUI.ExpandableView);
                setHeight = findMethodBestMatch(clazz, "setHeight", float.class);
                getHeight = findMethodBestMatch(clazz, "getHeight");
                getNaturalHeight = findMethodBestMatch(clazz, "getNaturalHeight", int.class);
            }
        }

        public static final class NotificationBackgroundView {
            private static final Class clazz = Classes.SystemUI.NotificationBackgroundView;

            public static Method setRippleColor;
            public static Method setTint;
            public static Method getActualHeight;

            private static void init() {
                setRippleColor = findMethodBestMatch(clazz, "setRippleColor", int.class);
                setTint = findMethodBestMatch(clazz, "setTint", int.class);
                getActualHeight = findMethodBestMatch(clazz, "getActualHeight");
            }
        }

        public static final class ExpandableView {
            private static final Class clazz = Classes.SystemUI.ExpandableView;

            public static Method isTransparent;
            public static Method getIntrinsicHeight;
            public static Method getActualHeight;
            public static Method getClipTopAmount;
            public static Method setActualHeight;
            public static Method setDimmed;
            public static Method setHideSensitive;
            public static Method setBelowSpeedBump;
            public static Method setDark;
            public static Method setClipTopAmount;
            public static Method updateClipping;
            public static Method getMaxContentHeight;
            public static Method getMinHeight;
            public static Method areChildrenExpanded;

            private static void init() {
                isTransparent = findMethodBestMatch(clazz, "isTransparent");
                getIntrinsicHeight = findMethodBestMatch(clazz, "getIntrinsicHeight");
                getActualHeight = findMethodBestMatch(clazz, "getActualHeight");
                getClipTopAmount = findMethodBestMatch(clazz, "getClipTopAmount");
                setActualHeight = findMethodBestMatch(clazz, "setActualHeight", int.class, boolean.class);
                setDimmed = findMethodBestMatch(clazz, "setDimmed", boolean.class, boolean.class);
                setHideSensitive = findMethodBestMatch(clazz, "setHideSensitive", boolean.class, boolean.class, long.class, long.class);
                setBelowSpeedBump = findMethodBestMatch(clazz, "setBelowSpeedBump", boolean.class);
                setDark = findMethodBestMatch(clazz, "setDark", boolean.class, boolean.class, long.class);
                setClipTopAmount = findMethodBestMatch(clazz, "setClipTopAmount", int.class);
                updateClipping = findMethodBestMatch(clazz, "updateClipping");
                getMaxContentHeight = findMethodBestMatch(clazz, "getMaxContentHeight");
                getMinHeight = findMethodBestMatch(clazz, "getMinHeight");
                areChildrenExpanded = findMethodBestMatch(clazz, "areChildrenExpanded");
            }
        }

        public static class ActivatableNotificationView {
            private static final Class clazz = Classes.SystemUI.ActivatableNotificationView;

            public static Method getRippleColor;
            public static Method makeInactive;
            public static Method cancelAppearAnimation;
            public static Method startActivateAnimation;
            public static Method fadeDimmedBackground;
            public static Method updateAppearAnimationAlpha;
            public static Method updateAppearRect;
            public static Method enableAppearDrawing;
            public static Method fadeInFromDark;
            public static Method setContentAlpha;
            public static Method updateBackground;

            private static void init() {
                getRippleColor = findMethodBestMatch(clazz, "getRippleColor");
                makeInactive = findMethodBestMatch(clazz, "makeInactive", boolean.class);
                cancelAppearAnimation = findMethodBestMatch(clazz, "cancelAppearAnimation");
                startActivateAnimation = findMethodBestMatch(clazz, "startActivateAnimation", boolean.class);
                fadeDimmedBackground = findMethodBestMatch(clazz, "fadeDimmedBackground");
                updateAppearAnimationAlpha = findMethodBestMatch(clazz, "updateAppearAnimationAlpha");
                updateAppearRect = findMethodBestMatch(clazz, "updateAppearRect");
                enableAppearDrawing = findMethodBestMatch(clazz, "enableAppearDrawing", boolean.class);
                fadeInFromDark = findMethodBestMatch(clazz, "fadeInFromDark", long.class);
                setContentAlpha = findMethodBestMatch(clazz, "setContentAlpha", float.class);
                updateBackground = findMethodBestMatch(clazz, "updateBackground");
            }
        }

        public static class NotificationContentView {
            private static final Class clazz = Classes.SystemUI.NotificationContentView;

            public static Method getExpandedChild;
            public static Method getContractedChild;
            public static Method getHeadsUpChild;
            public static Method updateClipping;
            public static Method runSwitchAnimation;
            public static Method calculateVisibleType;
            public static Method updateViewVisibilities;
            public static Method selectLayout;
            public static Method setDark;

            private static void init() {
                getExpandedChild = findMethodBestMatch(clazz, "getExpandedChild");
                getContractedChild = findMethodBestMatch(clazz, "getContractedChild");
                getHeadsUpChild = findMethodBestMatch(clazz, "getHeadsUpChild");
                updateClipping = findMethodBestMatch(clazz, "updateClipping");
                runSwitchAnimation = findMethodBestMatch(clazz, "runSwitchAnimation", int.class);
                calculateVisibleType = findMethodBestMatch(clazz, "calculateVisibleType");
                updateViewVisibilities = findMethodBestMatch(clazz, "updateViewVisibilities", int.class);
                selectLayout = findMethodBestMatch(clazz, "selectLayout", boolean.class, boolean.class);
                setDark = findMethodBestMatch(clazz, "setDark", boolean.class, boolean.class, long.class);
            }
        }


        public static class ExpandableNotificationRow {
            private static final Class clazz = Classes.SystemUI.ExpandableNotificationRow;

            public static Method isUserLocked;
            public static Method isExpanded;
            public static Method isExpandable;
            public static Method isHeadsUp;
            public static Method getIntrinsicHeight;
            public static Method getMaxContentHeight;
            public static Method getActualHeight;
            public static Method getMinHeight;
            public static Method getStatusBarNotification;
            public static Method getShowingLayout;
            public static Method getNotificationChildren;
            public static Method notifyHeightChanged;
            public static Method setUserExpanded;
            public static Method setUserLocked;
            public static Method setShowingLegacyBackground;
            public static Method setIconAnimationRunning;
            public static Method setIconAnimationRunningForChild;
            public static Method removeChildNotification;
            public static Method animateShowingPublic;
            public static Method addChildNotification;
            public static Method applyChildOrder;
            public static Method setChildrenExpanded;
            public static Method setSensitive;
            public static Method setHideSensitiveForIntrinsicHeight;
            public static Method isPinned;
            public static Method getHeadsUpHeight;
            public static Method applyChildrenState;
            public static Method setExpansionDisabled;
            public static Method setSystemExpanded;
            public static Method setHeadsUp;

            private static void init() {
                isUserLocked = findMethodBestMatch(clazz, "isUserLocked");
                isExpanded = findMethodBestMatch(clazz, "isExpanded");
                isExpandable = findMethodBestMatch(clazz, "isExpandable");
                isHeadsUp = findMethodBestMatch(clazz, "isHeadsUp");
                getActualHeight = findMethodBestMatch(clazz, "getActualHeight");
                getIntrinsicHeight = findMethodBestMatch(clazz, "getIntrinsicHeight");
                getMaxContentHeight = findMethodBestMatch(clazz, "getMaxContentHeight");
                getMinHeight = findMethodBestMatch(clazz, "getMinHeight");
                getStatusBarNotification = findMethodBestMatch(clazz, "getStatusBarNotification");
                getShowingLayout = findMethodBestMatch(clazz, "getShowingLayout");
                getNotificationChildren = findMethodBestMatch(clazz, "getNotificationChildren");
                notifyHeightChanged = findMethodBestMatch(clazz, "notifyHeightChanged", boolean.class);
                setUserExpanded = findMethodBestMatch(clazz, "setUserExpanded", boolean.class);
                setUserLocked = findMethodBestMatch(clazz, "setUserLocked", boolean.class);
                setShowingLegacyBackground = findMethodBestMatch(clazz, "setShowingLegacyBackground", boolean.class);
                setIconAnimationRunning = findMethodBestMatch(clazz, "setIconAnimationRunning", boolean.class);
                setIconAnimationRunningForChild = findMethodBestMatch(clazz, "setIconAnimationRunningForChild", boolean.class, View.class);
                removeChildNotification = findMethodBestMatch(clazz, "removeChildNotification", clazz);
                animateShowingPublic = findMethodBestMatch(clazz, "animateShowingPublic", long.class, long.class);
                addChildNotification = findMethodBestMatch(clazz, "addChildNotification", Classes.SystemUI.ExpandableNotificationRow, int.class);
                applyChildOrder = findMethodBestMatch(clazz, "applyChildOrder", List.class);
                setChildrenExpanded = findMethodBestMatch(clazz, "setChildrenExpanded", boolean.class, boolean.class);
                setSensitive = findMethodBestMatch(clazz, "setSensitive", boolean.class);
                setHideSensitiveForIntrinsicHeight = findMethodBestMatch(clazz, "setHideSensitiveForIntrinsicHeight", boolean.class);
                isPinned = findMethodBestMatch(clazz, "isPinned");
                getHeadsUpHeight = findMethodBestMatch(clazz, "getHeadsUpHeight");
                applyChildrenState = findMethodBestMatch(clazz, "applyChildrenState", Classes.SystemUI.StackScrollState);
                setExpansionDisabled = findMethodBestMatch(clazz, "setExpansionDisabled", boolean.class);
                setSystemExpanded = findMethodBestMatch(clazz, "setSystemExpanded", boolean.class);
                setHeadsUp = findMethodBestMatch(clazz, "setHeadsUp", boolean.class);
            }
        }

        public static class NotificationChildrenContainer {
            private static final Class clazz = Classes.SystemUI.NotificationChildrenContainer;

            public static Method inflateDivider;
            public static Method getNotificationChildren;
            public static Method getIntrinsicHeight;
            public static Method applyChildOrder;

            private static void init() {
                inflateDivider = XposedHelpers.findMethodBestMatch(clazz, "inflateDivider");
                getNotificationChildren = XposedHelpers.findMethodBestMatch(clazz, "getNotificationChildren");
                getIntrinsicHeight = findMethodBestMatch(clazz, "getIntrinsicHeight");
                applyChildOrder = XposedHelpers.findMethodBestMatch(clazz, "applyChildOrder", List.class);
            }
        }

        public static class NotificationGroupManager {
            private static final Class clazz = Classes.SystemUI.NotificationGroupManager;

            public static Method setGroupExpandedSbn;
            public static Method setGroupExpandedGroup;
            public static Method onEntryAdded;
            public static Method onEntryRemovedInternal;
            public static Method areGroupsProhibited;
            public static Method isChildInGroupWithSummary;
            public static Method getGroupSummary;

            private static void init() {
                setGroupExpandedSbn = findMethodBestMatch(clazz, "setGroupExpanded", StatusBarNotification.class, boolean.class);
                setGroupExpandedGroup = findMethodBestMatch(clazz, "setGroupExpanded", Classes.SystemUI.NotificationGroup, boolean.class);
                onEntryAdded = findMethodBestMatch(clazz, "onEntryAdded", Classes.SystemUI.NotificationDataEntry);
                onEntryRemovedInternal = findMethodBestMatch(clazz, "onEntryRemovedInternal", Classes.SystemUI.NotificationDataEntry, StatusBarNotification.class);
                areGroupsProhibited = findMethodBestMatch(clazz, "areGroupsProhibited");
                isChildInGroupWithSummary = findMethodBestMatch(clazz, "isChildInGroupWithSummary", StatusBarNotification.class);
                getGroupSummary = findMethodBestMatch(clazz, "getGroupSummary", StatusBarNotification.class);
                NotificationGroup.constructor = findConstructorBestMatch(Classes.SystemUI.NotificationGroup);
            }

            public static final class NotificationGroup {
                public static Constructor constructor;
            }
        }

        public static final class HeadsUpManager {
            private static final Class clazz = Classes.SystemUI.HeadsUpManager;

            public static Method isHeadsUp;
            public static Method updateNotification;
            public static Method showNotification;
            public static Method releaseImmediately;
            public static Method releaseAllImmediately;
            public static Method setEntryPinned;
            public static Method shouldHeadsUpBecomePinned;
            public static Method getTopEntry;
            public static Method removeHeadsUpEntry;

            private static void init() {
                isHeadsUp = findMethodBestMatch(clazz, "isHeadsUp", String.class);
                updateNotification = findMethodBestMatch(clazz, "updateNotification", Classes.SystemUI.NotificationDataEntry, boolean.class);
                showNotification = findMethodBestMatch(clazz, "showNotification", Classes.SystemUI.NotificationDataEntry);
                releaseImmediately = findMethodBestMatch(clazz, "releaseImmediately", String.class);
                releaseAllImmediately = findMethodBestMatch(clazz, "releaseAllImmediately");
                setEntryPinned = findMethodBestMatch(clazz, "setEntryPinned", Classes.SystemUI.HeadsUpEntry, boolean.class);
                shouldHeadsUpBecomePinned = findMethodBestMatch(clazz, "shouldHeadsUpBecomePinned", Classes.SystemUI.NotificationDataEntry);
                getTopEntry = findMethodBestMatch(clazz, "getTopEntry");
                removeHeadsUpEntry = findMethodBestMatch(clazz, "removeHeadsUpEntry", Classes.SystemUI.NotificationDataEntry);

                OnHeadsUpChangedListener.init();
            }

            public static final class OnHeadsUpChangedListener {
                private static final Class clazz = Classes.SystemUI.HeadsUpManagerOnHeadsUpChangedListener;

                public static Method onHeadsUpStateChanged;

                private static void init() {
                    onHeadsUpStateChanged = findMethodBestMatch(clazz, "onHeadsUpStateChanged", Classes.SystemUI.NotificationDataEntry, boolean.class);
                }
            }
        }

        public static final class HeadsUpEntry {
            private static final Class clazz = Classes.SystemUI.HeadsUpEntry;

            public static Method removeAutoRemovalCallbacks;
            public static Method compareTo;
            public static Method updateEntry;

            private static void init() {
                removeAutoRemovalCallbacks = findMethodBestMatch(clazz, "removeAutoRemovalCallbacks");
                compareTo = findMethodBestMatch(clazz, "compareTo", clazz);
                updateEntry = findMethodBestMatch(clazz, "updateEntry");
            }
        }

        public static final class NotificationData {
            private static final Class clazz = Classes.SystemUI.NotificationData;

            public static Method updateRanking;
            public static Method get;
            public static Method getActiveNotifications;

            private static void init() {
                updateRanking = findMethodBestMatch(clazz, "updateRanking", NotificationListenerService.RankingMap.class);
                get = findMethodBestMatch(clazz, "get", String.class);
                getActiveNotifications = findMethodBestMatch(clazz, "getActiveNotifications");
            }
        }
    }

}
