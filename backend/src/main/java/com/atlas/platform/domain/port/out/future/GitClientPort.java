package com.atlas.platform.domain.port.out.future;

/**
 * Future port for Git clone/pull operations. Not used by the MVP.
 */
public interface GitClientPort {

    void syncRepository(String repositoryUrl, String branch, String targetPath);
}
