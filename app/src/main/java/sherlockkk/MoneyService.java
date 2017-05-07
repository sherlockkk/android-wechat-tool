package sherlockkk;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import sherlockkk.tcp.TCPClient;


public class MoneyService extends AccessibilityService implements TCPClient.OnMessageReceived {
    private static final String TAG = "MoneyService";

    private List<AccessibilityNodeInfo> parents = new ArrayList<>();
    private boolean isMoneyOpenedAlready = false;
    private TCPClient mTcpClient = null;


    public MoneyService() {
    }

    @Override
    protected void onServiceConnected() {
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
        //初始化TCP客户端
        mTcpClient = new TCPClient(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mTcpClient.run();
            }
        }).start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
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
                        List<AccessibilityNodeInfo> nodeTiXian = node.findAccessibilityNodeInfosByText("提现");
                        if (nodeXiaoFei != null) {
                            String[] ids = new String[]{"com.tencent.mm:id/bjn", "com.tencent.mm:id/bjr"};
                            obtainNodeText(ids);
                        }
                        if (nodeTiXian != null) {
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
        List<String> nameList = new ArrayList<>();
        List<String> sumList = new ArrayList<>();
        String name = null;
        String sum = null;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(ids[0]);
            for (AccessibilityNodeInfo node : list) {
                name = node.getText().toString();
                nameList.add(name);
                Log.e(">>>>>", "obtainNodeText2 name: " + name);
            }
            List<AccessibilityNodeInfo> list2 = nodeInfo.findAccessibilityNodeInfosByViewId(ids[1]);
            for (AccessibilityNodeInfo node : list2) {
                sum = node.getText().toString();
                sumList.add(sum);
                Log.e(">>>>>", "obtainNodeText2 name: " + sum);
            }
        }
        for (int i = 0; i < nameList.size(); i++) {
            String msg = nameList.get(i) + "领取了" + sumList.get(i);
            sendTcpMsg("10 " + msg + "<END>\r\n");
        }
    }

    /**
     * 开启子线程给tcp服务端发送信息
     *
     * @param msg
     */
    private void sendTcpMsg(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(msg);
                }
            }
        }).start();
    }

    /**
     * 通过ID获取控件上显示文字
     *
     * @param id
     */
    private void obtainNodeText(String id) {
        List<String> msgSum = new ArrayList<>();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(id);
            for (AccessibilityNodeInfo item : list) {
                String sum = item.getText().toString();
                msgSum.add(sum);
            }
            sendTcpMsg("10 你领取了" + msgSum + "<END>\r\n");
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
            parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            parents.clear();
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
    }


    /**
     * 接收TCP服务器消息
     *
     * @param message
     */
    @Override
    public void messageReceived(String message) {
        Log.i("MoneyService", "messageReceived: " + message);
        if (message != null && message.contains("OpenAndReportRedPacket")) {
            getLastPacket();
        }
    }
}
