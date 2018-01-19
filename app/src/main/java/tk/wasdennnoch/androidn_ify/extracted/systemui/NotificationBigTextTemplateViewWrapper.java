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

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationContentHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationsStuff;
import tk.wasdennnoch.androidn_ify.utils.ReflectionUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_ANDROID;

/**
 * Wraps a notification containing a big text template
 */
public class NotificationBigTextTemplateViewWrapper extends NotificationTemplateViewWrapper {

    private TextView mBigtext;

    protected NotificationBigTextTemplateViewWrapper(Context ctx, View view,
            View row) {
        super(ctx, view, row);
    }

    private void resolveViews(StatusBarNotification notification) {
        mBigtext = mView.findViewById(mView.getContext().getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
    }

    @Override
    public void onContentUpdated(View row) {
        // Reinspect the notification. Before the super call, because the super call also updates
        // the transformation types and we need to have our values set by then.
        resolveViews((StatusBarNotification) ReflectionUtils.invoke(NotificationsStuff.methodGetStatusBarNotification, row));
        super.onContentUpdated(row);
    }

    @Override
    protected void updateTransformedTypes() {
        // This also clears the existing types
        super.updateTransformedTypes();
        if (mBigtext != null) {
            mTransformationHelper.addTransformedView(TransformableView.TRANSFORMING_VIEW_TEXT,
                    mBigtext);
        }
    }
}
