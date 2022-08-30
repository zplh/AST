package com.example.myapplication;

import com.example.annotation.MyAnnotation;

public class Studeng {

    private Studeng mStu;

    private int age;

    private Studeng(Builder builder) {
        age = builder.Age;
    }

    @MyAnnotation
    public int getAge() {
        return age;
    }

    public static class Builder {
        private int Age;
        public Studeng  build() {
            return new Studeng(this);
        }
        public Builder setAge(int a) {
            this.Age = a;
            return this;
        }
    }
}
