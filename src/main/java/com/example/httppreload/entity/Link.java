package com.example.httppreload.entity;

import java.util.LinkedHashMap;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class Link extends LinkedHashMap<String, String> {
  private static final long serialVersionUID = -3318116717517120162L;
  private static Pattern SIMPLE_TOKEN = Pattern.compile("^[\\w-]+$");

  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner(";");
    sj.add("<" + this.get("href") + ">");
    for (String k : this.keySet()) {
      if (k.equals("href"))
        continue;
      String v = this.get(k);
      if (SIMPLE_TOKEN.matcher(v).matches()) {
        sj.add(k + "=" + v);
      } else {
        sj.add(k + "=\"" + v + "\"");
      }
    }
    return sj.toString();
  }
}
