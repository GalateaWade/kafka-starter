package org.galatea.kafka.shell.controller;

import com.apple.foundationdb.tuple.Tuple;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.galatea.kafka.shell.consumer.ConsumerThreadController;
import org.galatea.kafka.shell.stores.OffsetTrackingRecordStore;
import org.rocksdb.RocksIterator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class ShellController {

  private final Map<String, String> storeAlias = new HashMap<>();
  private final RecordStoreController recordStoreController;
  private final ConsumerThreadController consumerThreadController;
  private final StatusController statusController;

  // TODO: live-updating status
  @ShellMethod("Get status of the service")
  public String status() throws InterruptedException {
    return statusController.printableStatus();
  }

  @ShellMethod("Search a store using REGEX")
  public String query(
      @ShellOption String storeName,
      @ShellOption String regex) {

    StringBuilder ob = new StringBuilder();
    if (!recordStoreController.storeExist(storeName) && !storeAlias.containsKey(storeName)) {
      return ob.append("Store or alias ").append(storeName).append(" does not exist").toString();
    } else if (!recordStoreController.storeExist(storeName)) {
      storeName = storeAlias.get(storeName);
    }

    OffsetTrackingRecordStore episodeStore = recordStoreController.getStores().get(storeName);

    RocksIterator iterator = episodeStore.getUnderlyingDb().newIterator();
    iterator.seekToFirst();
    Pattern p = Pattern.compile(regex);
    ob.append("Results for regex '").append(regex).append("':\n");
    Instant startTime = Instant.now();
    long numResults = 0;
    while (iterator.isValid()) {
      if (p.matcher((CharSequence) Tuple.fromBytes(iterator.value()).get(3)).find()) {
        Tuple valueTuple = Tuple.fromBytes(iterator.value());
        ob.append(Instant.ofEpochMilli(valueTuple.getLong(2)))
            .append(": ").append(valueTuple.get(3)).append("\n");
        numResults++;
      }
      iterator.next();
    }
    ob.append("\n").append(numResults).append(" Results found in ")
        .append(readableTimeSince(startTime)).append("\n");
    iterator.close();

    // invoke service
    return ob.toString();
  }

  private String readableTimeSince(Instant startTime) {
    long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
    if (duration > 1000 * 60) {
      return String.format("%.1fmin", (double) duration / 60000);
    } else if (duration > 1000) {
      return String.format("%.3fsec", (double) duration / 1000);
    } else {
      return String.format("%dms", duration);
    }
  }

  @ShellMethod("Listen to a topic")
  public String listen(
      @ShellOption String topicName,
      @ShellOption String compact,
      @ShellOption(defaultValue = "null") String alias)
      throws ExecutionException, InterruptedException {
    if (alias.equals("null")) {
      alias = null;
    }

    StringBuilder ob = new StringBuilder();
    try {
      boolean effectiveCompact = Boolean.parseBoolean(compact);
      if (recordStoreController.storeExist(topicName, effectiveCompact)) {
        return ob.append("Already listening to topic ").append(topicName)
            .append(" with config compact=").append(effectiveCompact).toString();
      }
      OffsetTrackingRecordStore store = recordStoreController.newStore(topicName, effectiveCompact);
      consumerThreadController.addStoreAssignment(topicName, store);
      consumerThreadController.addTopicToAssignment(topicName);

      boolean createAlias = false;
      if (alias != null) {
        if (recordStoreController.storeExist(alias)) {
          ob.append("WARN: could not use alias ").append(alias)
              .append(" since a store exists with that name\n");
        } else {
          createAlias = true;
          storeAlias.put(alias, store.getStoreName());
        }
      }
      ob.append("Created store ").append(store.getStoreName());
      if (createAlias) {
        ob.append(" with alias ").append(alias);
      }

      return ob.toString();
    } catch (UndeclaredThrowableException e) {
      System.err.println("Could not listen to topic: " + e.getCause().getMessage());
      return "";
    }
  }

}
