package tk.wasdennnoch.androidn_ify.utils;

import android.view.View;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.*;

public final class Fields {

    private static final String TAG = Fields.class.getSimpleName();

    public static class Android {

        public static void init() {
            Icon.init();
            View.init();
        }

        public static final class Icon {
            private static final Class clazz = android.graphics.drawable.Icon.class;

            public static Field mType;

            private static void init() {
                mType = findField(clazz, "mType");
            }
        }

        public static final class View {
            private static final Class clazz = android.view.View.class;

            public static Field mPrivateFlags3;

            private static void init() {
                mPrivateFlags3 = findField(clazz, "mPrivateFlags3");
            }
        }
    }

    public static class SystemUI {

        public static void init() {
            ActivatableNotificationView.init();
            ExpandableView.init();
            ExpandableNotificationRow.init();
            NotificationContentView.init();
            NotificationChildrenContainer.init();
            NotificationGroupManager.init();
            NotificationGroupManager.NotificationGroup.init();
            HeadsUpManager.init();
            HeadsUpEntry.init();
            NotificationDataEntry.init();
            BaseStatusBar.init();
            PhoneStatusBar.init();
            NotificationQuickSettingsContainer.init();
            QSContainer.init();
            NotificationPanelView.init();
            NotificationStackScrollLayout.init();
            SwipeHelper.init();
            ExpandHelper.init();
            StackScrollAlgorithm.init();
            StackViewState.init();
            StackStateAnimator.init();
            AnimationFilter.init();
            ViewState.init();
            ScrimView.init();
            ScrimController.init();
        }

        public static final class ExpandableView {
            private static final Class clazz = Classes.SystemUI.ExpandableView;

            public static Field mClipRect;
            public static Field mClipTopAmount;
            public static Field mActualHeight;
            public static Field mClipTopOptimization;
            public static Field mActualHeightInitialized;

            private static void init() {
                mClipRect = findField(clazz, "mClipRect");
                mClipTopAmount = findField(clazz, "mClipTopAmount");
                mActualHeight = findField(clazz, "mActualHeight");
                mClipTopOptimization = findField(clazz, "mClipTopOptimization");
                mActualHeightInitialized = findField(clazz, "mActualHeightInitialized");
            }
        }

        public static final class ExpandableNotificationRow {
            private static final Class clazz = Classes.SystemUI.ExpandableNotificationRow;

            public static Field mShowingPublic;
            public static Field mShowingPublicInitialized;
            public static Field mSensitive;
            public static Field mHideSensitiveForIntrinsicHeight;
            public static Field mIsHeadsUp;
            public static Field mIsPinned;
            public static Field mIsSystemExpanded;
            public static Field mIsSystemChildExpanded;
            public static Field mUserExpanded;
            public static Field mHeadsUpHeight;
            public static Field mMaxExpandHeight;
            public static Field mChildrenContainer;
            public static Field mChildrenExpanded;
            public static Field mStatusBarNotification;
            public static Field mPrivateLayout;
            public static Field mPublicLayout;
            public static Field mHasUserChangedExpansion;
            public static Field mExpansionDisabled;
            public static Field mUserLocked;

            private static void init() {
                mShowingPublic = findField(clazz, "mShowingPublic");
                mShowingPublicInitialized = findField(clazz, "mShowingPublicInitialized");
                mSensitive = findField(clazz, "mSensitive");
                mHideSensitiveForIntrinsicHeight = findField(clazz, "mHideSensitiveForIntrinsicHeight");
                mIsHeadsUp = findField(clazz, "mIsHeadsUp");
                mIsPinned = findField(clazz, "mIsPinned");
                mIsSystemExpanded = findField(clazz, "mIsSystemExpanded");
                mIsSystemChildExpanded = findField(clazz, "mIsSystemChildExpanded");
                mUserExpanded = findField(clazz, "mUserExpanded");
                mHeadsUpHeight = findField(clazz, "mHeadsUpHeight");
                mMaxExpandHeight = findField(clazz, "mMaxExpandHeight");
                mChildrenContainer = findField(clazz, "mChildrenContainer");
                mChildrenExpanded = findField(clazz, "mChildrenExpanded");
                mStatusBarNotification = findField(clazz, "mStatusBarNotification");
                mPrivateLayout = findField(clazz, "mPrivateLayout");
                mPublicLayout = findField(clazz, "mPublicLayout");
                mHasUserChangedExpansion = findField(clazz, "mHasUserChangedExpansion");
                mExpansionDisabled = findField(clazz, "mExpansionDisabled");
                mUserLocked = findField(clazz, "mUserLocked");
            }
        }

