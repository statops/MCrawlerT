package com.morphoss.acal;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class L10nUtil {

	public static String capitaliseWords( String boringString ) {
		final Pattern wordSplitter = Pattern.compile("\\b(\\w+)\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
		StringBuffer shinyNewString = new StringBuffer();
		Matcher m = wordSplitter.matcher(boringString);
		while( m.find() ) {
			if ( m.group().matches("(de|von|in|to|for|of|vom|zu)") )
				m.appendReplacement(shinyNewString, m.group());
			else
				m.appendReplacement(shinyNewString, m.group().substring(0,1).toUpperCase() + m.group().substring(1));
		}
		m.appendTail(shinyNewString);
		return shinyNewString.toString();
	}

	
	public static String numSuffix( int suffixMe ) {
		Locale l = Locale.getDefault();
		if ( l.equals(Locale.ENGLISH) ) {
			switch (suffixMe % 10) {
				case 1:		return Integer.toString(suffixMe)+"st";
				case 2:		return Integer.toString(suffixMe)+"nd";
				case 3:		return Integer.toString(suffixMe)+"rd";
				default:	return Integer.toString(suffixMe)+"th";
			}
		}
		else {
			return Integer.toString(suffixMe);
		}
	}
}
