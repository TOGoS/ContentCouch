#!/bin/sh

# Assumes build.sh has already been run, yo.

rm -rf jar
mkdir -p jar/META-INF

cd jar
cp -al ../web/WEB-INF/classes/* .
unzip ../ext-lib/togos.mf-latest.jar

echo "Manifest-Version: 1.0" >META-INF/MANIFEST.MF
echo "Main-Class: contentcouch.app.ContentCouchCommand" >>META-INF/MANIFEST.MF

zip -r - . >../ContentCouch.jar
