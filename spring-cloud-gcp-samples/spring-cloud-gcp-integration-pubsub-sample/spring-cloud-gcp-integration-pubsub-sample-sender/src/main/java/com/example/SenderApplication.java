/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

/** Spring Integration Channel Adapters for Google Cloud Pub/Sub code sample. */
@SpringBootApplication
public class SenderApplication {

  private static final Log LOGGER = LogFactory.getLog(SenderApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(SenderApplication.class, args);
  }

  @Bean
  @ServiceActivator(inputChannel = "pubSubOutputChannel")
  public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
    PubSubMessageHandler adapter = new PubSubMessageHandler(pubsubTemplate, "exampleTopic");
    adapter.setFailureCallback(
        (exception, message) ->
            LOGGER.info("There was an error sending the message: " + message.getPayload()));

    adapter.setSuccessCallback(
        (messageId, message) ->
            LOGGER.info(
                "Message was sent successfully;\n\tpublish ID = "
                    + messageId
                    + "\n\tmessage="
                    + message.getPayload()));

    return adapter;
  }

  /** interface for sending a message to Pub/Sub. */
  @MessagingGateway(defaultRequestChannel = "pubSubOutputChannel")
  public interface PubSubOutboundGateway {

    void sendToPubSub(String text);
  }
}
