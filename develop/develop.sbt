val downloadBitcoind = Projects.`ln-vortex` / TaskKeys.downloadBitcoind

val downloadLnd = Projects.`ln-vortex` / TaskKeys.downloadLnd

Compile / compile := (Compile / compile)
  .dependsOn(downloadBitcoind, downloadLnd)
  .value

run / fork := true

mainClass := Some("com.lnvortex.develop.CreateLocalDevEnvironment")
