package com.atguigu.tableapiandflinksql

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.{EnvironmentSettings, Tumble}
import org.apache.flink.table.api.scala._

object FlinkTableExample01 {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
        env.setParallelism(1)

        val settings = EnvironmentSettings
            .newInstance()
            .useOldPlanner()
            .inStreamingMode()
            .build()

        val tableEnv = StreamTableEnvironment.create(
            env,
            settings
        )

        val stream = env
            .addSource(new SensorSource)
            .assignAscendingTimestamps(_.timestamp)

        val table = tableEnv
            .fromDataStream(stream , 'id , 'ts.rowtime , 'temperature)
                .window(Tumble over 10.seconds on 'ts as 'tw )
                .groupBy('id , 'tw)
                .select('id , 'id.count)
        table
            .toAppendStream[(String , Long)]
            .print()

        env.execute()
    }

}
