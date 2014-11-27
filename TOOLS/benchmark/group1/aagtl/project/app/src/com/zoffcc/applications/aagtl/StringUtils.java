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

import java.util.StringTokenizer;

public class StringUtils
{
	// \u3000 is the double-byte space character in UTF-8
	// \u00A0 is the non-breaking space character (&nbsp;)
	// \u2007 is the figure space character (&#8199;)
	// \u202F is the narrow non-breaking space character (&#8239;)
	public static final String WHITE_SPACES = " \r\n\t\u3000\u00A0\u2007\u202F";

	private StringUtils()
	{
	}

	private static String[][] htmlEscape = { { "&lt;", "<" }, { "&gt;", ">" }, { "&amp;", "&" }, { "&quot;", "\"" },
			{ "&agrave;", "à" }, { "&Agrave;", "À" }, { "&acirc;", "â" }, { "&auml;", "ä" }, { "&Auml;", "Ä" },
			{ "&Acirc;", "Â" }, { "&aring;", "å" }, { "&Aring;", "Å" }, { "&aelig;", "æ" }, { "&AElig;", "Æ" },
			{ "&ccedil;", "ç" }, { "&Ccedil;", "Ç" }, { "&eacute;", "é" }, { "&Eacute;", "É" }, { "&egrave;", "è" },
			{ "&Egrave;", "È" }, { "&ecirc;", "ê" }, { "&Ecirc;", "Ê" }, { "&euml;", "ë" }, { "&Euml;", "Ë" },
			{ "&iuml;", "ï" }, { "&Iuml;", "Ï" }, { "&ocirc;", "ô" }, { "&Ocirc;", "Ô" }, { "&ouml;", "ö" },
			{ "&Ouml;", "Ö" }, { "&oslash;", "ø" }, { "&Oslash;", "Ø" }, { "&szlig;", "ß" }, { "&ugrave;", "ù" },
			{ "&Ugrave;", "Ù" }, { "&ucirc;", "û" }, { "&Ucirc;", "Û" }, { "&uuml;", "ü" }, { "&Uuml;", "Ü" },
			{ "&nbsp;", " " }, { "&copy;", "\u00a9" }, { "&reg;", "\u00ae" }, { "&euro;", "\u20a0" } };

	public static final String unescapeHTML(String s, int start)
	{
		int i, j, k;

		i = s.indexOf("&", start);
		start = i + 1;
		if (i > -1)
		{
			j = s.indexOf(";", i);
			/*
			 * we don't want to start from the beginning
			 * the next time, to handle the case of the &
			 * thanks to Pieter Hertogh for the bug fix!
			 */
			if (j > i)
			{
				// ok this is not most optimized way to
				// do it, a StringBuffer would be better,
				// this is left as an exercise to the reader!
				String temp = s.substring(i, j + 1);
				// search in htmlEscape[][] if temp is there
				k = 0;
				while (k < htmlEscape.length)
				{
					if (htmlEscape[k][0].equals(temp))
						break;
					else
						k++;
				}
				if (k < htmlEscape.length)
				{
					s = s.substring(0, i) + htmlEscape[k][1] + s.substring(j + 1);
					return unescapeHTML(s, i); // recursive call
				}
			}
		}
		return s;
	}

	public static String stripAndCollapse(String str)
	{
		return collapseWhitespace(strip(str));
	}

	public static String strip(String str)
	{
		return megastrip(str, true, true, WHITE_SPACES);
	}

	public static String megastrip(String str, boolean left, boolean right, String what)
	{
		if (str == null)
		{
			return null;
		}

		int limitLeft = 0;
		int limitRight = str.length() - 1;

		while (left && limitLeft <= limitRight && what.indexOf(str.charAt(limitLeft)) >= 0)
		{
			limitLeft++;
		}
		while (right && limitRight >= limitLeft && what.indexOf(str.charAt(limitRight)) >= 0)
		{
			limitRight--;
		}

		return str.substring(limitLeft, limitRight + 1);
	}

	public static String collapseWhitespace(String str)
	{
		return collapse(str, WHITE_SPACES, " ");
	}

	public static String collapse(String str, String chars, String replacement)
	{
		if (str == null)
		{
			return null;
		}
		StringBuilder newStr = new StringBuilder();

		boolean prevCharMatched = false;
		char c;
		for (int i = 0; i < str.length(); i++)
		{
			c = str.charAt(i);
			if (chars.indexOf(c) != -1)
			{
				// this character is matched
				if (prevCharMatched)
				{
					// apparently a string of matched chars, so don't append
					// anything
					// to the string
					continue;
				}
				prevCharMatched = true;
				newStr.append(replacement);
			}
			else
			{
				prevCharMatched = false;
				newStr.append(c);
			}
		}

		return newStr.toString();
	}

	public static String fixedWidth(String str, int width)
	{
		String[] lines = split(str, "\n");
		return fixedWidth(lines, width);
	}

	public static String[] splitAndTrim(String str, String delims)
	{
		return split(str, delims, true);
	}

	public static String[] split(String str, String delims)
	{
		return split(str, delims, false);
	}

	/**
	 * Split "str" into tokens by delimiters and optionally remove white spaces
	 * from the splitted tokens.
	 * 
	 * @param trimTokens
	 *            if true, then trim the tokens
	 */
	public static String[] split(String str, String delims, boolean trimTokens)
	{
		StringTokenizer tokenizer = new StringTokenizer(str, delims);
		int n = tokenizer.countTokens();
		String[] list = new String[n];
		for (int i = 0; i < n; i++)
		{
			if (trimTokens)
			{
				list[i] = tokenizer.nextToken().trim();
			}
			else
			{
				list[i] = tokenizer.nextToken();
			}
		}
		return list;
	}

	public static String fixedWidth(String[] lines, int width)
	{
		StringBuilder formatStr = new StringBuilder();

		for (int i = 0; i < lines.length; i++)
		{
			int curWidth = 0;
			if (i != 0)
			{
				formatStr.append("\n");
			}
			// a small optimization
			if (lines[i].length() <= width)
			{
				formatStr.append(lines[i]);
				continue;
			}
			String[] words = splitAndTrim(lines[i], WHITE_SPACES);
			for (int j = 0; j < words.length; j++)
			{
				if (curWidth == 0 || (curWidth + words[j].length()) < width)
				{
					// add a space if we're not at the beginning of a line
					if (curWidth != 0)
					{
						formatStr.append(" ");
						curWidth += 1;
					}
					curWidth += words[j].length();
					formatStr.append(words[j]);
				}
				else
				{
					formatStr.append("\n");
					curWidth = words[j].length();
					formatStr.append(words[j]);
				}
			}
		}

		return formatStr.toString();
	}

	/*
	 * public static void main(String args[]) throws Exception
	 * {
	 * // to see accented character to the console
	 * java.io.PrintStream ps = new java.io.PrintStream(System.out, true,
	 * "Cp850");
	 * String test = "&copy; 2000  R&eacute;al Gagnon &lt;www.rgagnon.com&gt;";
	 * ps.println(test + "\n-->\n" + unescapeHTML(test, 0));
	 * }
	 */
}
