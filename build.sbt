lazy val root = Project(id = "kantan-parsers", base = file("."))
  .settings(moduleName := "root")
  .settings(
    publish         := {},
    publishLocal    := {},
    publishArtifact := false
  )
  .aggregate(core, examples)

lazy val core = project
  .settings(moduleName := "kantan.parsers")
  .in(file("core"))

lazy val examples = project
  .in(file("examples"))
  .settings(
    libraryDependencies += "org.typelevel" %% "jawn-ast" % Versions.jawn
  )
  .dependsOn(core)
