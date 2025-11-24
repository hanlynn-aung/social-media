# JDK Switcher Script for PowerShell
# Usage: 
# 1. Run this script to load the functions: . .\jdk-switcher.ps1
# 2. Use the commands: Set-Java8, Set-Java17, Set-Java21

function Set-Java8 {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
    $env:PATH = "$env:JAVA_HOME\bin;" + [System.Environment]::GetEnvironmentVariable("PATH", "User") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
    Write-Host "Switched to Java 8 ($env:JAVA_HOME)" -ForegroundColor Green
    java -version
}

function Set-Java17 {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    $env:PATH = "$env:JAVA_HOME\bin;" + [System.Environment]::GetEnvironmentVariable("PATH", "User") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
    Write-Host "Switched to Java 17 ($env:JAVA_HOME)" -ForegroundColor Green
    java -version
}

# Uncomment and adjust path if you have Java 21 installed
# function Set-Java21 {
#     $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
#     $env:PATH = "$env:JAVA_HOME\bin;" + [System.Environment]::GetEnvironmentVariable("PATH", "User") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
#     Write-Host "Switched to Java 21 ($env:JAVA_HOME)" -ForegroundColor Green
#     java -version
# }

Write-Host "JDK Switcher functions loaded." -ForegroundColor Cyan
Write-Host "Type 'Set-Java8' or 'Set-Java17' to switch JDKs for this session." -ForegroundColor Cyan
