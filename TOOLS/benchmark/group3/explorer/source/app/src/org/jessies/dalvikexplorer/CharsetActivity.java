package org.jessies.dalvikexplorer;

import java.nio.charset.Charset;
import java.util.Set;

public class CharsetActivity extends TextViewActivity {
  protected String extraName() {
    return "org.jessies.dalvikexplorer.Charset";
  }

  protected CharSequence title(String charsetName) {
    return "Charset \"" + charsetName + "\"";
  }

  protected String content(String charsetName) {
    return describeCharset(charsetName);
  }

  static String describeCharset(String name) {
    final StringBuilder result = new StringBuilder();
    result.append("<html>");
    final Charset charset = Charset.forName(name);
    append(result, "Canonical Name", charset.name());
    if (!charset.displayName().equals(charset.name())) {
      append(result, "Display Name", charset.displayName());
    }

    result.append("<p>");
    append(result, "Can Encode", charset.canEncode());
    append(result, "IANA Registered", charset.isRegistered());

    Set<String> aliases = charset.aliases();
    if (aliases.size() > 0) {
      result.append("<p><b>Aliases</b>\n");
      result.append(Utils.sortedStringOfStrings("<br>&nbsp;&nbsp;", aliases));
    }
    return result.toString();
  }
}
