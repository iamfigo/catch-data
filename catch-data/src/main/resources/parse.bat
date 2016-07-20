@echo off
set base_dir="%~dp0"
pushd %base_dir%
java -cp catch-data-jar-with-dependencies.jar com.huit.util.ParseData