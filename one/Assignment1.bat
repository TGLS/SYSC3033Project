@echo off
:: This works by calling three batch programs to run the java command and pause
:: if the java program terminates.
start a1Server.bat
start a1Intermediate.bat
start a1Client.bat