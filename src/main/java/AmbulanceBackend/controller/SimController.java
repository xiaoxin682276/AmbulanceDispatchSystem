package AmbulanceBackend.controller;

import AmbulanceBackend.model.Ambulance;
import AmbulanceBackend.model.Hospital;
import AmbulanceBackend.model.Patient;
import AmbulanceBackend.service.Simulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SimController {

    @Autowired
    private Simulator simulator;

    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody Map<String, Object> config) {
        System.out.println("收到前端参数: " + config);
        simulator.init(config);   // 把参数传给模拟器初始化
        simulator.startSimulation();
        return ResponseEntity.ok("模拟启动成功");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopSim() {
        simulator.stopSimulation();
        return ResponseEntity.ok("模拟已停止");
    }

    @PostMapping("/restart")
    public ResponseEntity<String> restartSimulation(@RequestBody Map<String, Object> config) {
        simulator.stopSimulation();    // 停止现有模拟
        simulator.init(config);        // 使用新参数重新初始化
        simulator.startSimulation();   // 重新启动模拟
        return ResponseEntity.ok("模拟重启成功");
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return simulator.getStatus();
    }


    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return simulator.getSummary();
    }
    public int getCurrentTime() {
        return simulator.getCurrentTime();
    }

    public List<Ambulance> getAmbulances() {
        return simulator.getAmbulances();
    }

    public Collection<Patient> getPatients() {
        return simulator.getPatients();
    }

    public List<Hospital> getHospitals() {
        return simulator.getHospitals();
    }

}

