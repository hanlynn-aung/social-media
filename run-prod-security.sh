#!/bin/bash
# Run Spring Boot with Production + Security profiles
# Usage: ./run-prod-security.sh

echo "Starting Social Media API with Production and Security profiles..."
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod,security"
