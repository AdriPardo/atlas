package com.atlas.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.PasswordEncoderPort;
import com.atlas.application.port.out.TokenProviderPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.domain.user.Role;
import com.atlas.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateFromAuthentikUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenProviderPort tokenProvider;

    private AuthenticateFromAuthentikUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateFromAuthentikUseCase(
                userRepository, passwordEncoder, tokenProvider, true, "Atlas Admins");
    }

    @Test
    void provisionsAdminFromAtlasAdminsGroup() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
        when(tokenProvider.getExpirationSeconds()).thenReturn(3600L);

        AuthenticationResult result = useCase.execute(new AuthenticateFromAuthentikUseCase.AuthentikIdentity(
                "alice", "authentik Admins|Atlas Admins", "alice@example.com", "Alice", "uid-1"));

        assertEquals("jwt-token", result.accessToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
        assertEquals("alice", captor.getValue().getUsername());
    }

    @Test
    void mapsOperatorWhenNoAdminGroup() {
        User existing = User.rehydrate(UUID.randomUUID(), "bob", "hash", Role.OPERATOR);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(existing));
        when(tokenProvider.generateToken(existing)).thenReturn("jwt");
        when(tokenProvider.getExpirationSeconds()).thenReturn(3600L);

        AuthenticationResult result = useCase.execute(new AuthenticateFromAuthentikUseCase.AuthentikIdentity(
                "bob", "operators|viewers", null, null, null));

        assertEquals("jwt", result.accessToken());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradesExistingUserToAdmin() {
        User existing = User.rehydrate(UUID.randomUUID(), "carol", "hash", Role.OPERATOR);
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt");
        when(tokenProvider.getExpirationSeconds()).thenReturn(3600L);

        useCase.execute(new AuthenticateFromAuthentikUseCase.AuthentikIdentity(
                "carol", "Atlas Admins", null, null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void rejectsWhenDisabled() {
        useCase = new AuthenticateFromAuthentikUseCase(
                userRepository, passwordEncoder, tokenProvider, false, "Atlas Admins");

        assertThrows(
                UnauthorizedException.class,
                () -> useCase.execute(new AuthenticateFromAuthentikUseCase.AuthentikIdentity(
                        "alice", "Atlas Admins", null, null, null)));
    }

    @Test
    void rejectsMissingUsername() {
        assertThrows(
                UnauthorizedException.class,
                () -> useCase.execute(new AuthenticateFromAuthentikUseCase.AuthentikIdentity(
                        "  ", "Atlas Admins", null, null, null)));
    }

    @Test
    void parseGroupsSupportsPipeAndComma() {
        assertEquals(2, AuthenticateFromAuthentikUseCase.parseGroups("a|b").size());
        assertEquals(2, AuthenticateFromAuthentikUseCase.parseGroups("a,b").size());
    }
}
