package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@JmixEntity
@Entity
public class Project extends BaseEntity {
    @InstanceName
    @Column(nullable = false)
    private String name;
    private byte[] image;
    @OneToMany
    private List<Room> rooms;

    public Project() {
    }

    public String getName() {
        return this.name;
    }

    public byte[] getImage() {
        return this.image;
    }

    public List<Room> getRooms() {
        return this.rooms;
    }

    public Project setName(String name) {
        this.name = name;
        return this;
    }

    public Project setImage(byte[] image) {
        this.image = image;
        return this;
    }

    public Project setRooms(List<Room> rooms) {
        this.rooms = rooms;
        return this;
    }

    public String toString() {
        return "Project(name=" + this.getName() + ", image=" + java.util.Arrays.toString(this.getImage()) + ", rooms=" + this.getRooms() + ")";
    }
}