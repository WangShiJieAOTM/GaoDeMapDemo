package com.example.gaodemapdemo;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void jumpSearchActivity(View view) {
        startActivity(new Intent(this, SearchActivity.class));
    }

    public void jumpBlueToothActivity(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(new Intent(this, BlueToothActivity.class));
        }
    }
}