package com.example.demo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ViewHolder {
	private SparseArray<View> mViews;
	// private int mPosition;
	private View mConvertView;

	public ViewHolder(Context context, int layoutId, ViewGroup parent, int position) {
		mViews = new SparseArray<View>();
		mConvertView = LayoutInflater.from(context).inflate(layoutId, null);
		// mPosition = position;
		mConvertView.setTag(this);
	}

	public static ViewHolder get(Context context, View convertView, int layoutId, ViewGroup parent, int position) {
		if (convertView == null) {
			return new ViewHolder(context, layoutId, parent, position);
		}
		return (ViewHolder) convertView.getTag();
	}

	@SuppressWarnings("unchecked")
	public <T extends View> T getView(int viewId) {
		View view = mViews.get(viewId);
		if (view == null) {
			view = mConvertView.findViewById(viewId);
			mViews.put(viewId, view);
		}
		return (T) view;
	}

	public View getConvertView() {
		return mConvertView;
	}

	// private int getPosition(){
	// return mPosition;
	// }

	public TextView getTextView(int viewId) {
		return getView(viewId);
	}

	public Button getButton(int viewId) {
		return getView(viewId);
	}

	public ImageView getImageView(int viewId) {
		return getView(viewId);
	}
	
	public CheckBox getCheckBox(int viewId){
		return getView(viewId);
	}
	public LinearLayout getLinearLayout(int viewId){
		return getView(viewId);
	}
	public RelativeLayout getRelativeLayout(int viewId){
		return getView(viewId);
	}

	public GridView getGridView(int viewId) {
		return getView(viewId);
	}

	public ViewHolder setTextView(int viewId, String text) {
		TextView view = getView(viewId);
		view.setText(text);
		return this;
	}
	
	public ViewHolder setTextView(int viewId, int resId) {
		TextView view = getView(viewId);
		view.setText(resId);
		return this;
	}

	public ViewHolder setButton(int viewId, String text) {
		Button view = getView(viewId);
		view.setText(text);
		return this;
	}
	
	public ViewHolder setButton(int viewId, int resId) {
		Button view = getView(viewId);
		view.setText(resId);
		return this;
	}
	
	public ViewHolder setImageView(int viewId, int resId) {
		ImageView view = getView(viewId);
		view.setImageResource(resId);
		return this;
	}

	public ViewHolder setImageView(int viewId, Bitmap bm) {
		ImageView view = getView(viewId);
		view.setImageBitmap(bm);
		return this;
	}
	
	public ViewHolder setImageView(int viewId, Drawable drawable) {
		ImageView view = getView(viewId);
		view.setImageDrawable(drawable);
		return this;
	}

	public ViewHolder setProgress(int viewId, int progress) {
		ProgressBar view = getView(viewId);
		view.setProgress(progress);
		return this;
	}
}
