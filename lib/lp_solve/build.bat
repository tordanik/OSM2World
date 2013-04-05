@echo off

REM ----------------------------------------------------------------
REM This is a build file for the lp_solve Java wrapper stub library
REM on Windows platforms.
REM
REM Requirements:
REM
REM - Microsoft Visual C++ compiler (I used V 7, others might work)
REM - Visual Studio envirement variables must be set.
REM - Java Development Kit 1.4.x installed
REM - JAVA_HOME environment variable set
REM - lp_solve windows archive lp_solve_5.5_dev.zip
REM
REM Change the paths below this line and you should be ready to go!
REM ----------------------------------------------------------------

REM -- Set the path to the lp_solve directories here !
REM --

set c=cl

REM determine platform (win32/win64)
echo main(){printf("SET PLATFORM=win%%d\n", (int) (sizeof(void *)*8));}>platform.c
%c% /nologo platform.c /Feplatform.exe
del platform.c
platform.exe >platform.bat
del platform.exe
call platform.bat
del platform.bat

if not exist %PLATFORM%\*.* md %PLATFORM%

REM -- Here we go !
REM --
set SRC_DIR=..\src\c
set OPTIONS=/LD /O2 /Gz /MT
set DEFINES=-DWIN32
REM The 64 bit java sdk also uses win32 as folder
set INCLUDES=-I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" -I %LPSOLVE_DIR% -I %SRC_DIR%
set LIBS=%LPSOLVE_DIR%\lpsolve55\bin\%PLATFORM%\lpsolve55.lib
set SRCFILES=%SRC_DIR%\lpsolve5j.cpp

cl %OPTIONS% %DEFINES% %INCLUDES% %LIBS% %SRCFILES% /Fe%PLATFORM%\lpsolve55j.dll

if exist *.obj del *.obj

set PLATFORM=