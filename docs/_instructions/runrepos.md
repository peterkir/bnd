---
layout: default
title: -runrepos REPO-NAME ( ',' REPO-NAME )*
class: Resolve
summary: |
   Order and select the repository for resolving against. The default order is all repositories in their plugin creation order.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runrepos=Maven Central, Main, Distro, ...`

- Pattern: `.*`

<!-- Manual content from: ext/runrepos.md --><br /><br />

The `-runrepos` instruction is used to restrict or order the available repositories. A `bndrun` file (see [Resolving](/chapters/250-resolving.html#resolving-in-bndtools)) can be based on a workspace or can be standalone. In the workspace case the, the repositories are defined in `build.bnd` or in a `*.bnd` file in the `cnf/ext` directory as bnd plugins. In the standalone case the repositories are either OSGi XML repositories listed in the `-standalone` instruction or they are also defined as plugins but now in the `bndrun` file.

In both cases there is an _ordered_ list of repositories. In the `-standalone` it is easy to change this order or exclude repositories. However, in the workspace case this is harder because the set of repositories is shared with many other projects. The `-runrepos` can then be used to exclude and reorder the list repositories. It simply lists the names of the repositories in the desired order. Each repository has its own name.

If `-runrepos` is ommited then all repositories having either no tags or the tag `resolve` will be included for resolution.
You can exclude certain repositories by assigning it a tag different than `resolve` (e.g. `<<EMTPY>>` or `foobar`). See [Tagging of repository plugins](/chapters/870-plugins.html#tagging-of-repository-plugins) for more details.


**Note** The name of a repository is not well defined. It is either the name of the repository or the `toString()` result. In the later case the name is sometimes a bit messy.

**Example 1: include specific repos**

	-runrepos: Maven Central, Main, Distro

This includes exactly the three repositories.

**Example 2: include all repos**

- remove / ommit the `-runrepos` instruction
- give all repositories either no tag or the tag `resolve`

e.g. a `.bndrun` file without  `-runrepos`  would include the following repos in the resolution:


```
aQute.bnd.repository.maven.provider.MavenBndRepository;\
        tags = "resolve"; \
        name="Maven Central A";\
        releaseUrl="${mavencentral}";\
        snapshotUrl="${ossrh}";\
        index="${.}/central.mvn";\
        readOnly=true,\
```

because it has the `resolve` tag

and also


```
aQute.bnd.repository.maven.provider.MavenBndRepository;\
        name="Maven Central B";\
        releaseUrl="${mavencentral}";\
        snapshotUrl="${ossrh}";\
        index="${.}/central.mvn";\
        readOnly=true,\
```

because there is no tags at all.

**Example 3: include all repos, except some**

- remove / ommit the `-runrepos` instruction
- give all repositories either no tag or the tag `resolve` which should be included
- give the repo which should be excluded the tag `<<EMTPY>>` or something else than `resolve` e.g. `foobar`

For example the following repository definition in `/cnf/build.bnd` would be excluded:

```
aQute.bnd.repository.maven.provider.MavenBndRepository;\
        tags = "<<EMPTY>>"; \
        name="Maven Central";\
        releaseUrl="${mavencentral}";\
        snapshotUrl="${ossrh}";\
        index="${.}/central.mvn";\
        readOnly=true,\
```

because it has a `<<EMPTY>>` tag (and thus no `resolve` tag).

An example use case is exclude the baseline-repository from resolution. In the case, you would not add the `resolve` tag to the baseline-repo, and then it won't be considered for resolution in a `.bndrun`.
