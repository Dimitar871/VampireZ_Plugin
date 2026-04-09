@echo off
echo Building plugin...
call mvn clean package

if %errorlevel% neq 0 (
    echo Build failed!
    exit /b %errorlevel%
)

echo Copying plugin to server...
copy /Y target\VampireZ-1.0.0.jar ..\test-server\plugins\

if %errorlevel% neq 0 (
    echo Copy failed!
    exit /b %errorlevel%
)

echo Plugin deployed successfully!
echo Starting server...
cd ..\test-server
java -jar paper-1.21.11-69.jar
