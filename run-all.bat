@echo off
echo 正在启动后端服务...
start java -jar ..\AmbulanceDispatchSystem\target\JavaWebLearning-0.0.1-SNAPSHOT.jar

timeout /t 3 >nul

echo 正在启动前端服务...
start ..\AmbulanceDispatchSystem\ambulance-frontend\start-frontend.bat

pause
