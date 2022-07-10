package com.example.httppreload.file;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileWatcher extends Thread {
  private static FileWatchListener NOOP_LISTENER = new FileWatchListener() {
  };

  private AtomicBoolean stopped = new AtomicBoolean(false);
  private Path dir;
  private FileWatchListener listener;
  private WatchService watchService;

  public FileWatcher(Path dir) {
    this.dir = dir;
    this.listener = NOOP_LISTENER;
  }

  @Override
  public void run() {
    try {
      WatchService watchService = FileSystems.getDefault().newWatchService();
      this.watchService = watchService;
      dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
      for (WatchKey key; (key = watchService.take()) != null && !stopped.get(); key.reset()) {
        for (WatchEvent<?> event : key.pollEvents()) {
          Path contextPath = (Path) event.context();
          if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            listener.onModify(contextPath);
          } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            listener.onDelete(contextPath);
          } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            listener.onCreate(contextPath);
          } else {
            // NOOP
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {

    } catch (ClosedWatchServiceException e) {

    }
  }

  public void stopWatch() {
    stopped.set(true);
    try {
      if (watchService != null) {
        watchService.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setListener(FileWatchListener listener) {
    this.listener = listener;
  }

}
