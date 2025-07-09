package AmbulanceBackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Ambulance {
    public enum State { IDLE, TO_PATIENT, TO_HOSPITAL, RETURNING }

    public int id;
    public int location;
    public State state;
    public int hospitalId;
    public Integer patientId = null;
    public int timeToNextEvent;

}
