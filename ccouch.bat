@echo off

java -cp %~dp0web\WEB-INF\classes;%dp0ext-lib\togos.mf-2009.08.07b.jar contentcouch.app.ContentCouchCommand -repo %~dp0junk-repo %*
