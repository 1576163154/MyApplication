package xion.coolweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import xion.coolweather.database.CoolWeatherDB;
import xion.coolweather.model.City;
import xion.coolweather.model.County;
import xion.coolweather.model.Province;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Administrator on 2016/9/21.
 */
public class Utility {
    /*
        解析处理服务器返回的数据
     */
    public synchronized static boolean handleProvinceResponse(CoolWeatherDB coolWeatherDB,String response){
        if(!TextUtils.isEmpty(response)){
            String [] allProvinces = response.split(",");
            if(allProvinces != null && allProvinces.length > 0){
                for (String p:allProvinces) {
                    String[] arrayP = p.split("\\|");//将 每个这样1901|南京的数据以"|"分开,依次将1901和南京装入array
                    Province province = new Province();
                    province.setProvinceCode(arrayP[0]);
                    province.setProvinceName(arrayP[1]);
                    coolWeatherDB.saveProvince(province);
                }
            return true;
            }
        }
        return false;
    }
    //按provinceId解析所有城市
    public synchronized static boolean handleCityResponse(CoolWeatherDB coolWeatherDB,String response,int provinceId){
        if(!TextUtils.isEmpty(response)){
            String [] allCities = response.split(",");//将 1901|南京,1902|无锡.... 这样数据先以","号分开
            if(allCities != null&&allCities.length > 0){
                for(String c : allCities){
                    String [] arrayC = c.split("\\|");
                    City city = new City();
                    city.setCityCode(arrayC[0]);
                    city.setCityName(arrayC[1]);
                    city.setProvinceId(provinceId);
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }
    public synchronized static boolean handleCountyResponse(CoolWeatherDB coolWeatherDB,String response,int cityId){
        if(!TextUtils.isEmpty(response)){
            String[] allCounties = response.split(",");
            if(allCounties != null && allCounties.length > 0){
                for(String co : allCounties){
                    String [] arrayCo = co.split("\\|");
                    County county = new County();
                    county.setCountyCode(arrayCo[0]);
                    county.setCountyName(arrayCo[1]);
                    county.setCityId(cityId);
                    coolWeatherDB.saveCounty(county);
                }
                return true;
            }
        }
        return false;
    }
    //解析返回的json文件中的天气数据
    public static void handleWeatherResponse(Context context, String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
            String cityName = weatherInfo.getString("city");
            String weatherCode = weatherInfo.getString("cityid");
            String temp1 = weatherInfo.getString("temp1");
            String temp2 = weatherInfo.getString("temp2");
            String weatherDesp = weatherInfo.getString("weather");
            String publishTime = weatherInfo.getString("ptime");
            saveWeatherInfo(context, cityName, weatherCode, temp1, temp2,
                    weatherDesp, publishTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将服务器返回的所有天气信息存储到SharedPreferences文件中。
     */
    public static void saveWeatherInfo(Context context, String cityName,
                                       String weatherCode, String temp1, String temp2, String weatherDesp,
                                       String publishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.commit();
    }
}
