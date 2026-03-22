package com.sharecart.dto;

import jakarta.validation.constraints.NotNull;

public class InviteRequest {

    @NotNull(message = "User ID must not be null")
    private Long userId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
