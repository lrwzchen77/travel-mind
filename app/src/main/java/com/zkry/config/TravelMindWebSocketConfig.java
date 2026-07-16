package com.zkry.config;

import cn.dev33.satoken.stp.StpUtil;
import com.zkry.api.trip.TripTaskWebSocketHandler;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class TravelMindWebSocketConfig implements WebSocketConfigurer {

    private final TripTaskWebSocketHandler tripTaskWebSocketHandler;

    public TravelMindWebSocketConfig(TripTaskWebSocketHandler tripTaskWebSocketHandler) {
        this.tripTaskWebSocketHandler = tripTaskWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tripTaskWebSocketHandler, "/api/user/trip/ws/{taskId}")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler handler, Map<String, Object> attributes) {
                    StpUtil.checkRole("user");
                    attributes.put(TripTaskWebSocketHandler.USER_ID_ATTRIBUTE, StpUtil.getLoginIdAsLong());
                    return true;
                }

                @Override
                public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler handler, Exception exception) {
                }
            })
            .setAllowedOriginPatterns("*");
    }
}
