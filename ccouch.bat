@echo off

java -cp %~dp0web\WEB-INF\classes contentcouch.app.ContentCouchCommand -repo %~dp0junk-repo %*
