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

/** Copyright (c) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zoffcc.applications.aagtl;

import java.util.regex.Pattern;

/**
 * Convert provided html formatted string to text format.
 * 
 * 
 */
public final class HtmlToText
{

	/**
	 * Regular expression to match html line breaks or paragraph tags
	 * and adjacent whitespace
	 */
	private static final Pattern htmlNewlinePattern = Pattern.compile("\\s*<(br|/?p)>\\s*");

	/** Regular expression to match list tags and adjacent whitespace */
	private static final Pattern htmlListPattern = Pattern.compile("\\s*<li>\\s*");

	/** Regular expression to match any remaining html tags */
	private static final Pattern htmlTagPattern = Pattern.compile("</?([^<]*)>");

	/** Maximum length of a line in email body (in characters) */
	public static final int EMAIL_LINE_WIDTH_MAX = 72;

	// This class should not be instantiated, hence the private constructor
	private HtmlToText()
	{
	}

	/**
	 * Convert provided html string to plain text preserving the formatting
	 * as much as possible. Ensure line wrapping to 72 chars as default.
	 * NOTE: add support for more HTML tags here.
	 * For the present, convert <br>
	 * to '\n'
	 * convert
	 * <p>
	 * and
	 * </p>
	 * to '\n'
	 * convert <li>to "\n- "
	 * 
	 * @throws NullPointerException
	 */
	public static String htmlToPlainText(String html)
	{

		if (html == null)
		{
			throw new NullPointerException("Html parameter may not be null.");
		}

		// Clear any html indentation and incidental whitespace
		String text = StringUtils.stripAndCollapse(html);

		/*
		 * Replace <br> and <p> tags with new line characters.
		 * Replace <li> tags (HTML bullets) with dashes.
		 * Remove any remaining HTML tags not supported yet.
		 * Finally replace any HTML escape string with appropriate character
		 */
		text = htmlNewlinePattern.matcher(text).replaceAll("\n");
		text = htmlListPattern.matcher(text).replaceAll("\n- ");
		text = htmlTagPattern.matcher(text).replaceAll("");
		text = StringUtils.unescapeHTML(text, 0).trim();

		/*
		 * Ensure no line of plain text is longer than default (72 chars)
		 * NOTE: Use String.split, NOT StringUtil.split, in order to preserve
		 * consecutive newline characters originating from <br> and <p> tags
		 */
		return StringUtils.fixedWidth(text.split("\n"), EMAIL_LINE_WIDTH_MAX);
	}
}
