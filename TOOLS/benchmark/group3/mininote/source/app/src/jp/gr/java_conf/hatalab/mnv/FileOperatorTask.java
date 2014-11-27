package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

// ファイル読み込みスレッド
class FileOperatorTask extends AsyncTask<String, Integer, Boolean>{

	private ProgressDialog mProgressDialog;
	private MainActivity mActivity;
	private boolean errorOccured = false;
	private String  errorMessage = "";
	private String  mResultMessage = "";

	private Handler handler;//バックグランドとUIとでやり取りするため
	
	private String mOperationType = "";
	private String mSrcFilePath   = "";
	private String mDstFilePath   = "";
	
	public static String DELETE_FILE = "DELETE";  
	public static String COPY_FILE = "COPY";  
	public static String MOVE_FILE = "MOVE";  
	
	
	// コンストラクタ
	public FileOperatorTask(MainActivity activity) {
		mActivity = activity;
	}
	
	// タスクを実行した直後にコールされる
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// プログレスバーを表示する
		mProgressDialog = new ProgressDialog(mActivity);
		
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {  
	        public void onCancel(DialogInterface dialog) {  
	          // Task を停止  
	        	taskCancel();  
	        }  
	      }); 
		
		//mProgressDialog.setMessage("Now in progress...");
		mProgressDialog.setMessage(mActivity.getText(R.string.notify_now_in_progress));
		mProgressDialog.show();
		errorOccured = false;

		//ハンドラー設定
		handler = new Handler();  
	}

	@Override
	protected Boolean doInBackground(String... params) {
		
		boolean result = false;
		
		String operationType = "";
		String srcFilePath   = "";
		String dstFilePath   = "";
		
		
		if(params.length > 0){
			operationType = params[0];
		}else{
			return result;
		}
		if(params.length > 1)srcFilePath   = params[1];
		if(params.length > 2)dstFilePath   = params[2];
		
		if(operationType.equals(DELETE_FILE)){
			result = deleteFile(new File(srcFilePath));
		}else if(operationType.equals(MOVE_FILE)){
			result = moveFile(new File(srcFilePath),new File(dstFilePath));			
		}else if(operationType.equals(COPY_FILE)){
			File srcFile = new File(srcFilePath);
			File dstFile = new File(dstFilePath);
			
			if(srcFile.isDirectory() && dstFilePath.startsWith(srcFilePath + "/")){
				//コピー先がコピー元ディレクトリの中にあったらエラーにする。
				errorOccured = true;
				//errorMessage = "Can not copy " + srcFilePath + " to " + dstFilePath +".";
				errorMessage = mActivity.getText(R.string.error_file_cannot_copy).toString() + "\n" + srcFilePath + " -> " + dstFilePath +".";
				return false;
			}
			result = copyFile(srcFile, dstFile);			
		}else{
			
		}
		
		return result;
	}
	
	// メインスレッド上で実行される
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
    	if(mProgressDialog != null && mProgressDialog.isShowing())mProgressDialog.dismiss();
		
    	if(result){
    		//処理成功
        	if(!mResultMessage.equals(""))
            	Toast.makeText(mActivity, mResultMessage, Toast.LENGTH_SHORT).show();
    	}else{
    		//処理失敗
    		if(errorOccured){
    			//検索に成功してもしなくても、検索中にエラーが発生していたら表示する。
    			if(!mActivity.isFinishing())MyUtil.showMessage(errorMessage ,mActivity);
//    	    	Toast.makeText(mActivity, R.string.notFound, Toast.LENGTH_SHORT).show();	
    		}else{
    			//if(!mActivity.isFinishing())MyUtil.showMessage("Operation Failed." ,mActivity);
    			if(!mActivity.isFinishing())MyUtil.showMessage(mActivity.getText(R.string.error_operation_failed).toString() ,mActivity);
    		}
    		
    	}

		mActivity.refreshDir();

	}



	private boolean deleteFile(File file){
		boolean result = false;
		if(isCancelled()){
			errorOccured = true;
			errorMessage = "operation cancelled.";
			return false;//AsyncTaskがcancelledならbreak
		}

		try {
			if (file.isDirectory()) {//ディレクトリの場合
				String[] children = file.list();//ディレクトリにあるすべてのファイルを処理する  
				for (int i=0; i<children.length; i++) {  
					boolean success = deleteFile(new File(file, children[i]));  
					if (!success) {  
						return false;  
					}  
				}  
			}  

			postMessage("Deleting " + file.getAbsolutePath());
			// 削除  
			return file.delete();  

		}catch (Exception e) {
//			e.printStackTrace();
			errorOccured = true;
//			errorMessage = e.toString();
			errorMessage = file.getAbsolutePath() + ":\n"+ e.toString() + "\n";

		}
		return result;
	}
	
	private boolean moveFile(File srcFile,File dstFile){
		boolean result = false;

		if (dstFile.exists()) {
			//同じ名前のファイルが存在したらNG
			errorOccured = true;
			errorMessage = mActivity.getText(R.string.error_file_already_exists).toString() + ": "  + dstFile.getName();
			return false;
		}
		//まずリネームしてる。
		if(srcFile.renameTo(dstFile)){
			mResultMessage = mActivity.getText(R.string.done_move_file).toString() + ": " + dstFile.getAbsolutePath();
			return true;// success to rename.
		}
		
		
		try {
			if(transferFile(srcFile,dstFile)){
				//copyが成功したら削除する。
				return deleteFile(srcFile);
			}
			return false;
			
		}catch (Exception e) {
//			e.printStackTrace();
			errorOccured = true;
			errorMessage = e.toString();
//			errorMessage = file.getAbsolutePath() + ":\n"+ e.toString() + "\n";

		}
		return result;
	}

	
	
	private boolean copyFile(File srcFile,File dstFile){
		boolean result = false;
		try {
			return transferFile(srcFile,dstFile);
			
		}catch (Exception e) {
//			e.printStackTrace();
			errorOccured = true;
			errorMessage = e.toString();
//			errorMessage = file.getAbsolutePath() + ":\n"+ e.toString() + "\n";

		}
		return result;
	}

	private boolean transferFile(File srcFile, File dstFile) throws Exception
	{
		if(isCancelled()){
			errorOccured = true;
			errorMessage = "operation cancelled.";
			return false;//AsyncTaskがcancelledならbreak
		}

		if(!srcFile.canRead()){
			errorOccured = true;
			errorMessage = mActivity.getText(R.string.error_file_cannot_read).toString() + ": "  + dstFile.getName();
			return false;

		
		}else if (srcFile.isDirectory()) {//ディレクトリの場合 

        	//dstFileディレクトリを作成する。
			if (dstFile.exists() && dstFile.isDirectory()) {
				//同じ名前のディレクトリが存在したらそれでOK

			}else if (dstFile.exists() && !dstFile.isDirectory()) {
					//同じ名前のファイルが存在したらNG
				errorOccured = true;
				errorMessage = mActivity.getText(R.string.error_file_already_exists).toString() + ": "  + dstFile.getName();
				return false;
				
			}else{
				//存在しなければ作る
				if(!dstFile.mkdirs())return false;
//				boolean success0 = MyUtil.createDir(dstFile);
//				if (!success0)return false;  
			}
        	
            String[] children = srcFile.list();//ディレクトリにあるすべてのファイルを処理する  
            for (int i=0; i<children.length; i++) {
            	boolean success1 = transferFile(new File(srcFile, children[i]), new File(dstFile, children[i]));
            	if (!success1)return false;
            }
            
            return true;
        }else{
			if (dstFile.exists()) {
//				throw new IOException("File already exists: " + dstFile.getName());
				errorOccured = true;
				errorMessage = mActivity.getText(R.string.error_file_already_exists).toString() + ": "  + dstFile.getName();
				return false;
			
			}else{
        	
				postMessage("Copying " + srcFile.getAbsolutePath());
				// copy 
				MyUtil.fileCopy(srcFile,dstFile);
				return true;
			}
        }
	}

	
	private void postMessage(final String msg){
		// Handlerに、UIスレッドへのmsgをPOSTする。  
		handler.post(new Runnable() {  
			public void run() {  
				mProgressDialog.setMessage(msg);

			}  
		});  
	}

	
    /* cancel() がコールされると呼び出される。 */
    @Override
    protected void onCancelled() {
    	if(mProgressDialog != null && mProgressDialog.isShowing())mProgressDialog.dismiss();
    	Toast.makeText(mActivity, "canceling...", Toast.LENGTH_SHORT).show();
    	mActivity.refreshDir();
    	super.onCancelled();
    }
    
    private void taskCancel(){
    	cancel(true);
    }
}