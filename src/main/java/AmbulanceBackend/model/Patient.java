package AmbulanceBackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Patient {
    public enum State { WAITING, PICKED_UP, ARRIVED }

    private int id;
    private int callTime;          // 呼叫时间
    private int location;          // 发生事件的地点
    private int destination;       // 送往的医院节点编号

    private State state = State.WAITING;
    private Integer assignedAmbulance = null;

    private int waitStartTime;     // 等待开始时间（通常等于 callTime）
    private int pickupTime = -1;   // 被接上时间
    private int arriveTime = -1;   // 到达医院时间
    private int totalTime = -1;    // 整体耗时（= arriveTime - callTime）

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", location=" + location +
                ", destination=" + destination +
                ", state=" + state +
                ", assignedAmbulance=" + assignedAmbulance +
                ", totalTime=" + totalTime +
                '}';
    }
}
