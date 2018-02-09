package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

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
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelViewHooks.getLayoutMinHeight;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.*;

public class StackScrollAlgorithmHooks {

    private static final String TAG = "StackScrollAlgorithmHooks";

    private static final int LOCATION_UNKNOWN = 0x00;
    private static final int LOCATION_FIRST_CARD = 0x01;
    private static final int LOCATION_TOP_STACK_HIDDEN = 0x02;
    private static final int LOCATION_MAIN_AREA = 0x08;

    private static final int MAX_ITEMS_IN_BOTTOM_STACK = 3;

    private static final String CHILD_NOT_FOUND_TAG = "StackScrollStateNoSuchChild";

    private static final Rect mClipBounds = new Rect();
    public static ViewGroup mStackScrollLayout;
    private static Object mStackScrollAlgorithm;
    private static float mStackTop = 0;
    private static float mStateTop = 0;

    private static boolean mQsExpanded;

    private static int mLayoutMinHeight = 0;
    private static float mAlpha;

    private static Field fieldVisibleChildren;
    private static Field fieldScrollY;
    private static Field fieldShadeExpanded;
    private static Field fieldTopPadding;
    private static Field fieldYTranslation;
    private static Field fieldLocation;
    private static Field fieldHeight;
    private static Field fieldBelowSpeedBump;
    private static Field fieldHideSensitive;
    private static Field fieldDimmed;
    private static Field fieldDark;
    private static Field fieldAlpha;
    private static Field fieldBottomStackSlowDownLength;
    private static Field fieldPaddingBetweenElements;

    private static Field fieldBottomStackIndentationFunctor;
    private static Field fieldTempAlgorithmState;
    private static Field fieldZDistanceBetweenElements;
    private static Field fieldZBasicHeight;
    private static Field fieldIsHeadsUp;
    private static Field fieldClipTopAmount;
    private static Field fieldGone;
    private static Field fieldItemsInBottomStack;
    private static Field fieldPartialInBottom;
    private static Field fieldZTranslation;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldIsExpanded;
    private static Field fieldLayoutHeight;
    private static Field fieldCollapsedSize;
    private static Field fieldHostView;
    private static Field fieldStateMap;
    private static Field fieldClearAllTopPadding;
    private static Field fieldAmbientState;
    private static Field fieldMaxLayoutHeight;
    private static Field fieldCurrentStackHeight;
    private static Field fieldTopPaddingScroller;

    private static Method methodGetViewStateForView;
    private static Method methodGetInnerHeight;
    private static Method methodGetScrollY;

    private static Method methodGetSpeedBumpIndex;
    private static Method methodGetTopPadding;
    private static Method methodGetStackTranslation;
    private static Method methodIsPinned;
    private static Method methodResetViewStates;
    private static Method methodGetOverScrollAmount;

    private static Method methodGetHeadsUpHeight;
    private static Method methodGetMaxHeadsUpTranslation;
    private static Method methodApplyState;
    private static Method methodApplyViewState;
    private static Method methodPerformVisibilityAnimationDismissView;
    private static Method methodWillBeGoneDismissView;
    private static Method methodPerformVisibilityAnimationEmptyShade;
    private static Method methodWillBeGoneEmptyShade;
    private static Method methodSetLayoutHeight;
    private static Method methodSetTopPadding;
    private static Method methodApplyChildrenState;


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
            fieldYTranslation = XposedHelpers.findField(StackViewState, "yTranslation");
            fieldLocation = XposedHelpers.findField(StackViewState, "location");
            fieldHeight = XposedHelpers.findField(StackViewState, "height");
            fieldBelowSpeedBump = XposedHelpers.findField(StackViewState, "belowSpeedBump");
            fieldHideSensitive = XposedHelpers.findField(StackViewState, "hideSensitive");
            fieldDimmed = XposedHelpers.findField(StackViewState, "dimmed");
            fieldDark = XposedHelpers.findField(StackViewState, "dark");
            fieldAlpha = XposedHelpers.findField(StackViewState, "alpha");
            fieldBottomStackSlowDownLength = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackSlowDownLength");
            fieldPaddingBetweenElements = XposedHelpers.findField(StackScrollAlgorithm, "mPaddingBetweenElements");

