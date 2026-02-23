package com.br.orderservice.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    public static final String TOPIC_ORDER_CREATED = "order.created";
    public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    public static final String TOPIC_ORDER_PAID = "order.paid";
    public static final String TOPIC_ORDER_SHIPPED = "order.shipped";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_CREATED, event.orderId().toString(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_CANCELLED, event.orderId().toString(), event);
    }

    public void publishOrderPaid(OrderPaidEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_PAID, event.orderId().toString(), event);
    }

    public void publishOrderShipped(OrderShippedEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_SHIPPED, event.orderId().toString(), event);
    }
}
