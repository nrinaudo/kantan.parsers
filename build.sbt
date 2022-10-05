lazy val root = Project(id = "kantan-parsers", base = file("."))
  .settings(
    moduleName     := "root",
    publish / skip := true
  )
  .aggregate(core, examples)

lazy val core = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(moduleName := "kantan.parsers")
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
  )
  .in(file("core"))

lazy val examples = project
  .enablePlugins(AutomateHeaderPlugin)
  .in(file("examples"))
  .settings(
    libraryDependencies += "org.typelevel" %% "jawn-ast" % Versions.jawn,
    publish / skip      := true
  )
  .dependsOn(core)
