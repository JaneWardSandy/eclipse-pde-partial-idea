package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation;

import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class Required implements com.intellij.util.xml.Required {
  public static final Required INSTANCE = new Required();

  @Override
  public boolean value() {
    return true;
  }

  @Override
  public boolean nonEmpty() {
    return true;
  }

  @Override
  public boolean identifier() {
    return false;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return com.intellij.util.xml.Required.class;
  }
}
