@echo off

:rm -rf web\WEB-INF\lib
:rm -rf web\WEB-INF\classes
:cp -r bin web\WEB-INF\classes
java -jar ext-lib\winstone-0.9.10.jar web --commonLibFolder=ext-lib --useServletReloading=true
