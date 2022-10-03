import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import sbt._, Keys._
import sbt.plugins.{JvmPlugin, SbtPlugin}
import sbt.ScriptedPlugin.autoImport._
import sbtrelease.ReleasePlugin, ReleasePlugin.autoImport._, ReleaseTransformations._, ReleaseKeys._
import xerial.sbt.Sonatype.SonatypeKeys._

object BuildPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = JvmPlugin

  override lazy val projectSettings = baseSettings ++ releaseSettings

  def baseSettings: Seq[sbt.Def.Setting[_]] =
    Seq(
      publishConfiguration := publishConfiguration.value.withOverwrite(true),
      organization         := "com.nrinaudo",
      organizationHomepage := Some(url("https://nrinaudo.github.io")),
      organizationName     := "Nicolas Rinaudo",
      startYear            := Some(2022),
      scalaVersion         := "3.1.3",
      licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage             := Some(url(s"https://nrinaudo.github.io/kantan.parsers")),
      developers := List(
        Developer("nrinaudo", "Nicolas Rinaudo", "nicolas@nrinaudo.com", url("https://twitter.com/nicolasrinaudo"))
      ),
      scmInfo := Some(
        ScmInfo(
          url(s"https://github.com/nrinaudo/kantan.parsers"),
          s"scm:git:git@github.com:nrinaudo/kantan.parsers.git"
        )
      ),
      scalacOptions ++= Seq("-source", "future", "-Ykind-projector:underscores", "-deprecation", "-unchecked"),
      publishTo := sonatypePublishToBundle.value
    )

    def releaseSettings: Seq[Setting[_]] =
      Seq(
        releaseProcess := Seq[ReleaseStep](
          checkSnapshotDependencies,
          inquireVersions,
          runClean,
          releaseStepCommand("scalafmtCheck"),
          releaseStepCommand("scalafmtSbtCheck"),
          setReleaseVersion,
          commitReleaseVersion,
          tagRelease,
          releaseStepCommand("publishSigned"),
          releaseStepCommand("sonatypeBundleRelease"),
          setNextVersion,
          commitNextVersion,
          pushChanges
        )
      )

}
