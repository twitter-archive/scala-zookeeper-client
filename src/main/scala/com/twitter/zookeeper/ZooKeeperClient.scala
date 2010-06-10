package com.twitter.zookeeper

import scala.collection.jcl.Conversions.{convertList, convertSet}
import scala.collection.mutable
import org.apache.zookeeper.{CreateMode, KeeperException, Watcher, WatchedEvent, ZooKeeper}
import org.apache.zookeeper.data.{ACL, Stat, Id}
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.Watcher.Event.EventType
import net.lag.logging.Logger
import net.lag.configgy.ConfigMap

// Watch helpers
class ZKWatch(watch : WatchedEvent => Unit) extends Watcher {
  override def process(event : WatchedEvent) { watch(event) }
}

object ZKWatch {
  def apply(watch : WatchedEvent => Unit) = { new ZKWatch(watch) }
}

class ZooKeeperClient(servers: String, sessionTimeout: Int, basePath: String, watcher: ZKWatch) {
  private val log = Logger.get

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

  def this(config: ConfigMap, watcher: ZKWatch) = {
    this(config.getString("zookeeper-client.hostlist").get, // Must be set. No sensible default.
         config.getInt("zookeeper-client.session-timeout",  3000),
         config.getString("base-path", ""),
         watcher)
  }

  def this(servers: String, watcher: ZKWatch) =
    this(servers, 3000, "", watcher)

  lazy val zk = new ZooKeeper(servers, sessionTimeout, watcher)

  private var connected = true

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
  def watchNode(path : String, onDataChanged : Option[Array[Byte]] => Unit) {
    def updateData {
      try {
        onDataChanged(Some(zk.getData(makeNodePath(path), dataGetter, null)))
      } catch {
        case e:KeeperException => {
          log.warning("Failed to read node %s: %s", path, e)
          deletedData
        }
      }
    }
    def deletedData {
      onDataChanged(None)
      if (zk.exists(makeNodePath(path), dataGetter) != null) {
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
    val children = zk.getChildren(node,
                                  ZKWatch {event => {
                                    if (event.getType == EventType.NodeChildrenChanged)
                                      watchChildren(node, updateChildren)}})
    updateChildren(children)
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
    val path = makeNodePath(node)
    def parentWatcher : ZKWatch = ZKWatch {
      event => if (event.getType == EventType.NodeCreated) {
        watchChildrenWithData(node, watchMap, deserialize, notifier)
      }
    }

    // Calls deserialize on the data and recursively calls itself when a child node is modified.
    def dataGetter(child: String): ZKWatch = ZKWatch { event =>
      if (event.getType == EventType.NodeDataChanged || event.getType == EventType.NodeCreated) {
        val data: Array[Byte] = zk.getData("%s/%s".format(node, child), dataGetter(child), null)
        val deserialized = deserialize(data)
        log.debug("Node updated: (%s -> %s)", child, deserialized)
        watchMap.synchronized {
          watchMap(child) = deserialized
        }
        notifier.map(f => f(child))
      } else if (event.getType == EventType.NodeDeleted) {
        log.debug("Node deleted: %s", child)
        watchMap.synchronized {
          watchMap -= child
        }
        notifier.map(f => f(child))
      }
    }

    val children: Seq[String] = try {
      zk.getChildren(path,
                     ZKWatch { event => {
                       if (event.getType == EventType.NodeChildrenChanged)
                         watchChildrenWithData(node, watchMap, deserialize, notifier)}})
    } catch {
      case e:KeeperException => {
        log.warning("Failed to read node %s: %s", path, e)
        if (zk.exists(path, parentWatcher) != null) {
          watchChildrenWithData(node, watchMap, deserialize, notifier)
        }
        List()
      }
    }

    // For each child that doesn't exist in the watchMap, setup a watcher and
    // add it to the watch map.
    for (child <- children if !watchMap.contains(child)) {
      val childPath = "%s/%s".format(path, child)
      log.debug("Getting zookeeper data for: %s".format(childPath))
      val data = zk.getData(childPath, dataGetter(child), null)
      val deserialized = deserialize(data)

      log.debug("Node found: (%s -> %s)", child, deserialized)
      watchMap.synchronized {
        watchMap(child) = deserialized
      }
      notifier.map(f => f(child))
    }
  }
}
