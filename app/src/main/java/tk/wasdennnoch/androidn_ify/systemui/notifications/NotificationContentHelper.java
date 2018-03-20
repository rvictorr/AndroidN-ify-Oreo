package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.HybridGroupManager;
import tk.wasdennnoch.androidn_ify.extracted.systemui.HybridNotificationView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationCustomViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputController;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TransformableView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationGroupManagerHooks;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.NotificationColorUtil;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks.post;
import static  tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationsStuff.*;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationContentView.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationContentView.*;


public class NotificationContentHelper {
    private static final String TAG = "NotificationContentHelper";

    private ResourceUtils res;
    private ExpandableNotificationRowHelper mRowHelper;

    public Object mGroupManager;
    public RemoteInputController mRemoteInputController;
    public StatusBarNotification mStatusBarNotification;
    public View.OnClickListener mExpandClickListener;

    public boolean mIsChildInGroup;

    private FrameLayout mNotificationContentView;
    public View mContainingNotification;

    /** The visible type at the start of a touch driven transformation */
    public int mTransformationStartVisibleType;
    /** The visible type at the start of an animation driven transformation */

    public int mAnimationStartVisibleType = UNDEFINED;

    public int mMinContractedHeight;
    public int mNotificationContentMarginEnd;
    public int mSingleLineWidthIndention;

    public int mSmallHeight;
    public int mHeadsUpHeight;
    public int mNotificationMaxHeight;
    public int mNotificationAmbientHeight;

    public RemoteInputView mExpandedRemoteInput;
    public RemoteInputView mHeadsUpRemoteInput;

    public View mContractedChild;
    public View mExpandedChild;
    public View mHeadsUpChild;
    public View mAmbientChild;

    public boolean mForceSelectNextLayout = true;
    public boolean mUserExpanding;
    public boolean mBeforeN;
    public boolean mClipToActualHeight = true;
    public boolean mExpandable;
    public boolean mLegacy;

    public PendingIntent mPreviousExpandedRemoteInputIntent;
    public PendingIntent mPreviousHeadsUpRemoteInputIntent;
    public RemoteInputView mCachedExpandedRemoteInput;
    public RemoteInputView mCachedHeadsUpRemoteInput;

    public HybridNotificationView mSingleLineView;
    public HybridNotificationView mAmbientSingleLineChild;
    public NotificationViewWrapper mContractedWrapper;
    public NotificationViewWrapper mExpandedWrapper;
    public NotificationViewWrapper mHeadsUpWrapper;
    public NotificationViewWrapper mAmbientWrapper;
    public HybridGroupManager mHybridGroupManager;

    public int mContentHeightAtAnimationStart = UNDEFINED;
    public boolean mFocusOnVisibilityChange;
    public boolean mHeadsUpAnimatingAway;
    public boolean mIconsVisible;
    public int mClipBottomAmount;
    public boolean mIsLowPriority;
    public boolean mIsContentExpandable;
    public boolean mShowAmbient;
    public boolean mOnKeyguard;

    public int previousHeight;

    public Runnable mExpandedVisibleListener;

