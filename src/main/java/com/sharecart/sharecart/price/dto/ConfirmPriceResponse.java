package com.sharecart.sharecart.price.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConfirmPriceResponse(
        UUID id,
        Integer savedCount,
        List<UUID> ids,
        String message
) {
}