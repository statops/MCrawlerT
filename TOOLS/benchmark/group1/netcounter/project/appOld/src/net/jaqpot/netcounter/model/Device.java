/*
 * Copyright (C) 2009 Cyril Jaquier
 *
 * This file is part of NetCounter.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.jaqpot.netcounter.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.service.SysClassNet;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

/**
 * 
 */
public abstract class Device {

	private static Device instance = null;

	private String[] mInterfaces = null;

	public synchronized static Device getDevice() {
		if (instance == null) {
			Log.i(Device.class.getName(), "Device: " + Build.DEVICE);
			// // All the devices we know about.
			// Device[] allDevices = { new DefaultDevice(), new DreamDevice(), new GenericDevice(),
			// new SamsungI7500Device(), new PulseDevice(), new DroidDevice(), new EveDevice() };
			// // Iterates over all the devices and try to found the corresponding one.
			// for (Device device : allDevices) {
			// if (Arrays.asList(device.getNames()).contains(Build.DEVICE)) {
			// instance = device;
			// break;
			// }
			// }
			// // Nothing found? Use the default device.
			// if (instance == null) {
			// instance = allDevices[0];
			// }
			instance = new DiscoverableDevice();
		}
		return instance;
	}

	public abstract String[] getNames();

	public abstract String getCell();

	public abstract String getWiFi();

	public abstract String getBluetooth();

	public synchronized String[] getInterfaces() {
		if (mInterfaces == null) {
			List<String> tmp = new ArrayList<String>();
			if (getCell() != null) {
				tmp.add(getCell());
			}
			if (getWiFi() != null) {
				tmp.add(getWiFi());
			}
			if (getBluetooth() != null) {
				tmp.add(getBluetooth());
			}
			mInterfaces = tmp.toArray(new String[tmp.size()]);
		}
		return mInterfaces;
	}

	public String getPrettyName(String inter) {
		Resources r = NetCounterApplication.resources();
		if (getCell() != null && getCell().equals(inter)) {
			return r.getString(R.string.interfaceTypeCell);
		} else if (getWiFi() != null && getWiFi().equals(inter)) {
			return r.getString(R.string.interfaceTypeWifi);
		} else if (getBluetooth() != null && getBluetooth().equals(inter)) {
			return r.getString(R.string.interfaceTypeBluetooth);
		} else {
			return inter;
		}
	}

	public int getIcon(String inter) {
		if (getCell() != null && getCell().equals(inter)) {
			return R.drawable.cell;
		} else if (getBluetooth() != null && getBluetooth().equals(inter)) {
			return R.drawable.bluetooth;
		} else {
			return R.drawable.wifi;
		}
	}

}

/**
 * Automatically discover the network interfaces. No real magic here, just try different possible
 * solutions.
 */
class DiscoverableDevice extends Device {

	private static final String[] CELL_INTERFACES = { //
	"rmnet0", "pdp0", "ppp0" //
	};

	private static final String[] WIFI_INTERFACES = { //
	"eth0", "tiwlan0", "wlan0", "athwlan0", "eth1" //
	};

	private String mCell = null;

	private String mWiFi = null;

	@Override
	public String[] getNames() {
		return null;
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		if (mCell == null) {
			for (String inter : CELL_INTERFACES) {
				if (SysClassNet.isUp(inter)) {
					Log.i(getClass().getName(), "Cell interface: " + inter);
					mCell = inter;
					break;
				}
			}
		}
		return mCell;
	}

	@Override
	public String getWiFi() {
		if (mWiFi == null) {
			for (String inter : WIFI_INTERFACES) {
				if (SysClassNet.isUp(inter)) {
					Log.i(getClass().getName(), "WiFi interface: " + inter);
					mWiFi = inter;
					break;
				}
			}
		}
		return mWiFi;
	}

	@Override
	public synchronized String[] getInterfaces() {
		// Do not cache the array.
		List<String> tmp = new ArrayList<String>();
		if (getCell() != null) {
			tmp.add(getCell());
		}
		if (getWiFi() != null) {
			tmp.add(getWiFi());
		}
		if (getBluetooth() != null) {
			tmp.add(getBluetooth());
		}
		return tmp.toArray(new String[tmp.size()]);
	}

