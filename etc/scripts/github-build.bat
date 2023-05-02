@REM
@REM Copyright (c) 2023 Oracle and/or its affiliates.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
@REM call %~dp0\includes\pipeline-env.bat %~dp0 '..\..'

cd %~dp0\..\..

mvn %MAVEN_ARGS% -f %cd%\pom.xml ^
    install -e ^
    -Dmaven.test.failure.ignore=false ^
    -Pjavadoc,sources,tests