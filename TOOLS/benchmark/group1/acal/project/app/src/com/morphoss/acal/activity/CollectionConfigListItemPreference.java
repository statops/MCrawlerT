package com.morphoss.acal.activity;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.morphoss.acal.R;

public class CollectionConfigListItemPreference extends Preference {

	private int collectionColour = 0;
	private String title = null;
	private String summary = null;
	private Context context;
	
	public CollectionConfigListItemPreference(Context context) {
		super(context);
		this.context = context;
	}

	public CollectionConfigListItemPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CollectionConfigListItemPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setCollectionColour(String newColour ) {
		try {
			collectionColour = Color.parseColor(newColour);
		} catch (IllegalArgumentException iae) { }
	}

	public void setTitle(String newTitle ) {
		title = newTitle;
	}

	public void setSummary(String newSummary ) {
		summary = newSummary;
	}

	public View getView(View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (LinearLayout) inflater.inflate(R.layout.collections_list_item, parent, false);

		View colourBar = v.findViewById(R.id.CollectionItemColorBar);
		colourBar.setBackgroundColor(collectionColour);

		TextView tvTitle = (TextView) v.findViewById(android.R.id.title);
		if ( title != null ) tvTitle.setText(title);

		TextView tvSummary = (TextView) v.findViewById(android.R.id.summary);
		if ( summary != null ) tvSummary.setText(summary);

		return v;
	}
}
