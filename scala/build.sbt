val Http4sVersion          = "0.23.30"
val CirceGenericVersion    = "0.14.5"
val CirceYamlVersion       = "0.16.1"
val MunitVersion           = "1.1.1"
val LogbackVersion         = "1.5.18"
val MunitCatsEffectVersion = "2.1.0"
val JwtVersion             = "0.9.1"
val MysqlVersion           = "8.0.11"
val DoobieVersion          = "1.0.0-RC8"
val BcryptVersion          = "0.3m"
val JaxbVersion            = "2.3.1"
val JansiVersion           = "2.4.0"
val TyrianVersion          = "0.14.0"


lazy val backend = (project in file("backend"))
  .settings(
    organization := "com.example",
    name := "pokemon",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.6.4",
    libraryDependencies ++= Seq(
      "org.http4s"           %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"           %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"           %% "http4s-circe"        % Http4sVersion,
      "org.http4s"           %% "http4s-dsl"          % Http4sVersion,
      "org.scalameta"        %% "munit"               % MunitVersion           % Test,
      "org.typelevel"        %% "munit-cats-effect"   % MunitCatsEffectVersion % Test,
      "ch.qos.logback"       %  "logback-classic"     % LogbackVersion         % Runtime,
      "io.jsonwebtoken"      % "jjwt"                 % JwtVersion,
      "javax.xml.bind"       % "jaxb-api"             % JaxbVersion,
      "org.tpolecat"         %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"         %% "doobie-hikari"       % DoobieVersion,
      "mysql"                % "mysql-connector-java" % MysqlVersion,
      "org.mindrot"          % "jbcrypt"              % BcryptVersion,
      "org.fusesource.jansi" % "jansi"                % JansiVersion,
      "io.circe"             %% "circe-generic"       % CirceGenericVersion,
      "io.circe"             %% "circe-yaml"          % CirceYamlVersion
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )

lazy val frontend = (project in file ("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(    
    scalaVersion := "3.6.4",
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"           % TyrianVersion,      
      "org.scalameta"   %%% "munit"               % MunitVersion % Test,
      "io.circe"        %%% "circe-core"          % CirceGenericVersion,
      "io.circe"        %%% "circe-generic"       % CirceGenericVersion,
      "io.circe"        %%% "circe-parser"        % CirceGenericVersion      
    )    
  )

lazy val root = project
  .in(file("."))
  .aggregate(backend, frontend)
