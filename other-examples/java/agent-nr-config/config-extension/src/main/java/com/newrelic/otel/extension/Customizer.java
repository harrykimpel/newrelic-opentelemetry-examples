package com.newrelic.otel.extension;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.UUID;

/**
 * Note this class is wired into SPI via {@code
 * resources/META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider}
 */
public class Customizer implements AutoConfigurationCustomizerProvider {

  private static final AttributeKey<String> SERVICE_INSTANCE_ID = stringKey("service.instance.id");
  private static final AttributeKey<String> HTTP_ROUTE = stringKey("http.route");

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    // Add additional resource attributes programmatically
    autoConfiguration.addResourceCustomizer(
        (resource, configProperties) ->
            resource.merge(
                Resource.builder().put(SERVICE_INSTANCE_ID, UUID.randomUUID().toString()).build()));

    // Set the sampler to be the default parentbased_always_on, but drop calls to spring
    // boot actuator endpoints
    autoConfiguration.addTracerProviderCustomizer(
        (sdkTracerProviderBuilder, configProperties) ->
            sdkTracerProviderBuilder.setSampler(
                Sampler.parentBased(
                    RuleBasedRoutingSampler.builder(SpanKind.SERVER, Sampler.alwaysOn())
                        .drop(HTTP_ROUTE, "/actuator.*")
                        .build())));
  }
}
