package com.example.httppreload.file;

import java.nio.file.Path;

public interface FileWatchListener {
  public default void onModify(Path contextPath) {
  }

  public default void onCreate(Path contextPath) {
  }

  public default void onDelete(Path contextPath) {
  }
}
