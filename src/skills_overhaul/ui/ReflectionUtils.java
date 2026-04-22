package skills_overhaul.ui;

import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal reflection helpers for navigating Starsector's core UI tree.
 * Mirrors the relevant bits of Second-in-Command's ReflectionUtils
 * without pulling in Kotlin stdlib.
 *
 * Starsector's concrete UI classes (com.fs.starfarer.ui.*) are obfuscated
 * and not in the public API, so we navigate by method name + type shape.
 */
public final class ReflectionUtils {

    private ReflectionUtils() {}

    /** Calls a zero-arg method on target by name; returns null on any failure. */
    public static Object invoke(String methodName, Object target) {
        if (target == null) return null;
        try {
            Method m = findMethod(target.getClass(), methodName, 0);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /** True iff target's class (or superclasses) declares a method of this name. */
    public static boolean hasMethodOfName(String methodName, Object target) {
        if (target == null) return false;
        return findMethod(target.getClass(), methodName, -1) != null;
    }

    /**
     * Returns children of a UIPanelAPI by reflecting its inner component list.
     * Starsector's panel classes store children in a private field; we discover
     * it by searching for a List field.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getChildrenCopy(UIPanelAPI panel) {
        if (panel == null) return Collections.emptyList();
        try {
            Method m = findMethod(panel.getClass(), "getChildrenCopy", 0);
            if (m != null) {
                m.setAccessible(true);
                Object r = m.invoke(panel);
                if (r instanceof List) return new ArrayList<>((List<Object>) r);
            }
            // Fall back: find any List field
            for (Field f : allFields(panel.getClass())) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object r = f.get(panel);
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

    private static Method findMethod(Class<?> cls, String name, int arity) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (arity >= 0 && m.getParameterCount() != arity) continue;
                return m;
            }
        }
        return null;
    }

    private static List<Field> allFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) out.add(f);
        }
        return out;
    }
}
