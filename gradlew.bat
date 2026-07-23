@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=
set JAVA_EXE=java
if not defined JAVA_HOME goto :findJavaHome
set JAVA_EXE=%JAVA_HOME%/bin/java
goto :javaExec
:findJavaHome
echo JAVA_HOME not set, using system java
goto :javaExec
:javaExec
"%JAVA_EXE%" -cp "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
