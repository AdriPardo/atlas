package com.atlas.domain.access;

public enum ProjectMemberRole {
    VIEWER,
    DEVELOPER,
    OPERATOR;

    public boolean allows(ProjectPermission permission) {
        return switch (permission) {
            case READ -> true;
            case WRITE -> this == DEVELOPER || this == OPERATOR;
            case DEPLOY -> this == OPERATOR;
            case MANAGE_MEMBERS -> this == OPERATOR;
        };
    }
}
