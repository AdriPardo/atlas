package com.atlas.application.port.out;

public interface BackupPolicyPort {

    boolean enabled();

    String directory();

    int keepCount();

    String cron();
}
