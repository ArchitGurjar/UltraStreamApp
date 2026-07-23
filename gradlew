#!/bin/sh
# This is a stub. It will download the wrapper if not present.
# We'll rely on the wrapper jar we already downloaded.
exec java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
