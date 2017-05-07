package sherlockkk.tcp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import sherlockkk.Config;

/**
 * Tcp 客户端
 *
 * @author SongJian
 * @created 2017-4-16.
 * @e-mail 1129574214@qq.com
 */

public class TCPClient {

    private String serverMessage;
    /**
     * 指定服务器Ip地址，端口号
     */
    public String serverip = ""; // your computer IP address
    public int serverport = 0;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    private PrintWriter out = null;
    private BufferedReader in = null;

    /**
     * 构造函数
     *
     * @param listener 收到消息的回调监听
     */
    public TCPClient(final OnMessageReceived listener) {
        mMessageListener = listener;
        serverip = Config.IP;
        serverport = Config.PORT;
    }

    /**
     * 客户端给服务端发送消息
     *
     * @param message 发送的消息
     */
    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    /**
     * 断开socket连接
     */
    public void stopClient() {
        mRun = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Socket socket;

    public void run() {
        mRun = true;
        try {
            //创建socket连接服务器
            socket = new Socket(serverip, serverport);
            try {
                //发送消息给服务器
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println("1 \"Android\"<END>\r\n");
                out.flush();
                //从服务器接收消息
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String temp;
                //while循环监听服务器消息
                while (mRun) {
                    while ((temp = in.readLine()) != null) {
                        serverMessage = in.readLine();
                        //收到消息回调函数
                        mMessageListener.messageReceived(serverMessage);
                    }
                    serverMessage = null;
                }
            } catch (Exception e) {
                Log.e("TCP SI Error", "SI: Error", e);
                e.printStackTrace();
            } finally {
                //关闭socket，释放资源
                socket.close();
            }
        } catch (Exception e) {
            Log.e("TCP SI Error", "SI: Error", e);
        }

    }

    //接收消息的回调接口
    public interface OnMessageReceived {
        void messageReceived(String message);
    }
}