name := "janalyse-series"

version := "1.6.3-SNAPSHOT"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.5", "2.11.7")

scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature", "-language:implicitConversions")


libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.+",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.+",
        "org.scala-lang.modules" %% "scala-swing" % "1.0.+")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}


libraryDependencies ++= Seq(
    "fr.janalyse"        %% "unittools"          % "0.2.+"
   ,"org.apache.commons" %  "commons-compress"   % "1.9"
   ,"org.jfree"          %  "jfreechart"         % "1.0.19"
   ,"org.slf4j"          % "slf4j-api"           % "1.7.+"
   ,"org.scalatest"      %% "scalatest"          % "2.2.+"  % "test"
)

resolvers += "JAnalyse Repository requirements" at "http://www.janalyse.fr/repository/"

