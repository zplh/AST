package com.example.mylibrary;


import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;


import com.example.annotation.MyAnnotation;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.example.annotation.MyAnnotation")
public class MyAbstractProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Elements mElementsUtil;
    private Filer mFilerUtil;
    private Trees mtrees;
    private TreeMaker mMaker;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        mMessager = processingEnv.getMessager();
        mElementsUtil = processingEnv.getElementUtils();
        mFilerUtil = processingEnv.getFiler();
        mtrees = Trees.instance(processingEnv);
        mMaker = TreeMaker.instance(((JavacProcessingEnvironment)processingEnv).getContext());
        mMessager.printMessage(Diagnostic.Kind.NOTE,"zhangpanpanpan");
    }
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element>  set1 = roundEnvironment.getElementsAnnotatedWith(MyAnnotation.class);
        for (Element  s : set1) {
            if (s.getKind() == ElementKind.METHOD) {
                mMessager.printMessage(Diagnostic.Kind.NOTE,"namebefore="+s.getSimpleName());
                JCTree tree = (JCTree) mtrees.getTree(s);
                TreeTranslator visitor = new Inliner();
                tree.accept(visitor);
                mMessager.printMessage(Diagnostic.Kind.NOTE,"name aftore="+((JCTree.JCMethodDecl)tree).getName().toString()+"   "+tree.hashCode());
            }
        }

        return true;
    }

    private class Inliner extends TreeTranslator {
        public void visitMethodDef(JCTree.JCMethodDecl tree) {


            if (tree.getName().toString().equals("getAge")) {
                Context context = new Context();
                Names names = new Names(context);
                tree.name = names.fromString("getcapture");
                super.visitMethodDef(tree);

//             tree = mMaker.MethodDef(
//                        tree.getModifiers(),
//                        names.fromString("getcapture"),
//                        tree.restype,
//                        tree.getTypeParameters(),
//                        tree.getParameters(),
//                        tree.getThrows(),
//                        tree.getBody(),
//                        tree.defaultValue);

                 this.result = tree;
                mMessager.printMessage(Diagnostic.Kind.NOTE, "haha=" + tree.getName().toString()+"   "+tree.hashCode());
            }
        }
    }
}
