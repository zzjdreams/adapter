package com.example.gpslocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.example.gpslocation.Util.MyAdapter;
import com.example.gpslocation.myView.BarView;
import com.example.gpslocation.myView.RadarView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class GpsActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private ArrayList<GpsSatellite> numSatelliteList;
    private int mSatelliteNum;
    private Location location;
    private String LocateType;
    private String TAG = "gpsActivity";

    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private float mSpeed;
    private float mBearing;

    private float[] barHigh;
    private String[] vContent; //纵轴内容
    private String[] hContent;//横轴内容
    private String[] textArrays;//条形图上的文字
    private float[] stX;

    private BarView show_bv;
    private RadarView show_rv;

    private Button btn_satelliteMap;
    private Button btn_satelliteBar;

    private ListView listView;

    private MyAdapter<GpsSatellite> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        init();
    }

    private void init() {
        initFun();
        bindView();
        if (!isGpsAble(locationManager)) {
            openGPS2();
        } else {
            initGps();
        }
        setListener();
        hContent = new String[10];
        vContent = new String[10];
        for (int i = 0; i < 9; i++) {
            vContent[i] = "" + i * 5;
        }
        vContent[9] = "snr";
        Arrays.fill(hContent, "");
        show_bv.setAxes(vContent, hContent);
    }

    private void setListener() {
        btn_satelliteBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show_bv.setVisibility(View.VISIBLE);
                show_rv.setVisibility(View.GONE);
            }
        });
        btn_satelliteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show_rv.setVisibility(View.VISIBLE);
                show_bv.setVisibility(View.GONE);
            }
        });
    }

    private void bindView() {
        btn_satelliteBar = (Button) findViewById(R.id.btn_satelliteBar);
        btn_satelliteMap = (Button) findViewById(R.id.btn_satelliteMap);

        show_bv = (BarView) findViewById(R.id.show_bv);
        show_rv = (RadarView) findViewById(R.id.show_rv);

        listView = (ListView) findViewById(R.id.lv_right);
    }

    private void initFun() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        numSatelliteList = new ArrayList();

    }


    private void initGps() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
            }
            LocateType = locationManager.GPS_PROVIDER;
            location = locationManager.getLastKnownLocation(LocateType);
            Log.i(TAG, "initGps: " + location);
            // 设置监听器，设置自动更新间隔这里设置1000ms，移动距离：0米。
            locationManager.requestLocationUpdates(LocateType, 1000, 0, locationListener);
            // 设置状态监听回调函数。statusListener是监听的回调函数。
            locationManager.addGpsStatusListener(statusListener);
            //另外给出 通过network定位设置
//            Log.e(TAG, "initGps: "+"权限不符合" );
        } else {
            Log.e(TAG, "initGps: " + "版本不符合");
        }

    }

    private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            // GPS状态变化时的回调，获取当前状态
            @SuppressLint("MissingPermission") GpsStatus status = locationManager.getGpsStatus(null);
            //自己编写的方法，获取卫星状态相关数据
            GetGPSStatus(event, status);
        }
    };

    private void GetGPSStatus(int event, GpsStatus status) {
        if (status == null) {
        } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            //获取最大的卫星数（这个只是一个预设值）
            int maxSatellites = status.getMaxSatellites();
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            numSatelliteList.clear();
            //记录实际的卫星数目
            int count = 0;

            while (it.hasNext() && count <= maxSatellites) {
                //保存卫星的数据到一个队列，用于刷新界面
                GpsSatellite s = it.next();
                numSatelliteList.add(s);
                count++;
                Log.d("main", "updateGpsStatus----count=" + count);
            }
            mSatelliteNum = numSatelliteList.size();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(200);
                }
            }).start();

        } else if (event == GpsStatus.GPS_EVENT_STARTED) {

            //定位启动
        } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
            //定位结束
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 200) {
                try {
                    Thread.sleep(500);
                    if (mSatelliteNum > 0) {
                        show_rv.setGpsSatellites(numSatelliteList);
                        if (mSatelliteNum < 10) {
                            barHigh = new float[mSatelliteNum];
                            textArrays = new String[mSatelliteNum];
                            hContent = new String[mSatelliteNum];
                            stX = new float[mSatelliteNum];
                        } else {
                            barHigh = new float[10];
                            textArrays = new String[10];
                            hContent = new String[10];
                            stX = new float[10];
                        }
                        Arrays.fill(barHigh, 0);
                        Arrays.fill(textArrays, "");
                        Arrays.fill(hContent, "");
                        Arrays.fill(stX, 0);
                        for (int i = 0; i < mSatelliteNum && i < 10; i++) {
                            barHigh[i] = numSatelliteList.get(i).getSnr() / 5;
                            textArrays[i] = "" + numSatelliteList.get(i).getSnr();
                            hContent[i] = "" + numSatelliteList.get(i).getPrn();
                        }
                        for (int i = 0; i < numSatelliteList.size() && i < 10; i++) {
                            stX[i] = i;
                        }

                        show_bv.setAll(barHigh, vContent, hContent, textArrays, stX);
                    }


                    adapter = new MyAdapter<GpsSatellite>((ArrayList) numSatelliteList, R.layout.bar_item) {
                        @Override
                        public void bindView(ViewHolder holder, GpsSatellite obj) {
                            holder.setText(R.id.sateId, ""+ obj.getPrn());
                            holder.setText(R.id.sateA, "" + obj.getAzimuth() + "°");
                            holder.setText(R.id.sateE, "" + obj.getElevation() + "°");
                            holder.setProcess(R.id.sateBar, (int) obj.getSnr());
                            if(obj.getPrn()<40){
                                holder.setImageResource(R.id.sateImg,R.mipmap.satellite_us);
                            }else if(obj.getPrn()>200){
                                holder.setImageResource(R.id.sateImg,R.mipmap.satellite_cn);
                            }else {
                                holder.setImageResource(R.id.sateImg,R.mipmap.satellite_xx);
                            }
                        }
                    };
                    listView.setAdapter(adapter);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
            updateToNewLocation(location);
            Log.d(TAG, "LocationListener  onLocationChanged");
        }

        public void onProviderDisabled(String provider) {
            //Provider被disable时触发此函数，比如GPS被关闭
            Log.d(TAG, "LocationListener  onProviderDisabled");
        }

        public void onProviderEnabled(String provider) {
            // Provider被enable时触发此函数，比如GPS被打开
            Log.d(TAG, "LocationListener  onProviderEnabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "LocationListener  onStatusChanged");
            // Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
            if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            }
        }
    };

    private void updateToNewLocation(Location location) {
//location对象是从上面定位服务回调函数的参数获取。
        mLatitude = location.getLatitude();// 经度
        mLongitude = location.getLongitude(); // 纬度
        mAltitude = location.getAltitude(); //海拔
        mSpeed = location.getSpeed();  //速度
        mBearing = location.getBearing(); //方向

        Log.e(TAG, "updateToNewLocation: " + mLatitude + "===" + mLongitude + "===" + mAltitude + "===" + mSpeed + "===" + mBearing);
    }

    private boolean isGpsAble(LocationManager lm) { //判断是否打开gps
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void openGPS2() {   //跳转打开GPS的页面
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        locationManager.removeGpsStatusListener(statusListener);
//        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


}
