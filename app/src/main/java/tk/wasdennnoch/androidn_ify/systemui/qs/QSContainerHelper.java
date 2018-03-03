package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSFooter;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSDetail;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationPanelView.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.QSContainer.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationPanelView.*;

@SuppressWarnings("ResourceType")
public class QSContainerHelper {

    private static final String TAG = "QSContainerHelper";

    private boolean reconfigureNotifPanel = false;

    private ResourceUtils res;
    private Context mContext;
    private View mBackground;
    private ViewGroup mNotificationPanelView;
    private ViewGroup mHeader;
    private ViewGroup mQSContainer;
    private ViewGroup mQSPanel;
    private QSFooter mQSFooter;
    private View mStackScroller;
    private QSDetail mQSDetail;
    private float mQsExpansion;
    private float mFullElevation;
    private int mGutterHeight;
    private int mHeaderHeight;

    private static final int CAP_HEIGHT = 1456;
    private static final int FONT_HEIGHT = 2163;
    private View mKeyguardStatusView;
    private TextView mClockView;
    private Rect mQsBounds = new Rect();
    private boolean mIsKeyguardShowing = false;
    private boolean mHeaderAnimating;

    public QSContainerHelper(ViewGroup notificationPanelView, ViewGroup qsContainer, ViewGroup header, ViewGroup qsPanel, QSFooter qsFooter) {
        mNotificationPanelView = notificationPanelView;
        mQSContainer = qsContainer;
        mHeader = header;
        mQSPanel = qsPanel;
        mQSFooter = qsFooter;
        mQSPanel.setClipToPadding(false);
        mQSContainer.setPadding(0, 0, 0, 0);
        mQSDetail = StatusBarHeaderHooks.qsHooks.setupQsDetail(mQSPanel, mHeader, mQSFooter);

        mContext = mQSContainer.getContext();
        int bgColor = android.support.v4.graphics.ColorUtils.setAlphaComponent(ColorUtils.getColorAttr(mContext, android.R.attr.colorPrimary), (int) (0.87 * 255));
        res = ResourceUtils.getInstance(mContext);

        mQSContainer.addView(mQSDetail);
        mQSContainer.setBackgroundColor(res.getResources().getColor(R.color.qs_background_dark, mContext.getTheme()));
//        mQSContainer.setBackgroundColor(bgColor);

        mFullElevation = mQSPanel.getElevation();

        mHeaderHeight = res.getDimensionPixelSize(R.dimen.status_bar_header_height);
        mGutterHeight = res.getDimensionPixelSize(R.dimen.qs_gutter_height);

        mQSPanel.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.qs_padding_bottom));

        FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) mQSPanel.getLayoutParams();
        qsPanelLp.setMargins(0, res.getDimensionPixelSize(R.dimen.qs_margin_top), 0, res.getDimensionPixelSize(R.dimen.qs_margin_bottom));
        qsPanel.setLayoutParams(qsPanelLp);

        //TODO fix landscape behavior
        ScrollView scrollView = mNotificationPanelView.findViewById(mContext.getResources().getIdentifier("scroll_view", "id", XposedHook.PACKAGE_SYSTEMUI));
        LinearLayout linearLayout = (LinearLayout) mQSContainer.getParent();

        linearLayout.removeAllViews();
        scrollView.removeView(linearLayout);
        scrollView.addView(mQSContainer);

        scrollView.setFillViewport(false);
        ViewGroup.LayoutParams scrollViewLayoutParams = scrollView.getLayoutParams();
        scrollViewLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
        scrollView.setLayoutParams(scrollViewLayoutParams);
        scrollView.setClipChildren(false);
        scrollView.setClipToPadding(false);

        mBackground = new View(mContext);
        mBackground.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBackground.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(-view.getWidth(), -view.getHeight(), 2 * view.getWidth(), view.getHeight()); //only the bottom part of the outline should be visible
            }
        }); //workaround for the shadow not being cast if the view is transparent
        mBackground.setElevation(res.getDimensionPixelSize(R.dimen.qs_container_elevation));
        mQSContainer.addView(mBackground, 0);
        mQSDetail.setElevation(res.getDimensionPixelSize(R.dimen.qs_container_elevation));

        if (ConfigUtils.qs().reconfigure_notification_panel) {
            reconfigureNotifPanel = true;

            mNotificationPanelView.removeView(mHeader);
            mQSContainer.addView(mHeader, 1);
            mQSContainer.setClipChildren(false);
            mQSContainer.setClipToPadding(false);
        }

        setUpOnLayout();
        mStackScroller.setFocusable(false);
    }

    public void setQsExpansion(float expansion, float headerTranslation) {
        expansion = Math.max(0, expansion);
        boolean keyguardShowing = getBoolean(mKeyguardShowing, mNotificationPanelView);
        if (mIsKeyguardShowing != keyguardShowing) {
            mIsKeyguardShowing = keyguardShowing;
            if (mIsKeyguardShowing) {
                expansion = 0;
                set(mQsExpansionHeight, mNotificationPanelView, expansion);
            }
        }
        mQsExpansion = expansion;
        updateBottom();
        final float translationScaleY = expansion - 1;
        if (!mHeaderAnimating) {
            int height = mHeader.getHeight() + mGutterHeight;
            if (mIsKeyguardShowing) {
                headerTranslation = translationScaleY * height;
            }
            mQSContainer.setTranslationY(headerTranslation);
            if (!reconfigureNotifPanel)
                mHeader.setTranslationY(height);
        }
        mQSFooter.setExpansion(mIsKeyguardShowing ? 1 : expansion);
        int heightDiff = mQSPanel.getBottom() - mHeader.getBottom() + mHeader.getPaddingBottom()
                + mQSFooter.getHeight();
        mQSPanel.setTranslationY(translationScaleY * heightDiff);
        mQSDetail.setFullyExpanded(expansion == 1);

        invoke(Methods.SystemUI.StatusBarHeaderView.setExpansion, mHeader, mIsKeyguardShowing ? 1 : expansion);

        // Set bounds on the QS panel so it doesn't run over the header.
        mQsBounds.top = (int) -mQSPanel.getTranslationY();
        mQsBounds.right = mQSPanel.getWidth();
        mQsBounds.bottom = mQSPanel.getHeight();
        mQSPanel.setClipBounds(mQsBounds);
    }

    public void updateBottom() {
        int height = calculateContainerHeight();
        int gutterHeight = Math.round(mQsExpansion * mGutterHeight);
        mQSContainer.setBottom(mQSContainer.getTop() + height + gutterHeight);
        if (reconfigureNotifPanel)
            mQSDetail.setBottom(mQSContainer.getTop() + height);
        mBackground.setBottom(mQSContainer.getTop() + height);
        // Pin QS Footer to the bottom of the panel.
        mQSFooter.setTranslationY(height - mQSFooter.getHeight());
        float elevation = mQsExpansion * mFullElevation;
        mQSDetail.setElevation(elevation);
        mBackground.setElevation(elevation);
        mQSFooter.setElevation(elevation);
        mQSPanel.setElevation(elevation);
    }

    private int calculateContainerHeight() {
        int mHeightOverride = getInt(Fields.SystemUI.QSContainer.mHeightOverride, mQSContainer);
        int heightOverride = mHeightOverride != -1 ? mHeightOverride : mQSContainer.getMeasuredHeight() - mHeaderHeight;
        return (int) (mQsExpansion * heightOverride) + mHeaderHeight;
    }

    private void setUpOnLayout() {
        mStackScroller = get(mNotificationStackScroller, mNotificationPanelView);
        mKeyguardStatusView = get(Fields.SystemUI.NotificationPanelView.mKeyguardStatusView, mNotificationPanelView);
        mClockView = get(Fields.SystemUI.NotificationPanelView.mClockView, mNotificationPanelView);
    }

    public void notificationPanelViewOnLayout(XC_MethodHook.MethodHookParam param) {
        int left = (int) param.args[1], top = (int) param.args[2], right = (int) param.args[3], bottom = (int) param.args[4];
        FrameLayout notificationPanelView = (FrameLayout) param.thisObject;

        // FrameLayout.onLayout
        invoke(layoutChildren, notificationPanelView, left, top, right, bottom, false);

        // PanelView.onLayout
        invoke(requestPanelHeightUpdate, notificationPanelView);
        set(mHasLayoutedSinceDown, notificationPanelView, true);
        if (getBoolean(mUpdateFlingOnLayout, notificationPanelView)) {
            invoke(abortAnimations, notificationPanelView);
            invoke(fling, notificationPanelView, getFloat(mUpdateFlingVelocity, notificationPanelView), true);
            set(mUpdateFlingOnLayout, notificationPanelView, false);
        }
        NotificationHooks.onPanelLaidOut(get(mStatusBar, param.thisObject));

        // NotificationPanelView.onLayout
        mKeyguardStatusView.setPivotX(notificationPanelView.getWidth() / 2);
        mKeyguardStatusView.setPivotY((FONT_HEIGHT - CAP_HEIGHT) / 2048f * mClockView.getTextSize());

        // Calculate quick setting heights.
        int oldMaxHeight = getInt(mQsMaxExpansionHeight, notificationPanelView);
        int qsMinExpansionHeight = getBoolean(mKeyguardShowing, notificationPanelView) ? 0 : mHeaderHeight;
        int qsMaxExpansionHeight = invoke(getDesiredHeight, mQSContainer);
        set(mQsMinExpansionHeight, notificationPanelView, qsMinExpansionHeight);
        set(mQsMaxExpansionHeight, notificationPanelView, qsMaxExpansionHeight);
        invoke(positionClockAndNotifications, notificationPanelView);
        boolean qsExpanded = getBoolean(mQsExpanded, notificationPanelView);
        if (qsExpanded && getBoolean(mQsFullyExpanded, notificationPanelView)) {
            set(mQsExpansionHeight, notificationPanelView, qsMaxExpansionHeight);
            invoke(requestScrollerTopPaddingUpdate, notificationPanelView, false);
            if (ConfigUtils.M) {
                invoke(requestPanelHeightUpdate, notificationPanelView);
                // Size has changed, start an animation.
                if (qsMaxExpansionHeight != oldMaxHeight) {
                    invoke(startQsSizeChangeAnimation, notificationPanelView, oldMaxHeight, qsMaxExpansionHeight);
                }
            }
        } else if (!qsExpanded) {
            setQsExpansion((float) invoke(getQsExpansionFraction, notificationPanelView),
                    (float) invoke(getHeaderTranslation, notificationPanelView));
            if (!ConfigUtils.M) {
                invoke(Methods.SystemUI.NotificationStackScrollLayout.setStackHeight, mStackScroller, invoke(getExpandedHeight, mNotificationPanelView));
                invoke(updateHeader, notificationPanelView);
            }
        }
        if (ConfigUtils.M) {
            invoke(updateStackHeight, notificationPanelView, invoke(getExpandedHeight, notificationPanelView));
            invoke(updateHeader, notificationPanelView);
        }

        if (ConfigUtils.M) {
            // If we are running a size change animation, the animation takes care of the height of
            // the container. However, if we are not animating, we always need to make the QS container
            // the desired height so when closing the QS detail, it stays smaller after the size change
            // animation is finished but the detail view is still being animated away (this animation
            // takes longer than the size change animation).
            if (get(mQsSizeChangeAnimator, notificationPanelView) == null) {
                if (qsMaxExpansionHeight != -1) qsMaxExpansionHeight -= mHeaderHeight;
                invoke(setHeightOverride, mQSContainer, qsMaxExpansionHeight);
            }
            invoke(updateMaxHeadsUpTranslation, notificationPanelView);
        }
    }

    public int getDesiredHeight() {
//        if (isCustomizing())
//            return mQSContainer.getHeight();
        if (mQSDetail.isClosingDetail()) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mQSPanel.getLayoutParams();
            int panelHeight = layoutParams.topMargin + layoutParams.bottomMargin +
                    + mQSPanel.getMeasuredHeight();
            return panelHeight + mQSContainer.getPaddingBottom() + mGutterHeight;
        } else {
            return mQSContainer.getMeasuredHeight() + mGutterHeight;
        }
    }

    public void animateHeaderSlidingIn() {
        if (!getBoolean(mQsExpanded, mNotificationPanelView)) {
            mHeaderAnimating = true;
            mQSContainer.getViewTreeObserver().addOnPreDrawListener(mStartHeaderSlidingIn);
        }
    }

    public void animateHeaderSlidingOut() {
        mHeaderAnimating = true;
        mQSContainer.animate().y(-mHeader.getHeight())
                .setStartDelay(0)
                .setDuration(360)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mQSContainer.animate().setListener(null);
                        mHeaderAnimating = false;
                        invoke(updateQsState, mNotificationPanelView);
                    }
                })
                .start();
    }

    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn
            = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            mQSContainer.getViewTreeObserver().removeOnPreDrawListener(this);
            mQSContainer.animate()
                    .translationY(0f)
                    .setDuration(448)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .setListener(mAnimateHeaderSlidingInListener)
                    .start();
            mQSContainer.setY(-mHeader.getHeight());
            return true;
        }
    };

    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener
            = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mHeaderAnimating = false;
            invoke(updateQsState, mNotificationPanelView);
        }
    };

    public void setGutterEnabled(boolean gutterEnabled) {
        if (gutterEnabled == (mGutterHeight != 0)) {
            return;
        }
        if (gutterEnabled) {
            mGutterHeight = res.getDimensionPixelSize(
                    R.dimen.qs_gutter_height);
        } else {
            mGutterHeight = 0;
        }
        updateBottom();
    }

    public void setKeyguardShowing(boolean keyguardShowing) {
        mIsKeyguardShowing = keyguardShowing;
    }

    public int getGutterHeight() {
        return mGutterHeight;
    }

    public int getBottom() {
        return mQSContainer.getBottom();
    }

    public QSDetail getQSDetail() {
        return mQSDetail;
    }

    public ViewGroup getQSContainer() { return mQSContainer; }

    public ViewGroup getQSPanel() { return mQSPanel; }

    public ViewGroup getHeader() { return mHeader; }
}
