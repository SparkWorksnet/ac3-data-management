package net.sparkworks.mapper;

import io.micrometer.core.instrument.MeterRegistry;
import net.sparkworks.cargo.client.config.CargoClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = {"net.sparkworks.mapper", CargoClientConfig.CARGO_CLIENT_BASE_PACKAGE_NAME})
public class Application {
    
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "hermes");
    }
    
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
