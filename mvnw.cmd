@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Apache Maven Wrapper startup script, version 3.3.2
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (
  @SET "BASE_DIR=%__MVNW_ARG0_NAME__%"
)
@SET WRAPPER_JAR="%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_PROPS="%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

@IF NOT EXIST %WRAPPER_JAR% (
  for /f "tokens=1,* delims==" %%A in (%WRAPPER_PROPS%) do (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  IF DEFINED WRAPPER_URL (
    curl -fsSL -o %WRAPPER_JAR% %WRAPPER_URL%
    IF ERRORLEVEL 1 (
      echo Failed to download maven-wrapper.jar 1>&2
      EXIT /B 1
    )
  )
)

for /f "tokens=1,* delims==" %%A in (%WRAPPER_PROPS%) do (
  IF "%%A"=="distributionUrl" SET DIST_URL=%%B
)

@SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
@IF NOT EXIST %MAVEN_JAVA_EXE% SET MAVEN_JAVA_EXE=java

%MAVEN_JAVA_EXE% %MAVEN_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" org.apache.maven.wrapper.MavenWrapperMain -distributionUrl %DIST_URL% %*
