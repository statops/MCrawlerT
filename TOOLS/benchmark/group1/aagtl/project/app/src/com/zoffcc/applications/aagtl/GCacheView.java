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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;

public class GCacheView extends ImageView
{

	GeocacheCoordinate gc = null;

	Paint box_paint = new Paint(0);
	Paint text_paint = new Paint(0);

	int details_loaded = 0;
	String gc_name_current = "";
	String gc_name_previous = "";
	int get_gc_from_db = 0;

	Boolean need_repaint = true;
	Boolean override_download = false;

	public Boolean download_details_thread_finished = false;

	public static final int SHOW_DESC = 1;
	public static final int SHOW_SHORT_DESC = 2;
	public static final int SHOW_LOGS = 3;
	public static final int SHOW_HINTS = 4;
	public static final int SHOW_WAYPOINTS = 5;

	public int show_field = SHOW_SHORT_DESC;

	aagtl main_aagtl;

	public GCacheView(Context context, aagtl main_aagtl)
	{
		super(context);

		text_paint.setColor(Color.WHITE);
		// text_paint.setStyle(Paint.Style.FILL);
		text_paint.setTextSize(16);
		// text_paint.setTypeface(Typeface.DEFAULT_BOLD);
		text_paint.setAntiAlias(true);

		this.main_aagtl = main_aagtl;
		this.clear_stuff();
	}

	public void set_cache(GeocacheCoordinate co)
	{
		// System.out.println("in set_cache");

		need_repaint = true;

		try
		{
			this.gc_name_previous = gc.name;
			// System.out.println("" + String.valueOf(this.gc.name));
		}
		catch (NullPointerException e)
		{
			this.gc_name_previous = "";
			// System.out.println("null");
		}

		this.gc_name_current = co.name;

		// System.out.println("cur=" + this.gc_name_current + " prev=" +
		// this.gc_name_previous);
		if (!this.gc_name_current.matches(this.gc_name_previous))
		{
			// System.out.println("details_loaded = 0");
			this.details_loaded = 0;
			// take new GC as cache-to-view
			this.gc = co;
			// set field back to shortdesc.
			show_field = GCacheView.SHOW_SHORT_DESC;
		}

		// System.out.println("" + String.valueOf(this.gc.name));
	}

	public GeocacheCoordinate get_cache()
	{
		return this.gc;
	}

	private class Thread_gcv1 extends Thread
	{
		Handler mHandler;
		Boolean do_close = false;

		Thread_gcv1(Handler h)
		{
			mHandler = h;
		}

		public void close_dialog()
		{
			this.do_close = true;
		}

