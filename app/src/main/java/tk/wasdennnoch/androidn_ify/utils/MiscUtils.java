package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class MiscUtils {

    public static boolean isGBInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(ConfigUtils.M ? "com.ceco.marshmallow.gravitybox" : "com.ceco.lollipop.gravitybox", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String readInputStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    public static JSONArray checkValidJSONArray(String json) throws JSONException {
        return new JSONArray(json.replace(" ", ""));
    }

    /**
     * Returns the method that is overridden by the given method.
     * It returns {@code null} if the method doesn't override another method or if that method is
     * abstract, i.e. if this is the first implementation in the hierarchy.
     */
	public static Method getOverriddenMethod(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
            return null;
        }

        String name = method.getName();
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> clazz = method.getDeclaringClass().getSuperclass();
        while (clazz != null) {
            try {
                Method superMethod = clazz.getDeclaredMethod(name, parameters);
                modifiers = superMethod.getModifiers();
                if (!Modifier.isPrivate(modifiers) && !Modifier.isAbstract(modifiers)) {
                    return superMethod;
                } else {
                    return null;
                }
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Returns all methods which this class overrides.
     */
	public static Set<Method> getOverriddenMethods(Class<?> clazz) {
        Set<Method> methods = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            Method overridden = getOverriddenMethod(method);
            if (overridden != null) {
                methods.add(overridden);
            }
        }
        return methods;
    }
}