package com.atlas.api.web;

import com.atlas.api.dto.response.UserResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.user.ListUsersUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final ListUsersUseCase listUsersUseCase;
    private final ApiMapper apiMapper;

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(listUsersUseCase.execute().stream().map(apiMapper::toUserResponse).toList());
    }
}
