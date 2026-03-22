package com.sharecart.dto;

import jakarta.validation.constraints.Min;

public class UpdateItemRequest {

    private String name;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private Boolean checked;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getChecked() { return checked; }
    public void setChecked(Boolean checked) { this.checked = checked; }
}
