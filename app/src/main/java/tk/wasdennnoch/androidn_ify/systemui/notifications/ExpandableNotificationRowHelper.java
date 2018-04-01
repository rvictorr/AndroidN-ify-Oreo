package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.Nullable;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Chronometer;
import android.widget.FrameLayout;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.HybridNotificationView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputController;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationChildrenContainerHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationGroupManagerHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.NotificationColorUtil;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.LAYER_TYPE_NONE;
import static android.view.View.VISIBLE;
import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_ANDROID;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ExpandableNotificationRow.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ExpandableNotificationRow.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationContentView.*;

public class ExpandableNotificationRowHelper {

    private FrameLayout mExpandableRow;
    private ResourceUtils res;
    private NotificationContentHelper mPrivateHelper;
    private NotificationContentHelper mPublicHelper;
    private ExpandableOutlineViewHelper mOutlineHelper;
    private ExpandableNotificationRowHelper mParentHelper;
    public NotificationChildrenContainerHelper mChildrenContainerHelper;

    private static Object mHeadsUpManager;

    private int mNotificationMinHeightLegacy;
    private int mMaxHeadsUpHeightLegacy;
    private int mMaxHeadsUpHeight;
    private int mNotificationMinHeight;
    private int mNotificationMaxHeight;
    private int mIncreasedPaddingBetweenElements;

    public FrameLayout mPublicLayout;
    public FrameLayout mPrivateLayout;
    public Object mEntry;
    public View mChildrenContainer;

    public Animator mTranslateAnim;
    public ArrayList<View> mTranslateableViews;

    public int mNotificationColor;
    public boolean mIsSummaryWithChildren;
    private ViewStub mSettingsIconRowStub;

    private boolean mJustClicked;
    public boolean mIconAnimationRunning;
    private boolean mShowNoBackground;
    private FrameLayout mNotificationParent;
    private OnExpandClickListener mOnExpandClickListener;
    private boolean mGroupExpansionChanging;
    public boolean mExpandedWhenPinned;

    private Object mGroupManager;

