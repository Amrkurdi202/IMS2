package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@JmixEntity
@Entity
@Table(name = "WAREHOUSE")
public class Warehouse extends BaseEntity {
    @Column(nullable = false)
    @InstanceName
    private String name;
    @Lob
    private byte[] image;

    public Warehouse() {
    }

    public String getName() {
        return this.name;
    }

    public byte[] getImage() {
        return this.image;
    }

    public Warehouse setName(String name) {
        this.name = name;
        return this;
    }

    public Warehouse setImage(byte[] image) {
        this.image = image;
        return this;
    }

    public String toString() {
        return "Warehouse(name=" + this.getName() + ", image=" + java.util.Arrays.toString(this.getImage()) + ")";
    }
}