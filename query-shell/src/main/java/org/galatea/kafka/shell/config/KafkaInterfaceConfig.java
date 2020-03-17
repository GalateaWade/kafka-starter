package org.galatea.kafka.shell.config;

import java.util.Properties;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.galatea.kafka.starter.messaging.AvroSerdeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaInterfaceConfig {

  @Bean
  public Consumer<GenericRecord, GenericRecord> consumer(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      @Value("${messaging.bootstrap-server}") String bootstrapServer) {
    Properties props = new Properties();
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);

    return new KafkaConsumer<>(props,
        AvroSerdeUtil.genericKeySerde(schemaRegistryUrl).deserializer(),
        AvroSerdeUtil.genericValueSerde(schemaRegistryUrl).deserializer());
  }

  @Bean
  public AdminClient adminClient(@Value("${messaging.bootstrap-server}") String bootstrapServer) {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    return KafkaAdminClient.create(props);
  }

}