	@Override
	public String getPrettyName(String inter) {
		Resources r = NetCounterApplication.resources();
		if (getCell() != null && getCell().equals(inter)) {
			return r.getString(R.string.interfaceTypeCell);
		} else if (getWiFi() != null && getWiFi().equals(inter)) {
			return r.getString(R.string.interfaceTypeWifi);
		} else if (getBluetooth() != null && getBluetooth().equals(inter)) {
			return r.getString(R.string.interfaceTypeBluetooth);
		}
		// If nothing found, try to return a best guess...
		if (Arrays.asList(CELL_INTERFACES).contains(inter)) {
			return r.getString(R.string.interfaceTypeCell);
		} else if (Arrays.asList(WIFI_INTERFACES).contains(inter)) {
			return r.getString(R.string.interfaceTypeWifi);
		}
		// Really but really nothing found.
		return inter;
	}

	@Override
	public int getIcon(String inter) {
		if (getCell() != null && getCell().equals(inter)) {
			return R.drawable.cell;
		} else if (getBluetooth() != null && getBluetooth().equals(inter)) {
			return R.drawable.bluetooth;
		}
		// If nothing found, try to return a best guess...
		if (Arrays.asList(CELL_INTERFACES).contains(inter)) {
			return R.drawable.cell;
		} else if (Arrays.asList(WIFI_INTERFACES).contains(inter)) {
			return R.drawable.wifi;
		}
		// Really but really nothing found.
		return R.drawable.wifi;
	}

}

/**
 * Generic device implementation corresponding to the emulator.
 */
class GenericDevice extends Device {

	@Override
	public String[] getNames() {
		return new String[] { "generic" };
	}

	@Override
	public String getBluetooth() {
		return null;
	}

	@Override
	public String getCell() {
		return null;
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}

}

/**
 * Default device implementation.
 */
class DefaultDevice extends Device {

	private static final String INTERFACE_PATTERN = "^wifi\\.interface=(\\S+)$";

	private static final String BUILD_PROP = "/system/build.prop";

	private String mWifi = null;

	@Override
	public String[] getNames() {
		return new String[0];
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		// 
		if (mWifi == null) {
			Pattern pattern = Pattern.compile(INTERFACE_PATTERN);
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(BUILD_PROP));
				String line;
				while ((line = br.readLine()) != null) {
					Matcher matcher = pattern.matcher(line);
					if (matcher.matches()) {
						mWifi = matcher.group(1);
						break;
					}
				}
			} catch (IOException e) {
				Log.e(getClass().getName(), "Unable to discover WiFi interface", e);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						// Silently ignore.
					}
				}
			}
			// Nothing found. Returns a "possible" default.
			if (mWifi == null) {
				mWifi = "eth0";
			}
		}
		return mWifi;
	}

}

/**
 * Default device implementation corresponding to the HTC Dream and HTC Magic.
 */
class DreamDevice extends Device {

	@Override
	public String[] getNames() {
		// TODO Get the device name of the HTC Magic.
		return new String[] { "dream" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "tiwlan0";
	}

}

/**
 * Device implementation for the Samsung I7500. Also works with the I5700 (Spica).
 */
class SamsungI7500Device extends Device {

	@Override
	public String[] getNames() {
		return new String[] { "GT-I7500", "spica", "GT-I5700" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "pdp0";
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}

}

/**
 * Device implementation for the T-Mobile Pulse (Huawei U8220). Also works for the Google Nexus One
 * and HTC Desire.
 */
class PulseDevice extends Device {

	@Override
	public String[] getNames() {
		return new String[] { "U8220", "passion", "bravo" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}

}

/**
 * Device implementation for the Motorola Droid.
 */
class DroidDevice extends Device {

	@Override
	public String[] getNames() {
		return new String[] { "sholes" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "ppp0";
	}

	@Override
	public String getWiFi() {
		return "tiwlan0";
	}

}

/**
 * Device implementation for the LG Eve Android GW620R.
 */
class EveDevice extends Device {

	@Override
	public String[] getNames() {
		return new String[] { "EVE" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "wlan0";
	}

}