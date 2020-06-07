package com.atguigu.project

import java.sql.Timestamp

import com.atguigu.project.AppMarketingByChannel.SimulatedEventSource
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object AppMarketingStatistics {
    def main(args: Array[String]): Unit = {
        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
        env.setParallelism(1)

        val stream = env
            .addSource(new SimulatedEventSource)
            .assignAscendingTimestamps(_.ts)
            .filter(_.behavior != "UNINSTALL")
            .map(r => ("dummy" , 1L))
            .keyBy(_._1)
            .timeWindow(Time.seconds(5),Time.seconds(1))
            .process(new MarketingCountTotal)
        stream.print()
        env.execute()
    }

    class MarketingCountTotal extends ProcessWindowFunction
        [(String, Long), (String, Long, Timestamp), String, TimeWindow]{
        override def process(key: String,
                             context: Context,
                             elements: Iterable[(String, Long)],
                             out: Collector[(String, Long, Timestamp)]): Unit = {
            out.collect((key, elements.size, new Timestamp(context.window.getEnd)))
        }
    }

}
