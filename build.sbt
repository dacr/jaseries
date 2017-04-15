name := "janalyse-series"
version := "1.7.1-SNAPSHOT"

organization :="fr.janalyse"
homepage := Some(new URL("https://github.com/dacr/jaseries"))

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1")
scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature", "-language:implicitConversions")


libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5",
        "org.scala-lang.modules" %% "scala-swing" % "2.0.0")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}


libraryDependencies ++= Seq(
    "fr.janalyse"        %% "unittools"          % "0.3.1"
   ,"org.apache.commons" %  "commons-compress"   % "1.13"
   ,"org.jfree"          %  "jfreechart"         % "1.0.19"
   ,"org.slf4j"          % "slf4j-api"           % "1.7.25"
   ,"org.scalatest"      %% "scalatest"          % "3.0.1"  % "test"
)




pomIncludeRepository := { _ => false }

useGpg := true

licenses += "Apache 2" -> url(s"http://www.apache.org/licenses/LICENSE-2.0.txt")
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true
publishArtifact in Test := false
publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

scmInfo := Some(ScmInfo(url(s"https://github.com/dacr/jaseries"), s"git@github.com:dacr/jaseries.git"))

pomExtra in Global := {
  <developers>
    <developer>
      <id>dacr</id>
      <name>David Crosson</name>
      <url>https://github.com/dacr</url>
    </developer>
  </developers>
}


import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    //runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )

