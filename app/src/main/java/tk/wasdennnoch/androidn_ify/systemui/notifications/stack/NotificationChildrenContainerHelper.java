package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.app.Notification;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.CrossFadeHelper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.HybridGroupManager;
import tk.wasdennnoch.androidn_ify.extracted.systemui.HybridNotificationView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationExpandButton;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationViewWrapper;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ViewInvertHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableNotificationRowHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableOutlineViewHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationContentHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHeaderView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.RelativeDateTimeView;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.NotificationHeaderUtil;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationChildrenContainer.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ViewState.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.StackViewState.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationChildrenContainer.*;

import static android.view.View.GONE;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.VISIBLE;

public class NotificationChildrenContainerHelper {

    private static final int NUMBER_OF_CHILDREN_WHEN_COLLAPSED = 2;
    private static final int NUMBER_OF_CHILDREN_WHEN_SYSTEM_EXPANDED = 5;
    private static final int NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED = 8;

    private ResourceUtils res;

    private ViewGroup mNotificationChildrenContainer;

    private HybridGroupManager mHybridGroupManager;
    private int mChildPadding;
    private int mDividerHeight;
    private int mMaxNotificationHeight;
    private int mNotificationHeaderMargin;
    private int mNotificatonTopPadding;
    private float mCollapsedBottompadding;
    private ViewInvertHelper mOverflowInvertHelper;
    private boolean mChildrenExpanded;
    private FrameLayout mNotificationParent;
    private ExpandableNotificationRowHelper mParentHelper;
    private TextView mOverflowNumber;
    private Object mGroupOverFlowState;
    private int mRealHeight;
    private boolean mUserLocked;
    private int mActualHeight;
    private boolean mNeverAppliedGroupState;
    private int mHeaderHeight;

    private NotificationHeaderView mNotificationHeader;
    private NotificationViewWrapper mNotificationHeaderWrapper;
    private NotificationHeaderUtil mHeaderUtil;
    private Object mHeaderViewState;

    private NotificationChildrenContainerHelper(Object obj) {
        XposedHelpers.setAdditionalInstanceField(obj, "mChildrenContainerHelper", this);
        mNotificationChildrenContainer = (ViewGroup) obj;
        res = ResourceUtils.getInstance(mNotificationChildrenContainer.getContext());
    }

    public static NotificationChildrenContainerHelper getInstance(Object obj) {
        NotificationChildrenContainerHelper helper = (NotificationChildrenContainerHelper) XposedHelpers.getAdditionalInstanceField(obj, "mChildrenContainerHelper");
        return helper != null ? helper : new NotificationChildrenContainerHelper(obj);
    }

    public void onConstructor() {
        initDimens();
        mHybridGroupManager = new HybridGroupManager(mNotificationChildrenContainer.getContext(), mNotificationChildrenContainer);
    }

    private void initDimens() {
        mChildPadding = res.getDimensionPixelSize(
                R.dimen.notification_children_padding);
        mDividerHeight = Math.max(1, res.getDimensionPixelSize(
                R.dimen.notification_divider_height));
        mHeaderHeight = res.getDimensionPixelSize(R.dimen.notification_header_height);
        mMaxNotificationHeight = res.getDimensionPixelSize(
                R.dimen.notification_max_height);
        mNotificationHeaderMargin = res.getDimensionPixelSize(
                R.dimen.notification_content_margin_top);
        mNotificatonTopPadding = res.getDimensionPixelSize(
                R.dimen.notification_children_container_top_padding);
        mCollapsedBottompadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_bottom);
    }

    public void onLayout(boolean changed, int l, int t, int r, int b) {
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        List<View> children = get(mChildren, mNotificationChildrenContainer);
        int childCount = Math.min(children.size(), NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED);

        for (int i = 0; i < childCount; i++) {
            View child = children.get(i);
            // We need to layout all children even the GONE ones, such that the heights are
            // calculated correctly as they are used to calculate how many we can fit on the screen
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            dividers.get(i).layout(0, 0, mNotificationChildrenContainer.getWidth(), mDividerHeight);
        }
        if (mOverflowNumber != null) {
            boolean isRtl = mNotificationChildrenContainer.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            int left = (isRtl ? 0 : mNotificationChildrenContainer.getWidth() - mOverflowNumber.getMeasuredWidth());
            int right = left + mOverflowNumber.getMeasuredWidth();
            mOverflowNumber.layout(left, 0, right, mOverflowNumber.getMeasuredHeight());
        }
        if (mNotificationHeader != null) {
            mNotificationHeader.layout(0, 0, mNotificationHeader.getMeasuredWidth(),
                    mNotificationHeader.getMeasuredHeight());
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int ownMaxHeight = mMaxNotificationHeight;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == MeasureSpec.EXACTLY;
        boolean isHeightLimited = heightMode == MeasureSpec.AT_MOST;
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (hasFixedHeight || isHeightLimited) {
            ownMaxHeight = Math.min(ownMaxHeight, size);
        }
        int newHeightSpec = MeasureSpec.makeMeasureSpec(ownMaxHeight, MeasureSpec.AT_MOST);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (mOverflowNumber != null) {
            mOverflowNumber.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    newHeightSpec);
        }
        int dividerHeightSpec = MeasureSpec.makeMeasureSpec(mDividerHeight, MeasureSpec.EXACTLY);
        int height = mNotificationHeaderMargin + mNotificatonTopPadding;
        int childCount = Math.min(children.size(), NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED);
        int collapsedChildren = getMaxAllowedVisibleChildren(true /* likeCollapsed */);
        int overflowIndex = childCount > collapsedChildren ? collapsedChildren - 1 : -1;

        for (int i = 0; i < childCount; i++) {
            ViewGroup child = children.get(i);
            // We need to measure all children even the GONE ones, such that the heights are
            // calculated correctly as they are used to calculate how many we can fit on the screen.
            boolean isOverflow = i == overflowIndex;
            ExpandableNotificationRowHelper.getInstance(child).setSingleLineWidthIndention(isOverflow && mOverflowNumber != null
                    ? mOverflowNumber.getMeasuredWidth()
                    : 0);
            child.measure(widthMeasureSpec, newHeightSpec);

            // layout the divider
            View divider = dividers.get(i);
            divider.measure(widthMeasureSpec, dividerHeightSpec);
            if (child.getVisibility() != GONE) {
                height += child.getMeasuredHeight() + mDividerHeight;

            }
        }
        mRealHeight = height;
        if (heightMode != MeasureSpec.UNSPECIFIED) {
            height = Math.min(height, size);
        }
        if (mNotificationHeader != null) {
            int headerHeightSpec = MeasureSpec.makeMeasureSpec(mHeaderHeight, MeasureSpec.EXACTLY);
            mNotificationHeader.measure(widthMeasureSpec, headerHeightSpec);
        }
        invoke(Methods.Android.View.setMeasuredDimension, mNotificationChildrenContainer, width, height);
    }

    /**
     * Add a child notification to this view.
     *
     * @param row the row to add
     * @param childIndex the index to add it at, if -1 it will be added at the end
     */
    public void addNotification(FrameLayout row, int childIndex) {
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int newIndex = childIndex < 0 ? children.size() : childIndex;
        children.add(newIndex, row);
        mNotificationChildrenContainer.addView(row);
        invoke(ExpandableNotificationRow.setUserLocked, row, mUserLocked);

        View divider = invoke(inflateDivider, mNotificationChildrenContainer);
        mNotificationChildrenContainer.addView(divider);
        dividers.add(newIndex, divider);

        updateGroupOverflow();
    }

    public void removeNotification(FrameLayout row) {
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);

        int childIndex = children.indexOf(row);
        children.remove(row);
        mNotificationChildrenContainer.removeView(row);

        final View divider = dividers.remove(childIndex);
        mNotificationChildrenContainer.removeView(divider);
        mNotificationChildrenContainer.getOverlay().add(divider);
        CrossFadeHelper.fadeOut(divider, new Runnable() {
            @Override
            public void run() {
                mNotificationChildrenContainer.getOverlay().remove(divider);
            }
        });

        set(Fields.SystemUI.ExpandableNotificationRow.mIsSystemChildExpanded, row, false);
        invoke(ExpandableNotificationRow.setUserLocked, row, false);
        updateGroupOverflow();
        if (!rowHelper.isRemoved()) {
            mHeaderUtil.restoreNotificationHeader(row);
        }
    }

    /**
     * @return The number of notification children in the container.
     */
    public int getNotificationChildCount() {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        return children.size();
    }

    public void recreateNotificationHeader(View.OnClickListener listener) {
        final Notification.Builder builder = NotificationHooks.recoverBuilder(mNotificationChildrenContainer.getContext(),
                ((StatusBarNotification) invoke(getStatusBarNotification, mNotificationParent)).getNotification());
        final RemoteViews header = NotificationHooks.makeNotificationHeader(builder);
        if (mNotificationHeader == null) {
            mNotificationHeader = (NotificationHeaderView) apply(header, mNotificationChildrenContainer.getContext(), mNotificationChildrenContainer, null);
            final View expandButton = mNotificationHeader.findViewById(
                    R.id.expand_button);
            expandButton.setVisibility(VISIBLE);
            mNotificationHeader.setOnClickListener(listener);
            mNotificationHeaderWrapper = NotificationViewWrapper.wrap(mNotificationChildrenContainer.getContext(),
                    mNotificationHeader, mNotificationParent);
            mNotificationChildrenContainer.addView(mNotificationHeader, 0);
            mNotificationChildrenContainer.invalidate();
        } else {
            header.reapply(mNotificationChildrenContainer.getContext(), mNotificationHeader);
            mNotificationHeaderWrapper.onContentUpdated(mNotificationParent);
        }
    }

    public View apply(RemoteViews remoteViews, Context context, ViewGroup parent, Object handler) {
        Method getRemoteViewsToApply = XposedHelpers.findMethodExact(RemoteViews.class, "getRemoteViewsToApply", Context.class);
        Method performApply = XposedHelpers.findMethodExact(RemoteViews.class, "performApply", View.class, ViewGroup.class, Classes.Android.remoteViewsOnClickHandler);
        RemoteViews rvToApply = invoke(getRemoteViewsToApply, remoteViews, (Context) context);

        View result = inflateView(context, rvToApply, parent);

        invoke(performApply, rvToApply, result, parent, handler);

        return result;
    }

    private View inflateView(Context context, RemoteViews rv, ViewGroup parent) {
        LayoutInflater inflater = getLayoutInflater(context);

        View v = inflater.inflate(R.layout.notification_template_header, parent, false);
        invoke(Methods.Android.View.setTagInternal, v, context.getResources().getIdentifier("widget_frame", "id", XposedHook.PACKAGE_ANDROID), rv.getLayoutId());
        return v;
    }

    private LayoutInflater getLayoutInflater(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(ResourceUtils.createOwnContext(context));
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (NotificationHeaderView.class.getSimpleName().equals(name)) {
                    return new NotificationHeaderView(context, attrs);
                } else if (NotificationExpandButton.class.getSimpleName().equals(name)) {
                    return new NotificationExpandButton(context, attrs);
                } else if (RelativeDateTimeView.class.getSimpleName().equals(name)){
                    return new RelativeDateTimeView(context, attrs);
                } else {
                    return null;
                }
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }
        });
        return inflater;
    }

    public void updateChildrenHeaderAppearance() {
        mHeaderUtil.updateChildrenHeaderAppearance();
    }

    public void updateGroupOverflow() {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int childCount = children.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true /* likeCollapsed */);
        if (childCount > maxAllowedVisibleChildren) {
            mOverflowNumber = mHybridGroupManager.bindOverflowNumber(
                    mOverflowNumber, childCount - maxAllowedVisibleChildren);
            if (mOverflowInvertHelper == null) {
                mOverflowInvertHelper = new ViewInvertHelper(mOverflowNumber,
                        700 /*NotificationPanelView.DOZE_ANIMATION_DURATION*/);
            }
            if (mGroupOverFlowState == null) {
                mGroupOverFlowState = newInstance(ViewState.constructor);
                mNeverAppliedGroupState = true;
            }
        } else if (mOverflowNumber != null) {
            mNotificationChildrenContainer.removeView(mOverflowNumber);
            if (mNotificationChildrenContainer.isShown()) {
                final View removedOverflowNumber = mOverflowNumber;
                invoke(Methods.Android.ViewGroup.addTransientView, mNotificationChildrenContainer, removedOverflowNumber,
                        invoke(Methods.Android.ViewGroup.getTransientViewCount, mNotificationChildrenContainer));
                CrossFadeHelper.fadeOut(removedOverflowNumber, new Runnable() {
                    @Override
                    public void run() {
                        invoke(Methods.Android.ViewGroup.removeTransientView, mNotificationChildrenContainer, removedOverflowNumber);
                    }
                });
            }
            mOverflowNumber = null;
            mOverflowInvertHelper = null;
            mGroupOverFlowState = null;
        }
    }

    private void updateExpansionStates() {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        if (mChildrenExpanded || mUserLocked) {
            // we don't modify it the group is expanded or if we are expanding it
            return;
        }
        int size = children.size();
        for (int i = 0; i < size; i++) {
            Object child = children.get(i);
            set(Fields.SystemUI.ExpandableNotificationRow.mIsSystemChildExpanded, child, i == 0 && size == 1);
        }
    }

    /**
     *
     * @return the intrinsic size of this children container, i.e the natural fully expanded state
     */
    public int getIntrinsicHeight() {
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren();
        return getIntrinsicHeight(maxAllowedVisibleChildren);
    }

    /**
     * @return the intrinsic height with a number of children given
     *         in @param maxAllowedVisibleChildren
     */
    private int getIntrinsicHeight(float maxAllowedVisibleChildren) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int intrinsicHeight = mNotificationHeaderMargin;
        int visibleChildren = 0;
        int childCount = children.size();
        boolean firstChild = true;
        float expandFactor = 0;
        if (mUserLocked) {
            expandFactor = getGroupExpandFraction();
        }
        for (int i = 0; i < childCount; i++) {
            if (visibleChildren >= maxAllowedVisibleChildren) {
                break;

            }
            if (!firstChild) {
                if (mUserLocked) {
                    intrinsicHeight += NotificationUtils.interpolate(mChildPadding, mDividerHeight,
                            expandFactor);
                } else {
                    intrinsicHeight += mChildrenExpanded ? mDividerHeight : mChildPadding;
                }
            } else {
                if (mUserLocked) {
                    intrinsicHeight += NotificationUtils.interpolate(
                            0,
                            mNotificatonTopPadding + mDividerHeight,
                            expandFactor);
                } else {
                    intrinsicHeight += mChildrenExpanded
                            ? mNotificatonTopPadding + mDividerHeight
                            : 0;
                }
                firstChild = false;
            }
            ViewGroup child = children.get(i);
            intrinsicHeight += (int) invoke(ExpandableNotificationRow.getIntrinsicHeight, child);
            visibleChildren++;
        }
        if (mUserLocked) {
            intrinsicHeight += NotificationUtils.interpolate(mCollapsedBottompadding, 0.0f,
                    expandFactor);
        } else if (!mChildrenExpanded) {
            intrinsicHeight += mCollapsedBottompadding;
        }
        return intrinsicHeight;
    }

    /**
     * Update the state of all its children based on a linear layout algorithm.
     *
     * @param resultState the state to update
     * @param parentState the state of the parent
     */
    public void getState(Object resultState, Object parentState) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int childCount = children.size();
        int yPosition = mNotificationHeaderMargin;
        boolean firstChild = true;
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren();
        int lastVisibleIndex = maxAllowedVisibleChildren - 1;
        int firstOverflowIndex = lastVisibleIndex + 1;
        float expandFactor = 0;
        if (mUserLocked) {
            expandFactor = getGroupExpandFraction();
            firstOverflowIndex = getMaxAllowedVisibleChildren(true /* likeCollapsed */);
        }

        boolean childrenExpanded = !mParentHelper.isGroupExpansionChanging()
                && mChildrenExpanded;
        int parentHeight = getInt(height, parentState);
        for (int i = 0; i < childCount; i++) {
            ViewGroup child = children.get(i);

            if (!firstChild) {
                if (mUserLocked) {
                    yPosition += NotificationUtils.interpolate(mChildPadding, mDividerHeight,
                            expandFactor);
                } else {
                    yPosition += mChildrenExpanded ? mDividerHeight : mChildPadding;
                }
            } else {
                if (mUserLocked) {
                    yPosition += NotificationUtils.interpolate(
                            0,
                            mNotificatonTopPadding + mDividerHeight,
                            expandFactor);
                } else {
                    yPosition += mChildrenExpanded ? mNotificatonTopPadding + mDividerHeight : 0;
                }
                firstChild = false;
            }

            Object childState = invoke(StackScrollState.getViewStateForView, resultState, child);
            int intrinsicHeight = invoke(ExpandableNotificationRow.getIntrinsicHeight, child);
            if (childrenExpanded) {
                // When a group is expanded and moving into bottom stack, the bottom visible child
                // adjusts its height to move into it. Children after it are hidden.
                if (updateChildStateForExpandedGroup(child, parentHeight, childState, yPosition)) {
                    // Clipping might be deactivated if the view is transforming, however, clipping
                    // the child into the bottom stack should take precedent over this.
//                    childState.isBottomClipped = true; //TODO: see what to do with this
                }
            } else {
//                childState.hidden = false; //TODO: see what to do with this
                set(height, childState, intrinsicHeight);
//                childState.isBottomClipped = false; //TODO: see what to do with this
            }
            set(yTranslation, childState, yPosition);
            // When the group is expanded, the children cast the shadows rather than the parent
            // so use the parent's elevation here.
            set(zTranslation, childState, childrenExpanded
                    ? mNotificationParent.getTranslationZ()
                    : 0);
            set(dimmed, childState, get(dimmed, parentState));
            set(dark, childState, get(dark, parentState));
            set(hideSensitive, childState, get(hideSensitive, parentState));
            set(belowSpeedBump, childState, get(belowSpeedBump, parentState));
            set(scale, childState, get(scale, parentState));
            set(clipTopAmount, childState, 0);
            set(alpha, childState, 0);
            if (i < firstOverflowIndex) {
                set(alpha, childState, 1);
            } else if (expandFactor == 1.0f && i <= lastVisibleIndex) {
                set(alpha, childState, (mActualHeight - getFloat(yTranslation, childState) / getInt(height, childState)));
                set(alpha, childState, Math.max(0.0f, Math.min(1.0f, getFloat(alpha, childState))));
            }
            set(location, childState, get(location, parentState));
            yPosition += intrinsicHeight;
        }
        if (mOverflowNumber != null) {
            ViewGroup overflowView = children.get(Math.min(
                    getMaxAllowedVisibleChildren(true /* likeCollapsed */), childCount) - 1);
            invoke(Methods.SystemUI.ViewState.copyFrom, mGroupOverFlowState, invoke(StackScrollState.getViewStateForView, resultState, overflowView));
            if (!mChildrenExpanded) {
                if (mUserLocked) {
                    HybridNotificationView singleLineView = ExpandableNotificationRowHelper.getInstance(overflowView).getSingleLineView();
                    View mirrorView = singleLineView.getTextView();
                    if (mirrorView.getVisibility() == GONE) {
                        mirrorView = singleLineView.getTitleView();
                    }
                    if (mirrorView.getVisibility() == GONE) {
                        mirrorView = singleLineView;
                    }
                    set(yTranslation, mGroupOverFlowState, getFloat(yTranslation, mGroupOverFlowState) + NotificationUtils.getRelativeYOffset(
                            mirrorView, overflowView));
                    set(alpha, mGroupOverFlowState, mirrorView.getAlpha());
                }
            } else {
                set(yTranslation, mGroupOverFlowState, getFloat(yTranslation, mGroupOverFlowState) + mNotificationHeaderMargin);
                set(alpha, mGroupOverFlowState, 0.0f);
            }
        }
        if (mNotificationHeader != null) {
            if (mHeaderViewState == null) {
                mHeaderViewState = newInstance(Methods.SystemUI.ViewState.constructor);
            }
            invoke(Methods.SystemUI.ViewState.initFrom, mHeaderViewState, mNotificationHeader);
            set(Fields.SystemUI.ViewState.zTranslation, mHeaderViewState, childrenExpanded
                    ? mNotificationParent.getTranslationZ()
                    : 0);
        }
    }

    /**
     * When moving into the bottom stack, the bottom visible child in an expanded group adjusts its
     * height, children in the group after this are gone.
     *
     * @param child the child who's height to adjust.
     * @param parentHeight the height of the parent.
     * @param childState the state to update.
     * @param yPosition the yPosition of the view.
     * @return true if children after this one should be hidden.
     */
    private boolean updateChildStateForExpandedGroup(ViewGroup child,
                                                     int parentHeight, Object childState, int yPosition) {
        final int top = yPosition + (int) invoke(ExpandableView.getClipTopAmount, child);
        final int intrinsicHeight = invoke(ExpandableNotificationRow.getIntrinsicHeight, child);
        final int bottom = top + intrinsicHeight;
        int newHeight = intrinsicHeight;
        if (bottom >= parentHeight) {
            // Child is either clipped or gone
            newHeight = Math.max((parentHeight - top), 0);
        }
        //childState.hidden = newHeight == 0; //TODO: see what to do with this
        set(height, childState, newHeight);
        return getInt(height, childState) != intrinsicHeight/* && !childState.hidden*/;
    }

    private int getMaxAllowedVisibleChildren() {
        return getMaxAllowedVisibleChildren(false /* likeCollapsed */);
    }

    private int getMaxAllowedVisibleChildren(boolean likeCollapsed) {
        if (!likeCollapsed && (mChildrenExpanded || (boolean) invoke(ExpandableNotificationRow.isUserLocked, mNotificationParent))) {
            return NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED;
        }
        if (!mParentHelper.isOnKeyguard()
                && ((boolean) invoke(ExpandableNotificationRow.isExpanded, mNotificationParent) || (boolean) invoke(ExpandableNotificationRow.isHeadsUp, mNotificationParent))) {
            return NUMBER_OF_CHILDREN_WHEN_SYSTEM_EXPANDED;
        }
        return NUMBER_OF_CHILDREN_WHEN_COLLAPSED;
    }

    public void applyState(Object state) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        int childCount = children.size();
        Object tmpState = newInstance(Methods.SystemUI.ViewState.constructor);
        float expandFraction = 0.0f;
        if (mUserLocked) {
            expandFraction = getGroupExpandFraction();
        }
        final boolean dividersVisible = mUserLocked
                || mParentHelper.isGroupExpansionChanging();
        for (int i = 0; i < childCount; i++) {
            ViewGroup child = children.get(i);
            Object viewState = invoke(StackScrollState.getViewStateForView, state, child);
            if (viewState == null)
                continue;

            invoke(StackScrollState.applyState, state, child, viewState);

            // layout the divider
            View divider = dividers.get(i);
            invoke(Methods.SystemUI.ViewState.initFrom, tmpState, divider);
            set(yTranslation, tmpState, getFloat(yTranslation, viewState) - mDividerHeight);
            float alpha = mChildrenExpanded && getFloat(Fields.SystemUI.ViewState.alpha, viewState) != 0 ? 0.5f : 0;
            if (mUserLocked && getFloat(Fields.SystemUI.ViewState.alpha, viewState)!= 0) {
                alpha = NotificationUtils.interpolate(0, 0.5f,
                        Math.min(getFloat(Fields.SystemUI.ViewState.alpha, viewState), expandFraction));
            }
//            tmpState.hidden = !dividersVisible; //TODO: see what to do with this
            set(Fields.SystemUI.ViewState.alpha, tmpState, alpha);
            invoke(StackScrollState.applyViewState, state, divider, tmpState);
            // There is no fake shadow to be drawn on the children
            ExpandableOutlineViewHelper.getInstance(child).setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (mOverflowNumber != null) {
            invoke(StackScrollState.applyViewState, state, mOverflowNumber, mGroupOverFlowState);
            mNeverAppliedGroupState = false;
        }
        if (mNotificationHeader != null) {
            invoke(StackScrollState.applyViewState, state, mNotificationHeader, mHeaderViewState);
        }
    }

    public void startAnimationToState(Object state, Object stateAnimator,
                                      long baseDelay, long duration) {
        List<View> dividers = get(mDividers, mNotificationChildrenContainer);
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int childCount = children.size();
        Object tmpState = newInstance(Methods.SystemUI.ViewState.constructor);
        float expandFraction = getGroupExpandFraction();
        final boolean dividersVisible = mUserLocked
                || mParentHelper.isGroupExpansionChanging();
        for (int i = childCount - 1; i >= 0; i--) {
            ViewGroup child = children.get(i);
            Object viewState = invoke(StackScrollState.getViewStateForView, state, child);
            if (viewState == null)
                return;
            invoke(StackStateAnimator.startStackAnimations, stateAnimator, child, viewState, state, -1, baseDelay);

            // layout the divider
            View divider = dividers.get(i);
            invoke(Methods.SystemUI.ViewState.initFrom, tmpState, divider);
            set(yTranslation, tmpState, getFloat(yTranslation, viewState) - mDividerHeight);
            float alpha = mChildrenExpanded && getFloat(Fields.SystemUI.ViewState.alpha, viewState) != 0 ? 0.5f : 0;
            if (mUserLocked && getFloat(Fields.SystemUI.ViewState.alpha, viewState) != 0) {
                alpha = NotificationUtils.interpolate(0, 0.5f,
                        Math.min(getFloat(Fields.SystemUI.ViewState.alpha, viewState), expandFraction));
            }
            //tmpState.hidden = !dividersVisible;
            set(Fields.SystemUI.ViewState.alpha, tmpState, alpha);
            invoke(StackStateAnimator.startViewAnimations, stateAnimator, divider, tmpState, baseDelay, duration);
            // There is no fake shadow to be drawn on the children
            ExpandableOutlineViewHelper.getInstance(child).setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (mOverflowNumber != null) {
            if (mNeverAppliedGroupState) {
                float alpha = getFloat(Fields.SystemUI.ViewState.alpha, mGroupOverFlowState);
                set(Fields.SystemUI.ViewState.alpha, mGroupOverFlowState, 0);
                invoke(StackScrollState.applyViewState, state, mOverflowNumber, mGroupOverFlowState);
                set(Fields.SystemUI.ViewState.alpha, mGroupOverFlowState, alpha);
                mNeverAppliedGroupState = false;
            }
            invoke(StackStateAnimator.startViewAnimations, stateAnimator, mOverflowNumber, mGroupOverFlowState,
                    baseDelay, duration);
        }
        if (mNotificationHeader != null) {
            invoke(StackScrollState.applyViewState, state, mNotificationHeader, mHeaderViewState);
        }
    }

    public void setChildrenExpanded(boolean childrenExpanded) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        mChildrenExpanded = childrenExpanded;
        updateExpansionStates();
        if (mNotificationHeader != null) {
            mNotificationHeader.setExpanded(childrenExpanded);
        }
        final int count = children.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ViewGroup child = children.get(childIdx);
            invoke(ExpandableNotificationRow.setChildrenExpanded, child, childrenExpanded, false);
        }
    }

    public void setNotificationParent(FrameLayout parent) {
        mNotificationParent = parent;
        mParentHelper = ExpandableNotificationRowHelper.getInstance(mNotificationParent);
        mHeaderUtil = new NotificationHeaderUtil(mNotificationParent);
    }

    public FrameLayout getNotificationParent() {
        return mNotificationParent;
    }

    public NotificationHeaderView getHeaderView() {
        return mNotificationHeader;
    }

    public void updateHeaderVisibility(int visibility) {
        if (mNotificationHeader != null) {
            mNotificationHeader.setVisibility(visibility);
        }
    }

    /**
     * Called when a groups expansion changes to adjust the background of the header view.
     *
     * @param expanded whether the group is expanded.
     */
    public void updateHeaderForExpansion(boolean expanded) {
        if (mNotificationHeader != null) {
            if (expanded) {
                ColorDrawable cd = new ColorDrawable();
                cd.setColor(ExpandableOutlineViewHelper.getInstance(mNotificationParent).calculateBgColor());
                mNotificationHeader.setHeaderBackgroundDrawable(cd);
            } else {
                mNotificationHeader.setHeaderBackgroundDrawable(null);
            }
        }
    }

    public int getMaxContentHeight() {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int maxContentHeight = mNotificationHeaderMargin + mNotificatonTopPadding;
        int visibleChildren = 0;
        int childCount = children.size();
        for (int i = 0; i < childCount; i++) {
            if (visibleChildren >= NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED) {
                break;
            }
            ViewGroup child = children.get(i);
            NotificationContentHelper showingLayoutHelper = NotificationContentHelper.getInstance(invoke(ExpandableNotificationRow.getShowingLayout, child));
            float childHeight = ExpandableNotificationRowHelper.isExpanded(child, true /* allowOnKeyguard */)
                    ? (int) invoke(ExpandableNotificationRow.getMaxExpandHeight, child)
                    : showingLayoutHelper.getMinHeight(true /* likeGroupExpanded */);
            maxContentHeight += childHeight;
            visibleChildren++;
        }
        if (visibleChildren > 0) {
            maxContentHeight += visibleChildren * mDividerHeight;
        }
        return maxContentHeight;
    }

    public void setActualHeight(int actualHeight) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        if (!mUserLocked) {
            return;
        }
        mActualHeight = actualHeight;
        float fraction = getGroupExpandFraction();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true /* forceCollapsed */);
        int childCount = children.size();
        for (int i = 0; i < childCount; i++) {
            ViewGroup child = children.get(i);
            NotificationContentHelper showingLayoutHelper = NotificationContentHelper.getInstance(invoke(ExpandableNotificationRow.getShowingLayout, child));
            float childHeight = ExpandableNotificationRowHelper.isExpanded(child, true /* allowOnKeyguard */)
                    ? (int) invoke(ExpandableNotificationRow.getMaxExpandHeight, child)
                    : showingLayoutHelper.getMinHeight(true /* likeGroupExpanded */);
            if (i < maxAllowedVisibleChildren) {
                float singleLineHeight = showingLayoutHelper.getMinHeight(
                        false /* likeGroupExpanded */);
                invoke(ExpandableView.setActualHeight, child, (int) NotificationUtils.interpolate(singleLineHeight,
                        childHeight, fraction), false);
            } else {
                invoke(ExpandableView.setActualHeight, child, (int) childHeight, false);
            }
        }
    }

    public float getGroupExpandFraction() {
        int visibleChildrenExpandedHeight = getVisibleChildrenExpandHeight();
        int minExpandHeight = getCollapsedHeight();
        float factor = (mActualHeight - minExpandHeight)
                / (float) (visibleChildrenExpandedHeight - minExpandHeight);
        return Math.max(0.0f, Math.min(1.0f, factor));
    }

    private int getVisibleChildrenExpandHeight() {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int intrinsicHeight = mNotificationHeaderMargin + mNotificatonTopPadding + mDividerHeight;
        int visibleChildren = 0;
        int childCount = children.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true /* forceCollapsed */);
        for (int i = 0; i < childCount; i++) {
            if (visibleChildren >= maxAllowedVisibleChildren) {
                break;
            }
            ViewGroup child = children.get(i);
            NotificationContentHelper showingLayoutHelper = NotificationContentHelper.getInstance(invoke(ExpandableNotificationRow.getShowingLayout, child));
            float childHeight = ExpandableNotificationRowHelper.isExpanded(child, true /* allowOnKeyguard */)
                    ? (int) invoke(ExpandableNotificationRow.getMaxExpandHeight, child)
                    : showingLayoutHelper.getMinHeight(true /* likeGroupExpanded */);
            intrinsicHeight += childHeight;
            visibleChildren++;
        }
        return intrinsicHeight;
    }

    public int getMinHeight() {
        return getMinHeight(NUMBER_OF_CHILDREN_WHEN_COLLAPSED);
    }

    public int getCollapsedHeight() {
        return getMinHeight(getMaxAllowedVisibleChildren(true /* forceCollapsed */));
    }

    private int getMinHeight(int maxAllowedVisibleChildren) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int minExpandHeight = mNotificationHeaderMargin;
        int visibleChildren = 0;
        boolean firstChild = true;
        int childCount = children.size();
        for (int i = 0; i < childCount; i++) {
            if (visibleChildren >= maxAllowedVisibleChildren) {
                break;
            }
            if (!firstChild) {
                minExpandHeight += mChildPadding;
            } else {
                firstChild = false;
            }
            ExpandableNotificationRowHelper childHelper = ExpandableNotificationRowHelper.getInstance(children.get(i));
            minExpandHeight += childHelper.getSingleLineView().getHeight();
            visibleChildren++;
        }
        minExpandHeight += mCollapsedBottompadding;
        return minExpandHeight;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (mOverflowNumber != null) {
            mOverflowInvertHelper.setInverted(dark, fade, delay);
        }
        mNotificationHeaderWrapper.setDark(dark, fade, delay);
    }

    public void reInflateViews(View.OnClickListener listener, StatusBarNotification notification) {
        List<View> mDividers = (List) XposedHelpers.getObjectField(mNotificationChildrenContainer, "mDividers");

        mNotificationChildrenContainer.removeView(mNotificationHeader);
        mNotificationHeader = null;
        recreateNotificationHeader(listener);
        initDimens();
        for (int i = 0; i < mDividers.size(); i++) {
            View prevDivider = mDividers.get(i);
            int index = mNotificationChildrenContainer.indexOfChild(prevDivider);
            mNotificationChildrenContainer.removeView(prevDivider);
            View divider = invoke(inflateDivider, mNotificationChildrenContainer);
            mNotificationChildrenContainer.addView(divider, index);
            mDividers.set(i, divider);
        }
        mNotificationChildrenContainer.removeView(mOverflowNumber);
        mOverflowNumber = null;
        mOverflowInvertHelper = null;
        mGroupOverFlowState = null;
        updateGroupOverflow();
    }

    public void setUserLocked(boolean userLocked) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        mUserLocked = userLocked;
        int childCount = children.size();
        for (int i = 0; i < childCount; i++) {
            ViewGroup child = children.get(i);
            invoke(ExpandableNotificationRow.setUserLocked, child, userLocked);
        }
    }

    public void onNotificationUpdated() {
        mHybridGroupManager.setOverflowNumberColor(mOverflowNumber,
                mParentHelper.getNotificationColor());
    }

    public int getPositionInLinearLayout(View childInGroup) {
        List<ViewGroup> children = get(mChildren, mNotificationChildrenContainer);
        int position = mNotificationHeaderMargin + mNotificatonTopPadding;

        for (int i = 0; i < children.size(); i++) {
            View child = children.get(i);
            boolean notGone = child.getVisibility() != View.GONE;
            if (notGone) {
                position += mDividerHeight;
            }
            if (child == childInGroup) {
                return position;
            }
            if (notGone) {
                position += (int) invoke(ExpandableNotificationRow.getIntrinsicHeight, child);
            }
        }
        return 0;
    }

    /**
     * @return The number of notification children in the container.
     */
    public static int getNotificationChildCount(Object container) {
        return ((List)get(mChildren, container)).size();
    }

}
