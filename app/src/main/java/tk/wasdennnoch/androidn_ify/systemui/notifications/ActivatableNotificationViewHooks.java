package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;

public class ActivatableNotificationViewHooks {

    private static final String TAG = "ActivatableNotificationViewHooks";

    private static final float BACKGROUND_ALPHA_DIMMED = NotificationStackScrollLayoutHooks.BACKGROUND_ALPHA_DIMMED;
    private static final float DARK_EXIT_SCALE_START = 0.93f;
    private static final int ANIMATION_DURATION_STANDARD = NotificationStackScrollLayoutHooks.ANIMATION_DURATION_STANDARD;
    private static final int BACKGROUND_ANIMATION_LENGTH_MS = 220;
    private static final int ACTIVATE_ANIMATION_LENGTH = 220;
    private static final int DARK_ANIMATION_LENGTH = 170;

    private static Method methodCancelAppearAnimation;
    private static Method methodStartActivateAnimation;
    private static Method methodFadeDimmedBackground;
    private static Method methodUpdateAppearAnimationAlpha;
    private static Method methodUpdateAppearRect;
    private static Method methodEnableAppearDrawing;
    private static Method methodFadeInFromDark;
    private static Method methodSetContentAlpha;
    public static Method methodUpdateClipping;

    private static Field fieldOnActivatedListener;

    private static int mBackgroundNormalVisibility;
    private static int mBackgroundDimmedVisibility;
    private static boolean mDark;

    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR
            = new PathInterpolator(0.6f, 0, 0.5f, 1);
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR
            = new PathInterpolator(0, 0, 0.5f, 1);

