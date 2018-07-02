package com.example.dell.data_collector;

import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class MainActivity extends AppCompatActivity {

    int MAX = 1000;//数组大小
    int lengh_name = 0;//Name数组下标
    int lengh_content = 0;//Content数组下标
    String[] from = {"name", "content"};              //这里是ListView显示内容每一列的列名
    int[] to = {R.id.name, R.id.content};   //这里是ListView显示每一列对应的list_item中控件的id
    String[] Name = new String[MAX]; //这里第一列所要显示的属性名
    String[] Content = new String[MAX];//这里是属性名对应的内容
    public static String UUID = "";//这里是设备的UUID号
    public static String postlabel = "";//这里是本次采集手机所处状态（平放于桌面 or 拿在手上）
    String X_lateral = "";
    String Y_longitudinal = "";
    String Z_vertical = "";
    String XYZ = "";
    private SensorManager sm;//传感器管理服务

    ArrayList<HashMap<String, String>> list = null;
    HashMap<String, String> map = null;
    private final static String TAG = "com.yin.system";
    private final static String[] ARGS_df = {"df"}; //存放获取系统存储分区的shell命令
    private final static String[] ARGS_fonts = {"ls", "-la", "/system", "/fonts"};
    public static final int FILTER_ALL_APP = 0; // 所有应用程序
    public static final int FILTER_SYSTEM_APP = 1; // 系统程序
    public static final int FILTER_THIRD_APP = 2; // 第三方应用程序
    public static final int TRACK_COUNT = 5; //需要采集的硬件传感器样本数目
    private static SharedPreferences preferences = null;
    private class ShellExecute {
        /*
         * args[0] : shell 命令 如"ls" 或"ls -1";
         * args[1] : 命令执行路径 如"/" ;
         */
        public String execute(String[] cmmand, String directory)
                throws IOException {
            String result = "";
            try {
                ProcessBuilder builder = new ProcessBuilder(cmmand);
                if (directory != null)
                    builder.directory(new File(directory));
                builder.redirectErrorStream(true);
                Process process = builder.start();
                //得到命令执行后的结果
                InputStream is = process.getInputStream();
                byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    result = result + new String(buffer);
                }
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    } //定义执行shell命令的类
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button finish = (Button)findViewById(R.id.finish);

        finish.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                PollingUtils.stopPollingService(MainActivity.this, AccService.class, AccService.ACTION);
            }
        });

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        postlabel = bundle.getString("postlabel");

        preferences = getSharedPreferences("firstrun", 0);
        boolean isfirst = preferences.getBoolean("isfirst", true);

        if(isfirst){
            preferences = getSharedPreferences("deviceuuid", 0);
            SharedPreferences.Editor editor = preferences.edit();
            if(UUID == null || "".equals(UUID)) {
                UUID = preferences.getString("device_id", null);
                UUID = java.util.UUID.randomUUID().toString();
                editor.putString("device_id", UUID);
                editor.commit();
            }
            preferences = getSharedPreferences("firstrun", 0);
            editor = preferences.edit();
            editor.putBoolean("isfirst",false);
            editor.commit();
        }
        else{
            preferences = getSharedPreferences("deviceuuid", 0);
            SharedPreferences.Editor editor = preferences.edit();
            UUID = preferences.getString("device_id", null);
        }

//       Get_Implicit_identification_useraction_information();//获取隐性标识用户行为信息
        Get_Implicit_identification_Equipment_information();//获取隐性标识硬件信息
        Get_Implicit_identification_software_information();//获取隐性标识软件信息
        Get_dominance_identification_information();//获取显性标识信息
