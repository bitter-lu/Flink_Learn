package com.atguigu.zone.tableapiandflinksql

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._


object EveryTenSecOnSlideWindow {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

        val settings = EnvironmentSettings
            .newInstance()
            .useOldPlanner()
            .inStreamingMode()
            .build()

        val tableEnv = StreamTableEnvironment
            .create(env, settings)

        val stream = env
            .addSource(new SensorSource)
            .assignAscendingTimestamps(_.timestamp)

        val dataTable = tableEnv
            .fromDataStream(stream, 'id, 'ts.rowtime, 'temperature)

        tableEnv
            .sqlQuery(
                "select id, count(id) from " + dataTable +
                    " group by id, hop(ts, interval '5' second , interval '10' second)"
            )
            .toRetractStream[(String , Long)]
            .print()

        env.execute()

    }

}
