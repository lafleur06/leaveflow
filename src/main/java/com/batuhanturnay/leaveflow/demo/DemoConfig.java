package com.batuhanturnay.leaveflow.demo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Everything in this package is active only with the {@code demo} profile. */
@Configuration(proxyBeanMethods = false)
@Profile("demo")
@EnableConfigurationProperties(DemoProperties.class)
class DemoConfig {

}
