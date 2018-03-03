package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.annotation.Nullable;
import android.service.notification.StatusBarNotification;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.Methods;

import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.NotificationDataEntry;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationDataEntry.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationGroupManager.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.NotificationGroupManager.NotificationGroup.*;
import static tk.wasdennnoch.androidn_ify.utils.Methods.SystemUI.NotificationGroupManager.*;
import static tk.wasdennnoch.androidn_ify.utils.Classes.SystemUI.NotificationGroupManager;

public class NotificationGroupManagerHooks {

    private static HashMap<String, StatusBarNotification> mIsolatedEntries = new HashMap<>();
    private static Object mHeadsUpManager;
    private static boolean mIsUpdatingUnchangedGroup;

    public static void hook() {

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "isVisible", StatusBarNotification.class, XC_MethodReplacement.returnConstant(true));
        XposedHelpers.findAndHookMethod(NotificationGroupManager, "hasGroupChildren", StatusBarNotification.class, XC_MethodReplacement.returnConstant(true));

//        XposedHelpers.findAndHookMethod(NotificationGroupManager, "isGroupExpanded", StatusBarNotification.class, new XC_MethodReplacement() {
//            @Override
//            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
//                return isGroupExpanded(param.thisObject, (StatusBarNotification) param.args[0]);
//            }
//        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "setGroupExpanded", StatusBarNotification.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                setGroupExpanded(param.thisObject, (StatusBarNotification) param.args[0], (boolean) param.args[1]);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "onEntryRemoved", Classes.SystemUI.NotificationDataEntry, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mIsolatedEntries.remove(get(key, param.thisObject));
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "onEntryRemovedInternal", Classes.SystemUI.NotificationDataEntry, StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object removed = param.args[0];
                final StatusBarNotification sbn = (StatusBarNotification) param.args[1];
                String groupKey = getGroupKey(sbn);
                final Object group = ((HashMap) get(mGroupMap, param.thisObject)).get(groupKey);
                if (group == null) {
                    // When an app posts 2 different notifications as summary of the same group, then a
                    // cancellation of the first notification removes this group.
                    // This situation is not supported and we will not allow such notifications anymore in
                    // the close future. See b/23676310 for reference.
                    return null;
                }
                HashSet childrenSet = get(children, group);
                if (isGroupChild(sbn)) {
                    childrenSet.remove(removed);
                } else {
                    set(summary, group, null);
                }
                updateSuppression(param.thisObject, group);
                if (childrenSet.isEmpty()) {
                    if (get(summary, group) == null) {
                        ((HashMap) get(mGroupMap, param.thisObject)).remove(groupKey);
                    }
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "onEntryAdded", NotificationDataEntry, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                final Object added = param.args[0];
                HashMap groupMap = get(mGroupMap, param.thisObject);
                final StatusBarNotification sbn = get(notification, added);
                boolean isGroupChild = isGroupChild(sbn);
                String groupKey = getGroupKey(sbn);
                Object group = groupMap.get(groupKey);
                if (group == null) {
                    group = newInstance(Methods.SystemUI.NotificationGroupManager.NotificationGroup.constructor);
                    groupMap.put(groupKey, group);
                }
                HashSet childrenSet = get(children, group);
                if (isGroupChild) {
                    childrenSet.add(added);
                    updateSuppression(param.thisObject, group);
                } else {
                    set(summary, group, added);
                    set(expanded, group, invoke(Methods.SystemUI.ExpandableNotificationRow.areChildrenExpanded, get(row, added)));
                    updateSuppression(param.thisObject, group);
                    if (!childrenSet.isEmpty()) {
                        HashSet childrenCopy =
                                (HashSet) childrenSet.clone();
                        for (Object child : childrenCopy) {
                            onEntryBecomingChild(param.thisObject, child);
                        }
                        XposedHelpers.callMethod(get(mListener, param.thisObject), "onGroupCreatedFromChildren", group);
                    }
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "onEntryUpdated", NotificationDataEntry, StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object entry = param.args[0];
                StatusBarNotification oldNotification = (StatusBarNotification) param.args[1];
                StatusBarNotification notif = get(notification, entry);
                HashMap groupMap = get(mGroupMap, param.thisObject);

                String oldKey = oldNotification.getGroupKey();
                String newKey = notif.getGroupKey();
                boolean groupKeysChanged = !oldKey.equals(newKey);
                boolean wasGroupChild = isGroupChild(oldNotification);
                boolean isGroupChild = isGroupChild(notif);
                mIsUpdatingUnchangedGroup = !groupKeysChanged && wasGroupChild == isGroupChild;
                if (groupMap.get(getGroupKey(oldNotification)) != null) {
                    invoke(onEntryRemovedInternal, param.thisObject, entry, oldNotification);
                }
                invoke(onEntryAdded, param.thisObject, entry);
                mIsUpdatingUnchangedGroup = false;
                if (isIsolated(notif)) {
                    mIsolatedEntries.put((String) get(key, entry), notif);
                    if (groupKeysChanged) {
                        updateSuppression(param.thisObject, groupMap.get(oldKey));
                        updateSuppression(param.thisObject, groupMap.get(newKey));
                    }
                } else if (!wasGroupChild && isGroupChild) {
                    onEntryBecomingChild(param.thisObject, entry);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "setStatusBarState", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                int newState = (int) param.args[0];

                if (getInt(mBarState, param.thisObject) == newState) {
                    return null;
                }

                set(mBarState, param.thisObject, newState);
                if (invoke(areGroupsProhibited, param.thisObject)) {
                    collapseAllGroups(param.thisObject);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "isChildInGroupWithSummary", StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                HashMap groupMap = get(mGroupMap, param.thisObject);

                if (!isGroupChild(sbn)) {
                    return false;
                }
                Object group = groupMap.get(getGroupKey(sbn));
                Object suppressed = null;
                if (group != null)
                    suppressed = XposedHelpers.getAdditionalInstanceField(group, "suppressed");
                if (group == null || get(summary, group) == null || (suppressed != null && (boolean) suppressed)) {
                    return false;
                }
                if (((HashSet) get(children, group)).isEmpty()) {
                    // If the suppression of a group changes because the last child was removed, this can
                    // still be called temporarily because the child hasn't been fully removed yet. Let's
                    // make sure we still return false in that case.
                    return false;
                }
                return true;
            }
        });

        XposedHelpers.findAndHookMethod(NotificationGroupManager, "getGroupSummary", StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return getGroupSummary(param.thisObject, getGroupKey((StatusBarNotification) param.args[0]));
            }
        });
    }

    public static boolean isGroupExpanded(Object groupManager, StatusBarNotification sbn) {
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        return getBoolean(expanded, group);
    }

    public static void setGroupExpanded(Object groupManager, StatusBarNotification sbn, boolean expanded) {
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(getGroupKey(sbn));
        if (group == null) {
            return;
        }
        invoke(setGroupExpandedGroup, groupManager, group, expanded);
    }

    private static void onEntryBecomingChild(Object groupManager, Object entry) {
        if (invoke(Methods.SystemUI.ExpandableNotificationRow.isHeadsUp, get(row, entry))) {
            onHeadsUpStateChanged(groupManager, entry, true);
        }
    }

    private static void updateSuppression(Object groupManager, Object group) {
        if (group == null) {
            return;
        }
        HashSet groupChildren = get(children, group);
        Object groupSummary = get(summary, group);
        StatusBarNotification sbn = null;
        if (groupSummary != null)
            sbn = get(notification, groupSummary);
        Object suppressed = XposedHelpers.getAdditionalInstanceField(group, "suppressed");
        boolean prevSuppressed = suppressed != null && (boolean) suppressed;
        boolean nowSuppressed;
        nowSuppressed = groupSummary != null && !getBoolean(expanded, group)
                && (groupChildren.size() == 1
                || (groupChildren.size() == 0
                && isGroupSummary(sbn.getNotification())
                && hasIsolatedChildren(group)));
        XposedHelpers.setAdditionalInstanceField(group, "suppressed", nowSuppressed);
        nowSuppressed = (boolean) XposedHelpers.getAdditionalInstanceField(group, "suppressed");
        if (prevSuppressed != nowSuppressed) {
            if (nowSuppressed) {
                handleSuppressedSummaryHeadsUpped(groupManager, groupSummary);
            }
            if (!mIsUpdatingUnchangedGroup) {
                NotificationStackScrollLayoutHooks.onGroupsChanged();
            }
        }
    }

    private static boolean hasIsolatedChildren(Object group) {
        StatusBarNotification sbn = get(notification, get(summary, group));
        return getNumberOfIsolatedChildren(sbn.getGroupKey()) != 0;
    }

    private static int getNumberOfIsolatedChildren(String groupKey) {
        int count = 0;
        for (StatusBarNotification sbn : mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                count++;
            }
        }
        return count;
    }

    private static Object getIsolatedChild(Object groupManager, String groupKey) {
        for (StatusBarNotification sbn : mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                return get(summary, ((HashMap) get(mGroupMap, groupManager)).get(sbn.getKey()));
            }
        }
        return null;
    }

    public static boolean isSummaryOfSuppressedGroup(Object groupManager, StatusBarNotification sbn) {
        return isGroupSuppressed(groupManager, getGroupKey(sbn)) && isGroupSummary(sbn.getNotification());
    }

    private static boolean isOnlyChild(Object groupManager, StatusBarNotification sbn) {

        return !isGroupSummary(sbn.getNotification())
                && getTotalNumberOfChildren(groupManager, sbn) == 1;
    }

    public static boolean isOnlyChildInGroup(Object groupManager, StatusBarNotification sbn) { //TODO: call in NotificationStackScrollLayout and BaseStatusBar (not sure if it is really needed though)
        if (!isOnlyChild(groupManager, sbn)) {
            return false;
        }
        View logicalGroupSummary = getLogicalGroupSummary(groupManager, sbn);
        return logicalGroupSummary != null
                && !invoke(Methods.SystemUI.ExpandableNotificationRow.getStatusBarNotification, logicalGroupSummary).equals(sbn);
    }

    private static int getTotalNumberOfChildren(Object groupManager, StatusBarNotification sbn) {
        int isolatedChildren = getNumberOfIsolatedChildren(sbn.getGroupKey());
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(sbn.getGroupKey());
        int realChildren = group != null ? ((HashSet) get(children, group)).size() : 0;
        return isolatedChildren + realChildren;
    }

    private static boolean isGroupSuppressed(Object groupManager, String groupKey) {
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(groupKey);
        Object suppressed = null;
        if (group != null)
            suppressed = XposedHelpers.getAdditionalInstanceField(group, "suppressed");
        return group != null && (suppressed != null && (boolean) suppressed);
    }

    public static void collapseAllGroups(Object groupManager) {
        // Because notifications can become isolated when the group becomes suppressed it can
        // lead to concurrent modifications while looping. We need to make a copy.
        ArrayList groupCopy = new ArrayList(((HashMap) get(mGroupMap, groupManager)).values());
        int size = groupCopy.size();
        for (int i = 0; i < size; i++) {
            Object group =  groupCopy.get(i);
            if (getBoolean(expanded, group)) {
                invoke(setGroupExpandedGroup, groupManager, group, false);
            }
            updateSuppression(groupManager, group);
        }
    }

    public static boolean isSummaryOfGroup(Object groupManager, StatusBarNotification sbn) {
        if (!isGroupSummary(sbn)) {
            return false;
        }
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        return !((HashSet) get(children, group)).isEmpty();
    }

    public static View getLogicalGroupSummary(Object groupManager,
            StatusBarNotification sbn) {
        return getGroupSummary(groupManager, sbn.getGroupKey());
    }

    @Nullable
    public static View getGroupSummary(Object groupManager, String groupKey) {
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(groupKey);
        return group == null ? null
                : get(summary, group) == null ? null
                : (View) get(row, get(summary, group));
    }

    public static boolean toggleGroupExpansion(Object groupManager, StatusBarNotification sbn) {
        Object group = ((HashMap) get(mGroupMap, groupManager)).get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        invoke(setGroupExpandedGroup, groupManager, group, !getBoolean(expanded, group));
        return getBoolean(expanded, group);
    }

    private static boolean isIsolated(StatusBarNotification sbn) {
        return mIsolatedEntries.containsKey(sbn.getKey());
    }

    private static boolean isGroupSummary(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return true;
        }
        return isGroupSummary(sbn.getNotification());
    }

    private static boolean isGroupChild(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return false;
        }
        return isGroup(sbn) && !isGroupSummary(sbn.getNotification());
    }

    private static String getGroupKey(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return sbn.getKey();
        }
        return sbn.getGroupKey();
    }

    public static void onHeadsUpStateChanged(Object groupManager, Object entry, boolean isHeadsUp) {
        final StatusBarNotification sbn = get(notification, entry);
        if (invoke(Methods.SystemUI.ExpandableNotificationRow.isHeadsUp, get(row, entry))) {
            if (shouldIsolate(groupManager, sbn)) {
                // We will be isolated now, so lets update the groups
                invoke(onEntryRemovedInternal, groupManager, entry, sbn);

                mIsolatedEntries.put(sbn.getKey(), sbn);

                invoke(onEntryAdded, groupManager, entry);
                // We also need to update the suppression of the old group, because this call comes
                // even before the groupManager knows about the notification at all.
                // When the notification gets added afterwards it is already isolated and therefore
                // it doesn't lead to an update.
                updateSuppression(groupManager, ((HashMap) get(mGroupMap, groupManager)).get(sbn.getGroupKey()));
                NotificationStackScrollLayoutHooks.onGroupsChanged();
            } else {
                handleSuppressedSummaryHeadsUpped(groupManager, entry);
            }
        } else {
            if (mIsolatedEntries.containsKey(sbn.getKey())) {
                // not isolated anymore, we need to update the groups
                invoke(onEntryRemovedInternal, groupManager, entry, sbn);
                mIsolatedEntries.remove(sbn.getKey());
                invoke(onEntryAdded, groupManager, entry);
                NotificationStackScrollLayoutHooks.onGroupsChanged();
            }
        }
    }

    private static void handleSuppressedSummaryHeadsUpped (Object groupManager, Object entry) {
        StatusBarNotification sbn = get(notification, entry);
        if (!isGroupSuppressed(groupManager, sbn.getGroupKey())
                || !isGroupSummary(sbn.getNotification())
                || !(boolean) invoke(Methods.SystemUI.ExpandableNotificationRow.isHeadsUp, get(row, entry))) {
            return;
        }
        // The parent of a suppressed group got huned, lets hun the child!
        Object notificationGroup = ((HashMap) get(mGroupMap, groupManager)).get(sbn.getGroupKey());
        if (notificationGroup != null) {
            Iterator<Object> iterator = ((HashSet) get(children, notificationGroup)).iterator();
            Object child = iterator.hasNext() ? iterator.next() : null;
            if (child == null) {
                child = getIsolatedChild(groupManager, sbn.getGroupKey());
            }
            if (child != null) {
                if (invoke(Methods.SystemUI.HeadsUpManager.isHeadsUp, mHeadsUpManager, get(key, child))) {
                    invoke(Methods.SystemUI.HeadsUpManager.updateNotification, mHeadsUpManager, child, true);
                } else {
                    invoke(Methods.SystemUI.HeadsUpManager.showNotification, mHeadsUpManager, child);
                }
            }
        }
        invoke(Methods.SystemUI.HeadsUpManager.releaseImmediately, mHeadsUpManager, get(key, entry));
    }

    private static boolean shouldIsolate(Object groupManager, StatusBarNotification sbn) {
        Object notificationGroup = ((HashMap) get(mGroupMap, groupManager)).get(sbn.getGroupKey());
        return (isGroup(sbn) && !isGroupSummary(sbn.getNotification()))
                && (sbn.getNotification().fullScreenIntent != null
                    || notificationGroup == null
                    || !getBoolean(expanded, notificationGroup)
                    || isGroupNotFullyVisible(notificationGroup));
    }

    private static boolean isGroupNotFullyVisible(Object notificationGroup) {
        Object groupSummary = get(summary, notificationGroup);
        return groupSummary == null
                || (int) invoke(Methods.SystemUI.ExpandableView.getClipTopAmount, get(row, groupSummary)) > 0
                || ((View) get(row, groupSummary)).getTranslationY() < 0;
    }

    public static void setHeadsUpManager(Object headsUpManager) {
        mHeadsUpManager = headsUpManager;
    }

    private static boolean isGroupSummary(Object n) {
        return invoke(Methods.Android.Notification.isGroupSummary, n);
    }

    private static boolean isGroup(StatusBarNotification sbn) {
        if (sbn.getNotification().getGroup() != null || sbn.getNotification().getSortKey() != null) {
            return true;
        }
        return false;
    }
}