        public static final class NotificationContentView {
            private static final Class clazz = Classes.SystemUI.NotificationContentView;

            public static Field mVisibleType;
            public static Field mContentHeight;
            public static Field mShowingLegacyBackground;
            public static Field mIsHeadsUp;
            public static Field mDark;
            public static Field mAnimate;
            public static Field mClipTopAmount;
            public static Field mContractedChild;
            public static Field mExpandedChild;
            public static Field mHeadsUpChild;
            public static Field mClipBounds;

            private static void init() {
                mVisibleType = findField(clazz, "mVisibleType");
                mContentHeight = findField(clazz, "mContentHeight");
                mShowingLegacyBackground = findField(clazz, "mShowingLegacyBackground");
                mIsHeadsUp = findField(clazz, "mIsHeadsUp");
                mClipTopAmount = findField(clazz, "mClipTopAmount");
                mDark = findField(clazz, "mDark");
                mAnimate = findField(clazz, "mAnimate");
                mContractedChild = findField(clazz, "mContractedChild");
                mExpandedChild = findField(clazz, "mExpandedChild");
                mHeadsUpChild = findField(clazz, "mHeadsUpChild");
                mClipBounds = findField(clazz, "mClipBounds");
            }
        }

        public static final class NotificationChildrenContainer {
            private static final Class clazz = Classes.SystemUI.NotificationChildrenContainer;

            public static Field mChildren;
            public static Field mDividers;

            private static void init() {
                mChildren = XposedHelpers.findField(clazz, "mChildren");
                mDividers = XposedHelpers.findField(clazz, "mDividers");
            }
        }

        public static final class NotificationGroupManager {
            private static final Class clazz = Classes.SystemUI.NotificationGroupManager;

            public static Field mGroupMap;
            public static Field mListener;
            public static Field mBarState;

            private static void init() {
                mGroupMap = findField(clazz, "mGroupMap");
                mListener = findField(clazz, "mListener");
                mBarState = findField(clazz, "mBarState");
                NotificationGroup.init();
            }

            public static final class NotificationGroup {
                private static final Class clazz = Classes.SystemUI.NotificationGroup;

                public static Field children;
                public static Field summary;
                public static Field expanded;

                private static void init() {
                    children = findField(clazz, "children");
                    summary = findField(clazz, "summary");
                    expanded = findField(clazz, "expanded");
                }
            }
        }

        public static final class ActivatableNotificationView {
            private static final Class clazz = Classes.SystemUI.ActivatableNotificationView;

            public static Field mDrawingAppearAnimation;
            public static Field mDark;
            public static Field mDimmed;
            public static Field mActivated;
            public static Field mShowingLegacyBackground;
            public static Field mIsBelowSpeedBump;
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
            public static Field mNormalColor;
            public static Field mLegacyColor;
            public static Field mLowPriorityColor;
            public static Field mTapTimeoutRunnable;
            public static Field mOnActivatedListener;
            public static Field mBgTint;

            private static void init() {
                mDrawingAppearAnimation = findField(clazz, "mDrawingAppearAnimation");
                mDark = findField(clazz, "mDark");
                mDimmed = findField(clazz, "mDimmed");
                mActivated = findField(clazz, "mActivated");
                mShowingLegacyBackground = findField(clazz, "mShowingLegacyBackground");
                mIsBelowSpeedBump = findField(clazz, "mIsBelowSpeedBump");
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
                mNormalColor = findField(clazz, "mNormalColor");
                mLegacyColor = findField(clazz, "mLegacyColor");
                mLowPriorityColor = findField(clazz, "mLowPriorityColor");
                mTapTimeoutRunnable = findField(clazz, "mTapTimeoutRunnable");
                mOnActivatedListener = findField(clazz, "mOnActivatedListener");
                mBgTint = findField(clazz, "mBgTint");
            }
        }

