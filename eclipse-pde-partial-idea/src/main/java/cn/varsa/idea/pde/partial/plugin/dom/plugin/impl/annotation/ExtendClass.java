package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation;

import com.intellij.util.xml.ExtendClassImpl;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class ExtendClass extends ExtendClassImpl {
    private final String[] classes;

    public ExtendClass(String[] classes) {
        this.classes = classes;
    }

    @Override
    public String[] value() {
        return classes;
    }
}
