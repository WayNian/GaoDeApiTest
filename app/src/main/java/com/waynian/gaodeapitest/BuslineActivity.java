package com.waynian.gaodeapitest;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.InfoWindowAdapter;
import com.amap.api.maps2d.AMap.OnMarkerClickListener;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.LatLngBounds;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.overlay.BusLineOverlay;
import com.amap.api.services.busline.BusLineItem;
import com.amap.api.services.busline.BusLineQuery;
import com.amap.api.services.busline.BusLineQuery.SearchType;
import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusLineSearch;
import com.amap.api.services.busline.BusLineSearch.OnBusLineSearchListener;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.busline.BusStationQuery;
import com.amap.api.services.busline.BusStationResult;
import com.amap.api.services.busline.BusStationSearch.OnBusStationSearchListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * AMapV1地图中简单介绍公交线路搜索
 */
public class BuslineActivity extends Activity implements OnMarkerClickListener,
        InfoWindowAdapter, OnBusLineSearchListener, AMap.OnMapLoadedListener,
        OnBusStationSearchListener {
    private static final String TGA = "BuslineActivity";
    private AMap aMap;
    private MapView mapView;
    private ProgressDialog progDialog = null;// 进度框
    private TextView tv_bus_name, tv_bus_station, tv_cur_station;
    private String itemCitys = "南京-025";
    private String cityCode = "";// 城市区号
    private int currentpage = 0;// 公交搜索当前页，第一页从0开始
    private BusLineResult busLineResult;// 公交线路搜索返回的结果
    private List<BusLineItem> lineItems = null;// 公交线路搜索返回的busline
    private BusLineQuery busLineQuery;// 公交线路查询的查询类

    private BusStationResult busStationResult;// 公交站点搜索返回的结果
    private List<BusStationItem> stationItems;// 公交站点搜索返回的busStation
    private BusStationQuery busStationQuery;// 公交站点查询的查询类

    private BusLineSearch busLineSearch;// 公交线路列表查询
    private String search = "98";

    private LatLng latlng = new LatLng(31.963451, 118.777519);

    private ListView lv_station;

    private Timer mTimer;
    private Handler mHandler;
    private int timecount;
    private Marker marker;

    private MyAdapter myAdapter;
    public static String station;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.busline_activity);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(bundle);// 此方法必须重写
        init();
        searchLine();
