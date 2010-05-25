package com.twitter.zookeeper

import java.net.{Socket, SocketException}
import org.scala_tools.javautils.Imports._
import org.apache.zookeeper.{CreateMode, Watcher, WatchedEvent}
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.KeeperException.NoNodeException
import org.apache.zookeeper.data.{ACL, Id}
import org.specs._
import net.lag.configgy.Configgy

class ZookeeperClientSpec extends Specification {
  "ZookeeperClient" should {
    Configgy.configure("src/main/resources/config.conf")

    val watcher = ZKWatch((a: WatchedEvent) => {})
    val configMap = Configgy.config
    val zkClient = new ZooKeeperClient(configMap, watcher)

    doBefore {
      // we need to be sure that a ZooKeeper server is running in order to test
      val hostlist = configMap.getString("zookeeper-client.hostlist", "localhost:2181")
      val socketPort = hostlist.split(":")
      new Socket(socketPort(0), socketPort(1).toInt) must throwA[SocketException]
    }

    doLast {
      zkClient.close
    }

    "be able to be instantiated with a FakeWatcher" in {
      zkClient mustNot beNull
    }

    "connect to local Zookeeper server and retrieve version" in {
      zkClient.isAlive mustBe true
    }

    "get data at a known-good specified path" in {
      val results: Array[Byte] = zkClient.get("/")
      results.size must beGreaterThanOrEqualTo(0)
    }

    "get data at a known-bad specified path" in {
      zkClient.get("/thisdoesnotexist") must throwA[NoNodeException]
    }

    "get list of children" in {
      zkClient.getChildren("/") must notBeEmpty
    }

    "create a node at a specified path" in {
      val data: Array[Byte] = Array(0x63)
      val id = new Id("world", "anyone")
      val createMode = EPHEMERAL

      zkClient.create("/foo", data, createMode) mustEqual "/foo"
      zkClient.delete("/foo")
    }
  }
}
