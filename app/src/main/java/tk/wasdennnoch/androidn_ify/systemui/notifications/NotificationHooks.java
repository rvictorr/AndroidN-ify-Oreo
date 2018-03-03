package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
import android.os.Handler;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.ColorInt;
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

import java.lang.reflect.Constructor;
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
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationGroupManagerHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.MediaNotificationView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationActionListLayout;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.systemui.statusbar.StatusBarHooks;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.MarginSpan;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.NotificationColorUtil;
import tk.wasdennnoch.androidn_ify.utils.ReflectionUtils;
import tk.wasdennnoch.androidn_ify.utils.RemoteLpTextView;
import tk.wasdennnoch.androidn_ify.utils.RemoteMarginLinearLayout;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static android.app.Notification.COLOR_DEFAULT;
import static android.app.Notification.EXTRA_INFO_TEXT;
import static android.app.Notification.EXTRA_LARGE_ICON;
import static android.app.Notification.EXTRA_SHOW_CHRONOMETER;
import static android.app.Notification.EXTRA_SHOW_WHEN;
import static android.app.Notification.EXTRA_SUB_TEXT;
import static android.app.Notification.EXTRA_TEMPLATE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static tk.wasdennnoch.androidn_ify.utils.NotificationColorUtil.satisfiesTextContrast;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;

@SuppressLint("StaticFieldLeak")
public class NotificationHooks {

    private static final String TAG = "NotificationHooks";

    private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String KEY_EXPAND_CLICK_LISTENER = "expandClickListener";
    public static final String EXTRA_SUBSTITUTE_APP_NAME = "nify.substName";
    public static final String EXTRA_COLORIZED = "android.colorized";
    public static final String EXTRA_ORIGINATING_USERID = "android.originatingUserId";
    private static final String EXTRA_REBUILD_CONTEXT_APPLICATION_INFO = "android.rebuild.applicationInfo";
    private static final String EXTRA_STYLE = "nify.style";
    public static final String EXTRA_CONTAINS_CUSTOM_VIEW = "android.contains.customView";

    @ColorInt
    public static final int COLOR_INVALID = 1;
    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is light.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20;

    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is dark.
     * A bit less then the above value, since it looks better on dark backgrounds.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_DARK = -10;

    private static final int REMOTE_INPUT_KEPT_ENTRY_AUTO_CANCEL_DELAY = 200;

    private static Class classBuilderRemoteViews;

    private static int mNotificationBgColor;
    private static int mNotificationBgDimmedColor;
    private static int mAccentColor = 0;
    private static final Map<String, Integer> mGeneratedColors = new HashMap<>();

//    protected static ArraySet<String> mKeysKeptForRemoteInput = new ArraySet<>();
    private static ArraySet mRemoteInputEntriesToRemoveOnCollapse = new ArraySet();

    public static Object mPhoneStatusBar;
    public static Object mHeadsUpManager;

    public static boolean remoteInputActive = false;
    private static int mMaxKeyguardNotifications;
    public static Object mStatusBarWindowManager = null;
    public static NotificationStackScrollLayoutHooks mStackScrollLayoutHooks;

    private static SensitiveNotificationFilter mSensitiveFilter = new SensitiveNotificationFilter();

    private static final XC_MethodHook inflateViewsHook = new XC_MethodHook() {

        @SuppressWarnings({"deprecation", "UnusedAssignment"})
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mHeadsUpManager = get(Fields.SystemUI.BaseStatusBar.mHeadsUpManager, param.thisObject);
            ExpandableNotificationRowHelper.setHeadsUpManager(mHeadsUpManager);
        }

        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (!(boolean) param.getResult()) return;

            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

            Object entry = param.args[0];
            Object row = get(Fields.SystemUI.NotificationDataEntry.row, entry);

            Object contentContainer = get(Fields.SystemUI.ExpandableNotificationRow.mPrivateLayout, row); // NotificationContentView
            Object contentContainerPublic = get(Fields.SystemUI.ExpandableNotificationRow.mPublicLayout, row);

            ExpandableNotificationRowHelper rowHelper = ExpandableNotificationRowHelper.getInstance(row);

            if (!ConfigUtils.notifications().change_style) return;

            Notification n = ((StatusBarNotification) get(Fields.SystemUI.NotificationDataEntry.notification, entry)).getNotification();
            final Notification.Builder recoveredBuilder
                    = NotificationHooks.recoverBuilder(context,
                    n);

            if (isMediaNotification(n)) {
                MediaNotificationProcessor processor = new MediaNotificationProcessor(context, ResourceUtils.getPackageContext(context));
                processor.processNotification(n, recoveredBuilder);
            }

            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");
            rowHelper.setRemoteInputController(mRemoteInputController);

            if (mPhoneStatusBar != null) {
                rowHelper.setOnExpandClickListener(new ExpandableNotificationRowHelper.OnExpandClickListener() {
                    @Override
                    public void onExpandClicked(Object clickedEntry, boolean nowExpanded) {
                        NotificationsStuff.setExpanded(mHeadsUpManager, clickedEntry, nowExpanded);
                        if (getInt(Fields.SystemUI.BaseStatusBar.mState, param.thisObject) == NotificationPanelHooks.STATE_KEYGUARD && nowExpanded) {
                            XposedHelpers.callMethod(mPhoneStatusBar, "goToLockedShade", get(Fields.SystemUI.NotificationDataEntry.row, clickedEntry));
                        }
                    }
                });
            }
            //}
            ((FrameLayout) row).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            rowHelper.onNotificationUpdated(entry);

            ConfigUtils.notifications().loadBlacklistedApps();
            StatusBarNotification sbn = invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, row);
            if (ConfigUtils.notifications().blacklistedApps.contains(sbn.getPackageName())) return;

            View privateView = invoke(Methods.SystemUI.NotificationContentView.getContractedChild, contentContainer);
            View publicView = invoke(Methods.SystemUI.NotificationContentView.getContractedChild, contentContainerPublic);

