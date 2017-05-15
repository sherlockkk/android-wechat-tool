package sherlockkk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sherlockkk.wechattool.R;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import sherlockkk.tcp.TCPClient;


public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tv_ip;
    private EditText et_ip;
    private EditText et_port;
    private Button btn_go;
    private Button btn_hand;
    private Config config;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "wake_lock");
        initView();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire();
    }


    /**
     * Activity销毁时释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    private void initView() {
        config = new Config();
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        if (isWifi()) {
            tv_ip.setText("本机IP:" + obtainHostIP());
        } else {
            tv_ip.setText("请连接wifi网络");
        }

        et_ip = (EditText) findViewById(R.id.et_ip);
        et_port = (EditText) findViewById(R.id.et_port);
        btn_go = (Button) findViewById(R.id.btn_go);
        btn_go.setOnClickListener(this);
        btn_hand = (Button) findViewById(R.id.btn_hand);
        btn_hand.setOnClickListener(this);
    }

    private boolean isWifi() {
        ConnectivityManager conMann = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = conMann.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_go:
                submit();
                break;
            case R.id.btn_hand:
                final String ip = et_ip.getText().toString().trim();
                final String port = et_port.getText().toString().trim();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TCPClient tcpClient = new TCPClient(ip, Integer.parseInt(port));
                        tcpClient.sendMessage("1 \"Android\"<END>\r\n");
                    }
                }).start();
                break;
        }
    }

    private void submit() {
        // validate
        String ip = et_ip.getText().toString().trim();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, "请输入ip地址", Toast.LENGTH_SHORT).show();
            return;
        }
        String port = et_port.getText().toString().trim();
        if (TextUtils.isEmpty(port)) {
            Toast.makeText(this, "请输入端口", Toast.LENGTH_SHORT).show();
            return;
        }
        Config.IP = ip;
        Config.PORT = Integer.parseInt(port);
        saveConfig(ip, port);
        goSettingActivity();
    }

    /**
     * 保存配置信息到SharedPreferences
     *
     * @param ip
     * @param port
     */
    private void saveConfig(String ip, String port) {
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ip", ip);
        editor.putString("port", port);
        editor.commit();
    }

    /**
     * 从SharedPreferences中加载配置信息
     */
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        if (sharedPreferences != null) {
            et_ip.setText(sharedPreferences.getString("ip", ""));
            et_port.setText(sharedPreferences.getString("port", ""));
        }
    }

    /**
     * 跳转Activity至设置界面
     */
    private void goSettingActivity() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请开启辅助服务！", Toast.LENGTH_LONG).show();
    }

    private String obtainHostIP() {
        String ip = null;
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        ip = intToIp(ipAddress);
        return ip;
    }

    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
}
