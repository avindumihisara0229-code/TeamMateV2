package main.java;

import java.util.ArrayList;
import java.util.List;

public class Team {
    int id;
    List<Participant> members = new ArrayList<>();
    public Team(int id) { this.id = id; }
}