            fieldBottomStackIndentationFunctor = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackIndentationFunctor");
            fieldTempAlgorithmState = XposedHelpers.findField(StackScrollAlgorithm, "mTempAlgorithmState");
            fieldZDistanceBetweenElements = XposedHelpers.findField(StackScrollAlgorithm, "mZDistanceBetweenElements");
            fieldZBasicHeight = XposedHelpers.findField(StackScrollAlgorithm, "mZBasicHeight");
            fieldZTranslation = XposedHelpers.findField(StackViewState, "zTranslation");
            fieldIsHeadsUp = XposedHelpers.findField(ExpandableNotificationRow, "mIsHeadsUp");
            fieldClipTopAmount = XposedHelpers.findField(StackViewState, "clipTopAmount");
            fieldGone = XposedHelpers.findField(StackViewState, "gone");
            fieldItemsInBottomStack = XposedHelpers.findField(StackScrollAlgorithmState, "itemsInBottomStack");
            fieldPartialInBottom = XposedHelpers.findField(StackScrollAlgorithmState, "partialInBottom");
            fieldBottomStackPeekSize = XposedHelpers.findField(StackScrollAlgorithm, "mBottomStackPeekSize");
            fieldIsExpanded = XposedHelpers.findField(StackScrollAlgorithm, "mIsExpanded");
            fieldLayoutHeight = XposedHelpers.findField(AmbientState, "mLayoutHeight");
            fieldHostView = XposedHelpers.findField(StackScrollState, "mHostView");
            fieldStateMap = XposedHelpers.findField(StackScrollState, "mStateMap");
            fieldClearAllTopPadding = XposedHelpers.findField(StackScrollState, "mClearAllTopPadding");

            fieldAmbientState = XposedHelpers.findField(NotificationStackScrollLayout, "mAmbientState");
            fieldMaxLayoutHeight = XposedHelpers.findField(NotificationStackScrollLayout, "mMaxLayoutHeight");
            fieldCurrentStackHeight = XposedHelpers.findField(NotificationStackScrollLayout, "mCurrentStackHeight");
            fieldTopPaddingScroller = XposedHelpers.findField(NotificationStackScrollLayout, "mTopPadding");

            methodGetViewStateForView = XposedHelpers.findMethodBestMatch(StackScrollState, "getViewStateForView", View.class);
            methodGetInnerHeight = XposedHelpers.findMethodBestMatch(AmbientState, "getInnerHeight");
            methodGetScrollY = XposedHelpers.findMethodBestMatch(AmbientState, "getScrollY");

            methodGetSpeedBumpIndex = XposedHelpers.findMethodBestMatch(AmbientState, "getSpeedBumpIndex");
            methodGetTopPadding = XposedHelpers.findMethodBestMatch(AmbientState, "getTopPadding");
            methodGetStackTranslation = XposedHelpers.findMethodBestMatch(AmbientState, "getStackTranslation");
            methodGetOverScrollAmount = XposedHelpers.findMethodBestMatch(AmbientState, "getOverScrollAmount", boolean.class);
            methodGetMaxHeadsUpTranslation = XposedHelpers.findMethodBestMatch(AmbientState, "getMaxHeadsUpTranslation");
            methodSetLayoutHeight = XposedHelpers.findMethodBestMatch(AmbientState, "setLayoutHeight", int.class);
            methodSetTopPadding = XposedHelpers.findMethodBestMatch(AmbientState, "setTopPadding", int.class);

