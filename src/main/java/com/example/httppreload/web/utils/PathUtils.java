package com.example.httppreload.web.utils;

public class PathUtils {
  public static String join(String seg, String... args) {
    String str = seg;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg == null || arg.length() == 0) {
        continue;
      }
      if (str.endsWith("/")) {
        str += arg.startsWith("/") ? arg.substring(1) : arg;
      } else {
        str += arg.startsWith("/") ? arg : "/" + arg;
      }
    }
    return str;
  }
}