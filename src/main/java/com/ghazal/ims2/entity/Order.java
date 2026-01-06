package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;

@JmixEntity
@Entity(name = "ims_Order")
@Table(name = "ims_order")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    private Warehouse warehouse;

    @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    public Order() {
    }

    public User getUser() {
        return this.user;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    public Project getProject() {
        return this.project;
    }

    public List<OrderItem> getItems() {
        return this.items;
    }

    public Order setUser(User user) {
        this.user = user;
        return this;
    }

    public Order setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        return this;
    }

    public Order setProject(Project project) {
        this.project = project;
        return this;
    }

    public Order setItems(List<OrderItem> items) {
        this.items = items;
        return this;
    }

    @InstanceName
    public String getInstanceName() {
        Object id = getId();
        return id == null ? "Order" : "Order " + id;
    }

    @Override
    public String toString() {
        return getInstanceName();
    }
}
