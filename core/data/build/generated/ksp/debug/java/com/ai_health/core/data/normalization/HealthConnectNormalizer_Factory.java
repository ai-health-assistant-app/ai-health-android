package com.ai_health.core.data.normalization;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class HealthConnectNormalizer_Factory implements Factory<HealthConnectNormalizer> {
  @Override
  public HealthConnectNormalizer get() {
    return newInstance();
  }

  public static HealthConnectNormalizer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HealthConnectNormalizer newInstance() {
    return new HealthConnectNormalizer();
  }

  private static final class InstanceHolder {
    private static final HealthConnectNormalizer_Factory INSTANCE = new HealthConnectNormalizer_Factory();
  }
}
