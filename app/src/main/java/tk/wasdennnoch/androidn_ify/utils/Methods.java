package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Method;

import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

public final class Methods {

    private static final String TAG = Methods.class.getSimpleName();
    
    public static final class SystemUI {

        public static void init() {
            StackScrollAlgorithm.init();
            ExpandableView.init();
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

        public static final class NotificationStackScrollLayout {

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
            }
        }
        
    }

}