    public static void hook() {
        try {
            ExpandableOutlineViewHelper.initFields();

            if (!ConfigUtils.notifications().enable_notifications_background)
                return;

            methodCancelAppearAnimation = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "cancelAppearAnimation");
            methodStartActivateAnimation = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "startActivateAnimation", boolean.class);
            methodFadeDimmedBackground = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "fadeDimmedBackground");
            methodUpdateAppearAnimationAlpha = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "updateAppearAnimationAlpha");
            methodUpdateAppearRect = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "updateAppearRect");
            methodEnableAppearDrawing = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "enableAppearDrawing", boolean.class);
            methodFadeInFromDark = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "fadeInFromDark", long.class);
            methodSetContentAlpha = XposedHelpers.findMethodBestMatch(ActivatableNotificationView, "setContentAlpha", float.class);
            methodUpdateClipping = XposedHelpers.findMethodBestMatch(ExpandableView, "updateClipping");
            fieldOnActivatedListener = XposedHelpers.findField(ActivatableNotificationView, "mOnActivatedListener");

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
                    helper.onFinishInflate();
                    helper.updateOutlineAlpha();
                }
            });

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
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
                    View mBackgroundNormal = helper.getBackgroundNormal();
                    if (!XposedHelpers.getBooleanField(param.thisObject, "mDark")) {
                        if (XposedHelpers.getBooleanField(param.thisObject, "mDimmed"))
                            mBackgroundNormal.setVisibility((XposedHelpers.getBooleanField(param.thisObject, "mActivated")
                                    ? View.VISIBLE
                                    : View.INVISIBLE));
                        else
                            XposedHelpers.callMethod(param.thisObject, "makeInactive", false);
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
                    if (enable != XposedHelpers.getBooleanField(param.thisObject, "mDrawingAppearAnimation")) {
                        XposedHelpers.setBooleanField(param.thisObject, "mDrawingAppearAnimation", enable);
                        if (!enable) {
                            methodSetContentAlpha.invoke(param.thisObject, 1.0f);
                            XposedHelpers.setFloatField(param.thisObject, "mAppearAnimationFraction", -1);
                            helper.setOutlineRect(null);
                        }
                    }
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(ActivatableNotificationView, "cancelAppearAnimation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getObjectField(param.thisObject, "mAppearAnimator") != null)
                        XposedHelpers.setObjectField(param.thisObject, "mAppearAnimator", null);
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
            if (XposedHelpers.getBooleanField(expandableView, "mDimmed") != dimmed) {
                XposedHelpers.setBooleanField(expandableView, "mDimmed", dimmed);
                helper.resetBackgroundAlpha();
                if (fade) {
                    methodFadeDimmedBackground.invoke(expandableView);
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
            float mAppearAnimationFraction = XposedHelpers.getFloatField(expandableView, "mAppearAnimationFraction");
            methodCancelAppearAnimation.invoke(expandableView);
            XposedHelpers.setFloatField(expandableView, "mAnimationTranslationY", translationDirection * (int) XposedHelpers.callMethod(expandableView, "getActualHeight"));
            if (mAppearAnimationFraction == -1.0f) {
                // not initialized yet, we start anew
                if (isAppearing) {
                    mAppearAnimationFraction = 0.0f;
                    XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", XposedHelpers.getFloatField(expandableView, "mAnimationTranslationY"));
                } else {
                    mAppearAnimationFraction = 1.0f;
                    XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", 0);
                }
            }
            float targetValue;
            if (isAppearing) {
                XposedHelpers.setObjectField(expandableView, "mCurrentAppearInterpolator", XposedHelpers.getObjectField(expandableView, "mSlowOutFastInInterpolator"));
                XposedHelpers.setObjectField(expandableView, "mCurrentAlphaInterpolator", Interpolators.LINEAR_OUT_SLOW_IN);
                targetValue = 1.0f;
            } else {
                XposedHelpers.setObjectField(expandableView, "mCurrentAppearInterpolator", Interpolators.FAST_OUT_SLOW_IN);
                XposedHelpers.setObjectField(expandableView, "mCurrentAlphaInterpolator", XposedHelpers.getObjectField(expandableView, "mSlowOutLinearInInterpolator"));
                targetValue = 0.0f;
            }
            XposedHelpers.setObjectField(expandableView, "mAppearAnimator", ValueAnimator.ofFloat(mAppearAnimationFraction,
                    targetValue));
            ValueAnimator mAppearAnimator = (ValueAnimator) XposedHelpers.getObjectField(expandableView, "mAppearAnimator");
            mAppearAnimator.setInterpolator(Interpolators.LINEAR);
            mAppearAnimator.setDuration(
                    (long) (duration * Math.abs(mAppearAnimationFraction - targetValue)));
            mAppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    XposedHelpers.setObjectField(expandableView, "mAppearAnimationFraction", animation.getAnimatedValue());
                    try {
                        methodUpdateAppearAnimationAlpha.invoke(expandableView);
                        methodUpdateAppearRect.invoke(expandableView);
                    } catch (IllegalAccessException | InvocationTargetException ignore) {
                        XposedHook.logI(TAG, ignore.toString());
                    }
                    expandableView.invalidate();
                }
            });
            if (delay > 0) {
                // we need to apply the initial state already to avoid drawn frames in the wrong state
                methodUpdateAppearAnimationAlpha.invoke(expandableView);
                methodUpdateAppearRect.invoke(expandableView);
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
                        try {
                            methodEnableAppearDrawing.invoke(expandableView, false);
                            if (Classes.SystemUI.ExpandableNotificationRow.isInstance(expandableView))
                                helper.onAppearAnimationFinished(isAppearing);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            XposedHook.logI(TAG, e.toString());
                        }
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
            View mBackgroundNormal = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundNormal");
            View mBackgroundDimmed = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundDimmed");
            mBackgroundDimmedVisibility = mBackgroundDimmed.getVisibility();
            mBackgroundNormalVisibility = mBackgroundNormal.getVisibility();
            mDark = XposedHelpers.getBooleanField(expandableView, "mDark");
            XposedHelpers.setBooleanField(expandableView, "mDark", (boolean) param.args[0]);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(param.thisObject);
            boolean dark = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            long delay = (long) param.args[2];
            XposedHelpers.setBooleanField(expandableView, "mDark", mDark);
            helper.getBackgroundDimmed().setVisibility(mBackgroundDimmedVisibility);
            helper.getBackgroundNormal().setVisibility(mBackgroundNormalVisibility);

            if (mDark == dark) {
                return;
            }
            XposedHelpers.setBooleanField(expandableView, "mDark", dark);
            mDark = dark;
            helper.updateBackground();
            if (!dark && fade && !mDark) {
                methodFadeInFromDark.invoke(expandableView, delay);
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
            View mBackgroundNormal = helper.getBackgroundNormal();
            if (!expandableView.isAttachedToWindow()) {
                return null;
            }
            int widthHalf = mBackgroundNormal.getWidth()/2;
            int heightHalf = ((int) XposedHelpers.callMethod(mBackgroundNormal, "getActualHeight"))/2;
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
            final View background = XposedHelpers.getBooleanField(expandableView, "mDimmed") ? mBackgroundDimmed : mBackgroundNormal;
            background.setAlpha(0f);
            helper.mBackgroundVisibilityUpdater.onAnimationUpdate(null);
            background.setPivotX(mBackgroundDimmed.getWidth() / 2f);
            background.setPivotY(((int) XposedHelpers.callMethod(expandableView, "getActualHeight")) / 2f);
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
            Object mOnActivatedListener = fieldOnActivatedListener.get(expandableView);
            if (XposedHelpers.getBooleanField(expandableView, "mActivated")) {
                XposedHelpers.setBooleanField(expandableView, "mActivated",  false);
                if (XposedHelpers.getBooleanField(expandableView, "mDimmed")) {
                    if (animate) {
                        methodStartActivateAnimation.invoke(expandableView,  true /* reverse */);
                    } else {
                        ExpandableOutlineViewHelper.getInstance(expandableView).updateBackground();
                    }
                }
            }
            if (mOnActivatedListener != null) {
                XposedHelpers.callMethod(mOnActivatedListener, "onActivationReset", expandableView);
            }
            ((FrameLayout) expandableView).removeCallbacks((Runnable) XposedHelpers.getObjectField(expandableView, "mTapTimeoutRunnable"));
            return null;
        }
    };

    private static XC_MethodReplacement fadeDimmedBackground = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object expandableView = param.thisObject;
            final ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            ValueAnimator mBackgroundAnimator = (ValueAnimator) XposedHelpers.getObjectField(expandableView, "mBackgroundAnimator");
            View mBackgroundNormal = helper.getBackgroundNormal();
            View mBackgroundDimmed = helper.getBackgroundDimmed();
            boolean mActivated = XposedHelpers.getBooleanField(expandableView, "mActivated");
            boolean mDimmed = XposedHelpers.getBooleanField(expandableView, "mDimmed");
            boolean mDark = XposedHelpers.getBooleanField(expandableView, "mDark");
            mBackgroundDimmed.animate().cancel();
            mBackgroundNormal.animate().cancel();
            if (mActivated) {
                helper.updateBackground();
                return null;
            }
            if (!mDark) {
                if (mDimmed) {
                    mBackgroundDimmed.setVisibility(View.VISIBLE);
                } else {
                    mBackgroundNormal.setVisibility(View.VISIBLE);
                }
            }
            float startAlpha = mDimmed ? 1f : 0;
            float endAlpha = mDimmed ? 0 : 1f;
            int duration = BACKGROUND_ANIMATION_LENGTH_MS;
            // Check whether there is already a background animation running.
            if (mBackgroundAnimator != null) {
                startAlpha = (Float) mBackgroundAnimator.getAnimatedValue();
                duration = (int) mBackgroundAnimator.getCurrentPlayTime();
                mBackgroundAnimator.removeAllListeners();
                mBackgroundAnimator.cancel();
                if (duration <= 0) {
                    helper.updateBackground();
                    return null;
                }
            }
            mBackgroundNormal.setAlpha(startAlpha);
            mBackgroundAnimator =
                    ObjectAnimator.ofFloat(mBackgroundNormal, View.ALPHA, startAlpha, endAlpha);
            mBackgroundAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            mBackgroundAnimator.setDuration(duration);
            mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    helper.updateBackground();
                    XposedHelpers.setObjectField(expandableView, "mBackgroundAnimator", null);
                    if (helper.mFadeInFromDarkAnimator == null) {
                        helper.mDimmedBackgroundFadeInAmount = -1;
                    }
                }
            });
            mBackgroundAnimator.addUpdateListener(helper.mBackgroundVisibilityUpdater);
            mBackgroundAnimator.start();
            return null;
        }
    };

    private static final XC_MethodReplacement updateAppearRect = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View expandableView = (View) param.thisObject;
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            float mAppearAnimationTranslation;
            float mAppearAnimationFraction = XposedHelpers.getFloatField(expandableView, "mAppearAnimationFraction");
            float mAnimationTranslationY = XposedHelpers.getFloatField(expandableView, "mAnimationTranslationY");
            RectF mAppearAnimationRect = (RectF) XposedHelpers.getObjectField(expandableView, "mAppearAnimationRect");
            Interpolator mCurrentAppearInterpolator = (Interpolator) XposedHelpers.getObjectField(expandableView, "mCurrentAppearInterpolator");

            float inverseFraction = (1.0f - mAppearAnimationFraction);
            float translationFraction = mCurrentAppearInterpolator.getInterpolation(inverseFraction);
            float translateYTotalAmount = translationFraction * mAnimationTranslationY;
            XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", translateYTotalAmount);
            mAppearAnimationTranslation = translateYTotalAmount;

            // handle width animation
            float widthFraction = (inverseFraction - (1.0f - 1.0f))
                    / (1.0f - 0.2f);
            widthFraction = Math.min(1.0f, Math.max(0.0f, widthFraction));
            widthFraction = mCurrentAppearInterpolator.getInterpolation(widthFraction);
            float left = (expandableView.getWidth() * (0.5f - 0.05f / 2.0f) *
                    widthFraction);
            float right = expandableView.getWidth() - left;

            // handle top animation
            float heightFraction = (inverseFraction - (1.0f - 1.0f)) /
                    1.0f;
            heightFraction = Math.max(0.0f, heightFraction);
            heightFraction = mCurrentAppearInterpolator.getInterpolation(heightFraction);

            float top;
            float bottom;
            final int actualHeight = (int) XposedHelpers.callMethod(expandableView, "getActualHeight");
            if (mAnimationTranslationY > 0.0f) {
                bottom = actualHeight - heightFraction * mAnimationTranslationY * 0.1f
                        - translateYTotalAmount;
                top = bottom * heightFraction;
            } else {
                top = heightFraction * (actualHeight + mAnimationTranslationY) * 0.1f -
                        translateYTotalAmount;
                bottom = actualHeight * (1 - heightFraction) + top * heightFraction;
            }
            mAppearAnimationRect.set(left, top, right, bottom);
            helper.setOutlineRect(left, top + mAppearAnimationTranslation, right,
                    bottom + mAppearAnimationTranslation);
            return null;
        }
    };
}
