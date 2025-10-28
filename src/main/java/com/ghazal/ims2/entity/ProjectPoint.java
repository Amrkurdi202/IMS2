package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.math.BigDecimal;

@JmixEntity
@Entity
@Table(name = "PROJECT_POINT")
public class ProjectPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @InstanceName
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    @InstanceName
    private BigDecimal quantity;

    public ProjectPoint() {
    }

    public Long getId() {
        return this.id;
    }

    public Point getPoint() {
        return this.point;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public ProjectPoint setId(Long id) {
        this.id = id;
        return this;
    }

    public ProjectPoint setPoint(Point point) {
        this.point = point;
        return this;
    }

    public ProjectPoint setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    @Override
    public String toString() {
        return "ProjectPoint{" +
                "id=" + id +
                ", point=" + point +
                ", room=" + room +
                ", quantity=" + quantity +
                '}';
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
