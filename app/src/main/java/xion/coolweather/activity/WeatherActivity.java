package xion.coolweather.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import xion.coolweather.R;
import xion.coolweather.receiver.AutoUpdateReceiver;
import xion.coolweather.service.AutoUpdateService;
import xion.coolweather.util.HttpCallbackListener;
import xion.coolweather.util.HttpUtil;
import xion.coolweather.util.Utility;


/**
 * Created by Administrator on 2016/11/5.
 */

public class WeatherActivity extends Activity implements View.OnClickListener {
    private LinearLayout ll_weather_info;
    private TextView tv_cityname;//城市名
    private TextView tv_publishtime;//发布时间
    private TextView tv_weatherdesp;//天气描述
    private TextView tv_temp1,tv_temp2;//温度
    private TextView tv_currentdata;//当前日期
    private Button btn_switchcity;//切换城市按钮
    private Button btn_refresh;//刷新按钮
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_layout);
        initWidget();
        initData();
        setWidget();
    }
    private void initWidget() {
        ll_weather_info = (LinearLayout) findViewById(R.id.ll_weather_info);
        tv_cityname = (TextView) findViewById(R.id.tv_cityname);
        tv_publishtime = (TextView) findViewById(R.id.tv_publishtime);
        tv_currentdata = (TextView) findViewById(R.id.tv_current_date);
        tv_weatherdesp = (TextView) findViewById(R.id.tv_weather_desp);
        tv_temp1 = (TextView) findViewById(R.id.tv_temp1);
        tv_temp2 = (TextView) findViewById(R.id.tv_temp2);
        btn_refresh = (Button) findViewById(R.id.btn_refresh);
        btn_switchcity = (Button) findViewById(R.id.btn_switchcity);
    }

    private void initData() {
        //接受从ChooseAreaActivity传递过来的信息
        String countyCode = getIntent().getStringExtra("county_code");
        if(!TextUtils.isEmpty(countyCode)){
            //countyCode不为空就去查询具体城市的信息
            tv_publishtime.setText("同步中...");
            //为更好的用户体验，在信息查询到之前我们隐藏天气信息布局
            ll_weather_info.setVisibility(View.INVISIBLE);
            tv_cityname.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        }else {
            showWeather();
        }
    }
    private void setWidget() {
        btn_switchcity.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);
    }
//以县级代号组装address进行查询
private void queryWeatherCode(String countyCode) {
    String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
    queryFromServer(    address, "countyCode");
}
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    // 处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_publishtime.setText("同步失败");
                    }
                });
            }
        });
    }
    //以天气代号组装address进行查询
    private void queryWeatherInfo(String weatherCode) {
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        queryFromServer(address, "weatherCode");
    }

    private void showWeather() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        tv_cityname.setText( sharedPreferences.getString("city_name", "没获取到"));
       tv_temp1.setText(sharedPreferences.getString("temp1", ""));
        tv_temp2.setText(sharedPreferences.getString("temp2", ""));
        tv_weatherdesp.setText(sharedPreferences.getString("weather_desp", ""));
        tv_publishtime.setText("今天" + sharedPreferences.getString("publish_time", "") + "发布");
        tv_currentdata.setText(sharedPreferences.getString("current_date", ""));
        ll_weather_info.setVisibility(View.VISIBLE);
        tv_cityname.setVisibility(View.VISIBLE);
        //在这里开启自动更新
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_switchcity:
                Intent intent = new Intent(getApplication(),ChooseAreaActivity.class);
                intent.putExtra("from_weatherActivity",true);
                startActivity(intent);
                finish();
                break;
            case R.id.btn_refresh:
                tv_publishtime.setText("同步中...");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String weatherCode = sharedPreferences.getString("weather_code","");
                if (!TextUtils.isEmpty(weatherCode)){
                    queryWeatherInfo(weatherCode);
                }
                break;
            default:
                break;
        }
    }
}
