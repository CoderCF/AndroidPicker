package com.cf.androidpickerlibrary;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.cf.androidpickerlibrary.utils.AssetsUtils;
import com.cf.androidpickerlibrary.wheelview.OnItemSelectedListener;
import com.cf.androidpickerlibrary.wheelview.WheelView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 描    述：
 * 创建日期：2017/7/20 14:26
 * 作    者：Chengfu
 * 邮    箱：
 * 备    注：
 */
public class AddressPicker extends Dialog implements View.OnClickListener{

    /**
     * 所有省
     */
    private String[] mProvinceDatas;
    /**
     * key - 省 value - 市
     */
    private Map<String, String[]> mCitisDatasMap = new HashMap<String, String[]>();
    /**
     * key - 市 values - 区
     */
    private Map<String, String[]> mDistrictDatasMap = new HashMap<String, String[]>();

    private Context context;
    private View view;
    private WheelView mViewProvince;
    private WheelView mViewCity;
    private WheelView mViewDistrict;
    private TextView mTvConfirm;
    private TextView mTvCancel;
    private OnAddressListener onAddressListener;

    public AddressPicker(Context context) {
        super(context, R.style.transparentWindowStyle);

        this.context = context;
        view=View.inflate(context,R.layout.layout_address_picker,null);

        initCityData();

        initView();
        initData();
        setListener();
        this.setContentView(view);

        this.setCanceledOnTouchOutside(true);

        //从底部弹出
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);  //此处可以设置dialog显示的位置
        window.setWindowAnimations(R.style.windowAnimationStyle);  //添加动画

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_confirm) {
            if (onAddressListener != null) {

                String mCurrentProvinceName = mProvinceDatas[mViewProvince.getCurrentItem()];

                int cityPos = mViewCity.getCurrentItem();
                cityPos = cityPos >= mCitisDatasMap.get(mCurrentProvinceName).length - 1 ? mCitisDatasMap.get(mCurrentProvinceName).length - 1 : cityPos;
                String mCurrentCityName = mCitisDatasMap.get(mCurrentProvinceName)[cityPos];

                int districtPos = mViewDistrict.getCurrentItem();
                districtPos = districtPos >= mDistrictDatasMap.get(mCurrentCityName).length - 1 ? mDistrictDatasMap.get(mCurrentCityName).length - 1 : districtPos;
                String mCurrentDistrictName = mDistrictDatasMap.get(mCurrentCityName)[districtPos];

                onAddressListener.onAddressSelected(mCurrentProvinceName, mCurrentCityName, mCurrentDistrictName);
            }
        }
        cancel();
    }

    /**
     * 回调接口
     */
    public interface OnAddressListener {
        void onAddressSelected(String province, String city, String district);
    }

    public void setAddressListener(OnAddressListener onAddressListener) {
        this.onAddressListener = onAddressListener;
    }

    /**
     * 初始化布局
     */
    private void initView() {
        mViewProvince = (WheelView) view.findViewById(R.id.wv_province);
        mViewCity = (WheelView) view.findViewById(R.id.wv_city);
        mViewDistrict = (WheelView) view.findViewById(R.id.wv_district);
        mTvConfirm = (TextView) view.findViewById(R.id.tv_confirm);
        mTvCancel = (TextView)view. findViewById(R.id.tv_cancel);

        // 设置可见条目数量
        mViewProvince.setVisibleItemCount(9);
        mViewCity.setVisibleItemCount(9);
        mViewDistrict.setVisibleItemCount(9 );
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //省
        mViewProvince.setItems(mProvinceDatas);
        mViewProvince.setCurrentItem(0);
        //市
        mViewCity.setItems(mCitisDatasMap.get(mProvinceDatas[0]));
        mViewCity.setCurrentItem(0);
        //区
        mViewDistrict.setItems(mDistrictDatasMap.get(mCitisDatasMap.get(mProvinceDatas[0])[0]));
        mViewDistrict.setCurrentItem(0);
    }

    /**
     * 设置监听
     */
    private void setListener() {
        //省-------------------------------------------
        mViewProvince.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
                updateCities(index);
            }
        });
        //市-------------------------------------------
        mViewCity.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
                updateAreas(index);
            }
        });
        mTvConfirm.setOnClickListener(this);
        mTvCancel.setOnClickListener(this);
    }

    String[] cities;

    /**
     * 根据当前的省，更新市WheelView的信息
     */
    private void updateCities(int index) {
        String province =  mProvinceDatas[index];
        cities = mCitisDatasMap.get(province);
        mViewCity.setItems(cities);
        mViewCity.setCurrentItem(0);

        updateAreas(0);
    }

    /**
     * 根据当前的市，更新区WheelView的信息
     */
    private void updateAreas(int index) {

        String city = cities[index];
        String[] areas = mDistrictDatasMap.get(city);

        mViewDistrict.setItems(areas);
        mViewDistrict.setCurrentItem(0);
    }

    /**
     * 从assert文件夹中读取省市区的json文件，然后转化为json对象
     * 解析json，完成后释放Json对象的内存
     */
    private void initCityData() {
        try {
            String json = AssetsUtils.readText(context, "city.json");
            if(TextUtils.isEmpty(json)) return;
            JSONObject mJsonObj = new JSONObject(json);
            JSONArray jsonArray = mJsonObj.getJSONArray("citylist");
            mProvinceDatas = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonP = jsonArray.getJSONObject(i);// 每个省的json对象
                String province = jsonP.getString("areaName");// 省名字
                mProvinceDatas[i] = province;
                JSONArray jsonCs = null;
                try {
                    /**
                     * Throws JSONException if the mapping doesn't exist or is
                     * not a JSONArray.
                     */
                    jsonCs = jsonP.getJSONArray("cities");
                } catch (Exception e1) {
                    continue;
                }
                String[] mCitiesDatas = new String[jsonCs.length()];
                for (int j = 0; j < jsonCs.length(); j++) {
                    JSONObject jsonCity = jsonCs.getJSONObject(j);
                    String city = jsonCity.getString("areaName");// 市名字
                    mCitiesDatas[j] = city;
                    JSONArray jsonAreas = null;
                    try {
                        /**
                         * Throws JSONException if the mapping doesn't exist or
                         * is not a JSONArray.
                         */
                        jsonAreas = jsonCity.getJSONArray("counties");
                    } catch (Exception e) {
                        continue;
                    }

                    String[] mAreasDatas = new String[jsonAreas.length()];// 当前市的所有区
                    for (int k = 0; k < jsonAreas.length(); k++) {
                        String area = jsonAreas.getJSONObject(k).getString("areaName");// 区域的名称
                        mAreasDatas[k] = area;
                    }
                    mDistrictDatasMap.put(city, mAreasDatas);
                }
                mCitisDatasMap.put(province, mCitiesDatas);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得当前区的索引
     * @param district
     */
    private int getDistrictIndex(String district) {
        String[] areas = mDistrictDatasMap.get("");
        for (int k = 0; k < areas.length; k++) {
            String area = areas[k];
            if (area.contains(district)) {
                return k;
            }
        }
        return 0;
    }
    /**
     * 获得当前市的索引
     * @param city
     */
    private int getCityIndex(String city) {
        String[] cities = mCitisDatasMap.get("");
        for (int j = 0; j < cities.length; j++) {
            String cit = cities[j];
            if (cit.contains(city)) {
                return j;
            }
        }
        return 0;

    }
    /**
     * 获得当前省的索引
     * @param province
     */
    private int getProvinceIndex(String province) {
        for (int i = 0; i < mProvinceDatas.length; i++) {
            String pro = mProvinceDatas[i];
            if (pro.contains(province)) {
                return i;
            }
        }
        return 0;
    }

}
