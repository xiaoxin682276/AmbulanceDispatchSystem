@echo off
echo ����������˷���...
start java -jar ..\AmbulanceDispatchSystem\target\JavaWebLearning-0.0.1-SNAPSHOT.jar

timeout /t 3 >nul

echo ��������ǰ�˷���...
start ..\AmbulanceDispatchSystem\ambulance-frontend\start-frontend.bat

pause
