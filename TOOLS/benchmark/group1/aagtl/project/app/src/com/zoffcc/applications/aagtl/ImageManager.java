/**
 * aagtl Advanced Geocaching Tool for Android
 * loosely based on agtl by Daniel Fett <fett@danielfett.de>
 * Copyright (C) 2010 - 2012 Zoff <aagtl@work.zoff.cc>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.aagtl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

public class ImageManager
{
	public void DownloadFromUrl(String imageURL, String fileName)
	{ // this is
		// the downloader method
		try
		{
			URL url = new URL(imageURL);
			File file = new File(fileName);

			//long startTime = System.currentTimeMillis();
			//Log.d("ImageManager", "download begining");
			//Log.d("ImageManager", "download url:" + url);
			//Log.d("ImageManager", "downloaded file name:" + fileName);
			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();
			ucon.setConnectTimeout(10000);
			ucon.setReadTimeout(7000);

			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is, HTMLDownloader.large_buffer_size);

			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(HTMLDownloader.default_buffer_size);
			int current = 0;
			while ((current = bis.read()) != -1)
			{
				baf.append((byte) current);
			}

			/* Convert the Bytes read to a String. */
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			fos.close();
			//Log.d("ImageManager", "download ready in"
			//		+ ((System.currentTimeMillis() - startTime) / 1000)
			//		+ " sec");

		}
		catch (SocketTimeoutException e2)
		{
			Log.d("ImageManager", "Connectiont timout: " + e2);
		}
		catch (IOException e)
		{
			Log.d("ImageManager", "Error: " + e);
		}
		catch (Exception e)
		{
			Log.d("ImageManager", "Error: " + e);
		}

	}

}
