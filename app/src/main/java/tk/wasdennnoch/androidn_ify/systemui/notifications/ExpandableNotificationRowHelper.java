package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.Nullable;
import android.os.Build;
import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputController;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.LAYER_TYPE_NONE;
import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_ANDROID;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ExpandableNotificationRow.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationContentView.*;

public class ExpandableNotificationRowHelper {

    private FrameLayout mExpandableRow;
    private ResourceUtils res;
    public NotificationContentHelper mPrivateHelper;
    public NotificationContentHelper mPublicHelper;
    private ExpandableOutlineViewHelper mOutlineHelper;

    public static Field fieldIsHeadsUp;
    public static Field fieldTrackingHeadsUp;
    public static Field fieldHeadsUpHeight;
    public static Field fieldHeadsUpManager;

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

    public Animator mTranslateAnim;
    public ArrayList<View> mTranslateableViews;

    public int mNotificationColor;
    private boolean mIsSummaryWithChildren = false;
    private ViewStub mSettingsIconRowStub;

    private boolean mJustClicked;
    private boolean mIconAnimationRunning;
    private boolean mShowNoBackground;
    private Object mNotificationParent;
    private OnExpandClickListener mOnExpandClickListener;
    private boolean mGroupExpansionChanging;
    public boolean mExpandedWhenPinned;

    private Object mGroupManager;

