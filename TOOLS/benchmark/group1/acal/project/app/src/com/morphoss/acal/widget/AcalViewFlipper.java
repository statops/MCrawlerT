/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ViewFlipper;

public class AcalViewFlipper extends ViewFlipper {

	private final static String	TAG	= "AcalViwFlipper";

	public AcalViewFlipper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDetachedFromWindow() {
		try {
			super.onDetachedFromWindow();
		}
        catch( IllegalArgumentException e )
        {
           Log.w( TAG, "Android project issue 6191 workaround." );
           /* Quick catch and continue on api level 7, the Eclair 2.1 */
        }
		catch (Exception e) {
			Log.d(TAG, "Stopped some other viewflipper crash not issue 6191");
			Log.d(TAG,Log.getStackTraceString(e));
		}
        finally
        {
           super.stopFlipping();
        }
	}

}
