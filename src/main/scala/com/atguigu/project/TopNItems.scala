package com.atguigu.project

import java.sql.Timestamp

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.state.ListStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

object TopNItems {

    case class UserBehaviror(
                            userId : Long,
                            itemId : Long,
                            categoryId : Int,
                            behaviror: String,
                            timestamp : Long
                            )

    case class ItemViewCount(
                            itemId : Long,
                            windowEnd : Long,
                            count : Long
                            )

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        val stream = env
            .readTextFile("F:\\IDEA\\IdeaProjects\\Flink1125SH\\src\\main\\resources\\UserBehavior.csv")
            .map(line => {
                val arr = line.split(",")
                UserBehaviror(arr(0).toLong , arr(1).toLong , arr(2).toInt , arr(3) , arr(4).toLong * 1000)
            })
            .filter(_.behaviror.equals("pv"))
            .assignAscendingTimestamps(_.timestamp)
            .keyBy(_.itemId)
            .timeWindow(Time.hours(1) , Time.minutes(5))
            .aggregate(new CountAgg , new WindowResult)
            .keyBy(_.windowEnd)
            .process(new TopN(3))

        stream.print()

        env.execute()

    }

    class CountAgg extends AggregateFunction[UserBehaviror , Long , Long] {
        override def createAccumulator(): Long = 0L

        override def add(value: UserBehaviror, accumulator: Long): Long = accumulator + 1

        override def getResult(accumulator: Long): Long = accumulator

        override def merge(a: Long, b: Long): Long = a + b
    }

    class WindowResult extends ProcessWindowFunction[Long , ItemViewCount , Long , TimeWindow] {
        override def process(key: Long,
                             context: Context,
                             elements: Iterable[Long],
                             out: Collector[ItemViewCount]): Unit = {
            out.collect(ItemViewCount(key , context.window.getEnd , elements.head))
        }
    }

    class TopN(val n: Int ) extends KeyedProcessFunction[Long,ItemViewCount,String] {
        lazy val itemList = getRuntimeContext.getListState(
            new ListStateDescriptor[ItemViewCount]("item-list" , Types.of[ItemViewCount])
        )

        override def processElement(value: ItemViewCount,
                                    ctx: KeyedProcessFunction[Long, ItemViewCount, String]#Context,
                                    out: Collector[String]): Unit = {
            itemList.add(value)
            ctx.timerService().registerEventTimeTimer(value.windowEnd + 100)
        }

        override def onTimer(timestamp: Long,
                             ctx: KeyedProcessFunction[Long, ItemViewCount, String]#OnTimerContext,
                             out: Collector[String]): Unit = {
            val allItems : ListBuffer[ItemViewCount] = ListBuffer()
            import scala.collection.JavaConversions._
            for (item <- itemList.get) {
                allItems += item
            }
            itemList.clear()

            val sortedItems = allItems
                .sortBy(-_.count)
                .take(n)

            val result = new StringBuilder
            result
                .append("========================================\n")
                .append("时间 :  ")
                .append(new Timestamp(timestamp - 100))
                .append("\n")

            for (i <- sortedItems.indices) {
                val cur = sortedItems(i)
                result
                    .append("No")
                    .append(i + 1)
                    .append(": 商品ID = ")
                    .append(cur.itemId)
                    .append(" 浏览量 = ")
                    .append(cur.count)
                    .append("\n")
            }

            result.append("========================================\n\n\n")


            Thread.sleep(1000)
            out.collect(result.toString())
        }
    }

}
