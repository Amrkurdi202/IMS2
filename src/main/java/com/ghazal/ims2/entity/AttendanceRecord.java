package com.ghazal.ims2.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "ATTENDANCE_RECORD")
public class AttendanceRecord {

    @Id
    @JmixGeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DAY_ID")
    private AttendanceDay day;

    @Enumerated(EnumType.STRING) // 👈 this tells JPA to store the enum as text
    @Column(name = "ACTION", nullable = false)
    private AttendanceAction action;

    @Column(name = "TIME")
    private LocalTime time;

    @InstanceName
    public String getDisplayName() {
        return action + " at " + time;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AttendanceDay getDay() { return day; }
    public void setDay(AttendanceDay day) { this.day = day; }

    public AttendanceAction getAction() { return action; }
    public void setAction(AttendanceAction action) { this.action = action; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }



}
