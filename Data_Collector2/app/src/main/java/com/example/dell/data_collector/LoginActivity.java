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
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button register=( Button) this.findViewById(R.id.register);//注册界面
        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        Button login=( Button) this.findViewById(R.id.login);//登录界面
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String username=((EditText)findViewById(R.id.login_username)).getText().toString().trim();//获取输入的用户名
                String password=(( EditText)findViewById(R.id.login_pwd)).getText().toString().trim();//获取输入的密码
                if("".equals(username) || "".equals(password))
                { //判断是用户名或者密码否为空
                    Toast.makeText(LoginActivity.this, "用户名或密码不能为空，请重新输入！", Toast.LENGTH_LONG).show();
                }
                else
                {
                    //PostThread postThread=new PostThread("uname",username);
                    // postThread.start();
                    // postThread=new PostThread("pwd",password);
                    //postThread.start();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username",username);
                    //intent.putExtra("password",password);
                    startActivity(intent);
                }
            }
        });
    }

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
            NameValuePair pair = new BasicNameValuePair( name,pwd);
            // NameValuePair pair2 = new BasicNameValuePair("Content", pwd);
            //将准备好的键值对对象放置在一个List当中
            ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(pair);
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
                        String result = reader.readLine();
                        Log.d("HTTP", "POST:" + result);
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
