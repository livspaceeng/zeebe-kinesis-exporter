package io.zeebe.exporters.kinesis;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecord;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporters.kinesis.config.Config;
import io.zeebe.exporters.kinesis.config.parser.ConfigParser;
import io.zeebe.exporters.kinesis.config.parser.TomlConfigParser;
import io.zeebe.exporters.kinesis.config.toml.TomlConfig;
import io.zeebe.exporters.kinesis.producer.DefaultKinesisProducerFactory;
import io.zeebe.exporters.kinesis.producer.KinesisProducerFactory;
import io.zeebe.exporters.kinesis.record.KinesisRecordFilter;
import io.zeebe.exporters.kinesis.record.RecordHandler;
import io.zeebe.exporters.kinesis.util.Request;
import io.zeebe.exporters.kinesis.util.RequestQueue;
import io.zeebe.protocol.record.Record;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;

/**
 * Implementation of a Zeebe exporter producing serialized records to a given Kinesis topic.
 *
 * <p>TODO: implement another transmission strategy using transactions and see which is better
 */
public class KinesisExporter implements Exporter {
  static final Duration IN_FLIGHT_RECORD_CHECKER_INTERVAL = Duration.ofSeconds(1);
  private static final int UNSET_POSITION = -1;

  private final KinesisProducerFactory producerFactory;
  private final ConfigParser<TomlConfig, Config> configParser;

  private boolean isClosed = true;
  private String id;
  private Controller controller;
  private Logger logger;

  private Config config;
  private RecordHandler recordHandler;
  private KinesisProducer producer;
  private RequestQueue requests;
  private long latestExportedPosition = UNSET_POSITION;

  public KinesisExporter() {
    this.producerFactory = new DefaultKinesisProducerFactory();
    this.configParser = new TomlConfigParser();
  }

  public KinesisExporter(
      KinesisProducerFactory producerFactory, ConfigParser<TomlConfig, Config> configParser) {
    this.producerFactory = producerFactory;
    this.configParser = configParser;
  }

  @Override
  public void configure(Context context) {
    this.logger = context.getLogger();
    this.id = context.getConfiguration().getId();

    final TomlConfig tomlConfig = context.getConfiguration().instantiate(TomlConfig.class);
    this.config = this.configParser.parse(tomlConfig);
    this.recordHandler = new RecordHandler(this.config.getRecords());

    context.setFilter(new KinesisRecordFilter(this.config.getRecords()));
    this.logger.debug("Configured exporter {}", this.id);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;
    this.isClosed = false;
    this.requests = new RequestQueue(this.config.getMaxInFlightRecords());
    this.producer = this.producerFactory.newProducer(this.config);
    this.controller.scheduleTask(
        this.config.getInFlightRecordCheckInterval(), this::checkCompletedInFlightRequests);

    this.logger.debug("Opened exporter {}", this.id);
  }

  @Override
  public void close() {
    closeInternal();
    checkCompletedInFlightRequests();
    requests.cancelAll();

    logger.debug("Closed exporter {}", id);
  }

  @Override
  public void export(Record record) {
    // The producer may be closed prematurely if an unrecoverable exception occurred, at which point
    // we ignore any further records; this way we do not block the exporter processor, and on
    // restart will reprocess all other records that we "missed" here.
    if (this.producer == null) {
      requests.cancelAll();
      logger.debug("Exporter {} was prematurely closed earlier; skipping record {}", id, record);
      return;
    }

    if (recordHandler.test(record)) {
      final UserRecord userRecord = recordHandler.transform(record);
      // final ProducerRecord<Record, Record> kinesisRecord = recordHandler.transform(record);
      final Future<UserRecordResult> future = producer.addUserRecord(userRecord);
      final Request request = new Request(record.getPosition(), future);

      while (!requests.offer(request)) {
        logger.trace("Too many in flight records, blocking until at least one completes...");
        requests.consume(this::updatePosition);
      }

      logger.debug("Exported record {}", record);
    }
  }

  /* assumes it is called strictly as a scheduled task */
  private void checkCompletedInFlightRequests() {
    requests.consumeCompleted(this::updatePosition);
    if (latestExportedPosition != UNSET_POSITION) {
      controller.updateLastExportedRecordPosition(latestExportedPosition);
    }

    if (!isClosed) {
      controller.scheduleTask(
          IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRequests);
    }
  }

  private void updatePosition(Request request) {
    try {
      latestExportedPosition = request.get();
    } catch (CancellationException e) {
      logger.error(
          "In flight record was cancelled prematurely, will stop exporting to prevent missing records");
      closeInternal();
    } catch (ExecutionException e) {
      logger.error(
          "Failed to ensure record was sent to Kinesis, will stop exporting to prevent missing records",
          e);
      closeInternal();
    } catch (InterruptedException e) { // NOSONAR: throwing InterruptException flags interrupt again
      closeInternal();
      e.printStackTrace();
    }
  }

  private void closeInternal() {
    if (!isClosed) {
      isClosed = true;

      if (producer != null) {
        final Duration closeTimeout = config.getProducer().getCloseTimeout();
        logger.debug("Closing producer with timeout {}", closeTimeout);
        producer.destroy();
        producer = null;
      }
    }
  }
}
