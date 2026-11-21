package com.batuhanturnay.leaveflow.common.web;

/** A lightweight pointer to a related record in a response: enough to display it and to link to it. */
public record Ref(Long id, String name) {

}
