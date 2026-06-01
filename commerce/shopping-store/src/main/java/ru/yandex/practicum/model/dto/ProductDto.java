package ru.yandex.practicum.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import ru.yandex.practicum.model.ProductCategory;
import ru.yandex.practicum.model.ProductState;
import ru.yandex.practicum.model.QuantityState;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    @Null(message = "ID должен быть null при создании")
    private UUID productId;

    @NotBlank(message = "Наименование товара не может быть пустым")
    private String productName;

    @NotBlank(message = "Описание товара не может быть пустым")
    private String description;

    private String imageSrc;

    @NotNull(message = "Статус количества не может быть null")
    private QuantityState quantityState;

    @NotNull(message = "Статус товара не может быть null")
    private ProductState productState;

    @NotNull(message = "Категория товара не может быть null")
    private ProductCategory productCategory;

    @Positive(message = "Цена должна быть положительным числом")
    @DecimalMin(value = "1", message = "Цена должна быть не менее 1")
    private double price;
}