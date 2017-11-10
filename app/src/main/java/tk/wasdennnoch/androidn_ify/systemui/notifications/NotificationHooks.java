package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XCallback;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputController;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.MediaNotificationView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationActionListLayout;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.systemui.statusbar.StatusBarHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.MarginSpan;
import tk.wasdennnoch.androidn_ify.utils.NotificationColorUtil;
import tk.wasdennnoch.androidn_ify.utils.RemoteLpTextView;
import tk.wasdennnoch.androidn_ify.utils.RemoteMarginLinearLayout;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

@SuppressLint("StaticFieldLeak")
public class NotificationHooks {

    private static final String TAG = "NotificationHooks";

    private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String KEY_EXPAND_CLICK_LISTENER = "expandClickListener";
    public static final String EXTRA_SUBSTITUTE_APP_NAME = "nify.substName";

    private static Class classExpandableNotificationRow;

    private static Field fieldState;

    private static int mNotificationBgColor;
    private static int mNotificationBgDimmedColor;
    private static int mAccentColor = 0;
    private static final Map<String, Integer> mGeneratedColors = new HashMap<>();

    protected static ArraySet<String> mKeysKeptForRemoteInput = new ArraySet<>();

    private static Object mPhoneStatusBar;

    public static boolean remoteInputActive = false;
    public static Object statusBarWindowManager = null;
    public static NotificationStackScrollLayoutHooks mStackScrollLayoutHooks;

    private static SensitiveNotificationFilter mSensitiveFilter = new SensitiveNotificationFilter();

    private static final XC_MethodHook inflateViewsHook = new XC_MethodHook() {

        @SuppressWarnings({"deprecation", "UnusedAssignment"})
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (!(boolean) param.getResult()) return;

            Object entry = param.args[0];
            Object row = XposedHelpers.getObjectField(entry, "row");
            ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);
            Object contentContainer = XposedHelpers.getObjectField(row, "mPrivateLayout"); // NotificationContentView
            Object contentContainerPublic = XposedHelpers.getObjectField(row, "mPublicLayout");
            final Object mHeadsUpManager = XposedHelpers.getObjectField(param.thisObject, "mHeadsUpManager");
            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");

            //if (row == null) {
                //row.setHeadsUpManager(mHeadsUpManager);
            rowHelper.setHeadsUpManager(mHeadsUpManager);
            rowHelper.setRemoteInputController(mRemoteInputController);
            if (mPhoneStatusBar != null) {
                rowHelper.setOnExpandClickListener(new ExpandableNotificationRowHelper.OnExpandClickListener() {
                    @Override
                    public void onExpandClicked(Object clickedEntry, boolean nowExpanded) {
                        //XposedHelpers.callMethod(mHeadsUpManager, "setExpanded", clickedEntry, nowExpanded); //TODO implement
                        if (NotificationContentHelper.getInt(fieldState, param.thisObject) == NotificationPanelHooks.STATE_KEYGUARD && nowExpanded) {
                            XposedHelpers.callMethod(mPhoneStatusBar, "goToLockedShade", XposedHelpers.getObjectField(clickedEntry, "row"));
                        }
                    }
                });
            }
            //}
            ((FrameLayout) row).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            rowHelper.onNotificationUpdated(entry);

            ConfigUtils.notifications().loadBlacklistedApps();
            StatusBarNotification sbn = (StatusBarNotification) XposedHelpers.callMethod(row, "getStatusBarNotification");
            if (ConfigUtils.notifications().blacklistedApps.contains(sbn.getPackageName())) return;

            View privateView = (View) XposedHelpers.callMethod(contentContainer, "getContractedChild");
            View publicView = (View) XposedHelpers.callMethod(contentContainerPublic, "getContractedChild");

            Context context = publicView.getContext();

            // Try to find app label for notifications without public version
            TextView appName = publicView.findViewById(R.id.public_app_name_text);
            if (appName == null) {
                // For notifications with public version
                appName = publicView.findViewById(R.id.app_name_text);
            }

            View time = publicView.findViewById(context.getResources().getIdentifier("time", "id", PACKAGE_SYSTEMUI));
            if (time != null) {
                publicView.findViewById(R.id.public_time_divider).setVisibility(time.getVisibility());
            }

            /*// Try to find icon for notifications without public version
            ImageView icon = publicView.findViewById(android.R.id.icon);
            if (icon == null) {
                // For notifications with public version
                icon = publicView.findViewById(R.id.notification_icon);
            }
            if (icon != null) {
                icon.setBackgroundResource(0);
                icon.setBackgroundColor(0x00000000);
                icon.setPadding(0, 0, 0, 0);
            }

            TextView privateAppName = privateView.findViewById(R.id.app_name_text);
            int color = privateAppName != null ? privateAppName.getTextColors().getDefaultColor() : sbn.getNotification().color;
            if (privateAppName != null) {
                if (appName != null) {
                    appName.setTextColor(privateAppName.getTextColors());
                    appName.setText(privateAppName.getText());
                }
                if (icon != null) {
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                }
            }*/

            // actions background
            View expandedChild = (View) XposedHelpers.callMethod(contentContainer, "getExpandedChild");
            View headsUpChild = ConfigUtils.M ? (View) XposedHelpers.callMethod(contentContainer, "getHeadsUpChild") : null;
            if (!ConfigUtils.notifications().custom_actions_color || !ConfigUtils.notifications().change_colors) {
                if (expandedChild != null || headsUpChild != null) {
                    int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
                    double[] lab = new double[3];
                    ColorUtils.colorToLAB(mNotificationBgColor, lab);
                    lab[0] = 1.0f - 0.95f * (1.0f - lab[0]);
                    int endColor = ColorUtils.setAlphaComponent(ColorUtils.LABToColor(lab[0], lab[1], lab[2]), Color.alpha(mNotificationBgColor));
                    if (expandedChild != null) {
                        View actionsExpanded = expandedChild.findViewById(actionsId);
                        if (actionsExpanded != null) {
                            actionsExpanded.setBackgroundColor(endColor);
                        }
                    }
                    if (headsUpChild != null) {
                        View actionsHeadsUp = headsUpChild.findViewById(actionsId);
                        if (actionsHeadsUp != null) {
                            actionsHeadsUp.setBackgroundColor(endColor);
                        }
                    }
                }
            }

