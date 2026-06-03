package ru.yandex.practicum.service;

import ru.yandex.practicum.model.AddProductToWarehouseRequest;
import ru.yandex.practicum.model.NewProductInWarehouseRequest;
import ru.yandex.practicum.model.dto.AddressDto;
import ru.yandex.practicum.model.dto.BookedProductsDto;
import ru.yandex.practicum.model.dto.ShoppingCartDto;

public interface WarehouseService {

    void newProductInWarehouse(NewProductInWarehouseRequest request);

    BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart);

    void addProductToWarehouse(AddProductToWarehouseRequest request);

    AddressDto getWarehouseAddress();
}