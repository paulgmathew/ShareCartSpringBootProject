package com.sharecart.sharecart.user.service;

import com.sharecart.sharecart.user.dto.UpdateUserLocationRequest;
import com.sharecart.sharecart.user.dto.UserLocationResponse;
import java.util.UUID;

public interface UserService {
    UserLocationResponse updateLocation(UUID userId, UpdateUserLocationRequest request);
    UserLocationResponse getLocation(UUID userId);
}
