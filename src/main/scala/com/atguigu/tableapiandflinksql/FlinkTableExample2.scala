package com.atguigu.tableapiandflinksql

import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._

object FlinkTableExample2 {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build()

        val tableEnv = StreamTableEnvironment.create(
            env,
            settings
        )

        val sinkDDL : String =
            """
              |create table dataTable (
              | id varchar(20) not null,
              | ts bigint,
              | temperature double,
              | pt AS PROCTIME()
              |) with (
              | 'connector.type' = 'filesystem',
              | 'connector.path' = 'F:\IDEA\IdeaProjects\Flink1125SH\src\main\resources\sensor.txt',
              | 'format.type' = 'csv'
              |)
            """.stripMargin

        tableEnv.sqlUpdate(sinkDDL)
        tableEnv
            .sqlQuery("select id, ts from dataTable")
            .toAppendStream[(String,Long)]
            .print()

        env.execute()
    }

}
