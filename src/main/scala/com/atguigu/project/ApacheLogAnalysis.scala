package com.atguigu.project

import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.state.ListStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

object ApacheLogAnalysis {

    case class ApacheLogEvent(
                                 ip: String,
                                 userId: String,
                                 eventTime: Long,
                                 method: String,
                                 url: String
                             )

    case class UrlViewCount(
                               url: String,
                               windowEnd: Long,
                               count: Long
                           )

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
        env.setParallelism(1)

        val stream = env
            .readTextFile("F:\\IDEA\\IdeaProjects\\Flink1125SH\\src\\main\\resources\\apachelog.txt")
            .map(line => {
                val linearray = line.split(" ")
                val simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss")
                val timestamp = simpleDateFormat.parse(linearray(3)).getTime
                ApacheLogEvent(
                    linearray(0),
                    linearray(2),
                    timestamp,
                    linearray(5),
                    linearray(6)
                )
            })
            .assignTimestampsAndWatermarks(
                new BoundedOutOfOrdernessTimestampExtractor[ApacheLogEvent](
                    Time.milliseconds(1000)
                ) {
                    override def extractTimestamp(element: ApacheLogEvent): Long = {
                        element.eventTime
                    }
                }
            )
            .keyBy(_.url)
            .timeWindow(Time.minutes(10), Time.seconds(5))
            .aggregate(new CountAgg(), new WindowResultFunction())
            .keyBy(_.windowEnd)
            .process(new TopNHotUrls(5))
            .print()

        env.execute("Traffic Analysis Job")
    }

    class CountAgg() extends AggregateFunction[ApacheLogEvent, Long, Long] {
        override def createAccumulator(): Long = 0L

        override def add(value: ApacheLogEvent, accumulator: Long): Long = accumulator + 1

        override def getResult(accumulator: Long): Long = accumulator

        override def merge(a: Long, b: Long): Long = a + b
    }

    class WindowResultFunction extends ProcessWindowFunction[Long, UrlViewCount, String, TimeWindow] {
        override def process(key: String,
                             context: Context,
                             elements: Iterable[Long],
                             out: Collector[UrlViewCount]): Unit = {
            out.collect(UrlViewCount(key, context.window.getEnd, elements.iterator.next()))
        }
    }

    class TopNHotUrls(topSize: Int) extends KeyedProcessFunction[Long,UrlViewCount,String] {
        lazy val urlState = getRuntimeContext.getListState(
            new ListStateDescriptor[UrlViewCount](
                "urlState-state",
                Types.of[UrlViewCount]
            )
        )

        override def processElement(value: UrlViewCount,
                                    ctx: KeyedProcessFunction[Long, UrlViewCount, String]#Context,
                                    out: Collector[String]): Unit = {
            urlState.add(value)
            ctx.timerService().registerEventTimeTimer(value.windowEnd + 1)
        }

        override def onTimer(timestamp: Long,
                             ctx: KeyedProcessFunction[Long, UrlViewCount, String]#OnTimerContext,
                             out: Collector[String]): Unit = {
            val allUrlViews : ListBuffer[UrlViewCount] = ListBuffer()
            import scala.collection.JavaConversions._
            for (urlView <- urlState.get) {
                allUrlViews += urlView
            }

            urlState.clear()

            val sortedUrlViews = allUrlViews.sortBy(_.count)(Ordering.Long.reverse)
                .take(topSize)
            val result : StringBuilder = new StringBuilder
            result
                .append("==============================================\n")
                .append(" 时间： ")
                .append(new Timestamp(timestamp - 1))
                .append("\n")
            for ( i <- sortedUrlViews.indices) {
                val currentUrlView : UrlViewCount = sortedUrlViews(i)
                result
                    .append("No")
                    .append(i + 1)
                    .append(": ")
                    .append("  URL=")
                    .append(currentUrlView.url)
                    .append("  流量=")
                    .append(currentUrlView.count)
                    .append("\n")
            }
            result
                .append("==============================================\n\n")
            // 控制输出频率，模拟实时滚动结果
            Thread.sleep(1000)
            out.collect(result.toString)
        }
    }

}
