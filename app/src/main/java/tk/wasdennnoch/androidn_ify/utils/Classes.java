package tk.wasdennnoch.androidn_ify.utils;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.service.notification.NotificationListenerService;
import android.widget.RemoteViews;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public final class Classes {

    private static final String TAG = Classes.class.getSimpleName();

    public static final class Android {
        private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;

        private static ClassLoader sClassLoader;

        public static Class remoteViewsOnClickHandler;
        public static Class BuilderRemoteViews;

        private Android() {}

        public static void init(ClassLoader classLoader) {
            sClassLoader = classLoader;

            remoteViewsOnClickHandler = XposedHelpers.findClass(RemoteViews.class.getName() + "$OnClickHandler", sClassLoader);
            BuilderRemoteViews = XposedHelpers.findClass(Notification.class.getName() + "$BuilderRemoteViews", classLoader);

            Methods.Android.init();
            Fields.Android.init();
        }

        public static ClassLoader getClassLoader() {
            return sClassLoader;
        }
    }

    public static final class SystemUI {
        private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
        private static final String PACKAGE_STATUSBAR = PACKAGE_SYSTEMUI + ".statusbar";
        private static final String PACKAGE_STATUSBAR_PHONE = PACKAGE_STATUSBAR + ".phone";
        private static final String PACKAGE_POLICY = PACKAGE_STATUSBAR + ".policy";
        private static final String PACKAGE_STATUSBAR_STACK = PACKAGE_STATUSBAR + ".stack";
        private static final String PACKAGE_QS = PACKAGE_SYSTEMUI + ".qs";

        private static ClassLoader sClassLoader;

        public static Class SystemUIApplication;
        public static Class ExpandHelper;
        public static Class ExpandHelperCallback;
        public static Class ViewScaler;

        public static Class BaseStatusBar;
        public static Class NotificationData;
        public static Class NotificationDataEntry;
        public static Class PhoneStatusBar;
        public static Class StatusBarBroadCastReceiver;
        public static Class StatusBarNotificationListener;
        public static Class StatusBarHeaderView;
        public static Class LayoutValues;
        public static Class NotificationPanelView;
        public static Class PanelView;
        public static Class ObservableScrollView;
        public static Class ScrimController;
        public static Class HeadsUpManager;
        public static Class HeadsUpManagerOnHeadsUpChangedListener;
        public static Class HeadsUpEntry;
        public static Class StatusBarClock;
        public static Class StatusBarWindowManager;
        public static Class StatusBarWindowManagerState;
        public static Class NotificationGroupManager;
        public static Class NotificationGroup;

        public static Class QSPanel;
        public static Class QSRecord;
        public static Class QSTileRecord;
        public static Class QSContainer;
        public static Class QSTile;
        public static Class QSState;
        public static Class DetailAdapter;
        public static Class QSTileView;
        public static Class QSDetailItems;

        public static Class NotificationBackgroundView;
        public static Class ExpandableView;
        public static Class ExpandableNotificationRow;
        public static Class ActivatableNotificationView;
        public static Class NotificationContentView;

        public static Class NotificationChildrenContainer;
        public static Class NotificationStackScrollLayout;
        public static Class SwipeHelper;
        public static Class SwipeHelperCallback;
        public static Class AnimationFilter;
        public static Class StackStateAnimator;
        public static Class StackScrollAlgorithm;
        public static Class StackScrollAlgorithmState;
        public static Class StackScrollState;
        public static Class StackViewState;
        public static Class ViewState;
        public static Class AmbientState;

        public static Class TunerService;
        public static Class Recents;
        public static Class RecentsActivity;

        public static Class EmptyShadeView;
        public static Class DismissView;
        public static Class ScrimView;

        private SystemUI() {}

        public static void init(ClassLoader classLoader) {
            sClassLoader = classLoader;

            initClasses();
            Methods.SystemUI.init();
            Fields.SystemUI.init();
        }

        private static void initClasses() {
            SystemUIApplication = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".SystemUIApplication", sClassLoader);
            ExpandHelper = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".ExpandHelper", sClassLoader);
            ExpandHelperCallback = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".ExpandHelper$Callback", sClassLoader);
            ViewScaler = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".ExpandHelper$ViewScaler", sClassLoader);

            BaseStatusBar = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".BaseStatusBar", sClassLoader);
            NotificationData = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".NotificationData", sClassLoader);
            NotificationDataEntry = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".NotificationData$Entry", sClassLoader);
            NotificationBackgroundView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".NotificationBackgroundView", sClassLoader);
            ExpandableView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".ExpandableView", sClassLoader);
            ExpandableNotificationRow = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".ExpandableNotificationRow", sClassLoader);
            NotificationContentView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".NotificationContentView", sClassLoader);
            ActivatableNotificationView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".ActivatableNotificationView", sClassLoader);
            PhoneStatusBar = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".PhoneStatusBar", sClassLoader);
            StatusBarHeaderView = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".StatusBarHeaderView", sClassLoader);
            LayoutValues = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".StatusBarHeaderView$LayoutValues", sClassLoader);
            NotificationPanelView = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".NotificationPanelView", sClassLoader);
            PanelView = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".PanelView", sClassLoader);
            ObservableScrollView = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".ObservableScrollView", sClassLoader);
            ScrimController = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".ScrimController", sClassLoader);
            NotificationGroupManager = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".NotificationGroupManager", sClassLoader);
            NotificationGroup = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".NotificationGroupManager$NotificationGroup", sClassLoader);
            StatusBarWindowManager = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".StatusBarWindowManager", sClassLoader);
            StatusBarWindowManagerState = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".StatusBarWindowManager.State", sClassLoader);

            HeadsUpManager = XposedHelpers.findClass(PACKAGE_POLICY + ".HeadsUpManager", sClassLoader);
            HeadsUpManagerOnHeadsUpChangedListener = XposedHelpers.findClass(PACKAGE_POLICY + ".HeadsUpManager$OnHeadsUpChangedListener", sClassLoader);
            HeadsUpEntry = XposedHelpers.findClass(PACKAGE_POLICY + ".HeadsUpManager$HeadsUpEntry", sClassLoader);
            StatusBarClock = XposedHelpers.findClass(PACKAGE_POLICY + ".Clock", sClassLoader);

            SwipeHelper = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".SwipeHelper", sClassLoader);
            SwipeHelperCallback = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".SwipeHelper$Callback", sClassLoader);

            NotificationStackScrollLayout = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".NotificationStackScrollLayout", sClassLoader);
            NotificationChildrenContainer = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".NotificationChildrenContainer", sClassLoader);
            AnimationFilter = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".AnimationFilter", sClassLoader);
            StackStateAnimator = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".StackStateAnimator", sClassLoader);
            StackScrollAlgorithm = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".StackScrollAlgorithm", sClassLoader);
            StackScrollAlgorithmState = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".StackScrollAlgorithm.StackScrollAlgorithmState", sClassLoader);
            StackScrollState = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".StackScrollState", sClassLoader);
            StackViewState = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".StackViewState", sClassLoader);
            ViewState = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".ViewState", sClassLoader);
            AmbientState = XposedHelpers.findClass(PACKAGE_STATUSBAR_STACK + ".AmbientState", sClassLoader);

            QSPanel = XposedHelpers.findClass(PACKAGE_QS + ".QSPanel", sClassLoader);
            QSRecord = XposedHelpers.findClass(PACKAGE_QS + ".QSPanel$Record", sClassLoader);
            QSTileRecord = XposedHelpers.findClass(PACKAGE_QS + ".QSPanel$TileRecord", sClassLoader);
            QSContainer = XposedHelpers.findClass(PACKAGE_QS + ".QSContainer", sClassLoader);
            QSTile = XposedHelpers.findClass(PACKAGE_QS + ".QSTile", sClassLoader);
            QSState = XposedHelpers.findClass(PACKAGE_QS + ".QSTile$State", sClassLoader);
            DetailAdapter = XposedHelpers.findClass(PACKAGE_QS + ".QSTile$DetailAdapter", sClassLoader);
            QSTileView = XposedHelpers.findClass(PACKAGE_QS + ".QSTileView", sClassLoader);
            QSDetailItems = XposedHelpers.findClass(PACKAGE_QS + ".QSDetailItems", sClassLoader);

            TunerService = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".tuner.TunerService", sClassLoader);

            Recents = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".recents.Recents", sClassLoader);
            RecentsActivity = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".recents.RecentsActivity", sClassLoader);

            DismissView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".DismissView", sClassLoader);
            EmptyShadeView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".EmptyShadeView", sClassLoader);
            ScrimView = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".ScrimView", sClassLoader);

            boolean foundClasses = false;

            for (int i = 1; !foundClasses; i++) {
                Class temp1 = null;
                Class temp2 = null;
                try {
                    temp1 = XposedHelpers.findClass(PACKAGE_STATUSBAR_PHONE + ".PhoneStatusBar$" + i, sClassLoader);
                    temp2 = XposedHelpers.findClass(PACKAGE_STATUSBAR + ".BaseStatusBar$" + i, sClassLoader);
                } catch (XposedHelpers.ClassNotFoundError ignore) {} //wtf

                if (temp1 != null && StatusBarBroadCastReceiver == null && BroadcastReceiver.class.isAssignableFrom(temp1)) {
                    StatusBarBroadCastReceiver = temp1;
                }

                if (temp2 != null && StatusBarNotificationListener == null && NotificationListenerService.class.isAssignableFrom(temp2)) {
                    StatusBarNotificationListener = temp2;
                }
                foundClasses = StatusBarBroadCastReceiver != null && StatusBarNotificationListener != null;
            }
        }

        public static ClassLoader getClassLoader() {
            return sClassLoader;
        }
    }

    public static final class Keyguard {
        public static Class CarrierText;

        private Keyguard() {}

        public static void init(ClassLoader classLoader) {
            CarrierText = XposedHelpers.findClass("com.android.keyguard.CarrierText", classLoader);
        }
    }



}
