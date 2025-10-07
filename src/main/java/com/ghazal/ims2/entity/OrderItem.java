package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.math.BigDecimal;

@JmixEntity
@Entity
@Table(name = "ORDER_ITEM")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @InstanceName
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
    @Column(nullable = false)
    private BigDecimal quantity;

    public OrderItem() {
    }

    public Long getId() {
        return this.id;
    }

    public Item getItem() {
        return this.item;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public OrderItem setId(Long id) {
        this.id = id;
        return this;
    }

    public OrderItem setItem(Item item) {
        this.item = item;
        return this;
    }

    public OrderItem setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public String toString() {
        return "OrderItem(id=" + this.getId() + ", item=" + this.getItem() + ", quantity=" + this.getQuantity() + ")";
    }
}
