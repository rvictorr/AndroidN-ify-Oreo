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

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * An expand button in a notification
 */
@RemoteViews.RemoteView
public class NotificationExpandButton extends ImageView {
    public NotificationExpandButton(Context context) {
        super(context);
    }

    public NotificationExpandButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationExpandButton(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NotificationExpandButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        XposedHelpers.findAndHookMethod(View.class, "getBoundsOnScreen", Rect.class, boolean.class, getBoundsOnScreen);
    }

    private XC_MethodHook getBoundsOnScreen = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (param.thisObject instanceof NotificationExpandButton) {
                Rect outRect = (Rect) param.args[0];
                extendRectToMinTouchSize(outRect);
            }
        }
    };

    /*public void getBoundsOnScreen(Rect outRect, boolean clipToParent) {
        Method getBoundsOnScreen = null;
        try {
            getBoundsOnScreen = View.class.getDeclaredMethod("getBoundsOnScreen", Rect.class, boolean.class);
        } catch (NoSuchMethodException ignore) {}
        try {
            getBoundsOnScreen.invoke(this, outRect, clipToParent);
        } catch (InvocationTargetException | IllegalAccessException ignore) {}
        extendRectToMinTouchSize(outRect);
    }*/

    private void extendRectToMinTouchSize(Rect rect) {
        int touchTargetSize = (int) (getResources().getDisplayMetrics().density * 48);
        rect.left = rect.centerX() - touchTargetSize / 2;
        rect.right = rect.left + touchTargetSize;
        rect.top = rect.centerY() - touchTargetSize / 2;
        rect.bottom = rect.top + touchTargetSize;
    }
}
