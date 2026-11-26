@echo off
title SRM AP LFM - Project Running
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-app.ps1"
if errorlevel 1 pause
