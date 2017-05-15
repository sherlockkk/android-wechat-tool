package sherlockkk.tcp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP服务端
 *
 * @author SongJian
 * @Date 2017/5/11
 * @Email songjian0x00@163.com
 */

public class TCPServer {
    private static final String TAG = "TCPServer";

    private ServerSocket serverSocket;
    private OnMsgReceived mMsgRecived;
    private Socket socket;


    public TCPServer(OnMsgReceived mMsgRecived) {
        this.mMsgRecived = mMsgRecived;
        new Thread(new Runnable() {
            @Override
            public void run() {
                runserver();
            }
        }).start();
    }

    public void runserver() {
        try {
            serverSocket = new ServerSocket(9998);
            while (true) {
                socket = serverSocket.accept();
                Log.i(TAG, "run: " + socket.getInetAddress());
                //获得输入流
                InputStream inputStream = socket.getInputStream();
                //获得输出流
                OutputStream outputStream = socket.getOutputStream();
                byte[] byteBuffer = new byte[8192];
                int temp = 0;
                String msg = "";
                //读取接收到的数据
                while ((temp = inputStream.read(byteBuffer)) != -1) {
                    //将byte转为string
                    msg = new String(byteBuffer, 0, temp);
                    if (msg.contains("<END>")) {
                        break;
                    }
                }
                //1 "Hello"<END>
                mMsgRecived.msgReceived(msg);
                if (msg.contains("<END>") && "Hello".equals(msg)) {
                    byte[] bytes = "0 \"Android\"<END>\r\n".getBytes();
                    outputStream.write(bytes, 0, bytes.length);
                    outputStream.flush();
                }

            }
//            socket.close();
//            serverSocket.close();
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
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收消息的回调接口
    public interface OnMsgReceived {
        void msgReceived(String message);
    }


}
