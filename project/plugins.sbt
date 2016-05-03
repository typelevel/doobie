resolvers += Resolver.url(
  "tpolecat-sbt-plugin-releases",
    url("http://dl.bintray.com/content/tpolecat/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("org.tpolecat"      % "tut-plugin"   % "0.4.2")
addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"   % "0.3.1")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"      % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"  % "1.0.0")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype" % "0.5.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-osgi"     % "0.8.0")
addSbtPlugin("org.scalastyle"      %% "scalastyle-sbt-plugin" % "0.8.0")
