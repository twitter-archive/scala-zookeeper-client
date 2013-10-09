# scala-zookeeper-client (DEPRECATED)

**This project is deprecated and will not be maintained.**

## About

A Scala client for [Apache ZooKeeper](http://hadoop.apache.org/zookeeper/), "a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services".

ZooKeeper provides a Java client library that's perfectly usable from Scala. This just wraps some idioms and niceties around that library to make it as Scala-friendly as possible. It also ships with tests, so you can have some confidence that you'll be able to interact with ZooKeeper from Scala in a predictable and reliable way.

The scala-zookeeper-client will automatically handle session expired events by creating a new connection to the Zookeeper servers. We also provide several commonly used operations and utilities on top of Zookeeper's minimal API:

* Create a path of nodes, similar to 'mkdir -p'
* Recursively delete a tree of nodes
* Watch a node forever
* Monitor a node's child set
* For a given node, maintain a map from the node's children to the each child's data

## Usage

### Basic usage:

    import com.twitter.zookeeper.ZooKeeperClient

    val zk = new ZooKeeperClient("localhost:2181")

    zk.createPath("/a/b/c")
    zk.createPath("/a/b/d")
    val children = zk.getChildren("/a/b") // Seq("c", "d")

    zk.set("/a/b/c", "foo".getBytes())
    val s = new String(zk.get("/a/b/c")) // "foo"

    zk.deleteRecursive("/a") // delete "a" and all children

### Advanced features:

Monitor a node forever:

    import com.twitter.zookeeper.ZooKeeperClient
    import org.apache.zookeeper.CreateMode

    val zk = new ZooKeeperClient("localhost:2181")
    zk.create("/test-node", "foo".getBytes, CreateMode.PERSISTENT)
    zk.watchNode("/test-node", { (data : Option[Array[Byte]]) =>
      data match {
        case Some(d) => println("Data updated: %s".format(new String(d)))
        case None => println("Node deleted")
      }
    })
    zk.set("/test-node", "bar".getBytes)
    zk.set("/test-node", "baz".getBytes)
    zk.delete("/test-node")

Monitor a node's children:

    import com.twitter.zookeeper.ZooKeeperClient
    import org.apache.zookeeper.CreateMode

    val zk = new ZooKeeperClient("localhost:2181")
    zk.create("/parent", null, CreateMode.PERSISTENT)
    zk.watchChildren("/parent", { (children : Seq[String]) =>
      println("Children: %s".format(children.mkString(", ")))
    })
    zk.create("/parent/child1", null, CreateMode.PERSISTENT)
    zk.create("/parent/child2", null, CreateMode.PERSISTENT)
    zk.delete("/parent/child1")
    zk.create("/parent/child3", null, CreateMode.PERSISTENT)
    zk.deleteRecursive("/parent")

For a given node, automatically maintain a map from the node's children to the each child's data:

    import com.twitter.zookeeper.ZooKeeperClient
    import org.apache.zookeeper.CreateMode
    import scala.collection.mutable

    val zk = new ZooKeeperClient("localhost:2181")
    val childMap = mutable.Map[String, String]()

    zk.create("/parent", null, CreateMode.PERSISTENT)
    zk.watchChildrenWithData("/parent", childMap, {data => new String(data)})

    zk.create("/parent/a", "foo".getBytes, CreateMode.PERSISTENT)
    zk.create("/parent/b", "bar".getBytes, CreateMode.PERSISTENT)
    println("child map: %s".format(childMap)) // NOTE: real code should synchronize access on childMap

    zk.delete("/parent/a")
    zk.set("/parent/b", "bar2".getBytes)
    zk.create("/parent/c", "baz".getBytes, CreateMode.PERSISTENT)
    println("child map: %s".format(childMap)) // NOTE: real code should synchronize access on childMap

Get the internal zookeeper handle to pass to other libraries:

    import com.twitter.zookeeper.ZooKeeperClient
    import org.apache.zookeeper.ZooKeeper

    def zkListener(zkHandle : ZooKeeper) {
      // use zkHandle
      println("Internal zookeeper handle changed: %s".format(zkHandle))
      // Note that zkListener will be called with a new handle whenever the Zookeeper session
      // has expired and a new connection to the Zookeeper servers is created
    }

    val zk = new ZooKeeperClient("localhost:2181", zkListener _)

## Authors

* [John Corwin](http://github.com/jcorwin)
* [Steve Jenson](http://github.com/stevej)
* [Matt Knox](http://github.com/mattknox)
* [Ian Ownbey](http://github.com/imownbey)
* [Ryan King](http://github.com/ryanking)
* [Alex Payne](http://github.com/al3x)

## License

Apache License, Version 2.0. See the LICENSE file for more information.
