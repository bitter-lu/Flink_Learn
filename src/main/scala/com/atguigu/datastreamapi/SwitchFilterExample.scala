package com.atguigu.datastreamapi

import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object SwitchFilterExample {

    def main(args: Array[String]): Unit = {

        val env = StreamExecutionEnvironment.getExecutionEnvironment
        env.setParallelism(1)

        val readings = env.addSource(new SensorSource)

        val switch = env.fromElements(
            ("sensor_2" , 10 * 1000L),
            ("sensor_6" , 20 * 1000L)
        )

        val forwardReadings = readings
            .keyBy(_.id)
            .connect(switch.keyBy(_._1))
            .process(new ReadingFilter)

        forwardReadings.print()

        env.execute()
    }

    class ReadingFilter extends CoProcessFunction[SensorReading,
        (String,Long) , SensorReading] {
        lazy val forwardingEnabled = getRuntimeContext.getState(
            new ValueStateDescriptor[Boolean]("switch" , Types.of[Boolean])
        )

        lazy val disableTimer = getRuntimeContext.getState(
            new ValueStateDescriptor[Long]("timer" , Types.of[Long])
        )

        override def processElement1(value: SensorReading,
                                     ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context,
                                     out: Collector[SensorReading]): Unit = {
            if (forwardingEnabled.value()) {
                out.collect(value)
            }
        }

        override def processElement2(value: (String, Long),
                                     ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context,
                                     out: Collector[SensorReading]): Unit = {
            forwardingEnabled.update(true)

            val timerTimestamp = ctx.timerService().currentProcessingTime() + value._2
            val curTimerTimestamp = disableTimer.value()
            if (timerTimestamp > curTimerTimestamp) {
                ctx.timerService().deleteProcessingTimeTimer(curTimerTimestamp)
                ctx.timerService().registerProcessingTimeTimer(timerTimestamp)
                disableTimer.update(curTimerTimestamp)
            }
        }

        override def onTimer(timestamp: Long,
                             ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#OnTimerContext,
                             out: Collector[SensorReading]): Unit = {
            forwardingEnabled.clear()
            disableTimer.clear()
        }
    }

}
