package com.atguigu.kafkaexample

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

object KafkaProducerUtil {

    def main(args: Array[String]): Unit = {

        writeToKafka("test")
    }

    def writeToKafka(topic: String): Unit = {
        val properties = new Properties()
        properties.put("bootstrap.servers" , "hadoop102:9092")
        properties.put(
            "key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer"
        )
        properties.put(
            "value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer"
        )

        val producer = new KafkaProducer[String, String](properties)
        val record = new ProducerRecord[String, String](topic, "hello world")
        producer.send(record)
        producer.close()
    }

}
