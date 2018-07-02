package com.example.dell.data_collector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by mingmin on 2018/6/2.
 */

public class postActivity extends AppCompatActivity {

    private Button btn1,btn2,btn3;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        btn1 = (Button)findViewById(R.id.button1);
        btn2 = (Button)findViewById(R.id.button2);
        btn3 = (Button)findViewById(R.id.button3);
        //当采集数据时手机平放在桌面上，点击btn1进入采集界面
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_post_flat = new Intent(postActivity.this, MainActivity.class);
                intent_post_flat.putExtra("postlabel", "1");
                startActivity(intent_post_flat);
            }
        });
        //当采集数据时手机拿在手里，点击btn2进入采集界面
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent_post_hand = new Intent(postActivity.this, MainActivity.class);
                intent_post_hand.putExtra("postlabel", "2");
                startActivity(intent_post_hand);
            }
        });
        //当采集无规律数据时，点击btn3进入采集界面
        btn3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent_post_hand = new Intent(postActivity.this, MainActivity.class);
                intent_post_hand.putExtra("postlabel", "3");
                startActivity(intent_post_hand);
            }
        });
    }
}
