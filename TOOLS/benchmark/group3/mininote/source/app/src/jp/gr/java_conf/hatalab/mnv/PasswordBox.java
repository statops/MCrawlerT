package jp.gr.java_conf.hatalab.mnv;

import java.util.Date;

import ssh.MD5;

public class PasswordBox {
	private static byte[] passDigest;
	private static long last_time;//タイマーに使う時刻格納変数
	private static long EXPIRED_TIME = 5*60*1000;//5分でタイマー満了
	
//	private Form inputPassword;
//	private TextField  passField;//テキストフィールド3(PASSWORD)
	
//	private TextBox tBox;
	    
	public PasswordBox(){
		passDigest = null;//new byte[16];
		last_time = 0;
	}
	
	public static byte[] getPassDigest(){
		expiredCheck();//タイマー満了していたらnullになる。
		return passDigest;
	}
	public static void resetPassword(){
		passDigest = null;
		last_time = 0;
	}
	public static void setPassword(String password){
		MD5 md5 = new MD5();
		byte[] pass = md5.digest(password.getBytes());
		if(passDigest == null){
			passDigest = new byte[16];
		}
		System.arraycopy(pass,0,passDigest,0,16);
		last_time = new Date().getTime();
		return ;
	}
	
	// true　タイマー満了していません。last_timeをupdateしました。
	// false　タイマー満了しました。　パスワードをリセットします。
	public static boolean expiredCheck(){
		long now = new Date().getTime();
		if(now - last_time > EXPIRED_TIME){
			resetPassword();
			return false;
		}else{
			last_time = now;
			return true;//タイマー満了していません。last_timeをupdateしました。
		}
	}
	
	/**
	public void setPassDigest(byte[] b){
		if(passDigest == null){
			passDigest = new byte[16];
		}
		System.arraycopy(b,0,passDigest,0,16);
	}
	**/
	
	//EXPIRED_TIMEを設定
	public static void setTimerVal(int min){
		EXPIRED_TIME = min*60*1000;
	}
	/**
	public static void updateResetTimer(){
		if(passDigest != null ){ //パスワードがセットされていなければ
			long now = new Date().getTime();
			last_time = now; //last_timeを更新
		}
	}
	**/
	
}
