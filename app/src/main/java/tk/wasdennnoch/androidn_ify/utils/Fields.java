package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Field;

import static de.robv.android.xposed.XposedHelpers.*;

public final class Fields {

    private static final String TAG = Fields.class.getSimpleName();

    public static class SystemUI {

        public static void init() {
            ActivatableNotificationView.init();
            ExpandableView.init();
            QSContainer.init();
            NotificationPanelView.init();
        }

        public static final class ExpandableView {
            private static final Class clazz = Classes.SystemUI.ExpandableView;

            public static Field mClipTopAmount;
            public static Field mActualHeight;

            private static void init() {
                mClipTopAmount = findField(clazz, "mClipTopAmount");
                mActualHeight = findField(clazz, "mActualHeight");
            }
        }

        public static final class ActivatableNotificationView {
            private static final Class clazz = Classes.SystemUI.ActivatableNotificationView;

            public static Field mDrawingAppearAnimation;
            public static Field mDark;
            public static Field mDimmed;
            public static Field mActivated;
            public static Field mAppearAnimationFraction;
            public static Field mAnimationTranslationY;
            public static Field mAppearAnimationTranslation;
            public static Field mAppearAnimationRect;
            public static Field mBackgroundAnimator;
            public static Field mSlowOutFastInInterpolator;
            public static Field mCurrentAlphaInterpolator;
            public static Field mCurrentAppearInterpolator;
            public static Field mAppearAnimator;
            public static Field mBackgroundNormal;
            public static Field mBackgroundDimmed;

            private static void init() {
                mDrawingAppearAnimation = findField(clazz, "mDrawingAppearAnimation");
                mDark = findField(clazz, "mDark");
                mDimmed = findField(clazz, "mDimmed");
                mActivated = findField(clazz, "mActivated");
                mAppearAnimationFraction = findField(clazz, "mAppearAnimationFraction");
                mAnimationTranslationY = findField(clazz, "mAnimationTranslationY");
                mAppearAnimationTranslation = findField(clazz, "mAppearAnimationTranslation");
                mAppearAnimationRect = findField(clazz, "mAppearAnimationRect");
                mBackgroundAnimator = findField(clazz, "mBackgroundAnimator");
                mSlowOutFastInInterpolator = findField(clazz, "mSlowOutFastInInterpolator");
                mCurrentAlphaInterpolator = findField(clazz, "mCurrentAlphaInterpolator");
                mCurrentAppearInterpolator = findField(clazz, "mCurrentAppearInterpolator");
                mAppearAnimator = findField(clazz, "mAppearAnimator");
                mBackgroundNormal = findField(clazz, "mBackgroundNormal");
                mBackgroundDimmed = findField(clazz, "mBackgroundDimmed");
            }
        }

        public static final class QSContainer {
            private static final Class clazz = Classes.SystemUI.QSContainer;

            public static Field mHeightOverride;

            private static void init() {
                mHeightOverride = findField(clazz, "mHeightOverride");
            }
        }

        public static final class NotificationPanelView {
            private static final Class clazz = Classes.SystemUI.NotificationPanelView;

            public static Field mQsExpansionHeight;
            public static Field mQsMinExpansionHeight;
            public static Field mQsMaxExpansionHeight;
            public static Field mUpdateFlingOnLayout;
            public static Field mHasLayoutedSinceDown;
            public static Field mKeyguardShowing;
            public static Field mQsExpanded;
            public static Field mQsFullyExpanded;
            public static Field mUpdateFlingVelocity;
            public static Field mStatusBar;
            public static Field mNotificationStackScroller;
            public static Field mQsPanel;
            public static Field mKeyguardStatusView;
            public static Field mClockView;
            public static Field mQsSizeChangeAnimator;
            public static Field mClockPositionAlgorithm;

            private static void init() {
                mQsExpansionHeight = findField(clazz, "mQsExpansionHeight");
                mQsMinExpansionHeight = findField(clazz, "mQsMinExpansionHeight");
                mQsMaxExpansionHeight = findField(clazz, "mQsMaxExpansionHeight");
                mUpdateFlingOnLayout = findField(clazz, "mUpdateFlingOnLayout");
                mHasLayoutedSinceDown = findField(clazz, "mHasLayoutedSinceDown");
                mKeyguardShowing = findField(clazz, "mKeyguardShowing");
                mQsExpanded = findField(clazz, "mQsExpanded");
                mQsFullyExpanded = findField(clazz, "mQsFullyExpanded");
                mUpdateFlingVelocity = findField(clazz, "mUpdateFlingVelocity");
                mStatusBar = findField(clazz, "mStatusBar");
                mNotificationStackScroller = findField(clazz, "mNotificationStackScroller");
                mQsPanel = findField(clazz, "mQsPanel");
                mKeyguardStatusView = findField(clazz, "mKeyguardStatusView");
                mClockView = findField(clazz, "mClockView");
                mQsSizeChangeAnimator = findField(clazz, "mQsSizeChangeAnimator");
                mClockPositionAlgorithm = findField(clazz, "mClockPositionAlgorithm");
            }
        }
    }
}
