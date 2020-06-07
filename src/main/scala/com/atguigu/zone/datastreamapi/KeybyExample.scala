package com.atguigu.zone.datastreamapi

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.scala._

object KeybyExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val keyed = env
            .addSource(new SensorSource)
            .keyBy(_.id)

        val min = keyed.min("temperature")

        min.print()

        env.execute()
    }

}
