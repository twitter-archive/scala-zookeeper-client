package com.twitter.zookeeper

trait ZookeeperClientConfig {
  val hostList: String
  val sessionTimeout: Int = 3000
  val basePath: String = ""
}