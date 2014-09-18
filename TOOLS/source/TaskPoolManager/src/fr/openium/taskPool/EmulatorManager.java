package fr.openium.taskPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class EmulatorManager implements Runnable {

	private String mSdk_adb;
	private IResultReceiver mResult;
	public static final int GET_AVAILABLE_EMULATEUR_LIST = 0;
	public static final int GET_AVAILABLE_EMULATEUR_WITHOUT_A_PACKAGE_RUNNING = 1;
	private static final String GET_DEVICE_LIST = "devices";
	private final int mTask;
	private String mPackageName;
	private String OFFLINE = "offline";
	private String DEVICE = "device";

	/**
	 * 
	 * @param sdk
	 *            : sdk path
	 * @param result
	 *            : the Interface to return the result
	 * @param task
	 *            : the type of task GET_AVAILABLE_EMULATEUR_LIST or
	 *            GET_AVAILABLE_EMULATEUR_WITHOUT_A_PACKAGE_RUNNING
	 * @param packageName
	 *            , if GET_AVAILABLE_EMULATEUR_WITHOUT_A_PACKAGE_RUNNING give
	 *            the package name to check else give null
	 */
	public EmulatorManager(String sdk, IResultReceiver result, int task,
			String packageName) {
		mSdk_adb = sdk;
		mResult = result;
		mTask = task;
		mPackageName = packageName;
	}

	private ArrayList<String> readProcessOutput(Process p) throws IOException {
		ArrayList<String> Bline = new ArrayList<String>();
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String ligne;
		while ((ligne = br.readLine()) != null) {
			Bline.add(ligne);
		}
		return Bline;
	}

	public ArrayList<String> getList_Available_Emulator() {
		String[] command = new String[] { mSdk_adb, GET_DEVICE_LIST };

		try {
			ProcessBuilder getEmulator_list_process = new ProcessBuilder(
					command);
			Process p = getEmulator_list_process.start();
			ArrayList<String> output = readProcessOutput(p);
			ArrayList<String> deviceList = new ArrayList<String>();
			output.remove(0);
			for (String emu : output) {
				if (emu == null || emu.equalsIgnoreCase("")) {
					continue;
				}
				StringTokenizer st = new StringTokenizer(emu);
				String deviceName = st.nextToken();
				String deviceStatus = st.nextToken();
				if (isNotOffLine(deviceStatus)) {
					deviceList.add(deviceName);
				}

			}
			if (mResult != null)
				mResult.update(deviceList);
			return deviceList;
			/**
			 * get the output
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	private boolean isNotOffLine(String deviceStatus) {
		// return !deviceStatus.equalsIgnoreCase(OFFLINE);
		return deviceStatus.equalsIgnoreCase(DEVICE);
	}

	public ArrayList<String> getList_Available_Emulator_without_Package() {
		// adb shell ps | grep com.android | awk '{print $9}'
		ArrayList<String> deviceList = new ArrayList<String>();
		ArrayList<String> currentDevice = getList_Available_Emulator();
		for (String emu : currentDevice) {
			if (isRunningOn(emu)) {
				deviceList.add(emu);
			}
		}
		if (mResult != null)
			mResult.update(deviceList);
		return deviceList;
	}

	private boolean isRunningOn(String emu) {
		String[] command = new String[] { mSdk_adb, "-s", emu, "shell", "ps" };
		System.out.println("Emulateur: " + emu);
		if (mPackageName == null) {
			throw new NullPointerException("the package name is Null");
		}
		ProcessBuilder getEmulator_list_process = new ProcessBuilder(command);
		try {
			Process p;
			p = getEmulator_list_process.start();
			ArrayList<String> output = readProcessOutput(p);
			System.out.println(output.toString());
			if (output.size() > 0 && output.toString().contains(mPackageName)) {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void run() {
		switch (mTask) {
		case GET_AVAILABLE_EMULATEUR_LIST:
			getList_Available_Emulator();
			break;
		case GET_AVAILABLE_EMULATEUR_WITHOUT_A_PACKAGE_RUNNING:
			getList_Available_Emulator_without_Package();
			break;

		default:
			break;
		}

	}
}
