package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.Methods;

import static tk.wasdennnoch.androidn_ify.extracted.systemui.StackStateAnimator.ANIMATION_DURATION_STANDARD;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks.BACKGROUND_ALPHA_DIMMED;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.ActivatableNotificationView;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.ExpandableNotificationRow;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ExpandableView.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.*;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;

public class ExpandableOutlineViewHelper {
    private static final String TAG = "ExpandableOutlineViewHelper";

    public FrameLayout mExpandableView;
    private ExpandableNotificationRowHelper mRowHelper;
    private FakeShadowView mFakeShadow;

    private boolean mCustomOutline;
    public boolean mClipToActualHeight = true;
    private float mOutlineAlpha = -1f;
    public float mNormalBackgroundVisibilityAmount;
    public float mDimmedBackgroundFadeInAmount = -1;
    public float mShadowAlpha = 1f;
    public float mBgAlpha = 1f;
    private int mBgTint = 0;
    public int mCurrentBackgroundTint;
    public int mTargetTint;
    public int mStartTint;
    private final Rect mOutlineRect = new Rect();
    public ValueAnimator mFadeInFromDarkAnimator;
    public ValueAnimator mBackgroundColorAnimator;

    private ViewOutlineProvider mProvider;

    public ValueAnimator.AnimatorUpdateListener mBackgroundVisibilityUpdater
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            setNormalBackgroundVisibilityAmount(getBackgroundNormal().getAlpha());
            mDimmedBackgroundFadeInAmount = getBackgroundDimmed().getAlpha();
        }
    };

    public AnimatorListenerAdapter mFadeInEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            mFadeInFromDarkAnimator = null;
            mDimmedBackgroundFadeInAmount = -1;
            updateBackground();
        }
    };
    public ValueAnimator.AnimatorUpdateListener mUpdateOutlineListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            updateOutlineAlpha();
        }
    };

    private ExpandableOutlineViewHelper(Object expandableView) {
        XposedHelpers.setAdditionalInstanceField(expandableView, "mOutlineViewHelper", this);
        mExpandableView = (FrameLayout) expandableView;
        init();
        if (ExpandableNotificationRow.isInstance(expandableView)) {
            mRowHelper = ExpandableNotificationRowHelper.getInstance(expandableView);
            mRowHelper.setOutlineHelper(this);
        }
    }

    public static ExpandableOutlineViewHelper getInstance(Object expandableView) {
        ExpandableOutlineViewHelper helper = (ExpandableOutlineViewHelper) XposedHelpers.getAdditionalInstanceField(expandableView, "mOutlineViewHelper");
        return helper != null ? helper : new ExpandableOutlineViewHelper(expandableView);
    }

    public ExpandableNotificationRowHelper getRowHelper() {
        return mRowHelper;
    }

    public void onFinishInflate() {
        if (mFakeShadow == null)
            mFakeShadow = mExpandableView.findViewById(R.id.fake_shadow);
        updateOutlineAlpha();
    }

    private void init() {
        mProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int translation = (int) getTranslation();
                if (!mCustomOutline) {
                    outline.setRect(translation,
                            getInt(SystemUI.ExpandableView.mClipTopAmount, mExpandableView),
                            mExpandableView.getWidth() + translation,
                            Math.max(getInt(SystemUI.ExpandableView.mActualHeight, mExpandableView), getInt(SystemUI.ExpandableView.mClipTopAmount, mExpandableView)));
                } else {
                    outline.setRect(mOutlineRect);
                }
                outline.setAlpha(mOutlineAlpha);
            }
        };
        mExpandableView.setOutlineProvider(mProvider);
    }

    public float getTranslation() {
        if (Classes.SystemUI.ExpandableNotificationRow.isInstance(mExpandableView) && mRowHelper != null) {
            return mRowHelper.getTranslation();
        }
        return mExpandableView.getTranslationX();
    }

    public float getOutlineAlpha() {
        return mOutlineAlpha;
    }

    public int getOutlineTranslation() {
        return mCustomOutline ? mOutlineRect.left : (int) getTranslation();
    }

    public void updateOutline() {
        if (mCustomOutline) {
            return;
        }
        boolean hasOutline = true;
        if (isChildInGroup()) {
            hasOutline = isGroupExpanded() && !isGroupExpansionChanging();
        } else if (isSummaryWithChildren()) {
            hasOutline = !isGroupExpanded() || isGroupExpansionChanging();
        }
        mExpandableView.setOutlineProvider(hasOutline ? mProvider : null);
    }

    public boolean isOutlineShowing() {
        ViewOutlineProvider op = mExpandableView.getOutlineProvider();
        return op != null;
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        mClipToActualHeight = clipToActualHeight;
        invoke(updateClipping, mExpandableView);
    }

    protected void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
        } else {
            mCustomOutline = false;
            mExpandableView.setClipToOutline(false);
            mExpandableView.invalidateOutline();
        }
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        mCustomOutline = true;
        mExpandableView.setClipToOutline(true);

        mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);

        // Outlines need to be at least 1 dp
        mOutlineRect.bottom = (int) Math.max(top, mOutlineRect.bottom);
        mOutlineRect.right = (int) Math.max(left, mOutlineRect.right);

        mExpandableView.invalidateOutline();
    }

    public float getShadowAlpha() {
        return mShadowAlpha;
    }

    public void setShadowAlpha(float shadowAlpha) {
        if (shadowAlpha != mShadowAlpha) {
            mShadowAlpha = shadowAlpha;
            updateOutlineAlpha();
        }
    }

    protected void setOutlineAlpha(float alpha) {
        if (alpha != mOutlineAlpha) {
            mOutlineAlpha = alpha;
            mExpandableView.invalidateOutline();
        }
    }

    public View getBackgroundNormal() {
        return get(SystemUI.ActivatableNotificationView.mBackgroundNormal, mExpandableView);
    }

    public View getBackgroundDimmed() {
        return get(SystemUI.ActivatableNotificationView.mBackgroundDimmed, mExpandableView);
    }

    protected void updateOutlineAlpha() {
        if (getBoolean(SystemUI.ActivatableNotificationView.mDark, mExpandableView)) {
            setOutlineAlpha(0f);
            return;
        }
        float alpha = BACKGROUND_ALPHA_DIMMED;
        alpha = (alpha + (1.0f - alpha) * mNormalBackgroundVisibilityAmount);
        alpha *= mShadowAlpha;
        if (mFadeInFromDarkAnimator != null) {
            alpha *= mFadeInFromDarkAnimator.getAnimatedFraction();
        }
        setOutlineAlpha(alpha);
    }

    public void setNormalBackgroundVisibilityAmount( float normalBackgroundVisibilityAmount) {
        mNormalBackgroundVisibilityAmount = normalBackgroundVisibilityAmount;
        updateOutlineAlpha();
    }

    /**
     * Sets the tint color of the background
     */
    public void setTintColor(int color) {
        setTintColor(color, false);
    }

    /**
     * Sets the tint color of the background
     */
    public void setTintColor(int color, boolean animated) {
        set(SystemUI.ActivatableNotificationView.mBgTint, mExpandableView, color);
        mBgTint = color;
        updateBackgroundTint(animated);
    }

    public int getBackgroundColorWithoutTint() {
        return calculateBgColor(false /* withTint */);
    }

    public int calculateBgColor() {
        return calculateBgColor(true /* withTint */);
    }

    public int calculateBgColor(boolean withTint) {
        boolean mShowingLegacyBackground = getBoolean(SystemUI.ActivatableNotificationView.mShowingLegacyBackground, mExpandableView);
        boolean mIsBelowSpeedBump = getBoolean(SystemUI.ActivatableNotificationView.mIsBelowSpeedBump, mExpandableView);
        if (withTint && mBgTint != 0) {
            return mBgTint;
        } else if (mShowingLegacyBackground) {
            return getInt(SystemUI.ActivatableNotificationView.mLegacyColor, mExpandableView);
        } else if (mIsBelowSpeedBump) {
            return getInt(SystemUI.ActivatableNotificationView.mLowPriorityColor, mExpandableView);
        } else {
            return getInt(SystemUI.ActivatableNotificationView.mNormalColor, mExpandableView);
        }
    }

    public void updateBackgroundTint(boolean animated) {
        if (mBackgroundColorAnimator != null) {
            mBackgroundColorAnimator.cancel();
        }
        int rippleColor = invoke(Methods.SystemUI.ActivatableNotificationView.getRippleColor, mExpandableView);
        invoke(Methods.SystemUI.NotificationBackgroundView.setRippleColor, getBackgroundDimmed(), rippleColor);
        invoke(Methods.SystemUI.NotificationBackgroundView.setRippleColor, getBackgroundNormal(), rippleColor);
        int color = calculateBgColor(true);
        if (!animated) {
            setBackgroundTintColor(color);
        } else if (color != mCurrentBackgroundTint) {
            mStartTint = mCurrentBackgroundTint;
            mTargetTint = color;
            mBackgroundColorAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            mBackgroundColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int newColor = NotificationUtils.interpolateColors(mStartTint, mTargetTint,
                            animation.getAnimatedFraction());
                    setBackgroundTintColor(newColor);
                }
            });
            mBackgroundColorAnimator.setDuration(ANIMATION_DURATION_STANDARD);
            mBackgroundColorAnimator.setInterpolator(Interpolators.LINEAR);
            mBackgroundColorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mBackgroundColorAnimator = null;
                }
            });
            mBackgroundColorAnimator.start();
        }
        if (getRowHelper() != null)
            getRowHelper().updateBackgroundTint();
    }

    public void setBackgroundTintColor(int color) {
        mCurrentBackgroundTint = color;
        if (color == getInt(SystemUI.ActivatableNotificationView.mNormalColor, mExpandableView)) {
            // We don't need to tint a normal notification
            color = 0;
        }
        invoke(Methods.SystemUI.NotificationBackgroundView.setTint, getBackgroundDimmed(), color);
        invoke(Methods.SystemUI.NotificationBackgroundView.setTint, getBackgroundNormal(), color);
    }

    public void resetBackgroundAlpha() {
        updateBackgroundAlpha(0f /* transformationAmount */);
    }

    public void updateBackgroundAlpha(float transformationAmount) {
        boolean isChildInGroup = isChildInGroup();
        mBgAlpha = isChildInGroup && getBoolean(SystemUI.ActivatableNotificationView.mDimmed, mExpandableView) ? transformationAmount : 1f;
        if (mDimmedBackgroundFadeInAmount != -1) {
            mBgAlpha *= mDimmedBackgroundFadeInAmount;
        }
        getBackgroundDimmed().setAlpha(mBgAlpha);
    }

    public void updateBackground() {
        invoke(Methods.SystemUI.ActivatableNotificationView.updateBackground, mExpandableView);
    }

    public void setFakeShadowIntensity(float shadowIntensity, float outlineAlpha, int shadowYEnd,
                                               int outlineTranslation) {
        if (mFakeShadow == null)
            return;
        mFakeShadow.setFakeShadowTranslationZ(shadowIntensity * (mExpandableView.getTranslationZ()
                        + FakeShadowView.SHADOW_SIBLING_TRESHOLD), outlineAlpha, shadowYEnd,
                outlineTranslation);
    }

    public int getCollapsedHeight() {
        return mRowHelper != null ? mRowHelper.getCollapsedHeight() : mExpandableView.getHeight();
    }

    public boolean isGroupExpansionChanging() {
        if (mRowHelper != null)
            return mRowHelper.isGroupExpansionChanging();
        return false;
    }

    public boolean isGroupExpanded() {
        if (mRowHelper != null)
            return mRowHelper.isGroupExpanded();
        return false;
    }

    public boolean isChildInGroup() {
        if (mRowHelper != null)
            return mRowHelper.isChildInGroup();
        return false;
    }

    public boolean isSummaryWithChildren() {
        if (mRowHelper != null)
            return mRowHelper.isSummaryWithChildren();
        return false;
    }
}
