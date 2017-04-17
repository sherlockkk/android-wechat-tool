package kompasim;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kompasim.tcp.TCPClient;

public class MoneyService extends AccessibilityService implements TCPClient.OnMessageReceived {

    private List<AccessibilityNodeInfo> parents = new ArrayList<>();
    private boolean isMoneyOpenedAlready = false;

    private String NONE = "none";
    private String MONEY = "money";
    private String ANSWER = "answer";
    private String whatIsIt = NONE;

    private TCPClient mTcpClient = null;

    public MoneyService() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.e("--->", "connect");
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
    }

    StringTest strings = new StringTest();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("--->", event.getText().toString());
                String[] splits = event.getText().toString().split(":");
                strings.strs = splits;
                sendTcpMsg(splits);
                Toast.makeText(this, "收到" + strings.strs[0].substring(1) + "的消息：" + strings.strs[1].substring(0, strings.strs[1].length() - 1), Toast.LENGTH_SHORT).show();
                Log.e("--->", event.getClassName().toString());
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]")) {
                            /**
                             * -----------------
                             */
//                            if (!MyApplication.money_.isChecked()) {
//                                return;
//                            }
                            whatIsIt = MONEY;
                            //模拟打开通知栏消息，即打开微信
                            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                    Log.e("songjian:", "进入微信1");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            /**
                             * -----------------
                             */
//                            if (!MyApplication.answer_.isChecked()) {
//                                return;
//                            }
                            whatIsIt = ANSWER;
                            //模拟打开通知栏消息，即打开微信
                            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                // kompsdim is it tencent
                                //
                                //
                                try {
                                    pendingIntent.send();
                                    Log.e("songjian:", "进入微信2");
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
                Log.e("--->", "wechat");
                Log.e("text--->", event.getText().toString());
                Log.e("class--->", event.getClassName().toString());
                // Log.e("kompasim:", getRootInActiveWindow().toString());


                String className = event.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //点击最后一个红包
                    Log.e("what-->", whatIsIt);
                    if (whatIsIt == MONEY) {
                        /**
                         * -----------------
                         */
//                        if (!MyApplication.money_.isChecked()) {
//                            return;
//                        }
                        Log.e("songjian", "启动，点击红包");
                        getLastPacket(event);
                    } else if (whatIsIt == ANSWER) {
                        /**
                         * -----------------
                         */
//                        if (!MyApplication.answer_.isChecked()) {
//                            return;
//                        }
                        Log.e("songjian", "启动，自动回复");
                        answer(event, strings.strs);
//                        answer(event);
                    }
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")) {
                    //开红包
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
     * 回复指定消息
     * 回复收到的联系人跟消息
     *
     * @param event
     * @param strings
     */
    private void answer(AccessibilityEvent event, String[] strings) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a3b");
            for (AccessibilityNodeInfo item : list) {
                Log.e("item-->", item.toString());
                // paste
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                item.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                item.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", strings[0].substring(1) + "说：" + strings[1].substring(0, strings[1].length() - 1));
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                item.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                // send
                inputClick("com.tencent.mm:id/a3h");
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            }
        }
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        whatIsIt = NONE;
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
            Toast.makeText(this, nameList.get(i) + "领取了" + sumList.get(i), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 通过ID获取控件显示文字
     *
     * @param id
     */
    private void obtainNodeText(String id) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(id);
            for (AccessibilityNodeInfo item : list) {
                Log.e(">>>", "obtainNodeText: " + item.getText().toString());
                Toast.makeText(this, "领取到红包金额：" + item.getText().toString(), Toast.LENGTH_SHORT).show();
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

    private void backHome() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 1000);
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    /**
     * 获取List中最后一个红包，并进行模拟点击
     */
    private void getLastPacket(AccessibilityEvent event) {
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
     * 回复固定消息
     *
     * @param event
     */
    public void answer(AccessibilityEvent event) {
        // 回复我很忙
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a3b");
            for (AccessibilityNodeInfo item : list) {
                Log.e("item-->", item.toString());
                // paste
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                item.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                item.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", "我现在正忙，稍后联系你!");
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                item.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                // send
                inputClick("com.tencent.mm:id/a3h");
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            }
        }
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        whatIsIt = NONE;
    }

    /**
     * 服务被中断时回调
     */
    @Override
    public void onInterrupt() {
        mTcpClient.stopClient();
    }

    /**
     * 发送收到的消息给TCP服务器
     *
     * @param splits
     */
    private void sendTcpMsg(final String[] splits) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(splits[0] + "：" + splits[1]);
                }
            }
        }).start();
    }

    /**
     * 接收TCP服务器消息
     *
     * @param message
     */
    @Override
    public void messageReceived(String message) {

    }
}

class StringTest {
    String[] strs = new String[]{};
}
