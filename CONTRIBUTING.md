# Developing Doobie

Doobie is a multi-module SBT project.
There is some code generation (project/FreeGen2.scala) which generates the free algebra (e.g. kleisliinterpreter.scala)
from JDBC driver classes definitions.

Running the tests or building the documentation site requires connection to the example Postgres docker contains,
which you can spin up using docker-compose:

```
docker compose up -d --force-recreate
```

Note: If you're using Apple Silicone Macbooks, you need to enable "Use Rosetta for x86_64/amd64 emulation on Apple Silicon" since there is no ARM64 image for postgis yet.

After that, in SBT you can run `test` to run tests, and `makeSite` to build the doc site

If you're editing code generation related code, you should reload the SBT project and then run the `freeGen2` SBT task
before compiling or running tests.

## Caveats when working on the code

## Avoiding internal cyclic module dependencies

For end users, doobie provides the aliases for high and low level APIs
such as `doobie.hi.HC`, `doobie.free.FPS`.
Due to how the module depends on one another, internally in doobie we cannot use
these aliases because it'll lead to cyclic module dependencies and cause runtime errors.

We recommend instead to use alias the imports like `doobie.hi.{connection => IHC}`
(`I` prefix stands for Internal and helps to avoid accidentally using e.g. `doobie.hi.HC`)

# Publishing

### Snapshots

doobie uses [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) which means all merges to `master` are pushed to Sonatype as snapshots and there's nothing to do.

### Releases

To make a release, make sure you're on the right commit, then tag it using the format here. This will trigger a release build.

```bash
git tag -a v1.2.3 -m v1.2.3
git push --tags
```

To update the doc site, check out the tag first.

```
git checkout v1.2.3
sbt docs/publishMicrosite
```
