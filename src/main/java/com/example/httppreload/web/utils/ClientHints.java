package com.example.httppreload.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletRequest;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.example.httppreload.web.filter.PreloadFilter;

public class ClientHints {
  private static Engine scriptEngine;
  private static Source scriptSource;
  private static Map<Thread, Context> weakMap;
  public static ThreadLocal<Context> contextTL;

  public static void init() {
    if (contextTL != null) {
      return;
    }
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    scriptEngine = Engine.newBuilder().build();
    try (InputStream input = PreloadFilter.class
        .getResourceAsStream("/com/example/httppreload/web/utils/ClientHints.js")) {
      String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      scriptSource = Source.create("js", "(" + content + ")");
    } catch (IOException e) {
      e.printStackTrace();
    }
    try (Context context = Context.newBuilder("js").engine(scriptEngine).build();) {
      context.eval(scriptSource);
    } catch (PolyglotException e) {
      e.printStackTrace();
    }
    weakMap = new WeakHashMap<Thread, Context>();
    contextTL = new ThreadLocal<Context>() {
      @Override
      protected Context initialValue() {
        Context context = Context.newBuilder("js").engine(scriptEngine).build();
        weakMap.put(Thread.currentThread(), context);
        return context;
      }
    };
  }

  public static void destroy() {
    if (weakMap != null) {
      weakMap.forEach((Thread t, Context context) -> {
        context.close();
      });
    }
  }

  private Context context;
  private transient Map<String, String> headers;
  private transient Value userAgentData;

  public ClientHints(HttpServletRequest request) {
    this.context = contextTL.get();
    this.headers = ServletUtils.getRequestHeaders(request);
    String uaBrands = request.getHeader("sec-ch-ua");
    String uaMobile = null;
    String uaPlatform = null;
    String userAgent = null;
    if (uaBrands != null) {
      uaMobile = request.getHeader("sec-ch-ua-mobile");
      uaPlatform = request.getHeader("sec-ch-ua-platform");
    } else {
      userAgent = request.getHeader("user-agent");
    }
    Value getUserAgentData = context.eval(scriptSource);
    this.userAgentData = getUserAgentData.execute(userAgent, uaBrands, uaMobile, uaPlatform);
  }

  public boolean checkCondition(Source condition) {
    return context.eval(condition).execute(userAgentData, headers).asBoolean();
  }

}
