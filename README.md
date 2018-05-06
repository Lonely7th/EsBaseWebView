这个项目包含WebView加载过程中可能遇到的各种异常以及处理方法，包括：

**1.网络连接断开和服务端异常**

**2.网络连接正常但无法访问Internet**

**3.弱网和服务器长时间无响应**

## 1.网络连接断开和服务端异常
这种情况下WebViewClient的执行顺序为：onPageStarted()>>onReceivedError()>>onPageFinished()
这类异常相对而言比较容易捕获，当网络连接断开或者服务端返回异常时，WebView会自动捕获并执行onReceivedError方法。
对于这种情况我们选择在onReceivedError方法中将加载状态置为加载失败，接下来在onPageFinished方法中判断加载状态并执行相应的处理。
```
@Override
public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
    super.onReceivedError(view, request, error);
    loadError = true;
}
```
```
@Override
public void onPageFinished(WebView view, String url) {
    super.onPageFinished(view, url);
    if(getProgress() >= 100){
        if(loadError){
            onLoadError();
        }else{
            onLoadSuccess();
        }
    }
}
```
## 2.网络连接正常但无法访问Internet
这种情况下WebViewClient的执行顺序为：onPageStarted()>>onPageFinished()
在网络连接正常但无法访问Internet的情况下，WebView并不会捕获异常，并且会执行onPageFinished方法表示加载任务已经完成，这种情况下我们无法通过onReceivedError方法判断加载失败的状态。
对于这种情况，我们需要判断页面是否真的已经加载完成并给与用户正确的提示，我们在WebView每次执行onPageFinished方法时使用ping命令来判断网络是否可用，如果网络可用再根据加载状态参数做相应的处理。
```
@Override
public void onPageFinished(WebView view, String url) {
    super.onPageFinished(view, url);
    if(getProgress() >= 100){
         new Thread(new Runnable() {
            @Override
            public void run() {
                if(NetworkUtils.isNetworkConnected(getContext()) && NetworkUtils.isNetworkOnline()){//判断网络连接是否异常
                    if(loadError){
                        //加载失败
                     }else{
                        //加载成功
                    }
               }else{
                    //网络异常
               }
            }
        }).start();
   }
}
```
## 3.弱网和服务器长时间无响应
这种情况下WebViewClient的执行顺序为：onPageStarted()>>长时间等待>>onReceivedError()>>onPageFinished()
在服务器长时间无响应的情况下，WebView会在执行完onPageStarted方法后进入长时间等待（大约2分钟），超过等待时间后会先后执行onReceivedError方法和onPageFinished方法。
从逻辑上看这其实是一种正常的流程，WebView捕获到请求超时异常后执行onReceivedError方法，之后执行onPageFinished方法完成加载任务，但这套流程的问题在于等待时间过长且无法设置，我们需要自定义请求超时时间。
对于这种情况，我们在onPageStarted方法中创建一个Message并延迟5000ms（超时时间）发送，Message发送后判断WebView是否已经加载完成，如果未完成则视为请求超时。
```
@Override
public void onPageStarted(WebView view, String url, Bitmap favicon) {
    super.onPageStarted(view, url, favicon);
    webHandler.sendEmptyMessageDelayed(408, TIMEOUT);
}
```
```
private Handler webHandler = new Handler(){
   @Override
   public void handleMessage(Message msg) {
       switch (msg.what){
            case 408://请求超时
                if (getProgress() < 100) {
                   loadError = true;
                   onLoadError();
                }
                break;
       }
    }
};
```
## 4.展示异常提示页面
最后我们谈一谈如何展示异常提示页面，当我们捕获到异常时，展示一个友好的异常提示页面是有助于提升用户体验的，由于WebView继承自ViewGroup，所以这里我们比较倾向于向WebView中addView的方式。
加载异常：
```
private void onLoadError(int flag){
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    while (getChildCount() >= 1) {
        removeViewAt(0);
    }
    addView(netErrorView, 0, lp);
}
```
正常加载：
```
private void onLoadSuccess(){
    while (getChildCount() >= 1) {
        removeViewAt(0);
    }
}
```
