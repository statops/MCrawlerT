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

package net.jaqpot.netcounter.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
*
 */
public class SysClassNet {

	private static final String SYS_CLASS_NET = "/sys/class/net/";

	private static final String CARRIER = "/carrier";

	private static final String RX_BYTES = "/statistics/rx_bytes";

	private static final String TX_BYTES = "/statistics/tx_bytes";

	/**
	 * Private constructor. This is an utility class.
	 */
	private SysClassNet() {
	}

	public static boolean isUp(String inter) {
		StringBuilder sb = new StringBuilder();
		sb.append(SYS_CLASS_NET).append(inter).append(CARRIER);
		return new File(sb.toString()).canRead();
	}

	public static long getRxBytes(String inter) throws IOException {
		return readLong(inter, RX_BYTES);
	}

	public static long getTxBytes(String inter) throws IOException {
		return readLong(inter, TX_BYTES);
	}

	private static RandomAccessFile getFile(String filename) throws IOException {
		File f = new File(filename);
		return new RandomAccessFile(f, "r");
	}

	private static long readLong(String inter, String file) {
		StringBuilder sb = new StringBuilder();
		sb.append(SYS_CLASS_NET).append(inter).append(file);
		RandomAccessFile raf = null;
		try {
			raf = getFile(sb.toString());
			return Long.valueOf(raf.readLine());
		} catch (Exception e) {
			return 0;
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
