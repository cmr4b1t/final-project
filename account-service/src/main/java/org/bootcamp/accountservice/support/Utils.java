package org.bootcamp.accountservice.support;

import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Utils {

  public static String generateId(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
  }
}
