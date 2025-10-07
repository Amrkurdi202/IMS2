package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


import java.util.List;

@JmixEntity
@Entity
@Table(name = "ROOM")
public class Room extends BaseEntity {
    @Column(nullable = false)
    @InstanceName
    private String name;
    @OneToMany
    private List<ProjectPoint> points;

    public Room() {
    }

    public String getName() {
        return this.name;
    }

    public List<ProjectPoint> getPoints() {
        return this.points;
    }

    public Room setName(String name) {
        this.name = name;
        return this;
    }

    public Room setPoints(List<ProjectPoint> points) {
        this.points = points;
        return this;
    }

    public String toString() {
        return "Room(name=" + this.getName() + ", points=" + this.getPoints() + ")";
    }
}