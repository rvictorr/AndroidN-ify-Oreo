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

package tk.wasdennnoch.androidn_ify.utils;

import android.app.Notification;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.ExpandableNotificationRowHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationContentHelper;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHeaderView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;

/**
 * A Util to manage {@link tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHeaderView} objects and their redundancies.
 */
public class NotificationHeaderUtil {

    private static final TextViewComparator sTextViewComparator = new TextViewComparator();
    private static final VisibilityApplicator sVisibilityApplicator = new VisibilityApplicator();
    private static  final DataExtractor sIconExtractor = new DataExtractor() {
        @Override
        public Object extractData(FrameLayout row) {
            return ((StatusBarNotification) XposedHelpers.callMethod(row, "getStatusBarNotification")).getNotification();
        }
    };
    private static final IconComparator sIconVisibilityComparator = new IconComparator() {
        public boolean compare(View parent, View child, Object parentData,
                               Object childData) {
            return hasSameIcon(parentData, childData)
                    && hasSameColor(parentData, childData);
        }
    };
    private static final IconComparator sGreyComparator = new IconComparator() {
        public boolean compare(View parent, View child, Object parentData,
                               Object childData) {
            return !hasSameIcon(parentData, childData)
                    || hasSameColor(parentData, childData);
        }
    };
    private final static ResultApplicator mGreyApplicator = new ResultApplicator() {
        @Override
        public void apply(View view, boolean apply) {
            NotificationHeaderView header = (NotificationHeaderView) view;
            ImageView icon = view.findViewById(
                    R.id.icon);
            ImageView expand = view.findViewById(
                    R.id.expand_button);
            applyToChild(icon, apply, header.getOriginalIconColor());
            applyToChild(expand, apply, header.getOriginalNotificationColor());
        }

        private void applyToChild(View view, boolean shouldApply, int originalColor) {
            if (originalColor != NotificationHeaderView.NO_COLOR) {
                ResourceUtils res = ResourceUtils.getInstance(view.getContext());
                ImageView imageView = (ImageView) view;
                imageView.getDrawable().mutate();
                if (shouldApply) {
                    // lets gray it out
                    int grey = res.getColor(
                            R.color.notification_icon_default_color);
                    imageView.getDrawable().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);
                } else {
                    // lets reset it
                    imageView.getDrawable().setColorFilter(originalColor,
                            PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
    };

    private final FrameLayout mRow;
    private final ArrayList<HeaderProcessor> mComparators = new ArrayList<>();
    private final HashSet<Integer> mDividers = new HashSet<>();

    public NotificationHeaderUtil(FrameLayout row) {
        mRow = row;
        // To hide the icons if they are the same and the color is the same
        mComparators.add(new HeaderProcessor(mRow,
                R.id.icon,
                sIconExtractor,
                sIconVisibilityComparator,
                sVisibilityApplicator));
        // To grey them out the icons and expand button when the icons are not the same
        mComparators.add(new HeaderProcessor(mRow,
                R.id.notification_header,
                sIconExtractor,
                sGreyComparator,
                mGreyApplicator));
//        mComparators.add(new HeaderProcessor(mRow, //TODO: fix this, NPE
//                R.id.profile_badge,
//                null /* Extractor */,
//                new ViewComparator() {
//                    @Override
//                    public boolean compare(View parent, View child, Object parentData,
//                                           Object childData) {
//                        return parent.getVisibility() != View.GONE;
//                    }
//
//                    @Override
//                    public boolean isEmpty(View view) {
//                        if (view instanceof ImageView) {
//                            return ((ImageView) view).getDrawable() == null;
//                        }
//                        return false;
//                    }
//                },
//                sVisibilityApplicator));
        mComparators.add(HeaderProcessor.forTextView(mRow,
                R.id.app_name_text));
        mComparators.add(HeaderProcessor.forTextView(mRow,
                R.id.header_text));
        mDividers.add(R.id.header_text_divider);
        mDividers.add(R.id.time_divider);
    }

    public void updateChildrenHeaderAppearance() {
        List<FrameLayout> notificationChildren = ReflectionUtils.invoke(Methods.SystemUI.ExpandableNotificationRow.getNotificationChildren, mRow);
        if (notificationChildren == null) {
            return;
        }
        // Initialize the comparators
        for (int compI = 0; compI < mComparators.size(); compI++) {
            mComparators.get(compI).init();
        }

        // Compare all notification headers
        for (int i = 0; i < notificationChildren.size(); i++) {
            FrameLayout row = notificationChildren.get(i);
            for (int compI = 0; compI < mComparators.size(); compI++) {
                mComparators.get(compI).compareToHeader(row);
            }
        }

        // Apply the comparison to the row
        for (int i = 0; i < notificationChildren.size(); i++) {
            FrameLayout row = notificationChildren.get(i);
            for (int compI = 0; compI < mComparators.size(); compI++) {
                mComparators.get(compI).apply(row);
            }
            // We need to sanitize the dividers since they might be off-balance now
            sanitizeHeaderViews(row);
        }
    }

    private void sanitizeHeaderViews(FrameLayout row) {
        ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);
        if (rowHelper.isSummaryWithChildren()) {
            sanitizeHeader(rowHelper.getNotificationHeader());
            return;
        }
        final NotificationContentHelper layoutHelper = rowHelper.getPrivateHelper();
        sanitizeChild(layoutHelper.getContractedChild());
        sanitizeChild(layoutHelper.getHeadsUpChild());
        sanitizeChild(layoutHelper.getExpandedChild());
    }

    private void sanitizeChild(View child) {
        if (child != null) {
            NotificationHeaderView header = child.findViewById(
                    R.id.notification_header);
            sanitizeHeader(header);
        }
    }

    private void sanitizeHeader(NotificationHeaderView rowHeader) {
        if (rowHeader == null) {
            return;
        }
        final int childCount = rowHeader.getChildCount();
        View time = rowHeader.findViewById(R.id.time);
        boolean hasVisibleText = false;
        for (int i = 1; i < childCount - 1 ; i++) {
            View child = rowHeader.getChildAt(i);
            if (child instanceof TextView
                    && child.getVisibility() != View.GONE
                    && !mDividers.contains(Integer.valueOf(child.getId()))
                    && child != time) {
                hasVisibleText = true;
                break;
            }
        }
        Notification notification = ((StatusBarNotification) ReflectionUtils.invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, mRow)).getNotification();
        // in case no view is visible we make sure the time is visible
        int timeVisibility = !hasVisibleText
                || NotificationHooks.showsTime(notification)
                ? View.VISIBLE : View.GONE;
        time.setVisibility(timeVisibility);
        View left = null;
        View right;
        for (int i = 1; i < childCount - 1 ; i++) {
            View child = rowHeader.getChildAt(i);
            if (mDividers.contains(Integer.valueOf(child.getId()))) {
                boolean visible = false;
                // Lets find the item to the right
                for (i++; i < childCount - 1; i++) {
                    right = rowHeader.getChildAt(i);
                    if (mDividers.contains(Integer.valueOf(right.getId()))) {
                        // A divider was found, this needs to be hidden
                        i--;
                        break;
                    } else if (right.getVisibility() != View.GONE && right instanceof TextView) {
                        visible = left != null;
                        left = right;
                        break;
                    }
                }
                child.setVisibility(visible ? View.VISIBLE : View.GONE);
            } else if (child.getVisibility() != View.GONE && child instanceof TextView) {
                left = child;
            }
        }
    }

    public void restoreNotificationHeader(FrameLayout row) {
        for (int compI = 0; compI < mComparators.size(); compI++) {
            mComparators.get(compI).apply(row, true /* reset */);
        }
        sanitizeHeaderViews(row);
    }

    private static class HeaderProcessor {
        private final int mId;
        private final DataExtractor mExtractor;
        private final ResultApplicator mApplicator;
        private final FrameLayout mParentRow;
        private final ExpandableNotificationRowHelper mParentHelper;
        private boolean mApply;
        private View mParentView;
        private ViewComparator mComparator;
        private Object mParentData;

        public static HeaderProcessor forTextView(FrameLayout row, int id) {
            return new HeaderProcessor(row, id, null, sTextViewComparator, sVisibilityApplicator);
        }

        HeaderProcessor(FrameLayout row, int id, DataExtractor extractor,
                        ViewComparator comparator,
                        ResultApplicator applicator) {
            mId = id;
            mExtractor = extractor;
            mApplicator = applicator;
            mComparator = comparator;
            mParentRow = row;
            mParentHelper = ExpandableNotificationRowHelper.getInstance(row);
        }

        public void init() {
            mParentView = mParentHelper.getNotificationHeader().findViewById(mId);
            mParentData = mExtractor == null ? null : mExtractor.extractData(mParentRow);
            mApply = !mComparator.isEmpty(mParentView);
        }
        public void compareToHeader(FrameLayout row) {
            if (!mApply) {
                return;
            }
            NotificationHeaderView header = ExpandableNotificationRowHelper.getInstance(row).getNotificationHeader();
            if (header == null) {
                mApply = false;
                return;
            }
            Object childData = mExtractor == null ? null : mExtractor.extractData(row);
            mApply = mComparator.compare(mParentView, header.findViewById(mId),
                    mParentData, childData);
        }

        public void apply(FrameLayout row) {
            apply(row, false /* reset */);
        }

        public void apply(FrameLayout row, boolean reset) {
            ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);
            boolean apply = mApply && !reset;
            if (rowHelper.isSummaryWithChildren()) {
                applyToView(apply, rowHelper.getNotificationHeader());
                return;
            }
            applyToView(apply, rowHelper.getPrivateHelper().getContractedChild());
            applyToView(apply, rowHelper.getPrivateHelper().getHeadsUpChild());
            applyToView(apply, rowHelper.getPrivateHelper().getExpandedChild());
        }

        private void applyToView(boolean apply, View parent) {
            if (parent != null) {
                View view = parent.findViewById(mId);
                if (view != null && !mComparator.isEmpty(view)) {
                    mApplicator.apply(view, apply);
                }
            }
        }
    }

