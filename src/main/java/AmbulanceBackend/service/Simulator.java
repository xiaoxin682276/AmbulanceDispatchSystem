package AmbulanceBackend.service;

import AmbulanceBackend.model.Ambulance;
import AmbulanceBackend.model.Event;
import AmbulanceBackend.model.Event.Type;
import AmbulanceBackend.model.Hospital;
import AmbulanceBackend.model.Patient;
import AmbulanceBackend.model.CityMap;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class Simulator {

    @Getter
    private volatile int currentTime = 0;

    private PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    @Getter
    private List<Ambulance> ambulances = new ArrayList<>();
    private Map<Integer, Patient> patients = new ConcurrentHashMap<>();
    @Getter
    private List<Hospital> hospitals = new ArrayList<>();
    private CityMap cityMap;
    private int patientIdCounter = 0;

    private AtomicBoolean running = new AtomicBoolean(false);
    private Thread simulationThread;
    private long startRealTimeMillis;
    private int simulationSpeed = 1;  // 模拟速度倍率，1表示1秒真实时间对应1秒模拟时间

    private int nextCallTime = 1;  // 下一个新增病人呼叫时间点
    private final int callInterval = 5;  // 病人呼叫间隔（秒）

    /**
     * 初始化模拟器
     */
    public void init(Map<String, Object> config) {
        currentTime = 0;
        eventQueue.clear();
        patients.clear();
        hospitals.clear();
        ambulances.clear();
        patientIdCounter = 0;

        // 读取前端传来的配置参数，注意类型转换
        int hospitalCount = 2; // 默认2个医院
        int totalAmbulances = 4; // 默认4辆救护车

        if (config.containsKey("hospitals")) {
            hospitalCount = Integer.parseInt(config.get("hospitals").toString());
        }
        if (config.containsKey("ambulances")) {
            totalAmbulances = Integer.parseInt(config.get("ambulances").toString());
        }

        // 简单地设定城市点数，建议根据hospitalCount动态调整或前端传入
        int cityPoints = Math.max(hospitalCount * 2, 6);
        cityMap = new CityMap(cityPoints);

        // 这里简单连通点，建议根据城市规模复杂设计路径
        for (int i = 0; i < cityPoints - 1; i++) {
            cityMap.addEdge(i, i + 1, 5); // 简单链路，每条边权重5
        }

        // 初始化医院，均匀分布在城市点上
        for (int i = 0; i < hospitalCount; i++) {
            Hospital h = new Hospital();
            h.setId(i);
            h.setLocation(i * 2); // 简单间隔2个点
            hospitals.add(h);
        }

        // 按医院分配救护车数量，均分
        int ambulancesPerHospital = totalAmbulances / hospitalCount;
        int ambulanceIdCounter = 0;

        for (Hospital h : hospitals) {
            for (int i = 0; i < ambulancesPerHospital; i++) {
                Ambulance a = new Ambulance();
                a.setId(ambulanceIdCounter++);
                a.setLocation(h.getLocation());
                a.setState(Ambulance.State.IDLE);
                a.setHospitalId(h.getId());

                ambulances.add(a);
                h.getIdleAmbulances().add(a);
            }
        }

        // 如果救护车数量不能被医院数整除，多出来的救护车放第一个医院
        int remaining = totalAmbulances - ambulancesPerHospital * hospitalCount;
        for (int i = 0; i < remaining; i++) {
            Ambulance a = new Ambulance();
            a.setId(ambulanceIdCounter++);
            a.setLocation(hospitals.get(0).getLocation());
            a.setState(Ambulance.State.IDLE);
            a.setHospitalId(hospitals.get(0).getId());

            ambulances.add(a);
            hospitals.get(0).getIdleAmbulances().add(a);
        }
// 首次定时病人生成事件，设定5时刻开始，每隔5单位时间生成新病人
        eventQueue.add(new Event(5, Type.NEW_PATIENTS, -1, -1));

        // 你可以根据需求新增初始病人呼叫事件，或者前端另触发新增病人
    }


    /**
     * 启动模拟线程
     */
    public synchronized void startSimulation() {
        if (running.get()) return;
        running.set(true);
        startRealTimeMillis = System.currentTimeMillis();
        currentTime = 0;

        simulationThread = new Thread(() -> {
            try {
                while (running.get()) {
                    long elapsedMillis = System.currentTimeMillis() - startRealTimeMillis;
                    int targetSimTime = (int) (elapsedMillis / 1000L) * simulationSpeed;

                    // 新增病人呼叫事件
                    while (nextCallTime <= targetSimTime) {
                        addNewPatientCall(nextCallTime);
                        nextCallTime += callInterval;
                    }

                    // 处理事件队列中当前模拟时间已到的事件
                    while (!eventQueue.isEmpty() && eventQueue.peek().getTime() <= targetSimTime) {
                        Event e = eventQueue.poll();
                        currentTime = e.getTime();
                        handleEvent(e);
                    }

                    currentTime = targetSimTime;

                    Thread.sleep(200);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /**
     * 停止模拟
     */
    public synchronized void stopSimulation() {
        running.set(false);
        if (simulationThread != null) {
            simulationThread.interrupt();
            simulationThread = null;
        }
    }

    /**
     * 新增病人呼叫事件
     */
    private void addNewPatientCall(int callTime) {
        int loc = new Random().nextInt(6);
        Patient p = new Patient();
        p.setId(patientIdCounter++);
        p.setCallTime(callTime);
        p.setLocation(loc);
        p.setWaitStartTime(callTime);
        patients.put(p.getId(), p);

        eventQueue.add(new Event(callTime, Type.CALL, p.getId(), -1));
    }

    /**
     * 事件分发处理
     */
    private void handleEvent(Event e) {
        switch (e.getType()) {
            case CALL -> handleCall(e);
            case ARRIVE_PATIENT -> handleArrivePatient(e);
            case ARRIVE_HOSPITAL -> handleArriveHospital(e);
            case RETURN_BASE -> handleReturnBase(e);
        }
    }

    /**
     * 处理病人呼叫事件，分配最近空闲救护车
     */
    private void handleCall(Event e) {
        Patient p = patients.get(e.getPatientId());
        Ambulance assigned = null;
        int minTime = Integer.MAX_VALUE;

        for (Ambulance amb : ambulances) {
            if (amb.getState() == Ambulance.State.IDLE) {
                int time = cityMap.shortestPath(amb.getLocation(), p.getLocation());
                if (time < minTime) {
                    minTime = time;
                    assigned = amb;
                }
            }
        }

        if (assigned != null) {
            assigned.setState(Ambulance.State.TO_PATIENT);
            assigned.setPatientId(p.getId());
            p.setAssignedAmbulance(assigned.getId());

            // 救护车从当前位置前往病人位置
            eventQueue.add(new Event(currentTime + minTime, Type.ARRIVE_PATIENT, p.getId(), assigned.getId()));
        }
    }

    /**
     * 救护车抵达病人位置，准备送往医院
     */
    private void handleArrivePatient(Event e) {
        Ambulance amb = ambulances.get(e.getAmbulanceId());
        Patient p = patients.get(e.getPatientId());

        amb.setState(Ambulance.State.TO_HOSPITAL);
        amb.setLocation(p.getLocation());

        // 选择最近医院
        Hospital nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Hospital h : hospitals) {
            int dist = cityMap.shortestPath(p.getLocation(), h.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = h;
            }
        }

        p.setDestination(nearest.getLocation());

        // 救护车前往医院
        eventQueue.add(new Event(currentTime + minDist, Type.ARRIVE_HOSPITAL, p.getId(), amb.getId()));
    }

    /**
     * 救护车到达医院，更新病人状态，救护车准备返回
     */
    private void handleArriveHospital(Event e) {
        Ambulance amb = ambulances.get(e.getAmbulanceId());
        Patient p = patients.get(e.getPatientId());

        amb.setState(Ambulance.State.RETURNING);
        amb.setLocation(p.getDestination());

        p.setState(Patient.State.ARRIVED);
        p.setArriveTime(currentTime);
        p.setTotalTime(currentTime - p.getCallTime());

        // 计算救护车返回基地时间
        Hospital homeHospital = hospitals.stream()
                .filter(h -> h.getId() == amb.getHospitalId())
                .findFirst()
                .orElse(null);

        if (homeHospital != null) {
            int backTime = cityMap.shortestPath(amb.getLocation(), homeHospital.getLocation());
            eventQueue.add(new Event(currentTime + backTime, Type.RETURN_BASE, -1, amb.getId()));
        }
    }

    /**
     * 救护车返回基地，恢复空闲状态
     */
    private void handleReturnBase(Event e) {
        Ambulance amb = ambulances.get(e.getAmbulanceId());

        Hospital homeHospital = hospitals.stream()
                .filter(h -> h.getId() == amb.getHospitalId())
                .findFirst()
                .orElse(null);

        if (homeHospital != null) {
            amb.setState(Ambulance.State.IDLE);
            amb.setPatientId(null);
            amb.setLocation(homeHospital.getLocation());
            homeHospital.getIdleAmbulances().add(amb);
        }
    }

    /**
     * 获取当前所有病人
     */
    public Collection<Patient> getPatients() {
        return patients.values();
    }

    /**
     * 获取当前状态，含中文状态描述
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("time", currentTime);

        List<Map<String, Object>> ambList = new ArrayList<>();
        for (Ambulance a : ambulances) {
            Map<String, Object> ambMap = new HashMap<>();
            ambMap.put("id", a.getId());
            ambMap.put("location", a.getLocation());
            ambMap.put("state", a.getState().name());
            ambMap.put("stateDesc", ambulanceStateDesc(a.getState()));
            ambMap.put("hospitalId", a.getHospitalId());
            ambMap.put("patientId", a.getPatientId());
            ambList.add(ambMap);
        }
        status.put("ambulances", ambList);

        List<Map<String, Object>> patientList = new ArrayList<>();
        for (Patient p : patients.values()) {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("id", p.getId());
            pMap.put("location", p.getLocation());
            pMap.put("state", p.getState().name());
            pMap.put("stateDesc", patientStateDesc(p.getState()));
            pMap.put("callTime", p.getCallTime());
            pMap.put("assignedAmbulance", p.getAssignedAmbulance());
            pMap.put("totalTime", p.getTotalTime());
            patientList.add(pMap);
        }
        status.put("patients", patientList);

        status.put("hospitals", hospitals);

        return status;
    }

    /**
     * 救护车状态中文说明
     */
    private String ambulanceStateDesc(Ambulance.State state) {
        return switch (state) {
            case IDLE -> "空闲";
            case TO_PATIENT -> "前往病人";
            case TO_HOSPITAL -> "送往医院";
            case RETURNING -> "返回基地";
        };
    }

    /**
     * 病人状态中文说明
     */
    private String patientStateDesc(Patient.State state) {
        return switch (state) {
            case WAITING -> "等待救援";
            case PICKED_UP -> "已接送";
            case ARRIVED -> "已送达医院";
        };
    }

    /**
     * 获取模拟总结信息，比如完成病人数和平均时间
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        long completedCount = patients.values().stream()
                .filter(p -> p.getState() == Patient.State.ARRIVED)
                .count();
        double avgTime = patients.values().stream()
                .filter(p -> p.getState() == Patient.State.ARRIVED)
                .mapToInt(Patient::getTotalTime)
                .average()
                .orElse(0);
        summary.put("completed", completedCount);
        summary.put("avgTime", avgTime);
        return summary;
    }
}
