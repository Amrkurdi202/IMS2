package com.ghazal.ims2.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.List;

@JmixEntity
@Entity
@Table(name = "ROOM")
public class Room extends BaseEntity {
    @Column(nullable = false)
    @InstanceName
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @InstanceName
    private Project project;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}