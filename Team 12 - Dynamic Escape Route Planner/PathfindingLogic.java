import java.util.*;

public class PathfindingLogic {
    public enum Status { RUNNING, FOUND, NO_PATH, SIMULATING, SIMULATION_REACHED, SIMULATION_NO_PATH, SIMULATION_REROUTING }

    private int rows, cols;
    private char[][] grid;
    
    private PriorityQueue<Node> pq;
    private int[][] dist, px, py;
    private boolean[][] visited;
    private List<int[]> finalPath;
    private Set<String> frontierSet = new HashSet<>();
    private Set<String> exploredSet = new HashSet<>();
    private int[] startPos, goalPos, agentPos;

    private int selectedAlgo;
    private int corridorCost = 1;
    private int roomCost = 5;
    private int calculatedCost;
    private int accumulatedCost = 0;
    private int disasterRate;
    private Random random = new Random();

    static int[][] directions = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    static class Node implements Comparable<Node> {
        int x, y, cost, priority;
        Node(int x, int y, int cost, int priority) {
            this.x = x; this.y = y; this.cost = cost; this.priority = priority;
        }
        public int compareTo(Node other) {
            if (this.priority == other.priority) {
                // Break ties by preferring paths that have traveled further (higher g-cost)
                // This makes A* significantly faster by exploring deeper nodes first!
                return Integer.compare(other.cost, this.cost);
            }
            return Integer.compare(this.priority, other.priority);
        }
    }

    public PathfindingLogic(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        initGrid();
    }

    public void initGrid() {
        grid = new char[rows][cols];
        generateCityMap();
    }

    public void setCell(int r, int c, char val) {
        if (val == 'S' || val == 'E') {
            for (int i=0; i<rows; i++) {
                for (int j=0; j<cols; j++) {
                    if (grid[i][j] == val) grid[i][j] = 'C';
                }
            }
        }
        grid[r][c] = val;
    }

