package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@JmixEntity
@Entity
@Table(name = "ITEM_UNIT_PRICE")
public class ItemUnitPrice extends BaseEntity {

    @InstanceName
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UNIT_ID", nullable = false)
    @InstanceName
    private Unit unit;

    @Column(name = "PRICE", nullable = false, precision = 20, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "WAREHOUSE_ID", nullable = false)
    private Warehouse warehouse;

    public Item getItem() {
        return item;
    }

    public Unit getUnit() {
        return unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public ItemUnitPrice setItem(Item item) {
        this.item = item;
        return this;
    }

    public ItemUnitPrice setUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    public ItemUnitPrice setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ItemUnitPrice setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        return this;
    }
}
