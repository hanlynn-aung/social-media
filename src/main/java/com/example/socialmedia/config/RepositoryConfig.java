package com.example.socialmedia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.socialmedia.repository",
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.REGEX,
                pattern = ".*MessageRepository"))
@EnableMongoRepositories(basePackages = {
        "com.example.socialmedia.audit",
        "com.example.socialmedia.repository"
})
public class RepositoryConfig {
}
