package com.twitter.zookeeper

import scala.collection.jcl.Conversions.{convertList, convertSet}
import scala.collection.mutable
import scala.collection.immutable.Set
import org.apache.zookeeper.{CreateMode, KeeperException, Watcher, WatchedEvent, ZooKeeper}
import org.apache.zookeeper.data.{ACL, Stat, Id}
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.Watcher.Event.KeeperState
import net.lag.logging.Logger
import net.lag.configgy.ConfigMap
import java.util.concurrent.CountDownLatch

// Watch helpers
class ZKWatch(watch : WatchedEvent => Unit) extends Watcher {
  override def process(event : WatchedEvent) { watch(event) }
}

object ZKWatch {
  def apply(watch : WatchedEvent => Unit) = { new ZKWatch(watch) }
}

class ZooKeeperClient(servers: String, sessionTimeout: Int, basePath : String, watcher: Option[ZKWatch]) extends Watcher {
  private val log = Logger.get
  private val connectionLatch = new CountDownLatch(1)
  private var connected = false
  private val zk = new ZooKeeper(servers, sessionTimeout, this)
  connectionLatch.await()

  def process(event : WatchedEvent) {
    log.info("Zookeeper event: %s".format(event))
    event.getState match {
      case KeeperState.SyncConnected => {
        if (!connected) {
          connected = true
          connectionLatch.countDown()
        }
      }
      case _ =>
    }
    watcher.map(w => w.process(event))
  }

  def this(config: ConfigMap, watcher: Option[ZKWatch]) = {
    this(config.getString("zookeeper-client.hostlist").get, // Must be set. No sensible default.
         config.getInt("zookeeper-client.session-timeout", 3000),
         config.getString("base-path", ""),
         watcher)
  }

  def this(config: ConfigMap, watcher: ZKWatch) = {
    this(config, Some(watcher))
  }

  def this(config: ConfigMap) = {
    this(config, None)
  }

  def this(servers: String, watcher: Option[ZKWatch]) =
    this(servers, 3000, "", watcher)

  def this(servers: String, watcher: ZKWatch) =
    this(servers, Some(watcher))

  def this(servers: String) =
    this(servers, None)


  /**
   * Given a string representing a path, return each subpath
   * Ex. subPaths("/a/b/c", "/") == ["/a", "/a/b", "/a/b/c"]
   */
  private def subPaths(path : String, sep : Char) = {
    val l = List.fromString(path, sep)
    val paths = l.foldLeft[List[String]](Nil){(xs, x) =>
      (xs.firstOption.getOrElse("") + sep.toString + x)::xs}
    paths.reverse
  }

  private def makeNodePath(path : String) = "%s/%s".format(basePath, path).replaceAll("//", "/")

  def getChildren(path: String): Seq[String] = {
    zk.getChildren(makeNodePath(path), false)
  }

  def close() = zk.close

  def isAlive: Boolean = {
    // If you can get the root, then we're alive.
    val result: Stat = zk.exists("/", false) // do not watch
    result.getVersion >= 0
  }

  def create(path: String, data: Array[Byte], createMode: CreateMode): String = {
    zk.create(makeNodePath(path), data, Ids.OPEN_ACL_UNSAFE, createMode)
  }

  /**
   * ZooKeeper version of mkdir -p
   */
  def createPath(path: String) {
    for (path <- subPaths(makeNodePath(path), '/')) {
      try {
        log.debug("Creating path in createPath: %s", path)
        zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      } catch {
        case _:KeeperException.NodeExistsException => {} // ignore existing nodes
      }
    }
  }

  def get(path: String): Array[Byte] = {
    zk.getData(makeNodePath(path), false, null)
  }

  def delete(path: String) {
    zk.delete(makeNodePath(path), -1)
  }

  /**
   * Watches a node. When the node's data is changed, onDataChanged will be called with the
   * new data value as a byte array. If the node is deleted, onDataChanged will be called with
   * None and will track the node's re-creation with an existence watch.
   */
  def watchNode(node : String, onDataChanged : Option[Array[Byte]] => Unit) {
    val path = makeNodePath(node)
    def updateData {
      try {
        onDataChanged(Some(zk.getData(path, dataGetter, null)))
      } catch {
        case e:KeeperException => {
          log.warning("Failed to read node %s: %s", path, e)
          deletedData
        }
      }
    }
    def deletedData {
      onDataChanged(None)
      if (zk.exists(path, dataGetter) != null) {
        // Node was re-created by the time we called zk.exist
        updateData
      }
    }
    def dataGetter : ZKWatch = ZKWatch {
      event =>
        if (event.getType == EventType.NodeDataChanged || event.getType == EventType.NodeCreated) {
          updateData
        } else if (event.getType == EventType.NodeDeleted) {
          deletedData
        }
    }
    updateData
  }

  /**
   * Gets the children for a node (relative path from our basePath), watches
   * for each NodeChildrenChanged event and runs the supplied updateChildren function and
   * re-watches the node's children.
   */
  def watchChildren(node : String, updateChildren : Seq[String] => Unit) {
    val path = makeNodePath(node)
    val childWatcher : ZKWatch =
      ZKWatch {event =>
        if (event.getType == EventType.NodeChildrenChanged ||
            event.getType == EventType.NodeCreated)
          watchChildren(node, updateChildren)}
    try {
      val children = zk.getChildren(path, childWatcher)
      updateChildren(children)
    } catch {
      case e:KeeperException => {
        log.warning("Failed to read node %s: %s", path, e)
        updateChildren(List())
        zk.exists(path, childWatcher)
      }
    }
  }

  /**
   * WARNING: watchMap must be thread-safe. Writing is synchronized on the watchMap. Readers MUST
   * also synchronize on the watchMap for safety.
   */
  def watchChildrenWithData[T](node : String, watchMap: mutable.Map[String, T], deserialize: Array[Byte] => T) {
    watchChildrenWithData(node, watchMap, deserialize, None)
  }

  /**
   * Watch a set of nodes with an explicit notifier. The notifier will be called whenever
   * the watchMap is modified
   */
  def watchChildrenWithData[T](node : String, watchMap: mutable.Map[String, T],
                               deserialize: Array[Byte] => T, notifier: String => Unit) {
    watchChildrenWithData(node, watchMap, deserialize, Some(notifier))
  }

  private def watchChildrenWithData[T](node : String, watchMap: mutable.Map[String, T],
                                       deserialize: Array[Byte] => T, notifier: Option[String => Unit]) {
    def nodeChanged(child : String)(childData : Option[Array[Byte]]) {
      childData match {
        case Some(data) => {
          watchMap.synchronized {
            watchMap(child) = deserialize(data)
          }
          notifier.map(f => f(child))
        }
        case None => // deletion handled via parent watch
      }
    }

    def parentWatcher(children : Seq[String]) {
      val childrenSet = Set(children : _*)
      val watchedKeys = Set(watchMap.keySet.toSeq : _*)
      val removedChildren = watchedKeys -- childrenSet
      val addedChildren = childrenSet -- watchedKeys
      watchMap.synchronized {
        // remove deleted children from the watch map
        for (child <- removedChildren) {
          watchMap -= child
        }
        // add new children to the watch map
        for (child <- addedChildren) {
          watchNode("%s/%s".format(node, child), nodeChanged(child))
        }
      }
      for (child <- removedChildren ++ addedChildren) {
        notifier.map(f => f(child))
      }
    }

    watchChildren(node, parentWatcher)
  }
}
