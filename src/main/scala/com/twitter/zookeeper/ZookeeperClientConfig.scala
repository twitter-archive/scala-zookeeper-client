package com.twitter.zookeeper

import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.util.Config

@deprecated("scala-zookeeper-client is deprecated in favor of util-zk", "3.0.7")
trait ZookeeperClientConfig extends Config[ZooKeeperClient] {
  var hostList = required[String]
  var sessionTimeout = 3000
  var basePath = ""

  def apply = {
    new ZooKeeperClient(this)
  }
}
