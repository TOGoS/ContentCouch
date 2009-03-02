@echo off

rem Allow more than 9 arguments...
set A1=%1
shift
set A2=%1
shift
set A3=%1
shift
set A4=%1
shift
set A5=%1
shift
set A6=%1
shift
set A7=%1
shift
set A8=%1
shift
set A9=%1
shift
set A10=%1
shift
set A11=%1
shift
set A12=%1
shift
set A13=%1
shift
set A14=%1
shift
set A15=%1
shift
set A16=%1
shift
set A17=%1
shift
set A18=%1
shift
set A19=%1
shift
set A20=%1
shift

java -cp %~dp0web\WEB-INF\classes contentcouch.app.ContentCouchCommand -repo:junk %~dp0junk-repo %1 %2 %3 %4 %5 %6 %7 %8 %9
