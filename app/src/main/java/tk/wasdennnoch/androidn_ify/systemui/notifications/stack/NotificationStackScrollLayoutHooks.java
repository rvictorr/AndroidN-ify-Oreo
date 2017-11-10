package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.misc.SafeOnPreDrawListener;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableNotificationRowHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableOutlineViewHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationsStuff;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ScrimHelper;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class NotificationStackScrollLayoutHooks implements View.OnApplyWindowInsetsListener {
    private static final String TAG = "NotificationStackScrollLayoutHooks";

    private static final int DARK_ANIMATION_ORIGIN_INDEX_ABOVE = -1;
    private static final int DARK_ANIMATION_ORIGIN_INDEX_BELOW = -2;
    private static final int ANIMATION_DELAY_PER_ELEMENT_DARK = 24;
    public static final int ANIMATION_DURATION_DIMMED_ACTIVATED = 220;
    public static final int ANIMATION_DURATION_STANDARD = 360;
    public static final float BACKGROUND_ALPHA_DIMMED = 0.7f;

    private int TAG_ANIMATOR_TRANSLATION_Y;
    private int TAG_END_TRANSLATION_Y;

    private Class<?> classActivatableNotificationView;
    private Class<?> classStackStateAnimator;
    private static ViewGroup mStackScrollLayout;
    private Context mContext;
    private ResourceUtils mRes;
    private static final Paint mBackgroundPaint = new Paint();
    private OverScroller mScroller;
    private Object mAmbientState;
    private Object mSwipeHelper;
    private ArrayList<View> mDraggedViews;

    private boolean mDisallowDismissInThisMotion;
    private boolean mAnimationRunning;
    private boolean mAnimationsEnabled = true;
    private boolean mDontClampNextScroll;
    private boolean mContinuousShadowUpdate;
    private boolean mIsExpanded;
    private static boolean mFadingOut = false;
    private static boolean mParentFadingOut;
    private static boolean mDrawBackgroundAsSrc;
    private static float mDimAmount;
    private static float mBackgroundFadeAmount = 1.0f;
    private static int mBgColor;
    private ValueAnimator mDimAnimator;

    private static final Property<ViewGroup, Float> BACKGROUND_FADE =
            new FloatProperty<ViewGroup>("backgroundFade") {
                @Override
                public void setValue(ViewGroup object, float value) {
                    setBackgroundFadeAmount(value);
                }

                @Override
                public Float get(ViewGroup object) {
                    return getBackgroundFadeAmount();
                }
            };

    private ValueAnimator.AnimatorUpdateListener mDimUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            setDimAmount((Float) animation.getAnimatedValue());
        }
    };

    private Animator.AnimatorListener mDimEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mDimAnimator = null;
        }
    };
    private ViewTreeObserver.OnPreDrawListener mBackgroundUpdater = new SafeOnPreDrawListener() {
        @Override
        public boolean onPreDrawSafe() {
            updateBackground();
            return true;
        }
    };
    private ViewTreeObserver.OnPreDrawListener mShadowUpdater = new SafeOnPreDrawListener() {
        @Override
        public boolean onPreDrawSafe() {
            updateViewShadows();
            return true;
        }
    };
    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f)
                .setDuration(200)
                .setInterpolator(Interpolators.ALPHA_IN);
    }
    private Rect mBackgroundBounds = new Rect();
    private Rect mStartAnimationRect = new Rect();
    private Rect mEndAnimationRect = new Rect();
    private Rect mCurrentBounds = new Rect(-1, -1, -1, -1);
    private boolean mAnimateNextBackgroundBottom;
    private boolean mAnimateNextBackgroundTop;
    private ObjectAnimator mBottomAnimator = null;
    private ObjectAnimator mTopAnimator = null;
    private FrameLayout mFirstVisibleBackgroundChild = null;
    private FrameLayout mLastVisibleBackgroundChild = null;
    private int mBottomInset = 0;
    private int mTopPadding;
    private float mStackTranslation;
    private View mForcedScroll = null;
    private static PorterDuffXfermode mSrcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    private ArrayList<View> mTmpSortedChildren = new ArrayList<>();
    private Comparator<View> mViewPositionComparator = new Comparator<View>() {
        @Override
        public int compare(View view, View otherView) {
            float endY = view.getTranslationY() + XposedHelpers.getIntField(view, "mActualHeight");
            float otherEndY = otherView.getTranslationY() + XposedHelpers.getIntField(otherView, "mActualHeight");
            if (endY < otherEndY) {
                return -1;
            } else if (endY > otherEndY) {
                return 1;
            } else {
                // The two notifications end at the same location
                return 0;
            }
        }
    };

    public NotificationStackScrollLayoutHooks(ClassLoader classLoader) {
        try {
            Class classNotificationStackScrollLayout = XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader);
            Class classBrightnessMirrorController = XposedHelpers.findClass("com.android.systemui.statusbar.policy.BrightnessMirrorController", classLoader);
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "initView", new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mStackScrollLayout = (ViewGroup) param.thisObject;
                    mScroller = (OverScroller) XposedHelpers.getObjectField(param.thisObject, "mScroller");
                    mAmbientState = XposedHelpers.getObjectField(param.thisObject, "mAmbientState");
                    mDraggedViews = (ArrayList<View>) XposedHelpers.getObjectField(mAmbientState, "mDraggedViews");
                    mSwipeHelper = XposedHelpers.getObjectField(param.thisObject, "mSwipeHelper");
                    mContext = (Context) param.args[0];
                    mRes = ResourceUtils.getInstance(mContext);
                    mBgColor = mRes.getColor(R.color.notification_shade_background_color);
                    initView();
                    hookSwipeHelper();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onDraw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (ConfigUtils.notifications().enable_notifications_background) {
                        Canvas canvas = (Canvas) param.args[0];
                        canvas.drawRect(0, mCurrentBounds.top, mStackScrollLayout.getWidth(), mCurrentBounds.bottom, mBackgroundPaint);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onViewRemovedInternal", View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Make sure the clipRect we might have set is removed
                    XposedHelpers.callMethod(param.args[0], "setClipTopAmount", 0);
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "startAnimationToState", new XC_MethodHook() {
                private boolean willUpdateBackground = false;

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    willUpdateBackground = false;
                    boolean mNeedsAnimation = XposedHelpers.getBooleanField(mStackScrollLayout, "mNeedsAnimation");
                    if (mNeedsAnimation) {
                        XposedHelpers.callMethod(mStackScrollLayout, "generateChildHierarchyEvents");
                        XposedHelpers.setBooleanField(mStackScrollLayout, "mNeedsAnimation", false);
                    }
                    Object mAnimationEvents = XposedHelpers.getObjectField(mStackScrollLayout, "mAnimationEvents");
                    boolean isEmpty = (boolean) XposedHelpers.callMethod(mAnimationEvents, "isEmpty");
                    boolean isCurrentlyAnimating = (boolean) XposedHelpers.callMethod(mStackScrollLayout, "isCurrentlyAnimating");
                    if (!isEmpty || isCurrentlyAnimating) {
                        setAnimationRunning(true);
                        willUpdateBackground = true;
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (willUpdateBackground) {
                        updateBackground();
                        updateViewShadows();
                        willUpdateBackground = false;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onChildAnimationFinished", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    setAnimationRunning(false);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    updateBackground();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "applyCurrentState", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    setAnimationRunning(false);
                    updateBackground();
                    updateViewShadows();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    updateFirstAndLastBackgroundViews();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setAnimationsEnabled", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mAnimationsEnabled = (boolean) param.args[0];
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setIsExpanded", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mIsExpanded = (boolean) param.args[0];
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setTopPadding", int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mTopPadding = (int) param.args[0];
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setStackTranslation", float.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mStackTranslation = (float) param.args[0];
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSwipeProgress", View.class, boolean.class, float.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true); // Don't fade out the notification
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onChildSnappedBack", View.class, new XC_MethodHook() {
                @SuppressWarnings("SuspiciousMethodCalls")
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mDraggedViews.remove(param.args[0]);
                    updateContinuousShadowDrawing();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onBeginDrag", View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    updateContinuousShadowDrawing();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSpeedBumpIndex", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    int newIndex = (int) param.args[0];
                    XposedHelpers.callMethod(mAmbientState, "setSpeedBumpIndex", newIndex);
                    return null;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "initDownStates", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MotionEvent ev = (MotionEvent) param.args[0];
                    if (ev.getAction() == MotionEvent.ACTION_DOWN)
                        mDisallowDismissInThisMotion = false;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mForcedScroll = null;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateChildren", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    updateForcedScroll();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "computeScroll", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mScroller.isFinished()) {
                        mDontClampNextScroll = false;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "overScrollBy", int.class, int.class,
                    int.class, int.class,
                    int.class, int.class,
                    int.class, int.class,
                    boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (mDontClampNextScroll) {
                                int range = (int) param.args[5];
                                range = Math.max(range, getOwnScrollY());
                                param.args[5] = range;
                            }
                        }
                    });
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "setSpeedBumpView", XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSpeedBump", boolean.class, XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setDimmed", boolean.class, boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean dimmed = (boolean) param.args[0];
                    boolean animate = (boolean) param.args[1];
                    XposedHelpers.callMethod(mAmbientState, "setDimmed", dimmed);

                    if (animate && mAnimationsEnabled) {
                        XposedHelpers.setBooleanField(mStackScrollLayout, "mDimmedNeedsAnimation", true);
                        XposedHelpers.setBooleanField(mStackScrollLayout, "mNeedsAnimation", true);
                        animateDimmed(dimmed);
                    } else {
                        setDimAmount(dimmed ? 1.0f : 0.0f);
                    }
                    XposedHelpers.callMethod(mStackScrollLayout, "requestChildrenUpdate");
                    return null;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setDark", boolean.class, boolean.class, PointF.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean dark = (boolean) param.args[0];
                    boolean animate = (boolean) param.args[1];
                    PointF touchWakeUpScreenLocation = (PointF) param.args[2];
                    XposedHelpers.callMethod(mAmbientState, "setDark", dark);
                    if (animate && mAnimationsEnabled) {
                        XposedHelpers.setBooleanField(mStackScrollLayout, "mDarkNeedsAnimation", true);
                        XposedHelpers.setIntField(mStackScrollLayout, "mDarkAnimationOriginIndex", (int) XposedHelpers.callMethod(mStackScrollLayout, "findDarkAnimationOriginIndex", touchWakeUpScreenLocation));
                        XposedHelpers.setBooleanField(mStackScrollLayout, "mNeedsAnimation", true);
                        //setBackgroundFadeAmount(0.0f);
                    } /*else if (!dark) {
                        setBackgroundFadeAmount(1.0f);
                    }*/
                    XposedHelpers.callMethod(mStackScrollLayout, "requestChildrenUpdate");
                    if (dark) {
                        mStackScrollLayout.setWillNotDraw(true);
                        ScrimHelper.setExcludedBackgroundArea(null);
                    } else {
                        updateBackground();
                        mStackScrollLayout.setWillNotDraw(false);
                    }
                    return null;
                }
            });
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "setScrimController", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ScrimHelper.setScrimBehind(param.args[0]);
                    ScrimHelper.setScrimBehindChangeRunnable(new Runnable() {
                        @Override
                        public void run() {
                            updateBackgroundDimming();
                        }
                    });
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "generateDarkEvent", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getBooleanField(param.thisObject, "mDarkNeedsAnimation"))
                        startBackgroundFadeIn();
                }
            });
            XposedHelpers.findAndHookMethod(classBrightnessMirrorController, "showMirror", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    setFadingOut(true);
                }
            });
            XposedHelpers.findAndHookMethod(classBrightnessMirrorController, "hideMirror", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    View mScrimBehind = (View) XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                    Object mPanelHolder = XposedHelpers.getObjectField(param.thisObject, "mPanelHolder");
                    final View mBrightnessMirror = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessMirror");
                    XposedHelpers.callMethod(mScrimBehind, "animateViewAlpha", 1.0f, 200, Interpolators.ALPHA_IN);

                    inAnimation((ViewPropertyAnimator) XposedHelpers.callMethod(mPanelHolder, "animate"))
                            .withLayer()
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    mBrightnessMirror.setVisibility(View.INVISIBLE);
                                    setFadingOut(false);
                                }
                            });
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "changeViewPosition", View.class, int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    View child = (View) param.args[0];
                    int newIndex = (int) param.args[1];
                    int currentIndex = (int) XposedHelpers.callMethod(param.thisObject, "indexOfChild", child);
                    ArrayList mChildrenChangingPositions = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mChildrenChangingPositions");
                    if (child != null && child.getParent() == param.thisObject && currentIndex != newIndex) {
                        XposedHelpers.setBooleanField(param.thisObject, "mChangePositionInProgress", true);
                        NotificationsStuff.setChangingPosition(child, true);
                        mStackScrollLayout.removeView(child);
                        mStackScrollLayout.addView(child, newIndex);
                        NotificationsStuff.setChangingPosition(child, false);
                        XposedHelpers.setBooleanField(param.thisObject, "mChangePositionInProgress", false);
                        if (mIsExpanded && mAnimationsEnabled && child.getVisibility() != View.GONE) {
                            mChildrenChangingPositions.add(child);
                            XposedHelpers.setBooleanField(param.thisObject, "mNeedsAnimation", true);
                        }
                    }
                    return null;
                }
            });

            classActivatableNotificationView = XposedHelpers.findClass("com.android.systemui.statusbar.ActivatableNotificationView", classLoader);
            classStackStateAnimator = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackStateAnimator", classLoader);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking NotificationStackScrollLayout", t);
        }
    }

    private void hookSwipeHelper() {
        Class classSwipeHelper = mSwipeHelper.getClass();
        XC_MethodHook touchEventHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDisallowDismissInThisMotion && param.thisObject == mSwipeHelper)
                    param.setResult(false);
            }
        };
        XposedHelpers.findAndHookMethod(classSwipeHelper, "onInterceptTouchEvent", MotionEvent.class, touchEventHook);
        XposedHelpers.findAndHookMethod(classSwipeHelper, "onTouchEvent", MotionEvent.class, touchEventHook);
        XposedHelpers.findAndHookMethod(classSwipeHelper, "setTranslation", View.class, float.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.args[0];
                float translate = (float) param.args[1];
                ExpandableNotificationRowHelper.getInstance(view).setTranslation(translate);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(classSwipeHelper, "getTranslation", View.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.args[0];
                return ExpandableNotificationRowHelper.getInstance(view).getTranslation();
            }
        });

        XposedHelpers.findAndHookMethod(classSwipeHelper, "getSwipeProgressForOffset", View.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.args[0];
                float translation = (float) XposedHelpers.callMethod(param.thisObject, "getTranslation", view);

                float viewSize = (float) XposedHelpers.callMethod(param.thisObject, "getSize", view);
                float result = Math.abs(translation / viewSize);
                return Math.min(Math.max(XposedHelpers.getFloatField(param.thisObject, "mMinSwipeProgress"), result), XposedHelpers.getFloatField(param.thisObject, "mMaxSwipeProgress"));
            }
        });

        XposedHelpers.findAndHookMethod(classSwipeHelper, "createTranslationAnimation", View.class, float.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                final Object swipeHelper = param.thisObject;
                final View v = (View) param.args[0];
                float target = (float) param.args[1];
                final boolean canBeDismissed = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(swipeHelper, "mCallback"), "canChildBeDismissed", v);
                ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        XposedHelpers.callMethod(swipeHelper, "updateSwipeProgressFromOffset", v, canBeDismissed);
                    }
                };
                return ExpandableNotificationRowHelper.getInstance(v).getTranslateViewAnimator(target, updateListener);
            }
        });
        /*XposedHelpers.findAndHookMethod(classSwipeHelper, "onTouchEvent", MotionEvent.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                MotionEvent ev = (MotionEvent) param.args[0];
                Object mVelocityTracker = XposedHelpers.getObjectField(param.thisObject, "mVelocityTracker");
                View mCurrView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrView");
                Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
                if (XposedHelpers.getBooleanField(param.thisObject, "mLongPressSent")) {
                    return true;
                }

                if (!XposedHelpers.getBooleanField(param.thisObject, "mDragging")) {
                    if (XposedHelpers.callMethod(mCallback, "getChildAtPosition", ev) != null) {
                        // We are dragging directly over a card, make sure that we also catch the gesture
                        // even if nobody else wants the touch event.
                        XposedHelpers.callMethod(param.thisObject, "onInterceptTouchEvent", ev);
                        return true;
                    } else {
                        // We are not doing anything, make sure the long press callback
                        // is not still ticking like a bomb waiting to go off.
                        XposedHelpers.callMethod(param.thisObject, "removeLongPressCallback");
                        return false;
                    }
                }

                XposedHelpers.callMethod(mVelocityTracker, "addMovement", ev);
                final int action = ev.getAction();
                switch (action) {
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_MOVE:
                        if (mCurrView != null) {
                            float delta = (float) XposedHelpers.callMethod(param.thisObject, "getPos", ev) - XposedHelpers.getFloatField(param.thisObject, "mInitialTouchPos");
                            float absDelta = Math.abs(delta);
                            if (absDelta >= (int) XposedHelpers.callMethod(param.thisObject, "getFalsingThreshold")) {
                                XposedHelpers.setBooleanField(param.thisObject, "mTouchAboveFalsingThreshold", true);
                            }
                            // don't let items that can't be dismissed be dragged more than
                            // maxScrollDistance
                            if (!(boolean) XposedHelpers.callMethod(mCallback, "canChildBeDismissed", mCurrView)) {
                                float size = (float) XposedHelpers.callMethod(param.thisObject, "getSize", mCurrView);
                                float maxScrollDistance = 0.25f * size;
                                if (absDelta >= size) {
                                    delta = delta > 0 ? maxScrollDistance : -maxScrollDistance;
                                } else {
                                    delta = maxScrollDistance * (float) Math.sin((delta/size)*(Math.PI/2));
                                }
                            }
                            XposedHelpers.callMethod(param.thisObject, "setTranslation", mCurrView,
                                    (float) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mTranslation") + delta);
                            XposedHelpers.callMethod(param.thisObject, "updateSwipeProgressFromOffset", mCurrView,
                                    XposedHelpers.getBooleanField(param.thisObject, "mCanCurrViewBeDimissed"));
                            //onMoveUpdate(mCurrView, mTranslation + delta, delta);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mCurrView == null) {
                            break;
                        }
                        float maxVelocity = 4000*//*MAX_DISMISS_VELOCITY*//* * XposedHelpers.getFloatField(param.thisObject, "mDensityScale");
                        XposedHelpers.callMethod(mVelocityTracker, "computeCurrentVelocity", 1000 *//* px/sec *//*, maxVelocity);

                        float escapeVelocity = 100f *//*SWIPE_ESCAPE_VELOCITY*//* * XposedHelpers.getFloatField(param.thisObject, "mDensityScale");
                        float velocity = (float) XposedHelpers.callMethod(param.thisObject, "getVelocity", mVelocityTracker);
                        float perpendicularVelocity = (float) XposedHelpers.callMethod(param.thisObject, "getPerpendicularVelocity", mVelocityTracker);
                        // Decide whether to dismiss the current view
                        boolean childSwipedFarEnough = Math.abs((float) XposedHelpers.callMethod(param.thisObject, "getTranslation", mCurrView)) > 0.4
                                * (float) XposedHelpers.callMethod(param.thisObject, "getSize", mCurrView);
                        boolean childSwipedFastEnough = (Math.abs(velocity) > escapeVelocity) &&
                                (Math.abs(velocity) > Math.abs(perpendicularVelocity)) &&
                                (velocity > 0) == ((float) XposedHelpers.callMethod(param.thisObject, "getTranslation", mCurrView) > 0);
                        boolean falsingDetected = (boolean) XposedHelpers.callMethod(mCallback, "isAntiFalsingNeeded")
                                && !XposedHelpers.getBooleanField(param.thisObject, "mTouchAboveFalsingThreshold");

                        boolean dismissChild = (boolean) XposedHelpers.callMethod(mCallback, "canChildBeDismissed", mCurrView)
                                && !falsingDetected && (childSwipedFastEnough || childSwipedFarEnough)
                                && ev.getActionMasked() == MotionEvent.ACTION_UP;

                        if (dismissChild) {
                                // flingadingy
                                XposedHelpers.callMethod(param.thisObject, "dismissChild", mCurrView, childSwipedFastEnough ? velocity : 0f);
                            } else {
                                // snappity
                                XposedHelpers.callMethod(mCallback, "onDragCancelled", mCurrView);
                                XposedHelpers.callMethod(param.thisObject, "snapChild", mCurrView, velocity);
                            XposedHelpers.setObjectField(param.thisObject, "mCurrView",  null);
                        }
                        XposedHelpers.setBooleanField(param.thisObject, "mDragging", false);
                        break;
                }
                return true;
            }
        });
        XposedHelpers.findAndHookMethod(classSwipeHelper, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                final MotionEvent ev = (MotionEvent) param.args[0];
                final Object swipeHelper = param.thisObject;
                final Object mLongPressListener = XposedHelpers.getObjectField(param.thisObject, "mLongPressListener");
                final int[] mTmpPos = (int[]) XposedHelpers.getObjectField(param.thisObject, "mTmpPos");
                Object mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler");
                Object mVelocityTracker = XposedHelpers.getObjectField(param.thisObject, "mVelocityTracker");
                Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
                Object mWatchLongPress = XposedHelpers.getObjectField(param.thisObject, "mWatchLongPress");
                long mLongPressTimeout = XposedHelpers.getLongField(param.thisObject, "mLongPressTimeout");
                final int action = ev.getAction();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        XposedHelpers.setBooleanField(param.thisObject, "mTouchAboveFalsingThreshold",false);
                        XposedHelpers.setBooleanField(param.thisObject, "mDragging", false);
                        XposedHelpers.setBooleanField(param.thisObject, "mLongPressSent", false);

                        XposedHelpers.callMethod(mVelocityTracker, "clear");
                        final View mCurrView = (View) XposedHelpers.callMethod(mCallback, "getChildAtPosition", ev);
                        XposedHelpers.setObjectField(param.thisObject, "mCurrView", XposedHelpers.callMethod(mCallback, "getChildAtPosition", ev));

                        if (mCurrView != null) {
                            XposedHelpers.setBooleanField(param.thisObject, "mCanCurrViewBeDimissed", (boolean) XposedHelpers.callMethod(mCallback, "canChildBeDismissed", mCurrView));
                            XposedHelpers.callMethod(mVelocityTracker, "addMovement", ev);
                            XposedHelpers.setFloatField(param.thisObject, "mInitialTouchPos", (float) XposedHelpers.callMethod(param.thisObject, "getPos", ev));
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mPerpendicularInitialTouchPos", getPerpendicularPos(param.thisObject, ev));
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mTranslation", XposedHelpers.callMethod(param.thisObject, "getTranslation", mCurrView));

                            if (XposedHelpers.getObjectField(param.thisObject, "mLongPressListener") != null) {
                                if (XposedHelpers.getObjectField(param.thisObject, "mWatchLongPress") == null) {
                                    XposedHelpers.setObjectField(param.thisObject, "mWatchLongPress", new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mCurrView != null && !XposedHelpers.getBooleanField(swipeHelper, "mLongPressSent")) {
                                                XposedHelpers.setBooleanField(swipeHelper, "mLongPressSent", true);
                                                mCurrView.sendAccessibilityEvent(
                                                        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                                                mCurrView.getLocationOnScreen(mTmpPos);
                                                final int x = (int) ev.getRawX() - mTmpPos[0];
                                                final int y = (int) ev.getRawY() - mTmpPos[1];
                                                XposedHelpers.callMethod(mLongPressListener, "onLongPress", mCurrView, x, y);
                                            }
                                        }
                                    });
                                }
                                XposedHelpers.callMethod(mHandler, "postDelayed", mWatchLongPress, mLongPressTimeout);
                            }
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        Object currView = XposedHelpers.getObjectField(param.thisObject, "mCurrView");
                        if (currView != null && !XposedHelpers.getBooleanField(param.thisObject, "mLongPressSent")) {
                            XposedHelpers.callMethod(mVelocityTracker, "addMovement", ev);
                            float pos = (float) XposedHelpers.callMethod(param.thisObject, "getPos", ev);
                            float perpendicularPos = getPerpendicularPos(param.thisObject, ev);
                            float deltaPerpendicular = perpendicularPos - (float) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mPerpendicularInitialTouchPos");
                            float delta = pos - XposedHelpers.getFloatField(param.thisObject, "mInitialTouchPos");
                            if (Math.abs(delta) > XposedHelpers.getFloatField(param.thisObject, "mPagingTouchSlop")
                                    && Math.abs(delta) > Math.abs(deltaPerpendicular)) {
                                XposedHelpers.callMethod(mCallback, "onBeginDrag", currView);
                                XposedHelpers.setBooleanField(param.thisObject, "mDragging", true);
                                XposedHelpers.setFloatField(param.thisObject, "mInitialTouchPos", (float) XposedHelpers.callMethod(param.thisObject, "getPos" , ev));
                                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mTranslation", XposedHelpers.callMethod(param.thisObject, "getTranslation", currView));
                                XposedHelpers.callMethod(param.thisObject, "removeLongPressCallback");
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        final boolean captured = (XposedHelpers.getBooleanField(param.thisObject, "mDragging")
                                || XposedHelpers.getBooleanField(param.thisObject, "mLongPressSent"));
                        XposedHelpers.setBooleanField(param.thisObject, "mDragging", false);
                        XposedHelpers.setObjectField(param.thisObject, "mCurrView", null);
                        XposedHelpers.setBooleanField(param.thisObject, "mLongPressSent", false);
                        XposedHelpers.callMethod(param.thisObject, "removeLongPressCallback");
                        if (captured) return true;
                        break;
                }
                return XposedHelpers.getBooleanField(param.thisObject, "mDragging") || XposedHelpers.getBooleanField(param.thisObject, "mLongPressSent");
            }
        });*/
    }

    private float getPerpendicularPos(Object swipeHelper, MotionEvent ev) {
        return XposedHelpers.getIntField(swipeHelper, "mSwipeDirection") == 0 /*X*/ ? ev.getY() : ev.getX();
    }

    private void updateFirstAndLastBackgroundViews() {
        FrameLayout firstChild = getFirstChildWithBackground();
        FrameLayout lastChild = getLastChildWithBackground();
        if (mAnimationsEnabled && mIsExpanded) {
            mAnimateNextBackgroundTop = firstChild != mFirstVisibleBackgroundChild;
            mAnimateNextBackgroundBottom = lastChild != mLastVisibleBackgroundChild;
        } else {
            mAnimateNextBackgroundTop = false;
            mAnimateNextBackgroundBottom = false;
        }
        mFirstVisibleBackgroundChild = firstChild;
        mLastVisibleBackgroundChild = lastChild;
    }

    private void initView() {
        TAG_ANIMATOR_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_tag", "id", PACKAGE_SYSTEMUI);
        TAG_END_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_end_value_tag", "id", PACKAGE_SYSTEMUI);
        mBackgroundPaint.setColor(0xFFEEEEEE);
        mBackgroundPaint.setXfermode(mSrcMode);
        mStackScrollLayout.setWillNotDraw(false);
        mStackScrollLayout.setOnApplyWindowInsetsListener(NotificationStackScrollLayoutHooks.this);
        mStackScrollLayout.setFocusable(true);
    }

    private void updateBackground() {
        if ((boolean) XposedHelpers.callMethod(mAmbientState, "isDark")) {
            return;
        }
        updateBackgroundBounds();
        if (!mCurrentBounds.equals(mBackgroundBounds)) {
            if (mAnimateNextBackgroundTop || mAnimateNextBackgroundBottom || areBoundsAnimating()) {
                startBackgroundAnimation();
            } else {
                mCurrentBounds.set(mBackgroundBounds);
                applyCurrentBackgroundBounds();
            }
        } else {
            if (mBottomAnimator != null) {
                mBottomAnimator.cancel();
            }
            if (mTopAnimator != null) {
                mTopAnimator.cancel();
            }
        }
        mAnimateNextBackgroundBottom = false;
        mAnimateNextBackgroundTop = false;
    }

    private static void setBackgroundFadeAmount(float fadeAmount) {
        mBackgroundFadeAmount = fadeAmount;
        updateBackgroundDimming();
    }

    public static float getBackgroundFadeAmount() {
        return mBackgroundFadeAmount;
    }

    public void setFadingOut(boolean fadingOut) {
        if (fadingOut != mFadingOut) {
            mFadingOut = fadingOut;
            updateFadingState();
        }
    }

    public void setParentFadingOut(boolean fadingOut) {
        if (fadingOut != mParentFadingOut) {
            mParentFadingOut = fadingOut;
            updateFadingState();
        }
    }

    private void updateFadingState() {
        if (mFadingOut || mParentFadingOut || (boolean) XposedHelpers.callMethod(mAmbientState, "isDark")) {
            ScrimHelper.setExcludedBackgroundArea(null);
        } else {
            applyCurrentBackgroundBounds();
        }
        updateSrcDrawing();
    }

    public static void setDrawBackgroundAsSrc(boolean asSrc) {
        mDrawBackgroundAsSrc = asSrc;
        updateSrcDrawing();
    }

    private static void updateSrcDrawing() {
        mBackgroundPaint.setXfermode(mDrawBackgroundAsSrc && (!mFadingOut && !mParentFadingOut)
                ? mSrcMode : null);
        mStackScrollLayout.invalidate();
    }

    private void startBackgroundFadeIn() {
        int mDarkAnimationOriginIndex = XposedHelpers.getIntField(mStackScrollLayout, "mDarkAnimationOriginIndex");
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(mStackScrollLayout, BACKGROUND_FADE, 0f, 1f);
        int maxLength;
        if (mDarkAnimationOriginIndex == DARK_ANIMATION_ORIGIN_INDEX_ABOVE
                || mDarkAnimationOriginIndex == DARK_ANIMATION_ORIGIN_INDEX_BELOW) {
            maxLength = (int) XposedHelpers.callMethod(mStackScrollLayout, "getNotGoneChildCount") - 1;
        } else {
            maxLength = Math.max(mDarkAnimationOriginIndex,
                    (int) XposedHelpers.callMethod(mStackScrollLayout, "getNotGoneChildCount") - mDarkAnimationOriginIndex - 1);
        }
        maxLength = Math.max(0, maxLength);
        long delay = maxLength * ANIMATION_DELAY_PER_ELEMENT_DARK;
        fadeAnimator.setStartDelay(delay);
        fadeAnimator.setDuration(ANIMATION_DURATION_STANDARD);
        fadeAnimator.setInterpolator(Interpolators.ALPHA_IN);
        fadeAnimator.start();
    }

    private void startBackgroundAnimation() {
        mCurrentBounds.left = mBackgroundBounds.left;
        mCurrentBounds.right = mBackgroundBounds.right;
        startBottomAnimation();
        startTopAnimation();
    }

    private void startTopAnimation() {
        int previousEndValue = mEndAnimationRect.top;
        int newEndValue = mBackgroundBounds.top;
        ObjectAnimator previousAnimator = mTopAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (!mAnimateNextBackgroundTop) {
            // just a local update was performed
            if (previousAnimator != null) {
                // we need to increase all animation keyframes of the previous animator by the
                // relative change to the end value
                int previousStartValue = mStartAnimationRect.top;
                PropertyValuesHolder[] values = previousAnimator.getValues();
                values[0].setIntValues(previousStartValue, newEndValue);
                mStartAnimationRect.top = previousStartValue;
                mEndAnimationRect.top = newEndValue;
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            } else {
                // no new animation needed, let's just apply the value
                setBackgroundTop(newEndValue);
                return;
            }
        }
        if (previousAnimator != null) {
            previousAnimator.cancel();
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundTop",
                mCurrentBounds.top, newEndValue);
        Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
        animator.setInterpolator(interpolator);
        animator.setDuration(ANIMATION_DURATION_STANDARD);
        // remove the tag when the animation is finished
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mStartAnimationRect.top = -1;
                mEndAnimationRect.top = -1;
                mTopAnimator = null;
            }
        });
        animator.start();
        mStartAnimationRect.top = mCurrentBounds.top;
        mEndAnimationRect.top = newEndValue;
        mTopAnimator = animator;
    }

    private void startBottomAnimation() {
        int previousStartValue = mStartAnimationRect.bottom;
        int previousEndValue = mEndAnimationRect.bottom;
        int newEndValue = mBackgroundBounds.bottom;
        ObjectAnimator previousAnimator = mBottomAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (!mAnimateNextBackgroundBottom) {
            // just a local update was performed
            if (previousAnimator != null) {
                // we need to increase all animation keyframes of the previous animator by the
                // relative change to the end value
                PropertyValuesHolder[] values = previousAnimator.getValues();
                values[0].setIntValues(previousStartValue, newEndValue);
                mStartAnimationRect.bottom = previousStartValue;
                mEndAnimationRect.bottom = newEndValue;
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            } else {
                // no new animation needed, let's just apply the value
                setBackgroundBottom(newEndValue);
                return;
            }
        }
        if (previousAnimator != null) {
            previousAnimator.cancel();
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundBottom",
                mCurrentBounds.bottom, newEndValue);
        Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
        animator.setInterpolator(interpolator);
        animator.setDuration(ANIMATION_DURATION_STANDARD);
        // remove the tag when the animation is finished
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mStartAnimationRect.bottom = -1;
                mEndAnimationRect.bottom = -1;
                mBottomAnimator = null;
            }
        });
        animator.start();
        mStartAnimationRect.bottom = mCurrentBounds.bottom;
        mEndAnimationRect.bottom = newEndValue;
        mBottomAnimator = animator;
    }

    private void setBackgroundTop(int top) {
        mCurrentBounds.top = top;
        applyCurrentBackgroundBounds();
    }

    public void setBackgroundBottom(int bottom) {
        mCurrentBounds.bottom = bottom;
        applyCurrentBackgroundBounds();
    }

    private void applyCurrentBackgroundBounds() {
        if (!mFadingOut) {
            ScrimHelper.setExcludedBackgroundArea(mCurrentBounds);
        }

        mStackScrollLayout.invalidate();
    }

    private boolean areBoundsAnimating() {
        return mBottomAnimator != null || mTopAnimator != null;
    }

    private void updateBackgroundBounds() {
        mBackgroundBounds.left = (int) mStackScrollLayout.getX();
        mBackgroundBounds.right = (int) (mStackScrollLayout.getX() + mStackScrollLayout.getWidth());
        if (!mIsExpanded) {
            mBackgroundBounds.top = 0;
            mBackgroundBounds.bottom = 0;
        }
        FrameLayout firstView = mFirstVisibleBackgroundChild;
        int top = 0;
        if (firstView != null) {
            int finalTranslationY = (int) getFinalTranslationY(firstView);
            if (mAnimateNextBackgroundTop
                    || mTopAnimator == null && mCurrentBounds.top == finalTranslationY
                    || mTopAnimator != null && mEndAnimationRect.top == finalTranslationY) {
                // we're ending up at the same location as we are now, lets just skip the animation
                top = finalTranslationY;
            } else {
                top = (int) firstView.getTranslationY();
            }
        }
        FrameLayout lastView = mLastVisibleBackgroundChild;
        int bottom = 0;
        if (lastView != null) {
            int finalTranslationY = (int) getFinalTranslationY(lastView);
            int finalHeight = getFinalActualHeight(lastView);
            int finalBottom = finalTranslationY + finalHeight;
            finalBottom = Math.min(finalBottom, mStackScrollLayout.getHeight());
            if (mAnimateNextBackgroundBottom
                    || mBottomAnimator == null && mCurrentBounds.bottom == finalBottom
                    || mBottomAnimator != null && mEndAnimationRect.bottom == finalBottom) {
                // we're ending up at the same location as we are now, lets just skip the animation
                bottom = finalBottom;
            } else {
                bottom = (int) (lastView.getTranslationY() + (int) XposedHelpers.callMethod(lastView, "getActualHeight"));
                bottom = Math.min(bottom, mStackScrollLayout.getHeight());
            }
        } else {
            top = (int) (mTopPadding + mStackTranslation);
            bottom = top;
        }
        if (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD) {
            mBackgroundBounds.top = (int) Math.max(mTopPadding + mStackTranslation, top);
        } else {
            // otherwise the animation from the shade to the keyguard will jump as it's maxed
            mBackgroundBounds.top = Math.max(0, top);
        }
        mBackgroundBounds.bottom = Math.min(mStackScrollLayout.getHeight(), Math.max(bottom, top));
    }

    private FrameLayout getLastChildWithBackground() {
        int childCount = mStackScrollLayout.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE
                    && instanceOf(child, classActivatableNotificationView)) {
                return (FrameLayout) child;
            }
        }
        return null;
    }

    private FrameLayout getFirstChildWithBackground() {
        int childCount = mStackScrollLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE
                    && instanceOf(child, classActivatableNotificationView)) {
                return (FrameLayout) child;
            }
        }
        return null;
    }

    private void setAnimationRunning(boolean animationRunning) {
        if (animationRunning != mAnimationRunning) {
            if (animationRunning) {
                mStackScrollLayout.getViewTreeObserver().addOnPreDrawListener(mBackgroundUpdater);
            } else {
                mStackScrollLayout.getViewTreeObserver().removeOnPreDrawListener(mBackgroundUpdater);
            }
            mAnimationRunning = animationRunning;
            updateContinuousShadowDrawing();
        }
    }

    private static void updateBackgroundDimming() {
        float alpha = BACKGROUND_ALPHA_DIMMED + (1 - BACKGROUND_ALPHA_DIMMED) * (1.0f - mDimAmount);
        alpha *= mBackgroundFadeAmount;
        // We need to manually blend in the background color
        int scrimColor = ScrimHelper.getScrimBehindColor();
        // SRC_OVER blending Sa + (1 - Sa)*Da, Rc = Sc + (1 - Sa)*Dc
        float alphaInv = 1 - alpha;
        int color = Color.argb((int) (alpha * 255 + alphaInv * Color.alpha(scrimColor)),
                (int) (mBackgroundFadeAmount * Color.red(mBgColor)
                        + alphaInv * Color.red(scrimColor)),
                (int) (mBackgroundFadeAmount * Color.green(mBgColor)
                        + alphaInv * Color.green(scrimColor)),
                (int) (mBackgroundFadeAmount * Color.blue(mBgColor)
                        + alphaInv * Color.blue(scrimColor)));
        mBackgroundPaint.setColor(color);
        mStackScrollLayout.invalidate();
    }

    private void setDimAmount(float dimAmount) {
        mDimAmount = dimAmount;
        updateBackgroundDimming();
    }

    private void animateDimmed(boolean dimmed) {
        if (mDimAnimator != null) {
            mDimAnimator.cancel();
        }
        float target = dimmed ? 1.0f : 0.0f;
        if (target == mDimAmount) {
            return;
        }
        mDimAnimator = TimeAnimator.ofFloat(mDimAmount, target);
        mDimAnimator.setDuration(ANIMATION_DURATION_DIMMED_ACTIVATED);
        mDimAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mDimAnimator.addListener(mDimEndListener);
        mDimAnimator.addUpdateListener(mDimUpdateListener);
        mDimAnimator.start();
    }

    private void updateContinuousShadowDrawing() {
        boolean continuousShadowUpdate = mAnimationRunning
                || !mDraggedViews.isEmpty();
        if (continuousShadowUpdate != mContinuousShadowUpdate) {
            if (continuousShadowUpdate) {
                mStackScrollLayout.getViewTreeObserver().addOnPreDrawListener(mShadowUpdater);
            } else {
                mStackScrollLayout.getViewTreeObserver().removeOnPreDrawListener(mShadowUpdater);
            }
            mContinuousShadowUpdate = continuousShadowUpdate;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateViewShadows() {
        // we need to work around an issue where the shadow would not cast between siblings when
        // their z difference is between 0 and 0.1

        // Lefts first sort by Z difference
        for (int i = 0; i < mStackScrollLayout.getChildCount(); i++) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                mTmpSortedChildren.add(child);
            }
        }
        Collections.sort(mTmpSortedChildren, mViewPositionComparator);

        // Now lets update the shadow for the views
        View previous = null;
        for (int i = 0; i < mTmpSortedChildren.size(); i++) {
            View expandableView = mTmpSortedChildren.get(i);
            float translationZ = expandableView.getTranslationZ();
            float otherZ = previous == null ? translationZ : previous.getTranslationZ();
            float diff = otherZ - translationZ;
            if (diff <= 0.0f || diff >= FakeShadowView.SHADOW_SIBLING_TRESHOLD) {
                // There is no fake shadow to be drawn
                setFakeShadowIntensity(expandableView, 0.0f, 0.0f, 0, 0);
            } else {
                float yLocation = previous.getTranslationY() + XposedHelpers.getIntField(previous, "mActualHeight") -
                        expandableView.getTranslationY();
                setFakeShadowIntensity(expandableView,
                        diff / FakeShadowView.SHADOW_SIBLING_TRESHOLD,
                        ExpandableOutlineViewHelper.getInstance(previous).getOutlineAlpha(), (int) yLocation,
                        ExpandableOutlineViewHelper.getInstance(previous).getOutlineTranslation());
            }
            previous = expandableView;
        }

        mTmpSortedChildren.clear();
    }

    private void setFakeShadowIntensity(View activatableNotificationView, float shadowIntensity, float outlineAlpha, int shadowYEnd,
                                        int outlineTranslation) {
        FakeShadowView mFakeShadow = activatableNotificationView.findViewById(R.id.fake_shadow);
        if (mFakeShadow != null)
            mFakeShadow.setFakeShadowTranslationZ(shadowIntensity * (activatableNotificationView.getTranslationZ()
                            + FakeShadowView.SHADOW_SIBLING_TRESHOLD), outlineAlpha, shadowYEnd,
                    outlineTranslation);
    }

    public void requestDisallowDismiss() {
        mDisallowDismissInThisMotion = true;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getChildTag(View child, int tag) {
        return (T) child.getTag(tag);
    }

    private float getFinalTranslationY(View view) {
        if (view == null) {
            return 0;
        }
        ValueAnimator yAnimator = getChildTag(view, TAG_ANIMATOR_TRANSLATION_Y);
        if (yAnimator == null) {
            return view.getTranslationY();
        } else {
            return getChildTag(view, TAG_END_TRANSLATION_Y);
        }
    }

    private int getFinalActualHeight(View view) {
        return (int) XposedHelpers.callStaticMethod(classStackStateAnimator, "getFinalActualHeight", view);
    }

    private boolean instanceOf(Object obj, Class<?> objClass) {
        return objClass.isAssignableFrom(obj.getClass());
    }

    private void updateForcedScroll() {
        if (mForcedScroll != null && (!mForcedScroll.hasFocus()
                || !mForcedScroll.isAttachedToWindow())) {
            mForcedScroll = null;
        }
        if (mForcedScroll != null) {
            View expandableView = mForcedScroll;
            int positionInLinearLayout = getPositionInLinearLayout(expandableView);
            int targetScroll = targetScrollForView(expandableView, positionInLinearLayout);
            int outOfViewScroll = positionInLinearLayout + getIntrinsicHeight(expandableView);

            targetScroll = Math.max(0, Math.min(targetScroll, getScrollRange()));

            // Only apply the scroll if we're scrolling the view upwards, or the view is so far up
            // that it is not visible anymore.
            int mOwnScrollY = getOwnScrollY();
            if (mOwnScrollY < targetScroll || outOfViewScroll < mOwnScrollY) {
                setOwnScrollY(mOwnScrollY);
            }
        }
    }

    public void lockScrollTo(View v) {
        if (mForcedScroll == v) {
            return;
        }
        mForcedScroll = v;
        scrollTo(v);
    }

    private boolean scrollTo(View v) {
        int positionInLinearLayout = getPositionInLinearLayout(v);
        int targetScroll = targetScrollForView(v, positionInLinearLayout);
        int outOfViewScroll = positionInLinearLayout + getIntrinsicHeight(v);

        // Only apply the scroll if we're scrolling the view upwards, or the view is so far up
        // that it is not visible anymore.
        int mOwnScrollY = getOwnScrollY();
        if (mOwnScrollY < targetScroll || outOfViewScroll < mOwnScrollY) {
            mScroller.startScroll(getScrollX(), mOwnScrollY, 0, targetScroll - mOwnScrollY);
            dontReportNextOverScroll();
            mStackScrollLayout.postInvalidateOnAnimation();
            return true;
        }
        return false;
    }

    @Override
    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        mBottomInset = insets.getSystemWindowInsetBottom();

        int range = getScrollRange();
        if (getOwnScrollY() > range) {
            // HACK: We're repeatedly getting staggered insets here while the IME is
            // animating away. To work around that we'll wait until things have settled.
            mStackScrollLayout.removeCallbacks(mReclamp);
            mStackScrollLayout.postDelayed(mReclamp, 50);
        } else if (mForcedScroll != null) {
            // The scroll was requested before we got the actual inset - in case we need
            // to scroll up some more do so now.
            scrollTo(mForcedScroll);
        }
        return insets;
    }

    private int targetScrollForView(View v, int positionInLinearLayout) {
        return positionInLinearLayout + getIntrinsicHeight(v) +
                getImeInset() - mStackScrollLayout.getHeight() + mTopPadding;
    }

    private int getImeInset() {
        return Math.max(0, mBottomInset - (mStackScrollLayout.getRootView().getHeight() - mStackScrollLayout.getHeight()));
    }

    private int getScrollX() {
        return XposedHelpers.getIntField(mStackScrollLayout, "mScrollX");
    }

    private int getOwnScrollY() {
        return XposedHelpers.getIntField(mStackScrollLayout, "mOwnScrollY");
    }

    private void setOwnScrollY(int ownScrollY) {
        XposedHelpers.setIntField(mStackScrollLayout, "mOwnScrollY", ownScrollY);
    }

    private Runnable mReclamp = new Runnable() {
        @Override
        public void run() {
            int range = getScrollRange();
            int mOwnScrollY = getOwnScrollY();
            mScroller.startScroll(getScrollX(), mOwnScrollY, 0, range - mOwnScrollY);
            dontReportNextOverScroll();
            mDontClampNextScroll = true;
            mStackScrollLayout.postInvalidateOnAnimation();
        }
    };

    private void dontReportNextOverScroll() {
        XposedHelpers.setBooleanField(mStackScrollLayout, "mDontReportNextOverScroll", true);
    }

    private int getIntrinsicHeight(View v) {
        return (int) XposedHelpers.callMethod(v, "getIntrinsicHeight");
    }

    private int getPositionInLinearLayout(View v) {
        return (int) XposedHelpers.callMethod(mStackScrollLayout, "getPositionInLinearLayout", v);
    }

    private int getScrollRange() {
        return (int) XposedHelpers.callMethod(mStackScrollLayout, "getScrollRange");
    }
}
