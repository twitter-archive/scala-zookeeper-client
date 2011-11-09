import sbt._
import Process._
import com.twitter.sbt._

class ZookeeperClientProject(info: ProjectInfo) extends StandardLibraryProject(info)
 with DefaultRepos
 with ProjectDependencies
 with CompileThriftFinagle
 with SubversionPublisher
 with EnsimeGenerator {

  override def subversionRepository = Some("https://svn.twitter.biz/maven/")

  val sp = "org.scala-tools.testing" % "specs_2.8.1"  % "1.6.7" % "test"
  val ostrich = "com.twitter" % "ostrich" % "4.10.0"
  val zk = "org.apache.zookeeper" % "zookeeper" % "3.3.3"

  override def ivyXML =
    <dependencies>
      <exclude module="jms"/>
      <exclude module="jmxtools"/>
      <exclude module="jmxri"/>
    </dependencies>

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

}
