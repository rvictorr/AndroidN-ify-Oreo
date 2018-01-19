package tk.wasdennnoch.androidn_ify.utils;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public final class Classes {

    private static final String TAG = Classes.class.getSimpleName();

    public static final class SystemUI {
        private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

        private static ClassLoader sClassLoader;

        public static Class SystemUIApplication;

        public static Class BaseStatusBar;
        public static Class PhoneStatusBar;
        public static Class StatusBarHeaderView;
        public static Class LayoutValues;
        public static Class NotificationPanelView;
        public static Class PanelView;
        public static Class ObservableScrollView;
        public static Class ScrimController;
        public static Class HeadsUpManager;
        public static Class StatusBarClock;
        public static Class StatusBarWindowManager;
        public static Class StatusBarWindowManagerState;

        public static Class QSPanel;
        public static Class QSRecord;
        public static Class QSContainer;
        public static Class QSTile;
        public static Class QSState;
        public static Class DetailAdapter;
        public static Class QSTileView;
        public static Class QSDetailItems;

        public static Class ExpandableView;
        public static Class ExpandableNotificationRow;
        public static Class ActivatableNotificationView;
        public static Class NotificationContentView;

        public static Class NotificationChildrenContainer;
        public static Class NotificationStackScrollLayout;
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

            SystemUIApplication = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".SystemUIApplication", classLoader);

            BaseStatusBar = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.BaseStatusBar", classLoader);
            PhoneStatusBar = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.PhoneStatusBar", classLoader);
            StatusBarHeaderView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarHeaderView", classLoader);
            LayoutValues = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarHeaderView$LayoutValues", classLoader);
            NotificationPanelView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.NotificationPanelView", classLoader);
            PanelView = XposedHelpers.findClass(PACKAGE_SYSTEMUI +  ".statusbar.phone.PanelView", classLoader);
            ObservableScrollView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.ObservableScrollView", classLoader);
            ScrimController = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.ScrimController", classLoader);

            HeadsUpManager = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.policy.HeadsUpManager", classLoader);
            StatusBarClock = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.policy.Clock", classLoader);

            NotificationStackScrollLayout = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.NotificationStackScrollLayout", classLoader);
            NotificationChildrenContainer = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.NotificationChildrenContainer", classLoader);
            StackScrollAlgorithm = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.StackScrollAlgorithm", classLoader);
            StackScrollAlgorithmState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.StackScrollAlgorithm.StackScrollAlgorithmState", classLoader);
            StackScrollState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.StackScrollState", classLoader);
            StackViewState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.StackViewState", classLoader);
            ViewState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.ViewState", classLoader);
            AmbientState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.stack.AmbientState", classLoader);

            StatusBarWindowManager = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager", classLoader);
            StatusBarWindowManagerState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager.State", classLoader);

            ExpandableView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ExpandableView", classLoader);
            ExpandableNotificationRow = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ExpandableNotificationRow", classLoader);
            NotificationContentView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.NotificationContentView", classLoader);
            ActivatableNotificationView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ActivatableNotificationView", classLoader);

            QSPanel = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSPanel", classLoader);
            QSRecord = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSPanel$Record", classLoader);
            QSContainer = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSContainer", classLoader);
            QSTile = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSTile", classLoader);
            QSState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSTile$State", classLoader);
            DetailAdapter = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSTile$DetailAdapter", classLoader);
            QSTileView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSTileView", classLoader);
            QSDetailItems = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".qs.QSDetailItems", classLoader);

            TunerService = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".tuner.TunerService", classLoader);

            Recents = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".recents.Recents", classLoader);
            RecentsActivity = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".recents.RecentsActivity", classLoader);

            DismissView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.DismissView", classLoader);
            EmptyShadeView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.EmptyShadeView", classLoader);
            ScrimView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ScrimView", classLoader);

            Methods.SystemUI.init();
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
