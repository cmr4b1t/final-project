package org.bootcamp.creditcardaccountservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaConsumerConfigTest {

    private KafkaConsumerConfig kafkaConsumerConfig;

    @BeforeEach
    void setUp() {
        kafkaConsumerConfig = new KafkaConsumerConfig();

        ReflectionTestUtils.setField(
            kafkaConsumerConfig,
            "bootstrap",
            "localhost:9092"
        );

        ReflectionTestUtils.setField(
            kafkaConsumerConfig,
            "groupId",
            "test-group"
        );
    }

    @Test
    void shouldCreateConsumerFactorySuccessfully() {
        ConsumerFactory<String, Object> consumerFactory =
            kafkaConsumerConfig.consumerFactory();

        assertNotNull(consumerFactory);

        Map<String, Object> configs =
            consumerFactory.getConfigurationProperties();

        assertEquals(
            "localhost:9092",
            configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)
        );

        assertEquals(
            "test-group",
            configs.get(ConsumerConfig.GROUP_ID_CONFIG)
        );

        assertEquals(
            StringDeserializer.class,
            configs.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)
        );

        assertEquals(
            StringDeserializer.class,
            configs.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG)
        );

        assertEquals(
            "earliest",
            configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)
        );
    }

    @Test
    void shouldCreateKafkaListenerContainerFactorySuccessfully() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            kafkaConsumerConfig.kafkaListenerContainerFactory();

        assertNotNull(factory);
        assertNotNull(factory.getConsumerFactory());

        assertEquals(
            ContainerProperties.AckMode.MANUAL,
            factory.getContainerProperties().getAckMode()
        );
    }
}