            if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                Notification.Action[] actions = sbn.getNotification().actions;
                if (actions != null) {
                    addRemoteInput(context, expandedChild, actions, null, null);
                    if (ConfigUtils.M)
                        addRemoteInput(context, headsUpChild, actions, getObjectField(param.thisObject, "mHeadsUpManager"), (String) getObjectField(entry, "key"));
                }
            }
        }
    };

    private static void addRemoteInput(Context context, View child, Notification.Action[] actions, final Object headsUpManager, final String key) {
        if (child == null) {
            return;
        }
        if (ConfigUtils.notifications().blacklistedApps.contains(context.getPackageName())) {
            return;
        }
        NotificationActionListLayout actionsLayout = child.findViewById(context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID));
        if (actionsLayout == null) {
            return;
        }

        for (int i = 0; i < actions.length; i++) {
            final Notification.Action action = actions[i];
            if (actions[i].getRemoteInputs() != null) {
                Button actionButton = (Button) actionsLayout.getChildAt(i);
                final View.OnClickListener old = (View.OnClickListener) getObjectField(XposedHelpers.callMethod(actionButton, "getListenerInfo"), "mOnClickListener");
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (ConfigUtils.notifications().allow_direct_reply_on_keyguard) {
                            handleRemoteInput(view);
                        } else {
                            SystemUIHooks.startRunnableDismissingKeyguard(new Runnable() {
                                @Override
                                public void run() {
                                    SystemUIHooks.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            handleRemoteInput(view);
                                        }
                                    });
                                }
                            });
                        }
                    }

                    private void handleRemoteInput(View view) {
                        Object headsUpEntry = headsUpManager != null ? XposedHelpers.callMethod(headsUpManager, "getHeadsUpEntry", key) : null;
                        RemoteInputHelper.handleRemoteInput(view, action.getRemoteInputs(), action.actionIntent);
                        /*if (!RemoteInputHelper.handleRemoteInput(view, action.getRemoteInputs(), action.actionIntent)) {
                            old.onClick(view);
                        }*/
                    }
                });
            }
        }
    }

    private static final XC_MethodHook getStandardViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.getResult();
            Notification.Builder mBuilder = (Notification.Builder) XposedHelpers.getObjectField(param.thisObject, "mBuilder");

            CharSequence overflowText = (CharSequence) (XposedHelpers.getBooleanField(param.thisObject, "mSummaryTextSet")
                    ? XposedHelpers.getObjectField(param.thisObject, "mSummaryText")
                    : XposedHelpers.getObjectField(mBuilder, "mSubText"));
            if (overflowText != null) {
                contentView.setTextViewText(R.id.header_text, processLegacyText(mBuilder, overflowText));
                contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
                contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            }
        }
    };

    private static final XC_MethodHook makeBigContentViewBigTextHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.getResult();
            Object builder = XposedHelpers.getObjectField(param.thisObject, "mBuilder");
            Object largeIcon = XposedHelpers.getObjectField(builder, "mLargeIcon");
            if (largeIcon == null)
                return;
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            ResourceUtils res = ResourceUtils.getInstance(context);
            CharSequence mBigText = (CharSequence) XposedHelpers.getObjectField(param.thisObject, "mBigText");
            String bigText = (XposedHelpers.callMethod(builder, "processLegacyText", mBigText)).toString();
            contentView.setTextViewText(res.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID), processBigText(bigText, res));
        }
    };

    private static final XC_MethodReplacement makeBigContentViewInbox = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object builder = XposedHelpers.getObjectField(param.thisObject, "mBuilder");
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            ResourceUtils res = ResourceUtils.getInstance(context);
            CharSequence oldBuilderContentText = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");
            ArrayList<CharSequence> texts = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mTexts");
            XposedHelpers.setObjectField(builder, "mContentText", null);

            RemoteViews contentView = (RemoteViews) XposedHelpers.callMethod(param.thisObject, "getStandardView", XposedHelpers.callMethod(builder, "getInboxLayoutResource"));
            XposedHelpers.setObjectField(builder, "mContentText", oldBuilderContentText);

            //ugly
            int[] rowIds = {context.getResources().getIdentifier("inbox_text0", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text1", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text2", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text3", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text4", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text5", "id", PACKAGE_ANDROID),
                    context.getResources().getIdentifier("inbox_text6", "id", PACKAGE_ANDROID)};

            // Make sure all rows are gone in case we reuse a view.
            for (int rowId : rowIds) {
                contentView.setViewVisibility(rowId, View.GONE);
            }

            int i=0;
            int topPadding = res.getDimensionPixelSize(
                    R.dimen.notification_inbox_item_top_padding);
            boolean first = true;
            int onlyViewId = 0;
            int maxRows = rowIds.length;
            if (((ArrayList) XposedHelpers.getObjectField(builder, "mActions")).size() > 0) {
                if (texts.size() < maxRows) {
                    maxRows--;
                } else {
                    i++; //workaround for Whatsapp last message getting cut off
                }
            }
            while (i < texts.size() && i < maxRows) {
                CharSequence str = texts.get(i);
                if (!TextUtils.isEmpty(str)) {
                    contentView.setViewVisibility(rowIds[i], View.VISIBLE);
                    contentView.setTextViewText(rowIds[i], (CharSequence) XposedHelpers.callMethod(builder, "processLegacyText", str));

                    contentView.setViewPadding(rowIds[i], 0, topPadding, 0, 0);
                    handleInboxImageMargin(builder, res, contentView, rowIds[i], first);
                    if (first) {
                        onlyViewId = rowIds[i];
                    } else {
                        onlyViewId = 0;
                    }
                    first = false;
                }
                i++;
            }
            if (onlyViewId != 0) {
                // We only have 1 entry, lets make it look like the normal Text of a Bigtext
                topPadding = res.getDimensionPixelSize(
                        R.dimen.notification_text_margin_top);
                contentView.setViewPadding(onlyViewId, 0, topPadding, 0, 0);
            }
            return contentView;
        }
    };

    private static void handleInboxImageMargin(Object builder, ResourceUtils res, RemoteViews contentView, int id, boolean first) {
        int endMargin = 0;
        if (first) {
            final int max = XposedHelpers.getIntField(builder, "mProgressMax");
            final boolean ind = XposedHelpers.getBooleanField(builder, "mProgressIndeterminate");
            boolean hasProgress = max != 0 || ind;
            if (XposedHelpers.getObjectField(builder, "mLargeIcon") != null && !hasProgress) {
                endMargin = res.getDimensionPixelSize(R.dimen.notification_content_picture_margin);
            }
        }
        contentView.setInt(id, "setMarginEnd", endMargin);
    }

    private static CharSequence processBigText(String text, ResourceUtils res) { //really hacky way to add the picture margin to the first two lines, since we cannot use ImageFloatingTextView
        String[] paragraphs = text.split("\\n");
        SpannableString ss = new SpannableString(paragraphs[0]);
        ss.setSpan(new MarginSpan(2, res.getDimensionPixelSize(R.dimen.notification_content_picture_margin)), 1, ss.length(), 0);
        return TextUtils.concat(ss, new SpannableString(text.replace(paragraphs[0], "")));
    }

    private static CharSequence processLegacyText(Object notifBuilder, CharSequence text) {
        try {
            return (CharSequence) XposedHelpers.callMethod(notifBuilder, "processLegacyText", text);
        } catch (Throwable t) {
            return (CharSequence) XposedHelpers.callMethod(notifBuilder, "processText",
                    XposedHelpers.callMethod(notifBuilder, "getTextColor", 255), text);
        }
    }

    private static void bindNotificationHeader(RemoteViews contentView, XC_MethodHook.MethodHookParam param) {
        Object builder = param.thisObject;
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        int color = resolveColor(builder);
        bindSmallIcon(contentView, builder, color);
        bindHeaderAppName(contentView, context, color, extras);
        bindHeaderText(contentView, builder, context);
        bindHeaderChronometerAndTime(contentView, builder);
        bindExpandButton(contentView, color);
    }

    private static void bindHeaderChronometerAndTime(RemoteViews contentView, Object builder) {
        long mWhen = XposedHelpers.getLongField(builder, "mWhen");
        if ((boolean) XposedHelpers.callMethod(builder, "showsTimeOrChronometer")) {
            contentView.setViewVisibility(R.id.time_divider, View.VISIBLE);
            if (XposedHelpers.getBooleanField(builder, "mUseChronometer")) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        mWhen + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", mWhen);
            }
        } else {
            contentView.setLong(R.id.time, "setTime", mWhen);
        }
    }

    private static void bindHeaderText(RemoteViews contentView, Object builder, Context context) {
        CharSequence mContentText = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");
        CharSequence mSubText = (CharSequence) XposedHelpers.getObjectField(builder, "mSubText");
        if (mContentText != null && mSubText != null) {
            contentView.setTextViewText(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID),
                    processLegacyText(builder, mContentText));
            contentView.setViewVisibility(context.getResources().getIdentifier("text2", "id", PACKAGE_ANDROID), View.GONE);
            contentView.setTextViewText(R.id.header_text, processLegacyText(builder, mSubText));
            contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
        }
        XposedHelpers.callMethod(builder, "unshrinkLine3Text", contentView);
    }

    private static void bindSmallIcon(RemoteViews contentView, Object builder, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Icon mSmallIcon = (Icon) XposedHelpers.getObjectField(builder, "mSmallIcon");
            contentView.setImageViewIcon(android.R.id.icon, mSmallIcon);
        } else {
            int mSmallIcon = XposedHelpers.getIntField(builder, "mSmallIcon");
            contentView.setImageViewResource(android.R.id.icon, mSmallIcon);
        }

        processSmallIconColor(contentView, builder, color);
    }

    private static void bindHeaderAppName(RemoteViews contentView, Context context, int color, Bundle extras) {
        contentView.setTextViewText(R.id.app_name_text, loadHeaderAppName(context, extras));
        contentView.setTextColor(R.id.app_name_text, color);
    }

    private static void processSmallIconColor(RemoteViews contentView, Object builder, int resolveColor) {
        boolean colorable = true;
        int color = NotificationHeaderView.NO_COLOR;
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        boolean legacy = false;
        try {
            legacy = (boolean) XposedHelpers.callMethod(builder, "isLegacy");
        } catch (Throwable ignore) {
        }
        if (legacy) {
            Object mColorUtil = XposedHelpers.getObjectField(builder, "mColorUtil");
            Object mSmallIcon = XposedHelpers.getObjectField(builder, "mSmallIcon"); // Icon if Marshmallow, int if Lollipop. So we shouldn't specify which type is this.
            if (!(boolean) XposedHelpers.callMethod(mColorUtil, "isGrayscaleIcon", context, mSmallIcon)) {
                colorable = false;
            }
        }

        if (colorable) {
            color = resolveColor;
            XposedHelpers.callMethod(contentView, "setDrawableParameters", android.R.id.icon, false, -1, color,
                    PorterDuff.Mode.SRC_ATOP, -1);
        }

        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static void bindExpandButton(RemoteViews contentView, int color) {
        XposedHelpers.callMethod(contentView, "setDrawableParameters", R.id.expand_button, false, -1, color,
                PorterDuff.Mode.SRC_ATOP, -1);
        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static int resolveColor(Object builder) {
        return (int) XposedHelpers.callMethod(builder, "resolveColor");
    }

    private static String loadHeaderAppName(Context context, Bundle extras) {
        if (extras != null && extras.containsKey(EXTRA_SUBSTITUTE_APP_NAME)) {
            // TODO why this doesn't work
            final String pkg = context.getPackageName();
            final String subName = extras.getString(EXTRA_SUBSTITUTE_APP_NAME);
            if (pkg.equals(XposedHook.PACKAGE_OWN)) {
                return subName;
            }
        }

        CharSequence appname = context.getPackageName();
        if (appname.equals(PACKAGE_SYSTEMUI))
            return context.getString(context.getResources().getIdentifier("android_system_label", "string", PACKAGE_ANDROID));
        try {
            appname = context.getString(context.getApplicationInfo().labelRes);
        } catch (Throwable t) {
            try {
                appname = context.getApplicationInfo().loadLabel(context.getPackageManager());
            } catch (Throwable ignore) {
            }
        }

        return String.valueOf(appname);
    }

    /**
     * @return true if this is a media notification
     *
     */
    public static boolean isMediaNotification(Object builder) {
        Class<? extends Notification.Style> style = getNotificationStyle(builder);
        if (Notification.MediaStyle.class.equals(style)) {
            return true;
        } /*else if (Notification.DecoratedMediaCustomViewStyle.class.equals(style)) {
            return true;
        }*/
        return false;
    }

    public static boolean isColorizedMedia(Object builder) {
        /*Class<? extends Notification.Style> style = getNotificationStyle(builder);
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        if (Notification.MediaStyle.class.equals(style)) {
            Boolean colorized = (Boolean) extras.get(EXTRA_COLORIZED);
            if ((colorized == null || colorized) && hasMediaSession(builder)) {
                return true;
            }
        } *//*else if (Notification.DecoratedMediaCustomViewStyle.class.equals(style)) {
            if (extras.getBoolean(EXTRA_COLORIZED) && hasMediaSession()) {
                return true;
            }
        }*//*
        return false;*/
        return true;
    }

    public static Class<? extends Notification.Style> getNotificationStyle(Object builder) {
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        String templateClass = extras.getString(Notification.EXTRA_TEMPLATE);

        if (!TextUtils.isEmpty(templateClass)) {
            return (Class) XposedHelpers.callStaticMethod(Notification.class, "getNotificationStyleClass", templateClass);
        }
        return null;
    }

    public static boolean hasMediaSession(Object builder) {
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        return extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) != null;
    }

    public static void setColorPalette(Notification.Builder builder, int backgroundColor, int foregroundColor) {
        XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColor", backgroundColor);
        XposedHelpers.setAdditionalInstanceField(builder, "mForegroundColor", foregroundColor);
        //mTextColorsAreForBackground = COLOR_INVALID;
        //ensureColors(builder);
    }

    private static int getBackgroundColor(Object builder) {
        Object color = XposedHelpers.getAdditionalInstanceField(builder, "mBackgroundColor");
        //if (isColorized()) {
            return color != null ? (int) color : XposedHelpers.getIntField(builder, "mColor");
        /*} else {
            return mBackgroundColorHint != COLOR_INVALID ? mBackgroundColorHint
                    : COLOR_DEFAULT;
        }*/
    }

    /*private static void ensureColors(Notification.Builder builder) {
        int backgroundColor = getBackgroundColor();
        if (mPrimaryTextColor == COLOR_INVALID
                || mSecondaryTextColor == COLOR_INVALID
                || mActionBarColor == COLOR_INVALID
                || mTextColorsAreForBackground != backgroundColor) {
            mTextColorsAreForBackground = backgroundColor;
            if (mForegroundColor == COLOR_INVALID || !isColorized()) {
                mPrimaryTextColor = NotificationColorUtil.resolvePrimaryColor(mContext,
                        backgroundColor);
                mSecondaryTextColor = NotificationColorUtil.resolveSecondaryColor(mContext,
                        backgroundColor);
                if (backgroundColor != COLOR_DEFAULT
                        && (mBackgroundColorHint != COLOR_INVALID || isColorized())) {
                    mPrimaryTextColor = NotificationColorUtil.findAlphaToMeetContrast(
                            mPrimaryTextColor, backgroundColor, 4.5);
                    mSecondaryTextColor = NotificationColorUtil.findAlphaToMeetContrast(
                            mSecondaryTextColor, backgroundColor, 4.5);
                }
            } else {
                double backLum = NotificationColorUtil.calculateLuminance(backgroundColor);
                double textLum = NotificationColorUtil.calculateLuminance(mForegroundColor);
                double contrast = NotificationColorUtil.calculateContrast(mForegroundColor,
                        backgroundColor);
                // We only respect the given colors if worst case Black or White still has
                // contrast
                boolean backgroundLight = backLum > textLum
                        && satisfiesTextContrast(backgroundColor, Color.BLACK)
                        || backLum <= textLum
                        && !satisfiesTextContrast(backgroundColor, Color.WHITE);
                if (contrast < 4.5f) {
                    if (backgroundLight) {
                        mSecondaryTextColor = NotificationColorUtil.findContrastColor(
                                mForegroundColor,
                                backgroundColor,
                                true *//* findFG *//*,
                                4.5f);
                        mPrimaryTextColor = NotificationColorUtil.changeColorLightness(
                                mSecondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT);
                    } else {
                        mSecondaryTextColor =
                                NotificationColorUtil.findContrastColorAgainstDark(
                                        mForegroundColor,
                                        backgroundColor,
                                        true *//* findFG *//*,
                                        4.5f);
                        mPrimaryTextColor = NotificationColorUtil.changeColorLightness(
                                mSecondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK);
                    }
                } else {
                    mPrimaryTextColor = mForegroundColor;
                    mSecondaryTextColor = NotificationColorUtil.changeColorLightness(
                            mPrimaryTextColor, backgroundLight ? LIGHTNESS_TEXT_DIFFERENCE_LIGHT
                                    : LIGHTNESS_TEXT_DIFFERENCE_DARK);
                    if (NotificationColorUtil.calculateContrast(mSecondaryTextColor,
                            backgroundColor) < 4.5f) {
                        // oh well the secondary is not good enough
                        if (backgroundLight) {
                            mSecondaryTextColor = NotificationColorUtil.findContrastColor(
                                    mSecondaryTextColor,
                                    backgroundColor,
                                    true *//* findFG *//*,
                                    4.5f);
                        } else {
                            mSecondaryTextColor
                                    = NotificationColorUtil.findContrastColorAgainstDark(
                                    mSecondaryTextColor,
                                    backgroundColor,
                                    true *//* findFG *//*,
                                    4.5f);
                        }
                        mPrimaryTextColor = NotificationColorUtil.changeColorLightness(
                                mSecondaryTextColor, backgroundLight
                                        ? -LIGHTNESS_TEXT_DIFFERENCE_LIGHT
                                        : -LIGHTNESS_TEXT_DIFFERENCE_DARK);
                    }
                }
            }
            mActionBarColor = NotificationColorUtil.resolveActionBarColor(mContext,
                    backgroundColor);
        }
    }*/

    private static void updateBackgroundColor(Object builder, RemoteViews contentView) {
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        //if (isMediaNotification(builder)) {
            contentView.setInt(context.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID), "setBackgroundColor",
                    getBackgroundColor(builder));
        /*} else {
            // Clear it!
            contentView.setInt(R.id.status_bar_latest_event_content, "setBackgroundResource",
                    0);
        }*/
    }


    /**
     * Construct a RemoteViews for the final big notification layout.
     */
    public static RemoteViews createBigContentView(Object builder, Notification n) {
        RemoteViews result = null;
        Notification.Style mStyle = (Notification.Style) XposedHelpers.getObjectField(builder, "mStyle");
        ArrayList<Notification.Action> mActions = (ArrayList<Notification.Action>) XposedHelpers.getObjectField(builder, "mActions");
        if (n.bigContentView != null
                && (mStyle == null/* || !mStyle.displayCustomViewInline()*/)) {
            return n.bigContentView;
        } else if (mStyle != null) {
            result = (RemoteViews) XposedHelpers.callMethod(mStyle, "makeBigContentView");
            hideLine1Text(result);
        } else if (mActions.size() != 0) {
            result = (RemoteViews) XposedHelpers.callMethod(builder, "applyStandardTemplateWithActions", XposedHelpers.callMethod(builder, "getBigBaseLayoutResource"));
        }
        adaptNotificationHeaderForBigContentView(result);
        return result;
    }

    private static void hideLine1Text(RemoteViews result) {
        if (result != null) {
            result.setViewVisibility(R.id.text_line_1, View.GONE);
        }
    }

    private static void adaptNotificationHeaderForBigContentView(RemoteViews result) {
        if (result != null) {
            result.setBoolean(R.id.notification_header, "setExpanded", true);
        }
    }

    private static boolean handleProgressBar(boolean hasProgress, RemoteViews contentView, Object builder, Resources res) {
        final int max = XposedHelpers.getIntField(builder, "mProgressMax");
        final boolean ind = XposedHelpers.getBooleanField(builder, "mProgressIndeterminate");
        if (hasProgress && (max != 0 || ind)) {
            CharSequence text = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");
            contentView.setTextViewText(R.id.text_line_1, text);
            contentView.setViewVisibility(R.id.progress_container, View.VISIBLE);
            contentView.setViewVisibility(R.id.text_line_1, View.VISIBLE);
            contentView.setViewVisibility(res.getIdentifier("line3", "id", PACKAGE_ANDROID), View.GONE);
            return true;
        } else {
            contentView.setViewVisibility(R.id.progress_container, View.GONE);
            return false;
        }
    }

    private static void setContentMinHeight(RemoteViews remoteView, boolean hasMinHeight, Context context) {
        int minHeight = 0;
        if (hasMinHeight) {
            // we need to set the minHeight of the notification
            minHeight = ResourceUtils.getInstance(context).getResources().getDimensionPixelSize(
                    R.dimen.notification_min_content_height);
        }
        remoteView.setInt(context.getResources().getIdentifier("notification_main_column", "id", PACKAGE_ANDROID), "setMinimumHeight", minHeight);
    }

    private static final XC_MethodHook applyStandardTemplateHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object builder = param.thisObject;
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            Resources res = context.getResources();
            RemoteViews contentView = (RemoteViews) param.getResult();
            CharSequence mContentInfo = (CharSequence) XposedHelpers.getObjectField(builder, "mContentInfo");

            bindNotificationHeader(contentView, param);

            boolean showProgress = handleProgressBar((boolean) param.args[1], contentView, builder, res);

            if (XposedHelpers.getObjectField(builder, "mContentTitle") != null) {
                contentView.setInt(res.getIdentifier("title", "id", PACKAGE_ANDROID), "setWidth", showProgress
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : ViewGroup.LayoutParams.MATCH_PARENT);
            }

            if (mContentInfo != null) {
                contentView.setTextViewText(R.id.header_text, processLegacyText(builder, mContentInfo));
                contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
                contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            }
            Object largeIcon = XposedHelpers.getObjectField(builder, "mLargeIcon");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                contentView.setImageViewIcon(res.getIdentifier("right_icon", "id", PACKAGE_ANDROID), (Icon) largeIcon);
            } else {
                contentView.setImageViewBitmap(res.getIdentifier("right_icon", "id", PACKAGE_ANDROID), (Bitmap) largeIcon);
            }

            if (XposedHelpers.getObjectField(builder, "mLargeIcon") != null) {
                if (ConfigUtils.notifications().blacklistedApps.contains(context.getPackageName()))
                    return;
                int notificationTextMarginEnd = R.dimen.notification_text_margin_end;
                int progressBarContainerMargin = R.dimen.notification_content_plus_picture_margin_end;
                contentView.setInt(res.getIdentifier("line1", "id", PACKAGE_ANDROID), "setMarginEnd", notificationTextMarginEnd);
                contentView.setInt(R.id.progress_container, "setMarginEnd", progressBarContainerMargin);
                contentView.setInt(res.getIdentifier("line3", "id", PACKAGE_ANDROID), "setMarginEnd", notificationTextMarginEnd);
            }

            //setContentMinHeight(contentView, showProgress || (largeIcon != null), context);

            contentView.setInt(res.getIdentifier("right_icon", "id", PACKAGE_ANDROID), "setBackgroundResource", 0);
            //updateBackgroundColor(builder, contentView); //TODO finish
        }
    };

    private static final XC_MethodHook makeMediaContentViewHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Notification.Builder builder = (Notification.Builder) XposedHelpers.getObjectField(param.thisObject, "mBuilder");
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            RemoteViews view = (RemoteViews) param.getResult();
            if (ConfigUtils.notifications().blacklistedApps.contains(context.getPackageName()))
                return;

            if (XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "mLargeIcon") != null) {
                view.setInt(context.getResources().getIdentifier("line1", "id", PACKAGE_ANDROID), "setMarginEnd", R.dimen.zero);
                view.setInt(context.getResources().getIdentifier("line3", "id", PACKAGE_ANDROID), "setMarginEnd", R.dimen.zero);
            }
        }
    };

    private static XC_MethodHook generateMediaActionButtonHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "mContext");
            RemoteViews button = (RemoteViews) param.getResult();

            XposedHelpers.callMethod(button, "setDrawableParameters", context.getResources().getIdentifier("action0", "id", PACKAGE_ANDROID), false, -1,
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "resolveColor"),
                    PorterDuff.Mode.SRC_ATOP, -1);
        }
    };

    private static final XC_MethodHook resetStandardTemplateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.args[0];
            contentView.setImageViewResource(android.R.id.icon, 0);
            contentView.setBoolean(R.id.notification_header, "setExpanded", false);
            contentView.setTextViewText(R.id.app_name_text, null);
            contentView.setViewVisibility(R.id.chronometer, View.GONE);
            contentView.setViewVisibility(R.id.header_text, View.GONE);
            contentView.setTextViewText(R.id.header_text, null);
            contentView.setViewVisibility(R.id.header_text_divider, View.GONE);
            contentView.setViewVisibility(R.id.time_divider, View.GONE);
            contentView.setViewVisibility(R.id.time, View.GONE);
            contentView.setViewVisibility(R.id.text_line_1, View.GONE);
            contentView.setTextViewText(R.id.text_line_1, null);
            contentView.setViewVisibility(R.id.progress_container, View.GONE);
        }
    };

    private static final XC_MethodHook processSmallIconAsLargeHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean legacy = false;
            try {
                legacy = ((boolean) XposedHelpers.callMethod(param.thisObject, "isLegacy"));
            } catch (Throwable ignore) {
            }
            if (!legacy) {
                RemoteViews contentView = (RemoteViews) param.args[1];
                int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
                XposedHelpers.callMethod(contentView, "setDrawableParameters",
                        android.R.id.icon,
                        false,
                        -1,
                        mColor,
                        PorterDuff.Mode.SRC_ATOP,
                        -1);
            }
            return null;
        }
    };

    private static final XC_MethodHook applyStandardTemplateWithActionsHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            RemoteViews big = (RemoteViews) param.getResult();
            big.setViewVisibility(context.getResources().getIdentifier("action_divider", "id", PACKAGE_ANDROID), View.GONE);

            ArrayList<Notification.Action> mActions = (ArrayList<Notification.Action>) XposedHelpers.getObjectField(param.thisObject, "mActions");
            big.setInt(R.id.notification_action_list_margin_target, "setMarginBottom",
                    mActions.size() > 0 ? R.dimen.notification_action_list_height : R.dimen.zero);
            big.setViewVisibility(R.id.actions_container, mActions.size() > 0 ? View.VISIBLE : View.GONE);
        }
    };

    private static final XC_MethodHook resetStandardTemplateWithActionsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews big = (RemoteViews) param.args[0];
            big.setInt(R.id.notification_action_list_margin_target, "setMarginBottom",
                    R.dimen.zero);
        }
    };

    private static final XC_MethodHook generateActionButtonHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
            int textViewId = context.getResources().getIdentifier("action0", "id", PACKAGE_ANDROID);
            Notification.Action action = (Notification.Action) param.args[0];

            RemoteViews button = (RemoteViews) param.getResult();
            if (action.title != null && action.title.length() != 0) {
                button.setTextViewCompoundDrawablesRelative(textViewId, 0, 0, 0, 0);
                button.setTextColor(textViewId, mColor);
            } else {
                XposedHelpers.callMethod(button, "setTextViewCompoundDrawablesRelativeColorFilter", textViewId, 0, mColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    };

    private static final XC_MethodReplacement resolveColorHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object builder = param.thisObject;
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            int mColor = XposedHelpers.getIntField(builder, "mColor");
            NotificationColorUtil.setContext(context);
            if (mColor != 0) return NotificationColorUtil.resolveContrastColor(mColor); // App specified color in notification builder
            if (mAccentColor == 0) {
                //noinspection deprecation
                mAccentColor = context.getResources().getColor(context.getResources().getIdentifier("notification_icon_bg_color", "color", PACKAGE_ANDROID));
            }
            int c = NotificationColorUtil.resolveContrastColor(mAccentColor);
            if (ConfigUtils.notifications().generate_notification_accent_color) {
                String packageName = context.getPackageName();
                if (mGeneratedColors.containsKey(packageName))
                    return mGeneratedColors.get(packageName);
                try {
                    Drawable appIcon = context.getPackageManager().getApplicationIcon(packageName);
                    c = tk.wasdennnoch.androidn_ify.utils.ColorUtils.generateColor(appIcon, mAccentColor);
                    mGeneratedColors.put(packageName, c);
                } catch (PackageManager.NameNotFoundException ignore) {
                }
            }
            return c;
        }
    };

    private static final XC_MethodHook buildHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            ConfigUtils.notifications().loadBlacklistedApps();
            String name = ((Context) getObjectField(param.thisObject, "mContext")).getPackageName();
            if (!RemoteInputHelper.DIRECT_REPLY_ENABLED || ConfigUtils.notifications().blacklistedApps.contains(name)) {
                return;
            }

            Notification.Builder b = (Notification.Builder) param.thisObject;
            XposedHelpers.setBooleanField(b, "mHasThreeLines", true);
            @SuppressWarnings("unchecked") List<Notification.Action> actions = (List<Notification.Action>) getObjectField(b, "mActions");
            /*if (!actions.isEmpty()*//* && haveRemoteInput(actions.toArray(new Notification.Action[actions.size()]))*//*) {
                return;
            }*/
            final List<Notification.Action> wearableRemoteInputActions = new ArrayList<>();

            final String EXTRA_WEARABLE_EXTENSIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "EXTRA_WEARABLE_EXTENSIONS");
            final String KEY_ACTIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "KEY_ACTIONS");
            Bundle wearableBundle = b.getExtras().getBundle(EXTRA_WEARABLE_EXTENSIONS);
            if (wearableBundle != null) {
                ArrayList<Notification.Action> wearableActions = wearableBundle.getParcelableArrayList(KEY_ACTIONS);
                if (wearableActions != null) {
                    for (int i = 0; i < wearableActions.size(); i++) {
                        //if (hasValidRemoteInput(wearableActions.get(i))) {
                            wearableRemoteInputActions.add(wearableActions.get(i));
                        //}
                    }
                }
            }
            if (wearableRemoteInputActions.size() > 0) {
                actions.addAll(0, wearableRemoteInputActions);
                return;
            }

            try {
                RemoteInput carRemoteInput = null;
                PendingIntent carReplyPendingIntent = null;

                final String EXTRA_CAR_EXTENDER = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.class, "EXTRA_CAR_EXTENDER");
                final String EXTRA_CONVERSATION = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.class, "EXTRA_CONVERSATION");
                final String KEY_REMOTE_INPUT = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.UnreadConversation.class, "KEY_REMOTE_INPUT");
                final String KEY_ON_REPLY = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.UnreadConversation.class, "KEY_ON_REPLY");

                Bundle carBundle = b.getExtras().getBundle(EXTRA_CAR_EXTENDER);
                if (carBundle != null) {
                    Bundle unreadConversation = carBundle.getBundle(EXTRA_CONVERSATION);
                    if (unreadConversation != null) {
                        carRemoteInput = unreadConversation.getParcelable(KEY_REMOTE_INPUT);
                        carReplyPendingIntent = unreadConversation.getParcelable(KEY_ON_REPLY);
                    }
                }
                if (carRemoteInput != null && carReplyPendingIntent != null) {
                    //noinspection deprecation
                    actions.add(0, new Notification.Action.Builder(0, "Reply", carReplyPendingIntent).addRemoteInput(carRemoteInput).build());
                }
            } catch (NoClassDefFoundError e) {
                // Ignore
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error in buildHook (car extender)", t);
            }
        }

        /*@Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Notification n = (Notification) param.getResult();
            Context mContext = (Context) getObjectField(param.thisObject, "mContext");
            Notification.Style mStyle = (Notification.Style) XposedHelpers.getObjectField(param.thisObject, "mStyle");
            if (mContext.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.N
                    && (mStyle == null*//* || !mStyle.displayCustomViewInline()*//*)) {
                *//*if (n.contentView == null) {
                    n.contentView = createContentView();
                    n.extras.putInt(EXTRA_REBUILD_CONTENT_VIEW_ACTION_COUNT,
                            n.contentView.getSequenceNumber());
                }*//*
                if (n.bigContentView == null) {
                    n.bigContentView = createBigContentView(param.thisObject, n);
                    if (n.bigContentView != null) {
                        n.extras.putInt("android.rebuild.bigViewActionCount",
                                (int) XposedHelpers.callMethod(n.bigContentView, "getSequenceNumber"));
                    }
                }
                *//*if (n.headsUpContentView == null) {
                    n.headsUpContentView = createHeadsUpContentView();
                    if (n.headsUpContentView != null) {
                        n.extras.putInt(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW_ACTION_COUNT,
                                n.headsUpContentView.getSequenceNumber());
                    }
                }*//*
            }
        }*/
    };

    private static final XC_MethodHook makeBigContentViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews result = (RemoteViews) param.getResult();
            adaptNotificationHeaderForBigContentView(result);
        }
    };

    private static final XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(param.thisObject, "mScaleDimmed", false);
        }
    };

    private static final XC_MethodHook updateWindowWidthHHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (RomUtils.isOneplusStock()) {
                return;
            }
            Dialog mDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
            ViewGroup mDialogView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mDialogView");
            Context context = mDialogView.getContext();
            Window window = mDialog.getWindow();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mDialogView.getLayoutParams();
            lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
            mDialogView.setLayoutParams(lp);
            //noinspection deprecation
            mDialogView.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            mDialogView.requestLayout(); // Required to apply the new margin
            assert window != null;
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.horizontalMargin = 0;
            window.setAttributes(wlp);
        }
    };
    private static final XC_MethodHook updateWidthHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Dialog mDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
            Resources res = mDialog.getContext().getResources();
            Window window = mDialog.getWindow();
            assert window != null;
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.horizontalMargin = 0;
            window.setAttributes(wlp);
            ViewGroup mContentParent = (ViewGroup) XposedHelpers.getObjectField(window, "mContentParent");
            ViewGroup panel = mContentParent.findViewById(res.getIdentifier("visible_panel", "id", PACKAGE_SYSTEMUI));
            ViewGroup dialogView = (ViewGroup) panel.getParent();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dialogView.getLayoutParams();
            lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
            //noinspection deprecation
            dialogView.setBackgroundColor(res.getColor(res.getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            dialogView.requestLayout(); // Required to apply the new margin
        }
    };

    private static final XC_MethodHook dismissViewButtonConstructorHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (param.thisObject instanceof TextView) {
                TextView button = (TextView) param.thisObject; // It's a TextView on some ROMs

                Drawable mAnimatedDismissDrawable = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mAnimatedDismissDrawable");
                mAnimatedDismissDrawable.setBounds(0, 0, 0, 0);
                Drawable mStaticDismissDrawable = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mStaticDismissDrawable");
                mStaticDismissDrawable.setBounds(0, 0, 0, 0);
                button.setVisibility(View.VISIBLE);
            }
        }
    };

    private static final XC_MethodHook calculateTopPaddingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) param.args[0];
            if (ConfigUtils.notifications().blacklistedApps.contains(context.getPackageName()))
                return;
            param.setResult(0);
        }
    };

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();

            mSensitiveFilter.hookRes(resparam);

            final XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
            XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

            if (config.notifications.change_style) {
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", ConfigUtils.notifications().enable_notifications_background ? modRes.fwd(R.dimen.notification_divider_height) : zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding_dimmed", ConfigUtils.notifications().enable_notifications_background ? modRes.fwd(R.dimen.notification_divider_height) : zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "speed_bump_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_min_height", modRes.fwd(R.dimen.notification_min_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_mid_height", modRes.fwd(R.dimen.notification_mid_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_max_height", modRes.fwd(R.dimen.notification_max_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "min_stack_height", modRes.fwd(R.dimen.min_stack_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "keyguard_clock_notifications_margin_min", modRes.fwd(R.dimen.keyguard_clock_notifications_margin_min));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "keyguard_clock_notifications_margin_max", modRes.fwd(R.dimen.keyguard_clock_notifications_margin_max));

                if (config.notifications.change_colors) {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_color", modRes.fwd(R.color.notification_material_background_color));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_dimmed_color", modRes.fwd(R.color.notification_material_background_dimmed_color));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_low_priority_color", modRes.fwd(R.color.notification_material_background_low_priority_color));
                }

                if (config.notifications.change_keyguard_max)
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "integer", "keyguard_max_notification_count", config.notifications.keyguard_max);

                /*try {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_divider_height", zero);
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_padding", zero);
                    //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "z_distance_between_notifications", zero);
                } catch (Throwable ignore) {
                }*/

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "notification_public_default", notification_public_default);
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_no_notifications", status_bar_no_notifications);
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_row", status_bar_notification_row);
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_keyguard_overflow", status_bar_notification_row);
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_row_media", status_bar_notification_row);
                } catch (Throwable ignore) {
                }

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackground(xResources);
                    }
                });
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackgroundDimmed(xResources);
                    }
                });

            }

            if (config.notifications.dismiss_button) {
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_dismiss_all", status_bar_notification_dismiss_all);
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "recents_dismiss_button", status_bar_notification_dismiss_all);
                } catch (Exception ignored) {
                }
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    @SuppressWarnings("deprecation")
    private static RippleDrawable getNotificationBackground(XResources xRes) {
        mNotificationBgColor = xRes.getColor(xRes.getIdentifier("notification_material_background_color", "color", PACKAGE_SYSTEMUI));
        return new RippleDrawable(
                ColorStateList.valueOf(xRes.getColor(xRes.getIdentifier("notification_ripple_untinted_color", "color", PACKAGE_SYSTEMUI))),
                getBackgroundRippleContent(mNotificationBgColor),
                null);
    }

    @SuppressWarnings("deprecation")
    private static RippleDrawable getNotificationBackgroundDimmed(XResources xRes) {
        mNotificationBgDimmedColor = xRes.getColor(xRes.getIdentifier("notification_material_background_dimmed_color", "color", PACKAGE_SYSTEMUI));
        return new RippleDrawable(
                ColorStateList.valueOf(xRes.getColor(xRes.getIdentifier("notification_ripple_untinted_color", "color", PACKAGE_SYSTEMUI))),
                getBackgroundRippleContent(mNotificationBgDimmedColor),
                null);
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private static Drawable getBackgroundRippleContent(int color) {
        return new ColorDrawable(color);
    }

    @SuppressWarnings("unused")
    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.notifications().change_style) {
                //TODO see if i can figure something out with notifcompat

                Class classNotificationBuilder = Notification.Builder.class;
                Class classNotificationStyle = Notification.Style.class;
                Class classNotificationMediaStyle = Notification.MediaStyle.class;
                Class classNotificationBigTextStyle = Notification.BigTextStyle.class;
                Class classNotificationInboxStyle = Notification.InboxStyle.class;
                Class classRemoteViews = RemoteViews.class;

                if (ConfigUtils.M) {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", Icon.class, classRemoteViews, processSmallIconAsLargeHook);
                } else {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", int.class, classRemoteViews, processSmallIconAsLargeHook);
                }
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyLargeIconBackground", classRemoteViews, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplate", int.class, boolean.class, applyStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplateWithActions", int.class, applyStandardTemplateWithActionsHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resetStandardTemplateWithActions", classRemoteViews, resetStandardTemplateWithActionsHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resetStandardTemplate", RemoteViews.class, resetStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "generateActionButton", Notification.Action.class, generateActionButtonHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resolveColor", resolveColorHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "calculateTopPadding", Context.class, boolean.class, float.class, calculateTopPaddingHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "build", buildHook);
                //XposedHelpers.findAndHookMethod(classNotificationBuilder, "populateBigContentView", Notification.class, XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classNotificationBuilder, "makeBigContentView", makeBigContentViewHook); //TODO find a way to make it work
                XposedHelpers.findAndHookMethod(NotificationCompat.Builder.class, "build", buildHook);
                XposedHelpers.findAndHookMethod(classNotificationStyle, "getStandardView", int.class, getStandardViewHook);
                XposedHelpers.findAndHookMethod(classNotificationBigTextStyle, "makeBigContentView", makeBigContentViewBigTextHook);
                XposedHelpers.findAndHookMethod(classNotificationInboxStyle, "makeBigContentView", makeBigContentViewInbox);

                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "hideRightIcon", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "styleText", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "generateMediaActionButton", Notification.Action.class, generateMediaActionButtonHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "makeMediaContentView", makeMediaContentViewHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "makeMediaBigContentView", makeMediaContentViewHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setBuilderContentView", Notification.class, RemoteViews.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        MediaNotificationProcessor processor = new MediaNotificationProcessor(context, ResourceUtils.getPackageContext(context));
                        processor.processNotification((Notification) param.args[0], (Notification.Builder) param.thisObject);
                    }
                }); //TODO finish
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking app", t);
        }
    }

    private static boolean removeNotification(Object headsUpManager, String key, boolean ignoreEarliestRemovalTime) {
        if ((boolean) XposedHelpers.callMethod(headsUpManager, "wasShownLongEnough", key) || ignoreEarliestRemovalTime) {
            XposedHelpers.callMethod(headsUpManager, "releaseImmediately", key);
            return true;
        } else {
            XposedHelpers.callMethod(XposedHelpers.callMethod(headsUpManager, "getHeadsUpEntry", key), "removeAsSoonAsPossible");
            return false;
        }
    }

    private static void removeRemoteInputEntriesKeptUntilCollapsed() {
        ArraySet mRemoteInputEntriesToRemoveOnCollapse = (ArraySet) XposedHelpers.getAdditionalInstanceField(mPhoneStatusBar, "mRemoteInputEntriesToRemoveOnCollapse");
        RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(mPhoneStatusBar, "mRemoteInputController");
        for (int i = 0; i < mRemoteInputEntriesToRemoveOnCollapse.size(); i++) {
            Object entry = mRemoteInputEntriesToRemoveOnCollapse.valueAt(i);
            mRemoteInputController.removeRemoteInput(entry, null);
            XposedHelpers.callMethod(mPhoneStatusBar, "removeNotification",
                    XposedHelpers.getObjectField(entry, "key"),
                    XposedHelpers.getObjectField(mPhoneStatusBar, "mLatestRankingMap"));
        }
        mRemoteInputEntriesToRemoveOnCollapse.clear();
    }

    public static void hookSystemUI(ClassLoader classLoader) {
        try {
            final ConfigUtils config = ConfigUtils.getInstance();

            mSensitiveFilter.hook(classLoader);

            if (config.notifications.change_style) {

                StatusBarHooks.create(classLoader);

                if (ConfigUtils.M) // For now
                    mStackScrollLayoutHooks = new NotificationStackScrollLayoutHooks(classLoader);

                final Class classBaseStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar", classLoader);
                Class classPhoneStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
                boolean foundClass = false;
                Class classBroadcastReceiver = null;
                for(int i = 1; !foundClass; i++) {
                    classBroadcastReceiver = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar$" + i, classLoader);
                    if (BroadcastReceiver.class.isAssignableFrom(classBroadcastReceiver))
                        foundClass = true;
                }
                Class classEntry = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationData.Entry", classLoader);
                Class classStackScrollAlgorithm = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm", classLoader);
                Class classNotificationGuts = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationGuts", classLoader);
                classExpandableNotificationRow = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableNotificationRow", classLoader);
                final Class<?> classMediaExpandableNotificationRow = getClassMediaExpandableNotificationRow(classLoader);
                Class classActivatableNotificationView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ActivatableNotificationView", classLoader);
                fieldState = XposedHelpers.findField(classBaseStatusBar, "mState");

                if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                    Class classStatusBarWindowManager = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager", classLoader);
                    Class classStatusBarWindowManagerState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager.State", classLoader);

                    XposedHelpers.findAndHookMethod(View.class, "dispatchStartTemporaryDetach", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int mPrivateFlags3 = XposedHelpers.getIntField(param.thisObject, "mPrivateFlags3");
                            XposedHelpers.setIntField(param.thisObject, "mPrivateFlags3",  mPrivateFlags3 | 0x2000000/* PFLAG3_TEMPORARY_DETACH*/);
                            XposedHook.logD(TAG, "dispatchStartTemporaryDetach called");
                        }
                    });

                    XposedHelpers.findAndHookMethod(View.class, "dispatchFinishTemporaryDetach", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int mPrivateFlags3 = XposedHelpers.getIntField(param.thisObject, "mPrivateFlags3");
                            XposedHelpers.setIntField(param.thisObject, "mPrivateFlags3",  mPrivateFlags3 & ~0x2000000/* PFLAG3_TEMPORARY_DETACH*/);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            if (view.hasWindowFocus() && view.hasFocus()) {
                                InputMethodManager imm = (InputMethodManager) XposedHelpers.callStaticMethod(InputMethodManager.class, "getInstance");
                                XposedHelpers.callMethod(imm, "focusIn", view);
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(View.class, "onDetachedFromWindowInternal", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int mPrivateFlags3 = XposedHelpers.getIntField(param.thisObject, "mPrivateFlags3");
                            XposedHelpers.setIntField(param.thisObject, "mPrivateFlags3",  mPrivateFlags3 & ~0x2000000/* PFLAG3_TEMPORARY_DETACH*/);
                        }
                    });

                    XposedHelpers.findAndHookMethod(classBaseStatusBar, "updateNotificationViews", classEntry, StatusBarNotification.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object entry = param.args[0];
                            Object row = XposedHelpers.getObjectField(entry, "row");
                            ExpandableNotificationRowHelper.getInstance(row).onNotificationUpdated(entry);
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "startKeyguard", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");
                            //mRemoteInputController.addCallback(mStatusBarKeyguardViewManager);

                            mRemoteInputController.addCallback(new RemoteInputController.Callback() {
                                @Override
                                public void onRemoteInputActive(boolean active) {}

                                @Override
                                public void onRemoteInputSent(Object entry) {
                                    Object key = XposedHelpers.getObjectField(entry, "key");
                                    /*if (mKeysKeptForRemoteInput.contains(key)) {
                                        XposedHelpers.callMethod(param.thisObject, "removeNotification", key, null);
                                    } else if (mRemoteInputEntriesToRemoveOnCollapse.contains(entry)) {
                                        // We're currently holding onto this notification, but from the apps point of
                                        // view it is already canceled, so we'll need to cancel it on the apps behalf
                                        // after sending - unless the app posts an update in the mean time, so wait a
                                        // bit.
                                        mHandler.postDelayed(() -> {
                                            if (mRemoteInputEntriesToRemoveOnCollapse.remove(entry)) {
                                                removeNotification(entry.key, null);
                                            }
                                        }, REMOTE_INPUT_KEPT_ENTRY_AUTO_CANCEL_DELAY);
                                    }*/
                                }
                            });
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "onHeadsUpPinnedModeChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean inPinnedMode = (boolean) param.args[0];
                            Object mNotificationPanel = XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
                            View mStackScroller = (View) XposedHelpers.getObjectField(param.thisObject, "mStackScroller");

                            if (!inPinnedMode) {
                                if ((boolean) XposedHelpers.callMethod(mNotificationPanel, "isFullyCollapsed")
                                        || !(boolean) XposedHelpers.callMethod(mNotificationPanel, "isTracking")) {
                                    XposedHelpers.callMethod(mStackScroller, "runAfterAnimationFinished", new Runnable() {
                                        @Override
                                        public void run() {
                                            removeRemoteInputEntriesKeptUntilCollapsed();
                                        }
                                    });
                                }
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "setBarState", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int state = (int) param.args[0];
                            if (state == NotificationPanelHooks.STATE_KEYGUARD) {
                                removeRemoteInputEntriesKeptUntilCollapsed();
                                //maybeEscalateHeadsUp();
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "setPanelExpanded", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isExpanded = (boolean) param.args[0];
                            if (!isExpanded) {
                                removeRemoteInputEntriesKeptUntilCollapsed();
                            }
                        }
                    });

                    XposedBridge.hookAllMethods(classPhoneStatusBar, "addNotification", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object mNotificationData = XposedHelpers.getObjectField(param.thisObject, "mNotificationData");
                            XposedHelpers.callMethod(mNotificationData, "updateRanking", param.args[1]);
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "removeNotification", String.class, NotificationListenerService.RankingMap.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object mNotificationPanel = XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
                            Object mNotificationData = XposedHelpers.getObjectField(param.thisObject, "mNotificationData");
                            Object mHeadsUpManager = XposedHelpers.getObjectField(param.thisObject, "mHeadsUpManager");
                            Object row = null;
                            ArraySet mRemoteInputEntriesToRemoveOnCollapse = new ArraySet<>();
                            HashSet mHeadsUpEntriesToRemoveOnSwitch = (HashSet) XposedHelpers.getObjectField(param.thisObject, "mHeadsUpEntriesToRemoveOnSwitch");
                            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mRemoteInputEntriesToRemoveOnCollapse", mRemoteInputEntriesToRemoveOnCollapse); //TODO move to basestatusbar
                            String key = (String) param.args[0];
                            NotificationListenerService.RankingMap ranking = (NotificationListenerService.RankingMap) param.args[1];
                            boolean deferRemoval = false;
                            if ((boolean) XposedHelpers.callMethod(param.thisObject, "isHeadsUp", key)) {
                                // A cancel() in response to a remote input shouldn't be delayed, as it makes the
                                // sending look longer than it takes.
                                boolean ignoreEarliestRemovalTime = mRemoteInputController.isSpinning(key);
                                deferRemoval = !removeNotification(mHeadsUpManager, key, ignoreEarliestRemovalTime);
                            }
                            if (key.equals(XposedHelpers.getObjectField(param.thisObject, "mMediaNotificationKey"))) {
                                XposedHelpers.callMethod(param.thisObject, "clearCurrentMediaNotification");
                                XposedHelpers.callMethod(param.thisObject, "updateMediaMetaData", true);
                            }
                            if (deferRemoval) {
                                XposedHelpers.setObjectField(param.thisObject, "mLatestRankingMap", ranking);
                                mHeadsUpEntriesToRemoveOnSwitch.add(XposedHelpers.callMethod(mHeadsUpManager, "getEntry", key));
                                return null;
                            }
                            Object entry = XposedHelpers.callMethod(mNotificationData, "get", key);
                            if (entry != null) {
                                row = XposedHelpers.getObjectField(entry, "row");
                            }
                            if (entry != null && mRemoteInputController.isRemoteInputActive(entry)
                                    && (row != null/* && !(boolean) XposedHelpers.callMethod(row, "isDismissed")*/)) { //TODO implement
                                XposedHelpers.setObjectField(param.thisObject, "mLatestRankingMap",  ranking);
                                mRemoteInputEntriesToRemoveOnCollapse.add(entry);
                                return null;
                            }

                            if (entry != null && row != null) {
                                ExpandableNotificationRowHelper.getInstance(row).setRemoved();
                            }
                            // Let's remove the children if this was a summary
                            //handleGroupSummaryRemoved(key, ranking); //TODO implement
                            StatusBarNotification old = (StatusBarNotification) XposedHelpers.callMethod(param.thisObject, "removeNotificationViews", key, ranking);

                            if (old != null) {
                                if (!(boolean) XposedHelpers.callMethod(param.thisObject, "hasActiveNotifications")
                                        && !(boolean) XposedHelpers.callMethod(mNotificationPanel, "isTracking")
                                        && !(boolean) XposedHelpers.callMethod(mNotificationPanel, "isQsExpanded")) {
                                    int state = XposedHelpers.getIntField(param.thisObject, "mState");
                                    if (state == 0 /*StatusBarState.SHADE*/) {
                                        XposedHelpers.callMethod(param.thisObject, "animateCollapsePanels");
                                    } else if (state == 2 /*StatusBarState.SHADE_LOCKED*/ && !(boolean) XposedHelpers.callMethod(param.thisObject, "isCollapsing")) {
                                        XposedHelpers.callMethod(param.thisObject, "goToKeyguard");
                                    }
                                }
                            }
                            XposedHelpers.callMethod(param.thisObject, "setAreThereNotifications");
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(classBroadcastReceiver, "onReceive", Context.class, Intent.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[1];
                            String action = intent.getAction();
                            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                                RemoteInputController mRemoteInputController =
                                        (RemoteInputController) XposedHelpers.getAdditionalInstanceField(XposedHelpers.getSurroundingThis(param.thisObject), "mRemoteInputController");
                                if (mRemoteInputController != null) {
                                    mRemoteInputController.closeRemoteInputs();
                                }
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "addStatusBarWindow", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            statusBarWindowManager = XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindowManager");
                            Object headsUpManager = XposedHelpers.getObjectField(param.thisObject, "mHeadsUpManager");
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mRemoteInputController", new RemoteInputController(statusBarWindowManager,
                                    headsUpManager));
                        }
                    });

                    XposedHelpers.findAndHookMethod(classStatusBarWindowManager, "applyFocusableFlag", classStatusBarWindowManagerState, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)
                                    XposedHelpers.getObjectField(param.thisObject, ConfigUtils.L1 ? "mLpChanged" : "mLp");
                            if (remoteInputActive) {
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
                                param.setResult(null);
                            }
                            windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)
                                    XposedHelpers.getObjectField(param.thisObject, ConfigUtils.L1 ? "mLpChanged" : "mLp");

                            windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classBaseStatusBar, "inflateViews", classEntry, ViewGroup.class, inflateViewsHook);
                XposedHelpers.findAndHookMethod(classStackScrollAlgorithm, "initConstants", Context.class, initConstantsHook);
                XposedHelpers.findAndHookMethod(classNotificationGuts, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Drawable bg = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mBackground");
                        if (bg instanceof ShapeDrawable) {
                            ((ShapeDrawable) bg).getPaint().setPathEffect(null);
                        } else if (bg instanceof GradientDrawable) {
                            ((GradientDrawable) bg).setCornerRadius(0);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setIconAnimationRunningForChild", boolean.class, View.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (classMediaExpandableNotificationRow != null && classMediaExpandableNotificationRow.isAssignableFrom(param.thisObject.getClass()))
                            return null;
                        boolean running = (boolean) param.args[0];
                        View child = (View) param.args[1];
                        if (child != null) {
                            ImageView icon = child.findViewById(android.R.id.icon);
                            if (icon == null)
                                icon = child.findViewById(com.android.internal.R.id.icon);
                            if (icon != null)
                                setIconRunning(param.thisObject, icon, running);
                            ImageView rightIcon = child.findViewById(
                                    com.android.internal.R.id.right_icon);
                            if (rightIcon != null)
                                setIconRunning(param.thisObject, rightIcon, running);
                        }
                        return null;
                    }

                    private void setIconRunning(Object row, ImageView icon, boolean running) {
                        XposedHelpers.callMethod(row, "setIconRunning", icon, running);
                    }
                });

                if (ConfigUtils.M && !ConfigUtils.notifications().enable_notifications_background) {
                    XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setHeadsUp", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isHeadsUp = (boolean) param.args[0];
                            FrameLayout row = (FrameLayout) param.thisObject;
                            row.findViewById(R.id.notification_divider).setAlpha(isHeadsUp ? 0 : 1);
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "start", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        mPhoneStatusBar = param.thisObject;
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = XposedHelpers.getObjectField(NotificationHooks.mPhoneStatusBar, "mNavigationBarView");
                        if (mNavigationBarView == null) {
                            QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                            if (qsCustomizer != null)
                                qsCustomizer.setHasNavBar(false);
                        }
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mRemoteInputEntriesToRemoveOnCollapse", new ArraySet<>());
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "onBackPressed", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.onBackPressed()) {
                            param.setResult(true);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "animateCollapsePanels", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.isCustomizing()) {
                            qsCustomizer.hide(true);
                        }
                    }
                });

                if (!ConfigUtils.notifications().enable_notifications_background) {
                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "updateNotificationShade", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ViewGroup stack = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStackScroller");
                            int childCount = stack.getChildCount();
                            boolean firstChild = true;
                            for (int i = 0; i < childCount; i++) {
                                View child = stack.getChildAt(i);
                                if (!classExpandableNotificationRow.isInstance(child)) {
                                    continue;
                                }
                                child.findViewById(R.id.notification_divider).setVisibility(firstChild ? View.INVISIBLE : View.VISIBLE);
                                firstChild = false;
                            }
                        }
                    });
                }

                if (ConfigUtils.M) {
                    Class classVolumeDialog = XposedHelpers.findClass("com.android.systemui.volume.VolumeDialog", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumeDialog, "updateWindowWidthH", updateWindowWidthHHook);
                } else {
                    Class classVolumePanel = XposedHelpers.findClass("com.android.systemui.volume.VolumePanel", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumePanel, "updateWidth", updateWidthHook);
                }

                XposedBridge.hookAllMethods(classBaseStatusBar, "applyColorsAndBackgrounds", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object entry = param.args[1];
                            View contentView = ConfigUtils.M ? (View) XposedHelpers.callMethod(entry, "getContentView") : (View) XposedHelpers.getObjectField(entry, "expanded");
                            if (contentView.getId() !=
                                    contentView.getContext().getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID)) {
                                // Using custom RemoteViews
                                int targetSdk = XposedHelpers.getIntField(entry, "targetSdk");
                                if (targetSdk >= Build.VERSION_CODES.GINGERBREAD
                                        && targetSdk < Build.VERSION_CODES.LOLLIPOP) {
                                    XposedHelpers.callMethod(XposedHelpers.getObjectField(entry, "row"), "setShowingLegacyBackground", true);
                                    XposedHelpers.setBooleanField(entry, "legacy", true);
                                }
                            }

                            ImageView icon = (ImageView) XposedHelpers.getObjectField(entry, "icon");
                            if (icon != null) {
                                int targetSdk = XposedHelpers.getIntField(entry, "targetSdk");
                                if (Build.VERSION.SDK_INT > 22) {
                                    icon.setTag(icon.getResources().getIdentifier("icon_is_pre_L", "id", PACKAGE_SYSTEMUI), targetSdk < Build.VERSION_CODES.LOLLIPOP);
                                } else {
                                    if (targetSdk >= Build.VERSION_CODES.LOLLIPOP) {
                                        icon.setColorFilter(icon.getResources().getColor(android.R.color.white));
                                    } else {
                                        icon.setColorFilter(null);
                                    }
                                }
                            }
                            param.setResult(null);
                        } catch (Throwable t) {
                            XposedHook.logE(TAG, "Error in applyColorsAndBackgrounds", t);
                        }
                    }
                });

                if (config.notifications.change_colors) {
                    XposedHelpers.findAndHookConstructor(classActivatableNotificationView, Context.class, AttributeSet.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) param.args[0];
                            ResourceUtils res = ResourceUtils.getInstance(context);
                            XposedHelpers.setIntField(param.thisObject, "mNormalColor", res.getColor(R.color.notification_material_background_color));
                            XposedHelpers.setIntField(param.thisObject, "mLowPriorityColor", res.getColor(R.color.notification_material_background_low_priority_color));
                        }
                    });
                }

            }

            if (config.notifications.dismiss_button) {
                Class classDismissViewButton;
                try {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewButton", classLoader);
                } catch (Throwable t) {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewImageButton", classLoader);
                }
                XposedHelpers.findAndHookConstructor(classDismissViewButton, Context.class, AttributeSet.class, int.class, int.class, dismissViewButtonConstructorHook);
                XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.DismissView", classLoader, "setOnButtonClickListener", View.OnClickListener.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        ((View) XposedHelpers.callMethod(param.thisObject, "findContentView")).setOnClickListener((View.OnClickListener) param.args[0]);
                        return null;
                    }
                });
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    public static Class getClassExpandableNotificationRow() {
        return classExpandableNotificationRow;
    }

    private static Class<?> getClassMediaExpandableNotificationRow(ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass("com.android.systemui.statusbar.MediaExpandableNotificationRow", classLoader);
        } catch (Throwable t) {
            XposedHook.logD(TAG, "Class MediaExpandableNotificationRow not found. Skipping media row check.");
        }
        return null;
    }

    private static boolean haveRemoteInput(@NonNull Notification.Action[] actions) {
        for (Notification.Action a : actions) {
            if (a.getRemoteInputs() != null) {
                for (RemoteInput ri : a.getRemoteInputs()) {
                    if (ri.getAllowFreeFormInput()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasValidRemoteInput(Notification.Action action) {
        if ((TextUtils.isEmpty(action.title)) || (action.actionIntent == null)) {
            return false;
        }
        RemoteInput[] remoteInputs = action.getRemoteInputs();
        if (remoteInputs == null || remoteInputs.length == 0) {
            return false;
        }

        for (RemoteInput input : remoteInputs) {
            CharSequence[] choices = input.getChoices();
            if (input.getAllowFreeFormInput() || (choices != null && choices.length != 0)) {
                return true;
            }
        }
        return false;
    }

    public static void hookResAndroid(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        try {

            if (ConfigUtils.notifications().change_style) {

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action", notification_material_action);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action_list", notification_material_action_list);
                try { //OOS
                    resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action_list_padding", notification_material_action_list);
                } catch (Throwable ignore) {}
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_media", notification_template_material_media);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_big_base); // Extra treatment
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media", notification_template_material_big_media);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media_narrow", notification_template_material_big_media);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_picture", notification_template_material_big_picture);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", notification_template_material_big_text);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", notification_template_material_inbox);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_media_action", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        ImageButton action = (ImageButton) liparam.view;
                        Context context = action.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        int width_height = res.getDimensionPixelSize(R.dimen.media_notification_action_button_size);
                        int padding = ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_media_action_padding);

                        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(width_height, width_height);
                        lParams.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_media_action_margin));
                        lParams.setMargins(0, 0, res.getDimensionPixelSize(R.dimen.notification_media_action_margin), 0);
                        action.setLayoutParams(lParams);
                        action.setPadding(padding, padding, padding, padding);
                        action.setBackground(res.getDrawable(R.drawable.notification_material_media_action_background));
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line1", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;
                        Context context = layout.getContext();

                        while (layout.getChildCount() > 1) {
                            layout.removeViewAt(1);
                        }

                        ViewGroup parentLayout = (ViewGroup) layout.getParent();
                        TextView title = (TextView) layout.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID));
                        TextView textLine1 = new TextView(context);
                        TextView newTitle = new RemoteLpTextView(context);
                        LinearLayout newLayout = new RemoteMarginLinearLayout(context);

                        if (title == null)
                            return;

                        newTitle.setId(title.getId());
                        newTitle.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Title);
                        newTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_title_text_size));
                        newTitle.setSingleLine();
                        newTitle.setEllipsize(TextUtils.TruncateAt.END);
                        newTitle.setHorizontalFadingEdgeEnabled(true);

                        layout.removeView(title);

                        newLayout.setId(layout.getId());

                        textLine1.setId(R.id.text_line_1);
                        textLine1.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification);
                        textLine1.setGravity(Gravity.END | Gravity.BOTTOM);
                        textLine1.setSingleLine();
                        textLine1.setEllipsize(TextUtils.TruncateAt.END);
                        textLine1.setHorizontalFadingEdgeEnabled(true);

                        LinearLayout.LayoutParams textLine1Lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                        textLine1Lp.setMarginStart(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_text_line1_margin_start));

                        newTitle.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                        textLine1.setLayoutParams(textLine1Lp);

                        layout.removeView(title);
                        newLayout.addView(newTitle);
                        newLayout.addView(textLine1);
                        parentLayout.removeView(layout);
                        parentLayout.addView(newLayout);
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line3", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;

                        Context context = layout.getContext();

                        if (layout.getChildAt(1) != null) {
                            layout.removeViewAt(1);
                        }

                        LinearLayout container = new RemoteMarginLinearLayout(context);
                        container.setId(layout.getId());

                        while (layout.getChildCount() > 0) {
                            View view = layout.getChildAt(0);
                            layout.removeView(view);
                            container.addView(view);
                        }
                        ViewGroup parent = (ViewGroup) layout.getParent();
                        if (parent == null)
                            return;
                        parent.removeView(layout);
                        parent.addView(container);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking framework resources", t);
        }
    }

    private static final XC_LayoutInflated notification_template_material_big_base = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            FrameLayout actionsContainer = layout.findViewById(R.id.actions_container);
            LinearLayout notificationActionListMarginTarget = new RemoteMarginLinearLayout(context);
            FrameLayout container = new FrameLayout(context);

            notificationActionListMarginTarget.setOrientation(LinearLayout.VERTICAL);
            notificationActionListMarginTarget.setId(R.id.notification_action_list_margin_target);
            FrameLayout.LayoutParams notificationActionListMarginTargetLp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationActionListMarginTargetLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_action_list_height);

            LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            containerLp.gravity = Gravity.TOP;

            notificationActionListMarginTarget.setLayoutParams(notificationActionListMarginTargetLp);
            container.setLayoutParams(containerLp);

            notificationActionListMarginTarget.addView(container);
            notificationMain.removeView(actionsContainer);

            while (layout.getChildCount() > 0) {
                View v = layout.getChildAt(0);
                layout.removeView(v);
                container.addView(v);
            }
            layout.addView(notificationActionListMarginTarget);
            layout.addView(actionsContainer);
        }
    };

    private static final XC_LayoutInflated notification_template_material_big_text = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notifMainPadding = res.getDimensionPixelSize(R.dimen.notification_inbox_padding);

            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            layout.findViewById(context.getResources().getIdentifier("line3", "id", PACKAGE_ANDROID)).setVisibility(View.GONE);
            layout.findViewById(context.getResources().getIdentifier("overflow_divider", "id", PACKAGE_ANDROID)).setVisibility(View.GONE);

            ImageView rightIcon = layout.findViewById(context.getResources().getIdentifier("right_icon", "id", PACKAGE_ANDROID));
            TextView bigText = layout.findViewById(context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
            NotificationHeaderView header = layout.findViewById(R.id.notification_header);
            FrameLayout actionsContainer = layout.findViewById(R.id.actions_container);
            LinearLayout progressContainer = layout.findViewById(R.id.progress_container);
            LinearLayout notificationActionListMarginTarget = new RemoteMarginLinearLayout(context);

            layout.removeView(progressContainer);

            bigText.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.notification_inbox_padding));
            bigText.setGravity(Gravity.TOP);

            LinearLayout bigTextParent = (LinearLayout) bigText.getParent();
            bigTextParent.removeView(bigText);
            notificationMain.removeView(bigTextParent);
            notificationMain.addView(bigText);
            notificationMain.removeView(actionsContainer);

            notificationActionListMarginTarget.setOrientation(LinearLayout.VERTICAL);
            notificationActionListMarginTarget.setId(R.id.notification_action_list_margin_target);
            FrameLayout.LayoutParams notificationActionListMarginTargetLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            notificationActionListMarginTargetLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            notificationActionListMarginTargetLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_action_list_height);

            LinearLayout.LayoutParams notificationMainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationMainLp.gravity = Gravity.TOP;
            notificationMain.setPaddingRelative(notifMainPadding, 0, notifMainPadding, 0);

            bigText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
            notificationActionListMarginTarget.setLayoutParams(notificationActionListMarginTargetLp);
            notificationMain.setLayoutParams(notificationMainLp);

            ((ViewGroup)rightIcon.getParent()).removeView(rightIcon);
            ((ViewGroup)header.getParent()).removeView(header);

            notificationMain.addView(progressContainer, 2);

            while (layout.getChildCount() > 0) {
                View v = layout.getChildAt(0);
                layout.removeView(v);
                notificationActionListMarginTarget.addView(v);
            }
            layout.addView(header);
            layout.addView(notificationActionListMarginTarget);
            layout.addView(actionsContainer);
            layout.addView(rightIcon);
            notificationMain.removeView(layout.findViewById(res.getResources().getIdentifier("line3", "id", PACKAGE_ANDROID)));
            notificationMain.removeView(layout.findViewById(res.getResources().getIdentifier("overflow_divider", "id", PACKAGE_ANDROID)));
        }
    };

    private static final XC_LayoutInflated notification_material_action_list = new XC_LayoutInflated(XCallback.PRIORITY_HIGHEST) {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            LinearLayout container = (LinearLayout) liparam.view;
            Context context = container.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);
            int actionsId = container.getId();
            int padding = res.getDimensionPixelSize(R.dimen.notification_action_button_margin_start);
            FrameLayout newContainer = new FrameLayout(context);
            ViewGroup parent = (ViewGroup) container.getParent();

            newContainer.setId(R.id.actions_container);

            NotificationActionListLayout notificationActionListLayout = new NotificationActionListLayout(context, null);
            notificationActionListLayout.setId(actionsId);
            notificationActionListLayout.setPaddingRelative(0, 0, padding, 0);
            notificationActionListLayout.setBackgroundColor(res.getColor(R.color.notification_action_list));
            notificationActionListLayout.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, res.getDimensionPixelSize(R.dimen.notification_action_list_height), Gravity.CENTER_VERTICAL));

            newContainer.addView(notificationActionListLayout);
            parent.removeView(container);
            parent.addView(newContainer);
            newContainer.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM));
        }
    };

    private static final XC_LayoutInflated notification_template_material_inbox = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notifMainPadding = res.getDimensionPixelSize(R.dimen.notification_inbox_padding);

            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            ImageView rightIcon = layout.findViewById(context.getResources().getIdentifier("right_icon", "id", PACKAGE_ANDROID));
            TextView text0 = layout.findViewById(context.getResources().getIdentifier("inbox_text0", "id", PACKAGE_ANDROID));
            TextView text6 = layout.findViewById(context.getResources().getIdentifier("inbox_text6", "id", PACKAGE_ANDROID));
            FrameLayout actionsContainer = notificationMain.findViewById(R.id.actions_container);
            NotificationHeaderView header = layout.findViewById(R.id.notification_header);
            LinearLayout progressContainer = layout.findViewById(R.id.progress_container);
            LinearLayout notificationActionListMarginTarget = new RemoteMarginLinearLayout(context);

            layout.removeView(progressContainer);

            LinearLayout text0Container = (LinearLayout) text0.getParent();
            text0Container.removeAllViews();
            notificationMain.addView(text0, 3);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            lp.topMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            notificationActionListMarginTarget.setOrientation(LinearLayout.VERTICAL);
            notificationActionListMarginTarget.setId(R.id.notification_action_list_margin_target);
            notificationActionListMarginTarget.setClipToPadding(false);

            LinearLayout.LayoutParams notificationMainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationMainLp.gravity = Gravity.TOP;
            notificationMain.setPaddingRelative(notifMainPadding, 0, notifMainPadding, notifMainPadding);
            notificationMain.setClipToPadding(false);

            ViewUtils.setMarginEnd((View) text0.getParent(),
                    res.getDimensionPixelSize(R.dimen.notification_content_picture_margin));

            notificationActionListMarginTarget.setLayoutParams(lp);
            notificationMain.setLayoutParams(notificationMainLp);

            ((ViewGroup)rightIcon.getParent()).removeView(rightIcon);
            ((ViewGroup)header.getParent()).removeView(header);
            notificationMain.removeView(actionsContainer);

            while (layout.getChildCount() > 0) {
                View v = layout.getChildAt(0);
                layout.removeView(v);
                notificationActionListMarginTarget.addView(v);
            }
            notificationMain.addView(progressContainer, 2);
            layout.addView(header);
            layout.addView(notificationActionListMarginTarget);
            layout.addView(actionsContainer);
            layout.addView(rightIcon);
            // Remove crap
            View v;
            while ((v = notificationMain.getChildAt(notificationMain.getChildCount() - 1)) != text6) {
                notificationMain.removeView(v);
            }
            for (int i = 3; i < notificationMain.getChildCount(); i++) {
                TextView line = new RemoteLpTextView(context);
                line.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
                line.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification);
                line.setSingleLine();
                line.setEllipsize(TextUtils.TruncateAt.END);
                line.setVisibility(View.GONE);
                line.setId(notificationMain.getChildAt(i).getId());
                notificationMain.removeViewAt(i);
                notificationMain.addView(line, i);
            }
            notificationMain.removeView(layout.findViewById(res.getResources().getIdentifier("line3", "id", PACKAGE_ANDROID))); //remove line3
        }
    };

    private static final XC_LayoutInflated status_bar_no_notifications = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int height = res.getDimensionPixelSize(R.dimen.notification_min_height);
            int paddingTop = res.getDimensionPixelSize(R.dimen.no_notifications_padding_top);
            int textSize = res.getDimensionPixelSize(R.dimen.no_notifications_text_size);

            TextView textView = layout.findViewById(context.getResources().getIdentifier("no_notifications", "id", PACKAGE_SYSTEMUI));
            FrameLayout.LayoutParams textViewLp = (FrameLayout.LayoutParams) textView.getLayoutParams();
            textViewLp.height = height;

            int paddingLeft = textView.getPaddingLeft();
            int paddingRight = textView.getPaddingRight();
            int paddingBottom = textView.getPaddingBottom();

            textView.setLayoutParams(textViewLp);
            textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    };

    private static final XC_LayoutInflated status_bar_notification_dismiss_all = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int dismissButtonPadding = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding);
            int dismissButtonPaddingTop = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding_top);

            View buttonView = layout.getChildAt(0);
            if (buttonView instanceof LinearLayout) {
                ((LinearLayout) buttonView).getChildAt(0).setVisibility(View.GONE);
                ((LinearLayout) buttonView).getChildAt(1).setVisibility(View.VISIBLE);
                buttonView = ((LinearLayout) buttonView).getChildAt(1);
                buttonView.setId(context.getResources().getIdentifier("dismiss_text", "id", PACKAGE_SYSTEMUI));
            }
            if (buttonView instanceof ImageButton) {
                layout.removeView(buttonView);
                buttonView = new Button(context);
                buttonView.setId(context.getResources().getIdentifier("dismiss_text", "id", PACKAGE_SYSTEMUI));
                buttonView.setFocusable(true);
                buttonView.setContentDescription(context.getResources().getString(context.getResources().getIdentifier("accessibility_clear_all", "string", PACKAGE_SYSTEMUI)));
                layout.addView(buttonView);
            }
            TextView button = (TextView) buttonView; // It's a TextView on some ROMs
            if (button.getParent() instanceof LinearLayout) { // this is probably only for Xperia devices
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                lp.gravity = Gravity.END;
                button.setLayoutParams(lp);
                LinearLayout parent = (LinearLayout) button.getParent();
                parent.setBackground(null);
                ViewUtils.setMarginEnd(parent, 0);
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                lp.gravity = Gravity.END;
                button.setLayoutParams(lp);
            }
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setTextColor(res.getColor(android.R.color.white));
            button.setAllCaps(true);
            button.setText(context.getString(context.getResources().getIdentifier("clear_all_notifications_text", "string", PACKAGE_SYSTEMUI)));
            button.setBackground(res.getDrawable(R.drawable.ripple_dismiss_all));
            button.setPadding(dismissButtonPadding, dismissButtonPaddingTop, dismissButtonPadding, dismissButtonPadding);
            button.setMinWidth(res.getDimensionPixelSize(R.dimen.notification_dismiss_button_min_width));
            button.setMinHeight(res.getDimensionPixelSize(R.dimen.notification_dismiss_button_min_height));
            button.setGravity(Gravity.CENTER);
            layout.setPaddingRelative(0, 0, res.getDimensionPixelSize(R.dimen.notification_dismiss_view_padding_right), 0);
        }
    };

    private static final XC_LayoutInflated notification_template_material_base = new XC_LayoutInflated(XCallback.PRIORITY_HIGHEST) {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            layout.removeViewAt(0);
            layout.addView(NotificationHeaderView.newHeader(context), 0);

            int notificationContentMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentMarginTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);

            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", PACKAGE_ANDROID));
            LinearLayout progressContainer = new RemoteMarginLinearLayout(context);
            if (notificationMain == null) { // Some ROMs completely removed the ID
                notificationMain = (LinearLayout) layout.getChildAt(layout.getChildCount() - 1);
            }
            ViewStub progressBar = notificationMain.findViewById(res.getResources().getIdentifier("progress", "id", PACKAGE_ANDROID));
            ViewUtils.setMarginEnd(progressBar, 0);

            FrameLayout.LayoutParams notificationMainLParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.TOP);
            notificationMainLParams.setMargins(notificationContentMargin, notificationContentMarginTop, notificationContentMargin, notificationContentMargin);
            notificationMainLParams.setMarginStart(notificationContentMargin);
            notificationMainLParams.setMarginEnd(notificationContentMargin);
            notificationMain.setMinimumHeight(res.getDimensionPixelSize(R.dimen.notification_min_content_height));

            ImageView rightIcon = getRightIcon(context);

            layout.addView(rightIcon);

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params == null)
                params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.height = WRAP_CONTENT;

            // Margins for every child except actions container
            int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
            int childCount = notificationMain.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = notificationMain.getChildAt(i);
                int id = child.getId();
                if (id == R.id.actions_container) {
                    if (ConfigUtils.notifications().custom_actions_color) {
                        child.findViewById(actionsId).setBackgroundColor(ConfigUtils.notifications().actions_color);
                    }
                }
            }
            FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM);
            progressLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            progressLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_margin_end));
            progressLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_progressbar_container_margin);
            progressContainer.setId(R.id.progress_container);

            notificationMain.setLayoutParams(notificationMainLParams);
            progressContainer.setLayoutParams(progressLp);
            layout.setLayoutParams(params);

            notificationMain.removeView(progressBar);
            progressContainer.addView(progressBar);
            layout.addView(progressContainer);
        }
    };

    private static final XC_LayoutInflated status_bar_notification_row = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            FrameLayout row = (FrameLayout) liparam.view;
            Context context = row.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            if (!ConfigUtils.notifications().enable_notifications_background) {

                int dividerHeight = res.getDimensionPixelSize(R.dimen.notification_separator_size);

                FrameLayout.LayoutParams dividerLp = new FrameLayout.LayoutParams(MATCH_PARENT, dividerHeight);
                dividerLp.gravity = Gravity.TOP;

                View divider = new View(context);
                divider.setBackgroundColor(0x1F000000);
                divider.setId(R.id.notification_divider);
                divider.setLayoutParams(dividerLp);

                row.addView(divider);
            }

            if (ConfigUtils.M) {
                FrameLayout.LayoutParams fakeShadowLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);

                FakeShadowView fakeShadow = new FakeShadowView(context);
                fakeShadow.setId(R.id.fake_shadow);
                fakeShadow.setLayoutParams(fakeShadowLp);

                row.addView(fakeShadow);
            }
        }
    };

    private static final XC_LayoutInflated notification_material_action = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            Button button = (Button) liparam.view;

            Context context = button.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);
            int sidePadding = res.getDimensionPixelSize(R.dimen.notification_actions_margin_start);
            int topBottomPadding = res.getDimensionPixelSize(R.dimen.notification_action_button_padding);

            ViewGroup.MarginLayoutParams buttonLp = new FrameLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.notification_action_button_height), Gravity.CENTER);
            buttonLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_action_button_margin_start));
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setPadding(sidePadding, topBottomPadding, sidePadding, topBottomPadding);
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.notification_action_button_text_size));
            button.setLayoutParams(buttonLp);
        }
    };

    @NonNull
    private static ImageView getRightIcon(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);

        int rightIconSize = res.getDimensionPixelSize(R.dimen.notification_right_icon_size);
        int rightIconMarginTop = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_top);
        int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);

        ImageView rightIcon = new ImageView(context);

        //noinspection SuspiciousNameCombination
        FrameLayout.LayoutParams rightIconLp = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
        rightIconLp.setMargins(0, rightIconMarginTop, 0, 0);
        rightIconLp.setMarginEnd(rightIconMarginEnd);
        rightIconLp.gravity = Gravity.TOP | Gravity.END;
        rightIcon.setLayoutParams(rightIconLp);
        rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

        return rightIcon;
    }

    @NonNull
    private static ImageView getLargeRightIcon(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);

        int rightIconSize = res.getDimensionPixelSize(R.dimen.media_notification_expanded_image_max_size);
        int rightIconMarginBottom = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_bottom);
        int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);

        ImageView rightIcon = new ImageView(context);

        //noinspection SuspiciousNameCombination
        FrameLayout.LayoutParams rightIconLp = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
        rightIconLp.setMargins(0, 0, 0, rightIconMarginBottom);
        rightIconLp.setMarginEnd(rightIconMarginEnd);
        rightIconLp.gravity = Gravity.BOTTOM | Gravity.END;
        rightIcon.setLayoutParams(rightIconLp);
        rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

        return rightIcon;
    }

    private static final XC_LayoutInflated notification_template_material_big_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout oldLayout = (RelativeLayout) liparam.view;
            Context context = oldLayout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);
            boolean isNarrow = liparam.resNames.fullName.contains("notification_template_material_big_media_narrow");
            int iconSize = res.getDimensionPixelSize(R.dimen.media_notification_expanded_image_max_size);
            View mediaActions = oldLayout.findViewById(context.getResources().getIdentifier("media_actions", "id", PACKAGE_ANDROID));
            ImageView rightIcon;
            if (isNarrow) {
                rightIcon = oldLayout.findViewById(context.getResources().getIdentifier("icon", "id", PACKAGE_ANDROID));
                rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", PACKAGE_ANDROID));
            }
            else
                rightIcon = getLargeRightIcon(context);

            FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_material_base", "layout", PACKAGE_ANDROID), null);
            View header = layout.findViewById(R.id.notification_header);
            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", PACKAGE_ANDROID));
            LinearLayout contentContainer = new LinearLayout(context);
            MediaNotificationView newLayout = new MediaNotificationView(context, rightIcon, mediaActions, header, notificationMain);

            oldLayout.removeAllViews();
            layout.removeAllViews();

            newLayout.setBackgroundColor(0);
            oldLayout.addView(newLayout);

            contentContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams mediaActionsLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            mediaActionsLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_media_actions_margin_top);
            mediaActions.setPaddingRelative(res.getDimensionPixelSize(R.dimen.big_media_actions_margin_start), 0, 0, res.getDimensionPixelSize(R.dimen.media_notification_actions_padding_bottom));

            LinearLayout.LayoutParams notificationMainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationMainLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_main_plus_big_picture_margin_end));
            notificationMainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            notificationMainLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            notificationMainLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_bottom);
            notificationMain.setMinimumHeight(res.getDimensionPixelSize(R.dimen.notification_min_content_height));

            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(0, 0, Gravity.TOP | Gravity.END);
            rightIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ViewUtils.setMarginEnd(header, res.getDimensionPixelSize(R.dimen.notification_content_plus_big_picture_margin_end));

            oldLayout.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            newLayout.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            contentContainer.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            mediaActions.setLayoutParams(mediaActionsLp);
            notificationMain.setLayoutParams(notificationMainLp);
            rightIcon.setLayoutParams(iconLp);

            newLayout.addView(rightIcon);
            contentContainer.addView(notificationMain);
            contentContainer.addView(mediaActions);

            newLayout.addView(header);
            newLayout.addView(contentContainer);
        }
    };

    private static final XC_LayoutInflated notification_template_material_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            LinearLayout oldLayout = (LinearLayout) liparam.view;
            Context context = oldLayout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int mediaMargin = res.getDimensionPixelSize(R.dimen.media_notification_actions_padding_bottom);

            View mediaActions = oldLayout.findViewById(context.getResources().getIdentifier("media_actions", "id", PACKAGE_ANDROID));
            ImageView rightIcon = oldLayout.findViewById(context.getResources().getIdentifier("right_icon", "id", PACKAGE_ANDROID));
            oldLayout.removeAllViews();
            ((ViewGroup) rightIcon.getParent()).removeView(rightIcon);

            FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_material_base", "layout", PACKAGE_ANDROID), null);
            View header = layout.findViewById(R.id.notification_header);
            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            LinearLayout contentContainer = new LinearLayout(context);

            layout.setId(R.id.dummy_id);
            oldLayout.addView(layout);

            contentContainer.setOrientation(LinearLayout.VERTICAL);
            contentContainer.setMinimumHeight(res.getDimensionPixelSize(R.dimen.notification_min_content_height));
            contentContainer.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.notification_content_margin_bottom));

            while (notificationMain.getChildCount() > 0) {
                View view = notificationMain.getChildAt(0);
                notificationMain.removeViewAt(0);
                contentContainer.addView(view);
            }

            layout.removeAllViews();

            int notificationMainId = notificationMain.getId();
            notificationMain = new RemoteMarginLinearLayout(context);
            notificationMain.setId(notificationMainId);
            notificationMain.setOrientation(LinearLayout.HORIZONTAL);

            FrameLayout.LayoutParams notificationMainLp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationMainLp.setMargins(0, res.getDimensionPixelSize(R.dimen.notification_content_margin_top), 0, 0);
            notificationMainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            notificationMainLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_plus_picture_margin_end));

            LinearLayout.LayoutParams contentContainerLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            contentContainerLp.gravity = Gravity.FILL_VERTICAL;
            contentContainerLp.weight = 1;

            LinearLayout.LayoutParams mediaActionsLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            mediaActionsLp.gravity = Gravity.BOTTOM | Gravity.END;
            mediaActionsLp.setMargins(0, 0, 0, mediaMargin);
            mediaActionsLp.setMarginStart(res.getDimensionPixelSize(R.dimen.media_actions_margin_start));

            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, Gravity.TOP | Gravity.END);
            rightIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            rightIcon.setAdjustViewBounds(true);
            rightIcon.setPadding(0, 0, 0, 0);

            notificationMain.setLayoutParams(notificationMainLp);
            contentContainer.setLayoutParams(contentContainerLp);
            mediaActions.setLayoutParams(mediaActionsLp);
            rightIcon.setLayoutParams(iconLp);

            notificationMain.addView(contentContainer);
            notificationMain.addView(mediaActions);

            layout.addView(rightIcon);
            layout.addView(header);
            layout.addView(notificationMain);
        }
    };

    private static final XC_LayoutInflated notification_template_material_big_picture = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            ImageView bigPicture = layout.findViewById(res.getResources().getIdentifier("big_picture", "id", PACKAGE_ANDROID));
            LinearLayout notificationMain = layout.findViewById(res.getResources().getIdentifier("notification_main_column", "id", PACKAGE_ANDROID));
            ImageView rightIcon = layout.findViewById(res.getResources().getIdentifier("right_icon", "id", PACKAGE_ANDROID));
            NotificationHeaderView header = layout.findViewById(R.id.notification_header);
            FrameLayout actionsContainer = layout.findViewById(R.id.actions_container);
            LinearLayout progressContainer = layout.findViewById(R.id.progress_container);
            LinearLayout notificationActionListMargin = new RemoteMarginLinearLayout(context);

            ((ViewGroup) progressContainer.getParent()).removeView(progressContainer);
            ((ViewGroup) notificationMain.getParent()).removeView(notificationMain);
            ((ViewGroup) header.getParent()).removeView(header);
            ((ViewGroup) rightIcon.getParent()).removeView(rightIcon);
            ((ViewGroup) actionsContainer.getParent()).removeView(actionsContainer);
            layout.removeViewAt(layout.getChildCount() - 1);

            FrameLayout.LayoutParams notificationActionListMarginLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.TOP);
            notificationActionListMarginLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            notificationActionListMargin.setOrientation(LinearLayout.VERTICAL);
            notificationActionListMargin.setClipToPadding(false);
            notificationActionListMargin.setId(R.id.notification_action_list_margin_target);

            LinearLayout.LayoutParams pictureLp = new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1);
            pictureLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            pictureLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_margin_end));
            pictureLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_big_picture_margin_top);
            pictureLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_big_picture_margin_bottom);
            bigPicture.setAdjustViewBounds(true);
            bigPicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            mainLp.gravity = Gravity.TOP;
            mainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            mainLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_margin_end));
            notificationMain.setOrientation(LinearLayout.VERTICAL);

            notificationActionListMargin.setLayoutParams(notificationActionListMarginLp);
            bigPicture.setLayoutParams(pictureLp);
            notificationMain.setLayoutParams(mainLp);

            notificationMain.addView(progressContainer, 2);

            layout.removeAllViews();
            notificationActionListMargin.addView(notificationMain);
            notificationActionListMargin.addView(bigPicture);
            layout.addView(header);
            layout.addView(rightIcon);
            layout.addView(notificationActionListMargin);
            layout.addView(actionsContainer);
            //TODO find out why line3 has Visibility.GONE
        }
    };

    @SuppressWarnings("deprecation")
    private static final XC_LayoutInflated notification_public_default = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout layout = (RelativeLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentMarginTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            int notificationHeaderMarginTop = res.getDimensionPixelSize(R.dimen.notification_fake_header_margin_top);
            int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            int iconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_icon_margin_end);
            int appNameMarginStart = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_start);
            int appNameMarginEnd = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_end);
            int dividerMarginTop = res.getDimensionPixelSize(R.dimen.notification_divider_margin_top);
            int timeMarginStart = res.getDimensionPixelSize(R.dimen.notification_time_margin_start);

            int iconId = context.getResources().getIdentifier("icon", "id", PACKAGE_SYSTEMUI);
            int timeId = context.getResources().getIdentifier("time", "id", PACKAGE_SYSTEMUI);
            int titleId = context.getResources().getIdentifier("title", "id", PACKAGE_SYSTEMUI);

            ImageView icon = layout.findViewById(iconId);
            DateTimeView time = layout.findViewById(timeId);
            TextView title = layout.findViewById(titleId);
            TextView textView = new TextView(context);
            TextView divider = new TextView(context);

            RelativeLayout.LayoutParams iconLParams = new RelativeLayout.LayoutParams(iconSize, iconSize);
            iconLParams.setMargins(notificationContentPadding, notificationHeaderMarginTop, iconMarginEnd, 0);

            RelativeLayout.LayoutParams timeLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            timeLParams.setMargins(timeMarginStart, 0, 0, 0);
            timeLParams.addRule(RelativeLayout.RIGHT_OF, R.id.public_app_name_text);
            timeLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);

            RelativeLayout.LayoutParams titleLParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            titleLParams.setMargins(notificationContentPadding, notificationContentMarginTop, 0, 0);
            //titleLParams.addRule(RelativeLayout.BELOW, iconId);

            RelativeLayout.LayoutParams textViewLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            textViewLParams.setMarginStart(appNameMarginStart);
            textViewLParams.setMarginEnd(appNameMarginEnd);
            textViewLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);
            textViewLParams.addRule(RelativeLayout.RIGHT_OF, iconId);

            RelativeLayout.LayoutParams dividerLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            dividerLParams.setMargins(0, dividerMarginTop, 0, 0);
            dividerLParams.addRule(RelativeLayout.RIGHT_OF, R.id.public_app_name_text);
            dividerLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);

            time.setGravity(View.TEXT_ALIGNMENT_CENTER);

            textView.setId(R.id.public_app_name_text);
            textView.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));

            divider.setId(R.id.public_time_divider);
            divider.setLayoutParams(dividerLParams);
            divider.setText(res.getString(R.string.notification_header_divider_symbol));
            divider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            divider.setVisibility(View.GONE);

            icon.setLayoutParams(iconLParams);
            time.setLayoutParams(timeLParams);
            title.setLayoutParams(titleLParams);
            textView.setLayoutParams(textViewLParams);

            layout.addView(textView);
            layout.addView(divider);
        }
    };
}
