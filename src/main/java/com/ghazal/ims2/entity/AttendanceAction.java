package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum AttendanceAction implements EnumClass<String> {
    IN("IN"),
    OUT("OUT"),
    BREAK("BREAK"),
    BREAK_END("BREAK_END");

    private final String id;

    AttendanceAction(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id; //
    }

    public static AttendanceAction fromId(String id) {
        for (AttendanceAction action : AttendanceAction.values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }
}
