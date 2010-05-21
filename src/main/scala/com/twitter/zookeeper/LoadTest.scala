package com.twitter.zookeeper.loadtest

import java.io.File
import org.apache.zookeeper.WatchedEvent
import com.twitter.jackhammer.LoggingLoadTest
import net.lag.configgy.{Config, Configgy}
import net.lag.logging.Logger


object LoadTest extends LoggingLoadTest {
  Configgy.configure("src/main/resources/config.conf")

  private val config = Configgy.config
  private val log = Logger.get

  val GETS = config.getInt("times-to-get", 100000)
  //val CONCURRENCY = config.getInt("concurrency", 4)
  val HOSTLIST = config.getString("hostlist", "localhost:2181")

  val zkClient = new ZooKeeperClient(HOSTLIST, ZKWatch((watch : WatchedEvent) => {}))

  def singleClientTest {
    runWithTimingNTimes(GETS) {
      zkClient.get("/")
    }
  }

  def parallelClientTest {
    runInActorNTimes(GETS) {
      zkClient.get("/")
    }
  }

  def main(args: Array[String]) {
    log.info("starting up")

    // run tests
    singleClientTest
    val singleClientLogFile = new File("/tmp/single-client-loadtest-%d.log".format(System.currentTimeMillis))
    dumpLogOutput(singleClientLogFile)

    parallelClientTest
    val parallelClientLogFile = new File("/tmp/parallel-client-loadtest-%d.log".format(System.currentTimeMillis))
    dumpLogOutput(parallelClientLogFile)

    // peace out
    log.info("done with tests, exiting")
    exit(0)
  }
}
