package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableNotificationRowHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableOutlineViewHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationsStuff;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelViewHooks.getLayoutMinHeight;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.StackScrollAlgorithm.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.AmbientState.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ViewState.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.StackViewState.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.StackScrollAlgorithm.*;

public class StackScrollAlgorithmHooks {

    private static final String TAG = "StackScrollAlgorithmHooks";

    private static final int LOCATION_UNKNOWN = 0x00;
    private static final int LOCATION_FIRST_CARD = 0x01;
    private static final int LOCATION_TOP_STACK_HIDDEN = 0x02;
    private static final int LOCATION_MAIN_AREA = 0x08;

    private static final int MAX_ITEMS_IN_BOTTOM_STACK = 3;

    private static final String CHILD_NOT_FOUND_TAG = "StackScrollStateNoSuchChild";
    private static int TAG_START_HEIGHT;
    private static int TAG_END_HEIGHT;
    private static int TAG_ANIMATOR_HEIGHT;

    private static boolean temp;

    private static final Rect mClipBounds = new Rect();
    public static final HashMap<View, Float> increasedPaddingMap = new HashMap<>();
    public static ViewGroup mStackScrollLayout;
    private static Object mStackScrollAlgorithm;
    private static float mStackTop = 0;
    private static float mStateTop = 0;

    private static boolean mQsExpanded;

    private static int mLayoutMinHeight = 0;
    private static int mIncreasedPaddingBetweenElements;
    private static int mPaddingBetweenElements;
    private static float mAlpha;

    private static View tempChild;
    private static int tempCollapsedSize;

    private static Field fieldVisibleChildren;
    private static Field fieldScrollY;
    private static Field fieldShadeExpanded;
    private static Field fieldTopPadding;
    private static Field fieldBottomStackSlowDownLength;

    private static Field fieldBottomStackIndentationFunctor;
    private static Field fieldTempAlgorithmState;
    private static Field fieldZDistanceBetweenElements;
    private static Field fieldZBasicHeight;
    private static Field fieldItemsInBottomStack;
    private static Field fieldPartialInBottom;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldIsExpanded;
    private static Field fieldLayoutHeight;
    private static Field fieldCollapsedSize;
    private static Field fieldHostView;
    private static Field fieldStateMap;
    private static Field fieldClearAllTopPadding;
    private static Field fieldTopPaddingScroller;

    private static Method methodGetInnerHeight;
    private static Method methodGetScrollY;

    private static Method methodGetSpeedBumpIndex;
    private static Method methodGetTopPadding;
    private static Method methodGetStackTranslation;
    private static Method methodGetOverScrollAmount;

    private static Method methodGetMaxHeadsUpTranslation;
    private static Method methodPerformVisibilityAnimationDismissView;
    private static Method methodWillBeGoneDismissView;
    private static Method methodPerformVisibilityAnimationEmptyShade;
    private static Method methodWillBeGoneEmptyShade;
    private static Method methodSetTopPadding;

    public static void hook() {
        try {
            final ConfigUtils config = ConfigUtils.getInstance();

            XposedBridge.hookAllMethods(NotificationStackScrollLayout, "initView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mStackScrollLayout = (ViewGroup) param.thisObject;
                    mStackScrollAlgorithm = XposedHelpers.getObjectField(mStackScrollLayout, "mStackScrollAlgorithm");
                }
            });

