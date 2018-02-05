package com.yangxian.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yangxian.coolweather.db.City;
import com.yangxian.coolweather.db.County;
import com.yangxian.coolweather.db.Province;
import com.yangxian.coolweather.util.HttpUtil;
import com.yangxian.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by MZyx on 2018/2/5.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private final String ADDRESS = "http://guolin.tech/api/china";

    private TextView mTitleText;
    private Button mBackButton;
    private ListView mListView;
    private ProgressDialog mDialog;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> dataList = new ArrayList<String>();

    private List<Province> provinces;
    private List<City> cities;
    private List<County> counties;

    private Province selectedProvince;
    private City selectedCity;

    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mTitleText = view.findViewById(R.id.title_text);
        mBackButton = view.findViewById(R.id.back_button);
        mListView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        mListView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (currentLevel) {
                    case LEVEL_PROVINCE:
                        selectedProvince = provinces.get(i);
                        queryCities();
                        break;
                    case LEVEL_CITY:
                        selectedCity = cities.get(i);
                        queryCounties();
                        break;
                    case LEVEL_COUNTY:
                        Toast.makeText(getContext(), "selected area:"+selectedProvince.getProvinceName()+"省"+selectedCity.getCityName()+"市"+counties.get(i).getCountyName()+"区/县", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }else if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        mTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        provinces = DataSupport.findAll(Province.class);
        if (provinces.size() > 0) {
            dataList.clear();
            for (Province province : provinces) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(ADDRESS, LEVEL_PROVINCE);
        }
    }

    private void queryCities() {
        mTitleText.setText(selectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        cities = DataSupport.where("provinceId = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cities.size() > 0) {
            dataList.clear();
            for (City city : cities) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = ADDRESS + "/" + provinceCode;
            queryFromServer(address, LEVEL_CITY);
        }
    }

    private void queryCounties() {
        mTitleText.setText(selectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        counties = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (counties.size() > 0) {
            dataList.clear();
            for (County county : counties) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            String address = ADDRESS + "/" + selectedProvince.getProvinceCode() + "/" + selectedCity.getCityCode();
            queryFromServer(address, LEVEL_COUNTY);
        }
    }

    private void queryFromServer(String address, final int type) {
        showProgress();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgress();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                switch (type) {
                    case LEVEL_PROVINCE:
                        result = Utility.handleProvinceResponse(responseText);
                        break;
                    case LEVEL_CITY:
                        result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                        break;
                    case LEVEL_COUNTY:
                        result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                        break;
                    default:
                        break;
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgress();
                            switch (type) {
                                case LEVEL_PROVINCE:
                                    queryProvinces();
                                    break;
                                case LEVEL_CITY:
                                    queryCities();
                                    break;
                                case LEVEL_COUNTY:
                                    queryCounties();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgress() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage("正在加载...");
            mDialog.setCanceledOnTouchOutside(false);
        }
        mDialog.show();
    }

    private void closeProgress() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }
}