    public View.OnClickListener mExpandClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            StatusBarNotification statusBarNotification = get(mStatusBarNotification, mExpandableRow);
            if (!getBoolean(mShowingPublic, mExpandableRow)
                    && NotificationGroupManagerHooks.isSummaryOfGroup(mGroupManager, statusBarNotification)) {
                mGroupExpansionChanging = true;
                final boolean wasExpanded = NotificationGroupManagerHooks.isGroupExpanded(mGroupManager, statusBarNotification);
                boolean nowExpanded = NotificationGroupManagerHooks.toggleGroupExpansion(mGroupManager, statusBarNotification);
                mOnExpandClickListener.onExpandClicked(mEntry, nowExpanded);
                XposedHelpers.callMethod(mExpandableRow, "logExpansionEvent", true /*userAction*/, wasExpanded);
            } else {
                if (v.isAccessibilityFocused()) {
                    mPrivateHelper.setFocusOnVisibilityChange();
                }
                boolean nowExpanded;
                if (getBoolean(mIsPinned, mExpandableRow)) {
                    nowExpanded = !mExpandedWhenPinned;
                    mExpandedWhenPinned = nowExpanded;
                } else {
                    nowExpanded = !(boolean) invoke(isExpanded, mExpandableRow);
                    setUserExpanded(nowExpanded);
                }
                invoke(notifyHeightChanged, mExpandableRow, true);
                mOnExpandClickListener.onExpandClicked(mEntry, nowExpanded);
                MetricsLogger.action(mExpandableRow.getContext(), 407 /*MetricsEvent.ACTION_NOTIFICATION_EXPANDER*/,
                        nowExpanded);
            }
        }
    };
    public boolean mForceUnlocked;
    private boolean mDismissed;
    private boolean mKeepInParent;
    private boolean mRemoved;
    private static final Property<View, Float> TRANSLATE_CONTENT =
            new FloatProperty<View>("translate") {
                @Override
                public void setValue(View object, float value) {
                    ExpandableNotificationRowHelper.getInstance(object).setTranslation(value);
                }

                @Override
                public Float get(View object) {
                    return ExpandableNotificationRowHelper.getInstance(object).getTranslation();
                }
            };
    private View.OnClickListener mOnClickListener;
    public boolean mHeadsupDisappearRunning;
    private View mChildAfterViewWhenDismissed;
    private View mGroupParentWhenDismissed;
    private boolean mRefocusOnDismiss;

    private ExpandableNotificationRowHelper(Object obj) {
        XposedHelpers.setAdditionalInstanceField(obj, "mExpandableRowHelper", this);
        mExpandableRow = (FrameLayout) obj;
        res = ResourceUtils.getInstance(mExpandableRow.getContext());
    }

    public static ExpandableNotificationRowHelper getInstance(Object obj) {
        ExpandableNotificationRowHelper helper = (ExpandableNotificationRowHelper) XposedHelpers.getAdditionalInstanceField(obj, "mExpandableRowHelper");
        return helper != null ? helper : new ExpandableNotificationRowHelper(obj);
    }

    public View getExpandableRow() {
        return mExpandableRow;
    }

    public void setOutlineHelper(ExpandableOutlineViewHelper helper) {
        mOutlineHelper = helper;
    }

    public ExpandableOutlineViewHelper getOutlineHelper() {
        return mOutlineHelper;
    }

    public void setGroupManager(Object groupManager) {
        mGroupManager = groupManager;
        mPrivateHelper.setGroupManager(groupManager);
    }

    public void setActualHeightAnimating(boolean animating) {
        if (mPrivateLayout != null) {
            mPrivateHelper.setContentHeightAnimating(animating);
        }
    }

    /**
     * @param atLeastMinHeight should the value returned be at least the minimum height.
     *                         Used to avoid cyclic calls
     * @return the height of the heads up notification when pinned
     */
    public int getPinnedHeadsUpHeight(boolean atLeastMinHeight) {
        int mHeadsUpHeight = getInt(Fields.SystemUI.ExpandableNotificationRow.mHeadsUpHeight, mExpandableRow);
        if (mIsSummaryWithChildren) {
            return (int) invoke(Methods.SystemUI.NotificationChildrenContainer.getIntrinsicHeight, mChildrenContainer);
        }
        if(mExpandedWhenPinned) {
            return Math.max(getInt(mMaxExpandHeight, mExpandableRow), mHeadsUpHeight);
        } else if (atLeastMinHeight) {
            return Math.max(getCollapsedHeight(), mHeadsUpHeight);
        } else {
            return mHeadsUpHeight;
        }
    }

    public void initDimens() {
        mNotificationMinHeightLegacy = getFontScaledHeight(R.dimen.notification_min_height_legacy);
        mNotificationMinHeight = getFontScaledHeight(R.dimen.notification_min_height);
        mNotificationMaxHeight = getFontScaledHeight(R.dimen.notification_max_height);
        mMaxHeadsUpHeightLegacy = getFontScaledHeight(
                R.dimen.notification_max_heads_up_height_legacy);
        mMaxHeadsUpHeight = getFontScaledHeight(R.dimen.notification_max_heads_up_height);
        mIncreasedPaddingBetweenElements = res.getResources()
                .getDimensionPixelSize(R.dimen.notification_divider_height_increased);
    }

    /**
     * @param dimenId the dimen to look up
     * @return the font scaled dimen as if it were in sp but doesn't shrink sizes below dp
     */
    public int getFontScaledHeight(int dimenId) {
        int dimensionPixelSize = res.getResources().getDimensionPixelSize(dimenId);
        float factor = Math.max(1.0f, res.getResources().getDisplayMetrics().scaledDensity /
                res.getResources().getDisplayMetrics().density);
        return (int) (dimensionPixelSize * factor);
    }

    public void onFinishInflate() {
        mPrivateLayout = get(Fields.SystemUI.ExpandableNotificationRow.mPrivateLayout, mExpandableRow);
        mPublicLayout = get(Fields.SystemUI.ExpandableNotificationRow.mPublicLayout, mExpandableRow);
        ViewStub mExpandButtonStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mExpandButtonStub");
        View mExpandButton = (View) XposedHelpers.getObjectField(mExpandableRow, "mExpandButton");
        ViewGroup mExpandButtonContainer = (ViewGroup) XposedHelpers.getObjectField(mExpandableRow, "mExpandButtonContainer");
        ViewStub mChildrenContainerStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mChildrenContainerStub");
        mChildrenContainer = get(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, mExpandableRow);
        ViewStub mGutsStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mGutsStub");
        View mVetoButton = (View) XposedHelpers.getObjectField(mExpandableRow, "mVetoButton");

        mPrivateHelper = NotificationContentHelper.getInstance(mPrivateLayout);
        mPublicHelper = NotificationContentHelper.getInstance(mPublicLayout);
        mPrivateHelper.setContainingNotification(mExpandableRow);
        mPublicHelper.setContainingNotification(mExpandableRow);
        mPrivateHelper.setExpandClickListener(mExpandClickListener);
        mPublicHelper.setExpandClickListener(mExpandClickListener);

        mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                set(Fields.SystemUI.ExpandableNotificationRow.mChildrenContainer, mExpandableRow, inflated);
                mChildrenContainer = inflated;
                mChildrenContainerHelper = NotificationChildrenContainerHelper.getInstance(mChildrenContainer);
                mChildrenContainerHelper.setNotificationParent(mExpandableRow);
                mChildrenContainerHelper.onNotificationUpdated();
                mTranslateableViews.add(inflated);
            }
        });
        mTranslateableViews = new ArrayList<>();
        for (int i = 0; i < mExpandableRow.getChildCount(); i++) {
            mTranslateableViews.add(mExpandableRow.getChildAt(i));
        }
        // Remove views that don't translate
        mTranslateableViews.remove(mVetoButton);
