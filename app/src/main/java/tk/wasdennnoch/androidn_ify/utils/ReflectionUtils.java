package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import tk.wasdennnoch.androidn_ify.XposedHook;

public class ReflectionUtils {

    private static final String TAG = ReflectionUtils.class.getSimpleName();

    public static <T> T get(Field field, Object object) {
        try {
            return (T) field.get(object);
        } catch (IllegalAccessException e) {
            XposedHook.logE(TAG, "Error getting value from field " + field.getName(), e);
            return null;
        }
    }

    public static boolean getBoolean(Field field, Object object) {
        return ReflectionUtils.<Boolean>get(field, object);
    }

    public static int getInt(Field field, Object object) {
        return ReflectionUtils.<Integer>get(field, object);
    }

    public static float getFloat(Field field, Object object) {
        return ReflectionUtils.<Float>get(field, object);
    }

    public static <T> void set(Field field, Object object, T value) {
        try {
            field.set(object, value);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error setting value of field: " + field.getName(), t);
        }
    }

    public static Object invoke(Method method, Object object, Object... args) {
        try {
            return method.invoke(object, args);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error invoking method " + method.getName(), t);
            return null;
        }
    }

}
