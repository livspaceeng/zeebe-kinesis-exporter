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

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.kinesis.producer.UserRecord;
import io.zeebe.exporters.kinesis.config.RecordConfig;
import io.zeebe.exporters.kinesis.config.RecordsConfig;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.test.exporter.record.MockRecord;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import org.junit.Test;

public class RecordHandlerTest {
  @Test
  public void shouldTransformRecord() {
    // given
    final MockRecord record = new MockRecord();
    final RecordConfig recordConfig = new RecordConfig();
    final RecordsConfig recordsConfig = new RecordsConfig();
    final RecordHandler recordHandler = new RecordHandler(recordsConfig);
    recordsConfig.getTypeMap().put(ValueType.DEPLOYMENT, recordConfig);
    recordConfig.setTopic("topic");
    record.getMetadata().setValueType(ValueType.DEPLOYMENT);

    // when
    final UserRecord transformed = recordHandler.transform(record);

    // then
    assertThat(transformed.getStreamName()).isEqualTo(recordConfig.getTopic());
    assertThat(transformed.getPartitionKey()).isEqualTo(Integer.toString(record.getPartitionId()));
    assertThat(transformed.getData()).isEqualTo(ByteBuffer.wrap(record.toJson().getBytes()));
  }

  @Test
  public void shouldTestRecordAsNotAllowed() {
    // given
    final MockRecord record = new MockRecord();
    final RecordConfig recordConfig = new RecordConfig();
    final RecordsConfig recordsConfig = new RecordsConfig();
    final RecordHandler recordHandler = new RecordHandler(recordsConfig);
    recordsConfig.getTypeMap().put(ValueType.DEPLOYMENT, recordConfig);
    recordConfig.setAllowedTypes(EnumSet.of(RecordType.COMMAND));
    record.getMetadata().setValueType(ValueType.DEPLOYMENT).setRecordType(RecordType.EVENT);

    // when - then
    assertThat(recordHandler.test(record)).isFalse();
  }

  @Test
  public void shouldTestRecordAsAllowed() {
    // given
    final MockRecord record = new MockRecord();
    final RecordConfig recordConfig = new RecordConfig();
    final RecordsConfig recordsConfig = new RecordsConfig();
    final RecordHandler recordHandler = new RecordHandler(recordsConfig);
    recordsConfig.getTypeMap().put(ValueType.DEPLOYMENT, recordConfig);
    recordConfig.setAllowedTypes(EnumSet.of(RecordType.EVENT));
    record.getMetadata().setValueType(ValueType.DEPLOYMENT).setRecordType(RecordType.EVENT);

    // when - then
    assertThat(recordHandler.test(record)).isTrue();
  }
}
