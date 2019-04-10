# Testing with Docker and Bazel

So you joined the Bazel family but now your builds are failing on missing docker tar files? This doc is for you.

## Basic config for consuming docker images
#### Or, `tar file for image with name [...] was not found`
Bazel tests are hermetic. We cannot run `docker pull` during our tests because we cannot access the internet.
Instead, we declare our images as dependencies of our tests and trust Bazel to provide the image as a tar file on disk. Our docker images become first-class build dependencies.

In order to set this up for your images, add a [target override](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/overrides.md#internaltargetsoverrides) to any test target that uses docker images. For example, a test that requires a wix-custom cassandra container, a standard mysql container, and a standard redis container, might look like this:
```json
{
  "targetOverrides": [
    {
      "label": "//bi-modules/wix-page-view-reporting-testapps/wix-page-view-reporting-bootstrap-testapp/src/it/scala/com/wixpress/bi:bi",
      "dockerImagesDeps":["docker-repo.wixpress.com/com.wixpress.test.dockerized-cassandra-image-3.0.9:rc", "mysql:5.7", "redis:4.0.11"]
    }
  ]
}
```
When you have this, the next time the migrator runs, it will generate the necessary Bazel config to download your images from the internet during the fetch phase of the build. The image will be available to your tests as a tar file.

## Important note about version tags

Do _not_ use tags such as `snapshot` and `latest`. These do not behave like maven snapshots anyway and have no special meaning to Bazel, so they will only confuse you. Use explicit version tags.


## Using docker tar images in tests

Now that Bazel takes care of the `docker pull` for us, we need to load our images slightly differently.  

The Framework's docker-testkit already has [built-in Bazel support](https://github.com/wix-private/server-infra/tree/master/framework/docker-testkit#bazel), so if you're using it, you do not need to change anything to be able to load your images from the tar files.  

If you insist on using your own solution, here's what you need to know:
1. Instead of `docker pull [image]`, use [`docker load -i [tar file]`](https://docs.docker.com/engine/reference/commandline/load/)
1. The name of the tar file is based on the image name, like so: `[repository]_[tag].tar` with any slashes replaced by underscores. E.g. `docker-repo.wixpress.com/com.wixpress.bla:1.23.0` will become `com_wixpress_bla_1.23.0.tar`. Note the registry part is omitted. `mysql:5.7` becomes `mysql_5.7.tar`.
1. The tar file will be located on disk along side the test class. It can be nested under a sibling directory, so you can find it by walking the file tree starting from your working directory.
