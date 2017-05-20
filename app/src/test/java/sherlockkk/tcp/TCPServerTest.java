package sherlockkk.tcp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author SongJian
 * @Date 2017/5/11
 * @Email songjian0x00@163.com
 */
public class TCPServerTest implements TCPServer.OnMsgReceived {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void connectTest() {
        final TCPServer server = new TCPServer(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
//                server.run();
            }
        }).start();
    }

    @Override
    public void msgReceived(String message) {
        System.out.println(message);
    }
}