    public char getCell(int r, int c) { return grid[r][c]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public void generateCityMap() {
        clearPath();
        for (int r = 0; r < rows; r++) Arrays.fill(grid[r], 'X'); 

        for (int r = 1; r < rows; r += 4) {
            for (int c = 0; c < cols; c++) grid[r][c] = 'C';
        }
        
        for (int c = 1; c < cols; c += 5) {
            for (int r = 0; r < rows; r++) grid[r][c] = 'C';
        }

        for(int r = 1; r < rows - 1; r++) {
            for(int c = 1; c < cols - 1; c++) {
                if(grid[r][c] == 'X') {
                    if (random.nextDouble() < 0.15) {
                        grid[r][c] = 'R'; // Parks/Plazas
                    } else if (random.nextDouble() < 0.05) {
                        grid[r][c] = 'C'; // Alleys
                    }
                }
            }
        }
        
        grid[1][1] = 'S';
        
        int lastRowStreet = 1;
        while(lastRowStreet + 4 < rows) lastRowStreet += 4;
        
        int lastColStreet = 1;
        while(lastColStreet + 5 < cols) lastColStreet += 5;
        
        grid[lastRowStreet][lastColStreet] = 'E';
    }

    public void clearPath() {
        finalPath = null;
        frontierSet.clear();
        exploredSet.clear();
        calculatedCost = 0;
        accumulatedCost = 0;
        agentPos = null;
    }

    public void clearDisasters() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = grid[r][c];
                if (ch == 'F' || ch == 'B' || ch == 'W' || ch == 'T') {
                    grid[r][c] = 'C';
                }
            }
        }
    }

    public void startAlgorithm(int algo) {
        clearPath();
        this.selectedAlgo = algo;
        
        startPos = findPos('S');
        goalPos = findPos('E');
        if (startPos == null || goalPos == null) return;

        dist = new int[rows][cols];
        px = new int[rows][cols];
        py = new int[rows][cols];
        visited = new boolean[rows][cols];

        for (int i = 0; i < rows; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
            Arrays.fill(px[i], -1);
            Arrays.fill(py[i], -1);
        }

        pq = new PriorityQueue<>();
        int initialPriority = (algo == 0 || algo == 2) ? heuristic(startPos[0], startPos[1], goalPos) : 0;
        
        pq.add(new Node(startPos[0], startPos[1], 0, initialPriority));
        dist[startPos[0]][startPos[1]] = 0;
    }

    public void startSimulation(int algo, int dRate) {
        this.disasterRate = dRate;
        startAlgorithm(algo);
        
        // Instantly find initial path
        Status s = Status.RUNNING;
        while (s == Status.RUNNING) {
            s = internalStep();
        }
        if (s == Status.FOUND) {
            buildPath(goalPos[0], goalPos[1]);
            calculatedCost = calculateTrueCost();
            agentPos = new int[]{startPos[0], startPos[1]};
        } else {
            agentPos = null;
        }
    }

    public Status simulateStep() {
        if (agentPos == null || goalPos == null) return Status.SIMULATION_NO_PATH;
        if (agentPos[0] == goalPos[0] && agentPos[1] == goalPos[1]) return Status.SIMULATION_REACHED;

        // Count total disasters on the board
        int numDisasters = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = grid[r][c];
                if (ch == 'F' || ch == 'B' || ch == 'W' || ch == 'T') numDisasters++;
            }
        }

        boolean disasterSpawned = false;
        // Spawn disaster directly ON the planned path (Max 3 allowed)
        if (disasterRate > 0 && finalPath != null && finalPath.size() > 2 && numDisasters < 3) {
            if (random.nextInt(100) < disasterRate) {
                // Pick a random node on the path. finalPath is Goal(0) to Agent(size-1).
                int idx = random.nextInt(finalPath.size() - 2) + 1; 
                int[] p = finalPath.get(idx);
                char originalCell = grid[p[0]][p[1]];
                if (originalCell == 'C' || originalCell == 'R') {
                    int type = random.nextInt(4);
                    if (type == 0) grid[p[0]][p[1]] = 'F'; 
                    else if (type == 1) grid[p[0]][p[1]] = 'B'; 
                    else if (type == 2) grid[p[0]][p[1]] = 'W'; 
                    else grid[p[0]][p[1]] = 'T'; 
                    
                    // Check if Goal is still reachable
                    if (!isReachable(agentPos, goalPos)) {
                        // Revert disaster if it completely blocks the goal
                        grid[p[0]][p[1]] = originalCell;
                    } else {
                        disasterSpawned = true;
                    }
                }
            }
        }

        if (disasterSpawned) {
            return Status.SIMULATION_REROUTING;
        }

        if (finalPath != null && finalPath.size() > 1) {
            int[] nextStep = finalPath.get(finalPath.size() - 2);
            
            int dx = Math.abs(agentPos[0] - nextStep[0]);
            int dy = Math.abs(agentPos[1] - nextStep[1]);
            int move = (dx + dy == 2) ? 2 : 1;
            accumulatedCost += move + getTerrainCost(grid[nextStep[0]][nextStep[1]]);
            
            agentPos[0] = nextStep[0];
            agentPos[1] = nextStep[1];
        }
        
        recalculatePathFromAgent();

        if (agentPos[0] == goalPos[0] && agentPos[1] == goalPos[1]) {
            return Status.SIMULATION_REACHED;
        }
        
        if (finalPath == null) {
            return Status.SIMULATION_NO_PATH;
        }

        return Status.SIMULATING;
    }

    public void recalculatePathFromAgent() {
        dist = new int[rows][cols];
        px = new int[rows][cols];
        py = new int[rows][cols];
        visited = new boolean[rows][cols];
        frontierSet.clear();
        exploredSet.clear();

        for (int i = 0; i < rows; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
            Arrays.fill(px[i], -1);
            Arrays.fill(py[i], -1);
        }

        pq = new PriorityQueue<>();
        int initialPriority = (selectedAlgo == 0 || selectedAlgo == 2) ? heuristic(agentPos[0], agentPos[1], goalPos) : 0;
        
        pq.add(new Node(agentPos[0], agentPos[1], 0, initialPriority));
        dist[agentPos[0]][agentPos[1]] = 0;

        Status s = Status.RUNNING;
        while (s == Status.RUNNING) {
            s = internalStep();
        }
        if (s == Status.FOUND) {
            buildPath(goalPos[0], goalPos[1]);
            calculatedCost = calculateTrueCost();
        } else {
            finalPath = null;
        }
    }

    public Status step() {
        Status s = internalStep();
        if (s == Status.FOUND) {
            buildPath(goalPos[0], goalPos[1]);
            calculatedCost = calculateTrueCost();
        }
        return s;
    }

    private Status internalStep() {
        if (pq == null || pq.isEmpty()) return Status.NO_PATH;
        
        Node cur = pq.poll();
        frontierSet.remove(cur.x + "," + cur.y);

        if (visited[cur.x][cur.y]) return Status.RUNNING;
        
        visited[cur.x][cur.y] = true;
        exploredSet.add(cur.x + "," + cur.y);

        if (cur.x == goalPos[0] && cur.y == goalPos[1]) {
            return Status.FOUND;
        }

        for (int[] d : directions) {
            int nx = cur.x + d[0], ny = cur.y + d[1];
            if (isValid(nx, ny)) {
                int dx = Math.abs(nx - cur.x);
                int dy = Math.abs(ny - cur.y);
                int move = (dx + dy == 2) ? 2 : 1;
                
                int tCost = getTerrainCost(grid[nx][ny]);
                if (tCost == Integer.MAX_VALUE) continue;

                int newCost = cur.cost + move + tCost;
                if (selectedAlgo == 3) newCost = cur.cost + 1;

                if (newCost < dist[nx][ny]) {
                    dist[nx][ny] = newCost;
                    px[nx][ny] = cur.x;
                    py[nx][ny] = cur.y;
                    
                    int priority = 0;
                    int h = heuristic(nx, ny, goalPos);
                    
                    if (selectedAlgo == 0) priority = newCost + h;
                    else if (selectedAlgo == 1) priority = newCost;
                    else if (selectedAlgo == 2) priority = h;
                    else if (selectedAlgo == 3) priority = newCost;
                    
                    pq.add(new Node(nx, ny, newCost, priority));
                    frontierSet.add(nx + "," + ny);
                }
            }
        }
        return Status.RUNNING;
    }

    private void buildPath(int ex, int ey) {
        finalPath = new ArrayList<>();
        int cx = ex, cy = ey;
        while (cx != -1 && cy != -1) {
            finalPath.add(new int[]{cx, cy});
            int tx = px[cx][cy];
            int ty = py[cx][cy];
            cx = tx;
            cy = ty;
        }
    }

    private int calculateTrueCost() {
        if (finalPath == null || finalPath.size() < 2) return 0;
        int total = 0;
        for (int i = 0; i < finalPath.size() - 1; i++) {
            int[] p1 = finalPath.get(i);
            int[] p2 = finalPath.get(i+1);
            int dx = Math.abs(p1[0] - p2[0]);
            int dy = Math.abs(p1[1] - p2[1]);
            int move = (dx + dy == 2) ? 2 : 1;
            total += move + getTerrainCost(grid[p1[0]][p1[1]]);
        }
        return total;
    }

    private int[] findPos(char target) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == target) return new int[]{i, j};
            }
        }
        return null;
    }

    private boolean isValid(int x, int y) {
        if (x < 0 || y < 0 || x >= rows || y >= cols) return false;
        char c = grid[x][y];
        return c != 'X' && c != 'B' && c != 'F' && c != 'W';
    }

    private boolean isReachable(int[] start, int[] end) {
        boolean[][] vis = new boolean[rows][cols];
        Queue<int[]> q = new LinkedList<>();
        q.add(start);
        vis[start[0]][start[1]] = true;
        while(!q.isEmpty()) {
            int[] curr = q.poll();
            if (curr[0] == end[0] && curr[1] == end[1]) return true;
            for (int[] d : directions) {
                int nx = curr[0] + d[0], ny = curr[1] + d[1];
                if (isValid(nx, ny) && !vis[nx][ny]) {
                    vis[nx][ny] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }

    private int heuristic(int x, int y, int[] goal) {
        // Optimal admissible heuristic for 8-way movement where orthogonal=2, diagonal=3
        int dx = Math.abs(x - goal[0]);
        int dy = Math.abs(y - goal[1]);
        return 3 * Math.min(dx, dy) + 2 * Math.abs(dx - dy);
    }

    private int getTerrainCost(char cell) {
        if (cell == 'C') return corridorCost;
        if (cell == 'R') return 15;
        if (cell == 'T') return 20; 
        if (cell == 'E' || cell == 'S') return 1;
        return Integer.MAX_VALUE;
    }

    // Getters for rendering
    public Set<String> getExploredSet() { return exploredSet; }
    public Set<String> getFrontierSet() { return frontierSet; }
    public List<int[]> getFinalPath() { return finalPath; }
    public int getCalculatedCost() { return calculatedCost + accumulatedCost; }
    public int[] getAgentPos() { return agentPos; }
    public boolean hasStartAndGoal() { return findPos('S') != null && findPos('E') != null; }
}
