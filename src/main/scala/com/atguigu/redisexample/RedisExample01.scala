package com.atguigu.redisexample

import com.atguigu.datastreamapi.{SensorReading, SensorSource}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisClusterConfig, FlinkJedisPoolConfig}
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}

object RedisExample01 {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val stream = env
            .addSource(new SensorSource)

        val conf = new FlinkJedisPoolConfig.Builder().setHost("hadoop102").build()

        stream.addSink(new RedisSink[SensorReading](conf,new RedisExampleMapper01))

        env.execute()
    }

    class RedisExampleMapper01 extends RedisMapper[SensorReading] {
        override def getCommandDescription: RedisCommandDescription = {
            new RedisCommandDescription(
                RedisCommand.HSET,
                "sersor_temperature"
            )
        }

        override def getKeyFromData(t: SensorReading): String = t.id

        override def getValueFromData(t: SensorReading): String = {
            t.temperature.toString
        }
    }

}