            XposedHelpers.findAndHookMethod(StackScrollAlgorithm, "initConstants", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    ResourceUtils res = ResourceUtils.getInstance(context);
                    mPaddingBetweenElements = Math.max(1, res.getDimensionPixelSize(R.dimen.notification_divider_height));
                    set(Fields.SystemUI.StackScrollAlgorithm.mPaddingBetweenElements, param.thisObject, mPaddingBetweenElements);
                    mIncreasedPaddingBetweenElements = res.getDimensionPixelSize(R.dimen.notification_divider_height_increased);
                    int mZDistanceBetweenElements = ResourceUtils.getInstance((Context) param.args[0])
                            .getDimensionPixelSize(R.dimen.z_distance_between_notifications);
                    int mZBasicHeight = 4 * mZDistanceBetweenElements;
                    XposedHelpers.setIntField(param.thisObject, "mZDistanceBetweenElements", mZDistanceBetweenElements);
                    XposedHelpers.setIntField(param.thisObject, "mZBasicHeight", mZBasicHeight);
                    try { //Xperia
                        XposedHelpers.setIntField(param.thisObject, "mZBasicHeightNormal", mZBasicHeight);
                        XposedHelpers.setIntField(param.thisObject, "mZBasicHeightDimmed", mZBasicHeight);
                    } catch (Throwable ignore) {}
                }
            });

            if (!config.notifications.new_stack_scroll_algorithm) return;


            fieldCollapsedSize = XposedHelpers.findField(StackScrollAlgorithm, "mCollapsedSize");
            fieldVisibleChildren = XposedHelpers.findField(StackScrollAlgorithmState, "visibleChildren");
            fieldScrollY = XposedHelpers.findField(StackScrollAlgorithmState, "scrollY");
            fieldShadeExpanded = XposedHelpers.findField(AmbientState, "mShadeExpanded");
            fieldTopPadding = XposedHelpers.findField(AmbientState, "mTopPadding");
            fieldBottomStackSlowDownLength = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackSlowDownLength");

            fieldBottomStackIndentationFunctor = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackIndentationFunctor");
            fieldTempAlgorithmState = XposedHelpers.findField(StackScrollAlgorithm, "mTempAlgorithmState");
            fieldZDistanceBetweenElements = XposedHelpers.findField(StackScrollAlgorithm, "mZDistanceBetweenElements");
            fieldZBasicHeight = XposedHelpers.findField(StackScrollAlgorithm, "mZBasicHeight");
            fieldItemsInBottomStack = XposedHelpers.findField(StackScrollAlgorithmState, "itemsInBottomStack");
            fieldPartialInBottom = XposedHelpers.findField(StackScrollAlgorithmState, "partialInBottom");
            fieldBottomStackPeekSize = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackPeekSize");
            fieldIsExpanded = XposedHelpers.findField(StackScrollAlgorithm, "mIsExpanded");
            fieldLayoutHeight = XposedHelpers.findField(AmbientState, "mLayoutHeight");
            fieldHostView = XposedHelpers.findField(StackScrollState, "mHostView");
            fieldStateMap = XposedHelpers.findField(StackScrollState, "mStateMap");
            fieldClearAllTopPadding = XposedHelpers.findField(StackScrollState, "mClearAllTopPadding");

            fieldTopPaddingScroller = XposedHelpers.findField(NotificationStackScrollLayout, "mTopPadding");

            methodGetInnerHeight = XposedHelpers.findMethodBestMatch(AmbientState, "getInnerHeight");
            methodGetScrollY = XposedHelpers.findMethodBestMatch(AmbientState, "getScrollY");

            methodGetSpeedBumpIndex = XposedHelpers.findMethodBestMatch(AmbientState, "getSpeedBumpIndex");
            methodGetTopPadding = XposedHelpers.findMethodBestMatch(AmbientState, "getTopPadding");
            methodGetStackTranslation = XposedHelpers.findMethodBestMatch(AmbientState, "getStackTranslation");
            methodGetOverScrollAmount = XposedHelpers.findMethodBestMatch(AmbientState, "getOverScrollAmount", boolean.class);
            methodGetMaxHeadsUpTranslation = XposedHelpers.findMethodBestMatch(AmbientState, "getMaxHeadsUpTranslation");
            methodSetTopPadding = XposedHelpers.findMethodBestMatch(AmbientState, "setTopPadding", int.class);

            methodPerformVisibilityAnimationDismissView = XposedHelpers.findMethodBestMatch(DismissView, "performVisibilityAnimation", boolean.class);
            methodWillBeGoneDismissView = XposedHelpers.findMethodBestMatch(DismissView, "willBeGone");
            methodPerformVisibilityAnimationEmptyShade = XposedHelpers.findMethodBestMatch(EmptyShadeView, "performVisibilityAnimation", boolean.class);
            methodWillBeGoneEmptyShade = XposedHelpers.findMethodBestMatch(EmptyShadeView, "willBeGone");

            XposedHelpers.findAndHookMethod(StackStateAnimator, "startScaleAnimation", View.class, ViewState, long.class, XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookConstructor(StackStateAnimator, NotificationStackScrollLayout, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = ((View) param.args[0]).getContext();
                    TAG_START_HEIGHT = context.getResources().getIdentifier("height_animator_start_value_tag", "id", XposedHook.PACKAGE_SYSTEMUI);
                    TAG_END_HEIGHT = context.getResources().getIdentifier("height_animator_end_value_tag", "id", XposedHook.PACKAGE_SYSTEMUI);
                    TAG_ANIMATOR_HEIGHT = context.getResources().getIdentifier("height_animator_tag", "id", XposedHook.PACKAGE_SYSTEMUI);
                }
            });
            XposedHelpers.findAndHookMethod(StackStateAnimator, "startHeightAnimation", ExpandableView, StackViewState, long.class, long.class, XStartHeightAnimation);

            XposedHelpers.findAndHookMethod(StackScrollState, "apply", apply);

            XposedHelpers.findAndHookMethod(AmbientState, "getInnerHeight", getInnerHeight);

            XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "updateAlgorithmHeightAndPadding", updateAlgorithmHeightAndPadding);
            XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
            XposedHelpers.findAndHookMethod(NotificationPanelView, "updateQsState", updateQsStateHook); //TODO: see if this is really needed

            XposedHelpers.findAndHookMethod(StackScrollAlgorithm, "notifyChildrenChanged", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "getStackScrollState", getStackScrollState);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "clampPositionToTopStackEnd", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateFirstChildMaxSizeToMaxHeight", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "onExpansionStarted", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "onExpansionStopped", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateFirstChildHeightWhileExpanding", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateIsSmallScreen", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateStateForChildFullyInBottomStack", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    set(Fields.SystemUI.StackScrollAlgorithm.mPaddingBetweenElements, param.thisObject, getPaddingAfterChild(param.args[0], tempChild));
