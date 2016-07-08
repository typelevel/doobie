
> *These notes are for Rob ... right now nobody else can publish.*

### Publishing a Snapshot

To publish a snapshot build all that is needed is:

```
> +publish
```

### Publishing a Release

This is more involved :-\

- Create a staging branch from the series tip:

```
git checkout -b 0.2.4-staging
```

- Edit `README.md`, `CHANGELOG.md`, and `01-Introduction.md` to ensure that the versions, etc. are correct, notable changes are listed, and contributors are credited. Commit these changes.

- Stage the tut doc in `tpolecat.github.io`:

```
> tut
...
> ctut
```

- Remove the `-SNAPSHOT` suffix from the tut doc root and then update `_config.yml`. Remove the warning block from the `00-index.md`.

- Stage scaladoc as well:

```
> unidoc

...

doobie (0.2.4-staging)$ mkdir ../tpolecat.github.io/doc/doobie/0.2.4
doobie (0.2.4-staging)$ mv target/scala-2.11/unidoc ../tpolecat.github.io/doc/doobie/0.2.4/api
```

- Update the version in `projects.html`.

- Commit and push the doc. Ensure that links from `projects.html` work, and that it looks generally ok. Note that source links won't work from the scaladoc until the tag is created.

- Commit and push the staging branch and ensure that doc links work and go to the correct versions. The `CHANGELOG` link will be wrong.

- Attempt to release. 
```
> release cross
```

- If all goes well, which is unlikely, it will ask for PGP keys. These are in a secure note in your keychain called "Sonatype/SBT PGP Keys". Eventually the build will crap out after pushing the branch. Finish up with:

```
> sonatypeReleaseAll
```

- Push the release branch, open a PR and merge.

- Have a cocktail.
