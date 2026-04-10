package com.sharecart.sharecart.common.exception;

public class ExpiredInviteTokenException extends RuntimeException {

    public ExpiredInviteTokenException() {
        super("Invite link has expired");
    }

    public ExpiredInviteTokenException(String message) {
        super(message);
    }
}