//            Context context = publicView.getContext();

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
            View expandedChild = invoke(Methods.SystemUI.NotificationContentView.getExpandedChild, contentContainer);
            View headsUpChild = ConfigUtils.M ? (View) invoke(Methods.SystemUI.NotificationContentView.getHeadsUpChild, contentContainer) : null;
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
                    addRemoteInput(context, expandedChild, actions);
                }
            }
        }
    };

    private static void addRemoteInput(Context context, View child, Notification.Action[] actions) {
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
                        RemoteInputHelper.handleRemoteInput(view, action.getRemoteInputs(), action.actionIntent);
                        /*if (!RemoteInputHelper.handleRemoteInput(view, action.getRemoteInputs(), action.actionIntent)) {
                            old.onClick(view);
                        }*/
                    }
                });
            }
        }
    }

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
            setTextViewColorSecondary(builder, contentView, context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
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

            int i = 0;
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
                    setTextViewColorSecondary(builder, contentView, rowIds[i]);
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
        bindNotificationHeader(contentView, builder);
    }

    private static void bindNotificationHeader(RemoteViews contentView, Object builder) {
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        bindSmallIcon(contentView, builder);
        bindHeaderAppName(contentView, builder, context, extras);
        bindHeaderText(contentView, builder, context);
        bindHeaderChronometerAndTime(contentView, builder);
        bindExpandButton(contentView, builder);
    }

    private static void bindHeaderChronometerAndTime(RemoteViews contentView, Object builder) {
        long mWhen = XposedHelpers.getLongField(builder, "mWhen");
        if ((boolean) XposedHelpers.callMethod(builder, "showsTimeOrChronometer")) {
            contentView.setViewVisibility(R.id.time_divider, View.VISIBLE);
            setTextViewColorSecondary(builder, contentView, R.id.time_divider);
            if (XposedHelpers.getBooleanField(builder, "mUseChronometer")) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        mWhen + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
                setTextViewColorSecondary(builder, contentView, R.id.chronometer);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", mWhen);
                setTextViewColorSecondary(builder, contentView, R.id.time);
            }
        } else {
            contentView.setLong(R.id.time, "setTime", mWhen);
        }
    }

    private static void bindHeaderText(RemoteViews contentView, Object builder, Context context) {
        Notification mN = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        Notification.Style mStyle = (Notification.Style) XposedHelpers.getObjectField(builder, "mStyle");

        CharSequence headerText = mN.extras.getCharSequence(EXTRA_SUB_TEXT);
        if (headerText == null && mStyle != null && XposedHelpers.getBooleanField(mStyle, "mSummaryTextSet")
                && !mStyle.getClass().equals(Notification.BigPictureStyle.class)) {
            headerText = (CharSequence) XposedHelpers.getObjectField(mStyle, "mSummaryText");
        }
        if (headerText == null
                && context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.N
                && mN.extras.getCharSequence(EXTRA_INFO_TEXT) != null) {
            headerText = mN.extras.getCharSequence(EXTRA_INFO_TEXT);
        }
        if (headerText != null) {
            // TODO: Remove the span entirely to only have the string with proper formatting.
            contentView.setTextViewText(R.id.header_text, processLegacyText(builder, headerText));
            setTextViewColorSecondary(builder, contentView, R.id.header_text);
            contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
            setTextViewColorSecondary(builder, contentView, R.id.header_text_divider);
        }

        XposedHelpers.callMethod(builder, "unshrinkLine3Text", contentView);
    }

    private static void bindSmallIcon(RemoteViews contentView, Object builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Icon mSmallIcon = (Icon) XposedHelpers.getObjectField(builder, "mSmallIcon");
            contentView.setImageViewIcon(R.id.icon, mSmallIcon);
        } else {
            int mSmallIcon = XposedHelpers.getIntField(builder, "mSmallIcon");
            contentView.setImageViewResource(R.id.icon, mSmallIcon);
        }

        processSmallIconColor(contentView, builder);
    }

    private static void bindHeaderAppName(RemoteViews contentView, Object builder, Context context, Bundle extras) {
        contentView.setTextViewText(R.id.app_name_text, loadHeaderAppName(context, extras));
        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        if (isColorized(n)) {
            setTextViewColorPrimary(builder, contentView, R.id.app_name_text);
        } else {
            contentView.setTextColor(R.id.app_name_text, resolveColor(builder));
        }
    }

    private static void processSmallIconColor(RemoteViews contentView, Object builder) {
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
            color = getPrimaryHighlightColor(builder);
            XposedHelpers.callMethod(contentView, "setDrawableParameters", R.id.icon, false, -1, color,
                    PorterDuff.Mode.SRC_ATOP, -1);
        }

        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static void bindExpandButton(RemoteViews contentView, Object builder) {
        int color = getPrimaryHighlightColor(builder);
        XposedHelpers.callMethod(contentView, "setDrawableParameters", R.id.expand_button, false, -1, color,
                PorterDuff.Mode.SRC_ATOP, -1);
        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static int getPrimaryHighlightColor(Object builder) {
        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        return isColorized(n) ? getPrimaryTextColor(builder) : resolveColor(builder);
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
    public static boolean isMediaNotification(Notification n) {
        Class<? extends Notification.Style> style = getNotificationStyle(n);
        if (Notification.MediaStyle.class.equals(style)) {
            return true;
        }
        return false;
//        return true;
    }

    public static boolean isColorizedMedia(Notification n) {
        //if (Notification.MediaStyle.class.equals(getNotificationStyle(n))) {
            /*Boolean colorized = (Boolean) n.extras.get(EXTRA_COLORIZED);
            if ((colorized == null || colorized) && hasMediaSession(n)) {*/
                //return true;
            //}
        //}
        //return false;
        return isMediaNotification(n);
    }

    /**
     * @return true if this notification is colorized.
     *
     */
    public static boolean isColorized(Notification n) {
        return isColorizedMedia(n);
        //return true;
        /*return extras.getBoolean(EXTRA_COLORIZED)
                && (hasColorizedPermission() || isForegroundService());*/
    }

    public static Class<? extends Notification.Style> getNotificationStyle(Notification n) {
        String templateClass = n.extras.getString(EXTRA_TEMPLATE);
        if (!TextUtils.isEmpty(templateClass)) {
            return getNotificationStyleClass(templateClass);
        }
        return null;
    }

    private static Class<? extends Notification.Style> getNotificationStyleClass(String templateClass) {
        Class<? extends Notification.Style>[] classes = new Class[]{
                Notification.BigTextStyle.class, Notification.BigPictureStyle.class, Notification.InboxStyle.class, Notification.MediaStyle.class};
        for (Class<? extends Notification.Style> innerClass : classes) {
            if (templateClass.equals(innerClass.getName())) {
                return innerClass;
            }
        }
        return null;
    }

    public static boolean hasMediaSession(Notification n) {
        return n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) != null;
    }

    public static void setColorPalette(Notification.Builder builder, int backgroundColor, int foregroundColor) {
        XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColor", backgroundColor);
        XposedHelpers.setAdditionalInstanceField(builder, "mForegroundColor", foregroundColor);
        XposedHelpers.setAdditionalInstanceField(builder, "mTextColorsAreForBackground", COLOR_INVALID);
        ensureColors(builder);
    }

    private static int getBackgroundColor(Object builder) {
        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        Object mBackgroundColor = XposedHelpers.getAdditionalInstanceField(builder, "mBackgroundColor");
        Object mBackgroundColorHint = XposedHelpers.getAdditionalInstanceField(builder, "mBackgroundColorHint");
        if (isColorized(n)) {
            return ((int) mBackgroundColor != COLOR_INVALID)
                    ? (int) mBackgroundColor
                    : XposedHelpers.getIntField(builder, "mColor");
        } else {
            return ((int) mBackgroundColorHint != COLOR_INVALID)
                    ? (int) mBackgroundColorHint
                    : COLOR_DEFAULT;
        }
    }

    public static int getPrimaryTextColor(Object builder) {
        ensureColors(builder);
        int mPrimaryTextColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mPrimaryTextColor");
        return mPrimaryTextColor;
    }

    public static int getSecondaryTextColor(Object builder) {
        ensureColors(builder);
        int mSecondaryTextColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mSecondaryTextColor");
        return mSecondaryTextColor;
    }

    private static int getActionBarColor(Object builder) {
        ensureColors(builder);
        int mActionBarColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mActionBarColor");
        return mActionBarColor;
    }

    private static void setTextViewColorPrimary(Object builder, RemoteViews contentView, int id) {
        ensureColors(builder);
        int color = (int) XposedHelpers.getAdditionalInstanceField(builder, "mPrimaryTextColor");
        contentView.setTextColor(id, color);
    }

    private static void setTextViewColorSecondary(Object builder, RemoteViews contentView, int id) {
        ensureColors(builder);
        int color = (int) XposedHelpers.getAdditionalInstanceField(builder, "mSecondaryTextColor");
        contentView.setTextColor(id, color);
    }

    private static void ensureColors(Object builder) {
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        int mForegroundColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mForegroundColor");
        int backgroundColor = getBackgroundColor(builder);
        int mPrimaryTextColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mPrimaryTextColor");
        int mSecondaryTextColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mSecondaryTextColor");
        int mActionBarColor = (int) XposedHelpers.getAdditionalInstanceField(builder, "mActionBarColor");
        int mTextColorsAreForBackground = (int) XposedHelpers.getAdditionalInstanceField(builder, "mTextColorsAreForBackground");
        int mBackgroundColorHint = (int) XposedHelpers.getAdditionalInstanceField(builder, "mBackgroundColorHint");

        NotificationColorUtil.setContext(context);
        if (mPrimaryTextColor == COLOR_INVALID
                || mSecondaryTextColor == COLOR_INVALID
                || mActionBarColor == COLOR_INVALID
                || mTextColorsAreForBackground != backgroundColor) {
            mTextColorsAreForBackground = backgroundColor;
            if (mForegroundColor == COLOR_INVALID || !isColorized(n)) {
                mPrimaryTextColor = NotificationColorUtil.resolvePrimaryColor(backgroundColor);
                mSecondaryTextColor = NotificationColorUtil.resolveSecondaryColor(backgroundColor);
                if (backgroundColor != COLOR_DEFAULT
                        && (mBackgroundColorHint != COLOR_INVALID || isColorized(n))) {
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
                                true  /*findFG*/ ,
                                4.5f);
                        mPrimaryTextColor = NotificationColorUtil.changeColorLightness(
                                mSecondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT);
                    } else {
                        mSecondaryTextColor =
                                NotificationColorUtil.findContrastColorAgainstDark(
                                        mForegroundColor,
                                        backgroundColor,
                                        true /*findFG*/,
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
                                    true /*findFG*/,
                                    4.5f);
                        } else {
                            mSecondaryTextColor
                                    = NotificationColorUtil.findContrastColorAgainstDark(
                                    mSecondaryTextColor,
                                    backgroundColor,
                                    true /*findFG*/,
                                    4.5f);
                        }
                        mPrimaryTextColor = NotificationColorUtil.changeColorLightness(
                                mSecondaryTextColor, backgroundLight
                                        ? -LIGHTNESS_TEXT_DIFFERENCE_LIGHT
                                        : -LIGHTNESS_TEXT_DIFFERENCE_DARK);
                    }
                }
            }
            mActionBarColor = NotificationColorUtil.resolveActionBarColor(backgroundColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mForegroundColor", mForegroundColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColor", backgroundColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mPrimaryTextColor", mPrimaryTextColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mSecondaryTextColor", mSecondaryTextColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mActionBarColor", mActionBarColor);
            XposedHelpers.setAdditionalInstanceField(builder, "mTextColorsAreForBackground", mTextColorsAreForBackground);
            XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColorHint", mBackgroundColorHint);
        }
    }

    private static void initColors(Object builder) {
        XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColor", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mForegroundColor", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mPrimaryTextColor", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mSecondaryTextColor", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mActionBarColor", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mTextColorsAreForBackground", COLOR_INVALID);
        XposedHelpers.setAdditionalInstanceField(builder, "mBackgroundColorHint", COLOR_INVALID);
    }

    private static void updateBackgroundColor(Object builder, RemoteViews contentView) {
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(builder, "mN");
        if (isColorized(n)) {
//////            if (isMediaNotification(n)) {
//////                contentView.setInt(context.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID), "setBackgroundResource",
//////                        0);
//                contentView.setInt(R.id.status_bar_latest_event_content, "setBackgroundColor",
//                        getBackgroundColor(builder));
//////            }
                contentView.setInt(context.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID), "setBackgroundColor",
                        getBackgroundColor(builder));
        } else {
            //Clear it!
//            contentView.setInt(R.id.status_bar_latest_event_content, "setBackgroundResource",
//                    0); //FIXME setting a background screws it up
            contentView.setInt(context.getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID), "setBackgroundResource",
                    0);
