package jp.gr.java_conf.hatalab.mnv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import ssh.Blowfish;
import ssh.MD5;


public class MyUtil {

	//テキストファイル読み込み(暗号化ファイルも復号化して)Stringを返却
	public static String readFile(String strFilePath,String charsetName) throws Exception
	{
		String strText = null;
		byte[] binText = readTextFile(strFilePath);
		//header check
		String BFHeader = "";
		if(binText.length >= 4 )BFHeader = new String(binText,0,4);
		if(BFHeader.equals("BF01")){//This File is BF01
			try{
				binText = decrypt(binText, PasswordBox.getPassDigest());
			}catch(Exception e){ 
				PasswordBox.resetPassword();
				throw e;
			}
			strText = setBinTextToStrText(binText,charsetName);
		}else{
			strText = setBinTextToStrText(binText,charsetName);		
		}
		return strText;
	}	
	
    // strText - binText間の変換
    // Charasetの変更と改行コードの変更を行う。
    private static String setBinTextToStrText(byte[] binText,String charsetName) throws Exception{
       	//バイナリデータをstrTextにセット
    	String strText = new String(binText,charsetName);
		//無条件にCRLF,CRはLFに変更
		strText = strText.replaceAll("\r\n", "\n").replaceAll("\r", "\n");	
		return strText;
    	
    }
    

    
    
    
    
    
    
    
    
