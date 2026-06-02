package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ChangeProductQuantityRequest;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.model.dto.ShoppingCartDto;
import ru.yandex.practicum.model.mapper.CartMapper;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);
        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);

        return cartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            CartItem existingItem = cartItemRepository
                    .findByShoppingCartAndProductId(cart, productId)
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                cartItemRepository.save(existingItem);
            } else {
                CartItem newItem = new CartItem();
                newItem.setShoppingCart(cart);
                newItem.setProductId(productId);
                newItem.setQuantity(quantity);
                cartItemRepository.save(newItem);
            }
        }

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        return cartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        validateUsername(username);

        shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .ifPresent(cart -> {
                    cart.setActive(false);
                    shoppingCartRepository.save(cart);
                    log.info("Deactivated cart for user: {}", username);
                });
    }

    @Override
    @Transactional
    public ShoppingCartDto removeProductsFromCart(String username, List<UUID> productIds) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        cartItemRepository.deleteByShoppingCartAndProductIdIn(cart, productIds);

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        return cartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public ShoppingCartDto updateProductQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        CartItem item = cartItemRepository
                .findByShoppingCartAndProductId(cart, request.getProductId())
                .orElseThrow(() -> new NoProductsInShoppingCartException(
                        "Product not found in cart: " + request.getProductId()));

        item.setQuantity(request.getNewQuantity());
        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        return cartMapper.toDto(cart, items);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Username cannot be empty");
        }
    }

    private ShoppingCart getOrCreateActiveCart(String username) {
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setUsername(username);
                    newCart.setActive(true);
                    return shoppingCartRepository.save(newCart);
                });
    }
}