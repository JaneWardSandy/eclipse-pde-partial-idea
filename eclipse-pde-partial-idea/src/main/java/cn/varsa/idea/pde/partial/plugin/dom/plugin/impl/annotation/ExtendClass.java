package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation;

import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class ExtendClass implements com.intellij.util.xml.ExtendClass {
  private final String[] classes;

  public ExtendClass(String[] classes) {
    this.classes = classes;
  }

  @Override
  public String[] value() {
    return classes;
  }

  @Override
  public boolean instantiatable() {
    return false;
  }

  @Override
  public boolean canBeDecorator() {
    return false;
  }

  @Override
  public boolean allowEmpty() {
    return false;
  }

  @Override
  public boolean allowNonPublic() {
    return false;
  }

  @Override
  public boolean allowAbstract() {
    return true;
  }

  @Override
  public boolean allowInterface() {
    return true;
  }

  @Override
  public boolean allowEnum() {
    return true;
  }

  @Override
  public boolean jvmFormat() {
    return true;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return com.intellij.util.xml.ExtendClass.class;
  }
}
