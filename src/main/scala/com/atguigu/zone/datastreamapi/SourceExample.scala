package com.atguigu.zone.datastreamapi

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.scala._

object SourceExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val stream = env.addSource(new SensorSource)

        stream
            .print()

        env.execute()

    }

}
