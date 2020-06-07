package com.atguigu.CEP

import org.apache.flink.cep.scala.CEP
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.collection.Map

object CEPExample {

    case class LoginEvent(userId: String,
                          ip: String,
                          eventType: String,
                          eventTime: String)

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        val stream = env
            .fromElements(
                LoginEvent("1", "192.168.0.1", "fail", "1558430842"),
                LoginEvent("1", "192.168.0.2", "fail", "1558430843"),
                LoginEvent("1", "192.168.0.3", "fail", "1558430844"),
                LoginEvent("2", "192.168.10.10", "success", "1558430845")
            )
            .assignAscendingTimestamps(_.eventTime.toLong * 1000L)
            .keyBy(_.userId)

        val pattern = Pattern
            .begin[LoginEvent]("begin")
            .where(_.eventType.equals("fail"))
            .next("next")
            .where(_.eventType.equals("fail"))
            .next("end")
            .where(_.eventType.equals("fail"))
            .within(Time.seconds(10))

        val loginStream = CEP.pattern(stream, pattern)

        loginStream
            .select((pattern: Map[String, Iterable[LoginEvent]]) => {
                val first = pattern.getOrElse("begin", null).iterator.next()
                val second = pattern.getOrElse("next", null).iterator.next()
                val third = pattern.getOrElse("end", null).iterator.next()

                (first.ip, second.ip, third.ip)
            })
            .print()

        env.execute()
    }

}
