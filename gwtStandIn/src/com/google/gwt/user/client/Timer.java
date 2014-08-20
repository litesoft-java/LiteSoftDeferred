package com.google.gwt.user.client;

@SuppressWarnings("UnusedParameters")
public abstract class Timer {
  public void cancel() {
  }

  public abstract void run();

  public void schedule(int delayMillis) {
  }

  public void scheduleRepeating(int periodMillis) {
  }
}
