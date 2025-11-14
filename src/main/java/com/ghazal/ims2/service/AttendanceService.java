package com.ghazal.ims2.service;

import com.ghazal.ims2.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuth;


    public void markAction(AttendanceAction action) {
        User currentUser = (User) currentAuth.getUser();
        AttendanceDay todayDay = getOrCreateToday(currentUser);

        AttendanceRecord record = dataManager.create(AttendanceRecord.class);
        record.setDay(todayDay);
        record.setAction(action);
        record.setTime(LocalTime.now());

        dataManager.save(record);
    }


    private AttendanceDay getOrCreateToday(User user) {
        LocalDate today = LocalDate.now();

        List<AttendanceDay> days = dataManager.load(AttendanceDay.class)
                .query("select d from AttendanceDay d where d.user = :user and d.day = :day")
                .parameter("user", user)
                .parameter("day", today)
                .list();

        if (days.isEmpty()) {
            AttendanceDay newDay = dataManager.create(AttendanceDay.class);
            newDay.setUser(user);
            newDay.setDay(today);
            newDay.setCaption("Attendance for " + today);
            return dataManager.save(newDay);
        }

        return days.get(0);
    }


    public List<AttendanceRecord> getTodayRecords() {
        User currentUser = (User) currentAuth.getUser();
        AttendanceDay todayDay = getOrCreateToday(currentUser);

        return dataManager.load(AttendanceRecord.class)
                .query("select r from AttendanceRecord r where r.day = :day order by r.time asc")
                .parameter("day", todayDay)
                .list();
    }
}
