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
package io.zeebe.exporters.kinesis.record;

import com.amazonaws.services.kinesis.producer.UserRecord;
import io.zeebe.exporters.kinesis.config.RecordConfig;
import io.zeebe.exporters.kinesis.config.RecordsConfig;
import io.zeebe.protocol.record.Record;
import java.nio.ByteBuffer;

public class RecordHandler {
  private final RecordsConfig configuration;

  public RecordHandler(RecordsConfig configuration) {
    this.configuration = configuration;
  }

  public UserRecord transform(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return new UserRecord(
        config.getTopic(),
        Integer.toString(record.getPartitionId()),
        ByteBuffer.wrap(record.toJson().getBytes()));
  }

  public boolean test(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return config.getAllowedTypes().contains(record.getRecordType());
  }

  private <T extends Record> RecordConfig getRecordConfig(T record) {
    return configuration.forType(record.getValueType());
  }
}
