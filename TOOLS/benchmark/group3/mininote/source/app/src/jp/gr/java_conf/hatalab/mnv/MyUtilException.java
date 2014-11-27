package jp.gr.java_conf.hatalab.mnv;

public class MyUtilException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int code;
	
	public MyUtilException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