    private interface ViewComparator {
        /**
         * @param parent the parent view
         * @param child the child view
         * @param parentData optional data for the parent
         * @param childData optional data for the child
         * @return whether to views are the same
         */
        boolean compare(View parent, View child, Object parentData, Object childData);
        boolean isEmpty(View view);
    }

    private interface DataExtractor {
        Object extractData(FrameLayout row);
    }

    private static class TextViewComparator implements ViewComparator {
        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            TextView parentView = (TextView) parent;
            TextView childView = (TextView) child;
            return parentView.getText().equals(childView.getText());
        }

        @Override
        public boolean isEmpty(View view) {
            return TextUtils.isEmpty(((TextView) view).getText());
        }
    }

    private static abstract class IconComparator implements ViewComparator {
        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return false;
        }

        protected boolean hasSameIcon(Object parentData, Object childData) {
            Icon parentIcon = ((Notification) parentData).getSmallIcon();
            Icon childIcon = ((Notification) childData).getSmallIcon();
            return ViewUtils.sameAs(parentIcon, childIcon);
        }

        /**
         * @return whether two ImageViews have the same colorFilterSet or none at all
         */
        protected boolean hasSameColor(Object parentData, Object childData) {
            int parentColor = ((Notification) parentData).color;
            int childColor = ((Notification) childData).color;
            return parentColor == childColor;
        }

        @Override
        public boolean isEmpty(View view) {
            return false;
        }
    }

    private interface ResultApplicator {
        void apply(View view, boolean apply);
    }

    private static class VisibilityApplicator implements ResultApplicator {

        @Override
        public void apply(View view, boolean apply) {
            view.setVisibility(apply ? View.GONE : View.VISIBLE);
        }
    }
}
