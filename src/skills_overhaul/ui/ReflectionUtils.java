package skills_overhaul.ui;

import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Minimal reflection helpers for navigating Starsector's core UI tree.
 *
 * Starsector's script classloader blocks direct references to
 * java.lang.reflect.Method / Field / Array for JAR classes it tracks.
 * This utility avoids all such references in its bytecode by:
 *
 *   1. Loading Method / Field Class objects via the bootstrap classloader.
 *   2. Using MethodHandles (loaded via lookup()) to call operations on them.
 *   3. Calling Class.getDeclaredMethods/Fields directly and iterating the
 *      resulting arrays with plain JVM array ops (cast to Object[], use .length).
 *
 * Mirrors the approach used by Second-in-Command (Lukas04 / Starficz).
 */
public final class ReflectionUtils {

    private ReflectionUtils() {}

    /* ── bootstrap-loaded Method / Field classes & handles ──────────────── */

    private static final Class<?> METHOD_CLS;
    private static final Class<?> FIELD_CLS;

    private static final MethodHandle METHOD_INVOKE;
    private static final MethodHandle METHOD_GET_NAME;
    private static final MethodHandle METHOD_GET_PARAM_COUNT;
    private static final MethodHandle METHOD_SET_ACCESSIBLE;

    private static final MethodHandle FIELD_GET;
    private static final MethodHandle FIELD_SET_ACCESSIBLE;
    private static final MethodHandle FIELD_GET_NAME;
    private static final MethodHandle FIELD_GET_TYPE;

    static {
        try {
            ClassLoader boot = String.class.getClassLoader();

            METHOD_CLS = Class.forName("java.lang.reflect.Method", false, boot);
            FIELD_CLS  = Class.forName("java.lang.reflect.Field",  false, boot);

            MethodHandles.Lookup pub = MethodHandles.lookup();

            METHOD_INVOKE = pub.findVirtual(METHOD_CLS, "invoke",
                MethodType.methodType(Object.class, Object.class, Object[].class));
            METHOD_GET_NAME = pub.findVirtual(METHOD_CLS, "getName",
                MethodType.methodType(String.class));
            METHOD_GET_PARAM_COUNT = pub.findVirtual(METHOD_CLS, "getParameterCount",
                MethodType.methodType(int.class));
            METHOD_SET_ACCESSIBLE = pub.findVirtual(METHOD_CLS, "setAccessible",
                MethodType.methodType(void.class, boolean.class));

            FIELD_GET = pub.findVirtual(FIELD_CLS, "get",
                MethodType.methodType(Object.class, Object.class));
            FIELD_SET_ACCESSIBLE = pub.findVirtual(FIELD_CLS, "setAccessible",
                MethodType.methodType(void.class, boolean.class));
            FIELD_GET_NAME = pub.findVirtual(FIELD_CLS, "getName",
                MethodType.methodType(String.class));
            FIELD_GET_TYPE = pub.findVirtual(FIELD_CLS, "getType",
                MethodType.methodType(Class.class));

        } catch (Throwable t) {
            throw new RuntimeException("ReflectionUtils init failed", t);
        }
    }

    private static final HashMap<Class<?>, java.util.HashSet<String>> methodNameCache = new HashMap<>();

    /* ── public API ─────────────────────────────────────────────────────── */

    /** Calls a zero-arg method on target by name; returns null on any failure. */
    public static Object invoke(String methodName, Object target) {
        if (target == null) return null;
        try {
            Object method = findMethod(target.getClass(), methodName, 0);
            if (method == null) {
                com.fs.starfarer.api.Global.getLogger(ReflectionUtils.class).warn(
                    "[ReflectionUtils] method not found: " + methodName
                    + " on " + target.getClass().getName());
                return null;
            }
            METHOD_SET_ACCESSIBLE.invoke(method, true);
            return METHOD_INVOKE.invoke(method, target, new Object[]{});
        } catch (Throwable t) {
            com.fs.starfarer.api.Global.getLogger(ReflectionUtils.class).warn(
                "[ReflectionUtils] invoke(" + methodName + ") on "
                + target.getClass().getName() + " failed", t);
            return null;
        }
    }

    /** True iff target's class (or superclasses) declares a method of this name. */
    public static boolean hasMethodOfName(String methodName, Object target) {
        if (target == null) return false;
        Class<?> clazz = target.getClass();
        java.util.HashSet<String> cache = methodNameCache.get(clazz);
        if (cache == null) {
            cache = new java.util.HashSet<>();
            try {
                Object raw = clazz.getDeclaredMethods();
                Object[] methods = (Object[]) raw;
                for (int i = 0; i < methods.length; i++) {
                    String name = (String) METHOD_GET_NAME.invoke(methods[i]);
                    cache.add(name);
                }
            } catch (Throwable ignored) {}
            methodNameCache.put(clazz, cache);
        }
        return cache.contains(methodName);
    }

    /** Returns children of a UIPanelAPI by reflecting its inner component list. */
    @SuppressWarnings("unchecked")
    public static List<Object> getChildrenCopy(UIPanelAPI panel) {
        if (panel == null) return Collections.emptyList();
        try {
            Object method = findMethod(panel.getClass(), "getChildrenCopy", 0);
            if (method != null) {
                METHOD_SET_ACCESSIBLE.invoke(method, true);
                Object r = METHOD_INVOKE.invoke(method, panel, new Object[]{});
                if (r instanceof List) return new ArrayList<>((List<Object>) r);
            }
            // Fallback: find any List field
            Object raw = panel.getClass().getDeclaredFields();
            Object[] fields = (Object[]) raw;
            for (int i = 0; i < fields.length; i++) {
                FIELD_SET_ACCESSIBLE.invoke(fields[i], true);
                Class<?> type = (Class<?>) FIELD_GET_TYPE.invoke(fields[i]);
                if (List.class.isAssignableFrom(type)) {
                    Object r = FIELD_GET.invoke(fields[i], panel);
                    if (r instanceof List) return new ArrayList<>((List<Object>) r);
                }
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    /** Returns the parent UIPanelAPI of a child component, or null. */
    public static UIPanelAPI getParent(Object child) {
        Object r = invoke("getParent", child);
        return r instanceof UIPanelAPI ? (UIPanelAPI) r : null;
    }

    /* ── helpers ────────────────────────────────────────────────────────── */

    /** Find a zero-arg method by name on cls or superclasses.
     *  Returns the raw Method object as Object. */
    private static Object findMethod(Class<?> cls, String name, int arity) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Object raw = c.getDeclaredMethods();
                Object[] methods = (Object[]) raw;
                for (int i = 0; i < methods.length; i++) {
                    String mName = (String) METHOD_GET_NAME.invoke(methods[i]);
                    if (!mName.equals(name)) continue;
                    if (arity >= 0) {
                        int count = (int) METHOD_GET_PARAM_COUNT.invoke(methods[i]);
                        if (count != arity) continue;
                    }
                    return methods[i];
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
