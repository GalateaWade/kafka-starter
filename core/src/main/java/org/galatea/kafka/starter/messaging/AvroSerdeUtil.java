package org.galatea.kafka.starter.messaging;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;

public class AvroSerdeUtil {

  public static <T extends SpecificRecord> Serde<T> specificKeySerde(String schemaRegistryUrl) {
    Serde<T> serde = new SpecificAvroSerde<>();
    serde.configure(schemaRegistryConfig(schemaRegistryUrl), true);
    return serde;
  }

  public static <T extends SpecificRecord> Serde<T> specificValueSerde(String schemaRegistryUrl) {
    Serde<T> serde = new SpecificAvroSerde<>();
    serde.configure(schemaRegistryConfig(schemaRegistryUrl), false);
    return serde;
  }

  public static Serde<GenericRecord> genericKeySerde(String schemaRegistryUrl) {
    Serde<GenericRecord> serde = new GenericAvroSerde();
    serde.configure(schemaRegistryConfig(schemaRegistryUrl), true);
    return serde;
  }

  public static Serde<GenericRecord> genericValueSerde(String schemaRegistryUrl) {
    Serde<GenericRecord> serde = new GenericAvroSerde();
    serde.configure(schemaRegistryConfig(schemaRegistryUrl), false);
    return serde;
  }

  private static Map<String, ?> schemaRegistryConfig(String schemaRegistryUrl) {
    return Collections
        .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
  }
}
