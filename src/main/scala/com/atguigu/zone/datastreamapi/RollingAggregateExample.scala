package com.atguigu.zone.datastreamapi

import org.apache.flink.streaming.api.scala._

object RollingAggregateExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val inputStream = env
            .fromElements(
                (1, 2, 2), (2, 3, 1), (2, 2, 4), (1, 5, 3)
            )

        val resultStream: DataStream[(Int, Int, Int)] = inputStream
            .keyBy(0)
            .sum(1)
        resultStream.print()

        env.execute()
    }

}
