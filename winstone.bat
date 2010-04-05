@echo off

:rm -rf web\WEB-INF\lib
:rm -rf web\WEB-INF\classes
:cp -r bin web\WEB-INF\classes
java -jar "%~dp0ext-lib\winstone-0.9.10.jar" "%~dp0web" --commonLibFolder="%~dp0ext-lib" --useServletReloading=true %*
