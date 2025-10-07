package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@JmixEntity
@Entity
public class Point extends BaseEntity {
    @InstanceName
    @Column(nullable = false)
    private String name;
    private byte[] image;
    private BigDecimal factor;

    public Point() {
    }

    public String getName() {
        return this.name;
    }

    public byte[] getImage() {
        return this.image;
    }

    public BigDecimal getFactor() {
        return this.factor;
    }

    public Point setName(String name) {
        this.name = name;
        return this;
    }

    public Point setImage(byte[] image) {
        this.image = image;
        return this;
    }

    public Point setFactor(BigDecimal factor) {
        this.factor = factor;
        return this;
    }

    public String toString() {
        return "Point(name=" + this.getName() + ", image=" + java.util.Arrays.toString(this.getImage()) + ", factor=" + this.getFactor() + ")";
    }
}
