package org.bootcamp.cardservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaProducerConfigTest {

    private KafkaProducerConfig kafkaProducerConfig;

    @BeforeEach
    void setUp() {
        kafkaProducerConfig = new KafkaProducerConfig();

        ReflectionTestUtils.setField(
            kafkaProducerConfig,
            "bootstrap",
            "localhost:9092"
        );
    }

    @Test
    void shouldCreateProducerPropsSuccessfully() {
        Map<String, Object> props =
            kafkaProducerConfig.producerProps();

        assertNotNull(props);

        assertEquals(
            "localhost:9092",
            props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
        );

        assertEquals(
            StringSerializer.class,
            props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
        );

        assertEquals(
            JsonSerializer.class,
            props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
        );
    }

    @Test
    void shouldCreateProducerFactorySuccessfully() {
        ProducerFactory<String, Object> producerFactory =
            kafkaProducerConfig.producerFactory();

        assertNotNull(producerFactory);

        Map<String, Object> configs =
            producerFactory.getConfigurationProperties();

        assertEquals(
            "localhost:9092",
            configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
        );

        assertEquals(
            StringSerializer.class,
            configs.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
        );

        assertEquals(
            JsonSerializer.class,
            configs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
        );
    }

    @Test
    void shouldCreateKafkaTemplateSuccessfully() {
        KafkaTemplate<String, Object> kafkaTemplate =
            kafkaProducerConfig.kafkaTemplate();

        assertNotNull(kafkaTemplate);
        assertNotNull(kafkaTemplate.getProducerFactory());
    }
}