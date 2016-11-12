package xion.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import xion.coolweather.R;
import xion.coolweather.database.CoolWeatherDB;
import xion.coolweather.model.City;
import xion.coolweather.model.County;
import xion.coolweather.model.Province;
import xion.coolweather.util.HttpCallbackListener;
import xion.coolweather.util.HttpUtil;
import xion.coolweather.util.Utility;

/**
 * Created by Administrator on 2016/9/21.
 */
public class ChooseAreaActivity extends AppCompatActivity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;//进度条
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> datalist = new ArrayList<String>();//List 是接口不能实例化，只能实例为一个ArrayList或LinkedList
    //这是一个显示在主页面的一个list，由于用户会随意点击省份，城市。故需要动态的将list赋给list
    private List<Province> provinceList;//省列表
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;
    private TextView tv_title;
    private ListView lv_main;
    private boolean isFromWeatherActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //如果之前已经选择某一城市，那么在加载choose_are.xml之前就直接加载weather_layout.xml
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weatherActivity",false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("city_selected",false) && !isFromWeatherActivity){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.choose_area);
        lv_main = (ListView) findViewById(R.id.lv_main);
        tv_title = (TextView) findViewById(R.id.tv_title);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, datalist);
        lv_main.setAdapter(adapter);
        coolWeatherDB = coolWeatherDB.getInstance(this);
        lv_main.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String countyCode = countyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();//初始先加载province数据
    }

    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvince();
        Log.d("data", String.valueOf(provinceList.size()));
        if (provinceList.size() > 0) {
            Log.d("data", String.valueOf(datalist.size()));
            datalist.clear();//将list中所有元素清空
            for (Province province : provinceList) {
                datalist.add(province.getProvinceName());
            }
        } else {
            queryFromServer(null, "province");
        }
        adapter.notifyDataSetChanged();
        lv_main.setSelection(0);
        tv_title.setText("中国");//设置此时的标题
        currentLevel = LEVEL_PROVINCE;
    }

    private void queryCities() {
        cityList = coolWeatherDB.loadCity(selectedProvince.getId());
        if (cityList.size() > 0) {
            datalist.clear();
            for (City city : cityList) {
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            lv_main.setSelection(0);
            tv_title.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    private void queryCounties() {
        countyList = coolWeatherDB.loadCounty(selectedCity.getId());
        if (countyList.size() > 0) {
            datalist.clear();
            for (County county : countyList) {
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            lv_main.setSelection(0);
            tv_title.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    private void queryFromServer(final String code, final String type) {
        String address;
        if (!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code +
                    ".xml";
        } else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if (type.equals("province")) {
                    result = Utility.handleProvinceResponse(coolWeatherDB, response);
                } else if (type.equals("city")) {
                    result = Utility.handleCityResponse(coolWeatherDB, response, selectedProvince.getId());
                } else if (type.equals("county")) {
                    result = Utility.handleCountyResponse(coolWeatherDB, response, selectedCity.getId());
                }
                if (result) {
                    //通过runOnUiThread回到主线程现修改界面ui
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度条
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryCities();
        }else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        }else {
            if (isFromWeatherActivity) {
                Intent intent = new Intent(this, WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
