package com.sharecart.sharecart.realtime.security;

import com.sharecart.sharecart.common.security.JwtUtil;
import com.sharecart.sharecart.shoppinglist.service.ListAccessService;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern LIST_TOPIC_PATTERN = Pattern.compile("^/topic/lists/([0-9a-fA-F-]{36})$");

    private final JwtUtil jwtUtil;
    private final ListAccessService listAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
            return message;
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header for STOMP CONNECT");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new AccessDeniedException("Invalid JWT token for STOMP CONNECT");
        }

        String userId = jwtUtil.extractUserId(token);
        accessor.setUser(userPrincipal(userId));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null) {
            throw new AccessDeniedException("Unauthenticated STOMP subscription");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Subscription destination is required");
        }

        Matcher matcher = LIST_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Subscription destination is not allowed");
        }

        UUID listId;
        UUID userId;
        try {
            listId = UUID.fromString(matcher.group(1));
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid subscription identifiers");
        }

        if (!listAccessService.canAccessList(userId, listId)) {
            throw new AccessDeniedException("User is not a member of the list");
        }
    }

    private Principal userPrincipal(String userId) {
        return () -> userId;
    }
}