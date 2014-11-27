package jp.gr.java_conf.hatalab.mnv;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class TemplateTextList extends ListActivity {
	/**
	"MM/dd/yy h:mmaa" -> "04/06/70 3:23am"
	"MMM dd, yyyy h:mmaa" -> "Apr 6, 1970 3:23am"
	"MMMM dd, yyyy h:mmaa" -> "April 6, 1970 3:23am"
	"E, MMMM dd, yyyy h:mmaa" -> "Mon, April 6, 1970 3:23am&
	"EEEE, MMMM dd, yyyy h:mmaa" -> "Monday, April 6, 1970 3:23am"
	"'Noteworthy day: 'M/d/yy" -> "Noteworthy day: 4/6/70"
	 */
	final private String[] format_sample = {
			"yyyy/MM/dd HH:mm:ss",
			"yyyy/MM/dd",
			"HH:mm:ss",
			"MM/dd/yy h:mmaa",
			"MMM dd, yyyy h:mmaa",
			"MMMM dd, yyyy h:mmaa",
			"E, MMMM dd, yyyy h:mmaa",
			"EEEE, MMMM dd, yyyy h:mmaa",
			"'Noteworthy day: 'M/d/yy"
	};
	final private static String DEFAULT_TEMPLATE_TEXT = "yyyy/MM/dd HH:mm:ss";
	
	//public static String PREFIX_NORMAL     = "normal:";
	//public static String PREFIX_TIMEFORMAT = "timeformat:";
	public static String PREFIX_NORMAL     = myTemplateText.PREFIX_NORMAL;
	public static String PREFIX_TIMEFORMAT = myTemplateText.PREFIX_TIMEFORMAT;
	
	private ArrayList<myTemplateText> mTemplateList;
	private TemplateListAdapter       mTemplateAdapter;
	
	private static int PREF_KEY = R.string.prefTemplateTextKeyPrefix;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.template_list);
        
		TextView txtTitle = (TextView)findViewById(R.id.template_list_title);
		txtTitle.setText(getText(R.string.template_list_title));
		
		TextView emptyView = (TextView)findViewById(android.R.id.empty);
		emptyView.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
//				myTemplateText item = new myTemplateText("");
//				mTemplateAdapter.add(item);
				editTemplate(-1, "",false);
			}
		});
		
		mTemplateList = loadPreferences(this);
		
