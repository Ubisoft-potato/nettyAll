package org.cyka.util;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

/** the rpc annotation process util */
public class RpcAnnotationProcessUtil {
  private static final Reflections reflections = new Reflections();

  public static Set<Class<?>> getAllAnnotatedClass(
      String basePath, Class<? extends Annotation> annotationClass) {
    return reflections.getTypesAnnotatedWith(annotationClass);
  }
}
