package com.waynian.gaodeapitest;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by waynian on 2016/8/16.
 */
public class MyAdapter extends BaseAdapter {
    private LayoutInflater mInflater = null;
    private Context ctx;
    private List<String> data;
    private LayoutInflater layoutInflater;

    public MyAdapter(Context ctx, List<String> data) {
        this.ctx = ctx;
        this.data = data;
        this.layoutInflater = LayoutInflater.from(ctx);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = layoutInflater.inflate(R.layout.list_item, null);
        TextView tv_station = (TextView) view.findViewById(R.id.tv_station);
        tv_station.setText(data.get(position));

        String station = "石马";
        String start_station = data.get(0);
        String end_station = data.get(data.size() - 1);

        if (start_station.equals(data.get(position))) {
            tv_station.setText(data.get(0) + "  起始站");
            tv_station.setTextColor(Color.BLUE);
        }else if (end_station.equals(data.get(position))) {
            tv_station.setText(data.get(data.size() - 1) + "  终点站");
            tv_station.setTextColor(Color.BLUE);
        }else if(station.equals(data.get(position))){
            tv_station.setText(data.get(position));
            tv_station.setTextColor(Color.RED);
        }

//       if (position<19){
//            tv_station.setText(data.get(position));
//            tv_station.setTextColor(Color.RED);
//        }else if (position>19){
//            tv_station.setText(data.get(position));
//            tv_station.setTextColor(Color.BLUE);
//        }else if (position == 19){
//           tv_station.setText(data.get(position));
//           tv_station.setTextColor(Color.YELLOW);
//       }
        return view;
    }
}
