package com.example.demo.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public abstract class CommonAdapter<T> extends BaseAdapter {

	private Context context;
	private List<T> list;
	private int layoutId;

	public CommonAdapter(Context context, List<T> list, int layoutId) {
		this.context = context;
		this.list = list;
		this.layoutId = layoutId;
	}

	@Override
	public int getCount() {
		if (list != null)
			return list.size();
		return 0;
	}

	@Override
	public T getItem(int position) {
		if(list == null) return null;
		if(position < 0 || position >= list.size()) return null;
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = ViewHolder.get(context, convertView, layoutId, parent, position);
		convert(holder, getItem(position));
		return holder.getConvertView();
	}
	
	public abstract void convert(ViewHolder holder, T item);	

}