        public static final class HeadsUpManager {
            private static final Class clazz = Classes.SystemUI.HeadsUpManager;

            public static Field mHeadsUpEntries;
            public static Field mEntriesToRemoveAfterExpand;
            public static Field mBar;
            public static Field mStatusBarWindowView;
            public static Field mTrackingHeadsUp;
            public static Field mIsExpanded;
            public static Field mHeadsUpGoingAway;
            public static Field mWaitingOnCollapseWhenGoingAway;
            public static Field mHasPinnedNotification;
            public static Field mTmpTwoArray;
            public static Field mStatusBarHeight;
            public static Field mHandler;
            public static Field mMinimumDisplayTime;
            public static Field mHeadsUpNotificationDecay;
            public static Field mClock;
            public static Field mListeners;
            public static Field mEntryPool;
            public static Field mReleaseOnExpandFinish;

            private static void init() {
                mHeadsUpEntries = findField(clazz, "mHeadsUpEntries");
                mEntriesToRemoveAfterExpand = findField(clazz, "mEntriesToRemoveAfterExpand");
                mBar = findField(clazz, "mBar");
                mStatusBarWindowView = findField(clazz, "mStatusBarWindowView");
                mTrackingHeadsUp = findField(clazz, "mTrackingHeadsUp");
                mIsExpanded = findField(clazz, "mIsExpanded");
                mHeadsUpGoingAway = findField(clazz, "mHeadsUpGoingAway");
                mWaitingOnCollapseWhenGoingAway = findField(clazz, "mWaitingOnCollapseWhenGoingAway");
                mHasPinnedNotification = findField(clazz, "mHasPinnedNotification");
                mTmpTwoArray = findField(clazz, "mTmpTwoArray");
                mStatusBarHeight = findField(clazz, "mStatusBarHeight");
                mHandler = findField(clazz, "mHandler");
                mMinimumDisplayTime = findField(clazz, "mMinimumDisplayTime");
                mHeadsUpNotificationDecay = findField(clazz, "mHeadsUpNotificationDecay");
                mClock = findField(clazz, "mClock");
                mListeners = findField(clazz, "mListeners");
                mEntryPool = findField(clazz, "mEntryPool");
                mReleaseOnExpandFinish = findField(clazz, "mReleaseOnExpandFinish");
            }
        }

        public static final class HeadsUpEntry {
            private static final Class clazz = Classes.SystemUI.HeadsUpEntry;

            public static Field entry;
            public static Field postTime;
            public static Field earliestRemovaltime;
            public static Field mRemoveHeadsUpRunnable;

            private static void init() {
                entry = findField(clazz, "entry");
                postTime = findField(clazz, "postTime");
                earliestRemovaltime = findField(clazz, "earliestRemovaltime");
                mRemoveHeadsUpRunnable = findField(clazz, "mRemoveHeadsUpRunnable");
            }
        }

        public static final class BaseStatusBar {
            private static final Class clazz = Classes.SystemUI.BaseStatusBar;

            public static Field mShowLockscreenNotifications;
            public static Field mState;
            public static Field mNotificationData;
            public static Field mGroupManager;
            public static Field mHeadsUpManager;
            public static Field mStackScroller;
            public static Field mDismissView;
            public static Field mEmptyShadeView;
            public static Field mHandler;
            public static Field mContext;

            private static void init() {
                mShowLockscreenNotifications = findField(clazz, "mShowLockscreenNotifications");
                mState = findField(clazz, "mState");
                mNotificationData = findField(clazz, "mNotificationData");
                mGroupManager = findField(clazz, "mGroupManager");
                mHeadsUpManager = findField(clazz, "mHeadsUpManager");
                mStackScroller = findField(clazz, "mStackScroller");
                mDismissView = findField(clazz, "mDismissView");
                mEmptyShadeView = findField(clazz, "mEmptyShadeView");
                mHandler = findField(clazz, "mHandler");
                mContext = findField(clazz, "mContext");
            }
        }