            methodIsPinned = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "isPinned");
            methodGetHeadsUpHeight = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "getHeadsUpHeight");
            methodApplyChildrenState = XposedHelpers.findMethodBestMatch(ExpandableNotificationRow, "applyChildrenState", StackScrollState);

            methodResetViewStates = XposedHelpers.findMethodBestMatch(StackScrollState, "resetViewStates");

            methodApplyState = XposedHelpers.findMethodBestMatch(StackScrollState, "applyState", ExpandableView, StackViewState);
            methodApplyViewState = XposedHelpers.findMethodBestMatch(StackScrollState, "applyViewState", View.class, ViewState);
            methodPerformVisibilityAnimationDismissView = XposedHelpers.findMethodBestMatch(DismissView, "performVisibilityAnimation", boolean.class);
            methodWillBeGoneDismissView = XposedHelpers.findMethodBestMatch(DismissView, "willBeGone");
            methodPerformVisibilityAnimationEmptyShade = XposedHelpers.findMethodBestMatch(EmptyShadeView, "performVisibilityAnimation", boolean.class);
            methodWillBeGoneEmptyShade = XposedHelpers.findMethodBestMatch(EmptyShadeView, "willBeGone");

            XposedHelpers.findAndHookMethod(StackScrollState, "apply", apply);

            XposedHelpers.findAndHookMethod(AmbientState, "getInnerHeight", getInnerHeight);

            XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "updateAlgorithmHeightAndPadding", updateAlgorithmHeightAndPadding);
            XposedHelpers.findAndHookMethod(NotificationStackScrollLayout, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
            XposedHelpers.findAndHookMethod(NotificationPanelView, "updateQsState", updateQsStateHook); //TODO: see if this is really needed

            XposedBridge.hookAllMethods(StackScrollAlgorithm, "getStackScrollState", getStackScrollState);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "clampPositionToTopStackEnd", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateFirstChildMaxSizeToMaxHeight", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "onExpansionStarted", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateFirstChildHeightWhileExpanding", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateIsSmallScreen", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "onExpansionStopped", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "notifyChildrenChanged", XC_MethodReplacement.DO_NOTHING);
            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateStateForChildFullyInBottomStack", new XC_MethodHook() {
                /*@Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object childViewState = param.args[2];
                    mAlpha = fieldAlpha.getFloat(childViewState);
                }*/

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
                    set(fieldHeight, childViewState, collapsedHeight);
                    //setFloat(fieldAlpha, childViewState, mAlpha);
                }
            });

            XposedBridge.hookAllMethods(StackScrollAlgorithm, "updateStateForChildTransitioningInBottom", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setBooleanField(param.thisObject, "mIsSmallScreen", true);
                }
            });

            /*XposedBridge.hookAllMethods(StackViewState, "copyFrom", copyFromHook);
            XposedBridge.hookAllMethods(StackScrollState, "resetViewState", resetViewStateHook);*/
            XposedBridge.hookAllMethods(StackScrollState, "applyState", applyState);


        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking StackScrollAlgorithm", t);
        }
    }

    private static void setLayoutMinHeight(int layoutMinHeight) {
        mLayoutMinHeight = layoutMinHeight;
    }

    private static void updateAlgorithmLayoutMinHeight() {
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
            Object mAmbientState = get(fieldAmbientState, stackScroller);
            int mMaxLayoutHeight = getInt(fieldMaxLayoutHeight, stackScroller);
            int mCurrentStackHeight = getInt(fieldCurrentStackHeight, stackScroller);
            int minLayoutHeight = Math.min(mMaxLayoutHeight, mCurrentStackHeight);
            invoke(methodSetLayoutHeight, mAmbientState, minLayoutHeight);
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
            invoke(methodResetViewStates, resultState);

            initAlgorithmState(algorithmState, ambientState);
            invoke(SystemUI.StackScrollAlgorithm.updateVisibleChildren, mStackScrollAlgorithm, resultState, algorithmState);
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
            if (getBoolean(fieldGone, state)) {
                return false;
            }
            invoke(methodApplyViewState, param.thisObject, view, state);

            int height = invoke(SystemUI.ExpandableView.getActualHeight, view);
            int newHeight = getInt(fieldHeight, state);

            // apply height
            if (height != newHeight) {
                invoke(SystemUI.ExpandableView.setActualHeight, view, newHeight, false /* notifyListeners */);
            }

            /*float shadowAlpha = helper.getShadowAlpha();
            float newShadowAlpha = (float) XposedHelpers.getAdditionalInstanceField(state, "shadowAlpha");

            // apply shadowAlpha
            if (shadowAlpha != newShadowAlpha) {
                helper.setShadowAlpha(newShadowAlpha);
            }*/

            // apply dimming
            invoke(SystemUI.ExpandableView.setDimmed, view, get(fieldDimmed, state), false /* animate */);

            // apply hiding sensitive
            invoke(SystemUI.ExpandableView.setHideSensitive, view, get(fieldHideSensitive, state), false /* animated */, 0 /* delay */, 0 /* duration */);

            // apply speed bump state
            invoke(SystemUI.ExpandableView.setBelowSpeedBump, view, get(fieldBelowSpeedBump, state));

            // apply dark
            invoke(SystemUI.ExpandableView.setDark, view, get(fieldDark, state), false /* animate */, 0 /* delay */);

            // apply clipping
            float oldClipTopAmount = (int) invoke(SystemUI.ExpandableView.getClipTopAmount, view);
            if (oldClipTopAmount != (int) get(fieldClipTopAmount, state)) {
                invoke(SystemUI.ExpandableView.setClipTopAmount, view, get(fieldClipTopAmount, state));
            }

            if (ExpandableNotificationRow.isInstance(view)) {
                Object row = view;
                /*Object isBottomClipped = XposedHelpers.getAdditionalInstanceField(state, "isBottomClipped");
                if ((isBottomClipped != null) && (boolean) isBottomClipped) {
                    ExpandableNotificationRowHelper.getInstance(row).setClipToActualHeight(true);
                }*/
                invoke(methodApplyChildrenState, row, param.thisObject);
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
                if (!(boolean) invoke(methodApplyState, stackScrollState, child, state)) {
                    continue;
                }
                if (DismissView.isInstance(child)) {
                    boolean willBeGone = invoke(methodWillBeGoneDismissView, child);
                    boolean visible = getInt(fieldClipTopAmount, state) < getInt(fieldClearAllTopPadding, stackScrollState);
                    invoke(methodPerformVisibilityAnimationDismissView, child, visible && !willBeGone);
                } else if (EmptyShadeView.isInstance(child)) {
                    boolean willBeGone = invoke(methodWillBeGoneEmptyShade, child);
                    boolean visible = getInt(fieldClipTopAmount, state) <= 0;
                    invoke(methodPerformVisibilityAnimationEmptyShade, child,
                            visible && !willBeGone);
                }
            }
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
            Object state = invoke(methodGetViewStateForView, resultState, child);
            Object headsUp = get(fieldIsHeadsUp, child);
            boolean mIsHeadsUp = headsUp != null && (boolean) headsUp;
            boolean isTransparent = invoke(SystemUI.ExpandableView.isTransparent, child);
            if (!mIsHeadsUp) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = getFloat(fieldYTranslation, state);
            float newHeight = getInt(fieldHeight, state);

            float newNotificationEnd = newYTranslation + newHeight;
            boolean isHeadsUp = (ExpandableNotificationRow.isInstance(child))
                    && (boolean) invoke(methodIsPinned, child);
            if (newYTranslation < previousNotificationEnd
                    && (!isHeadsUp || getBoolean(fieldShadeExpanded, ambientState))) {
                // The previous view is overlapping on top, clip!
                float overlapAmount = previousNotificationEnd - newYTranslation;
                set(fieldClipTopAmount, state, (int) overlapAmount);
            } else {
                set(fieldClipTopAmount, state, 0);
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
            Object isHeadsUp = get(fieldIsHeadsUp, child);
            boolean mIsHeadsUp = isHeadsUp != null && (boolean) isHeadsUp;
            Object childViewState = invoke(methodGetViewStateForView, resultState, child);
            float yTranslation = getFloat(fieldYTranslation, childViewState);
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
                set(fieldZTranslation, childViewState, mZBasicHeight - zSubtraction);
            } else if (mIsHeadsUp
                    && yTranslation < (float) invoke(methodGetTopPadding, ambientState)
                    + (float) invoke(methodGetStackTranslation, ambientState)) {
                if (childrenOnTop != 0.0f) {
                    childrenOnTop++;
                } else {
                    float overlap = (float) invoke(methodGetTopPadding, ambientState)
                            + (float) invoke(methodGetStackTranslation, ambientState) - getFloat(fieldYTranslation, childViewState);
                    childrenOnTop += Math.min(1.0f, overlap / (getInt(fieldHeight, childViewState)));
                }
                set(fieldZTranslation, childViewState, mZBasicHeight
                        + childrenOnTop * mZDistanceBetweenElements);
            } else {
                set(fieldZTranslation, childViewState, mZBasicHeight);
            }
        }
    }

    private static void initAlgorithmState(Object state,
                                           Object ambientState) {

        set(fieldItemsInBottomStack, state, 0.0f);
        set(fieldPartialInBottom, state, 0.0f);
        float bottomOverScroll = invoke(methodGetOverScrollAmount, ambientState, false /* onTop */);

        int scrollY = invoke(methodGetScrollY, ambientState);

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        set(fieldScrollY, state, (int) (scrollY + bottomOverScroll));
    }

    private static void clampPositionToBottomStackStart(Object childViewState,
                                                        int childHeight, int minHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);
        int bottomStackStart = (int) invoke(methodGetInnerHeight, ambientState)
                - mBottomStackPeekSize - mBottomStackSlowDownLength;
        int childStart = bottomStackStart - childHeight;
        if (childStart < getFloat(fieldYTranslation, childViewState)) {
            float newHeight = bottomStackStart - getFloat(fieldYTranslation, childViewState);
            if (newHeight < minHeight) {
                newHeight = minHeight;
                set(fieldYTranslation, childViewState, bottomStackStart - minHeight);
            }
            set(fieldHeight, childViewState, (int) newHeight);
        }
    }

    private static void updateFirstChildHeight(Object childViewState, int childHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        int bottomPeekStart = (int) invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize -
                mBottomStackSlowDownLength + (int) invoke(methodGetScrollY, ambientState);

        // Collapse and expand the first child while the shade is being expanded

        set(fieldHeight, childViewState, (int) Math.max(Math.min(bottomPeekStart, (float) childHeight),
                getInt(fieldCollapsedSize, mStackScrollAlgorithm)));
    }

    private static void updatePositionsForState(Object resultState, Object algorithmState, Object ambientState) {

        int collapsedHeight = getInt(fieldCollapsedSize, mStackScrollAlgorithm);

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
        int paddingAfterChild = getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm);

        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object childViewState = invoke(methodGetViewStateForView, resultState, child);
            set(fieldLocation, childViewState, LOCATION_UNKNOWN);
            int childHeight = getMaxAllowedChildHeight((View) child);
            set(fieldYTranslation, childViewState, currentYPosition);
            if (i == 0) {
                updateFirstChildHeight(childViewState, childHeight, ambientState);
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
                set(fieldLocation, childViewState, LOCATION_MAIN_AREA);
                clampPositionToBottomStackStart(childViewState, (getInt(fieldHeight, childViewState)), childHeight,
                        ambientState);
            }
            if (i == 0 && (int) invoke(methodGetScrollY, ambientState) <= 0) {
                // The first card can get into the bottom stack if it's the only one
                // on the lockscreen which pushes it up. Let's make sure that doesn't happen and
                // it stays at the top
                set(fieldYTranslation, childViewState, Math.max(0, getFloat(fieldYTranslation, childViewState)));
            }
            currentYPosition = getFloat(fieldYTranslation, childViewState) + childHeight + paddingAfterChild;
            if (currentYPosition <= 0) {
                set(fieldLocation, childViewState, LOCATION_TOP_STACK_HIDDEN);
            }
            if (getInt(fieldLocation, childViewState) == LOCATION_UNKNOWN) {
                XposedHook.logW(TAG, "Failed to assign location for child " + i);
            }
            float yTranslation = getFloat(fieldYTranslation, childViewState);
            set(fieldYTranslation, childViewState, yTranslation + (float) invoke(methodGetTopPadding, ambientState)
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
            boolean isHeadsUp = getBoolean(fieldIsHeadsUp, row);
            if (!isHeadsUp) {
                break;
            }
            Object childState = invoke(methodGetViewStateForView, resultState, row);
            if (topHeadsUpEntry == null) {
                topHeadsUpEntry = row;
                set(fieldLocation, childState, LOCATION_FIRST_CARD);
            }
            boolean isTopEntry = topHeadsUpEntry == row;
            float unmodifiedEndLocation = getFloat(fieldYTranslation, childState) + getInt(fieldHeight, childState);
            if (mIsExpanded) {
                // Ensure that the heads up is always visible even when scrolled off
                clampHunToTop(ambientState, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            boolean isPinned = invoke(methodIsPinned, row);
            if (isPinned) {
                set(fieldYTranslation, childState, Math.max(getFloat(fieldYTranslation, childState), 0));
                int height = invoke(methodGetHeadsUpHeight, row);
                set(fieldHeight, childState, height);
                Object topState = invoke(methodGetViewStateForView, resultState, topHeadsUpEntry);
                if (!isTopEntry && (!mIsExpanded
                        || unmodifiedEndLocation < getFloat(fieldYTranslation, topState) + getInt(fieldHeight, topState))) {
                    // Ensure that a headsUp doesn't vertically extend further than the heads-up at
                    // the top most z-position
                    set(fieldHeight, childState, invoke(methodGetHeadsUpHeight, row));
                    set(fieldYTranslation, childState, getFloat(fieldYTranslation, topState) + getInt(fieldHeight, topState)
                            - getInt(fieldHeight, childState));
                }
            }
        }
    }

    private static int getMaxAllowedChildHeight(View child) {
        if (ExpandableView.isInstance(child)) {
            return (int) invoke(SystemUI.ExpandableView.getIntrinsicHeight, child);
        }
        return child == null ? getInt(fieldCollapsedSize, mStackScrollAlgorithm) : (int) invoke(SystemUI.ExpandableView.getHeight, child);
    }

    private static void clampHunToTop(Object ambientState,
                                      Object childState) {
        float newTranslation = Math.max(((float) invoke(methodGetTopPadding, ambientState)
                + (float) invoke(methodGetStackTranslation, ambientState)), getFloat(fieldYTranslation, childState));
        set(fieldHeight, childState, (int) Math.max(getInt(fieldHeight, childState) - (newTranslation
                - getFloat(fieldYTranslation, childState)), getInt(fieldCollapsedSize, mStackScrollAlgorithm)));
        set(fieldYTranslation, childState, newTranslation);
    }

    private static void clampHunToMaxTranslation(Object ambientState, Object row,
                                                 Object childState) {
        float newTranslation;
        float bottomPosition = (float) invoke(methodGetMaxHeadsUpTranslation, ambientState) - getInt(fieldCollapsedSize, mStackScrollAlgorithm);
        newTranslation = Math.min(getFloat(fieldYTranslation, childState), bottomPosition);
        set(fieldHeight, childState, Math.max(getInt(fieldHeight, childState), (int) invoke(methodGetHeadsUpHeight, row)));
        set(fieldYTranslation, childState, newTranslation);
    }
}
