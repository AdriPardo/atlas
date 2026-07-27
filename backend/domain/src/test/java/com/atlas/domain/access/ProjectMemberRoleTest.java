package com.atlas.domain.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectMemberRoleTest {

    @Test
    void viewerCanOnlyRead() {
        assertTrue(ProjectMemberRole.VIEWER.allows(ProjectPermission.READ));
        assertFalse(ProjectMemberRole.VIEWER.allows(ProjectPermission.WRITE));
        assertFalse(ProjectMemberRole.VIEWER.allows(ProjectPermission.DEPLOY));
        assertFalse(ProjectMemberRole.VIEWER.allows(ProjectPermission.MANAGE_MEMBERS));
    }

    @Test
    void developerCanWriteButNotDeployOrManage() {
        assertTrue(ProjectMemberRole.DEVELOPER.allows(ProjectPermission.READ));
        assertTrue(ProjectMemberRole.DEVELOPER.allows(ProjectPermission.WRITE));
        assertFalse(ProjectMemberRole.DEVELOPER.allows(ProjectPermission.DEPLOY));
        assertFalse(ProjectMemberRole.DEVELOPER.allows(ProjectPermission.MANAGE_MEMBERS));
    }

    @Test
    void operatorHasFullProjectPermissions() {
        assertTrue(ProjectMemberRole.OPERATOR.allows(ProjectPermission.READ));
        assertTrue(ProjectMemberRole.OPERATOR.allows(ProjectPermission.WRITE));
        assertTrue(ProjectMemberRole.OPERATOR.allows(ProjectPermission.DEPLOY));
        assertTrue(ProjectMemberRole.OPERATOR.allows(ProjectPermission.MANAGE_MEMBERS));
    }
}
