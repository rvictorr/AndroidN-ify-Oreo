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

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.app.Notification;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

/**
 * A class managing hybrid groups that include {@link HybridNotificationView} and the notification
 * group overflow.
 */
public class HybridGroupManager {

    private final Context mContext;
    private ViewGroup mParent;
    private int mOverflowNumberColor;

    public HybridGroupManager(Context ctx, ViewGroup parent) {
        mContext = ctx;
        mParent = parent;
    }

    private HybridNotificationView inflateHybridView() {
        HybridNotificationView hybrid = (HybridNotificationView) getLayoutInflater(mContext).inflate(
                R.layout.hybrid_notification, mParent, false);
        mParent.addView(hybrid);
        return hybrid;
    }

    private TextView inflateOverflowNumber() {
        TextView numberView = (TextView) getLayoutInflater(mContext).inflate(
                R.layout.hybrid_overflow_number, mParent, false);
        mParent.addView(numberView);
        updateOverFlowNumberColor(numberView);
        return numberView;
    }

    private LayoutInflater getLayoutInflater(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(ResourceUtils.createOwnContext(context));
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name.equals(HybridNotificationView.class.getCanonicalName())) {
                    return new HybridNotificationView(context, attrs);
                } else {
                    return null;
                }
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }
        });
        return inflater;
    }

    private void updateOverFlowNumberColor(TextView numberView) {
        numberView.setTextColor(mOverflowNumberColor);
    }

    public void setOverflowNumberColor(TextView numberView, int overflowNumberColor) {
        mOverflowNumberColor = overflowNumberColor;
        if (numberView != null) {
            updateOverFlowNumberColor(numberView);
        }
    }

    public HybridNotificationView bindFromNotification(HybridNotificationView reusableView,
            Notification notification) {
        if (reusableView == null) {
            reusableView = inflateHybridView();
        }
        CharSequence titleText = resolveTitle(notification);
        CharSequence contentText = resolveText(notification);
        reusableView.bind(titleText, contentText);
        return reusableView;
    }

    private CharSequence resolveText(Notification notification) {
        CharSequence contentText = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (contentText == null) {
            contentText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        return contentText;
    }

    private CharSequence resolveTitle(Notification notification) {
        CharSequence titleText = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        if (titleText == null) {
            titleText = notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        }
        return titleText;
    }

    public TextView bindOverflowNumber(TextView reusableView, int number) {
        if (reusableView == null) {
            reusableView = inflateOverflowNumber();
        }
        ResourceUtils res = ResourceUtils.getInstance(mContext);
        String text = res.getResources().getString(
                R.string.notification_group_overflow_indicator, number);
        if (!text.equals(reusableView.getText())) {
            reusableView.setText(text);
        }
        String contentDescription = String.format(res.getResources().getQuantityString(
                R.plurals.notification_group_overflow_description, number), number);

        reusableView.setContentDescription(contentDescription);
        return reusableView;
    }
}
