name := "janalyse-series"

version := "1.6.1-rc2"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.10.3"

crossScalaVersions := Seq( "2.10.3")

scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature")

libraryDependencies <++=  scalaVersion { sv =>
   Seq("org.scala-lang" % "scala-swing" % sv)
}

libraryDependencies ++= Seq(
    "fr.janalyse"        %% "unittools"          % "0.2.3"
   ,"org.apache.commons" %  "commons-compress"   % "1.5"
   ,"org.jfree"          %  "jfreechart"         % "1.0.15"
   ,"com.typesafe"       %% "scalalogging-slf4j" % "1.0.1"
   ,"org.scalatest"      %% "scalatest"          % "1.9.2"  % "test"
   ,"junit"              %  "junit"              % "4.10"   % "test"
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
