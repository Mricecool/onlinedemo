package com.gogowan.petrochina.context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.volley.manager.LoadController;
import com.android.volley.manager.RequestManager;
import com.gogowan.petrochina.R;
import com.gogowan.petrochina.base.BaseActivity;
import com.gogowan.petrochina.base.JPushUtils;
import com.gogowan.petrochina.base.PalUtils;
import com.gogowan.petrochina.base.TitleActivity;
import com.gogowan.petrochina.bean.GasPriceResult;
import com.gogowan.petrochina.bean.LoginRequest;
import com.gogowan.petrochina.bean.LoginResult;
import com.gogowan.petrochina.bean.MsgType;
import com.gogowan.petrochina.bean.UpdateRequest;
import com.gogowan.petrochina.bean.UpdateResult;
import com.gogowan.petrochina.custom.DialogMsg;
import com.gogowan.petrochina.custom.StatusBarUtils;
import com.gogowan.petrochina.service.DownloadService;
import com.ioid.utils.JsonUtils;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import java.util.Map;

public class LoginActivity extends TitleActivity implements RequestManager.RequestListener {

    private static final boolean DEBUG = false;
    private static final boolean DEBUG1 = false;
    private LoginRequest loginRequest = new LoginRequest();
    private LoadController mLoadController;
    private EditText login_account_et, login_pwd_et;
    private static final int LOGIN_RQT = 1;

    private static final int UPDATE_RQT = 2;
    //版本名
    private String versionNameStr;
    //版本号
    private int versionCode;

    @Override
    protected int setContentViewId() {
        return R.layout.activity_login;
    }

    @Override
    protected boolean initData(Intent intent, Bundle savedInstanceState) {
        StatusBarUtils.setColor(this, ContextCompat.getColor(this, R.color.item_pressed));
        return true;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String p = prefs.getString("port", "http://123.57.142.199:8128");
        if (!TextUtils.isEmpty(p)) {
            //PalUtils.REQUEST_URI = p;
        }
        setTitle("登录", false);
        initListener();
        findViewById(R.id.login_bt).setOnClickListener(this);
        findViewById(R.id.login_bt2).setOnClickListener(this);
        findViewById(R.id.login_bt3).setOnClickListener(this);
        login_account_et = getView(R.id.login_account_et);
        login_pwd_et = getView(R.id.login_pwd_et);
        LoginRequest loginRequest = PalUtils.getLoginRequest(getApplicationContext());
        if (loginRequest != null) {
            login_account_et.setText(loginRequest.getLoginName());
            login_pwd_et.setText(loginRequest.getLoginPwd());
        }
        initDialogProgress("正在登录" + "\n" + "请稍候......");
        getVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onClicked(View v) {
        switch (v.getId()) {
            // 登录
            case R.id.login_bt:
                String account = login_account_et.getText().toString();
                account = account.replace(" ", "");
                String pwd = login_pwd_et.getText().toString();
                login(account, pwd);
//                if (PalUtils.DEBUG && DEBUG) {
//                    startActivity(new Intent(this, MarkActivity.class));
//                    finish();
//                }
                break;
            // 重置
            case R.id.login_bt2:
                login_account_et.setText("");
                login_pwd_et.setText("");
                loginRequest.setLoginName("");
                loginRequest.setLoginPwd("");
                PalUtils.saveLoginRequest(getApplicationContext(), loginRequest);
                break;
            case R.id.login_bt3:
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("端口设置");
                View vi = LayoutInflater.from(LoginActivity.this).inflate(R.layout.layout_port, null);
                final EditText txt = (EditText) vi.findViewById(R.id.txtPort);
                txt.setText(PalUtils.REQUEST_URI);
                builder.setView(vi);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String port = txt.getText().toString().trim();
                        if (!TextUtils.isEmpty(port)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                    LoginActivity.this).edit();
                            editor.putString("port", port);
                            editor.commit();
                            //PalUtils.REQUEST_URI = port;
                        }

                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
        }
    }

