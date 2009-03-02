@echo off

java -cp %~dp0web\WEB-INF\classes contentcouch.app.ContentCouchCommand -repo:junk %~dp0junk-repo %*
