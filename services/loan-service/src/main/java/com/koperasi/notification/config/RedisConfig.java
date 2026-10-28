package com.koperasi.notification.config;

import com.koperasi.notification.service.NotificationRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final NotificationProperties notificationProperties;

    @Bean
    public RedisMessageListenerContainer notificationRedisContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(notificationProperties.getFanoutChannel()));
        container.setRecoveryInterval(5_000L);
        return container;
    }
}
