package com.example.pressurereader;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static int testActivity;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button semi = findViewById(R.id.testSemi);
        Button full = findViewById(R.id.testBoth);

        semi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),TestSemiActivity.class);
                testActivity = 1;
                startActivity(i);
            }
        });

        full.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),TestBothActivity.class);
                testActivity = 2;
                startActivity(i);
            }
        });
    }

    public static int getTestActivity() {
        return testActivity;
    }
}