//        mTranslateableViews.remove(mSettingsIconRowStub);
        //mTranslateableViews.remove(mChildrenContainerStub);
//        mTranslateableViews.remove(mGutsStub);
    }

    public void resetTranslation() {
        if (mTranslateAnim != null) {
            mTranslateAnim.cancel();
        }
        if (mTranslateableViews != null) {
            for (int i = 0; i < mTranslateableViews.size(); i++) {
                mTranslateableViews.get(i).setTranslationX(0);
            }
        }
        mExpandableRow.invalidateOutline();
        /*if (mSettingsIconRow != null) {
            mSettingsIconRow.resetState();
        }*/
    }

    public void animateTranslateNotification(final float leftTarget) {
        if (mTranslateAnim != null) {
            mTranslateAnim.cancel();
        }
        mTranslateAnim = getTranslateViewAnimator(leftTarget, null /* updateListener */);
        if (mTranslateAnim != null) {
            mTranslateAnim.start();
        }
    }

    public void setTranslation(float translationX) {
        /*if (areGutsExposed()) {
            // Don't translate if guts are showing.
            return;
        }*/
        // Translate the group of views
        for (int i = 0; i < mTranslateableViews.size(); i++) {
            if (mTranslateableViews.get(i) != null) {
                mTranslateableViews.get(i).setTranslationX(translationX);
            }
        }
        mExpandableRow.invalidateOutline();
        /*if (mSettingsIconRow != null) {
            mSettingsIconRow.updateSettingsIcons(translationX, getMeasuredWidth());
        }*/
    }

    public float getTranslation() {
        if (mTranslateableViews != null && mTranslateableViews.size() > 0) {
            // All of the views in the list should have same translation, just use first one.
            return mTranslateableViews.get(0).getTranslationX();
        }
        return 0;
    }

    public Animator getTranslateViewAnimator(final float leftTarget,
                                             ValueAnimator.AnimatorUpdateListener listener) {
        if (mTranslateAnim != null) {
            mTranslateAnim.cancel();
        }
        /*if (areGutsExposed()) {
            // No translation if guts are exposed.
            return null;
        }*/
        final ObjectAnimator translateAnim = ObjectAnimator.ofFloat(mExpandableRow, TRANSLATE_CONTENT,
                leftTarget);
        if (listener != null) {
            translateAnim.addUpdateListener(listener);
        }
        translateAnim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator anim) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator anim) {
                /*if (!cancelled && mSettingsIconRow != null && leftTarget == 0) {
                    mSettingsIconRow.resetState();
                    mTranslateAnim = null;
                }*/
            }
        });
        mTranslateAnim = translateAnim;
        return translateAnim;
    }

    public void setOnClickListener(@Nullable View.OnClickListener l) {
        mOnClickListener = l;
        updateClickAndFocus();
    }

    public void updateClickAndFocus() {
        boolean normalChild = !isChildInGroup() || isGroupExpanded();
        boolean clickable = mOnClickListener != null && normalChild;
        if (mExpandableRow.isFocusable() != normalChild) {
            mExpandableRow.setFocusable(normalChild);
        }
        if (mExpandableRow.isClickable() != clickable) {
            mExpandableRow.setClickable(clickable);
        }
    }

    public static void setHeadsUpManager(Object headsUpManager) {
        mHeadsUpManager = headsUpManager;
    }

    public int getNotificationColor() {
        return mNotificationColor;
    }

    private void updateNotificationColor() {
        NotificationColorUtil.setContext(mExpandableRow.getContext());
        mNotificationColor = NotificationColorUtil.resolveContrastColor(((StatusBarNotification) invoke(getStatusBarNotification, mExpandableRow)).getNotification().color);
    }

    public HybridNotificationView getSingleLineView() {
        return mPrivateHelper.getSingleLineView();
    }

    public boolean isOnKeyguard() {
        return isOnKeyguard(mExpandableRow);
    }

    public static boolean isOnKeyguard(Object row) {
        return getBoolean(Fields.SystemUI.ExpandableNotificationRow.mExpansionDisabled, row);
    }

    public void removeAllChildren() {
        List<FrameLayout> notificationChildren
                = invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, mChildrenContainer);
        ArrayList<FrameLayout> clonedList = new ArrayList<>(notificationChildren);
        for (int i = 0; i < clonedList.size(); i++) {
            FrameLayout row = clonedList.get(i);
            ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);
            if (rowHelper.keepInParent()) {
                continue;
            }
            mChildrenContainerHelper.removeNotification(row);
            rowHelper.setIsChildInGroup(false, null);
        }
        onChildrenCountChanged();
    }

    public void setForceUnlocked(boolean forceUnlocked) {
        mForceUnlocked = forceUnlocked;
        if (mIsSummaryWithChildren) {
            List notificationChildren = invoke(getNotificationChildren, mExpandableRow);
            for (Object child : notificationChildren) {
                ExpandableNotificationRowHelper.getInstance(child).setForceUnlocked(forceUnlocked);
            }
        }
    }

    public boolean isGroupExpansionChanging() {
        if (isChildInGroup()) {
            return mParentHelper.isGroupExpansionChanging();
        }
        return mGroupExpansionChanging;
    }

    public NotificationContentHelper getPrivateHelper() {
        return mPrivateHelper;
    }

    public NotificationContentHelper getPublicHelper() {
        return mPublicHelper;
    }

    public void setGroupExpansionChanging(boolean changing) { //TODO: call in goToLockedShade (PhoneStatusBar)
        mGroupExpansionChanging = changing;
    }

    public void onChildrenCountChanged() {

        mIsSummaryWithChildren = NotificationsStuff.ENABLE_CHILD_NOTIFICATIONS
                && mChildrenContainer != null && mChildrenContainerHelper.getNotificationChildCount() > 0;
        if (mIsSummaryWithChildren && mChildrenContainerHelper.getHeaderView() == null) {
            mChildrenContainerHelper.recreateNotificationHeader(mExpandClickListener);
        }
        getShowingHelper().updateBackgroundColor(false  /*animate*/ );
        mPrivateHelper.updateExpandButtons((boolean) invoke(isExpandable, mExpandableRow));
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
    }

    public void updateChildrenVisibility() {
        boolean showingPublic = getBoolean(mShowingPublic, mExpandableRow);
        mPrivateLayout.setVisibility(!showingPublic && !mIsSummaryWithChildren ? VISIBLE
                : INVISIBLE);
        if (mChildrenContainer != null) {
            mChildrenContainer.setVisibility(!showingPublic && mIsSummaryWithChildren ? VISIBLE
                    : INVISIBLE);
            mChildrenContainerHelper.updateHeaderVisibility(!showingPublic && mIsSummaryWithChildren
                    ? VISIBLE
                    : INVISIBLE);
        }
        // The limits might have changed if the view suddenly became a group or vice versa
        updateLimits();
    }

    public void updateChildrenHeaderAppearance() {
        if (mIsSummaryWithChildren) {
            mChildrenContainerHelper.updateChildrenHeaderAppearance();
        }
    }

    public void notifyHeightChanged(boolean needsAnimation) {
        getShowingHelper().requestSelectLayout(needsAnimation || (boolean) invoke(isUserLocked, mExpandableRow));
    }

