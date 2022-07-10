package com.example.httppreload.web.utils;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {
  public static StringBuilder getHttpMessage(HttpServletRequest request) {
    StringBuilder sb=new StringBuilder(1024);
    String COLON = ": ";
    String protocol = request.getProtocol();
    String query = request.getQueryString();
    if(protocol.startsWith("HTTP/1")){
      String CRLF="\r\n";
      sb.append(request.getMethod()).append(" ").append(request.getRequestURI())
        .append(query==null?"":"?"+query).append(" ").append(protocol).append(CRLF);
      for(Enumeration<String> e=request.getHeaderNames(); e.hasMoreElements(); ) {
        String name = e.nextElement();
        String value = null;
        for(Enumeration<String> e2= request.getHeaders(name); e2.hasMoreElements(); ) {
          if(value == null) {
            value = e2.nextElement();
          }else if(name.equals("cookie")) {
            value = value+"; "+e2.nextElement();
          }else {
            value = value+", "+e2.nextElement();
          }
        }
        sb.append(name).append(COLON).append(value).append(CRLF);
      }
      sb.append(CRLF);
    }else{
      String EOL="\n";
      sb.append(":authority").append(COLON).append(request.getServerName()+":"+request.getServerPort()).append(EOL);
      sb.append(":method").append(COLON).append(request.getMethod()).append(EOL);
      sb.append(":path").append(COLON).append(request.getRequestURI()+(query==null?"":"?"+query)).append(EOL);
      sb.append(":scheme").append(COLON).append(request.getScheme()).append(EOL);
      for(Enumeration<String> e=request.getHeaderNames(); e.hasMoreElements(); ) {
        String name = e.nextElement();
        String value = null;
        for(Enumeration<String> e2= request.getHeaders(name); e2.hasMoreElements(); ) {
          if(value == null) {
            value = e2.nextElement();
          }else if(name.equals("cookie")) {
            value = value+"; "+e2.nextElement();
          }else {
            value = value+", "+e2.nextElement();
          }
        }
        sb.append(name).append(COLON).append(value).append(EOL);
      }
      sb.append(EOL);
    }
    return sb;
  }
  public static Map<String, String> getRequestHeaders(HttpServletRequest request) {
    Map<String, String> headers=new LinkedHashMap<String, String>(20);
    String query = request.getQueryString();
    headers.put(":authority", request.getServerName()+":"+request.getServerPort());
    headers.put(":method", request.getMethod());
    headers.put(":path", request.getRequestURI()+(query==null?"":"?"+query));
    headers.put(":scheme", request.getScheme());
    for(Enumeration<String> e=request.getHeaderNames(); e.hasMoreElements(); ) {
      String name = e.nextElement();
      String value = null;
      for(Enumeration<String> e2= request.getHeaders(name); e2.hasMoreElements(); ) {
        if (value == null) {
          value = e2.nextElement();
        } else if(name.equals("cookie")) {
          value = value + "; " + e2.nextElement();
        } else {
          value = value + ", " + e2.nextElement();
        }
      }
      headers.put(name, value);
    }
    return headers;
  }

}