//        contentView.setInt(R.id.status_bar_latest_event_content, "setBackgroundColor",
//                0x99ff0000); //FIXME setting a background screws it up
        }
    }

    /**
     * Construct a RemoteViews for the final notification header only
     */
    public static RemoteViews makeNotificationHeader(Notification.Builder builder) {
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Constructor constructor = XposedHelpers.findConstructorBestMatch(classBuilderRemoteViews, ApplicationInfo.class, int.class);
        RemoteViews header = newInstance(constructor, ResourceUtils.createOwnContext(context).getApplicationInfo(), R.layout.notification_template_header);
        resetNotificationHeader(header);
        bindNotificationHeader(header, builder);
        return header;
    }

    public static boolean showsTime(Notification notification) {
        return notification.when != 0 && notification.extras.getBoolean(EXTRA_SHOW_WHEN);
    }

    private static boolean showsTimeOrChronometer(Notification notification) {
        return showsTime(notification) || showsChronometer(notification);
    }

    public static boolean showsChronometer(Notification notification) {
        return notification.when != 0 && notification.extras.getBoolean(EXTRA_SHOW_CHRONOMETER);
    }

    /**
     * Resets the notification header to its original state
     */
    private static void resetNotificationHeader(RemoteViews contentView) {
        // Small icon doesn't need to be reset, as it's always set. Resetting would prevent
        // re-using the drawable when the notification is updated.
        contentView.setBoolean(R.id.notification_header, "setExpanded", false);
        contentView.setTextViewText(R.id.app_name_text, null);
        contentView.setViewVisibility(R.id.chronometer, View.GONE);
        contentView.setViewVisibility(R.id.header_text, View.GONE);
        contentView.setTextViewText(R.id.header_text, null);
        contentView.setViewVisibility(R.id.header_text_divider, View.GONE);
        contentView.setViewVisibility(R.id.time_divider, View.GONE);
        contentView.setViewVisibility(R.id.time, View.GONE);
        contentView.setImageViewIcon(R.id.profile_badge, null);
        contentView.setViewVisibility(R.id.profile_badge, View.GONE);
    }

    private static void hideLine1Text(RemoteViews result) {
        if (result != null) {
            result.setViewVisibility(R.id.text_line_1, View.GONE);
        }
    }

    private static void makeHeaderExpanded(RemoteViews result) {
        if (result != null) {
            result.setBoolean(R.id.notification_header, "setExpanded", true);
        }
    }

    private static boolean handleProgressBar(boolean hasProgress, RemoteViews contentView, Object builder, Resources res) {
        final int max = XposedHelpers.getIntField(builder, "mProgressMax");
        final boolean ind = XposedHelpers.getBooleanField(builder, "mProgressIndeterminate");
        if (hasProgress && (max != 0 || ind)) {
            contentView.setViewVisibility(R.id.progress_container, View.VISIBLE);
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
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            Object builder = param.thisObject;
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            Resources res = context.getResources();
            RemoteViews contentView = (RemoteViews) param.getResult();
            CharSequence mContentText = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");

            updateBackgroundColor(builder, contentView);
            bindNotificationHeader(contentView, param);

            boolean showProgress = handleProgressBar((boolean) param.args[1], contentView, builder, res);

            if (XposedHelpers.getObjectField(builder, "mContentTitle") != null) {
                int id = res.getIdentifier("title", "id", PACKAGE_ANDROID);
                contentView.setInt(id, "setWidth", showProgress
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : ViewGroup.LayoutParams.MATCH_PARENT);
                setTextViewColorPrimary(builder, contentView, id);
            }

            if (mContentText != null) {
                int textId = showProgress ? R.id.text_line_1
                        : res.getIdentifier("text", "id", PACKAGE_ANDROID);
                contentView.setTextViewText(textId, mContentText);
                setTextViewColorSecondary(builder, contentView, textId);
                contentView.setViewVisibility(textId, View.VISIBLE);
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

            contentView.setViewVisibility(context.getResources().getIdentifier("text2", "id", PACKAGE_ANDROID), View.GONE);
            //contentView.setViewVisibility(res.getIdentifier("line3", "id", PACKAGE_ANDROID), View.VISIBLE);
            contentView.setInt(res.getIdentifier("right_icon", "id", PACKAGE_ANDROID), "setBackgroundResource", 0);
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
                    getPrimaryHighlightColor(XposedHelpers.getObjectField(param.thisObject, "mBuilder")),
                    PorterDuff.Mode.SRC_ATOP, -1);
        }
    };

    private static final XC_MethodHook resetStandardTemplateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.args[0];
            contentView.setImageViewResource(R.id.icon, 0);
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

            int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
            ArrayList<Notification.Action> mActions = (ArrayList<Notification.Action>) XposedHelpers.getObjectField(param.thisObject, "mActions");
            int N = mActions.size();
            if (N > 0) {
                big.setInt(R.id.notification_action_list_margin_target, "setMarginBottom", R.dimen.notification_action_list_height);
                if (isColorized((Notification) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mN"))) {
                    big.setInt(actionsId, "setBackgroundColor", getActionBarColor(param.thisObject));
                } else {
                    big.setInt(actionsId, "setBackgroundColor", ResourceUtils.getInstance(context).getColor(R.color.notification_action_list));
                    big.setViewVisibility(R.id.actions_container, View.VISIBLE);
                }
            } else {
                big.setViewVisibility(R.id.actions_container, View.GONE);
            }
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
                //button.setTextColor(textViewId, mColor);
                if (isColorized((Notification) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mN"))) {
                    setTextViewColorPrimary(param.thisObject, button, textViewId);
                } else {
                    button.setTextColor(textViewId, mColor);
                }
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
            if (mAccentColor == 0) {
                //noinspection deprecation
                mAccentColor = context.getResources().getColor(context.getResources().getIdentifier("notification_icon_bg_color", "color", PACKAGE_ANDROID));
            }
            int c;
            if (mColor == COLOR_DEFAULT) {
                ensureColors(builder);
                c = NotificationColorUtil.resolveContrastColor(mAccentColor);
                //c = (int) XposedHelpers.getAdditionalInstanceField(builder, "mSecondaryTextColor");

            } else {
                 c = NotificationColorUtil.resolveContrastColor(mColor);
            }
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
            Context context = (Context) getObjectField(param.thisObject, "mContext");
            String name = context.getPackageName();
            if (!RemoteInputHelper.DIRECT_REPLY_ENABLED || ConfigUtils.notifications().blacklistedApps.contains(name)) {
                return;
            }

            Notification.Builder b = (Notification.Builder) param.thisObject;
            XposedHelpers.setBooleanField(b, "mHasThreeLines", true);
            @SuppressWarnings("unchecked") List<Notification.Action> actions = (List<Notification.Action>) getObjectField(b, "mActions");
            if (!actions.isEmpty() && haveRemoteInput(actions.toArray(new Notification.Action[actions.size()]))) {
                return;
            }
            final List<Notification.Action> wearableRemoteInputActions = new ArrayList<>();

            final String EXTRA_WEARABLE_EXTENSIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "EXTRA_WEARABLE_EXTENSIONS");
            final String KEY_ACTIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "KEY_ACTIONS");
            Bundle wearableBundle = b.getExtras().getBundle(EXTRA_WEARABLE_EXTENSIONS);
            if (wearableBundle != null) {
                ArrayList<Notification.Action> wearableActions = wearableBundle.getParcelableArrayList(KEY_ACTIONS);
                if (wearableActions != null) {
                    for (int i = 0; i < wearableActions.size(); i++) {
                        if (hasValidRemoteInput(wearableActions.get(i))) {
                            wearableRemoteInputActions.add(wearableActions.get(i));
                        }
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

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Notification n = (Notification) param.getResult();
            Notification.Builder builder = (Notification.Builder) param.thisObject;
            XposedHelpers.setAdditionalInstanceField(n, "mBuilder", builder);
        }
    };

    private static final XC_MethodHook makeBigContentViewBigPictureHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.getResult();
            Object builder = XposedHelpers.getObjectField(param.thisObject, "mBuilder");
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            int textId = context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID);

            if (XposedHelpers.getBooleanField(param.thisObject, "mSummaryTextSet")) {
                contentView.setTextViewText(textId, processLegacyText(builder, (CharSequence)XposedHelpers.getObjectField(param.thisObject, "mSummaryText")));
                setTextViewColorSecondary(builder, contentView, textId);
                contentView.setViewVisibility(textId, View.VISIBLE);
                contentView.setViewVisibility(context.getResources().getIdentifier("line3", "id", PACKAGE_ANDROID) , View.VISIBLE);
            }
        }
    };

    private static final XC_MethodHook setBuilderBigContentViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews result = (RemoteViews) param.args[1];
            makeHeaderExpanded(result);
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

                final Class classNotificationBuilder = Notification.Builder.class;
                classBuilderRemoteViews = XposedHelpers.findClass(Notification.class.getName() + "$BuilderRemoteViews", classLoader);
                Class classNotificationStyle = Notification.Style.class;
                Class classNotificationMediaStyle = Notification.MediaStyle.class;
                Class classNotificationBigTextStyle = Notification.BigTextStyle.class;
                Class classNotificationBigPictureStyle = Notification.BigPictureStyle.class;
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
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setBuilderBigContentView", Notification.class, RemoteViews.class, setBuilderBigContentViewHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setLargeIcon", Icon.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Notification n = (Notification) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mN");
                        Icon icon = (Icon) param.args[0];
                        n.extras.putParcelable(EXTRA_LARGE_ICON, icon);
                    }
                });

                XposedHelpers.findAndHookMethod(NotificationCompat.Builder.class, "build", buildHook);
                XposedHelpers.findAndHookMethod(classNotificationBigTextStyle, "makeBigContentView", makeBigContentViewBigTextHook);
                XposedHelpers.findAndHookMethod(classNotificationInboxStyle, "makeBigContentView", makeBigContentViewInbox);
                XposedHelpers.findAndHookMethod(classNotificationBigPictureStyle, "makeBigContentView", makeBigContentViewBigPictureHook);
                XC_MethodHook setBuilderContentViewHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object builder = param.thisObject;
                        XposedHelpers.setAdditionalInstanceField(builder, "mN", param.args[0]);
                        initColors(builder);
                    }
                };
