package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.AlphaOptimizedImageView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NonInterceptingScrollView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSAnimator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSDetail;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSFooter;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QuickQSPanel;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.misc.SafeRunnable;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.StackScrollAlgorithmHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.DetailViewManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.KeyguardMonitor;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSDetailItemsHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.QuickSettingsHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.BluetoothTileHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.CellularTileHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.DndTileHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.WifiTileHook;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

@SuppressLint("StaticFieldLeak")
public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = CLASS_STATUS_BAR_HEADER_VIEW + "$LayoutValues";
    private static final String CLASS_QS_DETAIL_ITEMS = "com.android.systemui.qs.QSDetailItems";
    private static final String CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel";
    private static final String CLASS_QS_DRAG_PANEL = "com.android.systemui.qs.QSDragPanel";
    private static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    private static final String CLASS_QS_STATE = CLASS_QS_TILE + "$State";
    private static final String CLASS_QS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    private static final String CLASS_DETAIL_ADAPTER = CLASS_QS_TILE + "$DetailAdapter";

    private static boolean mCollapseAfterHideDetails = false;
    private static boolean mHideTunerIcon = false;
    private static boolean mHideEditTiles = false;
    private static boolean mHideCarrierLabel = false;

    private static Constructor mCarrierTextConstructor;
    private static Constructor mStatusBarClockConstructor;

    public static ViewGroup mStatusBarHeaderView;

    private static View mSystemIconsSuperContainer;
    private static ViewGroup mSystemIcons;
    private static View mDateGroup;
    private static View mBattery;
    private static AlphaOptimizedImageView mEdit;
    private static FrameLayout mMultiUserSwitch;
    private static View mClock;
    private static TextView mDateCollapsed;
    private static TextView mDateExpanded;
    private static ImageButton mSettingsButton;
    private static View mSettingsContainer;
    private static View mQsDetailHeader;
    private static TextView mQsDetailHeaderTitle;
    private static Switch mQsDetailHeaderSwitch;
    private static Button mAlarmStatus;
    private static TextView mEditTileDoneText;
    private static View mTunerIcon;
    private static LinearLayout mWeatherContainer;
    private static LinearLayout mBatteryContainer;
    private static LinearLayout mTopContainer;
    private static View mTaskManagerButton;
    private static View mCustomQSEditButton;
    private static View mCustomQSEditButton2;
    private static TextView mCarrierText;
    private static View mOriginalCarrierText;
    private static TextView mBatteryLevel;

    private static ExpandableIndicator mExpandIndicator;
    private static LinearLayout mDateTimeAlarmGroup;
    private static LinearLayout mRightContainer;
    private static ImageView mAlarmStatusCollapsed;

    private static QuickQSPanel mHeaderQsPanel;
    public static ViewGroup mQsPanel;
    private static ViewGroup mQsContainer;
    private static QSFooter mQsFooter;

    public static Context mContext;
    private static ResourceUtils mResUtils;
    private static View mCurrentDetailView;

    private static ImageView mQsRightButton;

    private static boolean mHasEditPanel = false;
    public static boolean mShowingDetail;
    public static boolean mUseDragPanel = false;

    public static boolean mExpanded;
    private static float mExpansion = 0;
    private static float mAlarmStatusX;
    private static boolean mRecreatingStatusBar = false;

    private static final ArrayList<String> mPreviousTiles = new ArrayList<>();
    public static ArrayList<Object> mRecords;

    private static final Rect mClipBounds = new Rect();

    public static QSAnimator mQsAnimator;
    public static QuickSettingsHooks qsHooks;

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "onFinishInflateHook called");

            mStatusBarHeaderView = (ViewGroup) param.thisObject;
            mContext = mStatusBarHeaderView.getContext();
            mResUtils = ResourceUtils.getInstance(mContext);
            ResourceUtils res = mResUtils;
            ConfigUtils config = ConfigUtils.getInstance();

            try {
                if (!config.qs.keep_header_background) {
                    //noinspection deprecation
                    mStatusBarHeaderView.setBackgroundColor(config.qs.fix_header_space ? 0 :
                            mContext.getResources().getColor(mContext.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't change header background color", t);
            }

            TextView mEmergencyCallsOnly;
            TextView mWeatherLine1 = null;
            TextView mWeatherLine2 = null;
            try {
                mSystemIconsSuperContainer = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSystemIconsSuperContainer");
                mSystemIcons = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSystemIcons");
                mBattery = mSystemIcons.findViewById(mContext.getResources().getIdentifier("battery", "id", PACKAGE_SYSTEMUI));
                mBatteryLevel = (TextView) mSystemIconsSuperContainer.findViewById(mContext.getResources().getIdentifier("battery_level", "id", PACKAGE_SYSTEMUI));
                mDateGroup = (View) XposedHelpers.getObjectField(param.thisObject, "mDateGroup");
                mClock = (View) XposedHelpers.getObjectField(param.thisObject, "mClock");
                mMultiUserSwitch = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch");
                mDateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                mDateExpanded = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateExpanded");
                mSettingsButton = (ImageButton) XposedHelpers.getObjectField(param.thisObject, "mSettingsButton");
                mQsDetailHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeader");
                mQsDetailHeaderTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderTitle");
                mQsDetailHeaderSwitch = (Switch) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderSwitch");
                mEmergencyCallsOnly = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEmergencyCallsOnly");
                mAlarmStatus = (Button) XposedHelpers.getObjectField(param.thisObject, "mAlarmStatus");
                if (mHasEditPanel) {
                    mEditTileDoneText = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEditTileDoneText");
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't find required views, aborting", t);
                return;
            }
            mAlarmStatus.setOnClickListener(null);
            // Separate try-catch for settings button as some ROMs removed the container around it
            try {
                mSettingsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsContainer");
            } catch (Throwable t) {
                mSettingsContainer = mSettingsButton;
            }
            mTunerIcon = mSettingsContainer.findViewById(mContext.getResources().getIdentifier("tuner_icon", "id", PACKAGE_SYSTEMUI));
            mHideTunerIcon = config.qs.hide_tuner_icon;
            mHideEditTiles = config.qs.hide_edit_tiles;
            mHideCarrierLabel = config.qs.hide_carrier_label;
            mCarrierText = (TextView) mCarrierTextConstructor.newInstance(mContext);
            try {
                mWeatherContainer = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mWeatherContainer");
                mWeatherLine1 = (TextView) XposedHelpers.getObjectField(param.thisObject, "mWeatherLine1");
                mWeatherLine2 = (TextView) XposedHelpers.getObjectField(param.thisObject, "mWeatherLine2");
            } catch (Throwable ignore) {
            }
            try {
                mOriginalCarrierText = (View) XposedHelpers.getObjectField(param.thisObject, "mCarrierText");
            } catch (Throwable ignore) {
            }
            try {
                mTaskManagerButton = (View) XposedHelpers.getObjectField(param.thisObject, "mTaskManagerButton");
            } catch (Throwable ignore) {
            }
            try { // Sony
                mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mSomcQuickSettings");
            } catch (Throwable i) {
                try { // PA
                    mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mQsAddButton");
                } catch (Throwable g) {
                    try { // OOS2 & 3
                        mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mEditModeButton");
                        mCustomQSEditButton2 = (View) XposedHelpers.getObjectField(param.thisObject, "mResetButton");
                        XposedHelpers.setObjectField(param.thisObject, "mEditModeButton", new ImageView(mContext));
                    } catch (Throwable ignore) {
                    }
                }
            }
            try { // PA seems to screw around a bit (the image has the bg color and overlaps the expand indicator ripple)
                ((View) XposedHelpers.getObjectField(param.thisObject, "mBackgroundImage")).setVisibility(View.GONE);
            } catch (Throwable ignore) {
            }

            try {

                boolean mShowTaskManager = true;
                try {
                    mShowTaskManager = XposedHelpers.getBooleanField(param.thisObject, "mShowTaskManager");
                } catch (Throwable ignore) {
                }

                int rippleRes = mContext.getResources().getIdentifier("ripple_drawable", "drawable", XposedHook.PACKAGE_SYSTEMUI);
                int rightIconHeight = res.getDimensionPixelSize(R.dimen.right_icon_size);
                int rightIconWidth = mTaskManagerButton != null && mShowTaskManager ? res.getDimensionPixelSize(R.dimen.right_icon_width_small) : rightIconHeight;
                int expandIndicatorPadding = res.getDimensionPixelSize(R.dimen.expand_indicator_padding);
                int headerItemsMarginTop = res.getDimensionPixelSize(R.dimen.header_items_margin_top);
                int dateTimeMarginStart = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_start);
                int dateTimePadding = res.getDimensionPixelSize(R.dimen.date_time_group_padding);
                int elevation = res.getDimensionPixelSize(R.dimen.qs_container_elevation);
                int topContainerPadding = res.getDimensionPixelSize(R.dimen.qs_top_container_padding);
                float qsTimeExpandedSize = res.getDimension(R.dimen.qs_time_expanded_size);

                Drawable alarmSmall = mContext.getDrawable(mContext.getResources().getIdentifier("ic_access_alarms_small", "drawable", XposedHook.PACKAGE_SYSTEMUI));

                ((ViewGroup) mClock.getParent()).removeView(mClock);
                ((ViewGroup) mMultiUserSwitch.getParent()).removeView(mMultiUserSwitch);
                ((ViewGroup) mDateCollapsed.getParent()).removeView(mDateCollapsed);
                ((ViewGroup) mSettingsContainer.getParent()).removeView(mSettingsContainer);
                ((ViewGroup) mAlarmStatus.getParent()).removeView(mAlarmStatus);
                ((ViewGroup) mEmergencyCallsOnly.getParent()).removeView(mEmergencyCallsOnly);
                ((ViewGroup) mBattery.getParent()).removeView(mBattery);
                ((ViewGroup) mBatteryLevel.getParent()).removeView(mBatteryLevel);
                createEditButton(rightIconHeight, rightIconWidth);

                mSettingsButton.setImageDrawable(res.getDrawable(R.drawable.ic_settings_16dp));

                LinearLayout.LayoutParams rightContainerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                rightContainerLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_end));
                rightContainerLp.setMarginStart(dateTimeMarginStart);
                mRightContainer = new LinearLayout(mContext);
                mRightContainer.setLayoutParams(rightContainerLp);
                mRightContainer.setGravity(Gravity.END);
                mRightContainer.setOrientation(LinearLayout.HORIZONTAL);
                mRightContainer.setClipChildren(false);

                LinearLayout.LayoutParams multiUserSwitchLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mMultiUserSwitch.setLayoutParams(multiUserSwitchLp);

                LinearLayout.LayoutParams settingsContainerLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mSettingsContainer.setLayoutParams(settingsContainerLp);

                LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(rightIconHeight, rightIconHeight); // Requires full width
                mExpandIndicator = new ExpandableIndicator(mContext);
                mExpandIndicator.setLayoutParams(expandIndicatorLp);
                mExpandIndicator.setPadding(expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding);
                mExpandIndicator.setClickable(true);
                mExpandIndicator.setFocusable(true);
                mExpandIndicator.setFocusableInTouchMode(false);
                mExpandIndicator.setCropToPadding(false);
                mExpandIndicator.setBackgroundResource(rippleRes);
                mExpandIndicator.setId(R.id.statusbar_header_expand_indicator);

                TypedArray ta = mContext.obtainStyledAttributes(new int[] {android.R.attr.selectableItemBackground});
                int selectableItemBackground = ta.getResourceId(0, 0);
                ta.recycle();
                LinearLayout.LayoutParams dateTimeAlarmGroupLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dateTimeAlarmGroupLp.setMarginStart(dateTimeMarginStart);
                dateTimeAlarmGroupLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_end));

                mDateTimeAlarmGroup = new LinearLayout(mContext);
                mDateTimeAlarmGroup.setBackgroundResource(selectableItemBackground);
                mDateTimeAlarmGroup.setClickable(true);
                mDateTimeAlarmGroup.setFocusable(true);
                mDateTimeAlarmGroup.setBaselineAligned(false);
                mDateTimeAlarmGroup.setLayoutParams(dateTimeAlarmGroupLp);
                mDateTimeAlarmGroup.setId(R.id.date_time_alarm_group);
                mDateTimeAlarmGroup.setGravity(Gravity.CENTER_VERTICAL);
                mDateTimeAlarmGroup.setOrientation(LinearLayout.HORIZONTAL);
                mDateTimeAlarmGroup.setPadding(dateTimePadding, dateTimePadding, dateTimePadding, dateTimePadding);

                mAlarmStatus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, WRAP_CONTENT));
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, qsTimeExpandedSize);
                mAlarmStatus.setTextColor(ColorUtils.getColorAttr(mContext, android.R.attr.textColorPrimary));
                mAlarmStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                mAlarmStatus.setCompoundDrawablePadding(0);
                mAlarmStatus.setPadding(0, 0, 0, 0);
                mAlarmStatus.setGravity(Gravity.CENTER_VERTICAL);
                mAlarmStatus.setClickable(false);
                mAlarmStatus.setBackground(null);
                mAlarmStatus.setVisibility(View.GONE);

                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mClock = (TextView) mStatusBarClockConstructor.newInstance(mContext);
                ((TextView) mClock).setSingleLine();
                ((TextView) mClock).setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                ((TextView) mClock).setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.status_bar_clock_size));
                ((TextView) mClock).setTextColor(res.getResources().getColor(R.color.status_bar_clock_color));
                ((TextView) mClock).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                mClock.setPaddingRelative(res.getDimensionPixelSize(R.dimen.status_bar_clock_starting_padding), 0, 0, 0);
                mClock.setLayoutParams(clockLp);
                LinearLayout.LayoutParams dateCollapsedLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, WRAP_CONTENT);
                dateCollapsedLp.gravity = Gravity.CENTER_VERTICAL;
                mDateCollapsed.setLayoutParams(dateCollapsedLp);
                mDateCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, qsTimeExpandedSize);
                mDateCollapsed.setTextColor(ColorUtils.getColorAttr(mContext, android.R.attr.textColorPrimary));
                mDateCollapsed.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));


                LinearLayout.LayoutParams alarmStatusCollapsedLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                alarmStatusCollapsedLp.gravity = Gravity.CENTER;
                mAlarmStatusCollapsed = new AlphaOptimizedImageView(mContext);
                mAlarmStatusCollapsed.setLayoutParams(alarmStatusCollapsedLp);
                mAlarmStatusCollapsed.setId(R.id.alarm_status_collapsed);
                mAlarmStatusCollapsed.setImageDrawable(alarmSmall);
                mAlarmStatusCollapsed.setClickable(false);
                mAlarmStatusCollapsed.setFocusable(false);
                mAlarmStatusCollapsed.setVisibility(View.GONE);
                mAlarmStatusCollapsed.setPaddingRelative(res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding), 0, res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding), 0);

                RelativeLayout.LayoutParams headerQsPanelLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.qs_quick_tile_size));
                headerQsPanelLp.topMargin = res.getDimensionPixelSize(R.dimen.qs_quick_panel_margin_top);
                headerQsPanelLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                headerQsPanelLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                mHeaderQsPanel = new QuickQSPanel(mContext);
                mHeaderQsPanel.setId(R.id.quick_qs_panel);
                mHeaderQsPanel.setLayoutParams(headerQsPanelLp);
                mHeaderQsPanel.setClipChildren(false);
                mHeaderQsPanel.setClipToPadding(false);
                mHeaderQsPanel.setFocusable(true);

                RelativeLayout.LayoutParams topContainerLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.qs_top_container_height));
                topContainerLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                mTopContainer = new LinearLayout(mContext);
                mTopContainer.setLayoutParams(topContainerLp);
                mTopContainer.setGravity(Gravity.CENTER);
                mTopContainer.setOrientation(LinearLayout.HORIZONTAL);
                mTopContainer.setClipToPadding(false);
                mTopContainer.setClipChildren(false);
                mTopContainer.setPaddingRelative(topContainerPadding, 0, topContainerPadding, 0);

                if (mWeatherContainer != null && mWeatherLine1 != null && mWeatherLine2 != null) {
                    RelativeLayout.LayoutParams weatherContainerLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    weatherContainerLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    //weatherContainerLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    weatherContainerLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                    weatherContainerLp.bottomMargin = headerItemsMarginTop;
                    //mWeatherContainer.setOrientation(LinearLayout.HORIZONTAL); // TODO Setting the orientation completely fu**s it up?! Positioned at the parent top
                    mWeatherContainer.setLayoutParams(weatherContainerLp);

                    //mWeatherLine1.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                    //mWeatherLine2.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                    //int padding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_weather_padding_end", "dimen", PACKAGE_SYSTEMUI));
                    //mWeatherLine1.setPadding(0, 0, 0, 0);
                    //mWeatherLine2.setPadding(0, 0, padding, 0);

                    //TextView dash = new TextView(mWeatherLine2.getContext());
                    //dash.setText(" - ");
                    //mWeatherContainer.addView(dash, 1, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                }
                LinearLayout.LayoutParams carrierTextLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                mCarrierText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mCarrierText.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
                mCarrierText.setTextColor(ColorUtils.getColorAttr(mContext, android.R.attr.textColorPrimary));
                mCarrierText.setSingleLine();
                mCarrierText.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                mCarrierText.setLayoutParams(carrierTextLp);
                mCarrierText.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams batteryContainerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mBatteryContainer = new LinearLayout(mContext);
                mBatteryContainer.setOrientation(LinearLayout.HORIZONTAL);
                mBatteryContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                mBatteryContainer.setLayoutParams(batteryContainerLp);
                ViewGroup.MarginLayoutParams batteryLevelLp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mBatteryLevel.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                mBatteryLevel.setSingleLine();
                mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX, qsTimeExpandedSize);
                mBatteryLevel.setTextColor(ColorUtils.getColorAttr(mContext, android.R.attr.textColorPrimary));
                mBatteryLevel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                mBatteryLevel.setPaddingRelative(0, 0, res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0);
                ViewGroup.MarginLayoutParams batteryLp = new ViewGroup.MarginLayoutParams(res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width), res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
                mBatteryContainer.addView(mBatteryLevel, batteryLevelLp);
                mBatteryContainer.addView(mBattery, batteryLp);
                if (mTaskManagerButton != null) {
                    ((ViewGroup) mTaskManagerButton.getParent()).removeView(mTaskManagerButton);
                    LinearLayout.LayoutParams taskManagerButtonLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                    mTaskManagerButton.setLayoutParams(taskManagerButtonLp);
                }

                /*try { // OOS (and maybe more in the future)
                    XposedHelpers.findMethodBestMatch(mStatusBarHeaderView.getClass(), "startDateActivity");
                    if (mStatusBarHeaderView instanceof View.OnClickListener) {
                        View.OnClickListener l = (View.OnClickListener) mStatusBarHeaderView;
                        mDateCollapsed.setOnClickListener(l);
                        mDateExpanded.setOnClickListener(l);
                    }
                } catch (Throwable ignore) {
                }*/

                mTopContainer.addView(mCarrierText);
                mTopContainer.addView(mBatteryContainer);
                mTopContainer.addView(mClock);

                if (mTaskManagerButton != null)
                    mRightContainer.addView(mTaskManagerButton);

                mRightContainer.addView(mMultiUserSwitch);
                mRightContainer.addView(mEdit);
                mRightContainer.addView(mSettingsContainer);
                mRightContainer.addView(mExpandIndicator);

                mDateTimeAlarmGroup.addView(mDateCollapsed);
                mDateTimeAlarmGroup.addView(mAlarmStatusCollapsed);
                mDateTimeAlarmGroup.addView(mAlarmStatus); // The view HAS to be attached to a parent, otherwise it apparently gets GC -> NPE. We hide it later if necessary
                mDateTimeAlarmGroup.setOnClickListener((View.OnClickListener) mStatusBarHeaderView);

                LinearLayout.LayoutParams qsFooterLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.qs_quick_tile_size));
                mQsFooter = new QSFooter(mContext);
                mQsFooter.setOrientation(LinearLayout.HORIZONTAL);
                mQsFooter.setBaselineAligned(false);
                mQsFooter.setClickable(false);
                mQsFooter.setClipToPadding(false);
                mQsFooter.setClipChildren(false);
                mQsFooter.setId(R.id.qs_footer);
                mQsFooter.setElevation(elevation);
                mQsFooter.setLayoutParams(qsFooterLp);
                mQsFooter.setGravity(Gravity.CENTER_VERTICAL);
                mQsFooter.setBackgroundColor(0);
                mQsFooter.addView(mDateTimeAlarmGroup);
                mQsFooter.addView(mRightContainer);
                mQsFooter.init();
                mQsFooter.setActivityStarter(XposedHelpers.getObjectField(mStatusBarHeaderView, "mActivityStarter"));

                mStatusBarHeaderView.addView(mTopContainer, 0);
                mStatusBarHeaderView.addView(mHeaderQsPanel, 1);
                mQsContainer.addView(mQsFooter);
                mQsPanel.setClipChildren(false);
                mQsPanel.setBackgroundColor(0);
                mStatusBarHeaderView.setClipChildren(false);
                mStatusBarHeaderView.setClipToPadding(false);
                mStatusBarHeaderView.setClickable(false);
                mStatusBarHeaderView.setFocusable(false);

                mStatusBarHeaderView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRect(mClipBounds);
                    }
                });

            } catch (Throwable t) {
                // :(
                XposedHook.logE(TAG, "Error modifying header layout", t);
                return;
            }

            DetailViewManager.init(mContext, mStatusBarHeaderView, mQsPanel, mHasEditPanel);
            postSetupAnimators();

        }
    };

    private static void createEditButton(int height, int width) {
        mEdit = new AlphaOptimizedImageView(mContext);

        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(width, height);
        mEdit.setLayoutParams(editLp);

        int padding = mResUtils.getDimensionPixelSize(R.dimen.qs_edit_padding);

        TypedValue background = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, background, true);

        mEdit.setId(R.id.qs_edit);
        mEdit.setClickable(true);
        mEdit.setFocusable(true);
        mEdit.setImageDrawable(mResUtils.getDrawable(R.drawable.ic_mode_edit));
        mEdit.setBackground(mContext.getDrawable(background.resourceId));
        mEdit.setPadding(padding, padding, padding, padding);
        mEdit.setOnClickListener(onClickListener);
    }

    private static final XC_MethodHook onLayoutHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mAlarmStatusX = mAlarmStatus.getX();
        }
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mAlarmStatus.setX(mAlarmStatusX);
        }
    };

    private static final XC_MethodHook setExpansionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            float f = (float) param.args[0];
            mExpanded = f > 0;
            mExpansion = f;
            try {
                mQsAnimator.setPosition(f);
            } catch (Throwable ignore) {
                // Oh god, a massive spam wall coming right at you, quick, hide!
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int mGridHeight = qsHooks.getGridHeight();
            if (mGridHeight == 0)
                return;
            View view = (View) param.thisObject;
            float height = view.getHeight();
            height += (int) (mGridHeight * mExpansion);
            mClipBounds.set(view.getPaddingLeft(), 0, view.getWidth() - view.getPaddingRight(), (int) height);
            view.setClipBounds(mClipBounds);
            view.invalidateOutline();
        }
    };
    private static final XC_MethodHook updateVisibilities = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (mSystemIconsSuperContainer != null) {
                mBatteryLevel.setVisibility(View.VISIBLE);
                boolean mExpanded = XposedHelpers.getBooleanField(param.thisObject, "mExpanded");
                mSystemIconsSuperContainer.setVisibility(View.GONE);
                mDateExpanded.setVisibility(View.GONE);
                mDateGroup.setVisibility(View.GONE);

                if (mHideTunerIcon && mTunerIcon != null) mTunerIcon.setVisibility(View.INVISIBLE);
                if (mHideEditTiles && mCustomQSEditButton != null) {
                    mCustomQSEditButton.setVisibility(View.GONE);
                    if (mCustomQSEditButton2 != null) mCustomQSEditButton2.setVisibility(View.GONE);
                }
                if (mHideCarrierLabel && mCarrierText != null)
                    mCarrierText.setVisibility(View.INVISIBLE);
                if (mWeatherContainer != null) {
                    try {
                        mWeatherContainer.setVisibility(mExpanded && XposedHelpers.getBooleanField(mStatusBarHeaderView, "mShowWeather") ? View.VISIBLE : View.INVISIBLE);
                    } catch (Throwable ignored) {
                    }
                }
                if (mOriginalCarrierText != null)
                    mOriginalCarrierText.setVisibility(View.GONE);
            }
            return null;
        }
    };
    private static final XC_MethodHook setEditingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            boolean editing = (boolean) param.args[0];
            boolean shouldShowViews = !editing && XposedHelpers.getBooleanField(param.thisObject, "mExpanded");
            if (mDateTimeAlarmGroup != null) {
                mDateTimeAlarmGroup.setVisibility(editing ? View.INVISIBLE : View.VISIBLE);
                mMultiUserSwitch.setVisibility(shouldShowViews ? View.VISIBLE : View.INVISIBLE);
                mSettingsContainer.setVisibility(shouldShowViews ? View.VISIBLE : View.INVISIBLE);
                mExpandIndicator.setVisibility(editing ? View.INVISIBLE : View.VISIBLE);
            }
        }
    };

    private static final XC_MethodHook setTilesHook = new XC_MethodHook() {
        boolean cancelled = false;

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (mHeaderQsPanel != null) { // keep
                XposedHook.logD(TAG, "setTilesHook Called");
                if (mRecreatingStatusBar) {
                    XposedHook.logD(TAG, "setTilesHook: Skipping changed check due to StatusBar recreation");
                    return; // Otherwise all tiles are gone after recreation
                }
                if (mUseDragPanel && !RomUtils.isAicp()) {
                    XposedHook.logD(TAG, "setTilesHook: Skipping check because mUseDragPanel && !RomUtils.isAicp()");
                    return; // CM has tile caching logics
                }
                // Only set up views if the tiles actually changed
                if (param.args == null || param.args.length == 0) {
                    XposedHook.logD(TAG, "setTilesHook: Skipping check because param.args == null || param.args.length == 0");
                    return; // PA already checks itself
                }
                Collection tiles = (Collection) param.args[0];
                ArrayList<String> newTiles = new ArrayList<>();
                for (Object qstile : tiles) {
                    newTiles.add(qstile.getClass().getSimpleName());
                }
                cancelled = false;
                if (mPreviousTiles.equals(newTiles)) {
                    cancelled = true;
                    XposedHook.logD(TAG, "setTilesHook: Cancelling original method because mPreviousTiles.equals(newTiles)");
                    param.setResult(null);
                    return;
                }
                mPreviousTiles.clear();
                mPreviousTiles.addAll(newTiles);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mHeaderQsPanel != null) { // keep
                try {
                    //noinspection unchecked
                    mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                    // OOS: sometimes mRecords still seems to be in the StatusBarHeaderView (but empty)
                    if (mRecords.size() == 0) throw new Throwable();
                } catch (Throwable t) {
                    try { // OOS
                        //noinspection unchecked
                        mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mGridView"), "mRecords");
                    } catch (Throwable t2) {
                        XposedHook.logE(TAG, "No tile record field found (" + t.getClass().getSimpleName() + " and " + t2.getClass().getSimpleName() + ")", null);
                        return;
                    }
                }
                if (!cancelled) {
                    mHeaderQsPanel.setTiles(mRecords);
                } else {
                    XposedHook.logD(TAG, "setTilesHook: Not setting tiles to header because cancelled");
                }
            }
        }
    };

    private static final XC_MethodHook handleStateChangedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            // This method gets called from two different processes, so we have
            // to check if we are in the right one by testing if the panel is null
            if (mHeaderQsPanel != null) {
                Object state = XposedHelpers.getObjectField(param.thisObject, "mState");
                mHeaderQsPanel.handleStateChanged(param.thisObject, state);
                NotificationPanelHooks.handleStateChanged(param.thisObject, state);
            }
        }
    };

    private static final XC_MethodHook setupViewsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            View pageIndicator = (View) XposedHelpers.getObjectField(param.thisObject, "mPageIndicator");
            pageIndicator.setAlpha(0);
        }
    };

    private static void wrapQsDetail(LinearLayout layout) {
        Context context = layout.getContext();

        FrameLayout content = (FrameLayout) layout.findViewById(android.R.id.content);
        ViewUtils.setHeight(content, ViewGroup.LayoutParams.MATCH_PARENT);

        int position = layout.indexOfChild(content);
        layout.removeView(content);

        LinearLayout.LayoutParams scrollViewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        scrollViewLp.weight = 1;
        NonInterceptingScrollView scrollView = new NonInterceptingScrollView(context);
        scrollView.setLayoutParams(scrollViewLp);
        scrollView.addView(content);
        scrollView.setFillViewport(true);

        layout.addView(scrollView, position);
    }

    private static final XC_MethodHook handleShowDetailImplHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            boolean show = (boolean) param.args[1];
            Object tileRecord = param.args[0];
            QSDetail qsDetail = qsHooks.getQSDetail();
            if (show ? NotificationPanelHooks.isCollapsed() : mCollapseAfterHideDetails) {
                param.args[2] = mQsAnimator.getTileViewX(tileRecord);
                param.args[3] = 0;
                if (!show) {
                    NotificationPanelHooks.collapseIfNecessary();
                }
            } else {
                if (tileRecord != null) {
                    try {
                        View tileView = (View) XposedHelpers.getObjectField(tileRecord, "tileView");
                        param.args[2] = tileView.getLeft() + tileView.getWidth() / 2;
                        param.args[3] = getDetailY(tileView) + qsHooks.getTileLayout().getOffsetTop(tileRecord) + mQsPanel.getTop();
                    } catch (Throwable ignore) { // OOS3
                    }
                }
            }
            mCollapseAfterHideDetails = false;
            if (qsDetail != null) {
                qsDetail.handleShowingDetail(param.args[0], (boolean) param.args[1], (int) param.args[2], (int) param.args[3]);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            boolean show = (boolean) param.args[1];
            XposedHook.logD(TAG, "handleShowDetailImpl: " + (show ? "showing" : "hiding") + " detail; expanding: " + NotificationPanelHooks.isCollapsed() + ";");
            if (show && NotificationPanelHooks.isCollapsed()) {
                mCollapseAfterHideDetails = true;
                NotificationPanelHooks.expandIfNecessary();
            }
        }
    };

    private static int getDetailY(View tileView) {
        boolean dual = XposedHelpers.getBooleanField(tileView, "mDual");
        FrameLayout mIconFrame = (FrameLayout) XposedHelpers.getAdditionalInstanceField(tileView, "mIconFrame");
        RelativeLayout mLabelContainer = (RelativeLayout) XposedHelpers.getAdditionalInstanceField(tileView, "mLabelContainer");
        if (dual) {
            return tileView.getTop() + mLabelContainer.getTop() + mLabelContainer.getHeight() / 2;
        } else {
            return tileView.getTop() + mIconFrame.getTop() + (mIconFrame.getHeight() / 2);
        }
    }

    private static void showDetailAdapter(boolean show, Object adapter, int[] locationInWindow) throws Exception {
        Class<?> classRecord = XposedHelpers.findClass(CLASS_QS_PANEL + "$Record", mQsPanel.getContext().getClassLoader());

        int xInWindow = locationInWindow[0];
        int yInWindow = locationInWindow[1];
        ((View) mQsPanel.getParent()).getLocationInWindow(locationInWindow);

        Constructor c = classRecord.getDeclaredConstructor();
        c.setAccessible(true);
        Object r = c.newInstance();

        XposedHelpers.setObjectField(r, "detailAdapter", adapter);
        XposedHelpers.setIntField(r, "x", xInWindow - locationInWindow[0]);
        XposedHelpers.setIntField(r, "y", yInWindow - locationInWindow[1]);

        locationInWindow[0] = xInWindow;
        locationInWindow[1] = yInWindow;

        XposedHelpers.callMethod(mQsPanel, "showDetail", show, r);
    }

    private static void handleShowingDetail(final Object detail) { //TODO see what we do with this
        if (ConfigUtils.qs().fix_header_space) return;
        final boolean showingDetail = detail != null;
        mCurrentDetailView = getCurrentDetailView();
        int rightButtonVisibility = View.GONE;
        DetailViewManager.DetailViewAdapter detailViewAdapter = DetailViewManager.getInstance().getDetailViewAdapter(mCurrentDetailView);
        if (detailViewAdapter != null && detailViewAdapter.hasRightButton()) {
            rightButtonVisibility = View.VISIBLE;
            mQsRightButton.setImageDrawable(mResUtils.getDrawable(detailViewAdapter.getRightButtonResId()));
        }
        mQsRightButton.setVisibility(rightButtonVisibility);
        // Fixes an issue with the indicator having two backgrounds when layer type is hardware
        mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
        transition(mDateTimeAlarmGroup, !showingDetail);
        transition(mRightContainer, !showingDetail);
        transition(mExpandIndicator, !showingDetail);
        if (mExpansion < 1)
            transition(mHeaderQsPanel, !showingDetail);
        if (mWeatherContainer != null) {
            try {
                if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mShowWeather"))
                    transition(mWeatherContainer, !showingDetail);
            } catch (Throwable ignored) {
            }
        }
        transition(mQsDetailHeader, showingDetail);
        mShowingDetail = showingDetail;
        XposedHelpers.setBooleanField(mStatusBarHeaderView, "mShowingDetail", showingDetail);
        if (showingDetail) {
            XposedHook.logD(TAG, "handleShowingDetail: showing detail; " + detail.getClass().getSimpleName());
            try {
                mQsDetailHeaderTitle.setText((int) XposedHelpers.callMethod(detail, "getTitle"));
            } catch (Throwable t) {
                Context context = mQsDetailHeaderTitle.getContext();
                Class<?> classQSTile = XposedHelpers.findClass(CLASS_QS_TILE, context.getClassLoader());
                mQsDetailHeaderTitle.setText((String) XposedHelpers.callStaticMethod(classQSTile, "getDetailAdapterTitle", context, detail));
            }
            final Boolean toggleState = (Boolean) XposedHelpers.callMethod(detail, "getToggleState");
            if (toggleState == null) {
                mQsDetailHeaderSwitch.setVisibility(View.INVISIBLE);
                mQsDetailHeader.setClickable(false);
            } else {
                mQsDetailHeaderSwitch.setVisibility(View.VISIBLE);
                mQsDetailHeaderSwitch.setChecked(toggleState);
                mQsDetailHeader.setClickable(true);
                mQsDetailHeader.setOnClickListener(new SafeOnClickListener(TAG, "Error in mQsDetailHeader click listener") {
                    @Override
                    public void onClickSafe(View v) {
                        boolean checked = !mQsDetailHeaderSwitch.isChecked();
                        mQsDetailHeaderSwitch.setChecked(checked);
                        XposedHelpers.callMethod(detail, "setToggleState", checked);
                    }
                });
            }
            if (mHasEditPanel) {
                if ((int) XposedHelpers.callMethod(detail, "getTitle")
                        == mQsDetailHeader.getResources().getIdentifier("quick_settings_edit_label", "string", PACKAGE_SYSTEMUI)) {
                    mEditTileDoneText.setVisibility(View.VISIBLE);
                } else {
                    mEditTileDoneText.setVisibility(View.GONE);
                }
            }
        } else {
            XposedHook.logD(TAG, "handleShowingDetail: hiding detail; collapsing: " + mCollapseAfterHideDetails);
            mQsDetailHeader.setClickable(false);
        }
    }

    private static View getCurrentDetailView() {
        Object detailRecord = XposedHelpers.getObjectField(mQsPanel, "mDetailRecord");
        if (detailRecord != null) {
            Object detailView = XposedHelpers.getObjectField(detailRecord, "detailView");
            if (detailView != null && detailView instanceof View) {
                return (View) detailView;
            }
        }
        return null;
    }

    public static void transition(final View v, final boolean in) {
        if (in) {
            v.bringToFront();
            v.setVisibility(View.VISIBLE);
        }
        if (v.hasOverlappingRendering()) {
            v.animate().withLayer();
        }
        v.animate()
                .alpha(in ? 1 : 0)
                .withEndAction(new SafeRunnable() {
                    @Override
                    public void runSafe() {
                        if (!in) {
                            v.setVisibility(View.INVISIBLE);
                        }
                        if (!ConfigUtils.M)
                            XposedHelpers.setBooleanField(mStatusBarHeaderView, "mDetailTransitioning", false);
                    }
                })
                .start();
    }

    private static final View.OnClickListener onClickListener = new SafeOnClickListener(TAG, "Error in onClickListener") {
        @Override
        public void onClickSafe(View v) {
            switch (v.getId()) {
                case R.id.qs_right:
                    if (mCurrentDetailView != null && mCurrentDetailView instanceof DetailViewManager.DetailViewAdapter) {
                        ((DetailViewManager.DetailViewAdapter) mCurrentDetailView).handleRightButtonClick();
                    }
                    break;
                case R.id.qs_edit:
                    int[] loc = new int[2];
                    v.getLocationInWindow(loc);
                    onClickEdit(loc[0] + (v.getWidth() / 2), loc[1] + (v.getHeight() / 2));
                    break;
            }
        }
    };

    private static void onClickEdit(int vx, int vy) {
        final int x = StackScrollAlgorithmHooks.mStackScrollLayout.getLeft() + vx;

        showEditDismissingKeyguard(x, vy);
    }

    public static void showEditDismissingKeyguard(final int x, final int y) {
        SystemUIHooks.startRunnableDismissingKeyguard(new Runnable() {
            @Override
            public void run() {
                showEdit(x, y);
            }
        });
    }

    private static void showEdit(final int x, final int y) {
        mQsPanel.post(new Runnable() {
            @Override
            public void run() {
                NotificationPanelHooks.showQsCustomizer(mRecords, x, y);
            }
        });
    }

    private static KeyguardMonitor.Callback mKeyguardCallback = new KeyguardMonitor.Callback() {
        @Override
        public void onKeyguardChanged() {
            postSetupAnimatorsImpl();
            QSTileHostHooks.mKeyguard.removeCallback(this);
        }
    };

    public static void postSetupAnimators() {
        XposedHook.logD(TAG, "postSetupAnimators called");
        KeyguardMonitor keyguardMonitor = QSTileHostHooks.mKeyguard;
        if (keyguardMonitor == null || !keyguardMonitor.isShowing())
            postSetupAnimatorsImpl();
        else
            keyguardMonitor.addCallback(mKeyguardCallback);
    }

    private static void postSetupAnimatorsImpl() {
        XposedHook.logD(TAG, "postSetupAnimatorsImpl called");
        // Wait until the layout is set up
        // It already works after 2 frames on my device, but just to be sure use 3
        mQsPanel.post(new Runnable() {
            @Override
            public void run() {
                mQsPanel.post(new Runnable() {
                    @Override
                    public void run() {
                        mQsPanel.post(new SafeRunnable() {
                            @Override
                            public void runSafe() {
                                if (mQsAnimator != null) {
                                    mQsAnimator.mUpdateAnimators.run();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public static void hookKeyguard(ClassLoader classLoader) {
        try {
            mCarrierTextConstructor = XposedHelpers.findClass("com.android.keyguard.CarrierText", classLoader).getDeclaredConstructor(Context.class);
        } catch (NoSuchMethodException ignore) {
        }
    }


    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.qs().header) {

                qsHooks = QuickSettingsHooks.create(classLoader);

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);
                Class<?> classLayoutValues = XposedHelpers.findClass(CLASS_LAYOUT_VALUES, classLoader);
                Class<?> classQSPanel = XposedHelpers.findClass(CLASS_QS_PANEL, classLoader);
                Class<?> classQSTile = XposedHelpers.findClass(CLASS_QS_TILE, classLoader);
                Class<?> classQSTileView = XposedHelpers.findClass(CLASS_QS_TILE_VIEW, classLoader);
                Class<?> classPhoneStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
                Class<?> classStatusBarClock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", classLoader);
                Class<?> classQSDetailItems = XposedHelpers.findClass(CLASS_QS_DETAIL_ITEMS, classLoader);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
                XposedHelpers.findAndHookMethod(classQSDetailItems, "setMinHeightInItems", int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classQSDetailItems, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        QSDetailItemsHelper.getInstance(param.thisObject).onFinishInflate();
                    }
                });
                XposedBridge.hookAllMethods(classQSDetailItems, "handleSetItems", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        QSDetailItemsHelper.getInstance(param.thisObject).handleSetItems((Object[]) param.args[0]);
                        return null;
                    }
                });
                XposedHelpers.findAndHookMethod(classQSDetailItems, "handleSetItemsVisible", boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        QSDetailItemsHelper.getInstance(param.thisObject).handleSetItemsVisible((boolean) param.args[0]);
                        return null;
                    }
                });
                XposedBridge.hookAllMethods(classQSDetailItems, "handleSetCallback", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        QSDetailItemsHelper.getInstance(param.thisObject).handleSetCallback(param.args[0]);
                    }
                });
                mStatusBarClockConstructor = classStatusBarClock.getConstructor(Context.class);

                try {
                    XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setEditing", boolean.class, setEditingHook);
                    mHasEditPanel = true;
                } catch (NoSuchMethodError ignore) {
                    mHasEditPanel = false;
                }

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setExpansion", float.class, setExpansionHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateVisibilities", updateVisibilities);

                try {
                    // Every time you make a typo, the errorists win.
                    XposedHelpers.findAndHookMethod(classLayoutValues, "interpoloate", classLayoutValues, classLayoutValues, float.class, XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable ignore) { // yeah thx Bliss
                    XposedHelpers.findAndHookMethod(classLayoutValues, "interpolate", classLayoutValues, classLayoutValues, float.class, XC_MethodReplacement.DO_NOTHING);
                }
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "requestCaptureValues", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "applyLayoutValues", classLayoutValues, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "captureLayoutValues", classLayoutValues, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateLayoutValues", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockCollapsedMargin", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateHeights", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSignalClusterDetachment", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSystemIconsLayoutParams", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAvatarScale", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockScale", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAmPmTranslation", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockLp", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateMultiUserSwitch", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setClipping", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onNextAlarmChanged", AlarmManager.AlarmClockInfo.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mQsFooter.onNextAlarmChanged((AlarmManager.AlarmClockInfo) param.args[0]);
                    }
                });

                if (!ConfigUtils.qs().keep_header_background) {
                    try { // AICP test
                        XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "doUpdateStatusBarCustomHeader", Drawable.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
                    } catch (Throwable ignore) {
                    }
                }

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClickTargets", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mAlarmStatus.setClickable(false);
                    }
                });

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onClick", View.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] == mAlarmStatus) {
                            param.setResult(null);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object v = param.args[0];
                        if (v == mDateTimeAlarmGroup) {
                            try {
                                XposedHelpers.callMethod(param.thisObject, "startDateActivity");
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                });
                if (ConfigUtils.qs().fix_header_space) {
                    XposedHelpers.findAndHookMethod(classQSPanel, "showDetailAdapter", boolean.class, CLASS_DETAIL_ADAPTER, int[].class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            showDetailAdapter((boolean) param.args[0], param.args[1], (int[]) param.args[2]);
                            return null;
                        }
                    });
                }

                mUseDragPanel = false;
                try {
                    Class<?> classQSDragPanel = XposedHelpers.findClass(CLASS_QS_DRAG_PANEL, classLoader);
                    XposedHelpers.findAndHookMethod(classQSDragPanel, "setTiles", Collection.class, setTilesHook);
                    XposedHelpers.findAndHookMethod(classQSDragPanel, "setupViews", setupViewsHook);
                    XposedBridge.hookAllMethods(classQSDragPanel, "handleShowDetailImpl", handleShowDetailImplHook);
                    mUseDragPanel = true;
                } catch (Throwable ignore) {
                    XposedHelpers.findAndHookConstructor(classQSPanel, Context.class, AttributeSet.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            wrapQsDetail((LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mDetail"));
                        }
                    });
                    XposedBridge.hookAllMethods(classQSPanel, "handleShowDetailImpl", handleShowDetailImplHook);
                    try {
                        XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", Collection.class, setTilesHook);
                    } catch (Throwable t) { // PA
                        XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", setTilesHook);
                    }
                }

                try {
                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "recreateStatusBar", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            mRecreatingStatusBar = true;
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mRecreatingStatusBar = false;
                        }
                    });
                } catch (Throwable ignore) {
                }

                QSTileHostHooks.hook(classLoader);


                final WifiTileHook w = new WifiTileHook(classLoader);
                final BluetoothTileHook b = new BluetoothTileHook(classLoader);
                final CellularTileHook c = new CellularTileHook(classLoader);
                final DndTileHook d = new DndTileHook(classLoader);
                if (ConfigUtils.L1) {
                    XposedHelpers.findAndHookMethod(classQSTile, "handleLongClick", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object that = param.thisObject;
                            if (w.maybeHandleLongClick(that) || b.maybeHandleLongClick(that) || c.maybeHandleLongClick(that) || d.maybeHandleLongClick(that))
                                param.setResult(null);
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classQSTile, "handleStateChanged", handleStateChangedHook);

                try { // OOS3
                    XposedHelpers.findAndHookMethod(classQSTileView, "setOverlay", CLASS_QS_TILE + "$Mode", XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable ignore) {
                }

                if (ConfigUtils.L1) {
                    XposedHelpers.findAndHookMethod(classQSTileView, "setIcon", ImageView.class, CLASS_QS_STATE, new XC_MethodHook() {
                        boolean forceAnim = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            View iv = (View) param.args[0];
                            Object headerItem = XposedHelpers.getAdditionalInstanceField(param.thisObject, "headerTileRowItem");
                            forceAnim = headerItem != null && (boolean) headerItem &&
                                    !Objects.equals(XposedHelpers.getObjectField(param.args[1], "icon"),
                                            iv.getTag(iv.getResources().getIdentifier("qs_icon_tag", "id", PACKAGE_SYSTEMUI)));
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (forceAnim) {
                                View iconView = (View) XposedHelpers.getObjectField(param.thisObject, "mIcon");
                                if (iconView instanceof ImageView) {
                                    Drawable icon = ((ImageView) iconView).getDrawable();
                                    if (icon instanceof Animatable) {
                                        if (iconView.isShown()) {
                                            ((Animatable) icon).start();
                                            String type = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "headerTileRowType");
                                            XposedHook.logD(TAG, "Animating QuickQS icon: " + forceAnim + (type != null ? ("; type: " + type) : ""));
                                        } else {
                                            ((Animatable) icon).stop();
                                        }
                                    }
                                }
                            }
                        }
                    });
                }

                if (ConfigUtils.qs().enable_qs_editor) {
                    XposedHelpers.findAndHookMethod(PACKAGE_SYSTEMUI + ".settings.BrightnessController", classLoader, "updateIcon", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View icon = (View) XposedHelpers.getObjectField(param.thisObject, "mIcon");
                            if (icon != null) icon.setVisibility(View.GONE);
                        }
                    });
                }

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static int R_string_battery_panel_title;

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            if (ConfigUtils.qs().header) {

                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);

                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                R_string_battery_panel_title = resparam.res.addResource(modRes, R.string.battery_panel_title);

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_panel_width", modRes.fwd(R.dimen.notification_panel_width));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_peek_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height_expanded", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_emergency_calls_only_text_size", modRes.fwd(R.dimen.emergency_calls_only_text_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_date_collapsed_size", modRes.fwd(R.dimen.qs_time_expanded_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_collapsed_size", modRes.fwd(R.dimen.multi_user_avatar_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_brightness_padding_top", modRes.fwd(R.dimen.brightness_slider_padding_top));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_panel_padding_bottom", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_time_expanded_size", modRes.fwd(R.dimen.qs_time_expanded_size));
                if (ConfigUtils.M)
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_expanded_size", modRes.fwd(R.dimen.multi_user_avatar_size));

                /*
                if (!ConfigUtils.qs().large_first_row) {
                    try {
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_dual_tile_height",
                                new XResources.DimensionReplacement(resparam.res.getDimensionPixelSize(
                                        resparam.res.getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI)),
                                        TypedValue.COMPLEX_UNIT_PX));
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_tile_divider_height", zero);
                    } catch (Throwable t) {
                        XposedHook.logE(TAG, "Couldn't change qs_dual_tile_height or qs_tile_divider_height (" + t.getClass().getSimpleName() + ")", null);
                    }
                }
                */

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "qs_tile_divider", 0x00000000);

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        View layout = liparam.view;
                        Context context = layout.getContext();
                        layout.setPadding(0, 0, 0, 0);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.height = ResourceUtils.getInstance(liparam.view.getContext()).getDimensionPixelSize(R.dimen.status_bar_header_height);
                        layout.setElevation(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.qs_container_elevation));
                    }
                });

                // Motorola
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "zz_moto_status_bar_expanded_header", new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            View layout = liparam.view;
                            Context context = layout.getContext();
                            layout.setPadding(0, 0, 0, 0);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) liparam.view.getLayoutParams();
                            params.height = ResourceUtils.getInstance(liparam.view.getContext()).getDimensionPixelSize(R.dimen.status_bar_header_height);
                            layout.setElevation(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.qs_container_elevation));
                        }
                    });
                } catch (Throwable ignore) {
                }

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_detail_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        try {
                            LinearLayout layout = (LinearLayout) liparam.view;
                            Context context = layout.getContext();

                            ResourceUtils res = ResourceUtils.getInstance(context);
                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                            int padding = context.getResources().getDimensionPixelSize(context.getResources().getIdentifier("qs_panel_padding", "dimen", PACKAGE_SYSTEMUI));

                            TextView title = (TextView) layout.findViewById(android.R.id.title);
                            title.setPadding(padding, 0, 0, 0);

                            mQsRightButton = (ImageView) inflater.inflate(res.getLayout(R.layout.qs_right_button), null);
                            mQsRightButton.setOnClickListener(onClickListener);
                            mQsRightButton.setVisibility(View.GONE);

                            layout.addView(mQsRightButton);
                            layout.setPadding(0, 0, padding, 0);
                            layout.setGravity(Gravity.CENTER);
                        } catch (Throwable ignore) {

                        }
                    }
                });

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        FrameLayout layout = (FrameLayout) liparam.view;
                        Context context = layout.getContext();

                        mQsContainer = layout;

                        layout.setElevation(0);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
                        params.setMargins(0, 0, 0, 0);
                        params.setMarginStart(0);
                        params.setMarginEnd(0);
                        layout.setLayoutParams(params);

                        mQsPanel = (ViewGroup) layout.findViewById(context.getResources().getIdentifier("quick_settings_panel", "id", PACKAGE_SYSTEMUI));
                        mQsPanel.setElevation(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.qs_container_elevation));
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    public static QuickQSPanel getHeaderQsPanel() {
        return mHeaderQsPanel;
    }

    public static ViewGroup getHeader() {
        return mStatusBarHeaderView;
    }

    public static QSAnimator getQsAnimator() {
        return mQsAnimator;
    }

    public static void createQsAnimator() {
        mQsAnimator = new QSAnimator(mQsContainer, mHeaderQsPanel, mQsPanel);
    }

    public static View getSettingsButton() {
        return mSettingsButton;
    }
}
