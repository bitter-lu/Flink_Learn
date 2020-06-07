package com.atguigu.zone.project

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011


object UniqueVisitorByBloomFilter {

    case class UserBehavior(
                           userId: Long,
                           itemId: Long,
                           categoryId: Int,
                           behavior: String,
                           timestamp: Long
                           )

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
        env.setParallelism(1)

        val properties = new Properties()
        properties.setProperty("bootstrap.servers","hadoop102:9092")
        properties.setProperty("group.id","consumer-group")
        properties.setProperty(
            "key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer"
        )
        properties.setProperty(
            "value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer"
        )
        properties.setProperty(
            "auto.offset.reset",
            "latest"
        )

        val stream = env
            .addSource(new FlinkKafkaConsumer011[String](
                "hotitems1",
                new SimpleStringSchema(),
                properties
            ))
    }

}
