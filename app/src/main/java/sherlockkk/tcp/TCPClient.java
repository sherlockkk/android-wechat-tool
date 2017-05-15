package sherlockkk.tcp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    Socket socket;
    public String serverip = ""; // your computer IP address
    public int serverport = 0;
    private OnMessageReceived mMessageListener = null;

    private OutputStream outputStream = null;
    private InputStream inputStream = null;

    public TCPClient(String ip, int port) {
        serverip = ip;
        serverport = port;
        try {
            socket = new Socket(serverip, serverport);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造函数
     *
     * @param listener 收到消息的回调监听
     */
    public TCPClient(final OnMessageReceived listener) {
        mMessageListener = listener;
        serverip = Config.IP;
        serverport = Config.PORT;
        try {
            socket = new Socket(serverip, serverport);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 客户端给服务端发送消息
     *
     * @param message 发送的消息
     */
    public void sendMessage(String message) {
        try {
            outputStream = socket.getOutputStream();
            if (outputStream != null) {
                try {
                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes, 0, messageBytes.length);
                    outputStream.flush();
                    readMessage();
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage() {
        try {
            if (inputStream != null) {
                byte[] buffer = new byte[8192];
                String message = "";
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    message += new String(buffer, 0, len);
                    if (message.contains("<END>")) {
                        break;
                    }
                }
                inputStream.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开socket连接
     */
    public void stopClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public void run() {
//        try {
//            //创建socket连接服务器
//
//            try {
//                //发送消息给服务器
////                outputStream = socket.getOutputStream();
////                byte[] hand = "1 \"Android\"<END>\r\n".getBytes();
////                outputStream.write(hand, 0, hand.length);
////                outputStream.flush();
//                //从服务器接收消息
//                inputStream = socket.getInputStream();
////                byte[] buffer = new byte[8192];
////                if (inputStream.read(buffer) != -1) {
////                    int len = inputStream.read(buffer);
////                    serverMessage = new String(buffer, 0, len);
////                }
////                if (serverMessage.contains("<END>")) {
////                    sendMessage("0 \"OK\"<END>\r\n");
////                }
////                mMessageListener.messageReceived(serverMessage);
//            } catch (Exception e) {
//                Log.e("TCP SI Error", "SI: Error", e);
//                e.printStackTrace();
//            } finally {
//                //关闭socket，释放资源
//                socket.close();
//            }
//        } catch (Exception e) {
//            Log.e("TCP SI Error", "SI: Error", e);
//        }
//    }

    //接收消息的回调接口
    public interface OnMessageReceived {
        void messageReceived(String message);
    }
}