#!/bin/sh

find src -name '*.java' >.src-files.lst
javac -d bin @.src-files.lst -target 1.4 -source 1.4