//        drawMarkers();
        mTimer = new Timer();
        //开始TimeTask
        setTimeTask();
    }

    private void setTimeTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandler.sendMessage(message);
            }
            //1000毫秒之后，1000毫秒执行一次
        }, 10000, 10000);

    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        tv_bus_name = (TextView) findViewById(R.id.tv_bus_name);
        tv_bus_station = (TextView) findViewById(R.id.tv_bus_station);
        tv_cur_station = (TextView) findViewById(R.id.tv_cur_station);
        lv_station = (ListView) findViewById(R.id.lv_station);
    }

    /**
     * 设置marker的监听和信息窗口的监听
     */
    private void setUpMap() {
        aMap.setOnMarkerClickListener(this);
        aMap.setInfoWindowAdapter(this);
        aMap.setOnMapLoadedListener(this);// 设置amap加载成功事件监听器
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mTimer.cancel();
    }

    /**
     * 绘制系统默认的1种marker背景图片
     */
    public void drawMarkers(String station, LatLng time_latLng) {
        marker = aMap.addMarker(new MarkerOptions()
                .position(time_latLng)
                .title(station)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(true));
        marker.showInfoWindow();// 设置默认显示一个infowinfow
    }

    /**
     * 监听amap地图加载成功事件回调
     */
    @Override
    public void onMapLoaded() {
        // 设置所有maker显示在当前可视区域地图中
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(latlng).build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
    }


    /**
     * 公交线路搜索
     */
    public void searchLine() {
        cityCode = itemCitys.substring(itemCitys.indexOf("-") + 1);
        currentpage = 0;// 第一页默认从0开始
        showProgressDialog();
        busLineQuery = new BusLineQuery(search, SearchType.BY_LINE_NAME,
                cityCode);// 第一个参数表示公交线路名，第二个参数表示公交线路查询，第三个参数表示所在城市名或者城市区号
        busLineQuery.setPageSize(10);// 设置每页返回多少条数据
        busLineQuery.setPageNumber(currentpage);// 设置查询第几页，第一页从0开始算起
        busLineSearch = new BusLineSearch(this, busLineQuery);// 设置条件
        busLineSearch.setOnBusLineSearchListener(this);// 设置查询结果的监听
        busLineSearch.searchBusLineAsyn();// 异步查询公交线路名称
//        drawMarkers();
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索:\n");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 提供一个给默认信息窗口定制内容的方法
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 提供一个个性化定制信息窗口的方法
     */
    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    /**
     * 点击marker回调函数
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;// 点击marker时把此marker显示在地图中心点
    }

    /**
     * 公交线路搜索返回的结果显示在dialog中
     */
    public void showResultList(List<BusLineItem> busLineItems) {
        BusLineDialog busLineDialog = new BusLineDialog(this, busLineItems);
        showProgressDialog();
        String lineId = busLineItems.get(0).getBusLineId();// 得到当前点击item公交线路id
        busLineQuery = new BusLineQuery(lineId, SearchType.BY_LINE_ID,
                cityCode);// 第一个参数表示公交线路id，第二个参数表示公交线路id查询，第三个参数表示所在城市名或者城市区号
        BusLineSearch busLineSearch = new BusLineSearch(
                BuslineActivity.this, busLineQuery);
        busLineSearch.setOnBusLineSearchListener(BuslineActivity.this);
        busLineSearch.searchBusLineAsyn();// 异步查询公交线路id
        busLineDialog.get_station();
//        drawMarkers();

    }

    interface OnListItemlistener {
        public void onListItemClick(BusLineDialog dialog, BusLineItem item);
    }

    /**
     * 所有公交线路显示页面
     */
    class BusLineDialog extends Dialog {

        private List<BusLineItem> busLineItems;
        private BusLineAdapter busLineAdapter;
        protected OnListItemlistener onListItemlistener;
        private List<String> list_station;


        private Context ctx;

        public BusLineDialog(Context context, int theme) {
            super(context, theme);
        }

        public void onListItemClicklistener(
                OnListItemlistener onListItemlistener) {
            this.onListItemlistener = onListItemlistener;

        }

        public BusLineDialog(Context context, List<BusLineItem> busLineItems) {
            this(context, android.R.style.Theme_NoTitleBar);
            this.busLineItems = busLineItems;
            busLineAdapter = new BusLineAdapter(context, busLineItems);
        }


        public void get_station() {
                /*
                 *获取公交线路  起步价 所有站点信息
				 */
            tv_bus_name.setText("公交：98路");
            List<BusStationItem> list = busLineItems.get(0).getBusStations();
//            Log.d(TGA, "get_station: " + list);
            String station = null;
            list_station = new ArrayList<String>();
            for (int i = 0; i < list.size(); i++) {
                station = list.get(i).getBusStationName();
                list_station.add(station);
            }
            Log.e(TGA, String.valueOf(list_station));

            myAdapter = new MyAdapter(BuslineActivity.this, list_station);
            lv_station.setAdapter(myAdapter);

//            int index = lv_station.getFirstVisiblePosition();
//            View v = lv_station.getChildAt(10);
//            int top = (v == null) ? 10 : v.getTop();
//            lv_station.setSelectionFromTop(15, top);
        }

    }

    /**
     * 公交站点查询结果回调
     */
    @Override
    public void onBusStationSearched(BusStationResult result, int rCode) {
        dissmissProgressDialog();
        if (rCode == 1000) {
            if (result != null && result.getPageCount() > 0
                    && result.getBusStations() != null
                    && result.getBusStations().size() > 0) {
                busStationResult = result;
                stationItems = result.getBusStations();
            } else {
                ToastUtil.show(BuslineActivity.this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtil.showerror(BuslineActivity.this, rCode);
        }


    }

    /**
     * 公交线路查询结果回调
     */
    @Override
    public void onBusLineSearched(BusLineResult result, int rCode) {
        dissmissProgressDialog();
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null
                    && result.getQuery().equals(busLineQuery)) {
                if (result.getQuery().getCategory() == SearchType.BY_LINE_NAME) {
                    if (result.getPageCount() > 0
                            && result.getBusLines() != null
                            && result.getBusLines().size() > 0) {
                        busLineResult = result;
                        lineItems = result.getBusLines();
                        showResultList(lineItems);
                    }
                } else if (result.getQuery().getCategory() == SearchType.BY_LINE_ID) {
                    aMap.clear();// 清理地图上的marker
                    busLineResult = result;
                    lineItems = busLineResult.getBusLines();
                    BusLineOverlay busLineOverlay = new BusLineOverlay(this,
                            aMap, lineItems.get(0));
                    busLineOverlay.removeFromMap();
                    busLineOverlay.addToMap();
                    busLineOverlay.zoomToSpan();
                }
            } else {
                ToastUtil.show(BuslineActivity.this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtil.showerror(BuslineActivity.this, rCode);
        }
        drawMarkers("石马", Constants.SHIMA);
        lv_station.setSelection(18);

        //定时器固定时间消息
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                myAdapter.notifyDataSetChanged();
                if (msg.what == 1) {
                    timecount++;
                    if ((timecount + 19) < myAdapter.getCount()) {
                        station = String.valueOf(myAdapter.getItem(timecount + 19));
                        Log.d(TGA, "handleMessage: " + station);
                        tv_cur_station.setText("当前站点：" + station);
                        tv_cur_station.setTextColor(Color.RED);

                        lv_station.getItemAtPosition(18);

//                        lv_station.setSelection(timecount+18);

                    } else {
                        timecount = 0;
                        marker.remove();
                        station = "石马";
                        drawMarkers(station, Constants.SHIMA);
                        tv_cur_station.setText("当前站点：石马");
                        tv_cur_station.setTextColor(Color.RED);


                    }


                    switch (timecount) {
                        case 1:
                            marker.remove();
                            drawMarkers("荷塘村", Constants.HETANG);
//                            changeStation("荷塘村");
                            break;
                        case 2:
                            marker.remove();
                            drawMarkers("吴尚村", Constants.WUSHANG);
//                            changeStation("吴尚村");
                            break;
                        case 3:
                            marker.remove();
                            drawMarkers("陇西路", Constants.LONGXILU);
//                            changeStation("陇西路");

                            break;
                        case 4:
                            marker.remove();
                            drawMarkers("铁心桥西", Constants.TIEXUNQIAOXI);
//                            changeStation("铁心桥西");

                            break;
                        case 5:
                            marker.remove();
                            drawMarkers("春江路东站", Constants.CHUNJIANGLUDONGZHAN);
//                            changeStation("春江路东站");

                            break;
                        case 6:
                            marker.remove();
                            drawMarkers("春江路西站", Constants.CHUNJIANGXILUXIZHAN);
//                            changeStation("春江路西站");

                            break;
                        case 7:
                            marker.remove();
                            drawMarkers("江泉路", Constants.JIANGQUANLU);
//                            changeStation("江泉路");

                            break;
                        case 8:
                            marker.remove();
                            drawMarkers("春江新城", Constants.CHUNJIANGXINCHERNG);
//                            changeStation("春江新城");
                        default:
                            break;
                    }

                }


            }
        };


    }



}
