
## Publishing - 0.5.x Branch

Despite a bunch of experimentation and reading a bunch of blog posts I still can't figure out how to do releases automatically. It's complicated as all fuck. So I'm giving up for now. It's not that hard to do it by hand. This is actually a lot simpler than it used to be.

*Right now only tpolecat can do a release. Sorry.*

#### Publishing a Snapshot

- This can be done at any time. You'll be prompted for the **gpg pass phrase**.

```
> +publishSigned
```

#### Publishing a Release

- Create a staging branch with a name like `v0.5.1-staging`.
- Make sure `CHANGELOG.md` looks ok and has a section for the release version, and make sure all contributors are recognized and thanked. Any last-minute changes can be pushed to the staging branch.
- Release. You will be prompted for the **version** and later for **gpg pass phrase**.

```
> release
```

- We can't publish the doc as an sbt-release step due to sbt-microsites [#210](https://github.com/47deg/sbt-microsites/issues/210) and related sbt limitations. So for now we need to say

```
> project docs
> set version := "<the version we just released>"
> publishMicrosite
> ^D
```

- Push the release branch, open a PR and merge.

#### Announcing the Release

- Update the Gitter room header.
- Tweet the release. Keep it simple:
  - version
  - thank contributors
  - link to repo
