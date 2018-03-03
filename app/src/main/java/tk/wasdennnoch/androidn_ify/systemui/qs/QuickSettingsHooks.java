package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.PagedTileLayout;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSDetail;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import tk.wasdennnoch.androidn_ify.utils.ReflectionUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class QuickSettingsHooks {

    private static final String TAG = "QuickSettingsHooks";

    static final String CLASS_QS_DRAG_PANEL = "com.android.systemui.qs.QSDragPanel";

    final Class mHookClass;
    private final Class mSecondHookClass;

    protected Context mContext;
    ViewGroup mQsPanel;
    View mBrightnessView;
    Object mFooter;
    View mDetail;
    View mDivider;

    private PagedTileLayout mTileLayout;
    private QSDetail mQSDetail;
    private boolean mHookedGetGridHeight = false;
    private boolean mExpanded;
    private XC_MethodReplacement
            getGridHeightHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return getGridHeight();
        }
    };

    private Method mSetDetailRecord;

    public static QuickSettingsHooks create() {
        ClassLoader classLoader = Classes.SystemUI.getClassLoader();
        try {
            XposedHelpers.findClass(CLASS_QS_DRAG_PANEL, classLoader);
            return new CMQuickSettingsHooks(classLoader);
        } catch (Throwable t) {
            return new QuickSettingsHooks(classLoader);
        }
    }

    QuickSettingsHooks(ClassLoader classLoader) {
        mHookClass = XposedHelpers.findClass(getHookClass(), classLoader);
        mSecondHookClass = XposedHelpers.findClass(getSecondHookClass(), classLoader);
        QuickSettingsTileHooks.hook();
        hookConstructor();
        hookOnMeasure();
        hookOnLayout();
        hookUpdateResources();
        hookSetTiles();
        hookAddTile();
        hookSetGridContentVisibility();
        hookSetExpanded();
        hookFireScanStateChanged();

        try {
            XposedHelpers.findAndHookMethod(mHookClass, "handleSetTileVisibility", View.class, int.class, XC_MethodReplacement.DO_NOTHING);
        } catch (NoSuchMethodError e) { //LOS
            try {
                XposedHelpers.findAndHookMethod(mSecondHookClass, "handleSetTileVisibility", View.class, int.class, XC_MethodReplacement.DO_NOTHING);
            } catch (NoSuchMethodError ignore) {}
        }
        Class<?> classRecord = XposedHelpers.findClass(getSecondHookClass() + "$Record", classLoader);
        mSetDetailRecord = XposedHelpers.findMethodExact(mSecondHookClass, "setDetailRecord", classRecord);
    }

    protected void addDivider() {
        mDivider = LayoutInflater.from(mContext).inflate(ResourceUtils.getInstance(mContext).getLayout(R.layout.qs_divider), mQsPanel, false);
        mDivider.setBackgroundColor(ColorUtils.applyAlpha(mDivider.getAlpha(), getColorForState(mContext)));
        mQsPanel.addView(mDivider);
    }

    public static int getColorForState(Context context) {
        /*switch (state) {
            case Tile.STATE_UNAVAILABLE:
                return Utils.getDisabled(context,
                        ColorUtils.getColorAttr(context, android.R.attr.colorForeground));
            case Tile.STATE_INACTIVE:
                return ColorUtils.getColorAttr(context, android.R.attr.textColorHint);
            case Tile.STATE_ACTIVE:*/
                return ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary);
            /*default:
                Log.e("QSTile", "Invalid state " + state);
                return 0;
        }*/
    }

    private void hookSetGridContentVisibility() {
        XC_MethodReplacement setGridContentVisibility = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                boolean visible = (boolean) param.args[0];
                ViewGroup qsPanel = (ViewGroup) param.thisObject;
                int newVis = visible ? VISIBLE : INVISIBLE;
                mQsPanel.setVisibility(newVis);
                if (XposedHelpers.getBooleanField(qsPanel, "mGridContentVisible") != visible) {
                    MetricsLogger.visibility(mContext, MetricsLogger.QS_PANEL, newVis);
                }
                XposedHelpers.setBooleanField(qsPanel, "mGridContentVisible", visible);
                return null;
            }
        };
        try {
            XposedHelpers.findAndHookMethod(mHookClass, "setGridContentVisibility", boolean.class, setGridContentVisibility);
        } catch (NoSuchMethodError e) {
            try { //LOS
                XposedHelpers.findAndHookMethod(mSecondHookClass, "setGridContentVisibility", boolean.class, setGridContentVisibility);
            } catch (NoSuchMethodError ignore) {}
        }
    }

    private void hookAddTile() {
        XC_MethodHook addTile = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ArrayList mRecords = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                ((View) XposedHelpers.getObjectField(mRecords.get(mRecords.size() - 1), "tileView")).setVisibility(VISIBLE);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(mHookClass, "addTile", Classes.SystemUI.QSTile, addTile);
        } catch (NoSuchMethodError e) { //LOS
            XposedHelpers.findAndHookMethod(mSecondHookClass, "addTile", Classes.SystemUI.QSTile, addTile);
        }
    }


    protected void hookConstructor() {
        XposedHelpers.findAndHookConstructor(mHookClass, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mQsPanel = (ViewGroup) param.thisObject;
                mContext = mQsPanel.getContext();
                mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessView");
                mFooter = XposedHelpers.getObjectField(param.thisObject, "mFooter");
                mDetail = (View) XposedHelpers.getObjectField(param.thisObject, "mDetail");
                setupTileLayout();
                addDivider();
            }
        });
    }

    private void hookUpdateResources() {
        XposedHelpers.findAndHookMethod(mHookClass, "updateResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mTileLayout != null) {
                    if (mTileLayout.updateResources())
                        StatusBarHeaderHooks.postSetupAnimators();
                    try {
                        mTileLayout.setColumnCount(XposedHelpers.getIntField(param.thisObject, "mColumns"));
                    } catch (Throwable ignore) {
                    }
                }
            }
        });
    }

    private void hookSetTiles() {
        XposedHelpers.findAndHookMethod(mHookClass, "setTiles", Collection.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> mRecords = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                for (Object record : mRecords) {
                    mTileLayout.removeTile(record);
                }
                mTileLayout.removeAll();
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> mRecords = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                for (Object record : mRecords) {
                    mTileLayout.addTile(record);
                }
            }
        });
    }

    private void hookSetExpanded() {
        XposedHelpers.findAndHookMethod(mHookClass, "setExpanded", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                boolean expanded = (boolean) param.args[0];
                if (mExpanded == expanded) return;
                mExpanded = expanded;
                if (!mExpanded) {
                    mTileLayout.setCurrentItem(0, false);
                }
            }
        });
    }

    public QSDetail getQSDetail() {
        return mQSDetail;
    }

    private void hookFireScanStateChanged() {
        if (!ConfigUtils.qs().fix_header_space) return;
        XC_MethodReplacement fireScanStateChanged = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                if (mQSDetail != null)
                    mQSDetail.onScanStateChanged((boolean) param.args[0]);
                return null;
            }
        };
        try {
            XposedHelpers.findAndHookMethod(mHookClass, "fireScanStateChanged", boolean.class, fireScanStateChanged);
        } catch (NoSuchMethodError e) { //apparently on some CM ROMs it's in QSPanel
            try {
                XposedHelpers.findAndHookMethod(mSecondHookClass, "fireScanStateChanged", boolean.class, fireScanStateChanged);
            } catch (Throwable ignore) {}
        }
    }

    void setupTileLayout() {
        mTileLayout = new PagedTileLayout(mContext, null);
        mQsPanel.addView(mTileLayout);
    }

    public View getDivider() {
        return mDivider;
    }

    public View getPageIndicator() {
        return mTileLayout.getDecorGroup();
    }


    private void hookOnMeasure() {
        XposedHelpers.findAndHookMethod(mHookClass, "onMeasure", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ArrayList records = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                for (Object record : records) {
                    Object tileView = XposedHelpers.getObjectField(record, "tileView");
                    Object tile = XposedHelpers.getObjectField(record, "tile");
                    boolean supportsDualTargets;
                    boolean changed;
                    try {
                        supportsDualTargets = (boolean) XposedHelpers.callMethod(tile, "supportsDualTargets");
                        changed = (boolean) XposedHelpers.callMethod(tileView, "setDual", supportsDualTargets);
                    } catch (NoSuchMethodError e) { //LOS
                        supportsDualTargets = (boolean) XposedHelpers.callMethod(tile, "hasDualTargetsDetails");
                        changed = (boolean) XposedHelpers.callMethod(tileView, "setDual", supportsDualTargets, supportsDualTargets);
                    }
                    if (changed) {
                        XposedHelpers.callMethod(tileView, "handleStateChanged", XposedHelpers.callMethod(tile, "getState"));
                    }
                }
                onMeasure((int) param.args[0], (int) param.args[1]);
                return null;
            }
        });
    }

    private void hookOnLayout() {
        XposedHelpers.findAndHookMethod(mHookClass, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                onLayout();
                return null;
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = View.MeasureSpec.getSize(widthMeasureSpec);
        final int brightnessHeight = ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.brightness_view_height);
        final int brightnessBottom = brightnessHeight + ((mBrightnessView.getMeasuredHeight() - brightnessHeight) / 2);
        mBrightnessView.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);
        mTileLayout.measure(exactly(width), 200);

        View footerView = (View) XposedHelpers.callMethod(mFooter, "getView");
        footerView.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);

        int h = brightnessBottom + mTileLayout.getMeasuredHeight() + mDivider.getHeight()/* + mPanelPaddingBottom*/;

        if ((boolean) XposedHelpers.callMethod(mFooter, "hasFooter")) {
            h += footerView.getMeasuredHeight();
        }
        if (!mHookedGetGridHeight) {
            try {
                XposedHelpers.setObjectField(mQsPanel, "mGridHeight", h);
            } catch (Throwable t) {
                try {
                    XposedHelpers.findAndHookMethod(mQsPanel.getClass(), "getGridHeight", getGridHeightHook);
                } catch (Throwable ignore) {
                    XposedHook.logW(TAG, "QSPanel#getGridHeight() doesn't exist!");
                }
                mHookedGetGridHeight = true;
            }
        }
        // Used to clip header too
        mDetail.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);

        if (mDetail.getMeasuredHeight() < h) {
            mDetail.measure(exactly(width), exactly(h));
        }
        if (isShowingDetail() && !isClosingDetail() && isExpanded()) {
            h = mDetail.getMeasuredHeight();
        }

        ReflectionUtils.invoke(Methods.Android.View.setMeasuredDimension, mQsPanel, width, h);
    }

    protected void onLayout() {
        final int w = mQsPanel.getWidth();
        final int brightnessHeight = ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.brightness_view_height);
        final int brightnessBottom = brightnessHeight + ((mBrightnessView.getMeasuredHeight() - brightnessHeight) / 2);
        final int dividerHeight = mDivider.getLayoutParams().height;

        mBrightnessView.layout(0, 0, w, mBrightnessView.getMeasuredHeight());

        int viewPagerBottom = brightnessBottom + mTileLayout.getMeasuredHeight();
        // view pager laid out from top of brightness view to bottom to page through settings
        mTileLayout.layout(0, brightnessBottom, w, viewPagerBottom);

        mDetail.layout(0, 0, w, mDetail.getMeasuredHeight());
        mDivider.layout(0, viewPagerBottom, w, viewPagerBottom + dividerHeight);

        if ((boolean) XposedHelpers.callMethod(mFooter, "hasFooter")) {
            View footer = (View) XposedHelpers.callMethod(mFooter, "getView");
            footer.layout(0, mQsPanel.getMeasuredHeight() - footer.getMeasuredHeight(),
                    footer.getMeasuredWidth(), mQsPanel.getMeasuredHeight());
        }
        if (ConfigUtils.L1 && !isShowingDetail() && !isClosingDetail()) {
            mBrightnessView.bringToFront();
        }
    }

    private boolean isShowingDetail() {
        return XposedHelpers.getObjectField(mQsPanel, "mDetailRecord") != null;
    }

    private boolean isClosingDetail() {
        return XposedHelpers.getBooleanField(mQsPanel, "mClosingDetail");
    }

    private boolean isExpanded() {
        return XposedHelpers.getBooleanField(mQsPanel, "mExpanded");
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
    }

    protected String getHookClass() {
        return Classes.SystemUI.QSPanel.getName();
    }

    protected String getSecondHookClass() {
        return getHookClass();
    }

    public PagedTileLayout getTileLayout() {
        return mTileLayout;
    }

    public View getFooter() {
        return (View)XposedHelpers.callMethod(XposedHelpers.getObjectField(mQsPanel, "mFooter"), "getView");
    }

    public int getGridHeight() {
        return mQsPanel.getMeasuredHeight();
    }

    public View getBrightnessView() {
        return mBrightnessView;
    }

    QSDetail setupQsDetail(ViewGroup panel, ViewGroup header, View footer) {
        mQSDetail = new QSDetail(panel.getContext(), panel, header, footer, mSetDetailRecord);
        return mQSDetail;
    }

    public interface QSTileLayout {
        void addTile(Object tile);

        void removeTile(Object tile);

        int getOffsetTop(Object tile);

        boolean updateResources();
    }
}
