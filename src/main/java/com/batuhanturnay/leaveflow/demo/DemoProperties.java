package com.batuhanturnay.leaveflow.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the public demo ({@code demo} profile).
 *
 * @param password the shared password of the demo accounts. It is shown on the login page on
 * purpose: these accounts exist only in a demo database.
 */
@ConfigurationProperties("leaveflow.demo")
public record DemoProperties(@DefaultValue("leaveflow-demo") String password) {

}
