package com.sharecart.sharecart.realtime.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sharecart.sharecart.common.security.JwtUtil;
import com.sharecart.sharecart.shoppinglist.service.ListAccessService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class StompAuthChannelInterceptorIntegrationTest {

    private static final String TEST_SECRET = "12345678901234567890123456789012";

    private JwtUtil jwtUtil;
    private MutableListAccessService listAccessService;
    private ChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 60_000L);
        listAccessService = new MutableListAccessService();
        interceptor = new StompAuthChannelInterceptor(jwtUtil, listAccessService);
        channel = new ExecutorSubscribableChannel();
    }

    @Test
    void shouldAuthenticateOnConnectWhenBearerTokenIsValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "user@example.com");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);

        Message<byte[]> message = message(accessor);
        Message<?> out = interceptor.preSend(message, channel);

        StompHeaderAccessor outAccessor = StompHeaderAccessor.wrap(out);
        assertNotNull(outAccessor.getUser());
        assertEquals(userId.toString(), outAccessor.getUser().getName());
    }

    @Test
    void shouldAllowSubscribeWhenUserHasListAccess() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        listAccessService.allowed = true;
        listAccessService.expectedUserId = userId;
        listAccessService.expectedListId = listId;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> userId.toString());
        accessor.setDestination("/topic/lists/" + listId);

        Message<byte[]> message = message(accessor);
        Message<?> out = interceptor.preSend(message, channel);

        assertNotNull(out);
    }

    @Test
    void shouldRejectSubscribeWhenUserIsNotListMember() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        listAccessService.allowed = false;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> userId.toString());
        accessor.setDestination("/topic/lists/" + listId);

        Message<byte[]> message = message(accessor);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void shouldRejectSubscribeForInvalidDestinationPattern() {
        UUID userId = UUID.randomUUID();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> userId.toString());
        accessor.setDestination("/topic/other/" + UUID.randomUUID());

        Message<byte[]> message = message(accessor);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static class MutableListAccessService implements ListAccessService {

        private boolean allowed = true;
        private UUID expectedUserId;
        private UUID expectedListId;

        @Override
        public boolean canAccessList(UUID userId, UUID listId) {
            if (expectedUserId != null && !expectedUserId.equals(userId)) {
                return false;
            }
            if (expectedListId != null && !expectedListId.equals(listId)) {
                return false;
            }
            return allowed;
        }
    }
}