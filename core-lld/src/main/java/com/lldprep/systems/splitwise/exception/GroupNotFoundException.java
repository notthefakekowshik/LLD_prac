package com.lldprep.systems.splitwise.exception;

public class GroupNotFoundException extends SplitwiseException {
    public GroupNotFoundException(String groupId) {
        super("Group not found: " + groupId);
    }
}
