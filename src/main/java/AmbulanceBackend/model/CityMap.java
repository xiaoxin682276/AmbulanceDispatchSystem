package AmbulanceBackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class CityMap {
    private int[][] graph;
    private int size;

    public CityMap(int size) {
        this.size = size;
        graph = new int[size][size];
        for (int i = 0; i < size; i++) Arrays.fill(graph[i], Integer.MAX_VALUE);
    }

    public void addEdge(int from, int to, int weight) {
        graph[from][to] = weight;
        graph[to][from] = weight;
    }

    public int shortestPath(int start, int end) {
        int[] dist = new int[size];
        boolean[] visited = new boolean[size];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        for (int i = 0; i < size; i++) {
            int u = -1, min = Integer.MAX_VALUE;
            for (int j = 0; j < size; j++) {
                if (!visited[j] && dist[j] < min) {
                    u = j; min = dist[j];
                }
            }
            if (u == -1) break;
            visited[u] = true;
            for (int v = 0; v < size; v++) {
                if (!visited[v] && graph[u][v] < Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v]) {
                    dist[v] = dist[u] + graph[u][v];
                }
            }
        }
        return dist[end];
    }
}