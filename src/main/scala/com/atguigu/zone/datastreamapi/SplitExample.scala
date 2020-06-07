package com.atguigu.zone.datastreamapi

import org.apache.flink.streaming.api.scala._

object SplitExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment

        println(env.getParallelism)

        val inputStream: DataStream[(Int, String)] = env
            .fromElements(
                (1001, "1001"),
                (999, "999")
            )

        val splitted: SplitStream[(Int, String)] = inputStream
            .split(t => if (t._1 > 1000) Seq("large") else Seq("small"))

        val large: DataStream[(Int, String)] = splitted.select("large")
        val small: DataStream[(Int, String)] = splitted.select("small")
        val all: DataStream[(Int, String)] = splitted.select("small" , "large")

        large.print()

        env.execute()
    }

}
