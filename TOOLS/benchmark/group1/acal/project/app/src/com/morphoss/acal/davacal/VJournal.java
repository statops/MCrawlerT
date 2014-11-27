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

package com.morphoss.acal.davacal;

import com.morphoss.acal.dataservice.JournalInstance;



public class VJournal extends Masterable {
	public static final String TAG = "aCal VJournal";
	
	public VJournal(ComponentParts splitter, VComponent parent) {
		super(splitter, parent);
	}

	public VJournal( VCalendar parent ) {
		super(VComponent.VJOURNAL, parent );
	}
	public  VJournal(JournalInstance journal) {
		super(VComponent.VJOURNAL,journal);

	}
	public VJournal() {
		this( new VCalendar() );
	}
	
}
