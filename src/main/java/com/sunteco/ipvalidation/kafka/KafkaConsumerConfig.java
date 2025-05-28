/*
 * Copyright (C) 2021 Sunteco, Inc.
 *
 * Sunteco License Notice
 *
 * The contents of this file are subject to the Sunteco License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. The Initial Developer of the Original
 * Code is Sunteco, JSC. Portions Copyright 2021 Sunteco JSC
 *
 * All Rights Reserved.
 */

package com.sunteco.ipvalidation.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    String hostName = "";
    @Value("${system.kafka.endpoint}")
    private String SYSTEM_KAFKA_ENDPOINT;
    @Value("${system.env}")
    private String systemEnvironment;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        try {
            InetAddress address = InetAddress.getLocalHost();
            hostName = address.getHostName();
            System.out.println("Hostname: " + address.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SYSTEM_KAFKA_ENDPOINT);
        if (systemEnvironment.equals("local") || systemEnvironment.equals("dang")) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumerGroup_" + systemEnvironment + "_" + hostName);
        } else {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumerGroup_" + systemEnvironment);
        }
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "client342");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "160000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "150000");

        if (!SYSTEM_KAFKA_ENDPOINT.contains("9092")) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/security/tls/keystore-system.jks");
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "password");
        }

//
//        security.protocol=SASL_SSL
//        sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule
//
//        username="super-user"
//        password="rSRciPnunG06";
//        sasl.mechanism=SCRAM-SHA-512

//        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
//        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
//        props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"super-user\" password=\"rSRciPnunG06\";");
//        props.put("sasl.mechanism", "SCRAM-SHA-512");

//        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/security/tls/keystore0.jks");
//        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "password");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String>
            factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
