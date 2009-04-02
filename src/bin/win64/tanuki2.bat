@echo off
@rem Nearly all the following borrowed from:
@rem http://svn.apache.org/repos/asf/geronimo/server/branches/2.1/assemblies/geronimo-boilerplate-minimal/src/main/underlay/bin/setjavaenv.bat


@REM Handle spaces in provided paths.  Also strips off quotes.
if defined var JAVA_HOME(
set JAVA_HOME=###%JAVA_HOME%###
set JAVA_HOME=%JAVA_HOME:"###=%
set JAVA_HOME=%JAVA_HOME:###"=%
set JAVA_HOME=%JAVA_HOME:###=%
@)
if defined var JRE_HOME(
set JRE_HOME=###%JRE_HOME%###
set JRE_HOME=%JRE_HOME:"###=%
set JRE_HOME=%JRE_HOME:###"=%
set JRE_HOME=%JRE_HOME:###=%
@)

@REM check that either JAVA_HOME or JRE_HOME are set
set jdkOrJreHomeSet=0
if not "%JAVA_HOME%" == "" set jdkOrJreHomeSet=1
if not "%JRE_HOME%" == "" set jdkOrJreHomeSet=1
if "%jdkOrJreHomeSet%" == "1" goto gotJdkOrJreHome
echo Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
echo At least one of these environment variable is needed to run this program
cmd /c exit /b 1
goto end

@REM If we get this far we have either JAVA_HOME or JRE_HOME set
@REM now check whether the command requires the JDK and if so
@REM check that JAVA_HOME is really pointing to the JDK files.
:gotJdkOrJreHome
set _REQUIRE_JDK=0
@rem if "%1" == "debug" set _REQUIRE_JDK=1
if "%_REQUIRE_JDK%" == "0" goto okJdkFileCheck

set jdkNotFound=0
if not exist "%JAVA_HOME%\bin\java.exe" set jdkNotFound=1
if not exist "%JAVA_HOME%\bin\javaw.exe" set jdkNotFound=1
if not exist "%JAVA_HOME%\bin\jdb.exe" set jdkNotFound=1
if not exist "%JAVA_HOME%\bin\javac.exe" set jdkNotFound=1
if %jdkNotFound% == 0 goto okJdkFileCheck
echo The JAVA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
echo NB: JAVA_HOME should point to a JDK not a JRE
cmd /c exit /b 1
goto end

:okJdkFileCheck
@REM default JRE_HOME to JAVA_HOME if not set.
if "%JRE_HOME%" == "" if exist "%JAVA_HOME%\bin\javac.exe" (set JRE_HOME=%JAVA_HOME%\jre) else set JRE_HOME=%JAVA_HOME%

@REM Set standard command for invoking Java.
@REM Note that NT requires a window name argument when using start.
@REM Also note the quoting as JAVA_HOME may contain spaces.
set _RUNJAVA="%JRE_HOME%\bin\java"
set _RUNJAVAW="%JRE_HOME%\bin\javaw"


@rem Run ${name}
start /b "" %_RUNJAVAW% -Xms${java.mem.initial}m -Xmx${java.mem.max}m -jar ${project.artifactId}-${project.version}.jar

:end

