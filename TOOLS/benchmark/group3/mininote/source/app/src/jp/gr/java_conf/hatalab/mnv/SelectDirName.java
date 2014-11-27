package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectDirName extends ListActivity{

	// インテント定数定義
	public static final String INTENT_DIRPATH = "DIRPATH";		// ディレクトリパス名
//	public static final String INTENT_FILENAME = "FILENAME";	// ファイル名
	public static final String INTENT_FILEPATH = "FILEPATH";	// フルパスファイル名

	// その他定数定義
	private static final String TITLE_OPEN = "Select Folder";

	private String DirPath = "/sdcard";
	private String filename;
	private List<String> items = null;

	private EditText mEdtFileName;

	@Override public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.select_dir);
		
		Button btnOK = (Button)findViewById(R.id.btnOK);
		btnOK.setText(R.string.alert_dialog_ok);

		Button btnCancel = (Button)findViewById(R.id.btnCancel);
		btnCancel.setText(R.string.alert_dialog_cancel);

		TextView txtTitle = (TextView)findViewById(R.id.txtTitle);

		
		mEdtFileName = (EditText)findViewById(R.id.edtFileName);


		Bundle extras = getIntent().getExtras();
		// タイトルの設定
		if (extras != null) {
			txtTitle.setText(R.string.title_select_folder);
//			txtTitle.setText(TITLE_OPEN);

			filename = extras.getString(INTENT_FILEPATH);

			File aDir = new File(filename);
			if(aDir.exists()){
				if(aDir.isDirectory()){
					DirPath = filename;
				}else{
					DirPath = aDir.getParent();					
				}
			}else{
				DirPath="/";				
			}

			EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
			edtFileName.setText(DirPath);

			
//			System.out.println("DirPath=" + DirPath);
//			System.out.println("FilePath=" + filename);
		}
		//リスト構築
		fillList();

		// OKボタンのクリックリスナー
		btnOK.setOnClickListener(new View.OnClickListener() {

			//		@Override
			public void onClick(View v) {
				Intent intent = new Intent();

				// OKボタン押下の場合は、以下のインテントを設定
				//  ファイル名/ディレクトリパス名/ファイルパス名
				EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
				String strDirName = edtFileName.getText().toString();

				
//				intent.putExtra(INTENT_FILENAME, strFileName );
				intent.putExtra(INTENT_DIRPATH, DirPath );
//				intent.putExtra(INTENT_DIRPATH, strDirName );
				
/**
				String strFilePath;
				if( DirPath.equals("/")) {
					strFilePath = "/" + strFileName;
				} else {
					strFilePath = DirPath + "/" + strFileName;
				}
				intent.putExtra(INTENT_FILEPATH, strFilePath );
**/				
				// 処理結果を設定
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		// Cancelボタンのクリックリスナー
		btnCancel.setOnClickListener(new View.OnClickListener() {
			//		@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				// 処理結果を設定
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});

		
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		// リストアダプタから現在選択中のファイル名／ディレクトリ名を取得
		String strItem = (String)getListAdapter().getItem(position);

		if( strItem.equals("..") ) {
			upOneLevel();
			/*
			// ディレクトリを1階層上がる場合
			if( DirPath.lastIndexOf("/") <= 0 ) {
				// ルートから1階層目の場合
				DirPath = DirPath.substring(0, DirPath.lastIndexOf("/") + 1 );
			} else {
				// ルートから2階層目以上の場合
				DirPath = DirPath.substring(0, DirPath.lastIndexOf("/"));
			}
			fillList();
			*/
		} else if( strItem.substring(strItem.length() - 1 ).equals("/") ) {
			// ディレクトリに入る場合
			if( DirPath.equals("/") ) {
				//ルートの場合
				DirPath += strItem;
			} else {
				//ルートから1階層目以上の場合
				DirPath = DirPath + "/" + strItem;
			}
			DirPath = DirPath.substring(0, DirPath.length() - 1 );
			fillList();
		} else {
			// ファイルの場合はエディットテキストに設定
			EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
			edtFileName.setText(strItem);
		}

	}


	private void upOneLevel(){
		String previousFileName = new File(DirPath).getName() + "/";

		
		// ディレクトリを1階層上がる場合
		if( DirPath.lastIndexOf("/") <= 0 ) {
			// ルートから1階層目の場合
			DirPath = DirPath.substring(0, DirPath.lastIndexOf("/") + 1 );
		} else {
			// ルートから2階層目以上の場合
			DirPath = DirPath.substring(0, DirPath.lastIndexOf("/"));
		}

		
		fillList();
		
		
		//カーソル参照していたフォルダの場所に合わせる。
		int posision = 0;
		for (String s : this.items) {
			//System.out.println(s);
			if(previousFileName.equals(s)){
				this.setSelection(posision);
				break;
			}
			posision++;
		}
	}





	// ファイルリスト構築
	private void fillList()
	{
		File[] files = new File(DirPath).listFiles();
		if( files == null ) {
			Toast.makeText(this, "Unable Access...",
					Toast.LENGTH_SHORT).show();
			return;
		}

		// カレントディレクトリ名をTextViewに設定
		TextView txtDirName = (TextView)findViewById(R.id.txtDirName);
		txtDirName.setText(DirPath);

//		System.out.println("DirPath=" + DirPath);
//		System.out.println("FilePath=" + filename);

		if( items != null ) {
			items.clear();
		}
		items = new ArrayList<String>();

		// ルートじゃない場合は、階層を上がれるように".."をArrayListの先頭に設定
		if( !DirPath.equals("/") ) {
			items.add("..");
		}

		// ファイル・ディレクトリがあるだけひたすらArrayListに追加する
		for( File file : files ) {
			if( file.isDirectory() ) {
				items.add(file.getName() + "/" );
			} else {
				// ディレクトリのみ表示
//				items.add(file.getName());
			}
//			System.out.println("file=" + file.getName());
		}

		// ArrayListをListActivityに設定する
		//ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.file_row, items);

		FileListAdapter fileList = new FileListAdapter(this,this.items);

		
		//昇順で並べるようにする。
		fileList.sort(new Comparator<String>() {
			public int compare(String object1, String object2) {
				return object1.compareToIgnoreCase(object2);
//				return object1.compareTo(object2);
			};
		});
		
		
		
		setListAdapter(fileList);
		
		//現在のディレクトリをテキストボックスにセット
		//EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
		mEdtFileName.setText(DirPath);
	}

    // キーイベント発生時、呼び出されます
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(!getListView().hasFocus()){
    		return super.dispatchKeyEvent(event);
    	}

    	
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DEL: // DELキー
//            	upOneLevel();//up dir
                return true;

            case KeyEvent.KEYCODE_BACK:
//    			Log.v("KeyEvent","KEYCODE_BACK,DOWN");

            	/*
    			if(!showBottomBarFlag){//back keyは無視するACTION_UPの時に処理をする。
    				mBackKeyDown = true;
    				return true;
    			}
    			*/
    			
    			break;
    			
    			
            case KeyEvent.KEYCODE_DPAD_LEFT: // 左キー
            	upOneLevel();//up dir
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: // 左キー
            	//upOneLevel();//up dir
            	//ToDo
            	KeyEvent e = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
                return super.dispatchKeyEvent(e);

    		default :
    			mBackKeyDown = false;
    			break;
    		}
    	}

        if (event.getAction() == KeyEvent.ACTION_UP) { // キーが離された時
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL: // DELキー
            	upOneLevel();//up dir
                return true;
            case KeyEvent.KEYCODE_BACK: // BACK KEY
            	
//    			Log.v("KeyEvent","KEYCODE_BACK,UP");
            	if(mBackKeyDown){
            		mBackKeyDown = false;//戻しておく
            		/*
            		if(!showBottomBarFlag){// 下部ボタンがないときは戻るボタンがup dirの機能となる
            			if(this.currentDirectory.getParent() == null
            					|| this.currentDirectory.toString().equals(mInitDirName) ){
            				PasswordBox.resetPassword();
            				finish();
            			}else{
            				upOneLevel();//up dir
            			}
            			return true;
            		}
            		*/
            	}else{
            		mBackKeyDown = false;
            	}
            	break;

            case KeyEvent.KEYCODE_DPAD_RIGHT: // 左キー
            	//upOneLevel();//up dir
            	//ToDo
            	KeyEvent e = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
                return super.dispatchKeyEvent(e);

            default:
        		mBackKeyDown = false;
        		break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

	
	
	
	
}
