package org.bootcamp.cardservice.support;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Utils {
  private static final SecureRandom secureRandom = new SecureRandom();

  public static String generateId(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
  }

  public static String generateCardNumberHash() {
    return sha256(String.valueOf(4000000000000000L + Math.abs(secureRandom.nextLong(999999999999999L))));
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}
