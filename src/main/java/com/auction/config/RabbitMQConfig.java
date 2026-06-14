package com.auction.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * <p>
 * 配置队列、交换机和绑定关系，支持出价事件的异步处理
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 出价事件 ====================

    /**
     * 出价事件队列（持久化）
     */
    public static final String BID_EVENT_QUEUE = "auction.bid.event.queue";

    /**
     * 出价事件交换机（Topic类型）
     */
    public static final String BID_EVENT_EXCHANGE = "auction.bid.exchange";

    /**
     * 出价事件路由键
     */
    public static final String BID_EVENT_ROUTING_KEY = "auction.bid.place";

    // ==================== WebSocket 广播队列 ====================

    /**
     * WebSocket 广播队列（非持久化，仅内存）
     */
    public static final String WEBSOCKET_BROADCAST_QUEUE = "auction.websocket.broadcast.queue";

    /**
     * WebSocket 交换机（Fanout类型）
     */
    public static final String WEBSOCKET_EXCHANGE = "auction.websocket.exchange";

    // ==================== 订单生成队列 ====================

    /**
     * 订单生成队列（持久化）
     */
    public static final String ORDER_CREATE_QUEUE = "auction.order.create.queue";

    /**
     * 订单交换机（Direct类型）
     */
    public static final String ORDER_EXCHANGE = "auction.order.exchange";

    /**
     * 订单路由键
     */
    public static final String ORDER_ROUTING_KEY = "auction.order.create";

    // ==================== Bean 定义 ====================

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate（使用JSON转换器）
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // ==================== 出价事件队列 ====================

    @Bean
    public Queue bidEventQueue() {
        return QueueBuilder.durable(BID_EVENT_QUEUE)
                .withArgument("x-max-length", 10000)  // 最大 10 万条消息
                .withArgument("x-max-priority", 10)    // 支持优先级
                .build();
    }

    @Bean
    public TopicExchange bidEventExchange() {
        return ExchangeBuilder.topicExchange(BID_EVENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Binding bidEventBinding() {
        return BindingBuilder.bind(bidEventQueue())
                .to(bidEventExchange())
                .with(BID_EVENT_ROUTING_KEY);
    }

    // ==================== WebSocket 广播队列 ====================

    @Bean
    public Queue websocketBroadcastQueue() {
        return QueueBuilder.nonDurable(WEBSOCKET_BROADCAST_QUEUE)
                .autoDelete()
                .build();
    }

    @Bean
    public FanoutExchange websocketExchange() {
        return ExchangeBuilder.fanoutExchange(WEBSOCKET_EXCHANGE)
                .durable(false)
                .autoDelete()
                .build();
    }

    @Bean
    public Binding websocketBinding() {
        return BindingBuilder.bind(websocketBroadcastQueue())
                .to(websocketExchange());
    }

    // ==================== 订单生成队列 ====================

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", "auction.dlx")  // 死信交换机
                .withArgument("x-dead-letter-routing-key", "order.failed")
                .build();
    }

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderCreateQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }
}
