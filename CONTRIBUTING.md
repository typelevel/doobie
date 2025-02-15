# Developing Doobie

Doobie is a multi-module SBT project.
There is some code generation (project/FreeGen2.scala) which generates the free algebra (e.g. kleisliinterpreter.scala)
from JDBC driver classes definitions.

Running the tests or building the documentation site requires connection to the example Postgres docker contains,
which you can spin up using docker-compose:

```
docker compose up -d --force-recreate
```

Note: If you're using Apple Silicone Macbooks (M1, M2, etc), you need to enable "Use Rosetta for x86_64/amd64 emulation on Apple Silicon" since there is no ARM64 image for postgis yet.

With the containers started, SBT you can run `test` to run tests, and `makeSite` to build the doc site.

## Fixing warnings

To improve code quality and bug, we enable many stricter scala compiler flags via the
[sbt-tpolecat](https://github.com/typelevel/sbt-tpolecat) plugin and in CI all warnings will be treated as errors.

For a more pleasant development experience, we default to `tpolecatDevMode` so warnings do not cause compilation errors.
You can use the sbt command `tpolecatCiMode` to enable strict mode and help catch any warnings you missed.

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

In Github Releases, create and publish new release with format v1.2.3. (This creates an annotated tag)

When publishing docs, ensure that the version in the sbt build is set to a "nice" tag.
This can be achieved by either checking the release itself (`git checkout v1.2.3`)
or explicitly overriding the version in sbt using `set ThisBuild / version := "1.2.3"`

After the version is set correctly:
```
# Make the git worktree where gh-page branch will be checked out and docs copied over
git fetch && git worktree add doc_worktree gh-pages
sbtn makeSite ghpagesSynchLocal 
# Check doc changes are expected
sbtn ghpagesPushSite
```
