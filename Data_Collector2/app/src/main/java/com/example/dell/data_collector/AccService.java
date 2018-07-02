package com.example.dell.data_collector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by mingmin on 2018/6/10.
 */

public class AccService extends Service {
    public static final String ACTION = "com.example.dell.data_collector.AccService";
    public static final int TRACK_COUNT = 1; //需要采集的硬件传感器样本数目
    private final static String TAG = "AccService";
    private SensorManager sm;//传感器管理服务
    WakeLock m_wklk;
    String X_lateral = "";
    String Y_longitudinal = "";
    String Z_vertical = "";
    String XYZ = "";
    String UUID = "";
    String postlabel = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        System.out.println("onCreate invoke");
        super.onCreate();

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        int sensorType = Sensor.TYPE_ACCELEROMETER;
        sm.registerListener(myAccelerometerListener,sm.getDefaultSensor(sensorType),100000);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        m_wklk = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AccService.class.getName());
        m_wklk.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        System.out.println("onStartCommand invoke");
        Log.i(TAG,"\n UUID "+MainActivity.UUID);
        X_lateral = "";
        Y_longitudinal = "";
        Z_vertical = "";
        XYZ = "";
        UUID = MainActivity.UUID;
        postlabel = MainActivity.postlabel;
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        int sensorType = Sensor.TYPE_ACCELEROMETER;
        sm.registerListener(myAccelerometerListener,sm.getDefaultSensor(sensorType),100000);
        Get_Accelerated_Sensor_Information();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        System.out.println("onDestroy invoke");
        m_wklk.release();
    }

    private void Get_Accelerated_Sensor_Information(){
        //创建一个SensorManager来获取系统的传感器服务
        new Thread(new Runnable(){
            @Override
            public void run(){
                int i = 0;
                while(i < TRACK_COUNT){
                    try{
                        Thread.sleep(3000);
                        i++;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sm.unregisterListener(myAccelerometerListener);
                Log.i(TAG,"\n X-axis "+X_lateral);
                Log.i(TAG,"\n Y-axis "+Y_longitudinal);
                Log.i(TAG,"\n Z-axis "+Z_vertical);
                Log.i(TAG,"\n Acc_xyz "+XYZ);
                PostThread postThread = new PostThread();
                postThread.start();
                try{
                    postThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    JSONObject jsonObject = new JSONObject(postresult);
                    Log.i("json:",jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /*
    *SensorEventListener接口的实现，需要实现两个方法
    */
    final SensorEventListener myAccelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                Log.i(TAG,String.valueOf(event.values[2]));
                X_lateral = X_lateral + String.valueOf(event.values[0]) + ",";
                Y_longitudinal = Y_longitudinal + String.valueOf(event.values[1]) + ",";
                Z_vertical = Z_vertical + String.valueOf(event.values[2]) + ",";
                XYZ = XYZ + String.valueOf(Math.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2])) + ",";
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG,"onAccuracyChanged");
        }
    };

    /********************** 将数据传入服务器*************************/
    //子线程：使用POST方法向服务器发送采集的数据
    private String postresult = "";//服务器反馈的值
    class PostThread extends Thread{
        @Override
        public void run(){
            HttpClient httpClient = new DefaultHttpClient();
            String url = "http://10.60.150.153:8082/device/acceleration4auth";
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setHeader("Accept", "application/json");
            ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("id",null);
                jsonObject.put("uuid", UUID);
                jsonObject.put("postlabel", postlabel);
                jsonObject.put("xlateral", X_lateral);
                jsonObject.put("ylongitudinal", Y_longitudinal);
                jsonObject.put("zvertical", Z_vertical);
                jsonObject.put("xyz", XYZ);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            try{
                Log.d("output:","1");
                StringEntity string = new StringEntity(jsonObject.toString(),"utf-8");
                Log.d("output:","2");
                string.setContentEncoding("UTF-8");
                Log.d("output:","3");
                string.setContentType("application/json");
                httpPost.setEntity(string);
                HttpResponse response = httpClient.execute(httpPost);
                Log.d("output:","4");
                Log.d("output:", String.valueOf(response.getStatusLine().getStatusCode()));
                if(response.getStatusLine().getStatusCode() == 200){
                    postresult = EntityUtils.toString(response.getEntity());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
