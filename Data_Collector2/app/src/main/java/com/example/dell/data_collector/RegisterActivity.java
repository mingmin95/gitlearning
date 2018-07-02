package com.example.dell.data_collector;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button submit=(Button)findViewById(R.id.submit);//获取“提交”按钮
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String user=((EditText)findViewById(R.id.txt_username)).getText().toString();//获取输入的用户名
                String pass=((EditText)findViewById(R.id.txt_password)).getText().toString();//获取输入的密码
                String repass=((EditText)findViewById(R.id.txt_repassword)).getText().toString();//获取输入的确认密码
                String email=((EditText)findViewById(R.id.txt_mailbox)).getText().toString();//获取输入的邮箱

                if(!"".equals(user) && !"".equals(pass) && !"".equals(email)){
                    //判断两次输入的密码是否一致
                    if(!pass.equals(repass)){
                        Toast.makeText(RegisterActivity.this,"两次输入的密码不一致，请重新输入！",Toast.LENGTH_LONG).show();
                        ((EditText)findViewById(R.id.txt_password)).setText("");//清空“密码”编辑框
                        ((EditText)findViewById(R.id.txt_repassword)).setText("");//清空“确认密码”编辑框
                        ((EditText)findViewById(R.id.txt_password)).requestFocus(); //让“密码”编辑框获得焦点
                    }
                    else {
                        postresult="";
                        //将注册信息发送到服务器端
                        PostThread postThread1 = new PostThread("uname", user);
                        postThread1.start();
                        PostThread postThread2 = new PostThread("pwd", pass);
                        postThread2.start();
                        PostThread postThread3 = new PostThread("email", email);
                        postThread3.start();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);  //启动新的Activity
                    }
                    //if( postresult =="exit") {
                    //  Toast.makeText(RegisterActivity.this, "登录信息错误，请重新输入！", Toast.LENGTH_LONG).show();
                    //}
                    //else {
                    //将收入与的信息保存到Bundle中，并启动一个新的Activitiy显示输入的用户注册信息
                    // Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    //startActivity(intent);  //启动新的Activity
                    // }
                }else{
                    Toast.makeText(RegisterActivity.this,"请将注册信息输入完整！",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private String postresult ="";

    /********************** 将数据传入服务器*************************/
    //子线程：使用POST方法向服务器发送用户名、密码等数据
    class PostThread extends Thread {
        String name;
        String pwd;
        public PostThread(String name, String pwd) {
            this.name = name;
            this.pwd = pwd;
        }
        @Override
        public void run() {
            HttpClient httpClient = new DefaultHttpClient();
            String url = "http://10.60.150.192/devicefinger/android.php";
            //第二步：生成使用POST方法的请求对象
            HttpPost httpPost = new HttpPost(url);
            //NameValuePair对象代表了一个需要发往服务器的键值对
            NameValuePair pair1 = new BasicNameValuePair( name,pwd);
            // NameValuePair pair2 = new BasicNameValuePair("Content", pwd);
            //将准备好的键值对对象放置在一个List当中
            ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(pair1);
            // pairs.add(pair2);
            try {
                //创建代表请求体的对象（注意，是请求体）
                HttpEntity requestEntity = new UrlEncodedFormEntity(pairs);
                //将请求体放置在请求对象当中
                httpPost.setEntity(requestEntity);
                //执行请求对象
                try {
                    //第三步：执行请求对象，获取服务器发还的相应对象
                    HttpResponse response = httpClient.execute(httpPost);
                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
                    if (response.getStatusLine().getStatusCode() == 200) {
                        //第五步：从相应对象当中取出数据，放到entity当中
                        HttpEntity entity = response.getEntity();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                        postresult = reader.readLine().toString().trim();
                        Log.d("HTTP", "POST:" + postresult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
