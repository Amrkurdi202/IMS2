package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@JmixEntity
@Entity(name = "ORDER_ITEM")
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @InstanceName
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID")   // nullable => can be null for custom items
    private Item item;

    @Column(name = "QUANTITY", nullable = false)
    private BigDecimal quantity;

    // price at the time of order (for normal catalog items)
    @Column(name = "UNIT_PRICE_AT_ORDER", precision = 20, scale = 2)
    private BigDecimal unitPriceAtOrder;

    // 🔹 NEW: is this a custom ad-hoc item (not from Item catalog)?
    @Column(name = "CUSTOM_ITEM")
    private Boolean customItem = false;

    // 🔹 NEW: free text name for custom items
    @Column(name = "CUSTOM_NAME")
    private String customName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    public OrderItem() {
    }

    // -------- getters / setters ----------

    public Long getId() {
        return id;
    }

    public OrderItem setId(Long id) {
        this.id = id;
        return this;
    }

    public Item getItem() {
        return item;
    }

    public OrderItem setItem(Item item) {
        this.item = item;
        return this;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public OrderItem setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public BigDecimal getUnitPriceAtOrder() {
        return unitPriceAtOrder;
    }

    public OrderItem setUnitPriceAtOrder(BigDecimal unitPriceAtOrder) {
        this.unitPriceAtOrder = unitPriceAtOrder;
        return this;
    }

    public Boolean getCustomItem() {
        return customItem;
    }

    public OrderItem setCustomItem(Boolean customItem) {
        this.customItem = customItem;
        return this;
    }

    public String getCustomName() {
        return customName;
    }

    public OrderItem setCustomName(String customName) {
        this.customName = customName;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public OrderItem setOrder(Order order) {
        this.order = order;
        return this;
    }

    @Override
    public String toString() {
        return "OrderItem(" +
                "id=" + id +
                ", item=" + item +
                ", quantity=" + quantity +
                ", customItem=" + customItem +
                ", customName=" + customName +
                ')';
    }
}
