@echo off
cd /d %~dp0

echo ���ڼ�� Node.js �Ƿ��Ѱ�װ...
where node >nul 2>nul
if errorlevel 1 (
    echo [����] δ��⵽ Node.js����� https://nodejs.org ���ز���װ��
    pause
    exit /b
)
echo Node.js �Ѱ�װ��


:: ��� axios �Ƿ��Ѱ�װ
echo ���ڼ�� axios �Ƿ��Ѱ�װ...
if not exist node_modules\axios (
    echo δ��⵽ axios�������Զ���װ axios...
    npm install axios
    if errorlevel 1 (
        echo [����] axios ��װʧ�ܣ��������硣
        pause
        exit /b
    )
) else (
    echo axios �Ѱ�װ��
)

:: ����ǰ�˷���
echo ����ǰ�˿�����������...
start powershell -ExecutionPolicy Bypass -Command "npm start"

timeout /t 5 >nul
start http://localhost:3000

pause
