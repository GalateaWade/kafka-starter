package org.galatea.kafka.starter.testing.avro.fieldtypes;

import org.galatea.kafka.starter.testing.avro.AvroFieldType;

public class AvroStringType implements AvroFieldType {

  @Override
  public Object defaultValue(String... params) {
    return "";
  }
}
