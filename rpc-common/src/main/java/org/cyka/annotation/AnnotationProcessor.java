package org.cyka.annotation;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

public class AnnotationProcessor {

  private final Reflections reflections;

  /**
   * search annotated class set
   *
   * @param annotationClass the annotation to be searched
   * @return set
   */
  public Set<Class<?>> getAllAnnotatedClass(Class<? extends Annotation> annotationClass) {
    return reflections.getTypesAnnotatedWith(annotationClass);
  }

  public AnnotationProcessor(String basePackage) {
    this.reflections = new Reflections(basePackage);
  }
}
