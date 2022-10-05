enablePlugins(AutomateHeaderPlugin)

name := "kantan.parsers"

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"       % Versions.scalatest               % "test",
  "org.scalatestplus" %% "scalacheck-1-17" % Versions.scalatestPlusScalacheck % "test",
  "org.typelevel"     %% "jawn-ast"        % Versions.jawn                    % "test"
)
