# migrator cheat-sheet
- Checkout Bazel build locally
```sh
git pull --rebase
git branch -a
git checkout bazel-mig-{LATEST}
```

- Create a `tree.txt` file with the deps tree, as maven interprets it:
```sh
mvn dependency:tree -Dverbose -DoutputFile=tree.txt
```

- See which 3rd-party dependencies and versions _Bazel_ resolved:
```sh
open ${PROJECT_ROOT}/third_party.bzl
```