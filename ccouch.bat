@echo off

java -cp %~dp0web\WEB-INF\classes contentcouch.app.ContentCouchCommand -repo %~dp0junk-repo %1 %2 %3 %4 %5 %6 %7 %8 %9
