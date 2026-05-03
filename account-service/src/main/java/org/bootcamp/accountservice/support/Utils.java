package org.bootcamp.accountservice.support;

import java.util.UUID;

public final class Utils {
  private Utils() {
  }

  public static String generateId(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }
}
