package jp.gr.java_conf.hatalab.mnv;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileListAdapter extends ArrayAdapter<String>{

	private LayoutInflater mInflater;
	private TextView mFileName;
	private ImageView mIcon;
	private float mFontSize;
	private int mImageHeight;
	public FileListAdapter(Context context, List<String> objects) {
		super(context, 0, objects);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mFontSize = Config.getFontSizeOnList();
		final float scale = getContext().getResources().getDisplayMetrics().density;  
		mImageHeight = (int)(mFontSize * scale * 4.0f / 3.0f + 0.5f);//4/3 up
		//mImageHeight = (int)((int)(mFontSize * scale   + 0.5f) * 4.0f / 3.0f + 0.5f);//4/3 up
		//mImageHeight = (int)( (int)(mFontSize * scale) * 4.0f / 3.0f );//4/3 up
		//Log.d("FileListAdapter","mImageHeight:" + mImageHeight);
		
	}

	// 1行ごとのビューを生成する
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (convertView == null) {
			view = mInflater.inflate(R.layout.file_row_with_icon, null);
		}

		// 現在参照しているリストの位置からItemを取得する
		String name = this.getItem(position);
		if (name != null) {
			// Itemから必要なデータを取り出し、それぞれTextViewにセットする
			mFileName = (TextView) view.findViewById(R.id.TextView);
			mFileName.setText(name);
			//mFileName.setTextSize(TypedValue.COMPLEX_UNIT_SP, Config.getFontSizeOnList());
			mFileName.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
			
			mIcon = (ImageView)view.findViewById(R.id.ImageView);

			if(name.endsWith("/")){ //directory
				mIcon.setImageResource(R.drawable.folder01);
			}else if(name.endsWith(".txt")){
				mIcon.setImageResource(R.drawable.textfile01);				
			}else if(name.endsWith(".chi")){
				mIcon.setImageResource(R.drawable.tombofile01);
			}else if(name.endsWith("..")){
				mIcon.setImageResource(R.drawable.updir01);
			}else{
				mIcon.setImageResource(R.drawable.otherfile02);
			}
			

			//int list_h1 = mFileName.getMeasuredHeight();// .getHeight();
			//int imageh1 = mIcon.getMeasuredHeight();// .getHeight();
			//int list_h2 = mFileName.getHeight();// .getHeight();
			//int imageh2 = mIcon.getHeight();// .getHeight();
			//float fh = mFileName.getTextSize();
			//Log.d("FileListAdapter","Height(list) :" + list_h1 + ", mImageHeight:" + mImageHeight);
			//Log.d("FileListAdapter","Height(image):" + imageh1 + "," + imageh2);
			mIcon.setLayoutParams(new LinearLayout.LayoutParams(mImageHeight,mImageHeight));

		}
		return view;
	}
	

	
}
