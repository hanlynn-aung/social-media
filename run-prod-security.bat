@echo off
REM Run Spring Boot with Production + Security profiles
REM Usage: run-prod-security.bat

echo Starting Social Media API with Production and Security profiles...
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod,security"
