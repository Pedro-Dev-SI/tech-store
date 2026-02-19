package com.br.inventoryservice.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {

    public static final String TOPIC_STOCK_RESERVED = "inventory.stock.reserved";
    public static final String TOPIC_STOCK_RELEASED = "inventory.stock.released";
    public static final String TOPIC_STOCK_CONFIRMED = "inventory.stock.confirmed";
    public static final String TOPIC_STOCK_LOW_ALERT = "inventory.stock.low-alert";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishStockReserved(StockReservedEvent event) {
        kafkaTemplate.send(TOPIC_STOCK_RESERVED, event.productId().toString(), event);
    }

    public void publishStockReleased(StockReleasedEvent event) {
        kafkaTemplate.send(TOPIC_STOCK_RELEASED, event.productId().toString(), event);
    }

    public void publishStockConfirmed(StockConfirmedEvent event) {
        kafkaTemplate.send(TOPIC_STOCK_CONFIRMED, event.productId().toString(), event);
    }

    public void publishStockLowAlert(StockLowAlertEvent event) {
        kafkaTemplate.send(TOPIC_STOCK_LOW_ALERT, event.productId().toString(), event);
    }
}
