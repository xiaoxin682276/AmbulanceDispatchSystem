package AmbulanceBackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;

@Setter
@Getter
public class Hospital {
    public int id;
    public int location;
    public Queue<Ambulance> idleAmbulances = new LinkedList<>();

}