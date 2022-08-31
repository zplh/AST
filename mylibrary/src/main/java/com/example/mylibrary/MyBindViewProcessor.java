package com.example.mylibrary;

import com.example.annotation.BindView;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.example.annotation.BindView")
public class MyBindViewProcessor extends AbstractProcessor {
    private Messager mMessager;
    public static final String GEN_CLASS_SUFFIX = "Injector";

    private Types mTypeUtils;//提供了和类型相关的一些操作，如获取父类、判断两个类是不是父子关系等
    private Elements mElementUtils;//提供了一些和元素相关的操作，如获取所在包的包名等
    private Filer mFiler;//Filer用于文件操作,用它去创建生成的代码文件

    //init方法是初始化的地方,我们可以通过ProcessingEnvironment获取到很多有用的工具类
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mTypeUtils = processingEnv.getTypeUtils();
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();

    }
    //该方法相当于处理器的main函数,在这里写你的扫描/评估和处理逻辑,以及生成java代码,RoundEnvironment可以查询包含
    //特定注解的被注解元素
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {


        //获取与InjectView注解相关的所有元素
        Set<? extends Element> elements = parse2Set(annotations, roundEnv);

        //获取分类后的集合
        Map<TypeElement, List<Element>> elementMap = parse2Map(elements);

        //遍历map,生成代码
        for (Map.Entry<TypeElement, List<Element>> entry : elementMap.entrySet()) {
            //生成注入代码
            generateInjectorCode(entry.getKey(), entry.getValue());
        }
        return true;
    }

    /**
     * 整合所有使用了注解的元素
     *
     * @param annotations
     * @param roundEnv
     * @return
     */
    private Set<? extends Element> parse2Set(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Element> set = new LinkedHashSet<>();
       /* for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            set.addAll(elements);
        }*/
        //为保证字段先解析,这里手动添加
        set.addAll(roundEnv.getElementsAnnotatedWith(BindView.class));
        return set;
    }

    /**
     * 将元素按TypeElement进行分类
     *
     * @param elements
     * @return
     */
    private Map<TypeElement, List<Element>> parse2Map(Set<? extends Element> elements) {

        Map<TypeElement, List<Element>> elementMap = new LinkedHashMap<>();

        //遍历所有被InjectView注释的元素
        for (Element element : elements) {
            //获取所在类的信息(一般指的是Activity/Fragment)
            TypeElement clazz = (TypeElement) element.getEnclosingElement();
            //按类存入map中,key相同会被覆盖,key代表的是元素所在的类信息,value是一个List
            //这样就可以把某个类和该类上使用了特定注解所有元素进行关联
            addElement(elementMap, clazz, element);
        }
        return elementMap;
    }


    //递归判断android.view.View是不是其父类
    private boolean isView(TypeMirror type) {
        //获取所有父类类型
        List<? extends TypeMirror> supers = mTypeUtils.directSupertypes(type);
        if (supers.size() == 0) {
            return false;
        }
        for (TypeMirror superType : supers) {
            if (superType.toString().equals("android.view.View") || isView(superType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加元素到map集合中
     *
     * @param elementMap key=clazz value=List<Element>
     * @param clazz      指定元素所在类的信息
     * @param element    指定元素
     */
    private void addElement(Map<TypeElement, List<Element>> elementMap,
                            TypeElement clazz, Element element) {
        List<Element> list = elementMap.get(clazz);
        if (list == null) {
            list = new ArrayList<>();
            elementMap.put(clazz, list);
        }
        list.add(element);
    }

    /**
     * 生成注入代码
     *
     * @param typeElement 元素所在类的Element
     * @param elements    需要注入的元素
     */
    private void generateInjectorCode(TypeElement typeElement, List<Element> elements) {

        //取出所在的类名
        String className = typeElement.getSimpleName().toString();
        String qualifiedName = typeElement.getQualifiedName().toString();

         //该类所在的包名
        String packageName = mElementUtils.getPackageOf(typeElement).asType().toString();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "className is "+className
                +"   qualifiedName="+qualifiedName
                +"   packageName="+packageName);

        //存储所有的字段名
        Map<Integer, String> fieldMap = new HashMap<>();

        //开始编写java类
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from ViewInjector. Do not modify!\n");
        builder.append("package " + packageName + ";\n"); //声明包
        builder.append("import " + qualifiedName + ";\n");//导包
        builder.append("import android.view.View;\n");//导包
        builder.append("public class " + className + GEN_CLASS_SUFFIX +" {\n");//声明类实现ViewBinder接口
        builder.append("\tpublic void bind(" + className + " args" + "){\n");//定义方法


        //解析注解
        for (Element element : elements) {
            if (element.getKind() == ElementKind.FIELD) {
                //如果不是View的子类则报错
                if (!isView(element.asType())) {
                    mMessager.printMessage(Diagnostic.Kind.ERROR, "is not a View", element);
                }
                //处理字段的注入
                //获取变量类型
                String type = element.asType().toString();
                //获取变量名
                String fieldName = element.getSimpleName().toString();
                //id
                int resourceId = element.getAnnotation(BindView.class).value();

                //将id和字段名关联存储
                fieldMap.put(resourceId, fieldName);

                //开始findViewById并赋值
                builder.append("\t\targs." + fieldName + "=(" + type + ")args.findViewById(" + resourceId + ");\n");


            } else {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "is not a FIELD or METHOD ", element);
            }
        }
        //添加结尾
        builder.append("\t}\n").append("}");

        //生成代码
        generateCode(className + GEN_CLASS_SUFFIX, builder.toString());

    }

    /**
     * 生成代码
     *
     * @param className java文件名
     * @param code      java代码
     */
    private void generateCode(String className, String code) {
        try {
            JavaFileObject file = mFiler.createSourceFile(className);
            Writer writer = file.openWriter();//拿到输出流
            writer.write(code);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
