::=======================================================================
:: The below will launch a dropbox-clone application in the command line.
:: Dropbox-clone is a minimal clone of the popular Dropbox application,
:: written in Java, using Spring and AWS (Cognito and S3). For Windows,
:: place this batch file in the user's C:/Aliases folder, after adding
:: C:/Aliases to PATH.
::=======================================================================
@echo off
echo.
java -jar C:\...\GitHub\dropbox-clone\target\dropbox-clone-0.0.2.jar %*