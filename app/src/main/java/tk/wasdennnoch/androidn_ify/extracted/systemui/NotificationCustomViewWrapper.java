/*
 * Copyright (C) 2014 The Android Open Source Project
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

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

import tk.wasdennnoch.androidn_ify.XposedHook;


/**
 * Wraps a notification containing a custom view.
 */
public class NotificationCustomViewWrapper extends NotificationViewWrapper {

    private final ViewInvertHelper mInvertHelper;
    private final Paint mGreyPaint = new Paint();
    private boolean mIsLegacy;
    private int mLegacyColor;

    protected NotificationCustomViewWrapper(Context ctx, View view, View row) {
        super(ctx, view, row);
        mInvertHelper = new ViewInvertHelper(view, 700 /*NotificationPanelView.DOZE_ANIMATION_DURATION*/);
        mLegacyColor = row.getContext().getColor(row.getContext().getResources().getIdentifier("notification_legacy_background_color", "color", XposedHook.PACKAGE_SYSTEMUI));
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark == mDark && mDarkInitialized) {
            return;
        }
        super.setDark(dark, fade, delay);
        if (!mIsLegacy && mShouldInvertDark) {
            if (fade) {
                mInvertHelper.fade(dark, delay);
            } else {
                mInvertHelper.update(dark);
            }
        } else {
            mView.setLayerType(dark ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
            if (fade) {
                fadeGrayscale(dark, delay);
            } else {
                updateGrayscale(dark);
            }
        }
    }

    protected void fadeGrayscale(final boolean dark, long delay) {
        getDozer().startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getDozer().updateGrayscaleMatrix((float) animation.getAnimatedValue());
                mGreyPaint.setColorFilter(
                        new ColorMatrixColorFilter(getDozer().getGrayscaleColorMatrix()));
                mView.setLayerPaint(mGreyPaint);
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    mView.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        });
    }

    protected void updateGrayscale(boolean dark) {
        if (dark) {
            getDozer().updateGrayscaleMatrix(1f);
            mGreyPaint.setColorFilter(
                    new ColorMatrixColorFilter(getDozer().getGrayscaleColorMatrix()));
            mView.setLayerPaint(mGreyPaint);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        mView.setAlpha(visible ? 1.0f : 0.0f);
    }

    @Override
    protected boolean shouldClearBackgroundOnReapply() {
        return false;
    }

    @Override
    public int getCustomBackgroundColor() {
        int customBackgroundColor = super.getCustomBackgroundColor();
        if (customBackgroundColor == 0 && mIsLegacy) {
            return mLegacyColor;
        }
        return customBackgroundColor;
    }

    @Override
    public void setShowingLegacyBackground(boolean showing) {
        super.setShowingLegacyBackground(showing);
        mIsLegacy = showing;
    }

    public void setLegacy(boolean legacy) {
        super.setLegacy(legacy);
        mIsLegacy = legacy;
    }
}
