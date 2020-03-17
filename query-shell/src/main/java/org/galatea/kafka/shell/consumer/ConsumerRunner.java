package org.galatea.kafka.shell.consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.galatea.kafka.shell.domain.ConsumerProperties;
import org.galatea.kafka.shell.domain.TopicPartitionOffsets;
import org.galatea.kafka.shell.stores.OffsetTrackingRecordStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerRunner implements Runnable {

  @Getter
  private final ConsumerProperties properties = new ConsumerProperties();
  private final Consumer<GenericRecord, GenericRecord> consumer;
  private static final Duration POLL_MAX_DURATION = Duration.ofSeconds(1);
  private final Set<OffsetTrackingRecordStore> stores = new HashSet<>();

  @Override
  public void run() {
    log.info("Started Thread");

    while (true) {
      if (properties.isAssignmentUpdated()) {
        log.info("Updating consumer assignment {}", properties.getAssignment());
        updateConsumerAssignment(consumer, properties);
        stores.clear();
        stores.addAll(properties.getStoreSubscription().values().stream()
            .flatMap(Collection::stream).collect(Collectors.toSet()));
        properties.setAssignmentUpdated(false);
      }
      if (properties.getAssignment().isEmpty()) {
        trySleep(1000);
        continue;
      }

      consumer.poll(POLL_MAX_DURATION).forEach(record -> {
        subscribedStores(record.topic()).forEach(store -> store.addRecord(record));
        updateStatistics(record);
      });

      Semaphore offsetRequestSemaphore = properties.getOffsetsRequested();
      if (offsetRequestSemaphore.availablePermits() == 0) {
        offsetRequestSemaphore.release(); // tell the main thread that the request has been received
        Map<TopicPartition, Long> endOffsets = consumer
            .endOffsets(properties.getAssignment());
        Map<TopicPartition, Long> beginningOffsets = consumer
            .beginningOffsets(properties.getAssignment());
        endOffsets.forEach(((topicPartition, endOffset) -> {
          Long beginningOffset = beginningOffsets.get(topicPartition);
          Map<TopicPartition, TopicPartitionOffsets> offsets = new HashMap<>();
          offsets.put(topicPartition, new TopicPartitionOffsets(beginningOffset, endOffset));
          properties.setOffsetsMap(offsets);
          offsetRequestSemaphore.release(); // tell main thread the request is fulfilled
        }));
      }


    }
  }

  private void updateStatistics(ConsumerRecord<GenericRecord, GenericRecord> record) {
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    properties.getConsumedMessages()
        .compute(topicPartition, (key, aLong) -> aLong != null ? aLong + 1 : 1);
    properties.getLatestOffset().put(topicPartition, record.offset());
  }

  private Set<OffsetTrackingRecordStore> subscribedStores(String topic) {
    return properties.getStoreSubscription().computeIfAbsent(topic, s -> new HashSet<>());
  }

  private static void updateConsumerAssignment(Consumer<GenericRecord, GenericRecord> consumer,
      ConsumerProperties properties) {

    consumer.assign(properties.getAssignment());
    log.info("Updated consumer assignment: {}", properties);

    consumer.seekToBeginning(properties.getSeekBeginningAssignment());
    log.info("Seek to beginning for {}", properties.getSeekBeginningAssignment());
    properties.getSeekBeginningAssignment().clear();
  }

  private static void trySleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      log.warn("Sleep interrupted: ", e);
      throw new IllegalStateException(e);
    }
  }
}
