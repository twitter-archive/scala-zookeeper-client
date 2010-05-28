import sbt._
import Process._
import com.twitter.sbt.{StandardProject, SubversionRepository}


class ZookeeperClientProject(info: ProjectInfo) extends StandardProject(info) {
  // Maven repositories
  val mavenDotOrg = "repo1" at "http://repo1.maven.org/maven2/"
  val jBoss = "jboss-repo" at "http://repository.jboss.org/maven2/"
  val nest = "nest" at "http://www.lag.net/nest/"
  val apache = "apache" at "http://people.apache.org/repo/m2-ibiblio-rsync-repository/"

  // dependencies
  val specs = "org.scala-tools.testing" % "specs" % "1.6.2"
  val vscaladoc = "org.scala-tools" % "vscaladoc" % "1.1-md-3"
  val markdownj = "markdownj" % "markdownj" % "1.0.2b4-0.3.0"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.5.8"
  val slf4jLog = "org.slf4j" % "slf4j-log4j12" % "1.5.8"
  val log4j = "apache-log4j" % "log4j" % "1.2.15"
  val commonsLogging = "commons-logging" % "commons-logging" % "1.1"
  val commonsLang = "commons-lang" % "commons-lang" % "2.2"
  val oro = "oro" % "oro" % "2.0.7"
  val configgy = "net.lag" % "configgy" % "1.4.7"
  val mockito = "org.mockito" % "mockito-core" % "1.8.1"
  val xrayspecs = "com.twitter" % "xrayspecs" % "1.0.5"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.1"
  val asm = "asm" % "asm-all" % "2.2"
  val objenesis = "org.objenesis" % "objenesis" % "1.1"
  val javautils = "org.scala-tools" % "javautils" % "2.7.4-0.1"
  val ostrich = "com.twitter" % "ostrich" % "1.1.6"
  val zookeeper = "org.apache" % "zookeeper" % "3.3.1"

  Credentials(Path.userHome / ".ivy2" / "credentials", log)
  val publishTo = "nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

}
