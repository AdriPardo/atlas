package com.atlas.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas")
public class AtlasProperties {

    private final Worker worker = new Worker();
    private final Workspace workspace = new Workspace();
    private final Docker docker = new Docker();
    private final Secrets secrets = new Secrets();
    private final Adapters adapters = new Adapters();
    private final Observability observability = new Observability();
    private final Retention retention = new Retention();
    private final Backup backup = new Backup();
    private final Networking networking = new Networking();

    public Worker getWorker() {
        return worker;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Docker getDocker() {
        return docker;
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public Adapters getAdapters() {
        return adapters;
    }

    public Observability getObservability() {
        return observability;
    }

    public Retention getRetention() {
        return retention;
    }

    public Backup getBackup() {
        return backup;
    }

    public Networking getNetworking() {
        return networking;
    }

    public static class Worker {
        private boolean enabled = true;
        private long pollIntervalMs = 2000;
        private int batchSize = 5;
        private String id = "atlas-worker";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Workspace {
        private String dir = "/var/lib/atlas/workspaces";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public static class Docker {
        private String host = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
    }

    public static class Secrets {
        private String masterKey = "";

        public String getMasterKey() {
            return masterKey;
        }

        public void setMasterKey(String masterKey) {
            this.masterKey = masterKey;
        }
    }

    public static class Adapters {
        /**
         * When false, Unsupported* stubs are used (useful for API-only tests without Docker/Git).
         */
        private boolean realEnabled = true;

        public boolean isRealEnabled() {
            return realEnabled;
        }

        public void setRealEnabled(boolean realEnabled) {
            this.realEnabled = realEnabled;
        }
    }

    public static class Observability {
        private String grafanaBaseUrl = "";
        private String lokiBaseUrl = "";

        public String getGrafanaBaseUrl() {
            return grafanaBaseUrl;
        }

        public void setGrafanaBaseUrl(String grafanaBaseUrl) {
            this.grafanaBaseUrl = grafanaBaseUrl;
        }

        public String getLokiBaseUrl() {
            return lokiBaseUrl;
        }

        public void setLokiBaseUrl(String lokiBaseUrl) {
            this.lokiBaseUrl = lokiBaseUrl;
        }
    }

    public static class Retention {
        private boolean enabled = true;
        private int jobsDays = 30;
        private int pipelineRunsDays = 30;
        private String cron = "0 0 3 * * *";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getJobsDays() {
            return jobsDays;
        }

        public void setJobsDays(int jobsDays) {
            this.jobsDays = jobsDays;
        }

        public int getPipelineRunsDays() {
            return pipelineRunsDays;
        }

        public void setPipelineRunsDays(int pipelineRunsDays) {
            this.pipelineRunsDays = pipelineRunsDays;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Backup {
        private boolean enabled = true;
        private String dir = "/var/lib/atlas/backups";
        private int keepCount = 7;
        private String cron = "0 30 2 * * *";
        private String pgDumpBinary = "pg_dump";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }

        public int getKeepCount() {
            return keepCount;
        }

        public void setKeepCount(int keepCount) {
            this.keepCount = keepCount;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getPgDumpBinary() {
            return pgDumpBinary;
        }

        public void setPgDumpBinary(String pgDumpBinary) {
            this.pgDumpBinary = pgDumpBinary;
        }
    }

    public static class Networking {
        private String traefikCertResolver = "letsencrypt";
        private int traefikBackendPort = 80;

        public String getTraefikCertResolver() {
            return traefikCertResolver;
        }

        public void setTraefikCertResolver(String traefikCertResolver) {
            this.traefikCertResolver = traefikCertResolver;
        }

        public int getTraefikBackendPort() {
            return traefikBackendPort;
        }

        public void setTraefikBackendPort(int traefikBackendPort) {
            this.traefikBackendPort = traefikBackendPort;
        }
    }
}
