package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Table(name = "Item")
@JmixEntity
@Entity
public class Item extends BaseEntity {
    @InstanceName
    @Column(nullable = false)
    private String name;
    @Column
    private byte[] image;

    public Item() {
    }

    public String getName() {
        return this.name;
    }

    public byte[] getImage() {
        return this.image;
    }

    public Item setName(String name) {
        this.name = name;
        return this;
    }

    public Item setImage(byte[] image) {
        this.image = image;
        return this;
    }

    public String toString() {
        return "Item(name=" + this.getName() + ", image=" + java.util.Arrays.toString(this.getImage()) + ")";
    }
}
