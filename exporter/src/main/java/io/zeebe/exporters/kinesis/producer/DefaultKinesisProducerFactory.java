/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.exporters.kinesis.producer;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import io.zeebe.exporters.kinesis.config.Config;

public class DefaultKinesisProducerFactory implements KinesisProducerFactory {
  @Override
  public KinesisProducer newProducer(Config config) {
    final KinesisProducerConfiguration options = new KinesisProducerConfiguration();
    //options.setCredentialsProvider(config.getAWSConfig());
    options.setRegion(config.getAwsRegion());
    //    options.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    //    options.put(
    //        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
    //        config.getProducer().getMaxConcurrentRequests());
    //    options.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);
    //    options.put(
    //        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
    //        (int) config.getProducer().getRequestTimeout().toMillis());
    //    options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getProducer().getServers());
    //    options.put(ProducerConfig.CLIENT_ID_CONFIG, config.getProducer().getClientId());
    //
    //    // allow user configuration to override producer options
    //    if (config.getProducer().getConfig() != null) {
    //      options.putAll(config.getProducer().getConfig());
    //    }
    //
    //    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordIdSerializer.class);
    //    options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GenericRecordSerializer.class);

    return new KinesisProducer(options);
  }
}
