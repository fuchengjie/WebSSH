package cn.objectspace.webssh.config;

import cn.objectspace.webssh.interceptor.WebSocketInterceptor;
import cn.objectspace.webssh.websocket.WebSSHWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @Description: websocket配置
* @Author: NoCortY
* @Date: 2020/3/8
*/
@Configuration
@EnableWebSocket
public class WebSSHWebSocketConfig implements WebSocketConfigurer{
    private static final Logger logger = LoggerFactory.getLogger(WebSSHWebSocketConfig.class);

    @Resource
    WebSSHWebSocketHandler webSSHWebSocketHandler;

    /**
     * WebSocket 允许的来源列表，通过 application.yml 的 webssh.allowed-origins 配置。
     * 留空时回退为允许任意源（仅适合内网调试，公网部署请配置明确的来源）。
     */
    @Value("${webssh.allowed-origins:}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        //socket通道
        //指定处理器和路径
        WebSocketHandlerRegistration registration = webSocketHandlerRegistry
                .addHandler(webSSHWebSocketHandler, "/webssh")
                .addInterceptors(new WebSocketInterceptor());

        // 过滤空白项，避免配置失误产生空串来源
        List<String> origins = allowedOrigins == null ? Collections.emptyList()
                : allowedOrigins.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toList());

        if (origins.isEmpty()) {
            logger.warn("webssh.allowed-origins 未配置，WebSocket 允许任意源访问，公网部署请配置明确的来源");
            registration.setAllowedOrigins("*");
        } else {
            registration.setAllowedOrigins(origins.toArray(new String[0]));
        }
    }
}
