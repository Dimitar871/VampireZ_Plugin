@echo off
echo Building plugin...
call mvnw.cmd clean package

if %errorlevel% neq 0 (
    echo Build failed!
    exit /b %errorlevel%
)

echo Copying plugin to server...
del /Q ..\test-server\plugins\VampireZ-*.jar 2>nul
for %%f in (target\VampireZ-*.jar) do copy /Y "%%f" ..\test-server\plugins\ >nul

if %errorlevel% neq 0 (
    echo Copy failed!
    exit /b %errorlevel%
)

echo Plugin deployed successfully!
echo Starting server...
cd ..\test-server
rem Newest paper-*.jar wins (alphabetical: paper-1.21.x before paper-26.x)
for %%j in (paper-*.jar) do set SERVER_JAR=%%j
java -jar %SERVER_JAR% --nogui
