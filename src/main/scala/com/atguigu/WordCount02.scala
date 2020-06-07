package com.atguigu

import org.apache.flink.streaming.api.scala._

object WordCount02 {

    case class WordWithCount (
                                 word : String,
                                 count : Int
                             )

    def main(args: Array[String]): Unit = {

        val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val text: DataStream[String] = env
            .socketTextStream("localhost", 9999, '\n')

        val wordCount: DataStream[WordWithCount] = text
            .flatMap(w => w.split(" "))
            .map(w => WordWithCount(w, 1))
            .keyBy("word")
            .sum("count")

        wordCount
            .print()

        env.execute()
    }

}
