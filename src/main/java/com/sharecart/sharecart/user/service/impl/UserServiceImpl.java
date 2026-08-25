package com.sharecart.sharecart.user.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.user.dto.UpdateUserLocationRequest;
import com.sharecart.sharecart.user.dto.UserLocationResponse;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import com.sharecart.sharecart.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserLocationResponse updateLocation(UUID userId, UpdateUserLocationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setHomeLatitude(request.latitude());
        user.setHomeLongitude(request.longitude());
        userRepository.save(user);
        return new UserLocationResponse(user.getHomeLatitude(), user.getHomeLongitude());
    }

    @Override
    @Transactional(readOnly = true)
    public UserLocationResponse getLocation(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserLocationResponse(user.getHomeLatitude(), user.getHomeLongitude());
    }
}
