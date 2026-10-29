package com.example;

public class LineCountApp {
  public static void main(String[] args) {
    // recursive call: "hello world" -> " world" -> "d"
    print("hello world", 5);
    // the same call twice, so both calls map to one generated test
    print("abc", 10);
    print("abc", 10);
    // failing calls
    print(null, 5);
    print("abc", 0);
  }

  private static void print(String text, int width) {
    // catch so the app exits normally and the trace is written
    try {
      System.out.println(LineCounter.lineCount(text, width));
    } catch (RuntimeException e) {
      System.out.println(e);
    }
  }
}
