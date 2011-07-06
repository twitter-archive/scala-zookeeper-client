import sbt._
import Process._
import com.twitter.sbt._

class ZookeeperClientProject(info: ProjectInfo) extends StandardLibraryProject(info) 
  with DefaultRepos
  with NoisyDependencies {

  val sp = "org.scala-tools.testing" % "specs_2.8.0"  % "1.6.5"
  val ostrich = "com.twitter" % "ostrich" % "4.4.0"
  val zookeeper = "org.apache" % "zookeeper" % "3.3.2-dev"

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

}
