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
package io.zeebe.exporters.kinesis.config.parser;

public class MockConfigParser<T, R> implements ConfigParser<T, R> {
  private final ConfigParser<T, R> defaultConfigParser;
  public R config;

  public MockConfigParser(ConfigParser<T, R> defaultConfigParser) {
    this.defaultConfigParser = defaultConfigParser;
  }

  @Override
  public R parse(T config) {
    if (this.config == null) {
      return defaultConfigParser.parse(config);
    }

    return this.config;
  }
}
