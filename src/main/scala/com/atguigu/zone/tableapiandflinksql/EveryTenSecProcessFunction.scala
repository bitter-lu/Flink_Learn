package com.atguigu.zone.tableapiandflinksql

import com.atguigu.datastreamapi.{SensorReading, SensorSource}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object EveryTenSecProcessFunction {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val stream = env
            .addSource(new SensorSource)
            .keyBy(_.id)
            .timeWindow(Time.seconds(5))
            .process(new ProWin)
    }

    class ProWin extends ProcessWindowFunction [SensorReading,
        String , String , TimeWindow] {
        override def process(key: String,
                             context: Context,
                             elements: Iterable[SensorReading],
                             out: Collector[String]): Unit = {
            out.collect(elements.size + "个元素")
        }
    }

}
