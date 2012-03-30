package com.twitter.zookeeper

import java.net.{Socket, ConnectException}
import org.apache.zookeeper.{CreateMode, Watcher, WatchedEvent, ZooKeeper}
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.KeeperException.NoNodeException
import org.apache.zookeeper.data.{ACL, Id}
import org.specs._
import scala.collection.mutable

class ZookeeperClientSpec extends SpecificationWithJUnit {
  val config = new TestConfig
  val hostlist = config.hostList
  //
  // doBeforeSpec {
  //   // we need to be sure that a ZooKeeper server is running in order to test
  //   println("Testing connection to ZooKeeper server at %s...".format(hostlist))
  //   val socketPort = hostlist.split(":")
  //   new Socket(socketPort(0), socketPort(1).toInt) mustNot throwA[ConnectException]
  // }
  //
  // "ZookeeperClient" should {
  //   shareVariables()
  //   var zkClient : ZooKeeperClient = null
  //
  //   doFirst {
  //     println("Attempting to connect to ZooKeeper server %s...".format(hostlist))
  //     zkClient = new ZooKeeperClient(config, None)
  //   }
  //
  //   doLast {
  //     zkClient.close
  //   }
  //
  //   "be able to be instantiated with a FakeWatcher" in {
  //     zkClient mustNot beNull
  //   }
  //
  //   "connect to local Zookeeper server and retrieve version" in {
  //     zkClient.isAlive mustBe true
  //   }
  //
  //   "get data at a known-good specified path" in {
  //     val results: Array[Byte] = zkClient.get("/")
  //     results.size must beGreaterThanOrEqualTo(0)
  //   }
  //
  //   "get data at a known-bad specified path" in {
  //     zkClient.get("/thisdoesnotexist") must throwA[NoNodeException]
  //   }
  //
  //   "get list of children" in {
  //     zkClient.getChildren("/") must notBeEmpty
  //   }
  //
  //   "create a node at a specified path" in {
  //     val data: Array[Byte] = Array(0x63)
  //     val id = new Id("world", "anyone")
  //     val createMode = EPHEMERAL
  //
  //     zkClient.create("/foo", data, createMode) mustEqual "/foo"
  //     zkClient.delete("/foo")
  //   }
  //
  //   "watch a node" in {
  //     val data: Array[Byte] = Array(0x63)
  //     val node = "/datanode"
  //     val createMode = EPHEMERAL
  //     var watchCount = 0
  //     def watcher(data : Option[Array[Byte]]) {
  //       watchCount += 1
  //     }
  //     zkClient.create(node, data, createMode)
  //     zkClient.watchNode(node, watcher)
  //     Thread.sleep(50L)
  //     watchCount mustEqual 1
  //     zkClient.delete("/datanode")
  //   }
  //
  //   "watch a tree of nodes" in {
  //     var children : Seq[String] = List()
  //     var watchCount = 0
  //     def watcher(nodes : Seq[String]) {
  //       watchCount += 1
  //       children = nodes
  //     }
  //     zkClient.createPath("/tree/a")
  //     zkClient.createPath("/tree/b")
  //     zkClient.watchChildren("/tree", watcher)
  //     children.size mustEqual 2
  //     children must containAll(List("a", "b"))
  //     watchCount mustEqual 1
  //     zkClient.createPath("/tree/c")
  //     Thread.sleep(50L)
  //     children.size mustEqual 3
  //     children must containAll(List("a", "b", "c"))
  //     watchCount mustEqual 2
  //     zkClient.delete("/tree/a")
  //     Thread.sleep(50L)
  //     children.size mustEqual 2
  //     children must containAll(List("b", "c"))
  //     watchCount mustEqual 3
  //     zkClient.deleteRecursive("/tree")
  //   }
  //
  //   "watch a tree of nodes with data" in {
  //     def mkNode(node : String) {
  //       zkClient.create("/root/" + node, node.getBytes, CreateMode.EPHEMERAL)
  //     }
  //     var children : mutable.Map[String,String] = mutable.Map()
  //     var watchCount = 0
  //     def notifier(child : String) {
  //       watchCount += 1
  //       if (children.contains(child)) {
  //         children(child) mustEqual child
  //       }
  //     }
  //     zkClient.createPath("/root")
  //     mkNode("a")
  //     mkNode("b")
  //     zkClient.watchChildrenWithData("/root", children,
  //                                    {(b : Array[Byte]) => new String(b)}, notifier)
  //     children.size mustEqual 2
  //     children.keySet must containAll(List("a", "b"))
  //     watchCount mustEqual 2
  //     mkNode("c")
  //     Thread.sleep(50L)
  //     children.size mustEqual 3
  //     children.keySet must containAll(List("a", "b", "c"))
  //     watchCount mustEqual 3
  //     zkClient.delete("/root/a")
  //     Thread.sleep(50L)
  //     children.size mustEqual 2
  //     children.keySet must containAll(List("b", "c"))
  //     watchCount mustEqual 4
  //     zkClient.deleteRecursive("/root")
  //   }
  //
  // }
}
