package com.maplehub.ecommerce.order.service;

import com.maplehub.ecommerce.common.exception.GlobalExceptionHandling.ResourceNotFoundException;
import com.maplehub.ecommerce.order.dto.OrderDto;
import com.maplehub.ecommerce.order.model.Order;
import com.maplehub.ecommerce.order.model.OrderItem;
import com.maplehub.ecommerce.order.model.OrderStatus;
import com.maplehub.ecommerce.order.repository.OrderRepository;
import com.maplehub.ecommerce.order.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDto.ItemRequest itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .build();
            order.addItem(item);
            total = total.add(itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }
        order.setTotalAmount(total);

        order = orderRepository.save(order);
        log.info("Order created: id={}, customer={}", order.getId(), order.getCustomerId());

        // Start the Saga
        sagaOrchestrator.startSaga(order);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDto.Response getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.Response> getCustomerOrders(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toResponse);
    }

    private OrderDto.Response toResponse(Order order) {
        return OrderDto.Response.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .items(order.getItems().stream()
                        .map(item -> OrderDto.ItemResponse.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .toList())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
