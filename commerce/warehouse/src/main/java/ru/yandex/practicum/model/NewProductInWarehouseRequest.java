package ru.yandex.practicum.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.model.dto.DimensionDto;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewProductInWarehouseRequest {

    @NotNull(message = "ID товара не может быть null")
    private UUID productId;

    private Boolean fragile;

    @Valid
    @NotNull(message = "Размеры товара не могут быть null")
    private DimensionDto dimension;

    @NotNull(message = "Вес товара не может быть null")
    @Positive(message = "Вес должен быть положительным")
    private Double weight;
}