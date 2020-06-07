package com.atguigu.zone.datastreamapi

import com.atguigu.datastreamapi.{SensorReading, SensorSource}
import org.apache.flink.api.common.functions.FilterFunction
import org.apache.flink.streaming.api.scala._

object FilterExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val stream = env
            .addSource(new SensorSource)
            .filter(new MyFilterFunction1)
            .print()

        env.execute()
    }

    class MyFilterFunction1 extends FilterFunction[SensorReading] {
        override def filter(value: SensorReading): Boolean = value.id == "sensor_6"
    }

}
