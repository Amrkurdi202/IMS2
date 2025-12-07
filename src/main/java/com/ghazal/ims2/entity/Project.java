package com.ghazal.ims2.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;

@JmixEntity
@Entity
@Table(name = "project")
public class Project extends BaseEntity {
    @InstanceName
    @Column(nullable = false)
    private String name;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] image;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
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