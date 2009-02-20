#!/bin/sh

find src -name '*.java' >.src-files.lst
javac -extdirs ext-lib -d bin @.src-files.lst -target 1.4 -source 1.4
