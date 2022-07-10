package com.example.httppreload.web.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.httppreload.entity.LinkHeader;
import com.example.httppreload.entity.PreloadManifest;
import com.example.httppreload.file.FileWatchListener;
import com.example.httppreload.file.FileWatcher;
import com.example.httppreload.web.support.TomcatEarlyHintsHelper;
import com.example.httppreload.web.utils.ClientHints;
import com.example.httppreload.web.utils.PathUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PreloadFilter implements Filter {
  private PreloadManifest manifest;
  private boolean prefersEarlyHints;
  private boolean watch;
  private Date lastModified = new Date();

  private FileWatcher watcher;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletContext application = filterConfig.getServletContext();
    String manifestFile = filterConfig.getInitParameter("manifestFile");
    if (manifestFile == null || manifestFile.length() == 0) {
      throw new ServletException("init-param manifestFile must be specified");
    }
    File file;
    if (manifestFile.startsWith("classpath:")) {
      file = new File(Thread.currentThread().getContextClassLoader()
          .getResource(manifestFile.substring("classpath:".length())).getFile());
    } else if (manifestFile.startsWith("/")) {
      file = new File(application.getRealPath(manifestFile));
    } else {
      throw new ServletException("init-param manifestFile should be a resource path starts with / or classpath:/");
    }
    if (!file.exists()) {
      throw new ServletException(
          new FileNotFoundException("Reousrce path \"" + manifestFile + "\" doesn't denote a file"));
    }
    PreloadFilter that = this;
    try {
      Class.forName("com.fasterxml.jackson.core.JsonStreamContext");
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
    }
    this.manifest = new PreloadManifest(); // in case of NllPointerException
    new Thread(() -> {
      try (InputStream input = new FileInputStream(file)) {
        ObjectMapper mapper = new ObjectMapper();
        that.manifest = mapper.readValue(input, PreloadManifest.class);
        manifest.normalize();
        if (manifest.hasAnyCondition()) {
          ClientHints.init();
        }
      } catch (IOException e) {
        System.err.println("Failed to load resourse " + manifestFile);
      }
    }).start();

    this.watch = "true".equals(filterConfig.getInitParameter("watch"));
    if (watch) {
      FileWatcher watcher = new FileWatcher(file.toPath().getParent());
      SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSXXX", Locale.UK);
      sdf.setTimeZone(TimeZone.getDefault());
      watcher.setListener(new FileWatchListener() {
        @Override
        public void onModify(Path contextPath) {
          if (!contextPath.toString().equals(file.getName())) {
            return;
          }
          Date dateModified = new Date();
          if (dateModified.getTime() - lastModified.getTime() < 1000) {
            return;
          }
          lastModified.setTime(dateModified.getTime());
          System.out.println("[" + sdf.format(dateModified) + "] Reloading resource " + manifestFile);
          try (InputStream input = new FileInputStream(file)) {
            ObjectMapper mapper = new ObjectMapper();
            that.manifest = mapper.readValue(input, PreloadManifest.class);
            manifest.normalize();
          } catch (IOException e) {
            System.err.println("Failed to load resource " + file.toString());
          }
        }
      });
      watcher.start();
      this.watcher = watcher;
    }
    this.prefersEarlyHints = "true".equals(filterConfig.getInitParameter("prefersEarlyHints"));
    if (prefersEarlyHints) {
      TomcatEarlyHintsHelper.init();
    }
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    String accept = request.getHeader("accept");
    if (accept != null && accept.startsWith("text/html") && request.getMethod().equals("GET")) {
      String reqPath = PathUtils.join(request.getServletContext().getContextPath(), request.getServletPath());
      List<LinkHeader> candidates = manifest.lookup(reqPath);
      setLinkHeader: if (candidates != null) {
        ClientHints clientHints = null;
        String headerValue = null;
        if (candidates.stream().anyMatch((c) -> c.getConditionId() != null)) {
          if (ClientHints.contextTL == null) { // necessary because of async initialization
            break setLinkHeader;
          }
          clientHints = new ClientHints(request);
          for (LinkHeader candidate : candidates) {
            String conditionId = candidate.getConditionId();
            if (conditionId == null || clientHints.checkCondition(candidate.getCondition())) {
              headerValue = headerValue == null ? candidate.getValue() : headerValue + ", " + candidate.getValue();
            }
          }
          if (headerValue == null) {
            break setLinkHeader;
          }
        } else {
          headerValue = candidates.get(0).getValue();
        }
        if (prefersEarlyHints) {
          if (clientHints == null) {
            if (ClientHints.contextTL == null) {
              break setLinkHeader;
            }
            clientHints = new ClientHints(request);
          }
          if (clientHints.checkCondition(manifest.supportsEarlyHintsCondition())) {
            try {
              TomcatEarlyHintsHelper.sendEarlyHints(response, "Link", headerValue);
              break setLinkHeader;
            } catch (IOException e) {
            }
          }
        }
        response.setHeader("Link", headerValue);
      }
    }
    chain.doFilter(req, resp);
  }

  @Override
  public void destroy() {
    ClientHints.destroy();
    if (watcher != null) {
      watcher.stopWatch();
    }
  }
}
