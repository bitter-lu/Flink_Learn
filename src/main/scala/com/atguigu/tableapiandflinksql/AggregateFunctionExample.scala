package com.atguigu.tableapiandflinksql

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.RowTypeInfo
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.table.functions.AggregateFunction
import org.apache.flink.types.Row

object AggregateFunctionExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build()

        val tEnv = StreamTableEnvironment.create(env,settings)

        val agg = new MyMinMax

        val stream = env.fromElements(
            (1,-1),
            (1,2)
        )

        val table = tEnv.fromDataStream(stream , 'key , 'a)
        table
            .groupBy('key)
            .aggregate(agg('a) as ('x , 'y))
            .select('key , 'x , 'y)
            .toRetractStream[(Long , Long , Long)]
            .print()

        env.execute()
    }

    case class MyMinMaxAcc(var min: Int , var max: Int)

    class MyMinMax extends AggregateFunction[Row , MyMinMaxAcc] {
        override def createAccumulator(): MyMinMaxAcc = {
            MyMinMaxAcc(Int.MaxValue , Int.MinValue)
        }

        override def getValue(acc: MyMinMaxAcc): Row = {
            Row.of(Integer.valueOf(acc.min) , Integer.valueOf(acc.max))
        }


        override def getResultType: TypeInformation[Row] = {
            new RowTypeInfo(Types.INT , Types.INT)
        }

        def accumulate(acc:MyMinMaxAcc , value:Int) = {
            if (value < acc.min) {
                acc.min = value
            }
            if (value > acc.max) {
                acc.max = value
            }
        }
    }



}
