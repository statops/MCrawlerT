package com.morphoss.acal.acaltime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;

import com.morphoss.acal.R;
import com.morphoss.acal.StaticHelpers;

public class AcalDateTimeFormatter {

	public static DateFormat longDate = DateFormat.getDateInstance(DateFormat.LONG);
	public static DateFormat shortDate = DateFormat.getDateInstance(DateFormat.SHORT);
	public static DateFormat timeAmPm = new SimpleDateFormat("hh:mmaa");
	public static DateFormat time24Hr = new SimpleDateFormat("HH:mm");

	public static DateFormat longDateTime = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

	public static String fmtFull( AcalDateTime dateTime, boolean prefer24hourFormat) {
		if ( dateTime == null ) return "- - - - - -";

		Date javaDate = dateTime.toJavaDate();
		StringBuilder b = new StringBuilder();
		if ( !dateTime.isDate() ) {
			b.append((prefer24hourFormat?time24Hr:timeAmPm).format(javaDate).toLowerCase());
			b.append(", ");
		}
		b.append(StaticHelpers.capitaliseWords(longDate.format(javaDate)));
		if ( !dateTime.isDate() && !dateTime.isFloating() ) {
			if ( ! TimeZone.getDefault().getID().equalsIgnoreCase( dateTime.getTimeZoneId() ) ) {
				b.append('\n');
				b.append(dateTime.getTimeZoneId());
			}
		}
		return b.toString();
	}

	/**
	 * TODO - Localisation doesn't work properly See http://code.google.com/p/android/issues/detail?id=12679
	 * 
	 * 
	 * Format an AcalDateTime into a short localised date/time of the format DATE, TIME where Date is a Short date
	 * specified by devices localisation settings and time is  hh:mmaa or HH:mm depending on the prefer24hourformat parameter.
	 * 
	 * If showDateIfToday is false AND if the dateTime passed is todays date, DATE = stringIfToday.
	 *  
	 * @param dateTime
	 * @param prefer24hourFormat
	 * @param showDateIfToday
	 * @param stringIfToday
	 * @return
	 */
	public static String fmtShort(AcalDateTime dateTime, boolean prefer24hourFormat, boolean showDateIfToday, String stringIfToday) {
		if ( dateTime == null ) return "- - - -";
		Date javaDate = dateTime.toJavaDate();
		boolean showDate = true;
		if (!showDateIfToday) {
			AcalDateTime now = new AcalDateTime().applyLocalTimeZone();
			if (now.applyLocalTimeZone().getEpochDay() == dateTime.clone().applyLocalTimeZone().getEpochDay()) showDate = false;
		}


		StringBuilder b = new StringBuilder();
		if (showDate) b.append(shortDate.format(javaDate));
		else b.append(stringIfToday);
		b.append(", ");
		if ( !dateTime.isDate() ) {
			b.append((prefer24hourFormat?time24Hr:timeAmPm).format(javaDate).toLowerCase());

		}

		if ( !dateTime.isDate() && !dateTime.isFloating() ) {
			if ( ! TimeZone.getDefault().getID().equalsIgnoreCase( dateTime.getTimeZoneId() ) ) {
				b.append(dateTime.getTimeZoneId());
			}
		}
		return b.toString();
	}

	public static String getDisplayTimeTextFull(AcalDateRange viewRange,  AcalDateTime start, AcalDateTime finish, boolean as24HourTime ) {
		AcalDateTime viewDateStart = viewRange.start;
		AcalDateTime viewDateEnd = viewRange.end;
		String timeText = "";
		String timeFormatString = (as24HourTime ? "HH:mm" : "hh:mmaa");
		SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormatString);

