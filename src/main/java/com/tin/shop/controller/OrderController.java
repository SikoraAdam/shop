package com.tin.shop.controller;

import com.tin.shop.dto.OrderProductDto;
import com.tin.shop.exception.ResourceNotFoundException;
import com.tin.shop.model.Order;
import com.tin.shop.model.OrderProduct;
import com.tin.shop.model.OrderStatus;
import com.tin.shop.service.OrderProductService;
import com.tin.shop.service.OrderService;
import com.tin.shop.service.ProductService;
import com.tin.shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    ProductService productService;
    OrderService orderService;
    OrderProductService orderProductService;
    UserService userService;

    @Autowired
    public OrderController(
            ProductService productService,
            OrderService orderService,
            OrderProductService orderProductService,
            UserService userService) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderProductService = orderProductService;
        this.userService = userService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public @NotNull Iterable<Order> list() {
        return this.orderService.getAllOrders();
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderForm form) {
        List<OrderProductDto> formDtos = form.getProductOrders();
        validateProductsExistence(formDtos);
        Order order = new Order();
        order.setStatus(OrderStatus.PAID.name());
        order.setUser(userService.getCurrentUser());
        order = this.orderService.create(order);

        List<OrderProduct> orderProducts = new ArrayList<>();
        for (OrderProductDto dto : formDtos) {
            orderProducts.add(orderProductService.create(new OrderProduct(order,
                    productService.getProduct(dto.getProduct().getId()), dto.getQuantity())));
        }

        order.setOrderProducts(orderProducts);

        this.orderService.update(order);

        String uri = ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/orders/{id}")
                .buildAndExpand(order.getId())
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", uri);

        return new ResponseEntity<>(order, headers, HttpStatus.CREATED);
    }

    private void validateProductsExistence(List<OrderProductDto> orderProducts) {
        List<OrderProductDto> list = orderProducts
                .stream()
                .filter(op -> Objects.isNull(
                        productService.getProduct(op.getProduct().getId())))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(list)) {
           throw new ResourceNotFoundException("Product not found");
        }
    }

    public static class OrderForm {

        private List<OrderProductDto> productOrders;

        public List<OrderProductDto> getProductOrders() {
            return productOrders;
        }

        public void setProductOrders(List<OrderProductDto> productOrders) {
            this.productOrders = productOrders;
        }
    }
}
