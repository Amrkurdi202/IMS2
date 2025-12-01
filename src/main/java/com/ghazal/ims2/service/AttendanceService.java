package com.ghazal.ims2.service;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceDay;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.entity.User;
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

    /**
     * Main entry used by AttendanceView buttons.
     * Enforces all business rules:
     *
     * 1) IN  allowed only when #IN == #OUT
     * 2) OUT allowed only when #IN != #OUT
     * 3) Break actions allowed only when OUT is allowed (i.e. #IN != #OUT)
     * 4) BREAK  allowed only when #BREAK == #BREAK_END  (no active break)
     * 5) BREAK_END allowed only when #BREAK != #BREAK_END (there is active break)
     *
     * If a rule is violated it throws IllegalStateException with a message
     * which AttendanceView shows as a notification.
     */
    public void markAction(AttendanceAction action) {
        User currentUser = (User) currentAuth.getUser();
        AttendanceDay todayDay = getOrCreateToday(currentUser);

        // Load today's records once
        List<AttendanceRecord> todayRecords = dataManager.load(AttendanceRecord.class)
                .query("select r from AttendanceRecord r where r.day = :day order by r.time asc")
                .parameter("day", todayDay)
                .list();

        long inCount        = todayRecords.stream().filter(r -> r.getAction() == AttendanceAction.IN).count();
        long outCount       = todayRecords.stream().filter(r -> r.getAction() == AttendanceAction.OUT).count();
        long breakStartCnt  = todayRecords.stream().filter(r -> r.getAction() == AttendanceAction.BREAK).count();
        long breakEndCnt    = todayRecords.stream().filter(r -> r.getAction() == AttendanceAction.BREAK_END).count();

        boolean inAllowedNow        = (inCount == outCount);
        boolean outAllowedNow       = (inCount != outCount);
        boolean breakStartAllowed   = outAllowedNow && (breakStartCnt == breakEndCnt);
        boolean breakEndAllowed     = outAllowedNow && (breakStartCnt != breakEndCnt);

        switch (action) {
            case IN -> {
                if (!inAllowedNow) {
                    throw new IllegalStateException("You must check OUT before checking IN again.");
                }
            }
            case OUT -> {
                if (!outAllowedNow) {
                    throw new IllegalStateException("You are not IN, so you cannot check OUT.");
                }
            }
            case BREAK -> {
                if (!breakStartAllowed) {
                    // either not IN, or there is already an open break
                    if (!outAllowedNow) {
                        throw new IllegalStateException("You must be IN (not already OUT) to start a break.");
                    }
                    throw new IllegalStateException("You already have an active break.");
                }
            }
            case BREAK_END -> {
                if (!breakEndAllowed) {
                    // either not IN, or no active break
                    if (!outAllowedNow) {
                        throw new IllegalStateException("You must be IN to end a break.");
                    }
                    throw new IllegalStateException("There is no active break to end.");
                }
            }
        }

        // If we reached here, action is allowed → save the record
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

    /** Still useful for reports or admin views if you need it. */
    public List<AttendanceRecord> getTodayRecords() {
        User currentUser = (User) currentAuth.getUser();
        AttendanceDay todayDay = getOrCreateToday(currentUser);

        return dataManager.load(AttendanceRecord.class)
                .query("select r from AttendanceRecord r where r.day = :day order by r.time asc")
                .parameter("day", todayDay)
                .list();
    }
}