        public static final class PhoneStatusBar {
            private static final Class clazz = Classes.SystemUI.PhoneStatusBar;

            public static Field mNotificationPanel;
            public static Field mTmpChildOrderMap;
            public static Field mShadeUpdates;
            public static Field mIconController;
            public static Field mKeyguardIconOverflowContainer;
            public static Field mHeadsUpEntriesToRemoveOnSwitch;
            public static Field mLatestRankingMap;

            private static void init() {
                mNotificationPanel = findField(clazz, "mNotificationPanel");
                mTmpChildOrderMap = findField(clazz, "mTmpChildOrderMap");
                mShadeUpdates = findField(clazz, "mShadeUpdates");
                mIconController = findField(clazz, "mIconController");
                mKeyguardIconOverflowContainer = findField(clazz, "mKeyguardIconOverflowContainer");
                mHeadsUpEntriesToRemoveOnSwitch = findField(clazz, "mHeadsUpEntriesToRemoveOnSwitch");
                mLatestRankingMap = findField(clazz, "mLatestRankingMap");
            }
        }

        public static final class NotificationDataEntry {
            private static final Class clazz = Classes.SystemUI.NotificationDataEntry;

            public static Field key;
            public static Field notification;
            public static Field icon;
            public static Field row;
            public static Field interruption;
            public static Field autoRedacted;
            public static Field legacy;
            public static Field targetSdk;
            public static Field lastFullScreenIntentLaunchTime;

            private static void init() {
                key = findField(clazz, "key");
                notification = findField(clazz, "notification");
                icon = findField(clazz, "icon");
                row = findField(clazz, "row");
                interruption = findField(clazz, "interruption");
                autoRedacted = findField(clazz, "autoRedacted");
                legacy = findField(clazz, "legacy");
                targetSdk = findField(clazz, "targetSdk");
                lastFullScreenIntentLaunchTime = findField(clazz, "lastFullScreenIntentLaunchTime");
            }
        }

        public static final class NotificationQuickSettingsContainer {
            private static final Class clazz = Classes.SystemUI.NotificationsQuickSettingsContainer;

            public static Field mQsExpanded;

            private static void init() {
                mQsExpanded = findField(clazz, "mQsExpanded");
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
            public static Field mQsPeekHeight;
            public static Field mShadeEmpty;
            public static Field mClockAnimator;
            public static Field mStatusBarMinHeight;
            public static Field mQsExpansionEnabled;
            public static Field mKeyguardStatusBar;

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
                mQsPeekHeight = findField(clazz, "mQsPeekHeight");
                mShadeEmpty = findField(clazz, "mShadeEmpty");
                mClockAnimator = findField(clazz, "mClockAnimator");
                mStatusBarMinHeight = findField(clazz, "mStatusBarMinHeight");
                mQsExpansionEnabled = findField(clazz, "mQsExpansionEnabled");
                mKeyguardStatusBar = findField(clazz, "mKeyguardStatusBar");
            }
        }

        public static final class NotificationStackScrollLayout {
            private static final Class clazz = Classes.SystemUI.NotificationStackScrollLayout;

            public static Field mPaddingBetweenElements;
            public static Field mBottomStackPeekSize;
            public static Field mBottomStackSlowDownHeight;
            public static Field mDontReportNextOverScroll;
            public static Field mChildrenToAddAnimated;
            public static Field mDisallowScrollingInThisMotion;
            public static Field mOwnScrollY;
            public static Field mScrollX;
            public static Field mStackTranslation;
            public static Field mMaxLayoutHeight;
            public static Field mCurrentStackHeight;
            public static Field mCurrentStackScrollState;
            public static Field mExpandedGroupView;
            public static Field mNeedsAnimation;
            public static Field mGroupManager;
            public static Field mAmbientState;
            public static Field mStackScrollAlgorithm;
            public static Field mContentHeight;
            public static Field mEmptyShadeView;
            public static Field mIsExpansionChanging;
            public static Field mLastSetStackHeight;
            public static Field mIsExpanded;
            public static Field mAnimationEvents;
            public static Field mChangePositionInProgress;
            public static Field mChildrenChangingPositions;
            public static Field mDimmedNeedsAnimation;
            public static Field mDarkNeedsAnimation;
            public static Field mDarkAnimationOriginIndex;