    public View.OnClickListener mExpandClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /*if (!mShowingPublic && mGroupManager.isSummaryOfGroup(mStatusBarNotification)) {
                mGroupExpansionChanging = true;
                final boolean wasExpanded = mGroupManager.isGroupExpanded(mStatusBarNotification);
                boolean nowExpanded = mGroupManager.toggleGroupExpansion(mStatusBarNotification);
                mOnExpandClickListener.onExpandClicked(mEntry, nowExpanded);
                MetricsLogger.action(mExpandableRow.getContext(), MetricsEvent.ACTION_NOTIFICATION_GROUP_EXPANDER,
                        nowExpanded);
                XposedHelpers.callMethod(mExpandableRow, "logExpansionEvent", true *//*userAction*//* , wasExpanded);
            } else {*/
                if (v.isAccessibilityFocused()) {
                    mPrivateHelper.setFocusOnVisibilityChange();
                }
                boolean nowExpanded;
                if (getBoolean(NotificationsStuff.fieldIsPinned, mExpandableRow)) {
                    nowExpanded = !mExpandedWhenPinned;
                    mExpandedWhenPinned = nowExpanded;
                } else {
                    nowExpanded = !(boolean) invoke(isExpanded, mExpandableRow);
                    invoke(setUserExpanded, mExpandableRow, nowExpanded);
                }
                invoke(notifyHeightChanged, mExpandableRow, true);
                mOnExpandClickListener.onExpandClicked(mEntry, nowExpanded);
                MetricsLogger.action(mExpandableRow.getContext(), 407 /*MetricsEvent.ACTION_NOTIFICATION_EXPANDER*/,
                        nowExpanded);
            //}
        }
    };
    private boolean mForceUnlocked;
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
        mOutlineHelper = ExpandableOutlineViewHelper.getInstance(mExpandableRow);
        mOutlineHelper.setContainingHelper(this);
    }

    public static ExpandableNotificationRowHelper getInstance(Object obj) {
        ExpandableNotificationRowHelper helper = (ExpandableNotificationRowHelper) XposedHelpers.getAdditionalInstanceField(obj, "mExpandableRowHelper");
        return helper != null ? helper : new ExpandableNotificationRowHelper(obj);
    }

    public View getExpandableRow() {
        return mExpandableRow;
    }

    public static void initFields() {
        fieldIsHeadsUp = XposedHelpers.findField(ExpandableNotificationRow, "mIsHeadsUp");
        fieldHeadsUpHeight = XposedHelpers.findField(ExpandableNotificationRow, "mHeadsUpHeight");
        fieldTrackingHeadsUp = XposedHelpers.findField(HeadsUpManager, "mTrackingHeadsUp");
    }

    public ExpandableOutlineViewHelper getOutlineHelper() {
        return mOutlineHelper;
    }

    public void setGroupManager(Object groupManager) {
        mGroupManager = groupManager;
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
        int mHeadsUpHeight = XposedHelpers.getIntField(mExpandableRow, "mHeadsUpHeight");
        /*if (mIsSummaryWithChildren) {
            return mChildrenContainer.getIntrinsicHeight();
        }*/
        if(mExpandedWhenPinned) {
            return Math.max((int) invoke(getMaxExpandHeight, mExpandableRow), mHeadsUpHeight);
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
        mPrivateLayout = (FrameLayout) XposedHelpers.getObjectField(mExpandableRow, "mPrivateLayout");
        mPublicLayout = (FrameLayout) XposedHelpers.getObjectField(mExpandableRow, "mPublicLayout");
        ViewStub mExpandButtonStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mExpandButtonStub");
        View mExpandButton = (View) XposedHelpers.getObjectField(mExpandableRow, "mExpandButton");
        ViewGroup mExpandButtonContainer = (ViewGroup) XposedHelpers.getObjectField(mExpandableRow, "mExpandButtonContainer");
        //ViewStub mChildrenContainerStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mChildrenContainerStub");
        ViewStub mGutsStub = (ViewStub) XposedHelpers.getObjectField(mExpandableRow, "mGutsStub");
        View mVetoButton = (View) XposedHelpers.getObjectField(mExpandableRow, "mVetoButton");

        mPrivateHelper = NotificationContentHelper.getInstance(mPrivateLayout);
        mPublicHelper = NotificationContentHelper.getInstance(mPublicLayout);
        mPrivateHelper.setContainingNotification(mExpandableRow);
        mPublicHelper.setContainingNotification(mExpandableRow);
        mPrivateHelper.setExpandClickListener(mExpandClickListener);
        mPublicHelper.setExpandClickListener(mExpandClickListener);

        mExpandButtonStub.setOnInflateListener(null);
        mExpandableRow.removeView(mExpandButtonStub);
        //mExpandButtonContainer.setOnClickListener(null);
        /*mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                XposedHelpers.setObjectField(mExpandableRow, "mChildrenContainer", inflated);
                //mChildrenContainer.setNotificationParent(ExpandableNotificationRow.this);
                //mChildrenContainer.onNotificationUpdated();
                mTranslateableViews.add(inflated);
            }
        });*/
        mTranslateableViews = new ArrayList<>();
        for (int i = 0; i < mExpandableRow.getChildCount(); i++) {
            mTranslateableViews.add(mExpandableRow.getChildAt(i));
        }
        // Remove views that don't translate
        mTranslateableViews.remove(mVetoButton);
        mTranslateableViews.remove(mSettingsIconRowStub);
        //mTranslateableViews.remove(mChildrenContainerStub);
        mTranslateableViews.remove(mGutsStub);
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
        boolean normalChild = true/*!isChildInGroup() || isGroupExpanded()*/;
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

    public boolean isOnKeyguard() {
        return XposedHelpers.getBooleanField(mExpandableRow, "mExpansionDisabled");
    }

    public boolean isGroupExpansionChanging() {
        /*if (isChildInGroup()) {
            return mNotificationParent.isGroupExpansionChanging();
        }*/
        return mGroupExpansionChanging;
    }

    public void setGroupExpansionChanging(boolean changing) {
        mGroupExpansionChanging = changing;
    }/*

    //@Override
    public void setActualHeightAnimating(boolean animating) {
        if (mPrivateLayout != null) {
            mPrivateLayout.setContentHeightAnimating(animating);
        }
    }*/

    /*private void onChildrenCountChanged() {

        mIsSummaryWithChildren = BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS
                && mChildrenContainer != null && mChildrenContainer.getNotificationChildCount() > 0;
        if (mIsSummaryWithChildren && mChildrenContainer.getHeaderView() == null) {
            mChildrenContainer.recreateNotificationHeader(mExpandClickListener,
                    mEntry.notification);
        }
        getShowingLayout().updateBackgroundColor(false *//* animate *//*);
        mPrivateLayout.updateExpandButtons(isExpandable());
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
    }

    public void updateChildrenHeaderAppearance() {
        if (mIsSummaryWithChildren) {
            mChildrenContainer.updateChildrenHeaderAppearance();
        }
    }*/

    public void notifyHeightChanged(boolean needsAnimation) {
        getShowingHelper().requestSelectLayout(needsAnimation || (boolean) invoke(isUserLocked, mExpandableRow));
    }

    public void makeActionsVisibile() {
        invoke(setUserExpanded, mExpandableRow, true/*, true*/);
        /*if (isChildInGroup()) {
            mGroupManager.setGroupExpanded(mStatusBarNotification, true);
        }*/
        invoke(notifyHeightChanged, mExpandableRow, false /* needsAnimation */);
    }

    /*public void setChildrenExpanded(boolean expanded, boolean animate) {
        mChildrenExpanded = expanded;
        if (mChildrenContainer != null) {
            mChildrenContainer.setChildrenExpanded(expanded);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
    }

    public static void applyTint(View v, int color) {
        int alpha;
        if (color != 0) {
            alpha = COLORED_DIVIDER_ALPHA;
        } else {
            color = 0xff000000;
            alpha = DEFAULT_DIVIDER_ALPHA;
        }
        if (v.getBackground() instanceof ColorDrawable) {
            ColorDrawable background = (ColorDrawable) v.getBackground();
            background.mutate();
            background.setColor(color);
            background.setAlpha(alpha);
        }
    }*/

    protected void onAppearAnimationFinished(boolean wasAppearing) {
        //View mChildrenContainer = (View) XposedHelpers.getObjectField(mExpandableRow, "mChildrenContainer");
        if (wasAppearing) {
            // During the animation the visible view might have changed, so let's make sure all
            // alphas are reset
            /*if (mChildrenContainer != null) {
                mChildrenContainer.setAlpha(1.0f);
                mChildrenContainer.setLayerType(LAYER_TYPE_NONE, null);
            }*/
            mPrivateLayout.setAlpha(1.0f);
            mPrivateLayout.setLayerType(LAYER_TYPE_NONE, null);
            mPublicLayout.setAlpha(1.0f);
            mPublicLayout.setLayerType(LAYER_TYPE_NONE, null);
        }
    }

    public void setHeadsUpAnimatingAway(boolean running) {
        mHeadsupDisappearRunning = running;
        mPrivateHelper.setHeadsUpAnimatingAway(running);
    }

    protected void updateBackgroundTint() {
        //updateBackgroundForGroupState(); //FIXME causes problems
        /*if (mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren =
                    mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow child = notificationChildren.get(i);
                child.updateBackgroundForGroupState();
            }
        }*/
    }

    /**
     * Updates the parent and children backgrounds in a group based on the expansion state.
     */
    public void updateBackgroundForGroupState() {
        /*if (mIsSummaryWithChildren) {
            // Only when the group has finished expanding do we hide its background.
            mShowNoBackground = isGroupExpanded() && !isGroupExpansionChanging() && !isUserLocked();
            mChildrenContainer.updateHeaderForExpansion(mShowNoBackground);
            List<ExpandableNotificationRow> children = mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).updateBackgroundForGroupState();
            }
        } else if (isChildInGroup()) {
            final int childColor = getShowingLayout().getBackgroundColorForExpansionState();
            // Only show a background if the group is expanded OR if it is expanding / collapsing
            // and has a custom background color
            final boolean showBackground = isGroupExpanded()
                    || ((mNotificationParent.isGroupExpansionChanging()
                    || mNotificationParent.isUserLocked()) && childColor != 0);
            mShowNoBackground = !showBackground;
        } else {*/
            // Only children or parents ever need no background.
            mShowNoBackground = false;
        //}
        mOutlineHelper.updateOutline();
        mOutlineHelper.updateBackground();
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        mOnExpandClickListener = onExpandClickListener;
    }

    public void onNotificationUpdated(Object entry) {
        mEntry = entry;
        XposedHelpers.setObjectField(mExpandableRow, "mStatusBarNotification", XposedHelpers.getObjectField(entry, "notification"));
        mPrivateHelper.onNotificationUpdated(entry);
        mPublicHelper.onNotificationUpdated(entry);
        //mShowingPublicInitialized = false;
        //updateNotificationColor();
        /*if (mIsSummaryWithChildren) {
            mChildrenContainer.recreateNotificationHeader(mExpandClickListener, mEntry.notification);
            mChildrenContainer.onNotificationUpdated();
        }
        if (mIconAnimationRunning) {
            setIconAnimationRunning(true);
        }
        if (mNotificationParent != null) {
            mNotificationParent.updateChildrenHeaderAppearance();
        }
        onChildrenCountChanged();*/
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
        boolean beforeN = XposedHelpers.getIntField(mEntry, "targetSdk") < Build.VERSION_CODES.N;
        int minHeight = customView && beforeN && !mIsSummaryWithChildren ?
                mNotificationMinHeightLegacy : mNotificationMinHeight;
        boolean headsUpCustom = invoke(getHeadsUpChild, layoutHelper.getContentView()) != null &&
                ((View) invoke(getHeadsUpChild, layoutHelper.getContentView())).getId()
                        != res.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID);
        int headsUpheight = headsUpCustom && beforeN ? mMaxHeadsUpHeightLegacy
                : mMaxHeadsUpHeight;
        layoutHelper.setHeights(minHeight, headsUpheight, mNotificationMaxHeight, 0);
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        mOutlineHelper.setClipToActualHeight(clipToActualHeight || (boolean) invoke(isUserLocked, mExpandableRow));
        NotificationContentHelper.getInstance(getShowingLayout())
                .setClipToActualHeight(clipToActualHeight || (boolean) invoke(isUserLocked, mExpandableRow));
    }

    public void setRemoteInputController(RemoteInputController r) {
        mPrivateHelper.setRemoteInputController(r);
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

    public int getMinHeight() {
        boolean isHeadsUp = getBoolean(fieldIsHeadsUp, mExpandableRow);
        boolean trackingHeadsUp = getBoolean(fieldTrackingHeadsUp, mHeadsUpManager);
        if (isHeadsUp && trackingHeadsUp) {
            return getPinnedHeadsUpHeight(false /* atLeastMinHeight */);
        /*} else if (mIsSummaryWithChildren && !isGroupExpanded() && !mShowingPublic) {
            return mChildrenContainer.getMinHeight();*/
        } else if (isHeadsUp) {
            return getInt(fieldHeadsUpHeight, mExpandableRow);
        }
        return getShowingHelper().getMinHeight();
    }

    public int getCollapsedHeight() {
        /*if (mIsSummaryWithChildren && !mShowingPublic) {
            return mChildrenContainer.getCollapsedHeight();
        }*/
        return getMinHeight();
    }

    public interface OnExpandClickListener {
        void onExpandClicked(Object clickedEntry, boolean nowExpanded);
    }
}