//    public void makeActionsVisibile() { //probably won't ever use this as it's related to some work stuff
//        //invoke(setUserExpanded, mExpandableRow, true/*, true*/);
//        setUserExpanded(true, true);
//        if (isChildInGroup()) {
//            XposedHelpers.callMethod(mGroupManager, "setGroupExpanded", get(mStatusBarNotification, mExpandableRow), true);
//        }
//        invoke(notifyHeightChanged, mExpandableRow, false /* needsAnimation */);
//    }

    public void setChildrenExpanded(boolean expanded, boolean animate) {
        if (mChildrenContainer != null) {
            mChildrenContainerHelper.setChildrenExpanded(expanded);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
    }

    protected void onAppearAnimationFinished(boolean wasAppearing) {
        if (wasAppearing) {
            // During the animation the visible view might have changed, so let's make sure all
            // alphas are reset
            if (mChildrenContainer != null) {
                mChildrenContainer.setAlpha(1.0f);
                mChildrenContainer.setLayerType(LAYER_TYPE_NONE, null);
            }
            mPrivateLayout.setAlpha(1.0f);
            mPrivateLayout.setLayerType(LAYER_TYPE_NONE, null);
            mPublicLayout.setAlpha(1.0f);
            mPublicLayout.setLayerType(LAYER_TYPE_NONE, null);
        }
    }

    public int getExtraBottomPadding() {
        if (mIsSummaryWithChildren && isGroupExpanded()) {
            return mIncreasedPaddingBetweenElements;
        }
        return 0;
    }

    public void setHeadsUpAnimatingAway(boolean running) {
        mHeadsupDisappearRunning = running;
        mPrivateHelper.setHeadsUpAnimatingAway(running);
    }

    protected void updateBackgroundTint() {
//        updateBackgroundForGroupState(); //TODO: causes problems
        if (mIsSummaryWithChildren) {
            List<ViewGroup> notificationChildren =
                    invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, mChildrenContainer);
            for (int i = 0; i < notificationChildren.size(); i++) {
                ViewGroup child = notificationChildren.get(i);
                ExpandableNotificationRowHelper.getInstance(child).updateBackgroundForGroupState();
            }
        }
    }

    /**
     * Called when a group has finished animating from collapsed or expanded state.
     */
    public void onFinishedExpansionChange() {
        mGroupExpansionChanging = false;
        updateBackgroundForGroupState();
    }

    /**
     * Updates the parent and children backgrounds in a group based on the expansion state.
     */
    public void updateBackgroundForGroupState() {
        if (mIsSummaryWithChildren) {
            // Only when the group has finished expanding do we hide its background.
            mShowNoBackground = isGroupExpanded() && !isGroupExpansionChanging() && !(boolean) invoke(isUserLocked, mExpandableRow);
            mChildrenContainerHelper.updateHeaderForExpansion(mShowNoBackground);
            List<ViewGroup> children = invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, mChildrenContainer);
            for (int i = 0; i < children.size(); i++) {
                ExpandableNotificationRowHelper.getInstance(children.get(i)).updateBackgroundForGroupState();
            }
        } else if (isChildInGroup()) {
            final int childColor = getShowingHelper().getBackgroundColorForExpansionState();
            // Only show a background if the group is expanded OR if it is expanding / collapsing
            // and has a custom background color
            final boolean showBackground = isGroupExpanded()
                    || ((mParentHelper.isGroupExpansionChanging()
                    || (boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isUserLocked, mExpandableRow)) && childColor != 0);
            mShowNoBackground = !showBackground;
        } else {
            // Only children or parents ever need no background.
            mShowNoBackground = false;
        }
        mOutlineHelper.updateOutline();
        mOutlineHelper.updateBackground();
    }

    public float getIncreasedPaddingAmount() {
        if (mIsSummaryWithChildren) {
            if (isGroupExpanded()) {
                return 1.0f;
            } else if (invoke(isUserLocked, mExpandableRow)) {
                return mChildrenContainerHelper.getGroupExpandFraction();
            }
        }
        return 0.0f;
    }

    protected boolean disallowSingleClick(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        NotificationHeaderView header = getVisibleNotificationHeader();
        if (header != null) {
            return header.isInTouchRect(x - getTranslation(), y);
        }
        return false; //TODO: do something useful
        //return super.disallowSingleClick(event);
    }

    public int getPositionOfChild(FrameLayout childRow) {
        if (mIsSummaryWithChildren) {
            return mChildrenContainerHelper.getPositionInLinearLayout(childRow);
        }
        return 0;
    }

    public boolean isGroupExpanded() {
        return NotificationGroupManagerHooks.isGroupExpanded(mGroupManager, (StatusBarNotification) get(mStatusBarNotification, mExpandableRow));
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        mOnExpandClickListener = onExpandClickListener;
    }

    public NotificationHeaderView getNotificationHeader() {
        if (mIsSummaryWithChildren) {
            return mChildrenContainerHelper.getHeaderView();
        }
        return mPrivateHelper.getNotificationHeader();
    }

    private NotificationHeaderView getVisibleNotificationHeader() {
        if (mIsSummaryWithChildren && !getBoolean(mShowingPublic, mExpandableRow)) {
            return mChildrenContainerHelper.getHeaderView();
        }
        return NotificationContentHelper.getInstance(getShowingLayout).getVisibleNotificationHeader();
    }

    public void onNotificationUpdated(Object entry) {
        mEntry = entry;
        set(mStatusBarNotification, mExpandableRow, get(Fields.SystemUI.NotificationDataEntry.notification, entry));
        mPrivateHelper.onNotificationUpdated(entry);
        mPublicHelper.onNotificationUpdated(entry);
        set(mShowingPublicInitialized, mExpandableRow, false);
        updateNotificationColor();
        if (mIsSummaryWithChildren) {
            mChildrenContainerHelper.recreateNotificationHeader(mExpandClickListener);
            mChildrenContainerHelper.onNotificationUpdated();
        }
        if (mIconAnimationRunning) {
            invoke(setIconAnimationRunning, mExpandableRow, true);
        }
        if (mNotificationParent != null) {
            mParentHelper.updateChildrenHeaderAppearance();
        }
        onChildrenCountChanged();
        // The public layouts expand button is always visible
        mPublicHelper.updateExpandButtons(true);
        updateLimits();
    }

    public void updateLimits() {
        updateLimitsForView(mPrivateHelper);
        updateLimitsForView(mPublicHelper);
    }

    private void updateLimitsForView(NotificationContentHelper layoutHelper) {
        boolean customView = ((View) invoke(getContractedChild, layoutHelper.getContentView())).getId()
                != res.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID);
        boolean beforeN = getInt(Fields.SystemUI.NotificationDataEntry.targetSdk, mEntry) < Build.VERSION_CODES.N;
        int minHeight = customView && beforeN && !mIsSummaryWithChildren ?
                mNotificationMinHeightLegacy : mNotificationMinHeight;
        boolean headsUpCustom = invoke(getHeadsUpChild, layoutHelper.getContentView()) != null &&
                ((View) invoke(getHeadsUpChild, layoutHelper.getContentView())).getId()
                        != res.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID);
        int headsUpheight = headsUpCustom && beforeN ? mMaxHeadsUpHeightLegacy
                : mMaxHeadsUpHeight;
        layoutHelper.setHeights(minHeight, headsUpheight, mNotificationMaxHeight, 0);
    }

    public boolean isChildInGroup() {
        return mNotificationParent != null;
    }

    public FrameLayout getNotificationParent() {
        return mNotificationParent;
    }

    public ExpandableNotificationRowHelper getNotificationParentHelper() {
        return mParentHelper;
    }

    /**
     * @param isChildInGroup Is this notification now in a group
     * @param parent the new parent notification
     */
    public void setIsChildInGroup(boolean isChildInGroup, FrameLayout parent) {
        boolean childInGroup = NotificationsStuff.ENABLE_CHILD_NOTIFICATIONS && isChildInGroup;
        mNotificationParent = childInGroup ? parent : null;
        if (childInGroup) {
            mParentHelper = ExpandableNotificationRowHelper.getInstance(mNotificationParent);
        }
        mPrivateHelper.setIsChildInGroup(childInGroup);
        mOutlineHelper.resetBackgroundAlpha();
        updateBackgroundForGroupState();
        updateClickAndFocus();
        if (mNotificationParent != null) {
            mParentHelper.updateBackgroundForGroupState();
        }
    }

    public boolean isSummaryWithChildren() {
        return mIsSummaryWithChildren;
    }

    public int getNumberOfNotificationChildren() {
        if (mChildrenContainer == null) {
            return 0;
        }
        return ((List)invoke(Methods.SystemUI.NotificationChildrenContainer.getNotificationChildren, mChildrenContainer)).size();
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        mOutlineHelper.setClipToActualHeight(clipToActualHeight || (boolean) invoke(isUserLocked, mExpandableRow));
        NotificationContentHelper.getInstance(getShowingLayout())
                .setClipToActualHeight(clipToActualHeight || (boolean) invoke(isUserLocked, mExpandableRow));
    }

    public void setRemoteInputController(RemoteInputController r) {
        mPrivateHelper.setRemoteInputController(r);
    }

    /**
     * Set this notification to be expanded by the user
     *
     * @param userExpanded whether the user wants this notification to be expanded
     */
    public void setUserExpanded(boolean userExpanded) {
        setUserExpanded(userExpanded, false /* allowChildExpansion */);
    }

    /**
     * Set this notification to be expanded by the user
     *
     * @param userExpanded whether the user wants this notification to be expanded
     * @param allowChildExpansion whether a call to this method allows expanding children
     */
    public void setUserExpanded(boolean userExpanded, boolean allowChildExpansion) {
        StatusBarNotification mStatusBarNotification = get(Fields.SystemUI.ExpandableNotificationRow.mStatusBarNotification, mExpandableRow);
        if (mIsSummaryWithChildren && !getBoolean(mShowingPublic, mExpandableRow) && allowChildExpansion) {
            final boolean wasExpanded = isGroupExpanded();
            invoke(Methods.SystemUI.NotificationGroupManager.setGroupExpandedSbn, mGroupManager, mStatusBarNotification, userExpanded);
            XposedHelpers.callMethod(mExpandableRow, "logExpansionEvent", true /* userAction */, wasExpanded);
            return;
        }
        invoke(setUserExpanded, mExpandableRow, userExpanded);
    }

    public void setDismissed(boolean dismissed, boolean fromAccessibility) {
        mDismissed = dismissed;
        mGroupParentWhenDismissed = mNotificationParent;
        mRefocusOnDismiss = fromAccessibility;
        mChildAfterViewWhenDismissed = null;
        if (isChildInGroup()) {
            List<FrameLayout> notificationChildren =
                    invoke(Methods.SystemUI.ExpandableNotificationRow.getNotificationChildren, mNotificationParent);
            int i = notificationChildren.indexOf(this);
            if (i != -1 && i < notificationChildren.size() - 1) {
                mChildAfterViewWhenDismissed = notificationChildren.get(i + 1);
            }
        }
    }

    public boolean isDismissed() {
        return mDismissed;
    }

    public boolean keepInParent() {
        return mKeepInParent;
    }

    public void setKeepInParent(boolean keepInParent) {
        mKeepInParent = keepInParent;
    }

    public boolean isRemoved() {
        return mRemoved;
    }

    public void setRemoved() {
        mRemoved = true;

        mPrivateHelper.setRemoved();
    }

    public void setContentBackground(int customBackgroundColor, boolean animate,
                                     View notificationContentView) {
        if (getShowingLayout() == notificationContentView) {
            mOutlineHelper.setTintColor(customBackgroundColor, animate);
        }
    }

    public View getShowingLayout() {
        return (View) invoke(getShowingLayout, mExpandableRow);
    }

    public NotificationContentHelper getShowingHelper() {
        return NotificationContentHelper.getInstance(getShowingLayout());
    }

    public void closeRemoteInput() {
        mPrivateHelper.closeRemoteInput();
        mPublicHelper.closeRemoteInput();
    }

    /**
     * Set by how much the single line view should be indented.
     */
    public void setSingleLineWidthIndention(int indention) {
        mPrivateHelper.setSingleLineWidthIndention(indention);
    }

    public int getMinHeight() {
        boolean isHeadsUp = get(mIsHeadsUp, mExpandableRow);
        if (isHeadsUp && NotificationsStuff.isTrackingHeadsUp(mHeadsUpManager)) {
            return getPinnedHeadsUpHeight(false /* atLeastMinHeight */);
        } else if (mIsSummaryWithChildren && !isGroupExpanded() && !getBoolean(mShowingPublic, mExpandableRow)) {
            return mChildrenContainerHelper.getMinHeight();
        } else if (isHeadsUp) {
            return getInt(mHeadsUpHeight, mExpandableRow);
        }
        return getShowingHelper().getMinHeight();
    }

    public int getCollapsedHeight() {
        if (mIsSummaryWithChildren && !getBoolean(mShowingPublic, mExpandableRow)) {
            return mChildrenContainerHelper.getCollapsedHeight();
        }
        return getMinHeight();
    }

    public static boolean isExpanded(Object row, boolean allowOnKeyguard) {
        return (!isOnKeyguard(row) || allowOnKeyguard)
                && (!getBoolean(Fields.SystemUI.ExpandableNotificationRow.mHasUserChangedExpansion, row)
                && (getBoolean(Fields.SystemUI.ExpandableNotificationRow.mIsSystemExpanded, row) || getBoolean(Fields.SystemUI.ExpandableNotificationRow.mIsSystemChildExpanded, row))
                || getBoolean(Fields.SystemUI.ExpandableNotificationRow.mUserExpanded, row));
    }

    public interface OnExpandClickListener {
        void onExpandClicked(Object clickedEntry, boolean nowExpanded);
    }
}