    @Override
    protected void handleMessage(BaseActivity activity, Message msg) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadController != null) {
            mLoadController.cancel();
        }
    }

    @Override
    public void onRequest() {

    }

    @Override
    public void onSuccess(String response, Map<String, String> headers, String url, int actionId) {
        hideDialogProgress();
        if (PalUtils.isEmpty(response)) {
            PalUtils.showShortToast(getApplicationContext(), getString(R.string.connect_error));
        } else {
            switch (actionId) {
                case LOGIN_RQT:
                    LoginResult loginResult = (LoginResult) PalUtils.getResponse(response, LoginResult.class);
                    if (loginResult != null && loginResult.getResult() >= 0) {
                        PalUtils.saveLoginRequest(getApplicationContext(), loginRequest);
                        PalUtils.saveLoginResult(getApplicationContext(), loginResult);
                        JPushUtils.initJPushAlias(getApplication(), loginResult.getUserID() + "");
                        if (loginResult.getPower() == 2) {
                            PalUtils.saveLoginStatus(getApplicationContext());
                            startActivity(new Intent(this, ApprovalActivity.class));
                            finish();
                        } else if (loginResult.getPower() == 1) {
                            PalUtils.saveLoginStatus(getApplicationContext());
                            if (PalUtils.DEBUG && DEBUG1) {
                                startActivity(new Intent(this, MarkActivity.class));
                                finish();
                                return;
                            }
                            if (PalUtils.DEBUG && DEBUG) {
                                if (loginResult.getEnterpriseOperate() == null) {
                                    PalUtils.showShortToast(getApplicationContext(), "该帐号未绑定加油站信息");
                                    GasPriceResult gasPriceResult = new GasPriceResult();
                                    gasPriceResult.setGasStation(getString(R.string.test_short));
                                    loginResult.setEnterpriseOperate(gasPriceResult);
                                    PalUtils.saveLoginResult(getApplicationContext(), loginResult);
                                }
                                startActivityForResult(new Intent(this, OwnMarkActivity.class), 0);
                                return;
                            }
                            if (loginResult.getEnterpriseOperate() == null) {
                                PalUtils.showShortToast(getApplicationContext(), "该帐号未绑定加油站信息");
                                return;
                            }
                            if (loginResult.isFirstLogin()) {
                                startActivityForResult(new Intent(this, OwnMarkActivity.class), 0);
                            } else {
                                startActivity(new Intent(this, MarkActivity.class));
                                finish();
                            }
                        } else {
                            PalUtils.showShortToast(getApplicationContext(), loginResult.getResultInfo());
                        }
                    } else if (loginResult != null) {
                        PalUtils.showShortToast(getApplicationContext(), loginResult.getResultInfo());
                    } else {
                        // wifi redirect 其他页面不管了
                    }
                    break;
                case UPDATE_RQT:
                    UpdateResult updateResult = (UpdateResult) PalUtils.getResponse(response, UpdateResult.class);
                    if (updateResult != null && updateResult.getResult() >= 0) {
                        showDialogMsg(updateResult);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onError(String errorMsg, String url, int actionId) {
        hideDialogProgress();
        switch (actionId) {
            case LOGIN_RQT:
                PalUtils.showShortToast(getApplicationContext(), getString(R.string.connect_error));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, PriceSettingActivity.class);
            intent.putExtra(PalUtils.TYPE_ITT, 1);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 登录
     */
    private void login(String account, String pwd) {
        if (PalUtils.isEmpty(account)) {
            PalUtils.showShortToast(getApplicationContext(), "请输入用户名");
            return;
        }
        if (PalUtils.isEmpty(pwd)) {
            PalUtils.showShortToast(getApplicationContext(), "请输入密码");
            return;
        }
        //判断中文用户名
/*        if (!PalUtils.isAccount(account)) {
            PalUtils.showShortToast(getApplicationContext(), "用户名格式不正确");
            return;
        }*/
        if (!PalUtils.isPassword(pwd)) {
            PalUtils.showShortToast(getApplicationContext(), "密码格式不正确");
            return;
        }
        showDialogProgress();
        loginRequest.setLoginName(account);
        loginRequest.setLoginPwd(pwd);
        String messageStr = PalUtils.getRequest(loginRequest, MsgType.LoginRequest);
        mLoadController = RequestManager.getInstance().post(MsgType.REQUEST_URL,
                messageStr, this, LOGIN_RQT);
    }

    private ImageView login_logo_im;
    private ScrollView login_sv;
    private int screenHeight; //屏幕高度
    private int keyHeight; //软件盘弹起后所占高度
    private float scale = 0.6f; //logo缩放比例

    /**
     * 键盘监听
     */
    private void initListener() {
        screenHeight = this.getResources().getDisplayMetrics().heightPixels; //获取屏幕高度
        keyHeight = screenHeight / 3;//弹起高度为屏幕高度的1/3

        login_sv = getView(R.id.login_sv);
        login_logo_im = getView(R.id.login_logo_im);

        login_sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        findViewById(R.id.root).addOnLayoutChangeListener(new ViewGroup.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
              /* old是改变前的左上右下坐标点值，没有old的是改变后的左上右下坐标点值
              现在认为只要控件将Activity向上推的高度超过了1/3屏幕高，就认为软键盘弹起*/
                if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {
                    PalUtils.showLogE("wenzhihao", "up------>" + (oldBottom - bottom));
                    getWeakRefHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            login_sv.smoothScrollTo(0, login_sv.getHeight());
                        }
                    }, 0);
                    zoomIn(login_logo_im, (oldBottom - bottom) - keyHeight);
                } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
                    PalUtils.showLogE("wenzhihao", "down------>" + (bottom - oldBottom));
                    //键盘收回后，logo恢复原来大小，位置同样回到初始位置
                    getWeakRefHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            login_sv.smoothScrollTo(0, login_sv.getHeight());
                        }
                    }, 0);
                    zoomOut(login_logo_im, (bottom - oldBottom) - keyHeight);
                }
            }
        });
    }

    /**
     * 缩小
     *
     * @param view
     */
    public void zoomIn(final View view, float dist) {
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();
        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, scale);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, scale);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", 0.0f, -dist);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX);
        mAnimatorSet.play(mAnimatorScaleX).with(mAnimatorScaleY);
        mAnimatorSet.setDuration(200);
        mAnimatorSet.start();
    }

    /**
     * f放大
     *
     * @param view
     */
    public void zoomOut(final View view, float dist) {
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();

        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", scale, 1.0f);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", scale, 1.0f);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), 0);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX);
        mAnimatorSet.play(mAnimatorScaleX).with(mAnimatorScaleY);
        mAnimatorSet.setDuration(200);
        mAnimatorSet.start();
    }


    /**
     * 版本比较，获取下载链接
     * 选择显示的布局
     */
    private void getVersion() {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //获得当前版本
        versionNameStr = info.versionName;
        versionCode = info.versionCode;
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setVersion(versionNameStr);
        updateRequest.setVersionCode(versionCode);

        String messageStr = PalUtils.getRequest(updateRequest, MsgType.VersionUpdate);
        mLoadController = RequestManager.getInstance().post(MsgType.REQUEST_URL,
                messageStr, this, UPDATE_RQT);
    }

    /**
     * 版本提示
     */
    private void showDialogMsg(final UpdateResult updateResult) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View convertview = inflater.inflate(R.layout.dialog_msg,
                null);
        DialogMsg.Builder builder = new DialogMsg.Builder(this);
        builder.setContentView(convertview);
        builder.setMsgColor(getResources().getColor(R.color.t_black1));
        builder.setPositiveButton("立刻升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (PalUtils.isDClick()) {
                    return;
                }
                update(updateResult);
            }
        });
        if (!updateResult.getIsUpdate()) {
            builder.setMessage("可以升级到" + updateResult.getVersion() + "版本");
            builder.setNegativeButton("暂不升级", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (PalUtils.isDClick()) {
                        return;
                    }
                }
            });
            DialogMsg dialogMsg = builder.create();
            dialogMsg.setCanceledOnTouchOutside(false);
            dialogMsg.show();
        } else {
            builder.setMessage("需要升级到" + updateResult.getVersion() + "版本");
            DialogMsg dialogMsg = builder.create();
            dialogMsg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            dialogMsg.setCanceledOnTouchOutside(false);
            dialogMsg.show();
        }
    }

    /**
     * 下载更新
     */
    private void update(UpdateResult updateResult) {
        //如果地址为空则不响应
        if (TextUtils.isEmpty(updateResult.getUrl())) {
            return;
        }
        if (PalUtils.NETTYPE_NULL == PalUtils.getNetworkType(LoginActivity.this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.connect_error), Toast.LENGTH_SHORT).show();
            return;
        }
        if (PalUtils.NETTYPE_WIFI != PalUtils.getNetworkType(LoginActivity.this)) {
            //不是在wifi下给提示
            showDialogMsg1(updateResult);
            return;
        }
        initDownload(updateResult);
    }

    /**
     * 移动网络提示
     */
    private void showDialogMsg1(final UpdateResult updateResult) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View convertview = inflater.inflate(R.layout.dialog_msg,
                null);
        DialogMsg.Builder builder = new DialogMsg.Builder(this);
        builder.setContentView(convertview);
        builder.setMessage("您正在使用移动网络下载\n可能产生较高流量费用");
        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (PalUtils.isDClick()) {
                    return;
                }
                initDownload(updateResult);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (PalUtils.isDClick()) {
                    return;
                }
            }
        });
        DialogMsg dialogMsg = builder.create();
        dialogMsg.show();
    }


    /**
     * 启动Service
     * 开始下载
     */
    private void initDownload(UpdateResult updateResult) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("Key_App_Name", getString(R.string.app_name) + " " + PalUtils.VERSION_HEAD + updateResult.getVersion());
        intent.putExtra("Key_Down_Url", updateResult.getUrl());
        startService(intent);
        Toast.makeText(getApplicationContext(), "正在下载，请稍候", Toast.LENGTH_SHORT).show();
    }
}
