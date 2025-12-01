package com.ghazal.ims2.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "ATTENDANCE_DAY",
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


}
