package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;

@JmixEntity
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @InstanceName
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @InstanceName
    @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    private Warehouse warehouse;

    @InstanceName
    @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;
    @OneToMany
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

    public String toString() {
        return "Order(user=" + this.getUser() + ", warehouse=" + this.getWarehouse() + ", project=" + this.getProject() + ", items=" + this.getItems() + ")";
    }
}
