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
    public static final String SERVERIP = "192.168.191.1"; // your computer IP address
    public static final int SERVERPORT = 9999;
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

            System.out.println("TCPClient send message: " + message);
        }
    }

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
//            InetAddress serverAddr = InetAddress.getByName(SERVERIP);

            Log.e("TCP SI Client", "SI: Connecting...");

            //创建socket连接服务器
            socket = new Socket(SERVERIP, SERVERPORT);
            try {

                //发送消息给服务器
//                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out = new PrintWriter(socket.getOutputStream());
                Log.e("TCP SI Client", "SI: Sent.");

                Log.e("TCP SI Client", "SI: Done.");

                //从服务器接收消息
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //while循环监听服务器消息
                while (mRun) {
                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null) {
                        //收到消息回调函数
                        mMessageListener.messageReceived(serverMessage);
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
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
        public void messageReceived(String message);
    }
}