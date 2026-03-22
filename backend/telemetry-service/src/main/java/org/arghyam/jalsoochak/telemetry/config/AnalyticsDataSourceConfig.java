package org.arghyam.jalsoochak.telemetry.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AnalyticsDataSourceConfig {

    @Bean(name = "analyticsDataSource")
    public DataSource analyticsDataSource(Environment environment) {
        String url = resolveAnalyticsUrl(environment);
        String username = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD");

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean(name = "analyticsJdbcTemplate")
    public JdbcTemplate analyticsJdbcTemplate(@Qualifier("analyticsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private String resolveAnalyticsUrl(Environment environment) {
        String explicitUrl = environment.getProperty("ANALYTICS_DATASOURCE_URL");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }

        String primaryUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (primaryUrl == null || primaryUrl.isBlank()) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL is required to derive analytics datasource URL");
        }

        int queryIndex = primaryUrl.indexOf('?');
        String base = queryIndex >= 0 ? primaryUrl.substring(0, queryIndex) : primaryUrl;
        String suffix = queryIndex >= 0 ? primaryUrl.substring(queryIndex) : "";
        int slashIndex = base.lastIndexOf('/');
        if (slashIndex < 0) {
            return primaryUrl;
        }
        String prefix = base.substring(0, slashIndex + 1);
        return prefix + "analytics" + suffix;
    }
}
