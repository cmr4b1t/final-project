package org.bootcamp.customerservice.domain.supports;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {
  public static String generateId(String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "");
  }
}