            public static Field viewScalerView;

            private static void init() {
                mPaddingBetweenElements = findField(clazz, "mPaddingBetweenElements");
                mBottomStackPeekSize = findField(clazz, "mBottomStackPeekSize");
                mBottomStackSlowDownHeight = findField(clazz, "mBottomStackSlowDownHeight");
                mDontReportNextOverScroll = findField(clazz, "mDontReportNextOverScroll");
                mChildrenToAddAnimated = findField(clazz, "mChildrenToAddAnimated");
                mDisallowScrollingInThisMotion = findField(clazz, "mDisallowScrollingInThisMotion");
                mOwnScrollY = findField(clazz, "mOwnScrollY");
                mScrollX = findField(clazz, "mScrollX");
                mStackTranslation = findField(clazz, "mStackTranslation");
                mMaxLayoutHeight = findField(clazz, "mMaxLayoutHeight");
                mCurrentStackHeight = findField(clazz, "mCurrentStackHeight");
                mCurrentStackScrollState = findField(clazz, "mCurrentStackScrollState");
                mExpandedGroupView = findField(clazz, "mExpandedGroupView");
                mNeedsAnimation = findField(clazz, "mNeedsAnimation");
                mGroupManager = findField(clazz, "mGroupManager");
                mAmbientState = findField(clazz, "mAmbientState");
                mStackScrollAlgorithm = findField(clazz, "mStackScrollAlgorithm");
                mContentHeight = findField(clazz, "mContentHeight");
                mEmptyShadeView = findField(clazz, "mEmptyShadeView");
                mIsExpansionChanging = findField(clazz, "mIsExpansionChanging");
                mCurrentStackHeight = findField(clazz, "mCurrentStackHeight");
                mLastSetStackHeight = findField(clazz, "mLastSetStackHeight");
                mIsExpanded = findField(clazz, "mIsExpanded");
                mAnimationEvents = findField(clazz, "mAnimationEvents");
                mChangePositionInProgress = findField(clazz, "mChangePositionInProgress");
                mChildrenChangingPositions = findField(clazz, "mChildrenChangingPositions");
                mDimmedNeedsAnimation = findField(clazz, "mDimmedNeedsAnimation");
                mDarkNeedsAnimation = findField(clazz, "mDarkNeedsAnimation");
                mDarkAnimationOriginIndex = findField(clazz, "mDarkAnimationOriginIndex");

                viewScalerView = findField(Classes.SystemUI.ViewScaler, "mView");
            }
        }

        public static final class SwipeHelper {
            private static final Class clazz = Classes.SystemUI.SwipeHelper;

            public static Field mMinSwipeProgress;
            public static Field mMaxSwipeProgress;
            public static Field mCallback;

            private static void init() {
                mMinSwipeProgress = findField(clazz, "mMinSwipeProgress");
                mMaxSwipeProgress = findField(clazz, "mMaxSwipeProgress");
                mCallback = findField(clazz, "mCallback");
            }
        }

        public static final class ExpandHelper {
            private static final Class clazz = Classes.SystemUI.ExpandHelper;

            public static Field mExpanding;
            public static Field mNaturalHeight;
            public static Field mOldHeight;
            public static Field mSmallSize;
            public static Field mScaler;
            public static Field mScaleAnimation;
            public static Field mCallback;
            public static Field mFlingAnimationUtils;
            public static Field mResizedView;
            public static Field mExpansionStyle;

