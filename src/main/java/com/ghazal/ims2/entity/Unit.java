package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


@JmixEntity
@Entity
@Table(name = "UNIT")
public class Unit extends BaseEntity {

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    @InstanceName
    private String name;

    public String getName() {
        return name;
    }

    public Unit setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
