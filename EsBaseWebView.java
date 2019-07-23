

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * Time ： 2018/4/24 .
 * Author ： JN Zhang .
 * Description ： .
 */

public class EsBaseWebView extends WebView {
    private static final String TAG = "EsBaseWebView";
    private static final long TIMEOUT = 5000;

    private OnEsWebViewClientListener onEsWebViewClientListener = null;
    //页面加载失败时显示
    private View serverErrorView = null;
    private View netErrorView = null;
    //页面加载失败标志
    private boolean loadError = false;
    //页面正在加载，避免多次重复加载
    private boolean isLoading = true;

    @SuppressLint("HandlerLeak")
    private Handler webHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 200://加载成功
                    onLoadSuccess();
                    break;
                case 500://服务器异常
                    onLoadError(msg.arg1);
                    break;
                case 408://请求超时
                    if (getProgress() < 100) {
                        loadError = true;
                        isLoading = false;
                        onLoadError(2);
                    }
                    break;
            }
        }
    };

    public EsBaseWebView(Context context) {
        super(context);
        initView();
    }

    public EsBaseWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView(){
        WebSettings setting = getSettings();
        String UA = setting.getUserAgentString();
        setting.setUserAgentString(UA+";XIAODAI");
        setting.setJavaScriptEnabled(true);
        setting.setDomStorageEnabled(true);
        setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.i(TAG,"onPageStarted");
                super.onPageStarted(view, url, favicon);
                webHandler.sendEmptyMessageDelayed(408, TIMEOUT);
                if(onEsWebViewClientListener != null){
                    onEsWebViewClientListener.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG,"onPageFinished");
                super.onPageFinished(view, url);
                if(getProgress() >= 100){
                    isLoading = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(NetworkUtils.isNetworkConnected(getContext()) && NetworkUtils.isNetworkOnline()){//判断网络连接是否异常
                                if(loadError){
                                    Message msg = new Message();
                                    msg.what = 500;
                                    msg.arg1 = 2;
                                    webHandler.sendMessage(msg);
                                }else{
                                    webHandler.sendEmptyMessage(200);
                                }
                            }else{
                                Message msg = new Message();
                                msg.what = 500;
                                msg.arg1 = 1;
                                webHandler.sendMessage(msg);
                            }
                        }
                    }).start();
                }
                if(onEsWebViewClientListener != null){
                    onEsWebViewClientListener.onPageFinished(view, url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(onEsWebViewClientListener != null){
                    return onEsWebViewClientListener.shouldOverrideUrlLoading(view, url);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.i(TAG,"onReceivedError:"+error.getErrorCode());
                }
                super.onReceivedError(view, request, error);
                loadError = true;
            }
        });
        //初始化服务器异常页面
        LayoutInflater inflater = LayoutInflater.from(getContext());
        serverErrorView = inflater.inflate(R.layout.server_error_layout,null);
        serverErrorView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isLoading){
                    reload();
                }
            }
        });
        //初始化网络连接异常的页面
        netErrorView = inflater.inflate(R.layout.net_error_layout,null);
        netErrorView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isLoading){
                    reload();
                }
            }
        });
    }

    @Override
    public void reload() {
        super.reload();
        isLoading = true;
        loadError = false;
    }

    /**
     * 加载失败
     * @param flag 1.网络连接异常  2.服务器异常
     */
    private void onLoadError(int flag){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        while (getChildCount() >= 1) {
            removeViewAt(0);
        }
        switch (flag){
            case 1:
                addView(netErrorView, 0, lp);
                break;
            case 2:
                addView(serverErrorView, 0, lp);
                break;
        }
    }

    /**
     * 加载成功
     */
    private void onLoadSuccess(){
        while (getChildCount() >= 1) {
            removeViewAt(0);
        }
    }

    /**
     * 设置加载过程监听
     */
    public void setOnEsWebViewClientListener(OnEsWebViewClientListener onEsWebViewClientListener){
        this.onEsWebViewClientListener = onEsWebViewClientListener;
    }

    public interface OnEsWebViewClientListener{
        void onPageStarted(WebView view, String url, Bitmap favicon);
        void onPageFinished(WebView view, String url);
        boolean shouldOverrideUrlLoading(WebView view, String url);
    }
}
