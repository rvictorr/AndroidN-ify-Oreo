package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.AutoSizingList;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class QSDetailItemsHelper {

    private static final String TAG = "QSDetailHooks";
    
    private Adapter mAdapter = new Adapter();
    private Object mCallback;
    private Context mContext;
    private LinearLayout mEmpty;
    private AutoSizingList mItemList;
    private Object[] mItems;
    private boolean mItemsVisible = true;
    private FrameLayout mQSDetailItems;

    private QSDetailItemsHelper(Object qsDetailItems) {
        mQSDetailItems = (FrameLayout) qsDetailItems;
        XposedHelpers.setAdditionalInstanceField(mQSDetailItems, "mHelper", this);
    }

    public static QSDetailItemsHelper getInstance(Object qsDetailItems) {
        QSDetailItemsHelper helper = (QSDetailItemsHelper) XposedHelpers.getAdditionalInstanceField(qsDetailItems, "mHelper");
        return helper != null ? helper : new QSDetailItemsHelper(qsDetailItems);
    }

    public void onFinishInflate() {
        mContext = (Context) XposedHelpers.getObjectField(mQSDetailItems, "mContext");
        ResourceUtils res = ResourceUtils.getInstance(mContext);

        mItemList = new AutoSizingList(mContext, null);
        mItemList.setId(android.R.id.list);
        mItemList.setItemSize(res.getDimensionPixelSize(R.dimen.qs_detail_item_height));
        mItemList.setOrientation(LinearLayout.VERTICAL);
        mItemList.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mEmpty = (LinearLayout) mQSDetailItems.findViewById(android.R.id.empty);
        mEmpty.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        mEmpty.setGravity(Gravity.CENTER);

        mQSDetailItems.setPaddingRelative(res.getDimensionPixelSize(R.dimen.qs_detail_padding_start), 0, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, res.getResources().getDisplayMetrics())), 0);
        mQSDetailItems.removeView(mQSDetailItems.findViewById(android.R.id.list));
        mQSDetailItems.addView(mItemList, 0);
        mQSDetailItems.removeView(mQSDetailItems.findViewById(res.getResources().getIdentifier("min_height_spacer", "id", XposedHook.PACKAGE_SYSTEMUI)));

        mItemList.setVisibility(GONE);
        mItemList.setAdapter(mAdapter);
    }

    public void handleSetItems(Object[] items) {
        final int itemCount = items != null ? items.length : 0;
        mEmpty.setVisibility(itemCount == 0 ? VISIBLE : GONE);
        mItemList.setVisibility(itemCount == 0 ? GONE : VISIBLE);
        mItems = items;
        mAdapter.notifyDataSetChanged();
    }

    public void handleSetItemsVisible(boolean visible) {
        if (mItemsVisible == visible) return;
        mItemsVisible = visible;
        for (int i = 0; i < mItemList.getChildCount(); i++) {
            mItemList.getChildAt(i).setVisibility(mItemsVisible ? VISIBLE : INVISIBLE);
        }
    }

    public void handleSetCallback(Object callback) {
        mCallback = callback;
    }

    private class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mItems != null ? mItems.length : 0;
        }

        @Override
        public Object getItem(int position) {
            return mItems[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final Object item = mItems[position];
            int icon = XposedHelpers.getIntField(item, "icon");
            final Drawable overlay = (Drawable) XposedHelpers.getObjectField(item, "overlay");
            CharSequence line1 = (CharSequence) XposedHelpers.getObjectField(item, "line1");
            CharSequence line2 = (CharSequence) XposedHelpers.getObjectField(item, "line2");
            boolean canDisconnect = XposedHelpers.getBooleanField(item, "canDisconnect");
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(mContext.getResources().getIdentifier("qs_detail_item", "layout", XposedHook.PACKAGE_SYSTEMUI), parent,
                        false);
            }
            view.setVisibility(mItemsVisible ? VISIBLE : INVISIBLE);
            final ImageView iv = (ImageView) view.findViewById(android.R.id.icon);

            iv.setImageResource(icon);
            iv.getOverlay().clear();
            if (overlay != null) {
                overlay.setBounds(0, 0, overlay.getIntrinsicWidth(),
                        overlay.getIntrinsicHeight());
                iv.getOverlay().add(overlay);
            }
            final TextView title = (TextView) view.findViewById(android.R.id.title);
            title.setText(line1);
            final TextView summary = (TextView) view.findViewById(android.R.id.summary);
            final boolean twoLines = !TextUtils.isEmpty(line2);
            title.setMaxLines(twoLines ? 1 : 2);
            summary.setVisibility(twoLines ? VISIBLE : GONE);
            summary.setText(twoLines ? line2 : null);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        XposedHelpers.callMethod(mCallback, "onDetailItemClick", item);
                    }
                }
            });
            final ImageView disconnect = (ImageView) view.findViewById(android.R.id.icon2);
            disconnect.setVisibility(canDisconnect ? VISIBLE : GONE);
            disconnect.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        XposedHelpers.callMethod(mCallback, "onDetailItemDisconnect", item);
                    }
                }
            });
            return view;
        }
    }
}
