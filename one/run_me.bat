@echo off
:: This works by calling three batch programs to run the java command and pause
:: if the java program terminates.
start Server.bat
start Intermediate.bat
start Client1.bat