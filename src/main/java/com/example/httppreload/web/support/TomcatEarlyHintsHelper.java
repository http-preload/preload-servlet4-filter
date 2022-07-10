package com.example.httppreload.web.support;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.http11.Http11OutputBuffer;
import org.apache.coyote.http2.Http2OutputBuffer;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.net.SocketWrapperBase;

public class TomcatEarlyHintsHelper {
  private static Field responseField;
  private static Field outputBufferField;
  private static Field socketWrapperField;
  private static Field nextField;
  private static Field hookField;

  public static void init() {
    try {
      TomcatEarlyHintsHelper.responseField = ResponseFacade.class.getDeclaredField("response");
      responseField.trySetAccessible();
      TomcatEarlyHintsHelper.outputBufferField = org.apache.coyote.Response.class.getDeclaredField("outputBuffer");
      outputBufferField.trySetAccessible();
      TomcatEarlyHintsHelper.socketWrapperField = Http11OutputBuffer.class.getDeclaredField("socketWrapper");
      socketWrapperField.trySetAccessible();
      TomcatEarlyHintsHelper.nextField = Http2OutputBuffer.class.getDeclaredField("next");
      nextField.trySetAccessible();
      TomcatEarlyHintsHelper.hookField = org.apache.coyote.Response.class.getDeclaredField("hook");
      hookField.trySetAccessible();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
  }

  public static boolean sendEarlyHints(HttpServletResponse response, String name, String value) throws IOException {
    String className = "org.apache.catalina.connector.ResponseFacade";
    if (!response.getClass().getName().equals(className) || response.isCommitted()) {
      return false;
    }
    try {
      Response catalinaResponse = (Response) responseField.get(response);
      org.apache.coyote.Response coyoteResponse = catalinaResponse.getCoyoteResponse();
      OutputBuffer outputBuffer0 = (OutputBuffer) outputBufferField.get(coyoteResponse);
      Class<?> outputBufferClass = outputBuffer0.getClass();
      switch (outputBufferClass.getName()) {
      case "org.apache.coyote.http11.Http11OutputBuffer": {
        SocketWrapperBase<?> socketWrapper = (SocketWrapperBase<?>) socketWrapperField.get(outputBuffer0);
        StringBuilder sb = new StringBuilder(256);
        sb.append("HTTP/1.1 103 Early Hints\r\n");
        sb.append(name).append(": ").append(value).append("\r\n");
        sb.append("\r\n");
        byte[] buf = sb.toString().getBytes(StandardCharsets.US_ASCII);
        socketWrapper.write(coyoteResponse.getWriteListener() == null, buf, 0, buf.length);
        if (socketWrapper.flush(true)) {
          throw new IOException("Failed to send HTTP 103 Early Hints response");
        }
        break;
      }
      case "org.apache.coyote.http2.Http2OutputBuffer": {
        // FIXME need optimization
        /* StreamProcessor*/ Object hook = hookField.get(coyoteResponse);
        Field streamField = hook.getClass().getDeclaredField("stream");
        streamField.trySetAccessible();
        /* Stream */ Object stream = streamField.get(hook);
        Field handlerField = hook.getClass().getDeclaredField("handler");
        handlerField.trySetAccessible();
        /* Http2UpgradeHandler */ Object handler = handlerField.get(hook);
        MimeHeaders headers = new MimeHeaders();
        int frameSize=32;
        headers.addValue(":status").setString(Integer.toString(103));
        frameSize+=8+name.length()+value.length();
        if(frameSize>1024) {
          frameSize = 2048;
        }else if(frameSize>512) {
          frameSize = 1024;
        }else if(frameSize>256) {
          frameSize = 512;
        }else if(frameSize>128) {
          frameSize = 256;
        }
        headers.addValue(name).setString(value);
        // writeHeaders(Stream stream, int pushedStreamId, MimeHeaders mimeHeaders, boolean endOfStream, int payloadSize)
        Method writeHeaders = handler.getClass().getDeclaredMethod("writeHeaders", stream.getClass(), int.class,
            MimeHeaders.class, boolean.class, int.class);
        writeHeaders.trySetAccessible();
        writeHeaders.invoke(handler, stream, 0, headers, false, frameSize);
        break;
      }
      default:
        return false;
      }
      return true;
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    return false;
  }
}
