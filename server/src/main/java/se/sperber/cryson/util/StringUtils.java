package se.sperber.cryson.util;

public class StringUtils {

  /**
   * Courtesy of http://stackoverflow.com/a/8512877 for efficient byte counting
   * @param sequence UTF8 CharSequence to count bytes of
   * @return count of utf8 bytes in sequence
   */
  public static int countUtf8Bytes(CharSequence sequence) {
    int count = 0;
    for (int i = 0, len = sequence.length(); i < len; i++) {
      char ch = sequence.charAt(i);
      if (ch <= 0x7F) {
        count++;
      } else if (ch <= 0x7FF) {
        count += 2;
      } else if (Character.isHighSurrogate(ch)) {
        count += 4;
        ++i;
      } else {
        count += 3;
      }
    }
    return count;
  }

}
