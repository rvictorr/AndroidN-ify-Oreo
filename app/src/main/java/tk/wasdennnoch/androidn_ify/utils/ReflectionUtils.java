package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tk.wasdennnoch.androidn_ify.XposedHook;

public class ReflectionUtils {

    private static final String TAG = ReflectionUtils.class.getSimpleName();

    public static <T> T get(Field field, Object object) {
        try {
            return (T) field.get(object);
        } catch (IllegalAccessException e) {
//            XposedHook.logE(TAG, "Error getting value from field " + field.getName(), e);
            return null;
        } catch (IllegalArgumentException e) { //TODO: maybe find a better way of handling this with less of a performance impact?
//            XposedHook.logE(TAG, "Error getting value from field " + field.getName() + ": illegal arguments; ", e);
            return null;
        }
    }

    public static boolean getBoolean(Field field, Object object) {
        return ReflectionUtils.<Boolean>get(field, object);
    }

    public static int getInt(Field field, Object object) {
        return ReflectionUtils.<Integer>get(field, object);
    }

    public static long getLong(Field field, Object object) {
        return ReflectionUtils.<Long>get(field, object);
    }

    public static float getFloat(Field field, Object object) {
        return ReflectionUtils.<Float>get(field, object);
    }

    public static double getDouble(Field field, Object object) {
        return ReflectionUtils.<Double>get(field, object);
    }

    public static <T> void set(Field field, Object object, T value) {
        try {
            field.set(object, value);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error setting value of field " + field.getName(), t);
        }
    }

    public static <T> T invoke(Method method, Object object, Object... args) {
        try {
            return (T) method.invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            XposedHook.logE(TAG, "Error invoking method " + method.getName(), e.getCause());
//            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            //XposedHook.logD(TAG, "Error invoking method " + method.getName() + ": " + e);
            throw new UncheckedIllegalArgumentException();
        }
    }

    public static <T> T newInstance(Constructor constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            XposedHook.logE(TAG, "Error instantiating class " + constructor.getDeclaringClass().getName(), e.getCause());
//            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            //XposedHook.logD(TAG, "Error invoking method " + method.getName() + ": " + e);
//            e.printStackTrace();
            throw new UncheckedIllegalArgumentException();
        }
    }

    public static class UncheckedIllegalArgumentException extends RuntimeException { }

}
