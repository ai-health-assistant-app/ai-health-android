package com.ai_health.core.data.repository;

import com.ai_health.core.data.local.AppDatabase;
import com.ai_health.core.data.normalization.HealthConnectNormalizer;
import com.ai_health.core.health.HealthConnectManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HealthRepositoryImpl_Factory implements Factory<HealthRepositoryImpl> {
  private final Provider<HealthConnectManager> healthConnectManagerProvider;

  private final Provider<AppDatabase> dbProvider;

  private final Provider<HealthConnectNormalizer> healthConnectNormalizerProvider;

  public HealthRepositoryImpl_Factory(Provider<HealthConnectManager> healthConnectManagerProvider,
      Provider<AppDatabase> dbProvider,
      Provider<HealthConnectNormalizer> healthConnectNormalizerProvider) {
    this.healthConnectManagerProvider = healthConnectManagerProvider;
    this.dbProvider = dbProvider;
    this.healthConnectNormalizerProvider = healthConnectNormalizerProvider;
  }

  @Override
  public HealthRepositoryImpl get() {
    return newInstance(healthConnectManagerProvider.get(), dbProvider.get(), healthConnectNormalizerProvider.get());
  }

  public static HealthRepositoryImpl_Factory create(
      Provider<HealthConnectManager> healthConnectManagerProvider, Provider<AppDatabase> dbProvider,
      Provider<HealthConnectNormalizer> healthConnectNormalizerProvider) {
    return new HealthRepositoryImpl_Factory(healthConnectManagerProvider, dbProvider, healthConnectNormalizerProvider);
  }

  public static HealthRepositoryImpl newInstance(HealthConnectManager healthConnectManager,
      AppDatabase db, HealthConnectNormalizer healthConnectNormalizer) {
    return new HealthRepositoryImpl(healthConnectManager, db, healthConnectNormalizer);
  }
}