		if ( start.before(viewDateStart) || (finish != null && finish.after(viewDateEnd)) ){
			if ( start.isDate() ) {
				timeText = AcalDateTime.fmtDayMonthYear(start)+ ", all day";
			}
			else {
				SimpleDateFormat startFormatter = timeFormatter;
				SimpleDateFormat finishFormatter = timeFormatter;

				if ( start.before(viewDateStart) || start.after(viewDateEnd) ) {
					startFormatter  = new SimpleDateFormat("MMM d, "+timeFormatString);
					if ( (finish.getYear() > start.getYear()) || (finish.getYearDay() > start.getYearDay()) )
						finishFormatter = new SimpleDateFormat("MMM d, "+timeFormatString);
				}

				timeText = (startFormatter.format(start.toJavaDate())+" - "
						+ (finish == null ? "null" : finishFormatter.format(finish.toJavaDate())));
			}
		}
		else if ( start.isDate()) {
			timeText = "All Day";
		}
		else {
			timeText = (timeFormatter.format(start.toJavaDate())+" - "
					+ (finish == null ? "null" : timeFormatter.format(finish.toJavaDate())));
		}
		return timeText;
	}
	
	/**
	* Return a pretty string indicating the time period of the event.  If the start or end
	* are on a different date to the view start/end then we also include the date on that
	* element.  If it is an all day event, we say so. 
	* @param viewDateStart - the start of the viewed range
	* @param viewDateEnd - the end of the viewed range
	* @param as24HourTime - from the pref
	* @return A nicely formatted string explaining the start/end of the event.
	*/
	public static String getDisplayTimeText(Context c,
			AcalDateTime viewDateStart, AcalDateTime viewDateEnd,
			AcalDateTime start, AcalDateTime end, boolean as24HourTime,
			boolean isAllDay) {
		String timeText = "";
		String timeFormatString = (as24HourTime ? "HH:mm" : "hh:mmaa");
		SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormatString);

		if ( start == null && end == null ) {
			return "null - null";
		}
		else if ( start == null ) {
			start = end;
		}
		Date st = start.toJavaDate();
		Date en = (end == null ? (isAllDay ? AcalDateTime.addDays(start, 1).toJavaDate() : st) : end.toJavaDate());
		if (start.before(viewDateStart) || end.after(viewDateEnd)) {
			if (isAllDay) {
				timeFormatter = new SimpleDateFormat("MMM d");
				timeText = c.getString(R.string.AllDaysInPeriod,
						timeFormatter.format(st), timeFormatter.format(en));
			} else {
				SimpleDateFormat startFormatter = timeFormatter;
				SimpleDateFormat finishFormatter = timeFormatter;

				if (start.before(viewDateStart))
					startFormatter = new SimpleDateFormat("MMM d, "
							+ timeFormatString);
				if (end != null && end.after(viewDateEnd))
					finishFormatter = new SimpleDateFormat("MMM d, "
							+ timeFormatString);

				timeText = startFormatter.format(st) + (end == null ? "" : " - " + finishFormatter.format(en));
			}
		} else if (isAllDay) {
			timeText = c.getString(R.string.ForTheWholeDay);
		} else {
			timeText = timeFormatter.format(st) + " - "
					+ timeFormatter.format(en);
		}
		return timeText;
	}

	/**
	 * Formatter for handling Task start/due/completed dates.
	 * @param c
	 * @param dtstart
	 * @param due
	 * @param completed
	 * @param as24HourTime
	 * @return
	 */
	public static CharSequence getTodoTimeText(Context c,
			AcalDateTime dtstart, AcalDateTime due, AcalDateTime completed, boolean as24HourTime) {

		if ( dtstart == null && due == null && completed == null ) return c.getString(R.string.Unscheduled);

		SimpleDateFormat formatter = new SimpleDateFormat(" MMM d, "+(as24HourTime ? "HH:mm" : "hh:mmaa"));

		return (dtstart == null ? "" : c.getString(R.string.FromPrompt) + format(formatter, dtstart)) +
				(dtstart != null && due != null ? " - " : "") +
				(due == null ? "" : c.getString(R.string.DuePrompt) + format(formatter, due)) +
				(completed != null ? (due != null || dtstart != null ? ", " : "") + 
						c.getString(R.string.CompletedPrompt) + format(formatter,completed):"")
				;
	}

	public static CharSequence getJournalTimeText(Context c, AcalDateTime dtstart, boolean as24HourTime) {
		if ( dtstart == null ) return c.getString(R.string.Unscheduled);

		SimpleDateFormat formatter = new SimpleDateFormat(" MMM d, "+(as24HourTime ? "HH:mm" : "hh:mmaa"));

		return c.getString(R.string.FromPrompt) + format(formatter, dtstart);
	}

	
	public static CharSequence format( SimpleDateFormat formatter, AcalDateTime when ) {
		return formatter.format(when.toJavaDate());
	}
}
