package tk.wasdennnoch.androidn_ify.utils;

import android.service.notification.StatusBarNotification;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

public final class Methods {

    private static final String TAG = Methods.class.getSimpleName();

    public static final class Android {

        public static void init() {
            View.init();
        }

        public static final class View {
            private static final Class clazz = android.view.View.class;

            public static Method isLayoutRtl;
            public static Method requestAccessibilityFocus;

            private static void init() {
                isLayoutRtl = findMethodBestMatch(clazz, "isLayoutRtl");
                requestAccessibilityFocus = findMethodBestMatch(clazz, "requestAccessibilityFocus");
            }
        }
    }
    
    public static final class SystemUI {

        public static void init() {
            BaseStatusBar.init();
            QSContainer.init();
            NotificationPanelView.init();
            NotificationStackScrollLayout.init();
            StackScrollAlgorithm.init();
            StackStateAnimator.init();
            ExpandableView.init();
            NotificationContentView.init();
            ExpandableNotificationRow.init();
        }

        public static final class BaseStatusBar {
            private static final Class clazz = Classes.SystemUI.BaseStatusBar;

            public static Method shouldShowOnKeyguard;

            private static void init() {
                try {
                    shouldShowOnKeyguard = findMethodBestMatch(clazz, "shouldShowOnKeyguard", StatusBarNotification.class);
                } catch (NoSuchMethodError ignore) { //Xperia
                    shouldShowOnKeyguard = findMethodBestMatch(clazz, "shouldShowOnKeyguard", StatusBarNotification.class, boolean.class);
                }
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
            }
        }

        public static final class StackScrollAlgorithm {
            private static final Class clazz = Classes.SystemUI.StackScrollAlgorithm;

            public static Method handleDraggedViews;
            public static Method updateDimmedActivatedHideSensitive;
            public static Method updateSpeedBumpState;
            public static Method getNotificationChildrenStates;
            public static Method updateVisibleChildren;
            public static Method updateStateForChildTransitioningInBottom;
            public static Method updateStateForChildFullyInBottomStack;

            private static void init() {
                handleDraggedViews = findMethodBestMatch(clazz, "handleDraggedViews",
                        Classes.SystemUI.AmbientState, Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateDimmedActivatedHideSensitive = findMethodBestMatch(clazz, "updateDimmedActivatedHideSensitive",
                        Classes.SystemUI.AmbientState, Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateSpeedBumpState = findMethodBestMatch(clazz, "updateSpeedBumpState",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState, int.class);
                getNotificationChildrenStates = findMethodBestMatch(clazz, "getNotificationChildrenStates",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateVisibleChildren = findMethodBestMatch(clazz, "updateVisibleChildren",
                        Classes.SystemUI.StackScrollState, Classes.SystemUI.StackScrollAlgorithmState);
                updateStateForChildTransitioningInBottom = findMethodBestMatch(clazz, "updateStateForChildTransitioningInBottom",
                        Classes.SystemUI.StackScrollAlgorithmState, float.class, float.class, float.class, Classes.SystemUI.StackViewState, int.class);
                updateStateForChildFullyInBottomStack = findMethodBestMatch(clazz, "updateStateForChildFullyInBottomStack",
                        Classes.SystemUI.StackScrollAlgorithmState, float.class, Classes.SystemUI.StackViewState, int.class, Classes.SystemUI.AmbientState);
            }
        }

        public static final class StackStateAnimator {
            private static final Class clazz = Classes.SystemUI.StackStateAnimator;

            public static Method getFinalActualHeight;

            private static void init() {
                getFinalActualHeight = findMethodBestMatch(clazz, "getFinalActualHeight", Classes.SystemUI.ExpandableView);
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
            public static Method generateChildHierarchyEvents;
            public static Method setStackHeight;

            private static void init() {
                setIsExpanded = findMethodBestMatch(clazz, "setIsExpanded", boolean.class);
                updateAlgorithmHeightAndPadding = findMethodBestMatch(clazz, "updateAlgorithmHeightAndPadding");
                requestChildrenUpdate = findMethodBestMatch(clazz, "requestChildrenUpdate");
                setStackTranslation = findMethodBestMatch(clazz, "setStackTranslation", float.class);
                setScrollingEnabled = findMethodBestMatch(clazz, "setScrollingEnabled", boolean.class);
                isAddOrRemoveAnimationPending = findMethodBestMatch(clazz, "isAddOrRemoveAnimationPending");
                generateChildHierarchyEvents = findMethodBestMatch(clazz, "generateChildHierarchyEvents");
                setStackHeight = findMethodBestMatch(clazz, "setStackHeight", float.class);
            }
        }

        public static final class ExpandableView {
            private static final Class clazz = Classes.SystemUI.ExpandableView;

            public static Method isTransparent;
            public static Method getHeight;
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

            private static void init() {
                isTransparent = findMethodBestMatch(clazz, "isTransparent");
                getHeight = findMethodBestMatch(clazz, "getHeight");
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

            private static void init() {
                getExpandedChild = findMethodBestMatch(clazz, "getExpandedChild");
                getContractedChild = findMethodBestMatch(clazz, "getContractedChild");
                getHeadsUpChild = findMethodBestMatch(clazz, "getHeadsUpChild");
                updateClipping = findMethodBestMatch(clazz, "updateClipping");
                runSwitchAnimation = findMethodBestMatch(clazz, "runSwitchAnimation", int.class);
                calculateVisibleType = findMethodBestMatch(clazz, "calculateVisibleType");
                updateViewVisibilities = findMethodBestMatch(clazz, "updateViewVisibilities", int.class);
                selectLayout = findMethodBestMatch(clazz, "selectLayout", boolean.class, boolean.class);
            }
        }


        public static class ExpandableNotificationRow {
            private static final Class clazz = Classes.SystemUI.ExpandableNotificationRow;

            public static Method isUserLocked;
            public static Method isExpanded;
            public static Method isExpandable;
            public static Method getIntrinsicHeight;
            public static Method getMaxContentHeight;
            public static Method getMaxExpandHeight;
            public static Method getActualHeight;
            public static Method getStatusBarNotification;
            public static Method getShowingLayout;
            public static Method notifyHeightChanged;
            public static Method setUserExpanded;

            private static void init() {
                isUserLocked = findMethodBestMatch(clazz, "isUserLocked");
                isExpanded = findMethodBestMatch(clazz, "isExpanded");
                isExpandable = findMethodBestMatch(clazz, "isExpandable");
                getActualHeight = findMethodBestMatch(clazz, "getActualHeight");
                getIntrinsicHeight = findMethodBestMatch(clazz, "getIntrinsicHeight");
                getMaxContentHeight = findMethodBestMatch(clazz, "getMaxContentHeight");
                getMaxExpandHeight = findMethodBestMatch(clazz, "getMaxExpandHeight");
                getStatusBarNotification = findMethodBestMatch(clazz, "getStatusBarNotification");
                getShowingLayout = findMethodBestMatch(clazz, "getShowingLayout");
                notifyHeightChanged = findMethodBestMatch(clazz, "notifyHeightChanged", boolean.class);
                setUserExpanded = findMethodBestMatch(clazz, "setUserExpanded", boolean.class);
            }
        }
    }

}
