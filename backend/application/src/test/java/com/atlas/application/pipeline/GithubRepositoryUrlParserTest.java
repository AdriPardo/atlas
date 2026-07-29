package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class GithubRepositoryUrlParserTest {

    @Test
    void parsesHttps() {
        Optional<GithubRepositoryUrlParser.OwnerRepo> parsed =
                GithubRepositoryUrlParser.parse("https://github.com/AdriPardo/reelpath.git");
        assertTrue(parsed.isPresent());
        assertEquals("AdriPardo", parsed.get().owner());
        assertEquals("reelpath", parsed.get().repo());
    }

    @Test
    void parsesSsh() {
        Optional<GithubRepositoryUrlParser.OwnerRepo> parsed =
                GithubRepositoryUrlParser.parse("git@github.com:AdriPardo/reelpath.git");
        assertTrue(parsed.isPresent());
        assertEquals("AdriPardo", parsed.get().owner());
        assertEquals("reelpath", parsed.get().repo());
    }

    @Test
    void rejectsNonGithub() {
        assertTrue(GithubRepositoryUrlParser.parse("https://gitlab.com/a/b.git").isEmpty());
    }
}
