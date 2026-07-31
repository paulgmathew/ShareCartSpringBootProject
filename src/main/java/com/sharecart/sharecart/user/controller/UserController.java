package com.sharecart.sharecart.user.controller;

import com.sharecart.sharecart.user.dto.UpdateUserLocationRequest;
import com.sharecart.sharecart.user.dto.UserLocationResponse;
import com.sharecart.sharecart.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me/location")
    public ResponseEntity<UserLocationResponse> updateLocation(@Valid @RequestBody UpdateUserLocationRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(userService.updateLocation(userId, request));
    }

    @GetMapping("/me/location")
    public ResponseEntity<UserLocationResponse> getLocation() {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(userService.getLocation(userId));
    }
}
