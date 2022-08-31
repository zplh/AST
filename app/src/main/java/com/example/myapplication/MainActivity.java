package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.annotation.BindView;

public class MainActivity extends Activity {
    @BindView(value = R.id.iv)
    protected ImageView mIv;
    @BindView(value = R.id.tv)
    protected TextView mTx;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Studeng b = new Studeng.Builder().setAge(5).setName("hehe").build();

        new com.example.myapplication.MainActivityInjector().bind(this);
        mTx.setText("hehe  "+b.getName()+"  "+b.getchange());
    }

}