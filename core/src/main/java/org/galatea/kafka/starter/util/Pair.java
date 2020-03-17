package org.galatea.kafka.starter.util;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@Value
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class Pair<K, V> {

  private K key;
  private V value;

  public static <K, V> Pair<K, V> of(K key, V value) {
    return new Pair<>(key, value);
  }
}
