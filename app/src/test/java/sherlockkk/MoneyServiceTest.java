package sherlockkk;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author SongJian
 * @Date 2017/5/13
 * @Email songjian0x00@163.com
 */
public class MoneyServiceTest {

    private String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a", Locale.ENGLISH);
        String formatStr = formatter.format(new Date());
        return formatStr;
    }


    @Test
    public void getTimeTest() {
        System.out.println(getTime());
    }

}