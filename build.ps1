Write-Host "Compiling LeakWatch..."
New-Item -ItemType Directory -Force -Path out\main, out\test | Out-Null
$mainSources = Get-ChildItem -Recurse -Path src\main -Filter *.java | ForEach-Object { $_.FullName }
javac -d out\main $mainSources
$testSources = Get-ChildItem -Recurse -Path src\test -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "out\main;lib\junit-platform-console-standalone-1.10.2.jar" -d out\test $testSources
Write-Host "Running tests..."
java -jar lib\junit-platform-console-standalone-1.10.2.jar execute -cp "out\main;out\test" --scan-classpath --details=tree
