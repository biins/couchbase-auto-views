package org.biins.cauchbase;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Janys
 */
public class AnnotationUtils {

    public static Map<Class<?>, List<Annotation>> annotationsByTypes(Annotation ... annotations) {
        Map<Class<?>, List<Annotation>> annotationMap = new HashMap<Class<?>, List<Annotation>>();
        for (Annotation annotation : annotations) {
            List<Annotation> annotationList = annotationMap.get(annotation.getClass());
            if (annotationList == null) {
                annotationList = new ArrayList<Annotation>();
            }
            annotationList.add(annotation);
            annotationMap.put(annotation.getClass(), annotationList);
        }
        return annotationMap;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> annotationsByType(Class<T> type, Annotation ... annotations) {
        return (List<T>) annotationsByTypes(annotations).get(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> get(Map<Class<?>, List<Annotation>> annotationMap, Class<T> type) {
        return (List<T>) annotationMap.get(type);
    }
}
