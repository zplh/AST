package com.example.mylibrary;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.sym.Literal;

import com.example.annotation.AST;
import com.example.annotation.BindView;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;


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
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;


@SupportedSourceVersion(SourceVersion.RELEASE_8)//java版本支持
@SupportedAnnotationTypes({"com.example.annotation.BindView"})//注意替换成你自己的注解名
public class MyASTProcessor extends AbstractProcessor {
    private Trees trees;
    private Messager mMessager;
    private HashMap<String, java.util.List<SourceType>> mCodes = new HashMap<String, java.util.List<SourceType>>();
    private HashMap<String,JCTree.JCMethodDecl> mTarget = new HashMap();
    ListBuffer<JCTree.JCStatement> mStatements = new ListBuffer<>();
    private TreeMaker mMaker;
    private Names mNames;
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        mMessager = env.getMessager();
        mMaker = TreeMaker.instance(((JavacProcessingEnvironment)processingEnv).getContext());
        Context context = new Context();
        mNames = new Names(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<Element> set1 = new LinkedHashSet<>();
            set1.addAll(roundEnv.getElementsAnnotatedWith(BindView.class));
            for (Element it : set1) {
                //处理字段的注入
                //获取变量类型
                String type = it.asType().toString();
                //获取变量名
                String fieldName = it.getSimpleName().toString();
                //id
                int resourceId = it.getAnnotation(BindView.class).value();
                TypeElement className = (TypeElement) it.getEnclosingElement();

                SourceType s = new SourceType(fieldName, resourceId, type);

                //将id和字段名关联存储
                if (mCodes.get(className.getQualifiedName().toString()) == null) {
                    java.util.List list = new ArrayList<>();
                    list.add(s);
                    mCodes.put(className.getQualifiedName().toString(), list);
                } else {
                    mCodes.get(className.getQualifiedName().toString()).add(s);
                }
            }
            Set<Element> set2 = new LinkedHashSet<>();
            set2.addAll(roundEnv.getElementsAnnotatedWith(Override.class));
            for (Element it : set2) {
                TypeElement typeElement = (TypeElement) it.getEnclosingElement();
                if (mCodes.keySet().contains(typeElement.getQualifiedName().toString())
                && it.getSimpleName().toString().equals("onCreate")) {
                    mTarget.put(typeElement.toString(), (JCTree.JCMethodDecl) trees.getTree(it));
                }
            }

            StringBuffer sb = new StringBuffer("hehe--");
            for (String m: mCodes.keySet()) {
                sb.append(m+"   ");
                for (int i =0; i< mCodes.get(m).size(); i++) {
                    sb.append(mCodes.get(m).get(i).toString()+"   ");
                }
            }
            for (String m: mTarget.keySet()) {
                sb.append(m+"   "+mTarget.get(m).getName());

            }
            mMessager.printMessage(Diagnostic.Kind.NOTE,sb);
        } else {
            mTarget.entrySet().stream()
                    .forEach(it -> {
                        if (mCodes.containsKey(it.getKey())) {
                            mCodes.get(it.getKey()).forEach(node -> {
                                JCTree.JCExpression jcExpression = memberAccess(node.mClassName);
                                mStatements.append(mMaker.Exec(mMaker.Assign(mMaker.Ident(mNames.fromString(node.mViewname)),
                                        mMaker.Apply(List.of(jcExpression),
                                                memberAccess("findViewById"),
                                                List.of(mMaker.Literal(node.mViewid))))));
                                mMessager.printMessage(Diagnostic.Kind.NOTE, mStatements.toString());
                            });
                            if (mStatements.size() > 0) {
                                List<JCTree.JCStatement> lst = mStatements.toList();
                                it.getValue().body.stats = it.getValue().body.stats.appendList(lst);
                            }
                        }
                    });
        }

        return false;
    }

    private JCTree.JCVariableDecl createVarDef(JCTree.JCModifiers modifiers, String name, JCTree.JCExpression varType, JCTree.JCExpression init) {
        return mMaker.VarDef(
                modifiers,
                //名字
                getNameFromString(name),
                //类型
                varType,
                //初始化语句
                init
        );
    }

    /**
     * 创建 域/方法 的多级访问, 方法的标识只能是最后一个
     *
     * @param components
     * @return
     */
    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = mMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = mMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }


    /**
     * 根据字符串获取Name，（利用Names的fromString静态方法）
     *
     * @param s
     * @return
     */
    private com.sun.tools.javac.util.Name getNameFromString(String s) {
        return mNames.fromString(s);
    }

    private static  class  SourceType {
        String mViewname;
        int mViewid;
        String mClassName;

        SourceType(String viewname, int viewid,String className) {
            this.mViewname = viewname;
            this.mViewid = viewid;
            this.mClassName = className;
        }

        public String toString() {
            String s;
            s= " mViewname= "+mViewname+" mViewid= "+mViewid+" mClassName= "+mClassName;
            return s;
        }
    }
}


