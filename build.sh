#!/usr/bin/env bash
set -e
echo "Compiling LeakWatch..."
mkdir -p out/main out/test
javac -d out/main $(find src/main -name "*.java")
if [[ "$(uname -s)" == MINGW* || "$(uname -s)" == MSYS* ]]; then SEP=";"; else SEP=":"; fi
javac -cp "out/main${SEP}lib/junit-platform-console-standalone-1.10.2.jar" -d out/test $(find src/test -name "*.java")
echo "Running tests..."
java -jar lib/junit-platform-console-standalone-1.10.2.jar execute -cp "out/main${SEP}out/test" --scan-classpath --details=tree
