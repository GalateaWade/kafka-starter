package org.galatea.kafka.shell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class ShellApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShellApplication.class, args);
  }

//  private final ConsumerThreadController consumerThreadController;
//  private final RecordStoreController recordStoreController;

//  @Override
//  public void run(String... args) throws Exception {
//    String topic = "ad.episode.store";
//    OffsetTrackingRecordStore episodeStore = recordStoreController
//        .newStore(topic, false);
//    consumerThreadController.addStoreAssignment(topic, episodeStore);
//    consumerThreadController.addTopicToAssignment(topic);
//
//
//  }
}
