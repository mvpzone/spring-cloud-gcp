/*
 * Copyright 2021-2021 the original author or authors.
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

package com.google.cloud.spring.pubsub.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.gax.batching.FlowController;
import com.google.api.gax.rpc.StatusCode.Code;
import com.google.pubsub.v1.ProjectSubscriptionName;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PubSubConfigurationTests {

  static final String QUALIFIED_SUBSCRIPTION_NAME = "projects/projectId/subscriptions/subscription-name";

  @Test
  void testDefaultHealthProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    pubSubConfiguration.initialize("projectId");
    PubSubConfiguration.Health health = pubSubConfiguration.getHealth();

    assertThat(health.getLagThreshold()).isNull();
    assertThat(health.getBacklogThreshold()).isNull();
    assertThat(health.getLookUpInterval()).isEqualTo(1);
    assertThat(health.getExecutorThreads()).isEqualTo(4);
  }

  @Test
  void testDefaultSubscriberProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    pubSubConfiguration.initialize("projectId");
    PubSubConfiguration.Subscriber subscriber = pubSubConfiguration.getSubscriber();
    PubSubConfiguration.FlowControl flowControl = subscriber.getFlowControl();
    PubSubConfiguration.Retry retrySettings = subscriber.getRetry();

    assertThat(subscriber.getExecutorThreads()).isNull();
    assertThat(subscriber.getMaxAcknowledgementThreads()).isEqualTo(4);
    assertThat(subscriber.getParallelPullCount()).isNull();

    assertThat(subscriber.getMaxAckExtensionPeriod()).isNull();
    assertThat(subscriber.getPullEndpoint()).isNull();
    assertThat(flowControl.getLimitExceededBehavior()).isNull();
    assertThat(flowControl.getMaxOutstandingElementCount()).isNull();
    assertThat(flowControl.getMaxOutstandingRequestBytes()).isNull();
    assertThat(retrySettings.getTotalTimeoutSeconds()).isNull();
    assertThat(retrySettings.getInitialRetryDelaySeconds()).isNull();
    assertThat(retrySettings.getRetryDelayMultiplier()).isNull();
    assertThat(retrySettings.getMaxRetryDelaySeconds()).isNull();
    assertThat(retrySettings.getMaxAttempts()).isNull();
    assertThat(retrySettings.getJittered()).isNull();
    assertThat(retrySettings.getInitialRpcTimeoutSeconds()).isNull();
    assertThat(retrySettings.getRpcTimeoutMultiplier()).isNull();
    assertThat(retrySettings.getMaxRpcTimeoutSeconds()).isNull();
    assertThat(subscriber.getRetryableCodes()).isNull();
  }

  @Test
  void testSubscriberProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = pubSubConfiguration.getSubscriber();

    subscriber.setExecutorThreads(1);
    subscriber.setMaxAcknowledgementThreads(3);
    subscriber.setParallelPullCount(1);

    subscriber.setMaxAckExtensionPeriod(1L);
    subscriber.setPullEndpoint("fake-endpoint");
    subscriber.setRetryableCodes(
        new Code[] {Code.UNKNOWN, Code.ABORTED, Code.UNAVAILABLE, Code.INTERNAL});

    pubSubConfiguration.initialize("projectId");

    assertThat(subscriber.getExecutorThreads()).isEqualTo(1);
    assertThat(subscriber.getMaxAcknowledgementThreads()).isEqualTo(3);
    assertThat(subscriber.getParallelPullCount()).isEqualTo(1);

    assertThat(subscriber.getMaxAckExtensionPeriod()).isEqualTo(1L);
    assertThat(subscriber.getPullEndpoint()).isEqualTo("fake-endpoint");
    assertThat(subscriber.getRetryableCodes())
        .containsExactly(Code.UNKNOWN, Code.ABORTED, Code.UNAVAILABLE, Code.INTERNAL);
  }

  @Test
  void testHealthProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Health health = pubSubConfiguration.getHealth();

    health.setLagThreshold(3);
    health.setBacklogThreshold(4);
    health.setLookUpInterval(6);
    health.setExecutorThreads(5);

    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.getHealth().getLagThreshold()).isEqualTo(3);
    assertThat(pubSubConfiguration.getHealth().getBacklogThreshold()).isEqualTo(4);
    assertThat(pubSubConfiguration.getHealth().getLookUpInterval()).isEqualTo(6);
    assertThat(pubSubConfiguration.getHealth().getExecutorThreads()).isEqualTo(5);
  }

  @Test
  void testSubscriberFlowControlSettings() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = pubSubConfiguration.getSubscriber();
    PubSubConfiguration.FlowControl flowControl = subscriber.getFlowControl();

    flowControl.setLimitExceededBehavior(FlowController.LimitExceededBehavior.Ignore);
    flowControl.setMaxOutstandingElementCount(1L);
    flowControl.setMaxOutstandingRequestBytes(2L);

    pubSubConfiguration.initialize("projectId");

    assertThat(flowControl.getLimitExceededBehavior())
        .isEqualTo(FlowController.LimitExceededBehavior.Ignore);
    assertThat(flowControl.getMaxOutstandingElementCount()).isEqualTo(1L);
    assertThat(flowControl.getMaxOutstandingRequestBytes()).isEqualTo(2L);
  }

  @Test
  void testComputeFlowControlSettings_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();

    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    PubSubConfiguration.FlowControl selectiveFlowControl = subscriber.getFlowControl();

    selectiveFlowControl.setLimitExceededBehavior(FlowController.LimitExceededBehavior.Ignore);
    selectiveFlowControl.setMaxOutstandingElementCount(1L);
    selectiveFlowControl.setMaxOutstandingRequestBytes(2L);

    ProjectSubscriptionName projectSubscriptionName =
        ProjectSubscriptionName.parse(QUALIFIED_SUBSCRIPTION_NAME);
    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));
    pubSubConfiguration.initialize("projectId");

    PubSubConfiguration.FlowControl result =
        pubSubConfiguration.computeSubscriberFlowControlSettings(projectSubscriptionName);

    assertThat(result.getLimitExceededBehavior())
        .isEqualTo(FlowController.LimitExceededBehavior.Ignore);
    assertThat(result.getMaxOutstandingElementCount()).isEqualTo(1L);
    assertThat(result.getMaxOutstandingRequestBytes()).isEqualTo(2L);
  }

  @Test
  void testComputeFlowControlSettings_returnGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();
    PubSubConfiguration.FlowControl globalFlowControl = globalSubscriber.getFlowControl();

    globalFlowControl.setLimitExceededBehavior(FlowController.LimitExceededBehavior.Ignore);
    globalFlowControl.setMaxOutstandingElementCount(1L);
    globalFlowControl.setMaxOutstandingRequestBytes(2L);

    pubSubConfiguration.initialize("projectId");

    ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of("projectId", "subscription-name");
    PubSubConfiguration.FlowControl result =
        pubSubConfiguration.computeSubscriberFlowControlSettings(projectSubscriptionName);

    assertThat(result.getLimitExceededBehavior())
        .isEqualTo(FlowController.LimitExceededBehavior.Ignore);
    assertThat(result.getMaxOutstandingElementCount()).isEqualTo(1L);
    assertThat(result.getMaxOutstandingRequestBytes()).isEqualTo(2L);
  }

  @Test
  void testComputeParallelPullCount_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setParallelPullCount(2);
    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));

    pubSubConfiguration.initialize("projectId");

    Integer result = pubSubConfiguration.computeParallelPullCount("subscription-name", "projectId");

    assertThat(result).isEqualTo(2);
  }

  @Test
  void testComputeParallelPullCount_returnGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();
    globalSubscriber.setParallelPullCount(2);
    pubSubConfiguration.initialize("projectId");

    Integer result = pubSubConfiguration.computeParallelPullCount("subscription-name", "projectId");

    assertThat(result).isEqualTo(2);
  }

  @Test
  void testComputePullEndpoint_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setPullEndpoint("endpoint");
    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));

    pubSubConfiguration.initialize("projectId");

    String result = pubSubConfiguration.computePullEndpoint("subscription-name", "projectId");

    assertThat(result).isEqualTo("endpoint");
  }

  @Test
  void testComputePullEndpoint_returnGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();
    globalSubscriber.setPullEndpoint("endpoint");
    pubSubConfiguration.initialize("projectId");

    String result = pubSubConfiguration.computePullEndpoint("subscription-name", "projectId");

    assertThat(result).isEqualTo("endpoint");
  }

  @Test
  void testComputeMaxAckExtensionPeriod_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setMaxAckExtensionPeriod(1L);
    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));

    pubSubConfiguration.initialize("projectId");

    Long result =
        pubSubConfiguration.computeMaxAckExtensionPeriod("subscription-name", "projectId");

    assertThat(result).isEqualTo(1);
  }

  @Test
  void testComputeMaxAckExtensionPeriod_returnGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();
    globalSubscriber.setMaxAckExtensionPeriod(2L);
    pubSubConfiguration.initialize("projectId");

    Long result =
        pubSubConfiguration.computeMaxAckExtensionPeriod("subscription-name", "projectId");

    assertThat(result).isEqualTo(2);
  }

  @Test
  void testComputeMaxAckExtensionPeriod_returnDefault() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    pubSubConfiguration.initialize("projectId");

    Long result =
        pubSubConfiguration.computeMaxAckExtensionPeriod("subscription-name", "projectId");

    assertThat(result).isZero();
  }

  @Test
  void testSubscriberRetrySettings() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = pubSubConfiguration.getSubscriber();
    PubSubConfiguration.Retry retrySettings = subscriber.getRetry();

    retrySettings.setTotalTimeoutSeconds(10L);
    retrySettings.setInitialRetryDelaySeconds(15L);
    retrySettings.setRetryDelayMultiplier(12.0);
    retrySettings.setMaxRetryDelaySeconds(10L);
    retrySettings.setMaxAttempts(13);
    retrySettings.setJittered(true);
    retrySettings.setInitialRpcTimeoutSeconds(11L);
    retrySettings.setRpcTimeoutMultiplier(14.0);
    retrySettings.setMaxRpcTimeoutSeconds(9L);

    pubSubConfiguration.initialize("projectId");

    assertThat(retrySettings.getTotalTimeoutSeconds()).isEqualTo(10L);
    assertThat(retrySettings.getInitialRetryDelaySeconds()).isEqualTo(15L);
    assertThat(retrySettings.getRetryDelayMultiplier()).isEqualTo(12.0);
    assertThat(retrySettings.getMaxRetryDelaySeconds()).isEqualTo(10L);
    assertThat(retrySettings.getMaxAttempts()).isEqualTo(13);
    assertThat(retrySettings.getJittered()).isTrue();
    assertThat(retrySettings.getInitialRpcTimeoutSeconds()).isEqualTo(11L);
    assertThat(retrySettings.getRpcTimeoutMultiplier()).isEqualTo(14.0);
    assertThat(retrySettings.getMaxRpcTimeoutSeconds()).isEqualTo(9L);
  }

  @Test
  void testComputeSubscriberRetrySettings_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    PubSubConfiguration.Retry retry = subscriber.getRetry();

    retry.setTotalTimeoutSeconds(10L);
    retry.setInitialRetryDelaySeconds(15L);
    retry.setRetryDelayMultiplier(12.0);
    retry.setMaxRetryDelaySeconds(10L);
    retry.setMaxAttempts(13);
    retry.setJittered(true);
    retry.setInitialRpcTimeoutSeconds(11L);
    retry.setRpcTimeoutMultiplier(14.0);
    retry.setMaxRpcTimeoutSeconds(9L);

    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));

    pubSubConfiguration.initialize("projectId");

    PubSubConfiguration.Retry result =
        pubSubConfiguration.computeSubscriberRetrySettings("subscription-name", "projectId");

    assertThat(result.getTotalTimeoutSeconds()).isEqualTo(10L);
    assertThat(result.getInitialRetryDelaySeconds()).isEqualTo(15L);
    assertThat(result.getRetryDelayMultiplier()).isEqualTo(12.0);
    assertThat(result.getMaxRetryDelaySeconds()).isEqualTo(10L);
    assertThat(result.getMaxAttempts()).isEqualTo(13);
    assertThat(result.getJittered()).isTrue();
    assertThat(result.getInitialRpcTimeoutSeconds()).isEqualTo(11L);
    assertThat(result.getRpcTimeoutMultiplier()).isEqualTo(14.0);
    assertThat(result.getMaxRpcTimeoutSeconds()).isEqualTo(9L);
  }

  @Test
  void testComputeSubscriberRetrySettings_returnGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();
    PubSubConfiguration.Retry retry = globalSubscriber.getRetry();

    retry.setTotalTimeoutSeconds(10L);
    retry.setInitialRetryDelaySeconds(15L);
    retry.setRetryDelayMultiplier(12.0);
    retry.setMaxRetryDelaySeconds(10L);
    retry.setMaxAttempts(13);
    retry.setJittered(true);
    retry.setInitialRpcTimeoutSeconds(11L);
    retry.setRpcTimeoutMultiplier(14.0);
    retry.setMaxRpcTimeoutSeconds(9L);

    pubSubConfiguration.initialize("projectId");

    PubSubConfiguration.Retry result =
        pubSubConfiguration.computeSubscriberRetrySettings("subscription-name", "projectId");

    assertThat(result.getTotalTimeoutSeconds()).isEqualTo(10L);
    assertThat(result.getInitialRetryDelaySeconds()).isEqualTo(15L);
    assertThat(result.getRetryDelayMultiplier()).isEqualTo(12.0);
    assertThat(result.getMaxRetryDelaySeconds()).isEqualTo(10L);
    assertThat(result.getMaxAttempts()).isEqualTo(13);
    assertThat(result.getInitialRpcTimeoutSeconds()).isEqualTo(11L);
    assertThat(result.getRpcTimeoutMultiplier()).isEqualTo(14.0);
    assertThat(result.getMaxRpcTimeoutSeconds()).isEqualTo(9L);
  }

  @Test
  void testComputeRetryableCodes_returnsGlobal() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber globalSubscriber = pubSubConfiguration.getSubscriber();

    globalSubscriber.setRetryableCodes(new Code[] {Code.INTERNAL});
    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.computeRetryableCodes("subscription-name", "projectId"))
        .containsExactly(Code.INTERNAL);
  }

  @Test
  void testComputeRetryableCodes_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setRetryableCodes(new Code[] {Code.INTERNAL});
    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));
    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.computeRetryableCodes("subscription-name", "projectId"))
        .containsExactly(Code.INTERNAL);
  }

  @Test
  void testSubscriberMapProperties_defaultOrGlobal_notAddedToMap() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).isEmpty();
    assertThat(
            pubSubConfiguration
                .getSubscriber("subscription-name", "projectId")
                .getExecutorThreads())
        .isNull();
    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).isEmpty();
  }

  @Test
  void testSubscriberMapProperties_subscriptionName_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setExecutorThreads(8);

    pubSubConfiguration.setSubscription(
        Collections.singletonMap("subscription-name", subscriber));

    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).hasSize(1);
    assertThat(
            pubSubConfiguration
                .getSubscriber("subscription-name", "projectId")
                .getExecutorThreads())
        .isEqualTo(8);
    // asserts that map did not change from a getter. Might not be needed now that map is immutable.
    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).hasSize(1);
    assertThat(
            pubSubConfiguration
                .getFullyQualifiedSubscriberProperties()
                .get(ProjectSubscriptionName.parse(QUALIFIED_SUBSCRIPTION_NAME))
                .getExecutorThreads())
        .isEqualTo(8);
  }

  @Test
  void testSubscriberMapProperties_fullNamePresentInMap_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setExecutorThreads(8);

    pubSubConfiguration.setSubscription(
        Collections.singletonMap(QUALIFIED_SUBSCRIPTION_NAME, subscriber));

    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).hasSize(1);
    assertThat(
            pubSubConfiguration
                .getSubscriber(QUALIFIED_SUBSCRIPTION_NAME, "projectId")
                .getExecutorThreads())
        .isEqualTo(8);
    assertThat(
            pubSubConfiguration
                .getFullyQualifiedSubscriberProperties()
                .get(ProjectSubscriptionName.parse(QUALIFIED_SUBSCRIPTION_NAME))
                .getExecutorThreads())
        .isEqualTo(8);
  }

  @Test
  void testSubscriberMapProperties_fullNamePresentInMap_projectIdIgnored_returnCustom() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Subscriber subscriber = new PubSubConfiguration.Subscriber();
    subscriber.setExecutorThreads(8);

    pubSubConfiguration.setSubscription(Collections.singletonMap(
        "projects/otherProjectId/subscriptions/subscription-name", subscriber));

    pubSubConfiguration.initialize("projectId");

    assertThat(pubSubConfiguration.getFullyQualifiedSubscriberProperties()).hasSize(1);
    assertThat(
            pubSubConfiguration
                .getSubscriber(
                    "projects/otherProjectId/subscriptions/subscription-name", "projectId")
                .getExecutorThreads())
        .isEqualTo(8);
    assertThat(
            pubSubConfiguration
                .getFullyQualifiedSubscriberProperties()
                .get(ProjectSubscriptionName.parse("projects/otherProjectId/subscriptions/subscription-name"))
                .getExecutorThreads())
        .isEqualTo(8);
  }

  @Test
  void testDefaultPublisherProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Publisher publisher = pubSubConfiguration.getPublisher();
    PubSubConfiguration.Batching batching = publisher.getBatching();
    PubSubConfiguration.Retry retrySettings = publisher.getRetry();

    pubSubConfiguration.initialize("projectId");

    assertThat(publisher.getExecutorThreads()).isEqualTo(4);
    assertThat(publisher.getEnableMessageOrdering()).isNull();
    assertThat(publisher.getEndpoint()).isNull();
    assertThat(batching.getElementCountThreshold()).isNull();
    assertThat(batching.getRequestByteThreshold()).isNull();
    assertThat(batching.getDelayThresholdSeconds()).isNull();
    assertThat(batching.getEnabled()).isNull();
    assertThat(batching.getFlowControl().getLimitExceededBehavior()).isNull();
    assertThat(batching.getFlowControl().getMaxOutstandingElementCount()).isNull();
    assertThat(batching.getFlowControl().getMaxOutstandingRequestBytes()).isNull();
    assertThat(retrySettings.getTotalTimeoutSeconds()).isNull();
    assertThat(retrySettings.getRetryDelayMultiplier()).isNull();
    assertThat(retrySettings.getMaxRetryDelaySeconds()).isNull();
    assertThat(retrySettings.getMaxAttempts()).isNull();
    assertThat(retrySettings.getJittered()).isNull();
    assertThat(retrySettings.getInitialRpcTimeoutSeconds()).isNull();
    assertThat(retrySettings.getRpcTimeoutMultiplier()).isNull();
    assertThat(retrySettings.getMaxRpcTimeoutSeconds()).isNull();
  }

  @Test
  void testPublisherProperties() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Publisher publisher = pubSubConfiguration.getPublisher();

    publisher.setExecutorThreads(5);
    publisher.setEnableMessageOrdering(true);
    publisher.setEndpoint("fake-endpoint");

    pubSubConfiguration.initialize("projectId");

    assertThat(publisher.getExecutorThreads()).isEqualTo(5);
    assertThat(publisher.getEnableMessageOrdering()).isTrue();
    assertThat(publisher.getEndpoint()).isEqualTo("fake-endpoint");
  }

  @Test
  void testPublisherBatchingSettings() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Publisher publisher = pubSubConfiguration.getPublisher();
    PubSubConfiguration.Batching batching = publisher.getBatching();

    batching.setElementCountThreshold(1L);
    batching.setRequestByteThreshold(5L);
    batching.setDelayThresholdSeconds(3L);
    batching.setEnabled(true);
    PubSubConfiguration.FlowControl flowControl = publisher.getBatching().getFlowControl();
    flowControl.setLimitExceededBehavior(FlowController.LimitExceededBehavior.Ignore);
    flowControl.setMaxOutstandingElementCount(6L);
    flowControl.setMaxOutstandingRequestBytes(3L);

    pubSubConfiguration.initialize("projectId");

    assertThat(batching.getElementCountThreshold()).isEqualTo(1L);
    assertThat(batching.getRequestByteThreshold()).isEqualTo(5L);
    assertThat(batching.getDelayThresholdSeconds()).isEqualTo(3L);
    assertThat(batching.getEnabled()).isTrue();
    assertThat(batching.getFlowControl().getLimitExceededBehavior())
        .isEqualTo(FlowController.LimitExceededBehavior.Ignore);
    assertThat(batching.getFlowControl().getMaxOutstandingElementCount()).isEqualTo(6L);
    assertThat(batching.getFlowControl().getMaxOutstandingRequestBytes()).isEqualTo(3L);
  }

  @Test
  void testPublisherRetrySettings() {
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    PubSubConfiguration.Publisher publisher = pubSubConfiguration.getPublisher();
    PubSubConfiguration.Retry retrySettings = publisher.getRetry();

    retrySettings.setTotalTimeoutSeconds(3L);
    retrySettings.setRetryDelayMultiplier(12.0);
    retrySettings.setMaxRetryDelaySeconds(2L);
    retrySettings.setMaxAttempts(3);
    retrySettings.setJittered(true);
    retrySettings.setInitialRpcTimeoutSeconds(6L);
    retrySettings.setRpcTimeoutMultiplier(12.0);
    retrySettings.setMaxRpcTimeoutSeconds(8L);

    pubSubConfiguration.initialize("projectId");

    assertThat(retrySettings.getTotalTimeoutSeconds()).isEqualTo(3L);
    assertThat(retrySettings.getRetryDelayMultiplier()).isEqualTo(12.0);
    assertThat(retrySettings.getMaxRetryDelaySeconds()).isEqualTo(2L);
    assertThat(retrySettings.getMaxAttempts()).isEqualTo(3);
    assertThat(retrySettings.getJittered()).isTrue();
    assertThat(retrySettings.getInitialRpcTimeoutSeconds()).isEqualTo(6L);
    assertThat(retrySettings.getRpcTimeoutMultiplier()).isEqualTo(12.0);
    assertThat(retrySettings.getMaxRpcTimeoutSeconds()).isEqualTo(8L);
  }
}
