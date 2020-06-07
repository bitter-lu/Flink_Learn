package com.atguigu.zone.tableapiandflinksql

import com.atguigu.datastreamapi.SensorSource
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._
import org.apache.flink.table.functions.ScalarFunction

object ScalarFunctionExample {

    def main(args: Array[String]): Unit = {

        val env =   StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build()

        val tEnv = StreamTableEnvironment.create(
            env,
            settings
        )

        val stream = env.addSource(new SensorSource)

        val hashCode = new HashCode(10)
        tEnv
            .registerFunction("hashcode" , hashCode)

        val table = tEnv.fromDataStream(stream , 'id)

        table
                .select('id , hashCode('id))
                .toAppendStream[(String , Int)]
                .print()

        tEnv.createTemporaryView("t" , table , 'id)
        tEnv
            .sqlQuery("select id , hashCode(id) from t")
            .toAppendStream[(String , Long)]
            .print()

        env.execute()
    }

    class HashCode(factor: Int) extends ScalarFunction {
        def eval(s : String) : Int = {
            s.hashCode * factor
        }
    }

}