    public final ViewTreeObserver.OnPreDrawListener mEnableAnimationPredrawListener
            = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            // We need to post since we don't want the notification to animate on the very first
            // frame
            post(new Runnable() {
                @Override
                public void run() {
                    XposedHelpers.setBooleanField(getContentView(), "mAnimate", true);
                }
            });
            getContentView().getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };

    private NotificationContentHelper(Object obj) {
        XposedHelpers.setAdditionalInstanceField(obj, "mNotificationsHelper", this);
        mNotificationContentView = (FrameLayout) obj;
        res = ResourceUtils.getInstance(mNotificationContentView.getContext());
    }

    public static NotificationContentHelper getInstance(Object obj) {
        NotificationContentHelper helper = (NotificationContentHelper) XposedHelpers.getAdditionalInstanceField(obj, "mNotificationsHelper");
        return helper != null ? helper : new NotificationContentHelper(obj);
    }

    public FrameLayout getContentView() {
        return mNotificationContentView;
    }

    public void onNotificationUpdated(Object entry) {
        mStatusBarNotification = get(Fields.SystemUI.NotificationDataEntry.notification, entry);
        mBeforeN = getInt(Fields.SystemUI.NotificationDataEntry.targetSdk, entry) < Build.VERSION_CODES.N;
        updateSingleLineView();
        applyRemoteInput(entry);
        View row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
        if (mContractedChild != null) {
            mContractedWrapper.onContentUpdated(row);
        }
        if (mExpandedChild != null) {
            mExpandedWrapper.onContentUpdated(row);
        }
        if (mHeadsUpChild != null) {
            mHeadsUpWrapper.onContentUpdated(row);
        }
        updateShowingLegacyBackground();
        mForceSelectNextLayout = true;
        invoke(setDark, getContentView(), getBoolean(Fields.SystemUI.NotificationContentView.mDark, getContentView()), false /* animate */, 0 /* delay */);
        mPreviousExpandedRemoteInputIntent = null;
        mPreviousHeadsUpRemoteInputIntent = null;
    }

    /*public boolean isShowingAmbient() {
        return mShowAmbient;
    }

    private boolean isHeadsUpAllowed() {
        return !mOnKeyguard && !mShowAmbient;
    }

    public void setShowAmbient(boolean showAmbient) {
        if (showAmbient != mShowAmbient) {
            mShowAmbient = showAmbient;
            if (mChildrenContainer != null) {
                mChildrenContainer.notifyShowAmbientChanged();
            }
            notifyHeightChanged(false*//* needsAnimation*//*);
        }
    }*/

    public View getContractedChild() {
        return mContractedChild;
    }

    public View getExpandedChild() {
        return mExpandedChild;
    }

    public View getHeadsUpChild() {
        return mHeadsUpChild;
    }

    public void init() {
        mHybridGroupManager = new HybridGroupManager(getContentView().getContext(), getContentView());
        ResourceUtils res = ResourceUtils.getInstance(getContentView().getContext());

        mMinContractedHeight = res.getDimensionPixelSize(
                R.dimen.min_notification_layout_height);
        mNotificationContentMarginEnd = res.getDimensionPixelSize(
                R.dimen.notification_content_margin_end);
    }

    public void setHeights(int smallHeight, int headsUpMaxHeight, int maxHeight,
                           int ambientHeight) {
        mSmallHeight = smallHeight;
        mHeadsUpHeight = headsUpMaxHeight;
        mNotificationMaxHeight = maxHeight;
        mNotificationAmbientHeight = ambientHeight;
    }

    public boolean updateContractedHeaderWidth() {
        // We need to update the expanded and the collapsed header to have exactly the same width to
        // have the expand buttons laid out at the same location.
        NotificationHeaderView contractedHeader = mContractedWrapper.getNotificationHeader();
        if (contractedHeader != null) {
            if (mExpandedChild != null
                    && mExpandedWrapper.getNotificationHeader() != null) {
                NotificationHeaderView expandedHeader = mExpandedWrapper.getNotificationHeader();
                int expandedSize = expandedHeader.getMeasuredWidth()
                        - expandedHeader.getPaddingEnd();
                int collapsedSize = contractedHeader.getMeasuredWidth()
                        - expandedHeader.getPaddingEnd();
                if (expandedSize != collapsedSize) {
                    int paddingEnd = contractedHeader.getMeasuredWidth() - expandedSize;
                    boolean isRtl = invoke(Methods.Android.View.isLayoutRtl, contractedHeader);
                    contractedHeader.setPadding(
                            isRtl
                                    ? paddingEnd
                                    : contractedHeader.getPaddingLeft(),
                            contractedHeader.getPaddingTop(),
                            isRtl
                                    ? contractedHeader.getPaddingLeft()
                                    : paddingEnd,
                            contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(true);
                    return true;
                }
            } else {
                int paddingEnd = mNotificationContentMarginEnd;
                if (contractedHeader.getPaddingEnd() != paddingEnd) {
                    boolean isRtl = invoke(Methods.Android.View.isLayoutRtl, contractedHeader);
                    contractedHeader.setPadding(
                            isRtl
                                    ? paddingEnd
                                    : contractedHeader.getPaddingLeft(),
                            contractedHeader.getPaddingTop(),
                            isRtl
                                    ? contractedHeader.getPaddingLeft()
                                    : paddingEnd,
                            contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(false);
                    return true;
                }
            }
        }
        return false;
    }

    /*public void setAmbientChild(View child) {
        if (mAmbientChild != null) {
            mAmbientChild.animate().cancel();
            getContentView().removeView(mAmbientChild);
        }
        if (child == null) {
            return;
        }
        getContentView().addView(child);
        mAmbientChild = child;
        mAmbientWrapper = NotificationViewWrapper.wrap(getContentView().getContext(), child,
                mContainingNotification);
    }*/

    public void focusExpandButtonIfNecessary() {
        if (mFocusOnVisibilityChange) {
            NotificationHeaderView header = getVisibleNotificationHeader();
            if (header != null) {
                ImageView expandButton = header.getExpandButton();
                if (expandButton != null) {
                    invoke(Methods.Android.View.requestAccessibilityFocus, expandButton);
                }
            }
            mFocusOnVisibilityChange = false;
        }
    }

    public void updateShowingLegacyBackground() {
        boolean mShowingLegacyBackground = getBoolean(Fields.SystemUI.NotificationContentView.mShowingLegacyBackground, getContentView());
        if (mContractedChild != null) {
            mContractedWrapper.setShowingLegacyBackground(mShowingLegacyBackground);
        }
        if (mExpandedChild != null) {
            mExpandedWrapper.setShowingLegacyBackground(mShowingLegacyBackground);
        }
        if (mHeadsUpChild != null) {
            mHeadsUpWrapper.setShowingLegacyBackground(mShowingLegacyBackground);
        }
    }

    public int getMinContentHeightHint() {
        if (mIsChildInGroup && isVisibleOrTransitioning(VISIBLE_TYPE_SINGLELINE)) {
            return res.getDimensionPixelSize(
                    R.dimen.notification_action_list_height);
        }

        // Transition between heads-up & expanded, or pinned.
        if (mHeadsUpChild != null && mExpandedChild != null) {
            boolean transitioningBetweenHunAndExpanded =
                    isTransitioningFromTo(VISIBLE_TYPE_HEADSUP, VISIBLE_TYPE_EXPANDED) ||
                    isTransitioningFromTo(VISIBLE_TYPE_EXPANDED, VISIBLE_TYPE_HEADSUP);
            boolean pinned = !isVisibleOrTransitioning(VISIBLE_TYPE_CONTRACTED)
                    && (getBoolean(mIsHeadsUp, getContentView()) || mHeadsUpAnimatingAway);
//                    && !mRowHelper.isOnKeyguard();
            if (transitioningBetweenHunAndExpanded || pinned) {
                return Math.min(mHeadsUpChild.getHeight(), mExpandedChild.getHeight());
            }
        }

        // Size change of the expanded version
        if ((getVisibleType() == VISIBLE_TYPE_EXPANDED) && mContentHeightAtAnimationStart >= 0
                && mExpandedChild != null) {
            return Math.min(mContentHeightAtAnimationStart, mExpandedChild.getHeight());
        }

        int hint;
//        if (mAmbientChild != null && isVisibleOrTransitioning(VISIBLE_TYPE_AMBIENT)) {
//            hint = mAmbientChild.getHeight();
//        } else if (mAmbientSingleLineChild != null && isVisibleOrTransitioning(
//                VISIBLE_TYPE_AMBIENT_SINGLELINE)) {
//            hint = mAmbientSingleLineChild.getHeight();
//        } else
        if (mHeadsUpChild != null && isVisibleOrTransitioning(VISIBLE_TYPE_HEADSUP)) {
            hint = mHeadsUpChild.getHeight();
        } else if (mExpandedChild != null) {
            hint = mExpandedChild.getHeight();
        } else {
            hint = mContractedChild.getHeight() + res.getDimensionPixelSize(
                    R.dimen.notification_action_list_height);
        }

        if (mExpandedChild != null && isVisibleOrTransitioning(VISIBLE_TYPE_EXPANDED)) {
            hint = Math.min(hint, mExpandedChild.getHeight());
        }
        return hint;
    }

    public boolean isTransitioningFromTo(int from, int to) {
        return (mTransformationStartVisibleType == from || mAnimationStartVisibleType == from)
                && getVisibleType() == to;
    }

    public boolean isVisibleOrTransitioning(int type) {
        return getVisibleType() == type || mTransformationStartVisibleType == type
                || mAnimationStartVisibleType == type;
    }

    public void updateContentTransformation() {
        int visibleType = invoke(calculateVisibleType, getContentView());
        if (visibleType != getVisibleType()) {
            // A new transformation starts
            mTransformationStartVisibleType = getVisibleType();
            final TransformableView shownView = getTransformableViewForVisibleType(visibleType);
            final TransformableView hiddenView = getTransformableViewForVisibleType(
                    mTransformationStartVisibleType);
            shownView.transformFrom(hiddenView, 0.0f);
            getViewForVisibleType(visibleType).setVisibility(VISIBLE);
            hiddenView.transformTo(shownView, 0.0f);
            set(mVisibleType, getContentView(), visibleType);
            updateBackgroundColor(true /*animate*/);
        }
        if (mForceSelectNextLayout) {
            forceUpdateVisibilities();
        }
        if (mTransformationStartVisibleType != UNDEFINED
                && getVisibleType() != mTransformationStartVisibleType
                && getViewForVisibleType(mTransformationStartVisibleType) != null) {
            final TransformableView shownView = getTransformableViewForVisibleType(getVisibleType());
            final TransformableView hiddenView = getTransformableViewForVisibleType(
                    mTransformationStartVisibleType);
            float transformationAmount = calculateTransformationAmount();
            shownView.transformFrom(hiddenView, transformationAmount);
            hiddenView.transformTo(shownView, transformationAmount);
            updateBackgroundTransformation(transformationAmount);
        } else {
            invoke(updateViewVisibilities, getContentView(), visibleType); //TODO: here's the problem (expand button visible)
            updateBackgroundColor(false);
        }
    }

    public void updateBackgroundTransformation(float transformationAmount) {
        int endColor = getBackgroundColor(getVisibleType());
        int startColor = getBackgroundColor(mTransformationStartVisibleType);
        if (endColor != startColor) {
            if (startColor == 0) {
                startColor = mRowHelper.getOutlineHelper().getBackgroundColorWithoutTint();
            }
            if (endColor == 0) {
                endColor = mRowHelper.getOutlineHelper().getBackgroundColorWithoutTint();
            }
            endColor = NotificationUtils.interpolateColors(startColor, endColor,
                    transformationAmount);
        }
        mRowHelper.getOutlineHelper().updateBackgroundAlpha(transformationAmount);
        mRowHelper.setContentBackground(endColor, false, getContentView());
    }

    public float calculateTransformationAmount() {
        int startHeight = getViewForVisibleType(mTransformationStartVisibleType).getHeight();
        int endHeight = getViewForVisibleType(getVisibleType()).getHeight();
        int progress = Math.abs(getContentHeight() - startHeight);
        int totalDistance = Math.abs(endHeight - startHeight);
        float amount = (float) progress / (float) totalDistance;
        return Math.min(1.0f, amount);
    }

    public int getMinHeight() {
        return getMinHeight(false /*likeGroupExpanded*/);
    }

    public int getMinHeight(boolean likeGroupExpanded) {
        /*if (mContainingNotification.isShowingAmbient()) {
            return getShowingAmbientView().getHeight(); //TODO implement
        } else */if (likeGroupExpanded || !mIsChildInGroup || isGroupExpanded()) {
            return mContractedChild.getHeight();
        } else {
            return mSingleLineView.getHeight();
        }
    }

    /*public View getShowingAmbientView() {
        View v = mIsChildInGroup ? mAmbientSingleLineChild : mAmbientChild;
        if (v != null) {
            return v;
        } else {
            return mContractedChild;
        }
    }*/

    public boolean isGroupExpanded() {
        return NotificationGroupManagerHooks.isGroupExpanded(mGroupManager, mStatusBarNotification);
    }

    /*public void setClipBottomAmount(int clipBottomAmount) {
        mClipBottomAmount = clipBottomAmount;
        XposedHelpers.callMethod(getContentView(), "updateClipping");
    }*/

    public void setClipToActualHeight(boolean clipToActualHeight) {
        mClipToActualHeight = clipToActualHeight;
        invoke(updateClipping, getContentView());
    }

    public void forceUpdateVisibilities() {
        forceUpdateVisibility(VISIBLE_TYPE_CONTRACTED, mContractedChild, mContractedWrapper);
        forceUpdateVisibility(VISIBLE_TYPE_EXPANDED, mExpandedChild, mExpandedWrapper);
        forceUpdateVisibility(VISIBLE_TYPE_HEADSUP, mHeadsUpChild, mHeadsUpWrapper);
        forceUpdateVisibility(VISIBLE_TYPE_SINGLELINE, mSingleLineView, mSingleLineView);
//        forceUpdateVisibility(VISIBLE_TYPE_AMBIENT, mAmbientChild, mAmbientWrapper);
//        forceUpdateVisibility(VISIBLE_TYPE_AMBIENT_SINGLELINE, mAmbientSingleLineChild,
//                mAmbientSingleLineChild);
//        fireExpandedVisibleListenerIfVisible();
        // forceUpdateVisibilities cancels outstanding animations without updating the
        // mAnimationStartVisibleType. Do so here instead.
//        mAnimationStartVisibleType = UNDEFINED;
    }

    /*public void fireExpandedVisibleListenerIfVisible() {
        if (mExpandedVisibleListener != null && mExpandedChild != null && isShown()
                && mExpandedChild.getVisibility() == VISIBLE) {
            Runnable listener = mExpandedVisibleListener;
            mExpandedVisibleListener = null;
            listener.run();
        }
    }*/

    public void forceUpdateVisibility(int type, View view, TransformableView wrapper) {
        if (view == null) {
            return;
        }
        boolean visible = getVisibleType() == type
                || mTransformationStartVisibleType == type;
        if (!visible) {
            view.setVisibility(INVISIBLE);
        } else {
            wrapper.setVisible(true);
        }
    }

    public void updateBackgroundColor(boolean animate) {
        int customBackgroundColor = getBackgroundColor(getVisibleType());
        mRowHelper.getOutlineHelper().resetBackgroundAlpha();
        mRowHelper.setContentBackground(customBackgroundColor, animate, getContentView());
    }

    public int getVisibleType() {
        return getInt(mVisibleType, getContentView());
    }

    public int getContentHeight() {
        return getInt(mContentHeight, getContentView());
    }

    public int getBackgroundColorForExpansionState() {
        // When expanding or user locked we want the new type, when collapsing we want
        // the original type
        final int visibleType = (getContainingHelper().isGroupExpanded()
                || (boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isUserLocked, mContainingNotification))
                ? (int) invoke(calculateVisibleType, getContentView())
                : getVisibleType();
        return getBackgroundColor(visibleType);
    }

    public int getBackgroundColor(int visibleType) {
        NotificationViewWrapper currentVisibleWrapper = getVisibleWrapper(visibleType);
        int customBackgroundColor = 0;
        if (currentVisibleWrapper != null) {
            customBackgroundColor = currentVisibleWrapper.getCustomBackgroundColor();
        }
        return customBackgroundColor;
    }

    public void updateViewVisibility(int visibleType, int type, View view,
                                     TransformableView wrapper) {
        if (view != null) {
            wrapper.setVisible(visibleType == type);
        }
    }

    public void transferRemoteInputFocus(int visibleType) {
        if (visibleType == VISIBLE_TYPE_HEADSUP
                && mHeadsUpRemoteInput != null
                && (mExpandedRemoteInput != null && mExpandedRemoteInput.isActive())) {
            mHeadsUpRemoteInput.stealFocusFrom(mExpandedRemoteInput);
        }
        if (visibleType == VISIBLE_TYPE_EXPANDED
                && mExpandedRemoteInput != null
                && (mHeadsUpRemoteInput != null && mHeadsUpRemoteInput.isActive())) {
            mExpandedRemoteInput.stealFocusFrom(mHeadsUpRemoteInput);
        }
    }

    /**
     * @param visibleType one of the static enum types in this view
     * @return the corresponding transformable view according to the given visible type
     */

    public TransformableView getTransformableViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case VISIBLE_TYPE_EXPANDED:
                return mExpandedWrapper;
            case VISIBLE_TYPE_HEADSUP:
                return mHeadsUpWrapper;
            case VISIBLE_TYPE_SINGLELINE:
                return mSingleLineView;
            /*case VISIBLE_TYPE_AMBIENT:
                return mAmbientWrapper;
            case VISIBLE_TYPE_AMBIENT_SINGLELINE:
                return mAmbientSingleLineChild;*/
            default:
                return mContractedWrapper;
        }
    }

    public boolean shouldContractedBeFixedSize() {
        return mBeforeN && mContractedWrapper instanceof NotificationCustomViewWrapper;
    }

    public View getViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case VISIBLE_TYPE_EXPANDED:
                return mExpandedChild;
            case VISIBLE_TYPE_HEADSUP:
                return mHeadsUpChild;
            case VISIBLE_TYPE_SINGLELINE:
                return mSingleLineView;
            /*case VISIBLE_TYPE_AMBIENT:
                return mAmbientChild;
            case VISIBLE_TYPE_AMBIENT_SINGLELINE:
                return mAmbientSingleLineChild;*/
            default:
                return mContractedChild;
        }
    }

    public NotificationViewWrapper getVisibleWrapper(int visibleType) {
        switch (visibleType) {
            case VISIBLE_TYPE_EXPANDED:
                return mExpandedWrapper;
            case VISIBLE_TYPE_HEADSUP:
                return mHeadsUpWrapper;
            case VISIBLE_TYPE_CONTRACTED:
                return mContractedWrapper;
            /*case VISIBLE_TYPE_AMBIENT:
                return mAmbientWrapper;*/
            default:
                return null;
        }
    }

    public int getVisualTypeForHeight(float viewHeight) {
        boolean noExpandedChild = mExpandedChild == null;
        if (!noExpandedChild && viewHeight == mExpandedChild.getHeight()) {
            return VISIBLE_TYPE_EXPANDED;
        }
        if (!mUserExpanding && mIsChildInGroup && !isGroupExpanded()) {
            return VISIBLE_TYPE_SINGLELINE;
        }

        if ((getBoolean(mIsHeadsUp, getContentView()) || mHeadsUpAnimatingAway) && mHeadsUpChild != null
               /* && !mRowHelper.isOnKeyguard()*/) {
            if (viewHeight <= mHeadsUpChild.getHeight() || noExpandedChild) {
                return VISIBLE_TYPE_HEADSUP;
            } else {
                return VISIBLE_TYPE_EXPANDED;
            }
        } else {
            if (noExpandedChild || (viewHeight <= mContractedChild.getHeight()
                    && (!mIsChildInGroup || isGroupExpanded()
                    || !ExpandableNotificationRowHelper.isExpanded(mContainingNotification, true  /*allowOnKeyguard*//* */)))) {
                return VISIBLE_TYPE_CONTRACTED;
            } else {
                return VISIBLE_TYPE_EXPANDED;
            }
        }
    }

    public void setLegacy(boolean legacy) {
        mLegacy = legacy;
        updateLegacy();
    }

    public void updateLegacy() {
        if (mContractedChild != null) {
            mContractedWrapper.setLegacy(mLegacy);
        }
        if (mExpandedChild != null) {
            mExpandedWrapper.setLegacy(mLegacy);
        }
        if (mHeadsUpChild != null) {
            mHeadsUpWrapper.setLegacy(mLegacy);
        }
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
        mIsChildInGroup = isChildInGroup;
//        if (mContractedChild != null) {
//            mContractedWrapper.setIsChildInGroup(mIsChildInGroup);
//        }
//        if (mExpandedChild != null) {
//            mExpandedWrapper.setIsChildInGroup(mIsChildInGroup);
//        }
//        if (mHeadsUpChild != null) {
//            mHeadsUpWrapper.setIsChildInGroup(mIsChildInGroup);
//        }
//        if (mAmbientChild != null) {
//            mAmbientWrapper.setIsChildInGroup(mIsChildInGroup);
//        }
        //updateAllSingleLineViews(); //TODO implement
        updateSingleLineView();
    }

    /*public void updateAllSingleLineViews() {
        updateSingleLineView();
        updateAmbientSingleLineView();
    }*/

    private void updateSingleLineView() {
        if (mIsChildInGroup) {
            mSingleLineView = mHybridGroupManager.bindFromNotification(
                    mSingleLineView, mStatusBarNotification.getNotification());
        } else if (mSingleLineView != null) {
            getContentView().removeView(mSingleLineView);
            mSingleLineView = null;
        }
    }

    /*public void updateAmbientSingleLineView() {
        if (mIsChildInGroup) {
            mAmbientSingleLineChild = mHybridGroupManager.bindAmbientFromNotification(
                    mAmbientSingleLineChild, mStatusBarNotification.getNotification());
        } else if (mAmbientSingleLineChild != null) {
            getContentView().removeView(mAmbientSingleLineChild);
            mAmbientSingleLineChild = null;
        }
    }*/

    public void applyRemoteInput(final Object entry) {
        if (mRemoteInputController == null) {
            return;
        }

        boolean hasRemoteInput = false;

        Notification notification = ((StatusBarNotification) get(Fields.SystemUI.NotificationDataEntry.notification, entry)).getNotification();
        Notification.Action[] actions = notification.actions;
        if (actions != null) {
            for (Notification.Action a : actions) {
                if (a.getRemoteInputs() != null) {
                    for (RemoteInput ri : a.getRemoteInputs()) {
                        if (ri.getAllowFreeFormInput()) {
                            hasRemoteInput = true;
                            break;
                        }
                    }
                }
            }
        }

        View bigContentView = mExpandedChild;
        if (bigContentView != null) {
            mExpandedRemoteInput = applyRemoteInput(bigContentView, entry, hasRemoteInput,
                    mPreviousExpandedRemoteInputIntent, mCachedExpandedRemoteInput);
        } else {
            mExpandedRemoteInput = null;
        }
        if (mCachedExpandedRemoteInput != null
                && mCachedExpandedRemoteInput != mExpandedRemoteInput) {
            // We had a cached remote input but didn't reuse it. Clean up required.
            mCachedExpandedRemoteInput.dispatchFinishTemporaryDetach();
        }
        mCachedExpandedRemoteInput = null;

        View headsUpContentView = get(Fields.SystemUI.NotificationContentView.mHeadsUpChild, getContentView());
        if (headsUpContentView != null) {
            mHeadsUpRemoteInput = applyRemoteInput(headsUpContentView, entry, hasRemoteInput,
                    mPreviousHeadsUpRemoteInputIntent, mCachedHeadsUpRemoteInput);
        } else {
            mHeadsUpRemoteInput = null;
        }
        if (mCachedHeadsUpRemoteInput != null
                && mCachedHeadsUpRemoteInput != mHeadsUpRemoteInput) {
            // We had a cached remote input but didn't reuse it. Clean up required.
            mCachedHeadsUpRemoteInput.dispatchFinishTemporaryDetach();
        }
        mCachedHeadsUpRemoteInput = null;
    }

    public RemoteInputView applyRemoteInput(View view, Object entry,
                                            boolean hasRemoteInput, PendingIntent existingPendingIntent,
                                            RemoteInputView cachedView) {
        //TODO: I think the problem with the onKeyUp and down not working is somewhere here
        Notification notification = ((StatusBarNotification) get(Fields.SystemUI.NotificationDataEntry.notification, entry)).getNotification();
        View actionContainerCandidate = view.findViewById(
                R.id.actions_container);
        if (actionContainerCandidate instanceof FrameLayout) {
            RemoteInputView existing = view.findViewWithTag(RemoteInputView.VIEW_TAG);

            if (existing != null) {
                existing.onNotificationUpdateOrReset();
            }

            if (existing == null && hasRemoteInput) {
                ViewGroup actionContainer = (FrameLayout) actionContainerCandidate;
                if (cachedView == null) {
                    RemoteInputView riv = RemoteInputView.inflate(
                            getContentView().getContext(), actionContainer, entry, mRemoteInputController);

                    riv.setVisibility(View.INVISIBLE);
                    actionContainer.addView(riv, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT)
                    );
                    existing = riv;
                } else {
                    actionContainer.addView(cachedView);
                    cachedView.dispatchFinishTemporaryDetach();
                    cachedView.requestFocus();
                    existing = cachedView;
                }
            }
            if (hasRemoteInput) {
                int color = notification.color;
                if (color == Notification.COLOR_DEFAULT) {
                    color = res.getColor(R.color.default_remote_input_background);
                }
                existing.setBackgroundColor(NotificationColorUtil.ensureTextBackgroundColor(color,
                        res.getColor(R.color.remote_input_text_enabled),
                        res.getColor(R.color.remote_input_hint)));

                if (existingPendingIntent != null || existing.isActive()) {
                    // The current action could be gone, or the pending intent no longer valid.
                    // If we find a matching action in the new notification, focus, otherwise close.
                    Notification.Action[] actions = notification.actions;
                    if (existingPendingIntent != null) {
                        existing.setPendingIntent(existingPendingIntent);
                    }
                    if (existing.updatePendingIntentFromActions(actions)) {
                        if (!existing.isActive()) {
                            existing.focus();
                        }
                    } else {
                        if (existing.isActive()) {
                            existing.close();
                        }
                    }
                }
            }
            return existing;
        }
        return null;
    }

    public void closeRemoteInput() {
        if (mHeadsUpRemoteInput != null) {
            mHeadsUpRemoteInput.close();
        }
        if (mExpandedRemoteInput != null) {
            mExpandedRemoteInput.close();
        }
    }

    public void setGroupManager(Object groupManager) {
        mGroupManager = groupManager;
    }

    public void setRemoteInputController(RemoteInputController r) {
        mRemoteInputController = r;
    }

    public void setExpandClickListener(View.OnClickListener expandClickListener) {
        mExpandClickListener = expandClickListener;
    }

    public void updateExpandButtons(boolean expandable) {
        mExpandable = expandable;
        // if the expanded child has the same height as the collapsed one we hide it.
        if (mExpandedChild != null && mExpandedChild.getHeight() != 0) { //TODO: probably has something to do with the lockscreen issue
            if ((!getBoolean(mIsHeadsUp, getContentView())/* && !mHeadsUpAnimatingAway*/)
                    || mHeadsUpChild == null/* || mRowHelper.isOnKeyguard()*/) {
                if (mExpandedChild.getHeight() == mContractedChild.getHeight()) {
                    expandable = false;
                }
            } else if (mExpandedChild.getHeight() == mHeadsUpChild.getHeight()) {
                expandable = false;
            }
        }
        if (mExpandedChild != null) {
            mExpandedWrapper.updateExpandability(expandable, mExpandClickListener);
        }
        if (mContractedChild != null) {
            mContractedWrapper.updateExpandability(expandable, mExpandClickListener);
        }
        if (mHeadsUpChild != null) {
            mHeadsUpWrapper.updateExpandability(expandable,  mExpandClickListener);
        }
        //mIsContentExpandable = expandable;
    }

    public NotificationHeaderView getNotificationHeader() {
        NotificationHeaderView header = null;
        if (mContractedChild != null) {
            header = mContractedWrapper.getNotificationHeader();
        }
        if (header == null && mExpandedChild != null) {
            header = mExpandedWrapper.getNotificationHeader();
        }
        if (header == null && mHeadsUpChild != null) {
            header = mHeadsUpWrapper.getNotificationHeader();
        }
        /*if (header == null && mAmbientChild != null) {
            header = mAmbientWrapper.getNotificationHeader();
        }*/
        return header;
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        NotificationViewWrapper wrapper = getVisibleWrapper(getVisibleType());
        return wrapper == null ? null : wrapper.getNotificationHeader();
    }

    public void setContainingNotification(View containingNotification) {
        if (mRowHelper == null || containingNotification != mContainingNotification)
            mRowHelper = ExpandableNotificationRowHelper.getInstance(containingNotification);
        mContainingNotification = containingNotification;
    }

    public void requestSelectLayout(boolean needsAnimation) {
        invoke(selectLayout, getContentView(), needsAnimation, false);
    }

    public void reInflateViews() {
        if (mIsChildInGroup && mSingleLineView != null) {
            getContentView().removeView(mSingleLineView);
            mSingleLineView = null;
            updateSingleLineView();
            //updateAllSingleLineViews(); //TODO implement
        }
    }

    public void setUserExpanding(boolean userExpanding) {
        mUserExpanding = userExpanding;
        if (userExpanding) {
            mTransformationStartVisibleType = getVisibleType();
        } else {
            mTransformationStartVisibleType = UNDEFINED;
            set(mVisibleType, getContentView(), invoke(calculateVisibleType, getContentView()));
            invoke(updateViewVisibilities, getContentView(), getVisibleType());
            updateBackgroundColor(false);
        }
    }

    /**
     * Set by how much the single line view should be indented. Used when a overflow indicator is
     * present and only during measuring
     */
    public void setSingleLineWidthIndention(int singleLineWidthIndention) {
        if (singleLineWidthIndention != mSingleLineWidthIndention) {
            mSingleLineWidthIndention = singleLineWidthIndention;
            mContainingNotification.forceLayout();
            getContentView().forceLayout();
        }
    }

    public HybridNotificationView getSingleLineView() {
        return mSingleLineView;
    }

    public void setRemoved() {
        if (mExpandedRemoteInput != null) {
            mExpandedRemoteInput.setRemoved();
        }
        if (mHeadsUpRemoteInput != null) {
            mHeadsUpRemoteInput.setRemoved();
        }
    }

    public void setContentHeightAnimating(boolean animating) {
        if (!animating) {
            mContentHeightAtAnimationStart = UNDEFINED;
        }
    }

    /*boolean isAnimatingVisibleType() {
        return mAnimationStartVisibleType != UNDEFINED;
    }*/

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        mHeadsUpAnimatingAway = headsUpAnimatingAway;
        invoke(selectLayout, getContentView(), false /*animate*/, true /*force*/);
    }

    public void setFocusOnVisibilityChange() {
        mFocusOnVisibilityChange = true;
    }

    public ExpandableNotificationRowHelper getContainingHelper() {
        return mRowHelper;
    }
}
