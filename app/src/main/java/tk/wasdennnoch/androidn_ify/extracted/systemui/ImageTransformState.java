/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.graphics.drawable.Icon;
import android.util.Pools;
import android.view.View;
import android.widget.ImageView;

import java.util.Objects;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationContentHelper;

/**
 * A transform state of a image view.
*/
public class ImageTransformState extends TransformState {
    public static final long ANIMATION_DURATION_LENGTH = 210;

    public static final int ICON_TAG = R.id.image_icon_tag;
    private static Pools.SimplePool<ImageTransformState> sInstancePool
            = new Pools.SimplePool<>(40);
    private Icon mIcon;

    @Override
    public void initFrom(View view) {
        super.initFrom(view);
        if (view instanceof ImageView) {
            mIcon = (Icon) view.getTag(ICON_TAG);
        }
    }

    @Override
    protected boolean sameAs(TransformState otherState) {
        if (otherState instanceof ImageTransformState) {
            return mIcon != null && sameAs(mIcon, ((ImageTransformState) otherState).getIcon());
        }
        return super.sameAs(otherState);
    }

    @Override
    public void appear(float transformationAmount, TransformableView otherView) {
        if (otherView instanceof HybridNotificationView) {
            if (transformationAmount == 0.0f) {
                mTransformedView.setPivotY(0);
                mTransformedView.setPivotX(mTransformedView.getWidth() / 2);
                prepareFadeIn();
            }
            transformationAmount = mapToDuration(transformationAmount);
            CrossFadeHelper.fadeIn(mTransformedView, transformationAmount, false /* remap */);
            transformationAmount = Interpolators.LINEAR_OUT_SLOW_IN.getInterpolation(
                    transformationAmount);
            mTransformedView.setScaleX(transformationAmount);
            mTransformedView.setScaleY(transformationAmount);
        } else {
            super.appear(transformationAmount, otherView);
        }
    }

    @Override
    public void disappear(float transformationAmount, TransformableView otherView) {
        if (otherView instanceof HybridNotificationView) {
            if (transformationAmount == 0.0f) {
                mTransformedView.setPivotY(0);
                mTransformedView.setPivotX(mTransformedView.getWidth() / 2);
            }
            transformationAmount = mapToDuration(1.0f - transformationAmount);
            CrossFadeHelper.fadeOut(mTransformedView, 1.0f - transformationAmount,
                    false /* remap */);
            transformationAmount = Interpolators.LINEAR_OUT_SLOW_IN.getInterpolation(
                    transformationAmount);
            mTransformedView.setScaleX(transformationAmount);
            mTransformedView.setScaleY(transformationAmount);
        } else {
            super.disappear(transformationAmount, otherView);
        }
    }

    private static float mapToDuration(float scaleAmount) {
        // Assuming a linear interpolator, we can easily map it to our new duration
        scaleAmount = (scaleAmount * StackStateAnimator.ANIMATION_DURATION_STANDARD
                - (StackStateAnimator.ANIMATION_DURATION_STANDARD - ANIMATION_DURATION_LENGTH))
                        / ANIMATION_DURATION_LENGTH;
        return Math.max(Math.min(scaleAmount, 1.0f), 0.0f);
    }

    public Icon getIcon() {
        return mIcon;
    }

    public static ImageTransformState obtain() {
        ImageTransformState instance = sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ImageTransformState();
    }

    @Override
    protected boolean transformScale() {
        return true;
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }

    @Override
    protected void reset() {
        super.reset();
        mIcon = null;
    }

    public boolean sameAs(Icon thisIcon, Icon otherIcon) {
        if (otherIcon == thisIcon) {
            return true;
        }
        if (NotificationContentHelper.get(fieldType, thisIcon) != NotificationContentHelper.get(fieldType, otherIcon)) {
            return false;
        }
        switch ((int) NotificationContentHelper.get(fieldType, thisIcon)) {
            case 1 /*TYPE_BITMAP*/:
                return NotificationContentHelper.invoke(methodGetBitmap, thisIcon) == NotificationContentHelper.invoke(methodGetBitmap, otherIcon);
            case 3 /*TYPE_DATA*/:
                return NotificationContentHelper.invoke(methodGetDataLength, thisIcon) == NotificationContentHelper.invoke(methodGetDataLength, otherIcon)
                        && NotificationContentHelper.invoke(methodGetDataOffset, thisIcon) == NotificationContentHelper.invoke(methodGetDataOffset, otherIcon)
                        && NotificationContentHelper.invoke(methodGetDataBytes, thisIcon) == NotificationContentHelper.invoke(methodGetDataBytes, otherIcon);
            case 2 /*TYPE_RESOURCE*/:
                return NotificationContentHelper.invoke(methodGetResId, thisIcon) == NotificationContentHelper.invoke(methodGetResId, otherIcon)
                        && Objects.equals(NotificationContentHelper.invoke(methodGetResPackage, thisIcon), NotificationContentHelper.invoke(methodGetResPackage, otherIcon));
            case 4 /*TYPE_URI*/:
                return Objects.equals(NotificationContentHelper.invoke(methodGetUriString, thisIcon), NotificationContentHelper.invoke(methodGetUriString, otherIcon));
        }
        return false;
    }
}
