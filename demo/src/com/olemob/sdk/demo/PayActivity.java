package com.olemob.sdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.olemob.accountsdk.OleAccountManager;
import com.olemob.accountsdk.entity.PassportInfo;
import com.olemob.accountsdk.utils.LogUtil;
import com.olemob.paysdk.OlePay;
import com.olemob.paysdk.entity.Currency;
import com.olemob.paysdk.entity.OlePayConfig;
import com.olemob.paysdk.entity.OrderInfo;
import com.olemob.paysdk.entity.OrderStateInfo;
import com.olemob.paysdk.entity.PayPlatform;
import com.olemob.paysdk.entity.ProductInfo;
import com.olemob.paysdk.interfaces.IOrderManager;
import com.olemob.paysdk.interfaces.InitCallback;
import com.olemob.paysdk.interfaces.OrderStateCallback;
import com.olemob.paysdk.interfaces.PayResultCallback;
import com.olemob.paysdk.manager.PayConstant;
import com.olemob.paysdk.manager.PayssionOrderManager;

import java.util.Arrays;

/**
 * PayDemo首页
 *
 * @author liuweiping
 * @since 2015年8月13日
 */
public class PayActivity extends Activity implements OnClickListener {
    // 海外版HaloPay平台的KEY
    private static final String KEY_HALOPAY = "olemob_halopay_001";
    private static final String APPSECRET_HALOPAY = "NaRllryetpCWLrC/AMQp8uEI7F1KEmEo";
    // 国内版iApppay平台的KEY
    private static final String KEY_IAPPPAY = "olemob_iapppay_001";
    private static final String APPSECRET_IAPPPAY = "8VZPoviu47kE8HaX/LSNL+EI7F1KEmEo";
    // 国内版Payssion平台的KEY
    private static final String KEY_PAYSSION = "olemob_payssion_001";
    private static final String APPSECRET_PAYSSION = "kxaA0wVtpYVWS9LM4dscBMBkr7zXh+M7";
    private EditText mProductName;
    private EditText mProductPrice;
    private EditText mUidET;

    private OrderInfo order;
    private Spinner mCurrencySP;
    private TextView mResultTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 横竖屏的设置
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        // findViewById
        mUidET = (EditText)findViewById(R.id.main_product_uid);
        mProductName = (EditText)findViewById(R.id.main_product_name);
        mProductName = (EditText)findViewById(R.id.main_product_name);
        mProductPrice = (EditText)findViewById(R.id.main_product_price);
        mCurrencySP = (Spinner)findViewById(R.id.main_product_currency);
        mResultTV = (TextView)findViewById(R.id.main_product_result);
        final Button payBtn = (Button)findViewById(R.id.main_product_start_pay);
        final Button checkPayBtn = (Button)findViewById(R.id.main_product_checkpay_status);
        // 货币单位的数组
        String[] currencyArray = Currency.getCurrencyArray();
        mCurrencySP.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currencyArray));
        // 默认选择美元
        int index = Arrays.binarySearch(currencyArray, Currency.USD.name());
        mCurrencySP.setSelection(index);
        payBtn.setOnClickListener(this);
        checkPayBtn.setOnClickListener(this);
        payBtn.setEnabled(false);
        checkPayBtn.setEnabled(false);

        // 获取用户UID
        PassportInfo info = OleAccountManager.getInstance().getPassportInfo();
        if (info != null) {
            mUidET.setText(String.valueOf(info.uid));
        }

        Toast.makeText(this, "正在初始化OlePay...", Toast.LENGTH_SHORT).show();
        // TODO: 打开支付SDK的DEBUG模式，正式发布的时候请去掉这一行
        PayConstant.DEBUG = true;
        // 初始化OlePay，海外版为PayPlatform.Halo; 国内版为PayPlatform.iapppay; SDK V1.2 新增Payssion
        String oleKey = KEY_PAYSSION;
        String oleSecret = APPSECRET_PAYSSION;
        PayPlatform platform = PayPlatform.Payssion;
        setTitle(platform.name()); // 设置页面标题
        OlePayConfig config = new OlePayConfig(getRequestedOrientation(), oleKey, oleSecret, platform);
        OlePay.getInstance().init(this, config, new InitCallback() {

            @Override
            public void onResult(int code, String msg) {
                switch (code) {
                    case 1:
                        payBtn.setEnabled(true);
                        checkPayBtn.setEnabled(true);
                        mResultTV.setText("初始化成功，可以下单");
                        break;
                    case 0:
                    default:
                        Toast.makeText(PayActivity.this, "初始化失败 - " + msg, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

    /**
     * 自定义Payssion的支付渠道列表.
     */
    private void setPayssionPaymentList() {
        IOrderManager orderManager = OlePay.getInstance().getOrderManager();
        // 仅在Payssion平台下支持此设置
        if (orderManager != null && orderManager instanceof PayssionOrderManager) {
            // 自定义以下三个支付渠道,会在下订单前提示用户选择一个.
            PayssionOrderManager.PaymentItem[] list = new PayssionOrderManager.PaymentItem[] { //
                PayssionOrderManager.PaymentItem.americanexpress_br, //
                PayssionOrderManager.PaymentItem.cashu, //
                PayssionOrderManager.PaymentItem.cherrycredits //
            };
            ((PayssionOrderManager)orderManager).setPaymentList(list);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_product_start_pay:
                if (!OlePay.getInstance().isInitReady()) {
                    Toast.makeText(this, "OlePay没有初始化！", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, "正在支付...", Toast.LENGTH_SHORT).show();

                String uid = mUidET.getText().toString();
                String name = mProductName.getText().toString();
                float price = 0;
                try {
                    price = Float.parseFloat(mProductPrice.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                String currency = (String)mCurrencySP.getSelectedItem();
                // 设置商品详情
                ProductInfo.Builder builder = new ProductInfo.Builder();
                builder.setName(name).setDescription("Olemob Product Demo Description").setPid("abcdefg")
                    .setPrice(price).setCurrency(currency);
                ProductInfo info = builder.create();

                // TODO: 仅在Payssion支付平台,可以自定义支付方式
//                setPayssionPaymentList();
                OlePay.getInstance().placeOrder(this, uid, info, new PayResultCallback() {

                    @Override
                    public void onResult(int code, OrderInfo order, String errorMsg) {
                        PayActivity.this.order = order;
                        if (code != OlePay.PAY_SUCCESS) {
                            Toast.makeText(getApplicationContext(), "支付失败 - " + errorMsg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "支付成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case R.id.main_product_checkpay_status:
                if (!OlePay.getInstance().isInitReady()) {
                    Toast.makeText(this, "OlePay没有初始化！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (this.order == null) {
                    Toast.makeText(this, "请先创建订单", Toast.LENGTH_SHORT).show();
                    return;
                }
                OlePay.getInstance().checkPay(this, order, new OrderStateCallback() {

                    @Override
                    public void onResult(int state, OrderStateInfo result) {
                        if (result == null) {
                            mResultTV.setText("查询订单失败 - " + state);
                            return;
                        }
                        mResultTV.setText(result.toString());
                    }
                });
                break;

            default:
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogUtil.e("newConfig = " + newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 某些支付渠道需要在此处调用onActivityResult, 比如Payssion
        OlePay.getInstance().onActivityResult(this, requestCode, resultCode, data);
    }
}
