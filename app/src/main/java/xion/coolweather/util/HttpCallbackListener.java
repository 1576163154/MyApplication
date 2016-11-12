package xion.coolweather.util;

/**
 * Created by Administrator on 2016/9/21.
 */
public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);
}
