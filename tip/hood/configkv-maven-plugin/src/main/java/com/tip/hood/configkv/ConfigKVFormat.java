package com.tip.hood.configkv;

import asia.redact.bracket.properties.OutputFormat;
import java.util.List;

/**
 * Format for configkv.properties file. Adapted from BasicOutputFormat.
 *
 * @author max
 */
public class ConfigKVFormat implements OutputFormat {

   private final static String EOL = System.getProperty("line.separator");
   private final String headerStart;

   public ConfigKVFormat(String headerStart) {
      this.headerStart = processHeader(headerStart);
   }

   public ConfigKVFormat() {
      this("");
   }

   private static String processHeader(String header) {
      //add comment symbol to input string
      if (header == null) {
         header = "";
      }
      String[] split = header.split("\n");
      StringBuilder b = new StringBuilder(EOL);
      for (String split1 : split) {
         b.append("#").append(split1).append(EOL);
      }
      return b.substring(0, b.length() - 1).toString();
   }

   @Override
   public String formatContentType() {
      StringBuilder b = new StringBuilder(headerStart);
      b.append(EOL);
//      b.append("#content=text/x-java-properties");
//      b.append(EOL);
//      b.append("#charset=UTF-8");
//      b.append(EOL);
      return b.toString();
   }

   @Override
   public String formatHeader() {
      StringBuilder b = new StringBuilder("#");
      b.append(new java.util.Date().toString());
      b.append(EOL);
      b.append(EOL);
      return b.toString();
   }

   @Override
   public String format(String key, char separator, List<String> values, List<String> comments) {

      if (key == null) {
         throw new RuntimeException("Key cannot be null in a format");
      }
      StringBuilder b = new StringBuilder();
      if (comments != null && comments.size() > 0) {
         for (String c : comments) {
            b.append(c);
            b.append(EOL);
         }
      }
      StringBuilder keyBuilder = new StringBuilder();
      for (int i = 0; i < key.length(); i++) {
         char ch = key.charAt(i);
         if (ch == ':' || ch == '=') {
            keyBuilder.append('\\');
         }
         keyBuilder.append(ch);
      }
      b.append(keyBuilder.toString());
      b.append(separator);

      if (values != null && values.size() > 0) {
         int count = values.size();
         int i = 0;
         for (String s : values) {
            b.append(s);
            if (i < count - 1) {
               b.append('\\');
            }
            b.append(EOL);
            i++;
         }
      }

      return b.toString();
   }

   @Override
   public String formatFooter() {
      StringBuilder b = new StringBuilder(EOL);
      b.append("# END of section ");
      b.append(EOL);
      return b.toString();
   }

}
