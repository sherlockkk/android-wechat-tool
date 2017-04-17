package kompasim;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.os.Bundle;
import android.widget.Toast;

import com.example.alvido_bahor.wechattool.R;

import kompasim.tcp.TCPClient;


public class MainActivity extends Activity {

    private TCPClient mTcpClient = null;
    private ConnectTask conctTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请开启辅助服务！", Toast.LENGTH_LONG).show();
        finish();
    }


    /**
     * tcp 异步连接TCP服务
     */
    public class ConnectTask extends AsyncTask<String, String, TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                    if (message != null) {
                        System.out.println("Return Message from Socket::::: >>>>> " + message);
                    }
                }
            });
            mTcpClient.run();
            if (mTcpClient != null) {
                mTcpClient.sendMessage("connected");
            }
            return null;
        }
    }

    /**
     * Activity销毁时释放资源
     */
    @Override
    protected void onDestroy() {
        try {
            mTcpClient.sendMessage("bye");
            mTcpClient.stopClient();
            conctTask.cancel(true);
            conctTask = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
