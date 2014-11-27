package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.util.Log;

public class DirList {
	
	private String mBaseDir = "/";
	private String mInitReturn = "";
	private String mPreviousReturn = "";
	private boolean mInitSearch = true;
//	private String mFilter  = "(.*\\.txt|.*\\.chi)";
	private String mFilter  = "(.*\\.txt|.*\\.chi|.*\\.par)";
//	private String mFilter  = ".*";
	
	
	public DirList(String initFilePath){
		File f = new File(initFilePath);
		if(!f.exists()){
			f = new File("/sdcard");
			if(!f.exists()){
				f = new File("/");
			}			
		}
		
		if(f.isDirectory()){
			mBaseDir = f.getAbsolutePath();//このファイル名が含まれる間は検索を続ける。
			mPreviousReturn = mBaseDir;
		}else{//ファイルの場合はそのファイルを含むディレクトリをbaseDirとする。
			mBaseDir = f.getParent();
			if(mBaseDir == null)mBaseDir = "/";
			mInitReturn = f.getAbsolutePath();//最初に返却するファイルパスはこれ
			mPreviousReturn = mInitReturn;
		}
		

//    	Log.d("DirList", "mInitDir = " + mBaseDir);

	}
//	private static String initDir = "/sdcard";

	public String nextFile(){
//    	Log.d("DirList.nextFile()", "mInitDir = " + mBaseDir);

    	if(mInitSearch){
    		mInitSearch = false;
    		
    		if(!mInitReturn.equals("")){//初回
    			mPreviousReturn = mInitReturn;
    			mInitReturn = "";
    			return mPreviousReturn;

    		}else{
    			String nextFile = searchNextFile(mBaseDir, "");
    			mPreviousReturn = nextFile;
    			return nextFile;
    			
    		}
    	}else{
    		if(mPreviousReturn.equals(""))return "";//前回見つからなかった場合はずっと空を返す。
    		
    		
			File f = new File(mPreviousReturn);
			String nextFile = searchNextFile(f.getParent(),f.getName());
			mPreviousReturn = nextFile;
			return nextFile;    		
    	}
    	
	}
	
	/**
	public static void main (String[] args) {

		String currentPath = "";
		String curDir = "";
		String name = "";

		if(args.length > 0){
			File f = new File(args[0]);
			curDir = f.getParent();
			name = f.getName();
		}else{
			curDir = initDir ;
			name = "";
		}

		String nextFile = searchNextFile(curDir, name);

		System.out.println("Result " + nextFile);


	}
    **/
//	private String searchNextFile(String filePath){
	private String searchNextFile(String currentDir, String fileName){
		System.out.println("searchNextFile(" + currentDir + " ," + fileName + ")");
		String nextFile = "";

		
		if(!currentDir.startsWith(mBaseDir) ){
//		if(path.equals(mInitDir)){
			System.out.println("currentDir:" + currentDir);
			System.out.println("fileName:" + fileName);
			System.out.println("mBaseDir:" + mBaseDir );
			return nextFile;// finish!
		}


		ArrayList<File> list = new ArrayList<File>();
		File baseFile = new File(currentDir);
		File[] files = baseFile.listFiles();

		// System.out.println("==== add list ====");
		for (File f : files) {
			if(f.getName().matches(mFilter) || f.isDirectory()){
				//if endwith(".txt") or endwith(".chi") or isDirectory
				list.add(f);
				// System.out.println("add list:" + f.getAbsolutePath());
			}
		}
		// System.out.println("==================");


		Collections.sort(list, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				File f1 = (File)o1;
				File f2 = (File)o2;
				String data1 = f1.getName();
				String data2 = f2.getName();
				return data1.compareToIgnoreCase(data2);
//				return data1.compareTo(data2);
			}
		});

/**
		System.out.println("==== list ====");
		for (int i = 0; i < list.size(); i++) {
			File f = list.get(i);
			if( f.isDirectory()){
				System.out.println(f.getAbsolutePath() + "/");
			}else{
				System.out.println(f.getAbsolutePath());
			}
		}
**/
//		System.out.println("==================");
		//fileNameがある時は、探して次のファイル名を検索
		int index = list.indexOf(new File(currentDir + "/" + fileName));
		//見つからなければ先頭、見つかれば次のファイルのindex
		index++; // if i = -1 ,i -> 0, not if i = -1 ; i + 1;(next index) 

		if(index >= list.size() ){
			//It was last index. and search up stair.
			if(currentDir.equals("/")){
				return ""; //　/をすべて調べ終わったら終わり。
			}else{
				File upDir = new File( currentDir );
				return searchNextFile( upDir.getParent() , upDir.getName());
			}
		}
//		System.out.println("+++++++++++++++++++++++++");


		for (int i = index; i < list.size(); i++) {
			File f = list.get(i);
//			System.out.println("Check file:" + f.getAbsolutePath());
			if(f.isDirectory()){
				String result = searchNextFile( f.getAbsolutePath(), "");
				if(result.equals("")){
					//skip
				}else{
					return result;
				}
			}else{
				return f.getAbsolutePath();
			}
		}



		return nextFile;
	}


}