/**
		mTemplateList.add(new myTemplateText("first templat\n\naaa"));
		mTemplateList.add(new myTemplateText("second template\naaa"));
		mTemplateList.add(new myTemplateText("third template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
		mTemplateList.add(new myTemplateText("netxt template\naaa"));
**/		
		
		mTemplateAdapter = new TemplateListAdapter(this, mTemplateList);
		
		getListView().addFooterView(getFooter());
		setListAdapter(mTemplateAdapter);
		
		
		//長おしアクション
		getListView().setOnItemLongClickListener(new OnItemLongClickListener()
		{
			//@Override
			public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {
				myLongClick(position);
				return true;
			}                    
		
		});	
    }


    
    //ボタンを押したときの動作
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		myTemplateText item;
		int max = l.getCount();
		item = (myTemplateText) l.getItemAtPosition(position);
		
		if(item != null){
			editTemplate(position, item.getText(),item.isTimeFormat());
		}
		if(position == max -1){//footer
			if(position > 99){
				Toast.makeText(TemplateTextList.this, "Too many..,Can not add.", Toast.LENGTH_SHORT).show();
				return;
			}

//			item = new myTemplateText("");
//			mTemplateAdapter.add(item);
//			editTemplate(position, item.getText(),item.isTimeFormat());
			editTemplate(-1, "", false);
			
		}

	}

	
	//Long Click時の動作
	private void myLongClick(final int position){

		myTemplateText item;
		item = (myTemplateText) this.getListView().getItemAtPosition(position);
		if(item == null){
			return;
		}

		final String text = item.getText();
		final boolean isTimeFormat = item.isTimeFormat();
		
		CharSequence[] menu = {
				getText(R.string.longclick_menu_up),     //item no 0
				getText(R.string.longclick_menu_down),   //item no 1
				getText(R.string.longclick_menu_edit),   //item no 2
				getText(R.string.longclick_menu_delete), //item no 3
				getText(R.string.longclick_menu_cancel)  //item no 4
		};
		
		new AlertDialog.Builder(this)
		.setTitle(R.string.longclick_menu_title)
		.setItems(menu, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				switch(item) {
				case 0:		// move the item up
					moveItemUp(position);
					break;

				case 1:		// move the item down
					moveItemDown(position);
					break;

				case 2:		// rename List
					editTemplate(position, text, isTimeFormat);
					break;

				case 3:		// remove from List
					removeItem(position);
					break;

				case 4:		// cancel
					break;

				default:
					break;
				}

//				Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
				
			}
		})
		.show();

	}

	private void updateTemplateItem(int position, String text, boolean isTimeFormat){
		//position == -1 : add new item.
		myTemplateText item;
		int cnt = mTemplateAdapter.getCount();
		if(position < 0 || cnt - 1 < position ){// out of bounce
			item = new myTemplateText(text, isTimeFormat);
			mTemplateAdapter.add(item);
			position = mTemplateAdapter.getPosition(item);

		}else{
			item = mTemplateAdapter.getItem(position);
			item.setText(text);
			item.setTimeFormat(isTimeFormat);
			
		}
		
//		mTemplateAdapter.getItem(position).setText(text);
//		mTemplateAdapter.getItem(position).setTimeFormat(isTimeFormat);
//		Toast.makeText(TemplateTextList.this, "position=" + position, Toast.LENGTH_SHORT).show();
		mTemplateAdapter.notifyDataSetChanged();
		savePreferences(position, item);

	}
	
	private void removeItem(int position){
		myTemplateText item = mTemplateAdapter.getItem(position);
		mTemplateAdapter.remove(item);
		mTemplateAdapter.notifyDataSetChanged();
		deletePreferences(position);

	}
	private void moveItemUp(int position){
		if(position < 1)return;
		swapItemOrder(position - 1 ,position);
	}
	private void moveItemDown(int position){
		if(mTemplateAdapter.getCount() - 2  < position)return;
		swapItemOrder(position ,position + 1 );
	}
	private void swapItemOrder(int i, int j){
		if(mTemplateAdapter.getCount() <= i || mTemplateAdapter.getCount() <= j)return;//Out of index
		if(i < 0 || j < 0)return; //Out of index
		
		myTemplateText item_i = mTemplateAdapter.getItem(i);
		myTemplateText item_j = mTemplateAdapter.getItem(j);
		mTemplateAdapter.remove(item_i);
		mTemplateAdapter.insert(item_j, i);
		mTemplateAdapter.remove(item_j);
		mTemplateAdapter.insert(item_i, j);
		
		mTemplateAdapter.notifyDataSetChanged();
		savePreferences(i, item_j);
		savePreferences(j, item_i);
	}
	
	
	
	private void editTemplate(final int position,  String text,boolean isTimeFormat){
//		final String key = url;
		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.edit_template_text, null);

		final EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
		nameEditText.setText(text);

		final CheckBox timeformatCheck = (CheckBox)inputView.findViewById(R.id.timeformat_checkbox);
		timeformatCheck.setChecked(isTimeFormat);

		final Button sampleButton = (Button)inputView.findViewById(R.id.sampleButton);
		if(!isTimeFormat && sampleButton.getVisibility() == View.VISIBLE)sampleButton.setVisibility(View.INVISIBLE);
		
		//checkボックスのクリックリスナー
		timeformatCheck.setOnClickListener(new View.OnClickListener(){
//			@Override
			public void onClick(View v) {
				if(timeformatCheck.isChecked() == true) {
					// チェックされた状態の時の処理を記述
					if(sampleButton.getVisibility() == View.INVISIBLE)
						sampleButton.setVisibility(View.VISIBLE);
				}
				else {
					// チェックされていない状態の時の処理を記述
					if(sampleButton.getVisibility() == View.VISIBLE)
						sampleButton.setVisibility(View.INVISIBLE);
				}
			}
		});

		//ボタンのクリックリスナー
		sampleButton.setOnClickListener(new View.OnClickListener() {
			//		@Override
			public void onClick(View v) {
				//ボタンを押したときの動作
				getTimeFormatSample(nameEditText);
			}

		});
		
		nameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
