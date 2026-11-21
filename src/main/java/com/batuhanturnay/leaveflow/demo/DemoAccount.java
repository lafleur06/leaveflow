package com.batuhanturnay.leaveflow.demo;

/**
 * An account offered on the login page of the demo.
 *
 * @param persona what you can try with it, e.g. "Manager: approves the engineering team's leave"
 */
public record DemoAccount(String name, String email, String password, String persona) {

}
