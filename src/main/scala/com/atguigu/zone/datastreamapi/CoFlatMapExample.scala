package com.atguigu.zone.datastreamapi

import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object CoFlatMapExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment

        val stream1: DataStream[(Int, String)] = env
            .fromElements(
                (1, "first stream"),
                (2, "first stream")
            )

        val stream2: DataStream[(Int, String)] = env
            .fromElements(
                (1, "second stream"),
                (2, "second stream")
            )

        stream1
            .keyBy(_._1)
            .connect(stream2.keyBy(_._1))
            .flatMap(new MyFaltMap)
            .print()

        env.execute()
    }

    class MyFaltMap extends CoFlatMapFunction[(Int, String),
        (Int, String), String] {
        override def flatMap1(value: (Int, String), out: Collector[String]): Unit = {
            out.collect(value._2)
        }

        override def flatMap2(value: (Int, String), out: Collector[String]): Unit = {
            out.collect(value._2)
            out.collect(value._2)
        }
    }

}
