package com.lldprep.systems.atm.model.enums;

public enum Denomination {
    NOTE_2000(2000),
    NOTE_500(500),
    NOTE_200(200),
    NOTE_100(100);

    private final int value;

    Denomination(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
