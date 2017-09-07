/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package tk.wasdennnoch.androidn_ify.extracted.systemui.qs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.TouchAnimator.Builder;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.TouchAnimator.ListenerAdapter;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class QSFooter extends LinearLayout implements OnClickListener {

    private static final float EXPAND_INDICATOR_THRESHOLD = 0.93f;

    private Context mContext;

    private Object mActivityStarter;
    private ImageButton mSettingsButton;
    protected View mSettingsContainer;

    private TextView mAlarmStatus;
    private View mAlarmStatusCollapsed;
    private View mDate;

    private boolean mExpanded;
    private boolean mAlarmShowing;

    protected ExpandableIndicator mExpandIndicator;

    private boolean mListening;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private boolean mShowEmergencyCallsOnly;
    protected FrameLayout mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private boolean mAlwaysShowMultiUserSwitch;

    protected TouchAnimator mSettingsAlpha;
    private float mExpansionAmount;

    protected View mEdit;
    private boolean mShowEditIcon = ConfigUtils.qs().enable_qs_editor;
    private TouchAnimator mAnimator;
    private View mDateTimeGroup;
    private boolean mKeyguardShowing;
    private TouchAnimator mAlarmAnimator;

    private ResourceUtils res;

    public QSFooter(Context context) {
        super(context);
        mContext = context;
        res = ResourceUtils.getInstance(mContext);
    }

    public void init() {
        mMultiUserSwitch = (FrameLayout) findViewById(mContext.getResources().getIdentifier("multi_user_switch", "id", XposedHook.PACKAGE_SYSTEMUI));
        mDate = findViewById(mContext.getResources().getIdentifier("date_collapsed", "id", XposedHook.PACKAGE_SYSTEMUI));
        mAlarmStatus = (TextView) findViewById(mContext.getResources().getIdentifier("alarm_status", "id", XposedHook.PACKAGE_SYSTEMUI));
        mSettingsButton = (ImageButton) findViewById(mContext.getResources().getIdentifier("settings_button", "id", XposedHook.PACKAGE_SYSTEMUI));
        mSettingsContainer = findViewById(mContext.getResources().getIdentifier("settings_button_container", "id", XposedHook.PACKAGE_SYSTEMUI));
        mDateTimeGroup = findViewById(R.id.date_time_alarm_group);
        mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
        mExpandIndicator = (ExpandableIndicator) findViewById(R.id.statusbar_header_expand_indicator);
        mEdit = findViewById(R.id.qs_edit);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEdit.setVisibility(mShowEditIcon ? VISIBLE : GONE);

        /*mExpandIndicator.setVisibility(
                res.getBoolean(R.bool.config_showQuickSettingsExpandIndicator)
                        ? VISIBLE : GONE);*/
        mExpandIndicator.setVisibility(VISIBLE);

        mDateTimeGroup.setOnClickListener(this);

        mMultiUserAvatar = (ImageView) mMultiUserSwitch.findViewById(mContext.getResources().getIdentifier("multi_user_avatar", "id", XposedHook.PACKAGE_SYSTEMUI));
        mAlwaysShowMultiUserSwitch = false/*res.getBoolean(R.bool.config_alwaysShowMultiUserSwitcher)*/;

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
        XposedHelpers.callMethod(mSettingsButton.getBackground(), "setForceSoftware", true);
        XposedHelpers.callMethod(mExpandIndicator.getBackground(), "setForceSoftware", true);

        updateResources();
    }

    public void setActivityStarter(Object activityStarter) {
        mActivityStarter = activityStarter;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            updateAnimator(r - l);
        }
    }

    private void updateAnimator(int width) {
        int numTiles = StatusBarHeaderHooks.getQsAnimator().getNumTiles();
        int size = res.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size)
                - res.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
        int remaining = (width - numTiles * size) / (numTiles - 1);
        int defSpace = res.getResources().getDimensionPixelSize(R.dimen.default_gear_space);

        mAnimator = new Builder()
                .addFloat(mSettingsContainer, "translationX", -(remaining - defSpace), 0)
                .addFloat(mSettingsButton, "rotation", -120, 0)
                .build();
        if (mAlarmShowing) {
            mAlarmAnimator = new Builder().addFloat(mDate, "alpha", 1, 0)
                    .addFloat(mDateTimeGroup, "translationX", 0, -mDate.getWidth())
                    .addFloat(mAlarmStatus, "alpha", 0, 1)
                    .setListener(new ListenerAdapter() {
                        @Override
                        public void onAnimationAtStart() {
                            mAlarmStatus.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationStarted() {
                            mAlarmStatus.setVisibility(View.VISIBLE);
                        }
                    }).build();
        } else {
            mAlarmAnimator = null;
            mAlarmStatus.setVisibility(View.GONE);
            mDate.setAlpha(1);
            mDateTimeGroup.setTranslationX(0);
        }
        setExpansion(mExpansionAmount);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        //FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);

        updateSettingsAnimator();
    }

    private void updateSettingsAnimator() {
        mSettingsAlpha = createSettingsAlphaAnimator();

        final boolean isRtl = (boolean) XposedHelpers.callMethod(this, "isLayoutRtl");
        if (isRtl && mDate.getWidth() == 0) {
            mDate.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mDate.setPivotX(getWidth());
                    mDate.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            mDate.setPivotX(isRtl ? mDate.getWidth() : 0);
        }
    }

    @Nullable
    private TouchAnimator createSettingsAlphaAnimator() {
        // If the settings icon is not shown and the user switcher is always shown, then there
        // is nothing to animate.
        if (!mShowEditIcon && mAlwaysShowMultiUserSwitch) {
            return null;
        }

        TouchAnimator.Builder animatorBuilder = new TouchAnimator.Builder();
        animatorBuilder.setStartDelay(QSAnimator.EXPANDED_TILE_DELAY);

        if (mShowEditIcon) {
            animatorBuilder.addFloat(mEdit, "alpha", 0, 1);
        }

        if (!mAlwaysShowMultiUserSwitch) {
            animatorBuilder.addFloat(mMultiUserSwitch, "alpha", 0, 1);
        }

        return animatorBuilder.build();
    }

    public void setKeyguardShowing(boolean keyguardShowing) {
        mKeyguardShowing = keyguardShowing;
        setExpansion(mExpansionAmount);
    }

    public void setExpanded(boolean expanded) {
        if (mExpanded != expanded) {
            mExpanded = expanded;
            updateEverything();
        }
    }

    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (mAlarmShowing != (nextAlarm != null)) {
            mAlarmShowing = nextAlarm != null;
            updateAnimator(getWidth());
            updateEverything();
        }
    }

    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        if (mAnimator != null) mAnimator.setPosition(headerExpansionFraction);
        if (mAlarmAnimator != null) mAlarmAnimator.setPosition(
                mKeyguardShowing ? 0 : headerExpansionFraction);

        if (mSettingsAlpha != null) {
            mSettingsAlpha.setPosition(headerExpansionFraction);
        }

        updateAlarmVisibilities();

        mExpandIndicator.setExpanded(headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD);
    }

    @VisibleForTesting
    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    private void updateAlarmVisibilities() {
        mAlarmStatusCollapsed.setVisibility(mAlarmShowing ? View.VISIBLE : View.GONE);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;
    }

    public View getExpandView() {
        return mExpandIndicator;
    }

    public void updateEverything() {
        post(new Runnable() {
            public void run() {
                updateVisibilities();
                setClickable(false);
            }
        });
    }

    private void updateVisibilities() {
        updateAlarmVisibilities();

        mMultiUserSwitch.setVisibility((mExpanded || mAlwaysShowMultiUserSwitch)
                ? View.VISIBLE : View.INVISIBLE);

        if (mShowEditIcon) {
            mEdit.setVisibility(!mExpanded ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mDateTimeGroup) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            XposedHelpers.callMethod(mActivityStarter, "startPendingIntentDismissingKeyguard", showIntent);
        }
    }


    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
            }
        }
    }
}