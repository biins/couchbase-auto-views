package org.biins.cauchbase;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Martin Janys
 */
public class AnnotationUtils {

    public static Map<Class<?>, List<Annotation>> annotationsByTypes(Annotation ... annotations) {
        Map<Class<?>, List<Annotation>> annotationMap = new HashMap<Class<?>, List<Annotation>>();
        for (Annotation annotation : annotations) {
            List<Annotation> annotationList = annotationMap.get(annotation.getClass());
            if (annotationList == null) {
                annotationList = new ArrayList<>();
            }
            annotationList.add(annotation);
            annotationMap.put(extractClass(annotation.getClass()), annotationList);
        }
        return annotationMap;
    }

    private static Class<?> extractClass(Class<? extends Annotation> annotationClass) {
        return annotationClass.getInterfaces()[0];
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> annotationsByType(Class<T> type, Annotation ... annotations) {
        Map<Class<?>, List<Annotation>> annotationsByTypes = annotationsByTypes(annotations);
        if (annotationsByTypes.containsKey(type)) {
            return (List<T>) annotationsByTypes.get(type);
        }
        else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> get(Map<Class<?>, List<Annotation>> annotationMap, Class<T> type) {
        if (annotationMap.containsKey(type)) {
            return (List<T>) annotationMap.get(type);
        }
        else {
            return Collections.emptyList();
        }
    }
}
