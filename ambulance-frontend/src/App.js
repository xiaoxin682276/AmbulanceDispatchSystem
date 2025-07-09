import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const ambulanceStateMap = {
  IDLE: '待命',
  TO_PATIENT: '前往病人',
  TO_HOSPITAL: '送往医院',
  RETURNING: '返回基地',
};

const patientStateMap = {
  WAITING: '等待接送',
  PICKED_UP: '正在转运',
  ARRIVED: '已送达',
};

function MiniMap({ hospitals, ambulances, patients, points, maxNodes = 20 }) {
  const displayCount = Math.min(points, maxNodes);
  const radius = 150;
  const centerX = radius + 40;
  const centerY = radius + 40;
  const nodeSize = 30;

  const positions = Array.from({ length: displayCount }, (_, i) => {
    const angle = (2 * Math.PI * i) / displayCount - Math.PI / 2;
    return {
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle),
    };
  });

  return (
    <svg width={centerX * 2} height={centerY * 2} style={{ background: '#fff', borderRadius: 10, border: '1px solid #ccc' }}>
      {positions.map((pos, i) => {
        const nextPos = positions[(i + 1) % displayCount];
        return (
          <line
            key={`line-${i}`}
            x1={pos.x}
            y1={pos.y}
            x2={nextPos.x}
            y2={nextPos.y}
            stroke="#718093"
            strokeWidth={2}
          />
        );
      })}

      {positions.map((pos, i) => {
        const hosHere = hospitals.filter(h => h.location === i);
        const ambHere = ambulances.filter(a => a.location === i);
        const patHere = patients.filter(p => p.location === i);

        return (
          <g key={`node-${i}`}>
            <circle
              cx={pos.x}
              cy={pos.y}
              r={nodeSize / 2}
              fill="#00a8ff"
              stroke="#1e90ff"
              strokeWidth={2}
            />
            <text
              x={pos.x}
              y={pos.y + 5}
              fontSize="14"
              fontWeight="bold"
              fill="#fff"
              textAnchor="middle"
            >
              {i}
            </text>

            <text x={pos.x} y={pos.y - 15} fontSize="10" fill="#44bd32" textAnchor="middle">
              {hosHere.length > 0 ? `🏥${hosHere.length}` : ''}
            </text>
            <text x={pos.x} y={pos.y - 28} fontSize="10" fill="#fbc531" textAnchor="middle">
              {ambHere.length > 0 ? `🚑${ambHere.length}` : ''}
            </text>
            <text x={pos.x} y={pos.y - 40} fontSize="10" fill="#e84118" textAnchor="middle">
              {patHere.length > 0 ? `🧍${patHere.length}` : ''}
            </text>
          </g>
        );
      })}
    </svg>
  );
}



function App() {
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState(null);
  const [log, setLog] = useState('');
  const [config, setConfig] = useState({});

  const handleConfigChange = (e) => {
    setConfig({ ...config, [e.target.name]: e.target.value });
  };

const isValidConfig = () => {
  const { points, hospitals, ambulances } = config;
  return (
    points && points > 0 &&
    hospitals && hospitals > 0 &&
    ambulances && ambulances > 0
  );
};

  const startSimulation = async () => {
    if (!isValidConfig()) {
      setLog('请填写合法的正整数配置参数');
      return;
    }
    setLog('启动中...');
    try {
      await axios.post('/api/start', config);
      setRunning(true);
      setLog('模拟已启动');
    } catch (err) {
      setLog('启动失败: ' + err.message);
    }
  };

  const stopSimulation = async () => {
    setLog('正在停止...');
    try {
      await axios.post('/api/stop');
      setRunning(false);
      setLog('模拟已停止');
    } catch (err) {
      setLog('停止失败: ' + err.message);
    }
  };

  const restartSimulation = async () => {
    if (!isValidConfig()) {
      setLog('请填写合法的正整数配置参数');
      return;
    }
    setLog('正在重启...');
    try {
      await axios.post('/api/restart', config);
      setRunning(true);
      setLog('模拟已重启');
    } catch (err) {
      setLog('重启失败: ' + err.message);
    }
  };

  useEffect(() => {
    if (!running) return;

    const fetchStatus = async () => {
      try {
        const res = await axios.get('/api/status');
        setStatus(res.data);
      } catch (err) {
        setLog('获取状态失败: ' + err.message);
      }
    };

    fetchStatus();
    const intervalId = setInterval(fetchStatus, 2000);
    return () => clearInterval(intervalId);
  }, [running]);

  return (
    <div className="container">
      <h1 className="title">🚑 救护车调度模拟系统</h1>

      <div className="config-panel">
        <h2>模拟配置</h2>
        <input name="points" placeholder="地点总数" onChange={handleConfigChange} />
        <input name="hospitals" placeholder="医院数量" onChange={handleConfigChange} />
        <input name="ambulances" placeholder="救护车数量" onChange={handleConfigChange} />
        <div style={{ marginTop: '10px' }}>
          <button onClick={startSimulation} disabled={running} className="start-button">
            启动模拟
          </button>
          <button onClick={stopSimulation} disabled={!running} className="stop-button">
            停止模拟
          </button>
          <button onClick={restartSimulation} className="restart-button">
            重启模拟
          </button>
        </div>
        <p className="log">{log}</p>
      </div>

      {status && (
        <div className="status-section">
          <div className="summary">
            <h2>当前时间: {status.time}</h2>
            <p>已送达病人数量: {status.patients.filter(p => p.state === 'ARRIVED').length}</p>
          </div>

          <div className="hospital-panel">
            <h2>医院待命车辆</h2>
            {[...new Set(status.ambulances.map(a => a.hospitalId))].map(hid => (
              <div key={hid} className="hospital">
                <h3>医院 {hid}</h3>
                <ul>
                  {status.ambulances.filter(a => a.hospitalId === hid && a.state === 'IDLE').map(a => (
                    <li key={a.id}>救护车 {a.id}</li>
                  ))}
                </ul>
              </div>
            ))}
          </div>

          <div className="entity-panel">
            <h2>🚑 救护车状态</h2>
            <ul>
              {status.ambulances.map((a) => (
                <li key={a.id} className="ambulance-item">
                  🚑 救护车 {a.id} - 位置: {a.location}, 状态: {ambulanceStateMap[a.state] || a.state}
                </li>
              ))}
            </ul>

            <h2>🧍 病人状态</h2>
            <ul>
              {status.patients.map((p) => (
                <li key={p.id} className="patient-item">
                  🧍 病人 {p.id} - 位置: {p.location}, 状态: {patientStateMap[p.state] || p.state}
                </li>
              ))}
            </ul>

            <h2>城市小地图</h2>
          <MiniMap 
            hospitals={status.hospitals} 
            ambulances={status.ambulances} 
            patients={status.patients} 
            points={parseInt(config.points) || 6} 
          />
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
