/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.morphoss.acal.R;

/**
 * @author Morphoss Ltd
 */

public class NumberPickerDialog extends Dialog implements OnClickListener {
	
	private NumberPicker numberPicker;
	private Button okButton;
	private Button cancelButton;
	private NumberSelectedListener nsl; 
	private EditText current;
	public NumberPickerDialog(Context context, NumberSelectedListener nsl, int initialValue, int min, int max)  {
    	super(context);
        setContentView(R.layout.number_picker_dialog);
        this.nsl = nsl;
        
        numberPicker = (NumberPicker)this.findViewById(R.id.NumberPicker);
        this.numberPicker.setRange(min,max);
        
        numberPicker.setCurrent(initialValue);
        
        current = (EditText)this.findViewById(R.id.timepicker_input);
        okButton = (Button)this.findViewById(R.id.NumberPickerOkButton);
        cancelButton = (Button)this.findViewById(R.id.NumberPickerCancelButton);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        
    }
	   
	@Override
	public void onClick(View v) {
		if (v == okButton) {
			try {
				int number = Integer.parseInt(current.getText().toString());
				if (number <= numberPicker.mEnd && number >= numberPicker.mStart)nsl.onNumberSelected(number);
			} catch (Exception e){}
		}
		this.dismiss();
	}
}
