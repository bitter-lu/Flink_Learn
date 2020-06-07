package com.atguigu.tableapiandflinksql

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.table.functions.TableFunction

object TableFunctionExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build()

        val tEnv = StreamTableEnvironment.create(env, settings)
        val split = new Split("#")

        val stream = env.fromElements("hello#world")

        val table = tEnv.fromDataStream(stream, 's)

        // table api使用udf
        table
            //.joinLateral((split('s) as ('word, 'length)))
            .leftOuterJoinLateral(split('s) as ('word, 'length))
            .select('s, 'word, 'length)
            .toAppendStream[(String, String, Long)]
            .print()

        // sql中使用udf
        tEnv.registerFunction("split", new Split("#"))

        tEnv.createTemporaryView("t", table, 's)

        tEnv
            // 大写`T`是sql中元组
            .sqlQuery(
            """select s, word, length
              |from t
              |left join lateral table(split(s)) as T(word , length)
              |on true
            """.stripMargin
        )
            .toAppendStream[(String, String, Long)]
            .print()

        tEnv
            .sqlQuery(
                """select s , word , length
                  |from t,
                  |lateral table(split(s)) as T(word , length)
                """.stripMargin
            )
            .toAppendStream[(String, String, Long)]
            .print()

        env.execute()
    }

    class Split(separator: String) extends TableFunction[(String, Int)] {
        def eval(s: String): Unit = {
            s.split(separator).foreach(x => collect((x, x.length)))
        }
    }

}
