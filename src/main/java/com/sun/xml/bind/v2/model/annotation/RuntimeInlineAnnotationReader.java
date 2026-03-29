package com.sun.xml.bind.v2.model.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class RuntimeInlineAnnotationReader extends AbstractInlineAnnotationReaderImpl<Type, Class, Field, Method> implements RuntimeAnnotationReader {
    private final Map<Class<? extends Annotation>, Map<String, Annotation>> packageCache = new HashMap<>();

    @Override
    public <A extends Annotation> A getFieldAnnotation(Class<A> annotation, Field field, Locatable srcPos) {
        return LocatableAnnotation.create(field.getAnnotation(annotation), srcPos);
    }

    @Override
    public boolean hasFieldAnnotation(Class<? extends Annotation> annotationType, Field field) {
        return field.isAnnotationPresent(annotationType);
    }

    @Override
    public boolean hasClassAnnotation(Class clazz, Class<? extends Annotation> annotationType) {
        return clazz.isAnnotationPresent(annotationType);
    }

    @Override
    public Annotation[] getAllFieldAnnotations(Field field, Locatable srcPos) {
        Annotation[] annotations = field.getAnnotations();
        for (int i = 0; i < annotations.length; ++i) {
            annotations[i] = LocatableAnnotation.create(annotations[i], srcPos);
        }
        return annotations;
    }

    @Override
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotation, Method method, Locatable srcPos) {
        return LocatableAnnotation.create(method.getAnnotation(annotation), srcPos);
    }

    @Override
    public boolean hasMethodAnnotation(Class<? extends Annotation> annotation, Method method) {
        return method.isAnnotationPresent(annotation);
    }

    @Override
    public Annotation[] getAllMethodAnnotations(Method method, Locatable srcPos) {
        Annotation[] annotations = method.getAnnotations();
        for (int i = 0; i < annotations.length; ++i) {
            annotations[i] = LocatableAnnotation.create(annotations[i], srcPos);
        }
        return annotations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getMethodParameterAnnotation(Class<A> annotation, Method method, int paramIndex, Locatable srcPos) {
        for (Annotation candidate : method.getParameterAnnotations()[paramIndex]) {
            if (candidate.annotationType() == annotation) {
                return (A) LocatableAnnotation.create(candidate, srcPos);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getClassAnnotation(Class<A> annotation, Class clazz, Locatable srcPos) {
        return LocatableAnnotation.create((A) clazz.getAnnotation(annotation), srcPos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getPackageAnnotation(Class<A> annotation, Class clazz, Locatable srcPos) {
        Package packageInfo = clazz.getPackage();
        if (packageInfo == null) {
            return null;
        }

        String packageName = packageInfo.getName();
        Map<String, Annotation> cache = this.packageCache.computeIfAbsent(annotation, key -> new HashMap<>());
        if (cache.containsKey(packageName)) {
            return (A) cache.get(packageName);
        }

        A resolved = LocatableAnnotation.create(this.readPackageAnnotation(annotation, clazz, packageInfo, packageName), srcPos);
        cache.put(packageName, resolved);
        return resolved;
    }

    private <A extends Annotation> A readPackageAnnotation(Class<A> annotation, Class clazz, Package packageInfo, String packageName) {
        A resolved = this.readPackageInfoClassAnnotation(annotation, clazz.getClassLoader(), packageName);
        if (resolved != null) {
            return resolved;
        }

        resolved = this.readPackageInfoClassAnnotation(annotation, Thread.currentThread().getContextClassLoader(), packageName);
        if (resolved != null) {
            return resolved;
        }

        try {
            return packageInfo.getAnnotation(annotation);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private <A extends Annotation> A readPackageInfoClassAnnotation(Class<A> annotation, ClassLoader loader, String packageName) {
        String className = packageName.isEmpty() ? "package-info" : packageName + ".package-info";
        if (loader == null) {
            return null;
        }

        try {
            Class<?> packageInfoClass = Class.forName(className, false, loader);
            return packageInfoClass.getAnnotation(annotation);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public Class getClassValue(Annotation annotation, String name) {
        try {
            return (Class) annotation.annotationType().getMethod(name).invoke(annotation);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InternalError(Messages.CLASS_NOT_FOUND.format(annotation.annotationType(), e.getMessage()));
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    @Override
    public Class[] getClassArrayValue(Annotation annotation, String name) {
        try {
            return (Class[]) annotation.annotationType().getMethod(name).invoke(annotation);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InternalError(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    @Override
    protected String fullName(Method method) {
        return method.getDeclaringClass().getName() + '#' + method.getName();
    }
}