		public void run()
		{
			// System.out.println("thread xx started");
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("command", 0);
			msg.setData(b);
			mHandler.sendMessage(msg);
			while (!do_close)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					this.do_close = true;
				}
			}
			// System.out.println("thread xx finished");
			msg = mHandler.obtainMessage();
			b = new Bundle();
			b.putInt("command", 1);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}

	private class Thread_gcv2 extends Thread
	{
		GCacheView main = null;

		Thread_gcv2(GCacheView main_ref)
		{
			this.main = main_ref;
		}

		public void run()
		{
			Thread_gcv1 t1 = new Thread_gcv1(this.main.main_aagtl.dl_handler);
			t1.start();

			// System.out.println("thread yy started");
			this.main.main_aagtl.cdol.update_coordinate(this.main.gc);
			// System.out.println("thread yy finished");
			this.main.download_details_thread_finished = true;
			this.main.details_loaded = 2;
			Message msg = this.main.main_aagtl.dl_handler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("command", 2);
			msg.setData(b);
			this.main.main_aagtl.dl_handler.sendMessage(msg);

			t1.close_dialog();
		}
	}

	public void onDraw(Canvas c)
	{
		// System.out.println("GCacheView: onDraw");

		if (this.gc == null)
		{
			c.drawColor(Color.RED);
		}
		else
		{
			try
			{
				c.drawColor(Color.BLACK);
				c.drawText(gc.name, 10, 50, text_paint);
				c.drawText(gc.title, 10, 50 + 30, text_paint);
				c.drawText(gc.type, 10, 50 + 2 * 30, text_paint);
				c.drawText("size: " + String.valueOf(gc.size), 10, 50 + 3 * 30, text_paint);
				c.drawText("terrain: " + String.valueOf(gc.terrain), 10, 50 + 4 * 30, text_paint);

				if (this.get_gc_from_db == 1)
				{
					// check in db for cache details
					c.drawText("checking database ...", 10, 50 + 6 * 30, text_paint);
					this.gc = this.main_aagtl.pv.get_point_full(this.gc.name);
					System.out.println("checking database");
					System.out.println("DESC=" + this.gc.desc);
					if (this.gc.desc==null)
					{
						this.gc.desc="please update details";
					}
					this.get_gc_from_db = 0;
				}

				if (this.details_loaded == 0)
				{
					if (!this.override_download)
					{
						// check in db for cache details
						c.drawText("checking database ...", 10, 50 + 6 * 30, text_paint);
						this.gc = this.main_aagtl.pv.get_point_full(this.gc.name);
						System.out.println("checking database");
						System.out.println("DESC=" + this.gc.desc);
					}
					else
					{
						// ok override, and download from internet.
						// set option back, this is a 1-shot option!
						System.out.println("here 001");
						this.override_download = false;
						// make sure we download it!
						this.gc.desc = null;
					}

					if (this.gc.desc == null)
					{
						// download from internet (in background thread)
						c.drawColor(Color.BLACK);

						c.drawText(gc.name, 10, 50, text_paint);
						c.drawText(gc.title, 10, 50 + 30, text_paint);
						c.drawText(gc.type, 10, 50 + 2 * 30, text_paint);
						c.drawText("size: " + String.valueOf(gc.size), 10, 50 + 3 * 30, text_paint);
						c.drawText("terrain: " + String.valueOf(gc.terrain), 10, 50 + 4 * 30, text_paint);

						c.drawText("downloading ...", 10, 50 + 6 * 30, text_paint);

						// System.out.println("downloading from internet");
						this.download_details_thread_finished = false;
						this.details_loaded = 1;
						Thread_gcv2 t2 = new Thread_gcv2(this);
						t2.start();

						need_repaint = true;
					}
					else
					{
						// ok found something in DB
						this.download_details_thread_finished = true;
						this.details_loaded = 2;

						need_repaint = true;
					}

				}
				else if (this.details_loaded == 1)
				{
					c.drawColor(Color.BLACK);
					c.drawText(gc.name, 10, 50, text_paint);
					c.drawText(gc.title, 10, 50 + 30, text_paint);
					c.drawText(gc.type, 10, 50 + 2 * 30, text_paint);
					c.drawText("size: " + String.valueOf(gc.size), 10, 50 + 3 * 30, text_paint);
					c.drawText("status: " + String.valueOf(GeocacheCoordinate.STATUS_HASH.get(gc.status)), 150, 50 + 3 * 30, text_paint);
					c.drawText("diff: " + String.valueOf((float) gc.difficulty / 10f), 10, 50 + 4 * 30, text_paint);
					c.drawText("terrain: " + String.valueOf((float) gc.terrain / 10f), 10, 50 + 5 * 30, text_paint);

					c.drawText("downloading ...", 10, 50 + 6 * 30, text_paint);
				}

				if (this.details_loaded == 2)
				{
					if (!need_repaint) // &&
										// (this.gc_name_current.matches(this.gc_name_previous)))
					{
						// dont repaint all the time
						return;
					}

					need_repaint = false;
					c.drawColor(Color.BLACK);
					c.drawText(gc.name, 10, 50, text_paint);
					c.drawText(gc.title, 10, 50 + 30, text_paint);
					c.drawText(gc.type, 10, 50 + 2 * 30, text_paint);
					c.drawText("size: " + String.valueOf(gc.size), 10, 50 + 3 * 30, text_paint);
					c.drawText("status: " + String.valueOf(GeocacheCoordinate.STATUS_HASH.get(gc.status)), 150, 50 + 3 * 30, text_paint);
					c.drawText("diff: " + String.valueOf((float) gc.difficulty / 10f), 10, 50 + 4 * 30, text_paint);
					c.drawText("terrain: " + String.valueOf((float) gc.terrain / 10f), 10, 50 + 5 * 30, text_paint);

					// c.drawText("details loaded", 10, 50 + 6 * 30, text_paint);
					c.drawText("", 10, 50 + 6 * 30, text_paint);

					String show_field_text = this.gc.shortdesc;
					switch (this.show_field)
					{
					case GCacheView.SHOW_SHORT_DESC:
						// already set above, as default. dont set again
						// show_field_text = this.gc.shortdesc;
						break;
					case GCacheView.SHOW_DESC:
						show_field_text = this.gc.desc;
						break;
					case GCacheView.SHOW_LOGS:
						show_field_text = this.gc.logs;
						break;
					case GCacheView.SHOW_HINTS:
						show_field_text = this.gc.hints;
						break;
					case GCacheView.SHOW_WAYPOINTS:
						show_field_text = this.gc.waypoints;
						break;
					}
					String nice_text = Html.fromHtml(show_field_text).toString().replaceAll("\n", "<br>");
					// add white text color
					nice_text = "<style> * { color: #FFFFFF; } </style> " + nice_text;

					// load the text into the webview
					this.main_aagtl.wv.loadDataWithBaseURL(null, nice_text, "text/html", "UTF-8", null);

					// transparent bg-color
					// this.main_aagtl.wv.setBackgroundColor(0);
					this.main_aagtl.wv.setBackgroundColor(Color.BLACK);
					// disable javascript
					this.main_aagtl.wv.getSettings().setJavaScriptEnabled(false);
					this.main_aagtl.wv.setVisibility(View.VISIBLE);
					this.main_aagtl.wv.bringToFront();
				}
			}
			catch (NullPointerException e)
			{

			}
		}
	}

	public void clear_stuff()
	{
	}

	public void onSizeChanged(int width, int height, int old_w, int old_h)
	{
		// System.out.println("GCacheView: onSizeChanged");
	}

}