	// テキストファイル読込処理
	public static byte[] readTextFile(String strFilePath) throws Exception
	{
		BufferedInputStream b_ins = null;

		try {
			File file = new File(strFilePath);
			byte[] buff = new byte[(int)file.length()];
			FileInputStream f_ins = new FileInputStream(strFilePath);

			b_ins = new BufferedInputStream(f_ins);
			b_ins.read(buff, 0, buff.length);

			return buff;
			//			return new String(buff);
		}
		finally {
			if (b_ins != null) {
				try {
					b_ins.close();
					b_ins = null;
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// テキストファイル書込処理
//	public static void writeTextFile(String strFilePath, String text) throws Exception
	public static void writeTextFile(String strFilePath, byte[] text) throws Exception
	{
		BufferedOutputStream bw = null;

		try {
			FileOutputStream fw = new FileOutputStream(strFilePath);
			bw = new BufferedOutputStream(fw);
			bw.write(text, 0, text.length);
		}
		finally {
			if (bw != null) {
				try {
					bw.close();
					bw = null;
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
//	public static String decrypt(byte[] data, byte[] passDigest) throws Exception
	public static byte[] decrypt(byte[] data, byte[] passDigest) throws Exception
	{
		
		int length = data.length;

		// "BF01" + int(size) + 8
//		if(length - 32 < 0)throw new IOException("Invalid file");
//		if(length % 8 != 0)throw new IOException("Invalid file size (not mutiples of 8)");
		if(length - 32 < 0)throw new MyUtilException(R.string.util_error_invalid_filesize1 ,"Invalid file");
		if(length % 8 != 0)throw new MyUtilException(R.string.util_error_invalid_filesize2, "Invalid file size (not mutiples of 8)");

		
		// header チェック
		String BFHeader = new String(data,0,4);
		//if(!BFHeader.equals("BF01"))throw new IOException("This File is NOT BF01");
		if(!BFHeader.equals("BF01"))throw new MyUtilException(R.string.util_error_invalid_fileheader,"This File is NOT BF01");
		// Size取得
		int BFSize=0;
		int k = 1;
		for(int i=4;i<8;i++){
			Byte bytedata = new Byte(data[i]);
			int size = Integer.parseInt(bytedata.toString());

			if(size < 0)size = 256 + size;
//			System.out.println( "check size:"+bytedata.toString()+ " "+ size + " "+k);

			BFSize = BFSize + size*k;
			k = k*256;
		}
		
		// System.out.println("Header:" + BFHeader);
		// System.out.println("Size:" + BFSize);

		//ここでpasswardを聞く	
//		byte[] pass = MiniTomboViewer.passwordBox.getPassDigest();

		//if(passDigest == null)throw new IOException("password input error");
		if(passDigest == null)throw new MyUtilException(R.string.util_error_passdigest_is_null, "password input error");

//		System.out.println("md5 ===============");
//		printHex(pass,pass.length);

//////////////////////////////////////////////////
//		 CryptManagerによる暗号化ファイルのフォーマット
//		 The format of the container is:
//		 0-3  : BF01(4 bytes)
//		 4-7  : data length (include randum area + md5sum)(4 bytes)
//		 8-15 :* random data(8 bytes)
//		16-31 :* md5sum of plain text(16 bytes)
//		32-   :* data


		// data部分をバイトオーダー変換
		data = orderConvert(data,length-8,8);

		Blowfish cipher = new Blowfish();
		cipher.setKey(passDigest);

		// 復号化
		byte[] dec = new byte[length-8];
		cipher.decrypt(data, 8, dec, 0, length-8);

		// バイトオーダー変換。元に戻す
		dec = orderConvert(dec,dec.length);

//		System.out.println("====================");
//		printHex(dec,dec.length);

		
		// 記録されたdigest値を取り出す
		byte[] orgMd5 = new byte[16];
		System.arraycopy(dec, 8, orgMd5, 0, 16);

		// データ部からdigestを計算
		MD5 md5 = new MD5();
		byte[] dataMd5 = md5.digest(dec,24,BFSize);
		
		if(!checkMD5(orgMd5,dataMd5)){
//			MiniTomboViewer.passwordBox.resetPassword();
//			throw new IOException("MD5 check sum error");
//			throw new IOException("Password is not correct.");
			throw new MyUtilException(R.string.util_error_md5_check_sum_error,"Password is not correct.");
		}
//		printHex(dataMd5, dataMd5.length);
//		printHex(orgMd5,16);
		
		
		
		//System.out.println("====================");
		//printHex(dec,length-8);
		//System.out.println(dec_text);

//		String dec_text = new String(dec,24,BFSize);
//		return dec_text;
	
		byte[] dec_data = new byte[BFSize];//return用buffer
		System.arraycopy(dec, 24, dec_data, 0, BFSize);//copy

		//		System.out.println("dec=" + dec.toString());
//		System.out.println("dec=" + new String(dec,24,BFSize));
//		System.out.println("dec_data=" + dec_data.toString());
//		System.out.println("dec_data=" + new String(dec_data,0,BFSize));

		return dec_data;
	}	

	
	

	public static byte[] encrypt(byte[] data, byte[] passDigest) throws Exception
	{
		MD5 md5 = new MD5();

		
		int enc_size ;
		if(data.length % 8 == 0){
			enc_size = data.length + 32;
		}else{
			enc_size = (data.length / 8 + 1 )*8 + 32; //8の倍数＋32ヘッダサイズ
		}
		
		byte[] savedata = new byte[enc_size];
		

		
//////////////////////////////////////////////////
//		 CryptManagerによる暗号化ファイルのフォーマット
//		 The format of the container is:
//		 0-3  : BF01(4 bytes)
//		 4-7  : data length (include randum area + md5sum)(4 bytes)
//		 8-15 :* random data(8 bytes)
//		16-31 :* md5sum of plain text(16 bytes)
//		32-   :* data

		
//		 0-3  : BF01(4 bytes)
		System.arraycopy("BF01".getBytes(),0,savedata,0,4);
		
		
//		 4-7  : data length (include randum area + md5sum)(4 bytes)
/***************
		int k = 1*256;
		int l = data.length;
		int j =  l % k;
		savedata[4]= new Integer(j).byteValue(); 
		for(int i=5;i<8;i++){
//			System.out.println("k="+k+",size="+l);
			l =  l / k;
			j = l % k;
			savedata[i]= new Integer(j).byteValue(); 

			k = k*256;

		}
***************/
		int l = data.length;
        savedata[7] = (byte)((l >>> 24 ) & 0xFF);
        savedata[6] = (byte)((l >>> 16 ) & 0xFF);
        savedata[5] = (byte)((l >>>  8 ) & 0xFF);
        savedata[4] = (byte)((l >>>  0 ) & 0xFF);

        
        

		// データ部からdigestを計算
		byte[] dataMd5 = md5.digest(data,0,data.length);

		byte[] tmpdata = new byte[enc_size - 8];//最初の8byteを除く部分のbuffer

		
		System.arraycopy(dataMd5, 0, tmpdata, 8, 16);//md5を格納　16-31(tmpdataの8から)
		System.arraycopy(data, 0, tmpdata, 24, data.length);

//		printHex(tmpdata,tmpdata.length);
//		System.out.println("=======================");
		
//		 8-15 :* random data(8 bytes)
		Random rand = new Random();
		for(int i =0;i<8;i++){
			tmpdata[i] = (byte)rand.nextInt(256); 
		}
		
		
		// data部分をバイトオーダー変換
		tmpdata = orderConvert(tmpdata,tmpdata.length);
		
		
		// 暗号化
		Blowfish cipher = new Blowfish();
		cipher.setKey(passDigest);

//		byte[] dec = new byte[length-8];
		cipher.encrypt(tmpdata, 0, savedata, 8, tmpdata.length);

		// data部分をバイトオーダー変換
		tmpdata = orderConvert(savedata,savedata.length-8,8);

//		printHex(savedata,savedata.length);
//		System.out.println("=======================");
				

		return savedata;
	}







	private static boolean checkMD5(byte[] a,byte[] b){
		if(a.length != 16 || b.length != 16)return false;

		for(int i=1;i<16;i++){
			if(a[i] != b[i]){
				return false;
			}
		}
		return true;

	}
	private	static byte[] orderConvert(byte[] b,int size){
		for(int i=3;i<size;i+=4){ 
			byte[] tmp = new byte[4];
			tmp[0] = b[i-3];
			tmp[1] = b[i-2];
			tmp[2] = b[i-1];
			tmp[3] = b[i-0];
			b[i-3]   = tmp[3];
			b[i-2] = tmp[2];
			b[i-1] = tmp[1];
			b[i-0] = tmp[0];
		}
		return b;
	}
	private	static byte[] orderConvert(byte[] b,int size,int start){
		for(int i=start+3;i<start+size;i+=4){ 
			byte[] tmp = new byte[4];
			tmp[0] = b[i-3];
			tmp[1] = b[i-2];
			tmp[2] = b[i-1];
			tmp[3] = b[i-0];
			b[i-3]   = tmp[3];
			b[i-2] = tmp[2];
			b[i-1] = tmp[1];
			b[i-0] = tmp[0];
		}
		return b;
	}

	private	static void printHex(byte[] b,int length){
		for(int i=0;i<length;i++){
			Byte bytedata =  new Byte(b[i]);
			int k = Integer.parseInt(bytedata.toString());
			if(k <= 0)k = 256 + k;
			k = k +256; //3桁目を生成
			System.out.print( Integer.toHexString(k).substring(1, 3));
			if(i%8==7){
				System.out.println("");
			}else{
				System.out.print(" ");
			}
		}
		System.out.println("");
	}

	
	
	
	
	// ファイル削除処理
	public static boolean deleteFile(File file) throws Exception
	{
         if (file.isDirectory()) {//ディレクトリの場合  
             String[] children = file.list();//ディレクトリにあるすべてのファイルを処理する  
             for (int i=0; i<children.length; i++) {  
                 boolean success = deleteFile(new File(file, children[i]));  
                 if (!success) {  
                     return false;  
                 }  
             }  
         }  
   
         // 削除  
         return file.delete();  
	}

	// ディレクトリ作成処理
//	public static void createDir(File dir) throws Exception
	public static boolean createDir(File dir) throws Exception
	{
			if (dir.exists()) {
//				throw new IOException("Folder/File already exists.");
				throw new MyUtilException(R.string.util_error_file_already_exists, "Folder/File already exists.");
			}else{
				return dir.mkdirs();    //make folders
			}

  
	}

	// ファイル名変更処理
	public static boolean renameFile(File srcFile, File dstFile) throws Exception
	{
			if (dstFile.exists()) {
//				throw new IOException("Folder/File already exists: " + dstFile.getName());
				throw new MyUtilException(R.string.util_error_file_already_exists, "Folder/File already exists.");
			}else{
				return srcFile.renameTo(dstFile);
			}

  
	}


	/**
	// ファイルコピー
	public static boolean copyFile(File srcFile, File dstFile) throws Exception
	{
			if (dstFile.exists()) {
				throw new IOException("File already exists: " + dstFile.getName());
			}
			// Copyの時はrenameじゃだめ。moveの時は一度renameを試してみる。
			//if(srcFile.renameTo(dstFile)){
			//	return true;// success to rename.
			//}
			//
			//renameできない場合は地道にコピーしていく
			return transferFile(srcFile,dstFile);
	}

	
	private static boolean transferFile(File srcFile, File dstFile) throws Exception
	{
		
        if (srcFile.isDirectory()) {//ディレクトリの場合  
        	//dstFileディレクトリを作成する。
        	boolean success0 = createDir(dstFile);
            if (!success0)return false;  
           
        	
            String[] children = srcFile.list();//ディレクトリにあるすべてのファイルを処理する  
            for (int i=0; i<children.length; i++) {
            	boolean success1 = transferFile(new File(srcFile, children[i]), new File(dstFile, children[i]));
            	if (!success1)return false;
            }
            return true;
        }else{
          	// copy 
        	fileCopy(srcFile,dstFile);
        	return true;
        }
	}
	**/

	// file copy using NIO Channel
	public static void fileCopy(File source, File target) throws Exception
//	private static void fileCopy(File source, File target) throws Exception
	{
		FileChannel in = null;
		FileChannel out = null;

		FileInputStream inStream = null;
		FileOutputStream outStream = null;

		try {
			inStream = new FileInputStream(source);
			outStream = new FileOutputStream(target);

			in = inStream.getChannel();
			out = outStream.getChannel();

			in.transferTo(0, in.size(), out);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			close(inStream);
			close(in);
			close(outStream);
			close(out);
		}
	}

	private static void close(Closeable closable) throws Exception{
		if (closable != null) {
			try {
				closable.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	
	/**
	 * 文字列を検索
	 *
	 * @param String keyword 検索文字(正規表現)
	 * @param String data    検索対象データ
	 * @param int start 検索開始位置
	 * @return GrepMatchInfo 文字列の位置、見つからない場合はnull
	 */
	public static GrepMatchInfo searchWord(String keyword, String data, int start){
		GrepMatchInfo result = null;
		Pattern pattern = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);

		if( matcher.find(start)) { //もしinitPosition以降に見つかったら
			int s = matcher.start();
			int e = s + matcher.group().length();
			result = new GrepMatchInfo(s,e);
		}
		
		return result;
		
	}

	public static GrepMatchInfo searchWordBackward(String keyword, String data, int start){
		GrepMatchInfo result = null;
		Pattern pattern = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		// start位置を超えるまで、findを続ける。超えたらその手前にマッチしたものを返す
		while(matcher.find()){
			int s = matcher.start();
			if(s >= start){ //スタート位置と同じかそれよりも後の位置でマッチしたらbreak
				break;
			}
			int e = s + matcher.group().length();
			result = new GrepMatchInfo(s,e);			
		}
				
		return result;
		
	}


	
	
	/**
	 * Dialogを表示
	 *
	 * @param msg 表示するメッセージ
	 */
	public static void showMessage(String msg, Activity activity){
		new AlertDialog.Builder(activity)
		.setMessage(msg)
		.setNeutralButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			// この中に"OK"時の処理をいれる。
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		})
		.show();
	}
	
	public static final Pattern WEB_URL_PATTERN
	= Pattern.compile(
			"((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
			+ "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
			+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)" //schema部分は必ず存在する物とする。
//			+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
			+ "((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}\\.)+"   // named host
			+ "(?:"   // plus top level domain
			+ "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
			+ "|(?:biz|b[abdefghijmnorstvwyz])"
			+ "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
			+ "|d[ejkmoz]"
			+ "|(?:edu|e[cegrstu])"
			+ "|f[ijkmor]"
			+ "|(?:gov|g[abdefghilmnpqrstuwy])"
			+ "|h[kmnrtu]"
			+ "|(?:info|int|i[delmnoqrst])"
			+ "|(?:jobs|j[emop])"
			+ "|k[eghimnrwyz]"
			+ "|l[abcikrstuvy]"
			+ "|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])"
			+ "|(?:name|net|n[acefgilopruz])"
			+ "|(?:org|om)"
			+ "|(?:pro|p[aefghklmnrstwy])"
			+ "|qa"
			+ "|r[eouw]"
			+ "|s[abcdeghijklmnortuvyz]"
			+ "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
			+ "|u[agkmsyz]"
			+ "|v[aceginu]"
			+ "|w[fs]"
			+ "|y[etu]"
			+ "|z[amw]))"
			+ "|(?:(?:25[0-5]|2[0-4]" // or ip address
			+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
			+ "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
			+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
			+ "|[1-9][0-9]|[0-9])))"
			+ "(?:\\:\\d{1,5})?)" // plus option port number
			+ "(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
			+ "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
			+ "(?:\\b|$)"); // and finally, a word boundary or end of
	// input.  This is to stop foo.sure from
	// matching as foo.su

	public static final Pattern IP_ADDRESS_PATTERN
	= Pattern.compile(
			"((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
			+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
			+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
			+ "|[1-9][0-9]|[0-9]))");


	public static final Pattern EMAIL_ADDRESS_PATTERN
	= Pattern.compile(
			"[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" +
			"\\@" +
			"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
			"(" +
			"\\." +
			"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
			")+"
	);

	public static final Pattern PHONE_PATTERN
	= Pattern.compile(                                  // sdd = space, dot, or dash
			"(\\+[0-9]+[\\- \\.]*)?"                    // +<digits><sdd>*
			+ "(\\([0-9]+\\)[\\- \\.]*)?"               // (<digits>)<sdd>*
			+ "([0-9][0-9\\- \\.][0-9\\- \\.]+[0-9])"); // <digit><digit|sdd>+<digit> 
	
 
}
