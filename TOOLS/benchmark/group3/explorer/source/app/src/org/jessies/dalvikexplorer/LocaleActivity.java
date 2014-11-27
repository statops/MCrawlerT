package org.jessies.dalvikexplorer;

import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;

public class LocaleActivity extends TextViewActivity {
  protected String extraName() {
    return "org.jessies.dalvikexplorer.Locale";
  }

  protected CharSequence title(String localeName) {
    return "Locale \"" + localeName + "\"";
  }

  protected String content(String localeName) {
    return describeLocale(localeName);
  }

  private static Locale localeByName(String name) {
    if (name.length() == 0) {
      return new Locale("", "", "");
    }

    int languageEnd = name.indexOf('_');
    if (languageEnd == -1) {
      return new Locale(name, "", "");
    }

    String language = name.substring(0, languageEnd);
    name = name.substring(languageEnd + 1);

    int countryEnd = name.indexOf('_');
    if (countryEnd == -1) {
      return new Locale(language, name, "");
    }

    String country = name.substring(0, countryEnd);
    String variant = name.substring(countryEnd + 1);

    return new Locale(language, country, variant);
  }

  static String describeLocale(String name) {
    final StringBuilder result = new StringBuilder();
    result.append("<html>");

    final Locale locale = localeByName(name);

    result.append("<p>");
    append(result, "Display Name", locale.getDisplayName());
    append(result, "Localized Display Name", locale.getDisplayName(locale));

    if (locale.getLanguage().length() > 0) {
      String iso3Language = "(not available)";
      try {
        iso3Language = locale.getISO3Language();
      } catch (MissingResourceException ignored) {
      }

      result.append("<p>");
      append(result, "Display Language", locale.getDisplayLanguage());
      append(result, "Localized Display Language", locale.getDisplayLanguage(locale));
      append(result, "2-Letter Language Code", locale.getLanguage());
      append(result, "3-Letter Language Code", iso3Language);
    }
    if (locale.getCountry().length() > 0) {
      String iso3Country = "(not available)";
      try {
        iso3Country = locale.getISO3Country();
      } catch (MissingResourceException ignored) {
      }

      result.append("<p>");
      append(result, "Display Country", locale.getDisplayCountry());
      append(result, "Localized Display Country", locale.getDisplayCountry(locale));
      append(result, "2-Letter Country Code", locale.getCountry());
      append(result, "3-Letter Country Code", iso3Country);
    }
    if (locale.getVariant().length() > 0) {
      result.append("<p>");
      append(result, "Display Variant", locale.getDisplayVariant());
      append(result, "Localized Display Variant", locale.getDisplayVariant(locale));
      append(result, "Variant Code", locale.getVariant());
    }

    result.append("<p><b>Number Formatting</b>");
    describeNumberFormat(result, "Decimal", NumberFormat.getInstance(locale), 1234.5, -1234.5);
    describeNumberFormat(result, "Integer", NumberFormat.getIntegerInstance(locale), 1234, -1234);
    describeNumberFormat(result, "Currency", NumberFormat.getCurrencyInstance(locale), 1234.5, -1234.5);
    describeNumberFormat(result, "Percent", NumberFormat.getPercentInstance(locale), 12.3);

    boolean hasLocaleData = hasLocaleData();

    if (!hasLocaleData) {
      result.append("<p><b>Decimal Format Symbols</b>");
      NumberFormat nf = NumberFormat.getInstance(locale);
      if (nf instanceof DecimalFormat) {
        describeDecimalFormatSymbols(result, ((DecimalFormat) nf).getDecimalFormatSymbols());
      } else {
        result.append("(Didn't expect " + nf.getClass() + ".)");
      }
    }

    Date now = new Date(); // FIXME: it might be more useful to always show a time in the afternoon, to make 24-hour patterns more obvious.
    result.append("<p><b>Date/Time Formatting</b>");
    describeDateFormat(result, "Full Date", DateFormat.getDateInstance(DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Date", DateFormat.getDateInstance(DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Date", DateFormat.getDateInstance(DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Date", DateFormat.getDateInstance(DateFormat.SHORT, locale), now);
    result.append("<p>");
    describeDateFormat(result, "Full Time", DateFormat.getTimeInstance(DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Time", DateFormat.getTimeInstance(DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Time", DateFormat.getTimeInstance(DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Time", DateFormat.getTimeInstance(DateFormat.SHORT, locale), now);
    result.append("<p>");
    describeDateFormat(result, "Full Date/Time", DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Date/Time", DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Date/Time", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Date/Time", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale), now);

    if (!hasLocaleData) {
      result.append("<p><b>Date Format Symbols</b><p>");
      DateFormat edf = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
      DateFormatSymbols edfs = ((SimpleDateFormat) edf).getDateFormatSymbols();
      DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, locale);
      DateFormatSymbols dfs = ((SimpleDateFormat) df).getDateFormatSymbols();
      append(result, "Local Pattern Chars", dfs.getLocalPatternChars());
      append(result, "Am/pm", Arrays.toString(dfs.getAmPmStrings()));
      append(result, "Eras", Arrays.toString(dfs.getEras()));
      append(result, "Months", Arrays.toString(dfs.getMonths()));
      append(result, "Short Months", Arrays.toString(dfs.getShortMonths()));
      append(result, "Weekdays", Arrays.toString(dfs.getWeekdays()));
      append(result, "Short Weekdays", Arrays.toString(dfs.getShortWeekdays()));
    }

    result.append("<p><b>Calendar</b><p>");
    Calendar c = Calendar.getInstance(locale);
    int firstDayOfWeek = c.getFirstDayOfWeek();
    String firstDayOfWeekString = new DateFormatSymbols(locale).getWeekdays()[firstDayOfWeek];
    String englishFirstDayOfWeekString = new DateFormatSymbols(Locale.US).getWeekdays()[firstDayOfWeek];
    String firstDayOfWeekDetails = firstDayOfWeek + " '" + firstDayOfWeekString + "'";
    if (!englishFirstDayOfWeekString.equals(firstDayOfWeekString)) {
      firstDayOfWeekDetails += " (" + englishFirstDayOfWeekString + ")";
    }
    append(result, "First Day of the Week", firstDayOfWeekDetails);
    append(result, "Minimal Days in First Week", c.getMinimalDaysInFirstWeek());

    // If this locale specifies a country, check out the currency.
    // Languages don't have currencies; countries do.
    if (!locale.getCountry().equals("")) {
      result.append("<p><b>Currency</b><p>");
      try {
        Currency currency = Currency.getInstance(locale);
        append(result, "ISO 4217 Currency Code", currency.getCurrencyCode());
        append(result, "Currency Symbol", unicodeString(currency.getSymbol(locale)) + " (" + currency.getSymbol(Locale.US) + ")");
        append(result, "Default Fraction Digits", currency.getDefaultFractionDigits());
      } catch (IllegalArgumentException ex) {
        result.append("<p>(This version of Android is unable to return a Currency for this Locale.)");
      }
    }

    result.append("<p><b>Data Availability</b><p>");
    appendAvailability(result, locale, "BreakIterator", BreakIterator.class);
    appendAvailability(result, locale, "Calendar", NumberFormat.class);
    appendAvailability(result, locale, "Collator", Collator.class);
    appendAvailability(result, locale, "DateFormat", DateFormat.class);
    appendAvailability(result, locale, "DateFormatSymbols", DateFormatSymbols.class);
    appendAvailability(result, locale, "DecimalFormatSymbols", DecimalFormatSymbols.class);
    appendAvailability(result, locale, "NumberFormat", NumberFormat.class);

    if (hasLocaleData) {
      result.append("<p><b>libcore.icu.LocaleData</b>");
      try {
        Object enUsData = getLocaleDataInstance(Locale.US);
        Object localeData = getLocaleDataInstance(locale);
        String[] previous;

        result.append("<p>");
        describeStringArray(result, "amPm", enUsData, localeData, null);
        describeStringArray(result, "eras", enUsData, localeData, null);

        result.append("<p>");
        previous = describeStringArray(result, "longMonthNames", enUsData, localeData, null);
        describeStringArray(result, "longStandAloneMonthNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "shortMonthNames", enUsData, localeData, null);
        describeStringArray(result, "shortStandAloneMonthNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "tinyMonthNames", enUsData, localeData, null);
        describeStringArray(result, "tinyStandAloneMonthNames", enUsData, localeData, previous);

        result.append("<p>");
        previous = describeStringArray(result, "longWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "longStandAloneWeekdayNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "shortWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "shortStandAloneWeekdayNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "tinyWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "tinyStandAloneWeekdayNames", enUsData, localeData, previous);

        result.append("<p>");
        describeString(result, "yesterday", enUsData, localeData);
        describeString(result, "today", enUsData, localeData);
        describeString(result, "tomorrow", enUsData, localeData);

        result.append("<p>");
        describeString(result, "timeFormat12", enUsData, localeData);
        describeString(result, "timeFormat24", enUsData, localeData);

        result.append("<p>");
        describeChar(result, "zeroDigit", enUsData, localeData);
        describeChar(result, "decimalSeparator", enUsData, localeData);
        describeChar(result, "groupingSeparator", enUsData, localeData);
        describeChar(result, "patternSeparator", enUsData, localeData);
        describeChar(result, "percent", enUsData, localeData);
        describeChar(result, "perMill", enUsData, localeData);
        describeChar(result, "monetarySeparator", enUsData, localeData);
        describeChar(result, "minusSign", enUsData, localeData);
        describeString(result, "exponentSeparator", enUsData, localeData);
        describeString(result, "infinity", enUsData, localeData);
        describeString(result, "NaN", enUsData, localeData);

      } catch (Exception ex) {
        result.append("(" + ex.getClass().getSimpleName() + " thrown: " + ex.getMessage() + ")");
        System.err.println(ex);
      }
    }

    return result.toString();
  }

  private static String[] describeStringArray(StringBuilder sb, String fieldName, Object enUsData, Object localeData, String[] previous) {
    try {
      String[] values = (String[]) localeData.getClass().getField(fieldName).get(localeData);

      if (Arrays.equals(values, previous)) {
        return values;
      }

      sb.append("<p><b>").append(fieldName).append("</b>\n");
      String[] enUsValues = (String[]) localeData.getClass().getField(fieldName).get(enUsData);
      for (int i = 0; i < values.length; ++i) {
        if (enUsValues[i].length() == 0) {
          continue; // Java's weekday names array has an empty first element.
        }
        sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(values[i]);
        if (!values[i].equals(enUsValues[i])) {
          sb.append("  (" + enUsValues[i] + ")");
        }
      }
      return values;
    } catch (Exception ignored) {
      sb.append("<p><b>").append(fieldName).append("</b>: <font color='red'>missing</font>");
      return null;
    }
  }

  private static void describeString(StringBuilder sb, String fieldName, Object enUsData, Object localeData) {
    String valueText = "<font color='red'>missing</font>";
    try {
      valueText = unicodeString((String) localeData.getClass().getField(fieldName).get(localeData));
    } catch (Exception ignored) {
    }
    append(sb, fieldName, valueText);
  }

  private static void describeChar(StringBuilder sb, String fieldName, Object enUsData, Object localeData) {
    String valueText = "<font color='red'>missing</font>";
    try {
      valueText = unicodeString(localeData.getClass().getField(fieldName).getChar(localeData));
    } catch (Exception ignored) {
    }
    append(sb, fieldName, valueText);
  }

  private static Object getLocaleDataInstance(Locale locale) {
    try {
      return localeDataClass().getMethod("get", Locale.class).invoke(null, locale);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean hasLocaleData() {
    return localeDataClass() != null;
  }

  private static Class<?> localeDataClass() {
    try {
      return Class.forName("libcore.icu.LocaleData");
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String unicodeString(char ch) {
    return unicodeString(Character.toString(ch));
  }

  private static String unicodeString(String s) {
    if (s.length() > 1) {
      // For actual text (like the Arabic NaN), this isn't obviously useful.
      return s;
    }
    if (!containsNonAscii(s)) {
      // If there are only ASCII characters, don't belabor the point.
      return s;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(s);
    sb.append("  (");
    for (int i = 0; i < s.length(); ++i) {
      sb.append(String.format("U+%04x", (int) s.charAt(i)));
    }
    sb.append(")");
    return sb.toString();
  }

  private static boolean containsNonAscii(String s) {
    for (int i = 0; i < s.length(); ++i) {
      if (s.charAt(i) > 0x7f) {
        return true;
      }
    }
    return false;
  }

  private static void appendAvailability(StringBuilder result, Locale locale, String name, Class<?> c) {
    String state = "unknown";
    try {
      Locale[] locales = (Locale[]) c.getMethod("getAvailableLocales").invoke(null);
      state = Arrays.asList(locales).contains(locale) ? "present" : "missing";
    } catch (Exception ex) {
      // DateFormatSymbols.getAvailableLocales and DecimalFormatSymbols.getAvailableLocales weren't added until Java 6.
    }
    append(result, name, state);
  }

  private static void describeDecimalFormatSymbols(StringBuilder result, DecimalFormatSymbols dfs) {
    result.append("Currency Symbol: " + unicodeString(dfs.getCurrencySymbol()) + "\n");
    result.append("International Currency Symbol: " + unicodeString(dfs.getInternationalCurrencySymbol()) + "\n");
    result.append("<p>");

    result.append("Digit: " + unicodeString(dfs.getDigit()) + "\n");
    result.append("Pattern Separator: " + unicodeString(dfs.getPatternSeparator()) + "\n");
    result.append("<p>");

    result.append("Decimal Separator: " + unicodeString(dfs.getDecimalSeparator()) + "\n");
    result.append("Monetary Decimal Separator: " + unicodeString(dfs.getMonetaryDecimalSeparator()) + "\n");
    // 1.6: result.append("Exponent Separator: " + dfs.getExponentSeparator() + "\n");
    result.append("Grouping Separator: " + unicodeString(dfs.getGroupingSeparator()) + "\n");

    result.append("Infinity: " + unicodeString(dfs.getInfinity()) + "\n");
    result.append("Minus Sign: " + unicodeString(dfs.getMinusSign()) + "\n");
    result.append("NaN: " + unicodeString(dfs.getNaN()) + "\n");
    result.append("Percent: " + unicodeString(dfs.getPercent()) + "\n");
    result.append("Per Mille: " + unicodeString(dfs.getPerMill()) + "\n");
    result.append("Zero Digit: " + unicodeString(dfs.getZeroDigit()) + "\n");
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i <= 9; ++i) {
      digits.append((char) (dfs.getZeroDigit() + i));
    }
    result.append("Digits: " + digits.toString() + "\n");
    result.append("<p>");
    result.append("<p>");
  }

  private static void describeDateFormat(StringBuilder result, String description, DateFormat dateFormat, Date when) {
    if (dateFormat instanceof SimpleDateFormat) {
      SimpleDateFormat sdf = (SimpleDateFormat) dateFormat;
      result.append("<p><b>" + description + "</b>");
      result.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + sdf.toPattern());
      result.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + sdf.format(when));
    }
  }

  private static void describeNumberFormat(StringBuilder result, String description, NumberFormat numberFormat, Number... values) {
    if (numberFormat instanceof DecimalFormat) {
      DecimalFormat df = (DecimalFormat) numberFormat;
      result.append("<p><b>" + description + "</b>");
      result.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + df.toPattern());
      for (Number value : values) {
        result.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + df.format(value));
      }
    }
  }
}
