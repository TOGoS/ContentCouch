#!/bin/sh

find src -name '*.java' >.src-files.lst
javac -d bin @.src-files.lst