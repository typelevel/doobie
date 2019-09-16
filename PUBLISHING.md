
## Publishing

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



