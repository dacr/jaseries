name := "janalyse-series"

version := "1.6.3"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.10.5"

crossScalaVersions := Seq("2.10.5", "2.11.6")

scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature", "-language:implicitConversions")

//libraryDependencies <++=  scalaVersion { sv =>
//   Seq("org.scala-lang" % "scala-swing" % sv)
//}

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
        "org.scala-lang.modules" %% "scala-swing" % "1.0.1")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}


libraryDependencies ++= Seq(
    "org.apache.commons" %  "commons-compress"   % "1.9"
   ,"org.jfree"          %  "jfreechart"         % "1.0.15"
   ,"org.slf4j"          % "slf4j-api"           % "1.7.+"
   ,"fr.janalyse"        %% "unittools"          % "0.2.+"
   ,"org.scalatest"      %% "scalatest"          % "2.2.+"  % "test"
)


resolvers += "JAnalyse RepositoryXXX" at "http://www.janalyse.fr/repository/"

publishTo := Some(
     Resolver.sftp(
         "JAnalyse Repository",
         "www.janalyse.fr",
         "/home/tomcat/webapps-janalyse/repository"
     ) as("tomcat", new File(util.Properties.userHome+"/.ssh/id_rsa"))
)

// Doesn't work
//scalacOptions in doc ++= Seq( 
//    "-doc-root-content", "src/main/scala/fr/janalyse/series/package.scala" 
//) 
