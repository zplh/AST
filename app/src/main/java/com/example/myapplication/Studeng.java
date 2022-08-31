package com.example.myapplication;

import com.example.annotation.AST;
import com.example.annotation.MyAnnotation;

public class Studeng {

    private Studeng mStu;

    private int age;
    private String name;

    private Studeng(Builder builder) {
        age = builder.Age;
    }

    @AST(type = AST.TYPE.SOURCE, value = AST.ID.MODULE_INIT, level = 1)//插入源，插桩类型是模块初始化，优先级是1
    public void autoInitModule() {
        age = 19;
    }

    @AST(type = AST.TYPE.TARGET, value = AST.ID.MODULE_INIT, level = AST.LEVEL.BEFORE)
    public String getName() {
        return name;
    }

    @MyAnnotation
    public int getAge() {
        return age;
    }

    public int getchange() {
        return age;
    }

    public static class Builder {
        private int Age;
        private String name;
        public Studeng  build() {
            return new Studeng(this);
        }
        public Builder setAge(int a) {
            this.Age = a;
            return this;
        }

        public Builder setName(String a) {
            this.name = a;
            return this;
        }
    }
}
