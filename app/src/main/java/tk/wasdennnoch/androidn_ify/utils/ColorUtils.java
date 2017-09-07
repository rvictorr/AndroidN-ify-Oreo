package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;

public class ColorUtils {

    public static int generateColor(Drawable drawable, int defaultColor) {
        Palette palette = Palette.from(convertToBitmap(drawable, 128, 128)).generate();
        int color = palette.getVibrantColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getLightVibrantColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getDarkMutedColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getLightMutedColor(defaultColor);
        return color;
    }

    private static Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);
        return bitmap;
    }

    @ColorInt
    public static int applyAlpha(float alpha, int inputColor) {
        alpha *= Color.alpha(inputColor);
        return Color.argb((int) (alpha), Color.red(inputColor), Color.green(inputColor),
                Color.blue(inputColor));
    }

    @ColorInt
    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        @ColorInt int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

}
