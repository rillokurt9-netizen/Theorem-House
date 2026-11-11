#!/bin/sh
# Needs JDK 11+. Open http://localhost:8080
javac *.java && java ApiServer "$@"
