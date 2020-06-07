package com.atguigu.datastreamapi

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object PeriodicWatermarkExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
        env.setParallelism(1)

        val stream = env
            .addSource(new SensorSource)
            .assignTimestampsAndWatermarks(new MyAssigner)
            .keyBy(_.id)
            .timeWindow(Time.seconds(5))
            .process(new Myprocess)
            .print()

        env.execute()
    }

    class MyAssigner extends AssignerWithPeriodicWatermarks[SensorReading] {
        val bound : Long = 0L
        var maxTs : Long = Long.MinValue + bound

        override def extractTimestamp(element: SensorReading, previousElementTimestamp: Long): Long = {
            maxTs = maxTs.max(element.timestamp)
            element.timestamp
        }

        override def getCurrentWatermark: Watermark = {
            new Watermark(maxTs - bound)
        }
    }

    class Myprocess extends ProcessWindowFunction[SensorReading , String , String , TimeWindow] {
        override def process(key: String,
                             context: Context,
                             elements: Iterable[SensorReading],
                             out: Collector[String]): Unit = {
            out.collect("hello word")
        }
    }

}
