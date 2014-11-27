package jp.gr.java_conf.hatalab.mnv;

import java.io.File;

public class FileInfo {
	private File mFile;
	private String mData = "";
	private int mSelStart;
	private int mSelEnd;
	
	
	public File getFile() {
		return mFile;
	}
	public void setFile(File mFile) {
		this.mFile = mFile;
	}
	public String getData() {
		return mData;
	}
	public void setData(String mData) {
		this.mData = mData;
	}
	public int getSelStart() {
		return mSelStart;
	}
	public void setSelStart(int mSelStart) {
		this.mSelStart = mSelStart;
	}
	public int getSelEnd() {
		return mSelEnd;
	}
	public void setSelEnd(int mSelEnd) {
		this.mSelEnd = mSelEnd;
	}
	

}
