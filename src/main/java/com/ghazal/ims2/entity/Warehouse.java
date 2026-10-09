package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@JmixEntity
@Entity
@Table(name = "warehouse")
public class Warehouse extends BaseEntity {

    @NotNull
    @Column(nullable = false)
    @InstanceName
    private String name;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] image;

    @NotNull
    @Column(name = "internal_", nullable = false)
    private Boolean internal = Boolean.FALSE;   // default = External

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public String getName() { return name; }
    public byte[] getImage() { return image; }
    public Boolean getInternal() { return internal; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }

    public Warehouse setName(String name) { this.name = name; return this; }
    public Warehouse setImage(byte[] image) { this.image = image; return this; }
    public Warehouse setInternal(Boolean internal) { this.internal = internal; return this; }
    public Warehouse setLatitude(Double latitude) { this.latitude = latitude; return this; }
    public Warehouse setLongitude(Double longitude) { this.longitude = longitude; return this; }
}
