import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import sbt._, Keys._
import sbt.plugins.{JvmPlugin, SbtPlugin}
import sbt.ScriptedPlugin.autoImport._
import sbtrelease.ReleasePlugin, ReleasePlugin.autoImport._, ReleaseTransformations._, ReleaseKeys._
import xerial.sbt.Sonatype.SonatypeKeys._

object BuildPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = JvmPlugin && ReleasePlugin

  override lazy val projectSettings = baseSettings ++ releaseSettings

  override def globalSettings = addCommandAlias(
    "validate",
    ";clean;scalafmtCheck;Test / scalafmtCheck;scalafmtSbtCheck;test"
  )

  def baseSettings: Seq[sbt.Def.Setting[_]] =
    Seq(
      publishConfiguration := publishConfiguration.value.withOverwrite(true),
      organization         := "com.nrinaudo",
      organizationHomepage := Some(url("https://nrinaudo.github.io")),
      organizationName     := "Nicolas Rinaudo",
      startYear            := Some(2022),
      scalaVersion         := "2.13.10",
      crossScalaVersions   := Seq("2.12.17", "2.13.10", "3.2.0"),
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
      scalacOptions ++= Seq("-deprecation", "-unchecked"),
      publishTo      := sonatypePublishToBundle.value
    )

  def releaseSettings: Seq[Setting[_]] =
    Seq(
      releaseCrossBuild := true,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
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
