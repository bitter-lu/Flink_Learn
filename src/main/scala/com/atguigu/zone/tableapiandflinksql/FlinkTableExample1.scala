package com.atguigu.zone.tableapiandflinksql

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.{EnvironmentSettings, Tumble}
import org.apache.flink.table.api.scala._

object FlinkTableExample1 {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
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

        val stream = env.addSource(new SensorSource)

        val table = tableEnv
            .fromDataStream(stream , 'id , 'temperature , 'pt.proctime)
            .window(Tumble over 10.seconds on 'pt as 'tw)
            .groupBy('id , 'tw)
            .select('id , 'id.count)

        println(tableEnv.explain(table))

        table
            .toRetractStream[(String , Long)]
            .print()

        env.execute()
    }
}
