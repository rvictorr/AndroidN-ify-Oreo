package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;

import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ExpandableView.*;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ActivatableNotificationView.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.ActivatableNotificationView.*;

public class ActivatableNotificationViewHooks {

    private static final String TAG = "ActivatableNotificationViewHooks";

    private static final float BACKGROUND_ALPHA_DIMMED = NotificationStackScrollLayoutHooks.BACKGROUND_ALPHA_DIMMED;
    private static final float DARK_EXIT_SCALE_START = 0.93f;
    private static final int ANIMATION_DURATION_STANDARD = NotificationStackScrollLayoutHooks.ANIMATION_DURATION_STANDARD;
    private static final int BACKGROUND_ANIMATION_LENGTH_MS = 220;
    private static final int ACTIVATE_ANIMATION_LENGTH = 220;
    private static final int DARK_ANIMATION_LENGTH = 170;

    private static int mBackgroundNormalVisibility;
    private static int mBackgroundDimmedVisibility;
    private static boolean tempDark;
    private static boolean tempActivated;

    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR
            = new PathInterpolator(0.6f, 0, 0.5f, 1);
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR
            = new PathInterpolator(0, 0, 0.5f, 1);

    public static void hook() {
        try {
            if (!ConfigUtils.notifications().enable_notifications_background)
                return;

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "setTintColor", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    int color = (int) param.args[0];
                    ExpandableOutlineViewHelper.getInstance(param.thisObject).setTintColor(color);
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "updateBackground", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    tempActivated = getBoolean(Fields.SystemUI.ActivatableNotificationView.mActivated, param.thisObject);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
                    View mBackgroundNormal = helper.getBackgroundNormal();
                    View mBackgroundDimmed = helper.getBackgroundDimmed();
                    if (!getBoolean(mDark, param.thisObject)) {
                        if (getBoolean(mDimmed, param.thisObject)) {
                            // When groups are animating to the expanded state from the lockscreen, show the
                            // normal background instead of the dimmed background
                            final boolean dontShowDimmed = helper.isGroupExpansionChanging() && helper.isChildInGroup();
                            mBackgroundDimmed.setVisibility(dontShowDimmed ? View.INVISIBLE : View.VISIBLE);
                            mBackgroundNormal.setVisibility((tempActivated || dontShowDimmed)
                                    ? View.VISIBLE
                                    : View.INVISIBLE);
                        } else
                            invoke(Methods.SystemUI.ActivatableNotificationView.makeInactive, param.thisObject, false);
                    }
                    helper.setNormalBackgroundVisibilityAmount(mBackgroundNormal.getVisibility() == View.VISIBLE ? 1.0f : 0.0f);
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "updateBackgroundTint", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
                    helper.updateBackgroundTint(false);
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "enableAppearDrawing", boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean enable = (boolean) param.args[0];
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
                    if (enable != getBoolean(mDrawingAppearAnimation, param.thisObject)) {
                        set(mDrawingAppearAnimation, param.thisObject, enable);
                        if (!enable) {
                            invoke(setContentAlpha, param.thisObject, 1.0f);
                            set(mAppearAnimationFraction, param.thisObject, -1);
                            helper.setOutlineRect(null);
                        }
                    }
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "cancelAppearAnimation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (get(mAppearAnimator, param.thisObject) != null)
                        set(mAppearAnimator, param.thisObject, null);
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "reset", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ExpandableOutlineViewHelper.getInstance(param.thisObject).resetBackgroundAlpha();
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "fadeDimmedBackground", fadeDimmedBackground);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "fadeInFromDark", long.class, fadeInFromDark);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "makeInactive", boolean.class, makeInactive);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "startActivateAnimation", boolean.class, startActivateAnimation);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "setDark", boolean.class, boolean.class, long.class, setDarkHook);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "startAppearAnimation", boolean.class, float.class, long.class, long.class, Runnable.class, startAppearAnimation);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "setDimmed", boolean.class, boolean.class, setDimmed);
            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "updateAppearRect", updateAppearRect);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking ActivatableNotificationView ", t);
        }
    }

    private static XC_MethodReplacement setDimmed = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            boolean dimmed = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            if (getBoolean(mDimmed, expandableView) != dimmed) {
                set(mDimmed, expandableView, dimmed);
                helper.resetBackgroundAlpha();
                if (fade) {
                    invoke(Methods.SystemUI.ActivatableNotificationView.fadeDimmedBackground, expandableView);
                } else {
                    helper.updateBackground();
                }
            }
            return null;
        }
    };

    private static XC_MethodReplacement startAppearAnimation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final FrameLayout expandableView = (FrameLayout) param.thisObject;
            final ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(param.thisObject);
            final boolean isAppearing = (boolean) param.args[0];
            float translationDirection = (float) param.args[1];
            long delay = (long) param.args[2];
            long duration = (long) param.args[3];
            final Runnable onFinishedRunnable = (Runnable) param.args[4];
            float appearAnimationFraction = getFloat(mAppearAnimationFraction, expandableView);
            invoke(cancelAppearAnimation, expandableView);
            set(mAnimationTranslationY, expandableView, translationDirection * (int) invoke(getActualHeight, expandableView));
            if (appearAnimationFraction == -1.0f) {
                // not initialized yet, we start anew
                if (isAppearing) {
                    appearAnimationFraction = 0.0f;
                    set(mAppearAnimationTranslation, expandableView, getFloat(mAnimationTranslationY, expandableView));
                } else {
                    appearAnimationFraction = 1.0f;
                    set(mAppearAnimationTranslation, expandableView, 0);
                }
            }
            float targetValue;
            if (isAppearing) {
                set(mCurrentAppearInterpolator, expandableView, get(mSlowOutFastInInterpolator, expandableView));
                set(mCurrentAlphaInterpolator, expandableView, Interpolators.LINEAR_OUT_SLOW_IN);
                targetValue = 1.0f;
            } else {
                set(mCurrentAppearInterpolator, expandableView, Interpolators.FAST_OUT_SLOW_IN);
                set(mCurrentAlphaInterpolator, expandableView, get(mSlowOutFastInInterpolator, expandableView));
                targetValue = 0.0f;
            }
            set(mAppearAnimator, expandableView, ValueAnimator.ofFloat(appearAnimationFraction, targetValue));
            ValueAnimator mAppearAnimator = get(Fields.SystemUI.ActivatableNotificationView.mAppearAnimator, expandableView);
            mAppearAnimator.setInterpolator(Interpolators.LINEAR);
            mAppearAnimator.setDuration(
                    (long) (duration * Math.abs(appearAnimationFraction - targetValue)));
            mAppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    set(mAppearAnimationFraction, expandableView, animation.getAnimatedValue());
                    invoke(updateAppearAnimationAlpha, expandableView);
                    invoke(Methods.SystemUI.ActivatableNotificationView.updateAppearRect, expandableView);
                    expandableView.invalidate();
                }
            });
            if (delay > 0) {
                // we need to apply the initial state already to avoid drawn frames in the wrong state
                invoke(updateAppearAnimationAlpha, expandableView);
                invoke(Methods.SystemUI.ActivatableNotificationView.updateAppearRect, expandableView);
                mAppearAnimator.setStartDelay(delay);
            }
            mAppearAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mWasCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onFinishedRunnable != null) {
                        onFinishedRunnable.run();
                    }
                    if (!mWasCancelled) {
                        invoke(enableAppearDrawing, expandableView, false);
                        if (Classes.SystemUI.ExpandableNotificationRow.isInstance(expandableView))
                            helper.onAppearAnimationFinished(isAppearing);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mWasCancelled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mWasCancelled = true;
                }
            });
            mAppearAnimator.start();
            return null;
        }
    };

    private static XC_MethodHook setDarkHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            View backgroundNormal = get(mBackgroundNormal, expandableView);
            View backgroundDimmed = get(mBackgroundDimmed, expandableView);
            mBackgroundDimmedVisibility = backgroundDimmed.getVisibility();
            mBackgroundNormalVisibility = backgroundNormal.getVisibility();
            tempDark = getBoolean(mDark, expandableView);
            set(mDark, expandableView, param.args[0]);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            boolean dark = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            long delay = (long) param.args[2];
            set(mDark, expandableView, tempDark);
            helper.getBackgroundDimmed().setVisibility(mBackgroundDimmedVisibility);
            helper.getBackgroundNormal().setVisibility(mBackgroundNormalVisibility);

            if (tempDark == dark) {
                return;
            }
            set(mDark, expandableView, dark);
            tempDark = dark;
            helper.updateBackground();
            if (!dark && fade && !tempDark) {
                invoke(Methods.SystemUI.ActivatableNotificationView.fadeInFromDark, expandableView, delay);
            }
            helper.updateOutlineAlpha();
        }
    };

    private static XC_MethodReplacement startActivateAnimation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final FrameLayout expandableView = (FrameLayout) param.thisObject;
            final ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            final boolean reverse = (boolean) param.args[0];
            View mBackgroundNormal = get(Fields.SystemUI.ActivatableNotificationView.mBackgroundNormal, expandableView);
            if (!expandableView.isAttachedToWindow()) {
                return null;
            }
            int widthHalf = mBackgroundNormal.getWidth()/2;
            int heightHalf = ((int) invoke(Methods.SystemUI.NotificationBackgroundView.getActualHeight, mBackgroundNormal))/2;
            float radius = (float) Math.sqrt(widthHalf*widthHalf + heightHalf*heightHalf);
            Animator animator;
            if (reverse) {
                animator = ViewAnimationUtils.createCircularReveal(mBackgroundNormal,
                        widthHalf, heightHalf, radius, 0);
            } else {
                animator = ViewAnimationUtils.createCircularReveal(mBackgroundNormal,
                        widthHalf, heightHalf, 0, radius);
            }
            mBackgroundNormal.setVisibility(View.VISIBLE);
            Interpolator interpolator;
            Interpolator alphaInterpolator;
            if (!reverse) {
                interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
                alphaInterpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            } else {
                interpolator = ACTIVATE_INVERSE_INTERPOLATOR;
                alphaInterpolator = ACTIVATE_INVERSE_ALPHA_INTERPOLATOR;
            }
            animator.setInterpolator(interpolator);
            animator.setDuration(ACTIVATE_ANIMATION_LENGTH);
            if (reverse) {
                mBackgroundNormal.setAlpha(1f);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                            helper.updateBackground();
                    }
                });
                animator.start();
            } else {
                mBackgroundNormal.setAlpha(0.4f);
                animator.start();
            }
            mBackgroundNormal.animate()
                    .alpha(reverse ? 0f : 1f)
                    .setInterpolator(alphaInterpolator)
                    .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float animatedFraction = animation.getAnimatedFraction();
                            if (reverse) {
                                animatedFraction = 1.0f - animatedFraction;
                            }
                            helper.setNormalBackgroundVisibilityAmount(animatedFraction);
                        }
                    })
                    .setDuration(ACTIVATE_ANIMATION_LENGTH);
            return null;
        }
    };

    private static XC_MethodReplacement fadeInFromDark = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            long delay = (long) param.args[0];
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            View mBackgroundNormal = helper.getBackgroundNormal();
            View mBackgroundDimmed = helper.getBackgroundDimmed();
            final View background = getBoolean(mDimmed, expandableView) ? mBackgroundDimmed : mBackgroundNormal;
            background.setAlpha(0f);
            helper.mBackgroundVisibilityUpdater.onAnimationUpdate(null);
            background.setPivotX(mBackgroundDimmed.getWidth() / 2f);
            background.setPivotY(((int) invoke(getActualHeight, expandableView)) / 2f);
            background.setScaleX(DARK_EXIT_SCALE_START);
            background.setScaleY(DARK_EXIT_SCALE_START);
            background.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DARK_ANIMATION_LENGTH)
                    .setStartDelay(delay)
                    .setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // Jump state if we are cancelled
                            background.setScaleX(1f);
                            background.setScaleY(1f);
                            background.setAlpha(1f);
                        }
                    })
                    .setUpdateListener(helper.mBackgroundVisibilityUpdater)
                    .start();
            helper.mFadeInFromDarkAnimator = TimeAnimator.ofFloat(0.0f, 1.0f);
            helper.mFadeInFromDarkAnimator.setDuration(DARK_ANIMATION_LENGTH);
            helper.mFadeInFromDarkAnimator.setStartDelay(delay);
            helper.mFadeInFromDarkAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            helper.mFadeInFromDarkAnimator.addListener(helper.mFadeInEndListener);
            helper.mFadeInFromDarkAnimator.addUpdateListener(helper.mUpdateOutlineListener);
            helper.mFadeInFromDarkAnimator.start();
            return null;
        }
    };

    private static XC_MethodReplacement makeInactive = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            boolean animate = (boolean) param.args[0];
            Object mOnActivatedListener = get(Fields.SystemUI.ActivatableNotificationView.mOnActivatedListener, expandableView);
            if (getBoolean(mActivated, expandableView)) {
                set(mActivated, expandableView,  false);
                if (getBoolean(mDimmed, expandableView)) {
                    if (animate) {
                        invoke(Methods.SystemUI.ActivatableNotificationView.startActivateAnimation, expandableView,  true /* reverse */);
                    } else {
                        ExpandableOutlineViewHelper.getInstance(expandableView).updateBackground();
                    }
                }
            }
            if (mOnActivatedListener != null) {
                XposedHelpers.callMethod(mOnActivatedListener, "onActivationReset", expandableView);
            }
            ((FrameLayout) expandableView).removeCallbacks((Runnable) get(Fields.SystemUI.ActivatableNotificationView.mTapTimeoutRunnable, expandableView));
            return null;
        }
    };

    private static XC_MethodReplacement fadeDimmedBackground = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object expandableView = param.thisObject;
            final ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            ValueAnimator backgroundAnimator = get(mBackgroundAnimator, expandableView);
            View mBackgroundNormal = helper.getBackgroundNormal();
            View mBackgroundDimmed = helper.getBackgroundDimmed();
            boolean activated = getBoolean(mActivated, expandableView);
            boolean dimmed = getBoolean(mDimmed, expandableView);
            boolean dark = getBoolean(mDark, expandableView);
            mBackgroundDimmed.animate().cancel();
            mBackgroundNormal.animate().cancel();
            if (activated) {
                helper.updateBackground();
                return null;
            }
            if (!dark) {
                if (dimmed) {
                    mBackgroundDimmed.setVisibility(View.VISIBLE);
                } else {
                    mBackgroundNormal.setVisibility(View.VISIBLE);
                }
            }
            float startAlpha = dimmed ? 1f : 0;
            float endAlpha = dimmed ? 0 : 1f;
            int duration = BACKGROUND_ANIMATION_LENGTH_MS;
            // Check whether there is already a background animation running.
            if (backgroundAnimator != null) {
                startAlpha = (Float) backgroundAnimator.getAnimatedValue();
                duration = (int) backgroundAnimator.getCurrentPlayTime();
                backgroundAnimator.removeAllListeners();
                backgroundAnimator.cancel();
                if (duration <= 0) {
                    helper.updateBackground();
                    return null;
                }
            }
            mBackgroundNormal.setAlpha(startAlpha);
            backgroundAnimator =
                    ObjectAnimator.ofFloat(mBackgroundNormal, View.ALPHA, startAlpha, endAlpha);
            backgroundAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            backgroundAnimator.setDuration(duration);
            backgroundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    helper.updateBackground();
                    set(mBackgroundAnimator, expandableView, null);
                    if (helper.mFadeInFromDarkAnimator == null) {
                        helper.mDimmedBackgroundFadeInAmount = -1;
                    }
                }
            });
            backgroundAnimator.addUpdateListener(helper.mBackgroundVisibilityUpdater);
            backgroundAnimator.start();
            return null;
        }
    };

    private static final XC_MethodReplacement updateAppearRect = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View expandableView = (View) param.thisObject;
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            float appearAnimationTranslation;
            float appearAnimationFraction = getFloat(mAppearAnimationFraction, expandableView);
            float animationTranslationY = getFloat(mAnimationTranslationY, expandableView);
            RectF appearAnimationRect = get(mAppearAnimationRect, expandableView);
            Interpolator currentAppearInterpolator = get(mCurrentAppearInterpolator, expandableView);

            float inverseFraction = (1.0f - appearAnimationFraction);
            float translationFraction = currentAppearInterpolator.getInterpolation(inverseFraction);
            float translateYTotalAmount = translationFraction * animationTranslationY;
            set(mAppearAnimationTranslation, expandableView, translateYTotalAmount);
            appearAnimationTranslation = translateYTotalAmount;

            // handle width animation
            float widthFraction = (inverseFraction - (1.0f - 1.0f))
                    / (1.0f - 0.2f);
            widthFraction = Math.min(1.0f, Math.max(0.0f, widthFraction));
            widthFraction = currentAppearInterpolator.getInterpolation(widthFraction);
            float left = (expandableView.getWidth() * (0.5f - 0.05f / 2.0f) *
                    widthFraction);
            float right = expandableView.getWidth() - left;

            // handle top animation
            float heightFraction = (inverseFraction - (1.0f - 1.0f)) /
                    1.0f;
            heightFraction = Math.max(0.0f, heightFraction);
            heightFraction = currentAppearInterpolator.getInterpolation(heightFraction);

            float top;
            float bottom;
            final int actualHeight = invoke(getActualHeight, expandableView);
            if (animationTranslationY > 0.0f) {
                bottom = actualHeight - heightFraction * animationTranslationY * 0.1f
                        - translateYTotalAmount;
                top = bottom * heightFraction;
            } else {
                top = heightFraction * (actualHeight + animationTranslationY) * 0.1f -
                        translateYTotalAmount;
                bottom = actualHeight * (1 - heightFraction) + top * heightFraction;
            }
            appearAnimationRect.set(left, top, right, bottom);
            helper.setOutlineRect(left, top + appearAnimationTranslation, right,
                    bottom + appearAnimationTranslation);
            return null;
        }
    };
}
