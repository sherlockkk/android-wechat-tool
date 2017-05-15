package sherlockkk;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sherlockkk.tcp.TCPClient;
import sherlockkk.tcp.TCPServer;


public class MoneyService extends AccessibilityService implements TCPClient.OnMessageReceived, TCPServer.OnMsgReceived {
    private static final String TAG = "MoneyService";

    private List<AccessibilityNodeInfo> parents;
    private boolean isMoneyOpenedAlready = false;
    private TCPClient mTcpClient = null;
    private TCPServer mTcpServer = null;
    private StringSender stringSender;


    int msgId;//消息ID
    List<String> nameList;//领取者的集合
    List<String> sumList;//领取金额的集合
    String openTime;//打开红包的时间
    String time;//收到红包消息时间

    public MoneyService() {
    }

    @Override
    protected void onServiceConnected() {
        mTcpServer = new TCPServer(this);//开启服务的时候初始化一个tcpserver
        msgId = 0;//消息ID
        parents = new ArrayList<>();
        nameList = new ArrayList<>();//领取者的集合
        sumList = new ArrayList<>();//领取金额的集合
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        // 响应事件的类型，这里是全部的响应事件（长按，单击，滑动等）
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        // 反馈给用户的类型，这里是语音提示
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        // 过滤的包名
        String[] packageNames = {"com.tencent.mm"};
        info.packageNames = packageNames;
        setServiceInfo(info);
        sendTcpMsg("1 \"Android\"<END>\r\n");

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//        mTcpServer.run();
//            }
//        }).start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                stringSender = new StringSender();
                stringSender.sender = event.getText().toString().split(":");
                Log.e("--->", event.getText().toString());
                Log.e("--->", event.getClassName().toString());
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]")) {
                            //模拟打开通知栏消息，即打开微信
                            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                Log.e(TAG, "onAccessibilityEvent className: " + className);
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    sendTcpMsg("3 \"RedPacket Received\"<END>\r\n");
                    time = getTime();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")) {
                    Log.e("songjian", "开红包");
                    inputClick("com.tencent.mm:id/bjj");
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //退出红包
                    Log.e("songjian", "抢完，退出红包");
                    AccessibilityNodeInfo node = getRootInActiveWindow();
                    if (node != null) {
                        //如果页面有带有“消费”字样,则为群红包，带有“提现”字样，则为私包
                        List<AccessibilityNodeInfo> nodeXiaoFei = node.findAccessibilityNodeInfosByText("消费");
                        if (nodeXiaoFei.size() == 0) {
                            nodeXiaoFei = node.findAccessibilityNodeInfosByText("发红包");
                            if (nodeXiaoFei.size() == 0) {
                                nodeXiaoFei = node.findAccessibilityNodeInfosByText("转账");
                            }
                        }
                        List<AccessibilityNodeInfo> nodeTiXian = node.findAccessibilityNodeInfosByText("提现");
                        if (nodeXiaoFei.size() != 0) {
                            String[] ids = new String[]{"com.tencent.mm:id/bjn", "com.tencent.mm:id/bjr"};
                            obtainNodeText(ids);
                        }
                        if (nodeTiXian.size() != 0) {
                            obtainNodeText("com.tencent.mm:id/bfw");
                        }
                    }
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 通过ID数组获取群红包领取明细
     *
     * @param ids
     */

