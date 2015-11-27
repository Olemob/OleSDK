package com.olemob.sdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.olemob.accountsdk.Constant;
import com.olemob.accountsdk.OleAccountManager;
import com.olemob.accountsdk.entity.OleConfig;
import com.olemob.accountsdk.entity.PassportInfo;
import com.olemob.accountsdk.interfaces.IShowToast;
import com.olemob.accountsdk.interfaces.MyToast;
import com.olemob.accountsdk.interfaces.PassportCallback;
import com.olemob.accountsdk.utils.ActivityUtil;
import com.olemob.accountsdk.utils.UIUtils;

/**
 * 用户SDK Demo
 *
 * @author liuweiping
 * @since 2015年8月3日
 */
public class UserAccountActivity extends Activity implements OnClickListener, IShowToast {

    private TextView mUidTV;
    private TextView mNickname;

    private PassportInfo mInfo;

    private MyToast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 判断屏幕方向
        int[] screenSize = ActivityUtil.getScreenSize(this);
        int layout = screenSize[0] > screenSize[1] ? R.layout.activity_account_land : R.layout.activity_account;
        setContentView(layout);
        // 初始化用户SDK
        initAccountSDK();
        initView();
        autoLogin();
    }

    private void initView() {
        Button btn = (Button)findViewById(R.id.main_login_btn);
        Button logout = (Button)findViewById(R.id.main_logout_btn);
        Button select = (Button)findViewById(R.id.main_select_btn);
        Button login = (Button)findViewById(R.id.main_onekey_login_btn);
        Button pay = (Button)findViewById(R.id.main_pay_btn);
        mUidTV = (TextView)findViewById(R.id.main_id_btn);
        mNickname = (TextView)findViewById(R.id.main_name_btn);
        login.setOnClickListener(this);
        btn.setOnClickListener(this);
        logout.setOnClickListener(this);
        select.setOnClickListener(this);
        pay.setOnClickListener(this);
    }

    /**
     * 调用autoLogin接口，在重新打开应用时的自动登录功能
     *
     * @since 2015年8月13日
     */
    private void autoLogin() {
        OleAccountManager.getInstance().autoLogin(this, new PassportCallback() {

            @Override
            public void onResult(int state, String msg) {
                if (state == PassportCallback.STATE_FAILED) {
                    showMsg(msg);
                } else {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            setData();
                        }
                    });
                }
            }
        });
    }

    private void setData() {
        mInfo = OleAccountManager.getInstance().getPassportInfo();
        if (mInfo != null) {
            mUidTV.setText(String.valueOf(mInfo.uid));
            if (TextUtils.isEmpty(mInfo.nickname)) {
                mNickname.setText(getResources().getString(R.string.ole_account_guest_name, mInfo.uid));
            } else {
                mNickname.setText(mInfo.nickname);
            }
        }
    }

    /**
     * 在启动时，或首次使用SDK前，需要对SDK的配置进行初始化。
     */
    private void initAccountSDK() {
        // 打开日志输出，正式发布时请去掉这一行，默认值为false
        Constant.DEBUG = true;
        OleConfig mConfig = new OleConfig();
        mConfig.setScreenType(getRequestedOrientation());
        OleAccountManager.getInstance().init(this, mConfig);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_login_btn:
                OleAccountManager.getInstance().startLogin(this);
                break;
            case R.id.main_logout_btn:
                int uid = OleAccountManager.getInstance().deleteAccount(this);
                if (uid != -1) {
                    mUidTV.setText("");
                    mNickname.setText("");
                    UIUtils.showToast(this, uid + "删除成功");
                } else {
                    UIUtils.showToast(this, "未发现已登录账号");
                }
                break;
            case R.id.main_select_btn:
                OleAccountManager.getInstance().switchAccount(this);
                break;
            case R.id.main_onekey_login_btn:
                mInfo = OleAccountManager.getInstance().startOneKeyLogin(this);
                if (mInfo != null) {
                    UIUtils.showToast(this, mInfo.nickname + "登录成功");
                    mUidTV.setText(String.valueOf(mInfo.uid));
                    mNickname.setText(mInfo.nickname);
                }
                break;
            case R.id.main_pay_btn:
                Intent intent = new Intent(this, PayActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.OTHER_RESULTCODE) {
            PassportInfo info;
            switch (resultCode) {
                case Constant.OTHER_RESULTCODE:
                    info = OleAccountManager.getInstance().getPassportInfo();
                    if (info == null) {
                        return;
                    }
                    if (TextUtils.isEmpty(info.nickname)) {
                        UIUtils.showToast(this, getString(R.string.ole_account_guest_success, info.uid));
                        mUidTV.setText(String.valueOf(info.uid));
                        mNickname.setText(getResources().getString(R.string.ole_account_guest_name, info.uid));
                    } else {
                        UIUtils.showToast(this, getString(R.string.ole_account_login_success, info.nickname));
                        mUidTV.setText(String.valueOf(info.uid));
                        mNickname.setText(info.nickname);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Toast封装,防止短时间内多次调用toast导致不停弹出的问题
     *
     * @param msgId 要显示的字符串的资源id
     * @see Toast
     */
    @Override
    public void showMsg(int msgId) {
        showMsg(getString(msgId));
    }

    /**
     * Toast封装,防止短时间内多次调用toast导致不停弹出的问题
     *
     * @param msgStr 要显示的字符串
     * @see Toast
     */
    @Override
    public synchronized void showMsg(String msgStr) {
        showMsg(msgStr, Toast.LENGTH_SHORT);
    }

    /**
     * Toast封装,防止短时间内多次调用toast导致不停弹出的问题
     *
     * @param msgId    要显示的字符串的资源id
     * @param duration Toast.LENGTH_SHORT or Toast.LENGTH_LONG
     * @see Toast
     */
    @Override
    public void showMsg(int msgId, int duration) {
        showMsg(getString(msgId), duration);
    }

    /**
     * Toast封装,防止短时间内多次调用toast导致不停弹出的问题
     *
     * @param msgStr   要显示的字符串
     * @param duration Toast.LENGTH_SHORT or Toast.LENGTH_LONG
     * @see Toast
     */
    @Override
    public synchronized void showMsg(String msgStr, int duration) {
        if (mToast == null) {
            mToast = new MyToast(this, msgStr, duration);
        } else {
            mToast.msgStr = msgStr;
            mToast.duration = duration;
        }
        runOnUiThread(mToast);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_account_land);
            initView();
            setData();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_account);
            initView();
            setData();
        }
    }
}
