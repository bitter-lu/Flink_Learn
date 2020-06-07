package com.atguigu

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object WordCount {

    case class WordWithCount(word: String, count: Int)

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        //设置并行度为1
        env.setParallelism(1)

        //定义DAG
        val text: DataStream[String] = env
            .socketTextStream("localhost", 9999, '\n')
        val windowCount: DataStream[WordWithCount] = text
            .flatMap(w => w.split(" "))
            .map(w => WordWithCount(w, 1))
            .keyBy("word")
            //.timeWindow(Time.seconds(5))
            .sum("count")

        windowCount
            .print()

        env.execute()
    }
}

