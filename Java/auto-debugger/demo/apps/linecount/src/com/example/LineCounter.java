package com.example;

public class LineCounter {
  /** Number of lines needed to show text when each line holds at most width characters. */
  public static int lineCount(String text, int width) {
    // Bugs: a null text throws NullPointerException, width 0 throws ArithmeticException ("/ by zero").
    if (text.length() / width == 0) {
      return text.isEmpty() ? 0 : 1;
    }
    return 1 + lineCount(text.substring(width), width);
  }
}