//                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setBuilderBigContentView", Notification.class, RemoteViews.class, setBuilderContentViewHook);
//                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setBuilderContentView", Notification.class, RemoteViews.class, setBuilderContentViewHook);

                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "hideRightIcon", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "styleText", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "generateMediaActionButton", Notification.Action.class, generateMediaActionButtonHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "makeMediaContentView", makeMediaContentViewHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "makeMediaBigContentView", makeMediaContentViewHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "getBigLayoutResource", int.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Notification.Builder builder = (Notification.Builder) XposedHelpers.getObjectField(param.thisObject, "mBuilder");
                        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
                        return context.getResources().getIdentifier("notification_template_material_big_media", "layout", PACKAGE_ANDROID);
                    }
                });

                XposedHelpers.findAndHookConstructor(classNotificationBuilder, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) param.args[0];
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mN", new Notification());
                        initColors(param.thisObject);
                    }
                });
                XposedHelpers.findAndHookConstructor(classNotificationBuilder, Context.class, Notification.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) param.args[0];
                        Notification.Builder builder = (Notification.Builder) param.thisObject;
                        Notification n = (Notification) param.args[1];
                        initColors(param.thisObject);
//                        if (isMediaNotification(n)) {
//                            MediaNotificationProcessor processor = new MediaNotificationProcessor(context, ResourceUtils.getPackageContext(context));
//                            processor.processNotification(n, builder);
//                        }
                        XposedHelpers.setAdditionalInstanceField(builder, "mN", n);
                    }
                });
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setBuilderContentView", Notification.class, RemoteViews.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object builder = param.thisObject;
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Notification n = (Notification) param.args[0];
//                        if (isMediaNotification(n)) {
//                            MediaNotificationProcessor processor = new MediaNotificationProcessor(context, ResourceUtils.getPackageContext(context));
//                            processor.processNotification(n, (Notification.Builder) builder);
//                        }
                    }
                }); //TODO finish
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "setStyle", Notification.Style.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Notification.Style style = (Notification.Style) param.args[0];
                        Notification notification = (Notification) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mN");
                        Notification.Style mStyle = (Notification.Style) XposedHelpers.getObjectField(param.thisObject, "mStyle");
                        if (mStyle != style) {
                            if (style != null) {
                                notification.extras.putString(EXTRA_TEMPLATE, style.getClass().getName());
                                XposedHelpers.setAdditionalInstanceField(notification, EXTRA_STYLE, style);
                            } else {
                                notification.extras.remove(EXTRA_TEMPLATE);
                            }
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking app", t);
        }
    }

    private static boolean removeNotification(Object headsUpManager, String key, boolean ignoreEarliestRemovalTime) {
        if ((boolean) XposedHelpers.callMethod(headsUpManager, "wasShownLongEnough", key) || ignoreEarliestRemovalTime) {
            invoke(Methods.SystemUI.HeadsUpManager.releaseImmediately, headsUpManager, key);
            return true;
        } else {
            XposedHelpers.callMethod(XposedHelpers.callMethod(headsUpManager, "getHeadsUpEntry", key), "removeAsSoonAsPossible");
            return false;
        }
    }

    private static void removeRemoteInputEntriesKeptUntilCollapsed() {
        RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(mPhoneStatusBar, "mRemoteInputController");
        for (int i = 0; i < mRemoteInputEntriesToRemoveOnCollapse.size(); i++) {
            Object entry = mRemoteInputEntriesToRemoveOnCollapse.valueAt(i);
            mRemoteInputController.removeRemoteInput(entry, null);
            invoke(Methods.SystemUI.BaseStatusBar.removeNotification, mPhoneStatusBar,
                    get(Fields.SystemUI.NotificationDataEntry.key, entry),
                    XposedHelpers.getObjectField(mPhoneStatusBar, "mLatestRankingMap"));
        }
        mRemoteInputEntriesToRemoveOnCollapse.clear();
    }

    public static void hookSystemUI() {
        try {
            final ConfigUtils config = ConfigUtils.getInstance();

            mSensitiveFilter.hook();

            final Class classNotificationClicker = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar.NotificationClicker", Classes.SystemUI.getClassLoader());
            XposedHelpers.findAndHookMethod(Classes.SystemUI.BaseStatusBar, "inflateViews", Classes.SystemUI.NotificationDataEntry, ViewGroup.class, inflateViewsHook);
            XposedHelpers.findAndHookMethod(Classes.SystemUI.BaseStatusBar, "updateNotificationViews", Classes.SystemUI.NotificationDataEntry, StatusBarNotification.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object entry = param.args[0];
                    Object row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
                    ExpandableNotificationRowHelper.getInstance(row).onNotificationUpdated(entry);
                }
            });


            if (config.notifications.change_style) {

                StatusBarHooks.create();

                if (ConfigUtils.M) // For now
                    mStackScrollLayoutHooks = new NotificationStackScrollLayoutHooks();

                boolean foundClass = false;
                Class classBroadcastReceiver = null;
                Class classNotificationListener = null;
                for(int i = 1; !foundClass; i++) {
                    try {
                        classBroadcastReceiver = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar$" + i, Classes.SystemUI.getClassLoader());
                    } catch (XposedHelpers.ClassNotFoundError ignore) {} //wtf Sony?? (some numbers missing)
                    if (classBroadcastReceiver != null && BroadcastReceiver.class.isAssignableFrom(classBroadcastReceiver))
                        foundClass = true;
                }
                foundClass = false;
                for(int i = 1; !foundClass; i++) {
                    try {
                        classNotificationListener = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar$" + i, Classes.SystemUI.getClassLoader());
                    } catch (XposedHelpers.ClassNotFoundError ignore) {} //wtf Sony?? (some numbers missing)
                    if (classNotificationListener != null && NotificationListenerService.class.isAssignableFrom(classNotificationListener))
                        foundClass = true;
                }

                final Class<?> classMediaExpandableNotificationRow = getClassMediaExpandableNotificationRow(Classes.SystemUI.getClassLoader());
                Class classNotificationGuts = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationGuts", Classes.SystemUI.getClassLoader());

                if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                    
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

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "getMaxKeyguardNotifications", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            boolean recompute = false;
//                            Object shouldRecompute = XposedHelpers.getAdditionalInstanceField(param.thisObject, "recompute");
//                            if (shouldRecompute != null && (boolean) shouldRecompute) {
//                                recompute = (boolean) shouldRecompute;
//                                XposedHelpers.setAdditionalInstanceField(param.thisObject, "recompute", false);
//                            }
                            return getMaxKeyguardNotifications(param.thisObject, recompute /* recompute */);
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "startKeyguard", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");
                            final Object mStatusBarKeyguardViewManager = XposedHelpers.getObjectField(param.thisObject, "mStatusBarKeyguardViewManager");
                            final Object mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler");

                            mRemoteInputController.addCallback(new RemoteInputController.Callback() {
                                @Override
                                public void onRemoteInputActive(boolean active) {
                                    XposedHelpers.callMethod(mStatusBarKeyguardViewManager, "updateStates");
                                }

                                @Override
                                public void onRemoteInputSent(Object entry) {
                                    final String key = get(Fields.SystemUI.NotificationDataEntry.key, entry);
                                    if (mRemoteInputEntriesToRemoveOnCollapse.contains(entry)) {
                                        // We're currently holding onto this notification, but from the apps point of
                                        // view it is already canceled, so we'll need to cancel it on the apps behalf
                                        // after sending - unless the app posts an update in the mean time, so wait a
                                        // bit.
                                        XposedHelpers.callMethod(mHandler, "postDelayed", new Runnable() {
                                            @Override
                                            public void run() {
                                                invoke(Methods.SystemUI.BaseStatusBar.removeNotification, param.thisObject, key, null);
                                            }
                                        }, REMOTE_INPUT_KEPT_ENTRY_AUTO_CANCEL_DELAY);
                                    }
                                }
                            });
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "onHeadsUpPinnedModeChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean inPinnedMode = (boolean) param.args[0];
                            Object mNotificationPanel = get(Fields.SystemUI.PhoneStatusBar.mNotificationPanel, param.thisObject);
                            ViewGroup mStackScroller = get(Fields.SystemUI.BaseStatusBar.mStackScroller, param.thisObject);

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

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "setBarState", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int state = (int) param.args[0];
                            if (state == NotificationPanelHooks.STATE_KEYGUARD) {
                                removeRemoteInputEntriesKeptUntilCollapsed();
                                invoke(Methods.SystemUI.PhoneStatusBar.maybeEscalateHeadsUp, param.thisObject);
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "setPanelExpanded", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isExpanded = (boolean) param.args[0];
                            if (!isExpanded) {
                                removeRemoteInputEntriesKeptUntilCollapsed();
                            }
                        }
                    });

                    XposedBridge.hookAllMethods(Classes.SystemUI.PhoneStatusBar, "addNotification", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object mNotificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, param.thisObject);
                            invoke(Methods.SystemUI.NotificationData.updateRanking, mNotificationData, param.args[1]);
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "removeNotification", String.class, NotificationListenerService.RankingMap.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            //TODO: this realllllyyyyy needs to pe optimized properly (fields etc.)
                            Object mNotificationPanel = get(Fields.SystemUI.PhoneStatusBar.mNotificationPanel, param.thisObject);
                            Object mNotificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, param.thisObject);
                            Object row = null;
                            HashSet mHeadsUpEntriesToRemoveOnSwitch = (HashSet) XposedHelpers.getObjectField(param.thisObject, "mHeadsUpEntriesToRemoveOnSwitch");
                            RemoteInputController mRemoteInputController = (RemoteInputController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mRemoteInputController");
                            String key = (String) param.args[0];
                            NotificationListenerService.RankingMap ranking = (NotificationListenerService.RankingMap) param.args[1];
                            boolean deferRemoval = false;
                            if (invoke(Methods.SystemUI.HeadsUpManager.isHeadsUp, mHeadsUpManager, key)) {
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
                            Object entry = invoke(Methods.SystemUI.NotificationData.get, mNotificationData, key);
                            if (entry != null) {
                                row = get(Fields.SystemUI.NotificationDataEntry.row, entry);
                            }
                            if (entry != null && mRemoteInputController.isRemoteInputActive(entry)
                                    && (row != null && !ExpandableNotificationRowHelper.getInstance(row).isDismissed())) {
                                XposedHelpers.setObjectField(param.thisObject, "mLatestRankingMap",  ranking);
                                mRemoteInputEntriesToRemoveOnCollapse.add(entry);
                                return null;
                            }

                            if (entry != null && row != null) {
                                ExpandableNotificationRowHelper.getInstance(row).setRemoved();
                            }
                            // Let's remove the children if this was a summary
                            NotificationsStuff.handleGroupSummaryRemoved(param.thisObject, key, ranking);
                            StatusBarNotification old = (StatusBarNotification) XposedHelpers.callMethod(param.thisObject, "removeNotificationViews", key, ranking);

                            if (old != null) {
                                if (!(boolean) XposedHelpers.callMethod(param.thisObject, "hasActiveNotifications")
                                        && !(boolean) XposedHelpers.callMethod(mNotificationPanel, "isTracking")
                                        && !(boolean) XposedHelpers.callMethod(mNotificationPanel, "isQsExpanded")) {
                                    int state = getInt(Fields.SystemUI.BaseStatusBar.mState, param.thisObject);
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

                    XposedHelpers.findAndHookMethod(classNotificationListener, "onNotificationPosted", StatusBarNotification.class, NotificationListenerService.RankingMap.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            final Object baseStatusBar = XposedHelpers.getSurroundingThis(param.thisObject);
                            final StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                            final NotificationListenerService.RankingMap rankingMap = (NotificationListenerService.RankingMap) param.args[1];
                            Handler handler = get(Fields.SystemUI.BaseStatusBar.mHandler, baseStatusBar);
                            final Object notificationData = get(Fields.SystemUI.BaseStatusBar.mNotificationData, baseStatusBar);
                            final Object groupManager = get(Fields.SystemUI.BaseStatusBar.mGroupManager, baseStatusBar);
                            if (sbn != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        processForRemoteInput(baseStatusBar, sbn.getNotification());
                                        String key = sbn.getKey();
                                        boolean isUpdate = invoke(Methods.SystemUI.NotificationData.get, notificationData, key) != null;

                                        // In case we don't allow child notifications, we ignore children of
                                        // notifications that have a summary, since we're not going to show them
                                        // anyway. This is true also when the summary is canceled,
                                        // because children are automatically canceled by NoMan in that case.
                                        if (!NotificationsStuff.ENABLE_CHILD_NOTIFICATIONS
                                                && (boolean) invoke(Methods.SystemUI.NotificationGroupManager.isChildInGroupWithSummary, groupManager, sbn)) {

                                            // Remove existing notification to avoid stale data.
                                            if (isUpdate) {
                                                invoke(Methods.SystemUI.BaseStatusBar.removeNotification, baseStatusBar, key, rankingMap);
                                            } else {
                                                invoke(Methods.SystemUI.NotificationData.updateRanking, notificationData, rankingMap);
                                            }
                                            return;
                                        }
                                        if (isUpdate) {
                                            invoke(Methods.SystemUI.BaseStatusBar.updateNotification, baseStatusBar, sbn, rankingMap);
                                        } else {
                                            invoke(Methods.SystemUI.BaseStatusBar.addNotification, baseStatusBar, sbn, rankingMap, null /* oldEntry */);
                                        }
                                    }
                                });
                            }
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "addStatusBarWindow", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mStatusBarWindowManager = XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindowManager");
                            Object headsUpManager = get(Fields.SystemUI.BaseStatusBar.mHeadsUpManager, param.thisObject);
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mRemoteInputController", new RemoteInputController(new RemoteInputController.Callback() {
                                @Override
                                public void onRemoteInputActive(boolean active) {
                                    RemoteInputHelper.setWindowManagerFocus(active);
                                }

                                @Override
                                public void onRemoteInputSent(Object entry) {

                                }
                            }, headsUpManager));
                        }
                    });

                    XposedHelpers.findAndHookMethod(Classes.SystemUI.StatusBarWindowManager, "applyFocusableFlag", Classes.SystemUI.StatusBarWindowManagerState, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)
                                    XposedHelpers.getObjectField(param.thisObject, ConfigUtils.L1 ? "mLpChanged" : "mLp");
                            if (remoteInputActive) {
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
                                param.setResult(null);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)
                                    XposedHelpers.getObjectField(param.thisObject, ConfigUtils.L1 ? "mLpChanged" : "mLp");

                            windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                        }
                    });
                }
                XposedHelpers.findAndHookMethod(Classes.SystemUI.StatusBarWindowManager, "applyFitsSystemWindows", Classes.SystemUI.StatusBarWindowManagerState, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        View mStatusBarView = (View) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");
                        Object state = param.args[0];
                        boolean fitsSystemWindows = !(boolean) invoke(Methods.SystemUI.StatusBarWindowManagerState.isKeyguardShowingAndNotOccluded, state);
                        if (mStatusBarView.getFitsSystemWindows() != fitsSystemWindows) {
                            mStatusBarView.setFitsSystemWindows(fitsSystemWindows);
                            mStatusBarView.requestApplyInsets();
                        }
                        return null;
                    }
                });
                XposedHelpers.findAndHookMethod(Classes.SystemUI.StackScrollAlgorithm, "initConstants", Context.class, initConstantsHook);
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

                XposedHelpers.findAndHookMethod(Classes.SystemUI.ExpandableNotificationRow, "setIconAnimationRunningForChild", boolean.class, View.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (classMediaExpandableNotificationRow != null && classMediaExpandableNotificationRow.isAssignableFrom(param.thisObject.getClass()))
                            return null;
                        boolean running = (boolean) param.args[0];
                        View child = (View) param.args[1];
                        if (child != null) {
                            ImageView icon = child.findViewById(R.id.icon);
                            if (icon == null)
                                icon = child.findViewById(com.android.internal.R.id.icon);
                            if (icon == null)
                                icon = child.findViewById(android.R.id.icon);
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
                    XposedHelpers.findAndHookMethod(Classes.SystemUI.ExpandableNotificationRow, "setHeadsUp", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isHeadsUp = (boolean) param.args[0];
                            FrameLayout row = (FrameLayout) param.thisObject;
                            row.findViewById(R.id.notification_divider).setAlpha(isHeadsUp ? 0 : 1);
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "start", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        mPhoneStatusBar = param.thisObject;
                    }
                });

                XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = XposedHelpers.getObjectField(NotificationHooks.mPhoneStatusBar, "mNavigationBarView");
                        Object groupManager = XposedHelpers.getObjectField(param.thisObject, "mGroupManager");
                        if (mNavigationBarView == null) {
                            QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                            if (qsCustomizer != null)
                                qsCustomizer.setHasNavBar(false);
                        }
                        NotificationGroupManagerHooks.setHeadsUpManager(get(Fields.SystemUI.BaseStatusBar.mHeadsUpManager, param.thisObject));
                        NotificationPanelViewHooks.setGroupManager(groupManager);
                        NotificationsStuff.setGroupManager(groupManager);
                    }
                });

                XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "onBackPressed", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.onBackPressed()) {
                            param.setResult(true);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "animateCollapsePanels", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.isCustomizing()) {
                            qsCustomizer.hide(true);
                        }
                    }
                });

                if (!ConfigUtils.notifications().enable_notifications_background) {
                    XposedHelpers.findAndHookMethod(Classes.SystemUI.PhoneStatusBar, "updateNotificationShade", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ViewGroup stack = get(Fields.SystemUI.BaseStatusBar.mStackScroller, param.thisObject);
                            int childCount = stack.getChildCount();
                            boolean firstChild = true;
                            for (int i = 0; i < childCount; i++) {
                                View child = stack.getChildAt(i);
                                if (!Classes.SystemUI.ExpandableNotificationRow.isInstance(child)) {
                                    continue;
                                }
                                child.findViewById(R.id.notification_divider).setVisibility(firstChild ? View.INVISIBLE : View.VISIBLE);
                                firstChild = false;
                            }
                        }
                    });
                }

                if (ConfigUtils.M) {
                    Class classVolumeDialog = XposedHelpers.findClass("com.android.systemui.volume.VolumeDialog", Classes.SystemUI.getClassLoader());
                    XposedHelpers.findAndHookMethod(classVolumeDialog, "updateWindowWidthH", updateWindowWidthHHook);
                } else {
                    Class classVolumePanel = XposedHelpers.findClass("com.android.systemui.volume.VolumePanel", Classes.SystemUI.getClassLoader());
                    XposedHelpers.findAndHookMethod(classVolumePanel, "updateWidth", updateWidthHook);
                }

                XposedBridge.hookAllMethods(Classes.SystemUI.BaseStatusBar, "applyColorsAndBackgrounds", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object entry = param.args[1];
                            View contentView = ConfigUtils.M ? (View) XposedHelpers.callMethod(entry, "getContentView") : (View) XposedHelpers.getObjectField(entry, "expanded");
                            if (contentView.getId() !=
                                    contentView.getContext().getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID)) {
                                // Using custom RemoteViews
                                int targetSdk = get(Fields.SystemUI.NotificationDataEntry.targetSdk, entry);
                                if (targetSdk >= Build.VERSION_CODES.GINGERBREAD
                                        && targetSdk < Build.VERSION_CODES.LOLLIPOP) {

                                    invoke(Methods.SystemUI.ExpandableNotificationRow.setShowingLegacyBackground, get(Fields.SystemUI.NotificationDataEntry.row, entry), "setShowingLegacyBackground", true);
                                    set(Fields.SystemUI.NotificationDataEntry.legacy, entry, true);
                                }
                            }

                            ImageView icon = get(Fields.SystemUI.NotificationDataEntry.icon, entry);
                            if (icon != null) {
                                int targetSdk = get(Fields.SystemUI.NotificationDataEntry.targetSdk, entry);
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

                XposedHelpers.findAndHookMethod(classNotificationClicker, "register", Classes.SystemUI.ExpandableNotificationRow, StatusBarNotification.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        View row = (View) param.args[0];
                        StatusBarNotification sbn = (StatusBarNotification) param.args[1];
                        Notification notification = sbn.getNotification();
                        if (notification.contentIntent != null || notification.fullScreenIntent != null) {
                            row.setOnClickListener((View.OnClickListener) param.thisObject);
                            ExpandableNotificationRowHelper.getInstance(row).setOnClickListener((View.OnClickListener) param.thisObject);
                        } else {
                            row.setOnClickListener(null);
                            ExpandableNotificationRowHelper.getInstance(row).setOnClickListener(null);
                        }
                        return null;
                    }
                });

                if (config.notifications.change_colors) {
                    XposedHelpers.findAndHookConstructor(Classes.SystemUI.ActivatableNotificationView, Context.class, AttributeSet.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) param.args[0];
                            ResourceUtils res = ResourceUtils.getInstance(context);
                            set(Fields.SystemUI.ActivatableNotificationView.mNormalColor, param.thisObject, res.getColor(R.color.notification_material_background_color));
                            set(Fields.SystemUI.ActivatableNotificationView.mLowPriorityColor, param.thisObject, res.getColor(R.color.notification_material_background_low_priority_color));
                        }
                    });
                }

            }

            if (config.notifications.dismiss_button) {
                Class classDismissViewButton;
                try {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewButton", Classes.SystemUI.getClassLoader());
                } catch (Throwable t) {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewImageButton", Classes.SystemUI.getClassLoader());
                }
                XposedHelpers.findAndHookConstructor(classDismissViewButton, Context.class, AttributeSet.class, int.class, int.class, dismissViewButtonConstructorHook);
                XposedHelpers.findAndHookMethod(Classes.SystemUI.DismissView, "setOnButtonClickListener", View.OnClickListener.class, new XC_MethodReplacement() {
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

    private static Class<?> getClassMediaExpandableNotificationRow(ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass("com.android.systemui.statusbar.MediaExpandableNotificationRow", classLoader);
        } catch (Throwable t) {
            XposedHook.logD(TAG, "Class MediaExpandableNotificationRow not found. Skipping media row check.");
        }
        return null;
    }

    public static void setActions(Object builder, Notification.Action... actions) {
        ArrayList mActions = (ArrayList) XposedHelpers.getObjectField(builder, "mActions"); //TODO: maybe optimize
        mActions.clear();
        for (int i = 0; i < actions.length; i++) {
            mActions.add(actions[i]);
        }
    }

    private static void processForRemoteInput(Object baseStatusBar, Notification n) {

//        XposedHook.logI(TAG, "in processForRemoteInput!");

        if (n.extras != null && n.extras.containsKey("android.wearable.EXTENSIONS") &&
                (n.actions == null || n.actions.length == 0)) {
            Notification.Action viableAction = null;
            Notification.WearableExtender we = new Notification.WearableExtender(n);

            List<Notification.Action> actions = we.getActions();
            final int numActions = actions.size();

            for (int i = 0; i < numActions; i++) {
                XposedHook.logI(TAG, "RemoteInputs: action " + i);
                Notification.Action action = actions.get(i);
                if (action == null) {
                    continue;
                }
                RemoteInput[] remoteInputs = action.getRemoteInputs();
                if (remoteInputs == null) {
                    continue;
                }
                XposedHook.logI(TAG, "RemoteInputs: remoteInputs not null!");
                for (RemoteInput ri : remoteInputs) {
                    if (ri.getAllowFreeFormInput()) {
                        viableAction = action;
                        break;
                    }
                }
                if (viableAction != null) {
                    break;
                }
            }

            if (viableAction != null) {
                Notification.Builder rebuilder = recoverBuilder((Context) XposedHelpers.getObjectField(baseStatusBar, "mContext"), n);
                setActions(rebuilder, viableAction);
                rebuilder.build(); // will rewrite n
                XposedHook.logI(TAG, "viableAction != null");
            }
        }
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

    public static int getMaxKeyguardNotifications(Object statusBar, boolean recompute) {
        View mNotificationPanel = get(Fields.SystemUI.PhoneStatusBar.mNotificationPanel, statusBar);
        if (recompute) {
            mMaxKeyguardNotifications = Math.max(1,
                    NotificationPanelViewHooks.computeMaxKeyguardNotifications(mNotificationPanel,
                            XposedHelpers.getIntField(statusBar, "mKeyguardMaxNotificationCount")));
            return mMaxKeyguardNotifications;
        }
        return mMaxKeyguardNotifications;
    }

    public static void onPanelLaidOut(Object statusBar) {
        if (getInt(Fields.SystemUI.BaseStatusBar.mState, statusBar) == NotificationPanelHooks.STATE_KEYGUARD) {
            // Since the number of notifications is determined based on the height of the view, we
            // need to update them.
            int maxBefore = getMaxKeyguardNotifications(statusBar, false /* recompute */);
            int maxNotifications = getMaxKeyguardNotifications(statusBar , true /* recompute */);
            if (maxBefore != maxNotifications) {
                //XposedHelpers.setAdditionalInstanceField(statusBar, "recompute", true);
                invoke(Methods.SystemUI.BaseStatusBar.updateRowStates, statusBar);
            }
        }
    }

    public static Notification.Builder recoverBuilder(Context context, Notification n) {
        // Re-create notification context so we can access app resources.
        ApplicationInfo applicationInfo = n.extras.getParcelable(
                EXTRA_REBUILD_CONTEXT_APPLICATION_INFO);
        Context builderContext;
        if (applicationInfo != null) {
            builderContext = (Context) XposedHelpers.callMethod(context, "createApplicationContext", applicationInfo,
                    Context.CONTEXT_RESTRICTED);
            if (builderContext == null) {
                builderContext = context;
            }
        } else {
            builderContext = context; // try with given context
        }
        n.extras.putParcelable(EXTRA_LARGE_ICON, n.getLargeIcon());
        return (Notification.Builder) newInstance(XposedHelpers.findConstructorBestMatch(Notification.Builder.class, Context.class, Notification.class), builderContext, n);
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
                        action.setLayoutParams(lParams);
                        action.setPaddingRelative(padding, padding, padding, padding);
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
                        TextView title = layout.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID));
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

            int notificationContentMargin = res.getDimensionPixelSize(R.dimen.notification_content_start_margin);
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
            progressLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_start_margin));
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

    private static final XC_LayoutInflated status_bar_notification_row = new XC_LayoutInflated(XCallback.PRIORITY_HIGHEST) {
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
                ExpandableOutlineViewHelper.getInstance(row).onFinishInflate();
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
            int iconSize = res.getDimensionPixelSize(R.dimen.media_notification_expanded_image_max_size);
            View mediaActions = oldLayout.findViewById(context.getResources().getIdentifier("media_actions", "id", PACKAGE_ANDROID));
            ImageView rightIcon = getLargeRightIcon(context);

            FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_material_base", "layout", PACKAGE_ANDROID), null);
            View header = layout.findViewById(R.id.notification_header);
            LinearLayout notificationMain = layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", PACKAGE_ANDROID));
            LinearLayout contentContainer = new LinearLayout(context);
            MediaNotificationView newLayout = new MediaNotificationView(context, rightIcon, mediaActions, header, notificationMain);
            newLayout.setId(R.id.status_bar_latest_event_content);

            oldLayout.removeAllViews();
            layout.removeAllViews();

            oldLayout.setBackgroundColor(0);
            oldLayout.addView(newLayout);

            contentContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams mediaActionsLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            mediaActionsLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_media_actions_margin_top);
            mediaActions.setPaddingRelative(res.getDimensionPixelSize(R.dimen.big_media_actions_margin_start), 0, 0, res.getDimensionPixelSize(R.dimen.media_notification_actions_padding_bottom));

            LinearLayout.LayoutParams notificationMainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            notificationMainLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_main_plus_big_picture_margin_end));
            notificationMainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_start_margin));
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

            layout.setId(R.id.status_bar_latest_event_content);
            oldLayout.setBackgroundColor(0);
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
            notificationMainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_start_margin));
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
            pictureLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_start_margin));
            pictureLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_margin_end));
            pictureLp.topMargin = res.getDimensionPixelSize(R.dimen.notification_big_picture_margin_top);
            pictureLp.bottomMargin = res.getDimensionPixelSize(R.dimen.notification_big_picture_margin_bottom);
            bigPicture.setAdjustViewBounds(true);
            bigPicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            mainLp.gravity = Gravity.TOP;
            mainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_start_margin));
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

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_start_margin);
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