//	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
	            if (hasFocus) {
	                inputMethodManager.showSoftInput(v, 0);//.showSoftInput(v.getWindowToken(),0);
//	                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            
	            }else{
	                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
	            }
	        }
	    });

		
		
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.edit_template_title)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				String text = nameEditText.getText().toString();
				boolean check = timeformatCheck.isChecked();
//				name = name.replaceAll("^[\\s　]*$", "");//空白だけ
				
				if(text.length()>0){
					//check Time format
					if(check){
						try{
							Date inDate = new Date();
							//SimpleDateFormat sdf = new SimpleDateFormat(text);
							//String formated text = sdf.format(inDate);

							SimpleDateFormat sdf = new SimpleDateFormat(text);
							String formated_text = sdf.format(inDate);
							
							updateTemplateItem(position, text, check);
						}catch(Exception e){
							//Toast.makeText(TemplateTextList.this, e.toString(), Toast.LENGTH_LONG).show();
							editTemplate(position, text, check);
							MyUtil.showMessage(e.toString(), TemplateTextList.this);
						}
					}else{
						updateTemplateItem(position, text, check);						
					}
					
					
				}else{
					//nameが空だったら終了する
					//	finish();
					Toast.makeText(TemplateTextList.this, "Input text is empty.", Toast.LENGTH_SHORT).show();
				}

			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//do nothing
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//do nothing
			}
		})
		.setView(inputView)
		.create();
		
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);


		alertDialog.show(); //ダイアログ表示

	}

	
	private View getFooter(){
		View footerView = getLayoutInflater().inflate(R.layout.template_list_footer, null);
		return footerView;
	}
	
	private void getTimeFormatSample(final EditText editView){

		CharSequence[] menu = new CharSequence[format_sample.length];
		Date inDate = new Date();		
		for(int i=0; i < menu.length; i++){
			SimpleDateFormat sdf = new SimpleDateFormat(format_sample[i]);
			menu[i] = sdf.format(inDate);
		}
		
		
		
		new AlertDialog.Builder(this)
		.setTitle(R.string.text_timeformat_example)
		.setItems(menu, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				int st = editView.getSelectionStart();
				int en = editView.getSelectionEnd();
				if(st > en){int tmp = st;st = en;en = tmp;}
				
				
				editView.getText().replace(st, en, format_sample[item]);
//				Toast.makeText(getApplicationContext(), "clicked:" + menu[item].toString(), Toast.LENGTH_SHORT).show();
				
			}
		})
		.show();

		
	}
	
	private void savePreferences(int number, myTemplateText item){
		String prefix ;
		//String key = getText(R.string.prefTemplateTextKeyPrefix).toString();
		String key = getText(PREF_KEY).toString();
		
		if(number > 99){
			Toast.makeText(TemplateTextList.this, "Can not save Preferences. Too many data.", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		DecimalFormat nf = new DecimalFormat("00");
		key = key + nf.format(number); //PrefTemplateText00,01,02,03,...
//		Toast.makeText(TemplateTextList.this, "key=" + key, Toast.LENGTH_SHORT).show();

		if(item.isTimeFormat()){
			prefix = TemplateTextList.PREFIX_TIMEFORMAT;
		}else{
			prefix = TemplateTextList.PREFIX_NORMAL;
			
		}
		
        //設定を保存
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); //←このthisは普通Activityとかね
        Editor ed = sp.edit();
        ed.putString(key, prefix + item.getText());
        ed.commit();

	}

	private void saveAllPreferences(){
		//String keyPrefix = getText(R.string.prefTemplateTextKeyPrefix).toString();
		String keyPrefix = getText(PREF_KEY).toString();
		
		DecimalFormat nf = new DecimalFormat("00");
		
		int max = mTemplateAdapter.getCount();
        //設定を保存
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); //←このthisは普通Activityとかね
        Editor ed = sp.edit();
		
		for(int i=0; i<max; i++){
			myTemplateText item = mTemplateAdapter.getItem(i);
			if(item != null){
				String key = keyPrefix + nf.format(i); //PrefTemplateText00,01,02,03,...
				String prefix;
				if(item.isTimeFormat()){
					prefix = TemplateTextList.PREFIX_TIMEFORMAT;
				}else{
					prefix = TemplateTextList.PREFIX_NORMAL;			
				}
		        ed.putString(key, prefix + item.getText());
			}
		}
        ed.commit();
	}
	private void deleteAllPreferences(){
		//String keyPrefix = getText(R.string.prefTemplateTextKeyPrefix).toString();
		String keyPrefix = getText(PREF_KEY).toString();
		
		DecimalFormat nf = new DecimalFormat("00");
        //設定を保存
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); //←このthisは普通Activityとかね
        Editor ed = sp.edit();
		
		for(int i=0; i<100; i++){
			String key = keyPrefix + nf.format(i); //PrefTemplateText00,01,02,03,...
			String value = sp.getString(key, null);
			if(value==null)break;
			ed.remove(key);
		}
        ed.commit();
	}

	
	private void deletePreferences(int number){
		deleteAllPreferences();
		saveAllPreferences();
		
		/**
		//String key = getText(R.string.prefTemplateTextKeyPrefix).toString();
		String key = = getText(PREF_KEY).toString();
		
		DecimalFormat nf = new DecimalFormat("00");
		key = key + nf.format(number); //PrefTemplateText00,01,02,03,...
        //設定を保存
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); //←このthisは普通Activityとかね
        Editor ed = sp.edit();
        ed.remove(key);
        ed.commit();
		 */
	}

	public static ArrayList<myTemplateText> loadPreferences(Activity act){
		
		initPreferences(act);
		
		//String prefix = act.getText(R.string.prefTemplateTextKeyPrefix).toString();
		String prefix = act.getText(PREF_KEY).toString();
		
		DecimalFormat nf = new DecimalFormat("00");
		ArrayList<myTemplateText> list  = new ArrayList<myTemplateText>();

		//設定
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(act); //←このthisは普通Actvity
		for(int i=0; i<100; i++){
			String key = prefix + nf.format(i); //PrefTemplateText00,01,02,03,...
			
			String value = sp.getString(key, null);
			if(value==null)break;
			
			myTemplateText item;
			
			if(value.startsWith(TemplateTextList.PREFIX_NORMAL)){
				value = value.replace(TemplateTextList.PREFIX_NORMAL, "");
				item = new myTemplateText(value,false);
				
			}else if(value.startsWith(TemplateTextList.PREFIX_TIMEFORMAT)){
				value = value.replace(TemplateTextList.PREFIX_TIMEFORMAT, "");
				item = new myTemplateText(value,true);
				
			}else{
				item = new myTemplateText(value,true);				
			}
			
//	    	Log.d("TemplateTextList", "key:" + key +", value:" + value);

			
			list.add(item);
			
		}

		return list;
	}
	
	private static void initPreferences(Activity act){
		String prefix ;
		//String key = act.getText(R.string.prefTemplateTextKeyPrefix).toString();//数字なしのkeyを初期化フラブとする。
		String key = act.getText(PREF_KEY).toString();
		
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(act); //←このthisは普通Actvity
		String value = sp.getString(key, null);
		if(value!=null)return;//既に値が入っていれば初回ではないので、以下の処理はスキップ
		
		
        Editor ed = sp.edit();
        ed.putString(key, "initialized");
		
		//初期値を保存
//		Toast.makeText(TemplateTextList.this, "key=" + key, Toast.LENGTH_SHORT).show();
		myTemplateText item = new myTemplateText(DEFAULT_TEMPLATE_TEXT.toString(),true);
		prefix = TemplateTextList.PREFIX_TIMEFORMAT;
        //設定を保存
        ed.putString(key+"00", prefix + item.getText());
        ed.commit();

	}

	
    class TemplateListAdapter extends ArrayAdapter<myTemplateText>{
		private LayoutInflater mInflater;

		public TemplateListAdapter(Context context, List<myTemplateText> objects) {
			super(context, 0, objects);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		
		}
		
		// 1行ごとのビューを生成する
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (convertView == null) {
				view = mInflater.inflate(R.layout.template_list_row, null);
			}
			TextView v= (TextView)view.findViewById(R.id.item_template);
			
			final myTemplateText item = this.getItem(position);
			if (item != null) {
				v.setText(item.getText());
			}
			
			return view;
		}
    	
    }
}