    private void obtainNodeText(String[] ids) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(ids[0]);
            for (AccessibilityNodeInfo node : list) {
                String name = node.getText().toString();
                nameList.add(name);
            }
            List<AccessibilityNodeInfo> list2 = nodeInfo.findAccessibilityNodeInfosByViewId(ids[1]);
            for (AccessibilityNodeInfo node : list2) {
                String sum = node.getText().toString();
                sumList.add(sum.substring(0, sum.length() - 1));
            }
        }
        for (int i = 0; i < nameList.size(); i++) {
            String msg = nameList.get(i) + "领取了" + sumList.get(i);
            Log.i(TAG, "obtainNodeText sumList: " + msg);
        }
        ++msgId;
        try {
            sendTcpMsg("12 " + getJson() + "<END>\r\n");
            clearData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clearData() {
        nameList.clear();
        sumList.clear();
    }


    /**
     * 构造json对象
     *
     * @return
     * @throws JSONException
     */
    public JSONObject getJson() throws JSONException {
        JSONObject dict = new JSONObject();
        JSONObject packet = null;
        if (nameList != null) {
            for (int i = 0; i < nameList.size(); i++) {
                JSONObject detail = new JSONObject();
                detail.put("IMNickName", nameList.get(i));
                detail.put("Amount", sumList.get(i));
                dict.put(nameList.get(i), detail);
            }
            JSONObject dictOther = new JSONObject();
            dictOther.put("LuckyDict", dict);
            dictOther.put("Sender", getSender());
            dictOther.put("Title", getTitle());
            dictOther.put("OpenTime", openTime);
            dictOther.put("LuckyCount", nameList.size());
            packet = new JSONObject();
            packet.put("RedPacket", dictOther);
            packet.put("MsgID", msgId);
            packet.put("Time", time);
            packet.put("GroupName", "");
            packet.put("Source", getSender());
        }
        return packet;
    }

    private String getTitle() {
        String title = stringSender.sender[1].substring(0, stringSender.sender[1].length() - 1);
        return title;
    }

    private String getSender() {
        String sender = stringSender.sender[0].substring(1);
        return sender;
    }

    private String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss SSS a", Locale.ENGLISH);
        String formatStr = formatter.format(new Date());
        return formatStr;
    }

    /**
     * 开启子线程给tcp服务端发送信息
     *
     * @param msg
     */
    /*
        "RedPacket": {
        "LuckyDict": {
            "张三": {
                "IMNickName": "张三",
                "Amount": 1.23
            },
            "李四": {
                "IMNickName": "李四",
                "Amount": 1.92
            }
        },
        "Sender": "发红包人",
        "Title": "红包文字恭喜发财",
        "OpenTime": "/Date(-62135596800000)/",  --此处为打开红包时间
        "LuckyCount": 2
    },
    "MsgID": 1,
    "Time": "/Date(1493999743680)/",  --此处为收到红包消息时间
    "GroupName": "群聊名称",
    "Source": "发消息的人"
}
     */
    private void sendTcpMsg(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mTcpClient = new TCPClient(MoneyService.this);
                mTcpClient.sendMessage(msg);
            }
        }).start();
    }

    /**
     * 通过ID获取控件上显示文字
     *
     * @param id
     */
    private void obtainNodeText(String id) {
//        List<String> msgSum = new ArrayList<>();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(id);
            for (AccessibilityNodeInfo item : list) {
                String sum = item.getText().toString();
                sumList.add(sum.substring(0, sum.length() - 1));
            }
            ++msgId;
            try {
                sendTcpMsg("12 " + getJson() + "<END>\r\n");
                clearData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     *
     * @param clickId
     */
    private void inputClick(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }


    /**
     * 获取List中最后一个红包，并进行模拟点击
     */
    private void getLastPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(">>>", "rootNode is　null");
            return;
        } else {
            isMoneyOpenedAlready = false;
            recycle(rootNode);
        }

        if (parents.size() > 0) {
            parents.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            parents.clear();
            openTime = getTime();
        }
    }


    /**
     * 回归函数遍历每一个节点，并将含有"领取红包"存进List中
     *
     * @param info
     */
    public void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            if (info.getText() != null) {
                Log.e("text is : ", info.getText().toString());
                if (info.getText().toString().contains("你领取了")) {
                    isMoneyOpenedAlready = true;
                }
                if ("领取红包".equals(info.getText().toString()) && (isMoneyOpenedAlready == false)) {
                    if (info.isClickable()) {
                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parents.add(parent);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        } else {
            for (int i = info.getChildCount() - 1; i >= 0; i--) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }

    /**
     * 服务被中断时回调
     */
    @Override
    public void onInterrupt() {
        mTcpClient.stopClient();
        mTcpServer.stopClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTcpClient.stopClient();
        mTcpServer.stopClient();
    }

    /**
     * 接收TCP服务器消息
     *
     * @param message 收到的消息
     */
    @Override
    public void messageReceived(String message) {
        Log.i("MoneyService", "messageReceived: " + message);
    }

    /**
     * 接收TCP客户端消息
     *
     * @param message 收到的消息
     */
    @Override
    public void msgReceived(String message) {
        Log.i(TAG, "msgReceived: " + message);
        if (message.contains("<END>") && message.contains("Hello")) {
            sendTcpMsg("0 \"Android\"<END>\r\n");
        } else if (message.contains("<END>") && message.contains("OpenAndReportRedPacket")) {
            sendTcpMsg("0 \"OK\"<END>\r\n");
            getLastPacket();
        } else if (message.contains("<END>") && message.substring(0, 2).equals("4 ")) {
            sendTcpMsg("0 " + message.substring(2, message.length()) + "\r\n");
        }
    }

    /**
     * 构造一个内部类用来在case代码块间传递数据
     */
    class StringSender {
        String[] sender;
    }
}
