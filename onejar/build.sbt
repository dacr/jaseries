import AssemblyKeys._

seq(assemblySettings: _*)

name := "janalyse-series-onejar"

scalaVersion := "2.10.3"

mainClass in assembly := Some("scala.tools.nsc.MainGenericRunner")

jarName in assembly := "jaseries.jar"

libraryDependencies <++=  scalaVersion { sv =>
       ("org.scala-lang" % "jline"           % sv  % "compile")  ::
       ("org.scala-lang" % "scala-compiler"  % sv  % "compile")  ::
       ("org.scala-lang" % "scalap"          % sv  % "compile")  ::Nil
}

libraryDependencies += "fr.janalyse"   %% "janalyse-series" % "1.6.1" % "compile"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp => 
  cp filter {c=> List("bcmail", "bcprov", "itext", "xml-apis") exists {c.data.getName contains _} }
}

// jansi is embedded inside jline !
excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {c=> List("jansi") exists {c.data.getName contains _} }
  }

