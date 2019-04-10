# How to get slack notifications?
Successfully managed to pass one of the migration phases? Great! Want to keep it green? Here is how: 

You can get notifications about __**regression**__ and __**fixed**__ events of the following jobs:
- Migrate
- Run Bazel
- Run Bazel on Google (remote)
- Run Maven
- Compare tests between bazel and maven

Notification will be send to slack channels that are listed in file: **`bazel_migration/slack_channels.txt`**.
The content of the file is list of slack channels (without `#` sign), comma delimited.

**For instance:**

   |```wixos,users    ```|
   |---------------------------------------|
   
   

**Note:** Please do not add `bazel-migrate-support` to the channel but your own private channels
