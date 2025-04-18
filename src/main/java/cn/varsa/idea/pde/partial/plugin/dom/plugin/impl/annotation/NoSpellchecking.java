package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation;

import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class NoSpellchecking implements com.intellij.spellchecker.xml.NoSpellchecking {
  public static final NoSpellchecking INSTANCE = new NoSpellchecking();

  @Override
  public Class<? extends Annotation> annotationType() {
    return com.intellij.spellchecker.xml.NoSpellchecking.class;
  }
}
