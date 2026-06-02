package ru.yandex.practicum.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.model.ProductCategory;
import ru.yandex.practicum.model.SetProductQuantityStateRequest;
import ru.yandex.practicum.model.dto.ProductDto;

import java.util.UUID;

public interface ProductService {

    Page<ProductDto> getProducts(ProductCategory category, Pageable pageable);

    ProductDto getProduct(UUID productId);

    ProductDto createProduct(ProductDto productDto);

    ProductDto updateProduct(ProductDto productDto);

    boolean removeProductFromStore(UUID productId);

    boolean setProductQuantityState(SetProductQuantityStateRequest request);
}