            private static void init() {
                mExpanding = findField(clazz, "mExpanding");
                mNaturalHeight = findField(clazz, "mNaturalHeight");
                mOldHeight = findField(clazz, "mOldHeight");
                mSmallSize = findField(clazz, "mSmallSize");
                mScaler = findField(clazz, "mScaler");
                mScaleAnimation = findField(clazz, "mScaleAnimation");
                mCallback = findField(clazz, "mCallback");
                mFlingAnimationUtils = findField(clazz, "mFlingAnimationUtils");
                mResizedView = findField(clazz, "mResizedView");
                mExpansionStyle = findField(clazz, "mExpansionStyle");
            }
        }

        public static final class ViewState {
            private static final Class clazz = Classes.SystemUI.ViewState;

            public static Field alpha;
            public static Field yTranslation;
            public static Field zTranslation;
            public static Field gone;
            public static Field scale;

            private static void init() {
                alpha = XposedHelpers.findField(clazz, "alpha");
                yTranslation = XposedHelpers.findField(clazz, "yTranslation");
                zTranslation = XposedHelpers.findField(clazz, "zTranslation");
                gone = XposedHelpers.findField(clazz, "gone");
                scale = XposedHelpers.findField(clazz, "scale");
            }
        }

        public static final class StackScrollAlgorithm {
            private static final Class clazz = Classes.SystemUI.StackScrollAlgorithm;

            public static Field mPaddingBetweenElements;
            public static Field mIsSmallScreen;

            private static void init() {
                mPaddingBetweenElements = findField(clazz, "mPaddingBetweenElements");
                mIsSmallScreen = findField(clazz, "mIsSmallScreen");
            }
        }

        public static final class StackViewState {
            private static final Class clazz = Classes.SystemUI.StackViewState;
            public static Field height;
            public static Field dimmed;
            public static Field dark;
            public static Field hideSensitive;
            public static Field belowSpeedBump;
            public static Field clipTopAmount;
            public static Field topOverLap;
            public static Field notGoneIndex;
            public static Field location;

            private static void init() {
                height = XposedHelpers.findField(clazz, "height");
                dimmed = XposedHelpers.findField(clazz, "dimmed");
                dark = XposedHelpers.findField(clazz, "dark");
                hideSensitive = XposedHelpers.findField(clazz, "hideSensitive");
                belowSpeedBump = XposedHelpers.findField(clazz, "belowSpeedBump");
                clipTopAmount = XposedHelpers.findField(clazz, "clipTopAmount");
                topOverLap = XposedHelpers.findField(clazz, "topOverLap");
                notGoneIndex = XposedHelpers.findField(clazz, "notGoneIndex");
                location = XposedHelpers.findField(clazz, "location");
            }
        }

        public static final class StackStateAnimator {
            private static Class clazz = Classes.SystemUI.StackStateAnimator;

            public static Field mAnimationFilter;

            private static void init() {
                mAnimationFilter = findField(clazz, "mAnimationFilter");
            }
        }

        public static final class AnimationFilter {
            private static final Class clazz = Classes.SystemUI.AnimationFilter;

            public static Field animateHeight;

            public static void init() {
                animateHeight = findField(clazz, "animateHeight");
            }
        }

        public static final class ScrimView {
            private static final Class clazz = Classes.SystemUI.ScrimView;

            public static Field mScrimColor;
            public static Field mIsEmpty;
            public static Field mDrawAsSrc;
            public static Field mViewAlpha;

            private static void init() {
                mScrimColor = findField(clazz, "mScrimColor");
                mIsEmpty = findField(clazz, "mIsEmpty");
                mDrawAsSrc = findField(clazz, "mDrawAsSrc");
                mViewAlpha = findField(clazz, "mViewAlpha");
            }
        }
        public static final class ScrimController {
            private static final Class clazz = Classes.SystemUI.ScrimController;

            public static Field mBackDropView;
            public static Field mScrimBehind;
            public static Field mScrimSrcEnabled;

            private static void init() {
                mBackDropView = findField(clazz, "mBackDropView");
                mScrimBehind = findField(clazz, "mScrimBehind");
                mScrimSrcEnabled = findField(clazz, "mScrimSrcEnabled");
            }
        }
    }

}
