name := "lagom13-java-service-locator-dns"

libraryDependencies ++= Seq(
  Library.lagom13JavaClient,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"
)

resolvers += Resolver.hajile