//                    Object childViewState = param.args[2];
//                    mAlpha = fieldAlpha.getFloat(childViewState);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object algorithmState = param.args[0];
                    Object childViewState = param.args[2];
                    int collapsedHeight = (int) param.args[3];
                    /*float itemsInBottomStack = getFloat(fieldItemsInBottomStack, algorithmState);
                    if (itemsInBottomStack >= MAX_ITEMS_IN_BOTTOM_STACK) {
                        if (itemsInBottomStack > MAX_ITEMS_IN_BOTTOM_STACK + 2) {
                            XposedHelpers.setAdditionalInstanceField(childViewState, "hidden", true);
                            XposedHelpers.setAdditionalInstanceField(childViewState, "shadowAlpha", 0.0f);
                        } else if (itemsInBottomStack > MAX_ITEMS_IN_BOTTOM_STACK + 1) {
                            XposedHelpers.setAdditionalInstanceField(childViewState, "shadowAlpha", 1.0f - (float) fieldPartialInBottom.get(algorithmState));
                        }
                    }*/
                    set(height, childViewState, collapsedHeight);
                    set(Fields.SystemUI.StackScrollAlgorithm.mPaddingBetweenElements, mStackScrollAlgorithm, mPaddingBetweenElements);
                    //setFloat(fieldAlpha, childViewState, mAlpha);
                }
            });

            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateStateForChildTransitioningInBottom", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    set(mIsSmallScreen, param.thisObject, true);
                    set(Fields.SystemUI.StackScrollAlgorithm.mPaddingBetweenElements, param.thisObject, getPaddingAfterChild(param.args[0], tempChild));
                    tempCollapsedSize = get(fieldCollapsedSize, param.thisObject);
                    set(fieldCollapsedSize, param.thisObject, ExpandableOutlineViewHelper.getInstance(tempChild).getCollapsedHeight());
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    set(fieldCollapsedSize, mStackScrollAlgorithm, tempCollapsedSize);
                    set(Fields.SystemUI.StackScrollAlgorithm.mPaddingBetweenElements, mStackScrollAlgorithm, mPaddingBetweenElements);
                }
            });

            /*XposedBridge.hookAllMethods(StackViewState, "copyFrom", copyFromHook);
            XposedBridge.hookAllMethods(StackScrollState, "resetViewState", resetViewStateHook);*/
            XposedBridge.hookAllMethods(StackScrollState, "applyState", applyState);
            XposedBridge.hookAllMethods(StackScrollState, "resetViewStates", resetViewStates);


        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking StackScrollAlgorithm", t);
        }
    }

    private static void setLayoutMinHeight(int layoutMinHeight) {
        mLayoutMinHeight = layoutMinHeight;
    }

    public static void updateAlgorithmLayoutMinHeight() {
        if (RomUtils.isOneplusStock()) {
            return;
        }
        setLayoutMinHeight(mQsExpanded && !isOnKeyguard() ? getLayoutMinHeight() : 0);
    }

    private static void setQsExpanded(boolean qsExpanded) {
        mQsExpanded = qsExpanded;
        updateAlgorithmLayoutMinHeight();
    }

    private static final XC_MethodHook updateQsStateHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            setQsExpanded(getBoolean(Fields.SystemUI.NotificationPanelView.mQsExpanded, param.thisObject));
        }
    };

    private static final XC_MethodReplacement getInnerHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mAmbientState = param.thisObject;
            int mLayoutHeight = getInt(fieldLayoutHeight, mAmbientState);
            int mTopPadding = getInt(fieldTopPadding, mAmbientState);
            return Math.max(mLayoutHeight - mTopPadding, mLayoutMinHeight);
        }
    };

    private static final XC_MethodReplacement updateAlgorithmHeightAndPadding = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            //replacing it rather than hooking it because it's different on Xperias
            Object stackScroller = param.thisObject;
            Object mAmbientState = get(Fields.SystemUI.NotificationStackScrollLayout.mAmbientState, stackScroller);
            int mMaxLayoutHeight = getInt(Fields.SystemUI.NotificationStackScrollLayout.mMaxLayoutHeight, stackScroller);
            int mCurrentStackHeight = getInt(Fields.SystemUI.NotificationStackScrollLayout.mCurrentStackHeight, stackScroller);
            int minLayoutHeight = Math.min(mMaxLayoutHeight, mCurrentStackHeight);
            invoke(setLayoutHeight, mAmbientState, minLayoutHeight);
            updateAlgorithmLayoutMinHeight();
            invoke(methodSetTopPadding, mAmbientState, getInt(fieldTopPaddingScroller, stackScroller));
            return null;
        }
    };

    private static final XC_MethodHook onLayoutHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            updateAlgorithmLayoutMinHeight();
        }
    };

    private static final XC_MethodReplacement getStackScrollState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            // The state of the local variables are saved in an algorithmState to easily subdivide it
            // into multiple phases.
            Object ambientState = param.args[0];
            Object resultState = param.args[1];
            Object algorithmState = get(fieldTempAlgorithmState, mStackScrollAlgorithm);

            // First we reset the view states to their default values.
            invoke(SystemUI.StackScrollState.resetViewStates, resultState);

            initAlgorithmState(resultState, algorithmState, ambientState);
            updatePositionsForState(resultState, algorithmState, ambientState);
            updateZValuesForState(resultState, algorithmState, ambientState);
            updateHeadsUpStates(resultState, algorithmState, ambientState);
            invoke(SystemUI.StackScrollAlgorithm.handleDraggedViews, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            invoke(SystemUI.StackScrollAlgorithm.updateDimmedActivatedHideSensitive, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            updateClipping(resultState, algorithmState, ambientState);
            invoke(SystemUI.StackScrollAlgorithm.updateSpeedBumpState, mStackScrollAlgorithm, resultState, algorithmState, invoke(methodGetSpeedBumpIndex, ambientState));
            invoke(SystemUI.StackScrollAlgorithm.getNotificationChildrenStates, mStackScrollAlgorithm, resultState, algorithmState);
            return null;
        }
    };

    private static final XC_MethodHook copyFromHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object viewState = param.args[0];
            if (StackViewState.isInstance(viewState)) {
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "isBottomClipped", XposedHelpers.getAdditionalInstanceField(viewState, "isBottomClipped"));
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "shadowAlpha", XposedHelpers.getAdditionalInstanceField(viewState, "shadowAlpha"));
            }
        }
    };

    private static final XC_MethodHook resetViewStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            View view = (View) param.args[0];
            Map mStateMap = get(fieldStateMap, param.thisObject);
            Object viewState = mStateMap.get(view);
            XposedHelpers.setAdditionalInstanceField(viewState, "shadowAlpha",  1f);
            XposedHelpers.setAdditionalInstanceField(viewState, "hidden",  false);
        }
    };

    private static final XC_MethodReplacement resetViewStates = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            ViewGroup mHostView = get(fieldHostView, param.thisObject);
            int numChildren = mHostView.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                Object child = mHostView.getChildAt(i);
                invoke(SystemUI.StackScrollState.resetViewState, param.thisObject, child);

                // handling reset for child notifications
                if (ExpandableNotificationRow.isInstance(child)) {
                    Object row = child;
                    List children = invoke(SystemUI.ExpandableNotificationRow.getNotificationChildren, row);
                    if (NotificationsStuff.isSummaryWithChildren(row) && children != null) {
                        for (Object childRow : children) {
                            invoke(SystemUI.StackScrollState.resetViewState, param.thisObject, childRow);
                        }
                    }
                }
            }
            return null;
        }
    };

    private static final XC_MethodReplacement applyState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View view = (View) param.args[0];
            Object state = param.args[1];
            //ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(view);
            if (state == null) {
                XposedHook.logW(CHILD_NOT_FOUND_TAG, "No child state was found when applying this state " +
                        "to the hostView");
                return false;
            }
            if (getBoolean(gone, state)) {
                return false;
            }
            invoke(SystemUI.StackScrollState.applyViewState, param.thisObject, view, state);

            int currentHeight = invoke(SystemUI.ExpandableView.getActualHeight, view);
            int newHeight = getInt(height, state);

            // apply height
            if (currentHeight != newHeight) {
                invoke(SystemUI.ExpandableView.setActualHeight, view, newHeight, false /* notifyListeners */);
            }

            /*float shadowAlpha = helper.getShadowAlpha();
            float newShadowAlpha = (float) XposedHelpers.getAdditionalInstanceField(state, "shadowAlpha");

            // apply shadowAlpha
            if (shadowAlpha != newShadowAlpha) {
                helper.setShadowAlpha(newShadowAlpha);
            }*/

            // apply dimming
            invoke(SystemUI.ExpandableView.setDimmed, view, get(dimmed, state), false /* animate */);

            // apply hiding sensitive
            invoke(SystemUI.ExpandableView.setHideSensitive, view, get(hideSensitive, state), false /* animated */, 0 /* delay */, 0 /* duration */);

            // apply speed bump state
            invoke(SystemUI.ExpandableView.setBelowSpeedBump, view, get(belowSpeedBump, state));

            // apply dark
            invoke(SystemUI.ExpandableView.setDark, view, get(dark, state), false /* animate */, 0 /* delay */);

            // apply clipping
            float oldClipTopAmount = (int) invoke(SystemUI.ExpandableView.getClipTopAmount, view);
            if (oldClipTopAmount != (int) get(clipTopAmount, state)) {
                invoke(SystemUI.ExpandableView.setClipTopAmount, view, get(clipTopAmount, state));
            }

            if (ExpandableNotificationRow.isInstance(view)) {
                Object row = view;
                /*Object isBottomClipped = XposedHelpers.getAdditionalInstanceField(state, "isBottomClipped");
                if ((isBottomClipped != null) && (boolean) isBottomClipped) {
                    ExpandableNotificationRowHelper.getInstance(row).setClipToActualHeight(true);
                }*/
                invoke(SystemUI.ExpandableNotificationRow.applyChildrenState, row, param.thisObject);
            }
            return true;
        }
    };

    private static final XC_MethodReplacement apply = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object stackScrollState = param.thisObject;
            ViewGroup mHostView = get(fieldHostView, stackScrollState);
            HashMap<?, ?> mStateMap = get(fieldStateMap, stackScrollState);
            int numChildren = mHostView.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                Object child = mHostView.getChildAt(i);
                Object state = mStateMap.get(child);
                if (!(boolean) invoke(SystemUI.StackScrollState.applyState, stackScrollState, child, state)) {
                    continue;
                }
                if (DismissView.isInstance(child)) {
                    boolean willBeGone = invoke(methodWillBeGoneDismissView, child);
                    boolean visible = getInt(clipTopAmount, state) < getInt(fieldClearAllTopPadding, stackScrollState);
                    invoke(methodPerformVisibilityAnimationDismissView, child, visible && !willBeGone);
                } else if (EmptyShadeView.isInstance(child)) {
                    boolean willBeGone = invoke(methodWillBeGoneEmptyShade, child);
                    boolean visible = getInt(clipTopAmount, state) <= 0;
                    invoke(methodPerformVisibilityAnimationEmptyShade, child,
                            visible && !willBeGone);
                }
            }
            return null;
        }
    };

    private static final XC_MethodReplacement XStartHeightAnimation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object stackAnimator = param.thisObject;
            final View child = (View) param.args[0];
            Object viewState = param.args[1];
            long duration = (long) param.args[2];
            long delay = (long) param.args[3];

            Integer previousStartValue = getChildTag(child, TAG_START_HEIGHT);
            Integer previousEndValue = getChildTag(child, TAG_END_HEIGHT);
            int newEndValue = get(Fields.SystemUI.StackViewState.height, viewState);
            if (previousEndValue != null && previousEndValue == newEndValue) {
                return null;
            }
            ValueAnimator previousAnimator = getChildTag(child, TAG_ANIMATOR_HEIGHT);
            if (!getBoolean(Fields.SystemUI.AnimationFilter.animateHeight, get(Fields.SystemUI.StackStateAnimator.mAnimationFilter, stackAnimator))) {
                // just a local update was performed
                if (previousAnimator != null) {
                    // we need to increase all animation keyframes of the previous animator by the
                    // relative change to the end value
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    int relativeDiff = newEndValue - previousEndValue;
                    int newStartValue = previousStartValue + relativeDiff;
                    values[0].setIntValues(newStartValue, newEndValue);
                    child.setTag(TAG_START_HEIGHT, newStartValue);
                    child.setTag(TAG_END_HEIGHT, newEndValue);
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return null;
                } else {
                    // no new animation needed, let's just apply the value
                    invoke(SystemUI.ExpandableView.setActualHeight, child, newEndValue, false);
                    return null;
                }
            }

            ValueAnimator animator = ValueAnimator.ofInt((int) invoke(SystemUI.ExpandableView.getActualHeight, child), newEndValue);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    invoke(SystemUI.ExpandableView.setActualHeight, child, animation.getAnimatedValue(),
                            false /* notifyListeners */);
                }
            });
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            long newDuration = invoke(SystemUI.StackStateAnimator.cancelAnimatorAndGetNewDuration, stackAnimator, duration, previousAnimator);
            animator.setDuration(newDuration);
            if (delay > 0 && (previousAnimator == null
                    || previousAnimator.getAnimatedFraction() == 0)) {
                animator.setStartDelay(delay);
            }
            animator.addListener((Animator.AnimatorListener) invoke(SystemUI.StackStateAnimator.getGlobalAnimationFinishedListener, stackAnimator));
            // remove the tag when the animation is finished
            animator.addListener(new AnimatorListenerAdapter() {
                boolean mWasCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(TAG_ANIMATOR_HEIGHT, null);
                    child.setTag(TAG_START_HEIGHT, null);
                    child.setTag(TAG_END_HEIGHT, null);
                    setActualHeightAnimating(child, false);
                    if (!mWasCancelled && ExpandableNotificationRow.isInstance(child)) {
                        ExpandableNotificationRowHelper.getInstance(child).setGroupExpansionChanging(
                                false /* isExpansionChanging */);
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
            invoke(SystemUI.StackStateAnimator.startAnimator, stackAnimator, animator);
            child.setTag(TAG_ANIMATOR_HEIGHT, animator);
            child.setTag(TAG_START_HEIGHT, invoke(SystemUI.ExpandableView.getActualHeight, child));
            child.setTag(TAG_END_HEIGHT, newEndValue);
            setActualHeightAnimating(child, true);

            return null;
        }
    };

    private static void updateClipping(Object resultState,
                                      Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = get(fieldVisibleChildren, algorithmState);

        float drawStart = (float) invoke(methodGetTopPadding, ambientState) + (float) invoke(methodGetStackTranslation, ambientState);

        float previousNotificationEnd = 0;
        float previousNotificationStart = 0;

        int childCount = visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object state = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, child);
            Object headsUp = get(Fields.SystemUI.ExpandableNotificationRow.mIsHeadsUp, child);
            boolean mIsHeadsUp = headsUp != null && (boolean) headsUp;
            boolean isTransparent = invoke(SystemUI.ExpandableView.isTransparent, child);
            if (!mIsHeadsUp) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = getFloat(yTranslation, state);
            float newHeight = getInt(height, state);

            float newNotificationEnd = newYTranslation + newHeight;
            boolean isHeadsUp = (ExpandableNotificationRow.isInstance(child))
                    && (boolean) invoke(SystemUI.ExpandableNotificationRow.isPinned, child);
            if (newYTranslation < previousNotificationEnd
                    && (!isHeadsUp || getBoolean(fieldShadeExpanded, ambientState))) {
                // The previous view is overlapping on top, clip!
                float overlapAmount = previousNotificationEnd - newYTranslation;
                set(clipTopAmount, state, (int) overlapAmount);
            } else {
                set(clipTopAmount, state, 0);
            }
            if (!isTransparent) {
                // Only update the previous values if we are not transparent,
                // otherwise we would clip to a transparent view.
                previousNotificationEnd = newNotificationEnd;
                previousNotificationStart = newYTranslation;

            }
        }
    }

    private static void updateZValuesForState(Object resultState,
                                              Object algorithmState, Object ambientState) {

        int mZDistanceBetweenElements = getInt(fieldZDistanceBetweenElements, mStackScrollAlgorithm);
        int mZBasicHeight = getInt(fieldZBasicHeight, mStackScrollAlgorithm);
        float itemsInBottomStack = getFloat(fieldItemsInBottomStack, algorithmState);
        List<ViewGroup> visibleChildren = get(fieldVisibleChildren, algorithmState);
        int childCount = visibleChildren.size();
        float childrenOnTop = 0.0f;
        for (int i = childCount - 1; i >= 0; i--) {
            View child = visibleChildren.get(i);
            Object isHeadsUp = get(Fields.SystemUI.ExpandableNotificationRow.mIsHeadsUp, child);
            boolean mIsHeadsUp = isHeadsUp != null && (boolean) isHeadsUp;
            Object childViewState = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, child);
            float yTranslation = getFloat(Fields.SystemUI.ViewState.yTranslation, childViewState);
            if (i > (childCount - 1 - itemsInBottomStack)) {
                // We are in the bottom stack
                float numItemsAbove = i - (childCount - 1 - itemsInBottomStack);
                float zSubtraction;
                if (numItemsAbove <= 1.0f) {
                    float factor = 0.2f;
                    // Lets fade in slower to the threshold to make the shadow fade in look nicer
                    if (numItemsAbove <= factor) {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                * numItemsAbove * (1.0f / factor);
                    } else {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                + (numItemsAbove - factor) * (1.0f / (1.0f - factor))
                                * (mZDistanceBetweenElements
                                - FakeShadowView.SHADOW_SIBLING_TRESHOLD);
                    }
                } else {
                    zSubtraction = numItemsAbove * mZDistanceBetweenElements;
                }
                set(zTranslation, childViewState, mZBasicHeight - zSubtraction);
            } else if (mIsHeadsUp
                    && yTranslation < (float) invoke(methodGetTopPadding, ambientState)
                    + (float) invoke(methodGetStackTranslation, ambientState)) {
                if (childrenOnTop != 0.0f) {
                    childrenOnTop++;
                } else {
                    float overlap = (float) invoke(methodGetTopPadding, ambientState)
                            + (float) invoke(methodGetStackTranslation, ambientState) - getFloat(Fields.SystemUI.ViewState.yTranslation, childViewState);
                    childrenOnTop += Math.min(1.0f, overlap / (getInt(height, childViewState)));
                }
                set(zTranslation, childViewState, mZBasicHeight
                        + childrenOnTop * mZDistanceBetweenElements);
            } else {
                set(zTranslation, childViewState, mZBasicHeight);
            }
        }
    }

    private static void initAlgorithmState(Object resultState, Object state,
                                           Object ambientState) {

        set(fieldItemsInBottomStack, state, 0.0f);
        set(fieldPartialInBottom, state, 0.0f);
        float bottomOverScroll = invoke(methodGetOverScrollAmount, ambientState, false /* onTop */);

        int scrollY = invoke(methodGetScrollY, ambientState);

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        set(fieldScrollY, state, (int) (scrollY + bottomOverScroll));

        //now init the visible children and update paddings
        ViewGroup hostView = invoke(SystemUI.StackScrollState.getHostView, resultState);
        int childCount = hostView.getChildCount();
        ArrayList visibleChildren = get(fieldVisibleChildren, state);
        visibleChildren.clear();
        visibleChildren.ensureCapacity(childCount);
        increasedPaddingMap.clear();
        int notGoneIndex = 0;
        View lastView = null;

        for (int i = 0; i < childCount; i++) {
            View v = hostView.getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                notGoneIndex = invoke(updateNotGoneIndex, mStackScrollAlgorithm, resultState, state, notGoneIndex, v);
                float increasedPadding = NotificationStackScrollLayoutHooks.getIncreasedPaddingAmount(v);
                if (increasedPadding != 0.0f) {
                    increasedPaddingMap.put(v, increasedPadding);
                    if (lastView != null) {
                        Float prevValue = increasedPaddingMap.get(lastView);
                        float newValue = prevValue != null
                                ? Math.max(prevValue, increasedPadding)
                                : increasedPadding;
                        increasedPaddingMap.put(lastView, newValue);
                    }
                }
                if (ExpandableNotificationRow.isInstance(v)) {
                    View row = v;

                    // handle the notgoneIndex for the children as well
                    List<View> children =
                            invoke(SystemUI.ExpandableNotificationRow.getNotificationChildren, row);
                    if (NotificationsStuff.isSummaryWithChildren(row) && children != null) {
                        for (View childRow : children) {
                            if (childRow.getVisibility() != View.GONE) {
                                Object childState
                                        = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, childRow);
                                set(Fields.SystemUI.StackViewState.notGoneIndex, childState, notGoneIndex);
                                notGoneIndex++;
                            }
                        }
                    }
                }
                lastView = v;
            }
        }
    }

    private static void clampPositionToBottomStackStart(Object childViewState,
                                                        int childHeight, int minHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);
        int bottomStackStart = (int) invoke(methodGetInnerHeight, ambientState)
                - mBottomStackPeekSize - mBottomStackSlowDownLength;
        int childStart = bottomStackStart - childHeight;
        if (childStart < getFloat(yTranslation, childViewState)) {
            float newHeight = bottomStackStart - getFloat(yTranslation, childViewState);
            if (newHeight < minHeight) {
                newHeight = minHeight;
                set(yTranslation, childViewState, bottomStackStart - minHeight);
            }
            set(height, childViewState, (int) newHeight);
        }
    }

    private static void updateFirstChildHeight(View child, Object childViewState, int childHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        int bottomPeekStart = (int) invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize -
                mBottomStackSlowDownLength + (int) invoke(methodGetScrollY, ambientState);

        // Collapse and expand the first child while the shade is being expanded

        set(height, childViewState, (int) Math.max(Math.min(bottomPeekStart, (float) childHeight),
                ExpandableOutlineViewHelper.getInstance(child).getCollapsedHeight()));
    }

    private static void updatePositionsForState(Object resultState, Object algorithmState, Object ambientState) {

        List<?> visibleChildren = get(fieldVisibleChildren, algorithmState);

        if (mStackScrollAlgorithm == null) return;

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        float bottomPeekStart = (int) invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize;

        // The position where the bottom stack starts.
        float bottomStackStart = bottomPeekStart - mBottomStackSlowDownLength;

        // The y coordinate of the current child.
        float currentYPosition = (float)-(getInt(fieldScrollY, algorithmState));

        int childCount = visibleChildren.size();
        int paddingAfterChild;

        for (int i = 0; i < childCount; i++) {
            View child = (View) visibleChildren.get(i);
            tempChild = child;
            Object childViewState = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, child);
            set(location, childViewState, LOCATION_UNKNOWN);
            paddingAfterChild = getPaddingAfterChild(algorithmState, child);
            int childHeight = getMaxAllowedChildHeight(child);
            int collapsedHeight = ExpandableOutlineViewHelper.getInstance(child).getCollapsedHeight();
            set(yTranslation, childViewState, currentYPosition);
            if (i == 0) {
                updateFirstChildHeight(child, childViewState, childHeight, ambientState);
            }
            // The y position after this element
            float nextYPosition = currentYPosition + childHeight +
                    paddingAfterChild;
            if (nextYPosition >= bottomStackStart) {
                // Case 1:
                // We are in the bottom stack.
                if (currentYPosition >= bottomStackStart) {
                    // According to the regular scroll view we are fully translated out of the
                    // bottom of the screen so we are fully in the bottom stack
                    invoke(SystemUI.StackScrollAlgorithm.updateStateForChildFullyInBottomStack, mStackScrollAlgorithm, algorithmState,
                            bottomStackStart, childViewState, collapsedHeight, ambientState);
                } else {
                    // According to the regular scroll view we are currently translating out of /
                    // into the bottom of the screen
                    invoke(SystemUI.StackScrollAlgorithm.updateStateForChildTransitioningInBottom, mStackScrollAlgorithm, algorithmState,
                            bottomStackStart, bottomPeekStart, currentYPosition,
                            childViewState, childHeight);
                }
            } else {
                // Case 2:
                // We are in the regular scroll area.
                set(location, childViewState, LOCATION_MAIN_AREA);
                clampPositionToBottomStackStart(childViewState, (getInt(height, childViewState)), childHeight,
                        ambientState);
            }
            if (i == 0 && (int) invoke(methodGetScrollY, ambientState) <= 0) {
                // The first card can get into the bottom stack if it's the only one
                // on the lockscreen which pushes it up. Let's make sure that doesn't happen and
                // it stays at the top
                set(yTranslation, childViewState, Math.max(0, getFloat(yTranslation, childViewState)));
            }
            currentYPosition = getFloat(yTranslation, childViewState) + childHeight + paddingAfterChild;
            if (currentYPosition <= 0) {
                set(location, childViewState, LOCATION_TOP_STACK_HIDDEN);
            }
            if (getInt(location, childViewState) == LOCATION_UNKNOWN) {
                XposedHook.logW(TAG, "Failed to assign location for child " + i);
            }
            float yTranslation = getFloat(Fields.SystemUI.ViewState.yTranslation, childViewState);
            set(Fields.SystemUI.ViewState.yTranslation, childViewState, yTranslation + (float) invoke(methodGetTopPadding, ambientState)
                    + (float) invoke(methodGetStackTranslation, ambientState));
        }
    }

    private static void updateHeadsUpStates(Object resultState,
                                            Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = get(fieldVisibleChildren, algorithmState);
        boolean mIsExpanded = getBoolean(fieldIsExpanded, mStackScrollAlgorithm);
        int childCount = visibleChildren.size();
        Object topHeadsUpEntry = null;
        for (int i = 0; i < childCount; i++) {
            View child = visibleChildren.get(i);
            if (!(ExpandableNotificationRow.isInstance(child))) {
                break;
            }
            Object row = child;
            boolean isHeadsUp = getBoolean(Fields.SystemUI.ExpandableNotificationRow.mIsHeadsUp, row);
            if (!isHeadsUp) {
                break;
            }
            Object childState = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, row);
            if (topHeadsUpEntry == null) {
                topHeadsUpEntry = row;
                set(location, childState, LOCATION_FIRST_CARD);
            }
            boolean isTopEntry = topHeadsUpEntry == row;
            float unmodifiedEndLocation = getFloat(yTranslation, childState) + getInt(height, childState);
            if (mIsExpanded) {
                // Ensure that the heads up is always visible even when scrolled off
                clampHunToTop(ambientState, row, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            boolean isPinned = invoke(SystemUI.ExpandableNotificationRow.isPinned, row);
            if (isPinned) {
                set(yTranslation, childState, Math.max(getFloat(yTranslation, childState), 0));
                int height = invoke(SystemUI.ExpandableNotificationRow.getHeadsUpHeight, row);
                set(Fields.SystemUI.StackViewState.height, childState, height);
                Object topState = invoke(SystemUI.StackScrollState.getViewStateForView, resultState, topHeadsUpEntry);
                if (!isTopEntry && (!mIsExpanded
                        || unmodifiedEndLocation < getFloat(yTranslation, topState) + getInt(Fields.SystemUI.StackViewState.height, topState))) {
                    // Ensure that a headsUp doesn't vertically extend further than the heads-up at
                    // the top most z-position
                    set(Fields.SystemUI.StackViewState.height, childState, invoke(SystemUI.ExpandableNotificationRow.getHeadsUpHeight, row));
                    set(yTranslation, childState, getFloat(yTranslation, topState) + getInt(Fields.SystemUI.StackViewState.height, topState)
                            - getInt(Fields.SystemUI.StackViewState.height, childState));
                }
            }
        }
    }

    private static int getMaxAllowedChildHeight(View child) {
        if (ExpandableView.isInstance(child)) {
            return (int) invoke(SystemUI.ExpandableView.getIntrinsicHeight, child);
        }
        return child == null ? getInt(fieldCollapsedSize, mStackScrollAlgorithm) : child.getHeight();
    }

    private static void clampHunToTop(Object ambientState, Object row,
                                      Object childState) {
        float newTranslation = Math.max(((float) invoke(methodGetTopPadding, ambientState)
                + (float) invoke(methodGetStackTranslation, ambientState)), getFloat(yTranslation, childState));
        set(height, childState, (int) Math.max(getInt(height, childState) - (newTranslation
                - getFloat(yTranslation, childState)), ExpandableNotificationRowHelper.getInstance(row).getCollapsedHeight()));
        set(yTranslation, childState, newTranslation);
    }

    private static void clampHunToMaxTranslation(Object ambientState, Object row,
                                                 Object childState) {
        ExpandableNotificationRowHelper helper = ExpandableNotificationRowHelper.getInstance(row);
        float newTranslation;
        float bottomPosition = (float) invoke(methodGetMaxHeadsUpTranslation, ambientState) - helper.getCollapsedHeight();
        newTranslation = Math.min(getFloat(yTranslation, childState), bottomPosition);
        set(height, childState, (int) Math.max(getInt(height, childState)
                - (getFloat(yTranslation, childState) - newTranslation), helper.getCollapsedHeight()));
        set(yTranslation, childState, newTranslation);
    }

    public static <T> T getChildTag(View child, int tag) {
        return (T) child.getTag(tag);
    }

    private static void setActualHeightAnimating(Object view, boolean animating) {
        if (!ExpandableNotificationRow.isInstance(view))
            return;
        ExpandableNotificationRowHelper.getInstance(view).setActualHeightAnimating(animating);
    }

    private static int getPaddingAfterChild(Object algorithmState,
                                     View child) {
        Float paddingValue = increasedPaddingMap.get(child);
        int paddingBetweenElements = mPaddingBetweenElements;
        return paddingValue == null
                ? paddingBetweenElements
                : (int) NotificationUtils.interpolate(paddingBetweenElements,
                mIncreasedPaddingBetweenElements,
                paddingValue);
    }
}
