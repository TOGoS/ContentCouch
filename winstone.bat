@echo off

:rm -rf web\WEB-INF\lib
:rm -rf web\WEB-INF\classes
:cp -r bin web\WEB-INF\classes
java -cp bin -jar F:\downloads\winstone-0.9.10.jar web --useServletReloading=true
