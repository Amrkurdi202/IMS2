package com.ghazal.ims2.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JmixEntity
@Entity
@Table(name = "attendance_day",
        uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID", "DAY_"}))
public class AttendanceDay {
    @Id
    @JmixGeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "DAY_")
    private LocalDate day;

    @InstanceName
    private String caption;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL)
    private List<AttendanceRecord> records;

    // Getters & Setters


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }


    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }


    public List<AttendanceRecord> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecord> records) {
        this.records = records;
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void updateCaption() {
        if (day != null && user != null) {
            caption = user.getUsername() + " - " + day.toString();
        } else if (day != null) {
            caption = day.toString();
        }
    }

    /**
     * Returns a comma-separated list of unique project names worked on this day
     */
    @Transient
    @JmixProperty
    public String getProjectsNames() {
        if (records == null || records.isEmpty()) {
            return "";
        }

        return records.stream()
                .map(AttendanceRecord::getProject)
                .filter(project -> project != null)
                .map(Project::getName)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

}
