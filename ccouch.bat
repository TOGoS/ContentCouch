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

java -cp %~dp0web\WEB-INF\classes contentcouch.app.ContentCouchCommand -repo:junk %~dp0junk-repo %A1% %A2% %A2% %A3% %A4% %A5% %A6% %A7% %A8% %A9% %A10% %A11% %A12% %A13% %A14% %A15% %A16% %A17% %A18% %A19% %A20%
