package com.atguigu.zone.datastreamapi

import org.apache.flink.streaming.api.functions.co.CoMapFunction
import org.apache.flink.streaming.api.scala._

object ConnectExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val one: DataStream[(Int, Long)] = env
            .fromElements(
                (1, 1L),
                (2, 2L),
                (3, 3L)
            )

        val two: DataStream[(Int, String)] = env
            .fromElements(
                (1, "1"),
                (2, "2"),
                (3, "3")
            )
        val connected: ConnectedStreams[(Int, Long), (Int, String)] = one
            .keyBy(_._1)
            .connect(
                two
                    .keyBy(_._1)
            )

        val comap: DataStream[String] = connected.map(new MyCoMap1)

        comap.print()

        env.execute()
    }

    class MyCoMap1 extends CoMapFunction[(Int , Long),
        (Int , String) , String] {
        override def map1(value: (Int, Long)): String = {
            "key为" + value._1 + "的数据来自第一条流"
        }

        override def map2(value: (Int, String)): String = {
            "key为" + value._1 + "的数据来自第二条流"
        }
    }

}
