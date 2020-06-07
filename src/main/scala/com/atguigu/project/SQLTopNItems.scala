package com.atguigu.project

import java.sql.Timestamp

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._

object SQLTopNItems {

    case class UserBehavior(
                               userId: Long,
                               itemId: Long,
                               categoryId: Int,
                               behavior: String,
                               timestamp: Long
                           )

    case class ItemViewCount(
                                itemId: Long,
                                windowEnd: Long,
                                count: Long
                            )

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        val stream = env
            .readTextFile("F:\\IDEA\\IdeaProjects\\Flink1125SH\\src\\main\\resources\\UserBehavior.csv")
            .map(line => {
                val arr = line.split(",")
                UserBehavior(arr(0).toLong, arr(1).toLong, arr(2).toInt, arr(3), arr(4).toLong * 1000)
            })
            .filter(_.behavior.equals("pv"))
            .assignAscendingTimestamps(_.timestamp)

        val settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build()

        val tEnv = StreamTableEnvironment.create(env, settings)

        tEnv.createTemporaryView("t", stream, 'itemId, 'timestamp.rowtime as 'ts)

        tEnv
            .sqlQuery(
                """
                  |select icount , windowEnd , row_num
                  |from (
                  |         select icount,
                  |         windowEnd,
                  |         row_number() over (partition by windowEnd order by icount desc) as row_num
                  |     from
                  |         (
                  |             select count(itemId) as icount,
                  |                 hop_end(ts , interval '5' minute , interval '1' hour) as windowEnd
                  |             from t group by hop(ts, interval '5' minute, interval '1' hour) , itemId) as topn
                  |)
                  |where row_num <= 3
                """.stripMargin
            )
            .toRetractStream[(Long , Timestamp , Long)]
            .print()

        env.execute()
    }
}
