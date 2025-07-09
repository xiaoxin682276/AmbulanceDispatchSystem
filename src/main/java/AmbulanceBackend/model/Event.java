package AmbulanceBackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Event implements Comparable<Event> {
    public enum Type { CALL, ARRIVE_PATIENT, ARRIVE_HOSPITAL, RETURN_BASE ,NEW_PATIENTS}

    public int time;
    public Type type;
    public int patientId;
    public int ambulanceId;

    public Event(int time, Type type, int patientId, int ambulanceId) {
        this.time = time;
        this.type = type;
        this.patientId = patientId;
        this.ambulanceId = ambulanceId;
    }

    @Override
    public int compareTo(Event o) {
        return Integer.compare(this.time, o.time);
    }
}