//        Get_Accelerated_Sensor_Information();//获取加速度传感器信息，在dismiss方法中将数据传给数据库

        PollingUtils.startPollingService(this, 6, AccService.class, AccService.ACTION);

        list = new ArrayList<HashMap<String, String>>();//创建ArrayList对象；
        //将数据存放进ArrayList对象中，数据安排的结构是，ListView的一行数据对应一个HashMap对象，
        //HashMap对象，以列名作为键，以该列的值作为Value，将各列信息添加进map中，然后再把每一列对应
        //的map对象添加到ArrayList中
        for (int i = 0; i < lengh_name; i++) {
            map = new HashMap<String, String>();       //为避免产生空指针异常，有几列就创建几个map对象
            map.put("content", Content[i]);
            map.put("name", Name[i]);
            list.add(map);
        }
        ListView listView = (ListView) this.findViewById(R.id.listView1);
        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.list_item, from, to);  //创建一个SimpleAdapter对象
        listView.setAdapter(adapter); //调用ListActivity的setListAdapter方法，为ListView设置适配器

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PollingUtils.stopPollingService(this, AccService.class, AccService.ACTION);
        System.out.println("Stop polling service...");

    }

    /*******************
     * 获取隐性标识硬件信息
     ******************/
    private void Get_Implicit_identification_Equipment_information()//获取隐性标识硬件信息
    {
        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "deviceInfo ";//设备型号
        Content[lengh_content++] = android.os.Build.MODEL;

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "deviceBrand";//设备制造商
        Content[lengh_content++] = android.os.Build.MANUFACTURER;

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "screenInfo";//屏幕信息
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        float density = dm.density;//分辨率
        int width = dm.widthPixels;//宽度
        int height = dm.heightPixels;//高度
        Content[lengh_content++] = "分辨率：" + String.valueOf(density) + "  宽度：" + String.valueOf(width) + "  高度：" + String.valueOf(height);

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "innerSpace";//内置存储空间大小
        Content[lengh_content++] = String.valueOf(getTotalInternalMemorySize()) + "G";

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "CPUInfo";
        Content[lengh_content++] = getCpuName();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "CPUCore";
        Content[lengh_content++] = String.valueOf(getNumCores());

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "CPUClock";
        Content[lengh_content++] = getMinCpuFreq();
    }

    public static double getTotalInternalMemorySize() {//获取内存总大小
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        //API Level 18中使用getBlockSizeLong()代替getBlockSize()
        //想要兼容API 18之前版本需使用getBlockSize()
        long blockSize;
        long totalBlocks;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            blockSize = stat.getBlockSizeLong();
            totalBlocks = stat.getBlockCountLong();
        }
        else{
            blockSize = stat.getBlockSize();
            totalBlocks = stat.getBlockCount();
        }
        return totalBlocks * blockSize / (1024 * 1024 * 1024);
    }//获取内置存储空间大小

    private static String getCpuName() {//CPU类型
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            String[] array = text.split(":\\s+", 2);
            for (int i = 0; i < array.length; i++) {
            }
            return array[1];
        }//catch(FileNotFoundExecption e){
        //e.printStackTrace();}
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }//获取CPU类型

    private int getNumCores()
    {//CPU核心数
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Log.d(TAG, "CPU Count: "+files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Print exception
            // Log.d(TAG, "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }//获取CPU核心数

    public static String getMinCpuFreq() {//获取CPU频率
        String result = "";
        ProcessBuilder cmd;
        try {
            String[] args = {"/system/bin/cat",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        }
        return result.trim();
    }//获取CPU频率

    /******************
     * 获取隐性标识软件信息
     ****************/
    private void Get_Implicit_identification_software_information()//获取隐性标识软件信息
    {
        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "systemCoreInfo ";//获取Android Linux内核版本信息
        Content[lengh_content++] = getLinuxKernalInfoEx();//getLinuxKernalInfo();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "User Agent";
        Content[lengh_content++] = System.getProperty("http.agent");

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "androidVersion";
        Content[lengh_content++] = android.os.Build.VERSION.RELEASE;

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "systemDirectoryStruct";//系统存储结构
        ShellExecute cmdexe1 = new ShellExecute();
        try {
            Content[lengh_content++] = cmdexe1.execute(ARGS_df, "/");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            e.printStackTrace();
        }
    }

    //获取Android Linux内核版本信息
    public String getLinuxKernalInfoEx() {
        String result = "";
        String line;
        String[] cmd = new String[]{"/system/bin/cat", "/proc/version"};
        String workdirectory = "/system/bin/";
        try {
            ProcessBuilder bulider = new ProcessBuilder(cmd);
            bulider.directory(new File(workdirectory));
            bulider.redirectErrorStream(true);
            Process process = bulider.start();
            InputStream in = process.getInputStream();
            InputStreamReader isrout = new InputStreamReader(in);
            BufferedReader brout = new BufferedReader(isrout, 8 * 1024);

            while ((line = brout.readLine()) != null) {
                result += line;
            }
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private Context context;

    /**********************
     * 获取隐性标识用户行为信息
     ********************/
    private void Get_Implicit_identification_useraction_information()//获取隐性标识用户行为信息
    {
       Name[lengh_name++] = String.valueOf(lengh_name - 1) + "timeArea";
        TimeZone tz = TimeZone.getDefault();
        Content[lengh_content++] = tz.getDisplayName(false, TimeZone.SHORT);

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "timeFormat";
        if (is24Formate()) {
            ;
            Content[lengh_content++] = "24小时制";
        } else {
            Content[lengh_content++] = "12小时制";
        }

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "autoNetSelect";
        String auto_time = Settings.System.getString(getContentResolver(), Settings.System.AUTO_TIME);
        if (auto_time.equals("1"))
            Content[lengh_content++] = "True";
        else
            Content[lengh_content++] = "false";

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "autoTimeAreaSelect";
        String auto_time_zone = Settings.System.getString(getContentResolver(), Settings.System.AUTO_TIME_ZONE);
        if (auto_time_zone.equals("1"))
            Content[lengh_content++] = "True";
        else
            Content[lengh_content++] = "false";

         Name[lengh_name++] = String.valueOf(lengh_name - 1) + "autoLockTime";
        Content[lengh_content++] = android.provider.Settings.System.getString(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT) + "ms";


        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "wifiRemind";
        String wifi_sleep_policy = android.provider.Settings.System.getString(getContentResolver(), Settings.System.WIFI_SLEEP_POLICY);
        if (wifi_sleep_policy.equals("1"))
            Content[lengh_content++] = "True";
        else
            Content[lengh_content++] = "False";

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "getLocationMethod ";//获取位置
        Content[lengh_content++] = get_location_way();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "unlockFeedback";
        if (unlockFeedback())
            Content[lengh_content++] =  "TURE";
        else
            Content[lengh_content++] =  "false";

        /*########################################################*/

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "inputInfo";//输入法信息
        Content[lengh_content++] = get_inputmethod();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "language";
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getDisplayLanguage();
        Content[lengh_content++] = language;

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "isRoot";
        String str = Build.TAGS;
        if (str.matches("test-keys")) {
            Content[lengh_content++] = "True";
        } else {
            Content[lengh_content++] = "False";
        }

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "fontSize";
        Resources resources = this.getResources();
        Configuration configuration = resources.getConfiguration();
        Content[lengh_content++] = String.valueOf(configuration.fontScale) + "f";

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "fontList";
        ShellExecute cmdexe3 = new ShellExecute();
        try {
            Content[lengh_content++] = cmdexe3.execute(ARGS_fonts, "/");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            e.printStackTrace();
        }

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "phoneBell";
        Content[lengh_content++] = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE).toString();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "alarmBell";
        Content[lengh_content++] = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "infoBell";
        Content[lengh_content++] = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "IP";
        try {
            Content[lengh_content++] = get_IP();
        }catch (Exception e)
        {
            e.printStackTrace();
        }


        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "appList";
        Content[lengh_content++] = filterApp(FILTER_THIRD_APP);

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "systemAppList";
        Content[lengh_content++] = filterApp(FILTER_SYSTEM_APP);
    }

    public boolean  unlockFeedback(){
        try {
            return android.provider.Settings.System.getInt(this.getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED) > 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isSecured(){
        boolean isSecured = false;
        String classPath = "com.android.internal.widget.LockPatternUtils";
        try{
            Class<?> lockPatternClass = Class.forName(classPath);
            Object lockPatternObject = lockPatternClass.getConstructor(Context.class).newInstance(getApplicationContext());
            Method method = lockPatternClass.getMethod("isSecure");
            isSecured = (boolean) method.invoke(lockPatternObject);
        }catch (Exception e){
            isSecured = false;
        }
        return isSecured;
    }

    private boolean is24Formate()//时间格式判断
    {
        ContentResolver cv = this.getContentResolver();
        String strTimeFormat = android.provider.Settings.System.getString(cv, android.provider.Settings.System.TIME_12_24);
        if (strTimeFormat != null && strTimeFormat.equals("24")) {
            return true;
        }
        return false;
    }

    private String get_location_way()//获取位置方式
    {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER) && !providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是GPS
            // locationProvider = LocationManager.GPS_PROVIDER;
            return "GPS";
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER) && !providers.contains(LocationManager.GPS_PROVIDER)) {
            //如果是Network
            // locationProvider = LocationManager.NETWORK_PROVIDER;
            return "NETWORK";
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER) && providers.contains(LocationManager.GPS_PROVIDER)) {
            return "NETWORK + GPS";
        } else {
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return "None";
        }
    }

    private String get_inputmethod()//获取输入法信息
    {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> methodList = imm.getInputMethodList();
        String name = "";
        for (InputMethodInfo mi : methodList) {
            name = name + mi.loadLabel(getPackageManager()) + "\n";
        }
        return name;
    }

    private String get_IP()//获取IP地址
    {
        //获取wifi服务，7.0之后必须使用getApplicationContext()
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
        return ip;
    }

    private PackageManager pm;

    private String filterApp(int type) {
        String all_imformation = "";
        // 获取PackageManager对象
        pm = getPackageManager();
        // 查询已经安装的应用程序
        List<ApplicationInfo> applicationInfos = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_PERMISSIONS);
        // 排序
        Collections.sort(applicationInfos, new ApplicationInfo.DisplayNameComparator(pm));
        switch (type) {
            case FILTER_ALL_APP:// 所有应用
                for (ApplicationInfo applicationInfo : applicationInfos) {
                    all_imformation += getAppInfo(applicationInfo);
                }

                break;
            case FILTER_SYSTEM_APP:// 系统应用
                for (ApplicationInfo applicationInfo : applicationInfos) {
                    if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        all_imformation += getAppInfo(applicationInfo);
                    }
                }
            case FILTER_THIRD_APP:// 第三方应用

                for (ApplicationInfo applicationInfo : applicationInfos) {
                    // 非系统应用
                    if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                        all_imformation += getAppInfo(applicationInfo);
                    }
                    // 系统应用，但更新后变成不是系统应用了
                    else if ((applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        all_imformation += getAppInfo(applicationInfo);
                    }
                }
            default:
                break;
        }
        return all_imformation;
    }

    private String getAppInfo(ApplicationInfo applicationInfo)//获取应用信息
    {
        String appimformation = "";
        int uid = -1;
        String appName = applicationInfo.loadLabel(pm).toString();// 应用名
        String packageName = applicationInfo.packageName;// 包名
        uid = applicationInfo.uid;
        appimformation = "应用名：" + appName + "\r\n"
                + " 包名：" + packageName + "\r\n"
                + "UID:" + uid + "\r\n";
        return appimformation;
    }

    /*******************
     * 获取显性标识信息
     ***********************/
    private void Get_dominance_identification_information()//获取显性标识信息
    {
        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "ANDROID_ID";
        Content[lengh_content++] = android.provider.Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "Serial Number";
        Content[lengh_content++] = get_serialNum();

        Name[lengh_name++] = String.valueOf(lengh_name - 1) + "MAC";
        int currentapiVersion=android.os.Build.VERSION.SDK_INT;
        try {
            if (currentapiVersion >= 6)
                Content[lengh_content++] = getAdressMacByInterface();
            else
                Content[lengh_content++] = getMacAddress(context);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String get_serialNum()//获取serialNum
    {
        String serialNum = "";
        int absent = TelephonyManager.SIM_STATE_ABSENT;
        if (1 == absent)//没有SIM卡
        {
            serialNum = android.os.Build.SERIAL;
        } else//有SIM卡
        {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            serialNum = tm.getSimSerialNumber();
        }
        return serialNum;
    }


    public static final String getMacAddress(Context context) {
        /**
         * 获取本机MAC地址，适用Android 6.0 版本以下系统，需要权限android.permission.ACCESS_WIFI_STATE
         * Android 6.0 及以上版本系统返回常量“02:00:00:00:00:00”
         * @param context
         * @return
         */
        WifiInfo wifiInfo = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        if(wifiInfo != null && wifiInfo.getMacAddress() != null) {
            return wifiInfo.getMacAddress();
        }
        return "";
    }

    public static final String getAdressMacByInterface(){
        /**
         * 获取本机MAC地址，适用Android 6.0 及以上版本系统
         * 调用此方法时需开启wifi
         * @return
         */
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : all) {
                if (networkInterface.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = networkInterface.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:",b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }



    public void onPause(){
//        sm.unregisterListener(myAccelerometerListener);
        super.onPause();
    }


}

