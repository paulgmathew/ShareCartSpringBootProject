package com.sharecart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateItemRequest {

    @NotBlank(message = "Item name must not be blank")
    private String name;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
