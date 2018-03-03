package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;

import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ScrimView.*;
import static tk.wasdennnoch.androidn_ify.utils.Fields.SystemUI.ScrimController.*;

public class ScrimHelper {
    private static final String TAG = "ScrimHelper";

    private static ScrimHelper mScrimBehindHelper;

    private View mScrimView;
    private final Paint mPaint = new Paint();
    private int mLeftInset = 0;
    private boolean mHasExcludedArea;
    private Rect mExcludedRect = new Rect();
    public Runnable mChangeRunnable;


    private ScrimHelper(Object scrimView) {
        mScrimView = (View) scrimView;
        XposedHelpers.setAdditionalInstanceField(scrimView, "mScrimHelper", this);
    }

    public static ScrimHelper getInstance(Object scrimView) {
        ScrimHelper helper = (ScrimHelper) XposedHelpers.getAdditionalInstanceField(scrimView, "mScrimHelper");
        return helper != null ? helper : new ScrimHelper(scrimView);
    }

    public void onDraw(Canvas canvas) {
        boolean isEmpty = getBoolean(mIsEmpty, mScrimView);
        boolean drawAsSrc = getBoolean(mDrawAsSrc, mScrimView);
        float viewAlpha = getFloat(mViewAlpha, mScrimView);

        if (drawAsSrc || (!isEmpty && viewAlpha > 0f)) {
            PorterDuff.Mode mode = drawAsSrc ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER;
            int color = getScrimColorWithAlpha();
            if (!mHasExcludedArea) {
                canvas.drawColor(color, mode);
            } else {
                mPaint.setColor(color);
                if (mExcludedRect.top > 0) {
                    canvas.drawRect(0, 0, mScrimView.getWidth(), mExcludedRect.top, mPaint);
                }
                if (mExcludedRect.left + mLeftInset > 0) {
                    canvas.drawRect(0, mExcludedRect.top, mExcludedRect.left + mLeftInset,
                            mExcludedRect.bottom, mPaint);
                }
                if (mExcludedRect.right + mLeftInset < mScrimView.getWidth()) {
                    canvas.drawRect(mExcludedRect.right + mLeftInset,
                            mExcludedRect.top,
                            mScrimView.getWidth(),
                            mExcludedRect.bottom,
                            mPaint);
                }
                if (mExcludedRect.bottom < mScrimView.getHeight()) {
                    canvas.drawRect(0, mExcludedRect.bottom, mScrimView.getWidth(), mScrimView.getHeight(), mPaint);
                }
            }
        }
    }

    public int getScrimColorWithAlpha() {
        float viewAlpha = getFloat(mViewAlpha, mScrimView);
        int color = getInt(mScrimColor, mScrimView);
        color = Color.argb((int) (Color.alpha(color) * viewAlpha), Color.red(color),
                Color.green(color), Color.blue(color));
        return color;
    }
    public void setDrawAsSrc(boolean asSrc) {
        mPaint.setXfermode(new PorterDuffXfermode(asSrc ? PorterDuff.Mode.SRC
                : PorterDuff.Mode.SRC_OVER));
    }

    public void setExcludedArea(Rect area) {
        if (area == null) {
            mHasExcludedArea = false;
            mScrimView.invalidate();
            return;
        }
        int left = Math.max(area.left, 0);
        int top = Math.max(area.top, 0);
        int right = Math.min(area.right, mScrimView.getWidth());
        int bottom = Math.min(area.bottom, mScrimView.getHeight());
        mExcludedRect.set(left, top, right, bottom);
        mHasExcludedArea = left < right && top < bottom;
        mScrimView.invalidate();
    }

    public void setChangeRunnable(Runnable changeRunnable) {
        mChangeRunnable = changeRunnable;
    }

    public void setLeftInset(int leftInset) {
        if (mLeftInset != leftInset) {
            mLeftInset = leftInset;
            if (mHasExcludedArea) {
                mScrimView.invalidate();
            }
        }
    }

    /* ScrimController stuff */

    public static void setScrimBehind(Object scrimController) {
        Object scrimBehind = get(mScrimBehind, scrimController);
        mScrimBehindHelper = ScrimHelper.getInstance(scrimBehind);
    }

    public static int getScrimBehindColor() {
        return mScrimBehindHelper.getScrimColorWithAlpha();
    }

    public static void setExcludedBackgroundArea(Rect area) {
        mScrimBehindHelper.setExcludedArea(area);
    }

    public static void setScrimBehindChangeRunnable(Runnable changeRunnable) {
        mScrimBehindHelper.setChangeRunnable(changeRunnable);
    }

    public static void setDrawBehindAsSrc(boolean asSrc) {
        mScrimBehindHelper.setDrawAsSrc(asSrc);
    }

    public static void setScrimBehindLeftInset(int inset) {
        mScrimBehindHelper.setLeftInset(inset);
    }

}