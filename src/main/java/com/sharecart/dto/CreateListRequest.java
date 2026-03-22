package com.sharecart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateListRequest {

    @NotBlank(message = "List name must not be blank")
    private String name;

    @NotNull(message = "Owner ID must not be null")
    private Long ownerId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
