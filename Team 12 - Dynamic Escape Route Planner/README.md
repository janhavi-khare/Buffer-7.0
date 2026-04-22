# 🗺️ Dynamic City Escape Route Planner

> A pathfinding simulator that navigates an agent through a procedurally generated city grid to a safe exit, dynamically replanning its route as disasters strike in real time.

---

## 📽️ Demo Video

**[▶ Watch Demo](YOUR_VIDEO_LINK_HERE)**

---

## 🧩 Problem Statement

In emergency scenarios — fires, floods, structural collapses — static evacuation routes fail the moment a road becomes impassable. This project models the real-world challenge of **dynamic pathfinding under uncertainty**: an agent must find the lowest-cost path from a start position `S` to an exit `E` on a city grid, but the environment is not static. Disasters spawn on the planned route mid-execution, blocking cells and forcing the agent to recompute a new optimal path from its current position — all without backtracking to the origin.

The core question this project answers is: **how quickly and efficiently can a search algorithm adapt its route when the world changes beneath it?**

The simulation supports four search strategies — A\*, Dijkstra, Greedy Best-First Search, and BFS — allowing direct comparison of how each handles the tension between exploration efficiency and path optimality in a changing environment.

---

## ✨ Features

- Procedurally generated city map (streets, alleys, parks, buildings) on every run
- Four selectable search algorithms with step-by-step and full-simulation modes
- Real-time disaster spawning (Fire 🔥, Barricade 🚧, Water 💧, Traffic 🚗) directly on the planned path
- Instant replanning from the agent's current position when the route is blocked
- Visual frontier/explored node highlighting so you can watch the algorithm think
- Accumulated movement cost tracking across reroutes

---

## 🔬 Algorithms Supported

| # | Algorithm | Strategy | Optimal? | Notes |
|---|-----------|----------|----------|-------|
| 0 | **A\*** | `f = g + h` | ✅ Yes | Chebyshev-based heuristic; tie-breaks by deeper g-cost |
| 1 | **Dijkstra** | `f = g` | ✅ Yes | Exhaustive; no heuristic |
| 2 | **Greedy Best-First** | `f = h` | ❌ No | Fast but ignores terrain cost |
| 3 | **BFS** | `f = steps` | Unweighted | Ignores terrain; uniform edge cost of 1 |

The heuristic used for A\* and Greedy is an **admissible Chebyshev variant** tuned for 8-directional movement with asymmetric edge costs (orthogonal = 2, diagonal = 3):

```
h(n) = 3 × min(dx, dy) + 2 × |dx - dy|
```

---

## 🏙️ Grid Cell Types

| Symbol | Meaning | Traversal Cost |
|--------|---------|----------------|
| `S` | Start position | 1 |
| `E` | Exit / Safe zone | 1 |
| `C` | Corridor / Road | 1 (configurable) |
| `R` | Park / Plaza | 15 |
| `T` | Traffic jam | 20 |
| `X` | Building (wall) | ∞ — impassable |
| `F` | Fire | ∞ — impassable |
| `B` | Barricade | ∞ — impassable |
| `W` | Water / Flood | ∞ — impassable |

Move costs are **additive**: diagonal moves cost `2 + terrain`, cardinal moves cost `1 + terrain`.

---

## 🗂️ Data Structures

### `PriorityQueue<Node>` — Open List
The frontier of nodes awaiting exploration. Each `Node` stores `(x, y, g-cost, f-priority)`. The queue is ordered by `f`; ties are broken by preferring nodes with a **higher g-cost** (deeper in the search tree), which empirically reduces total nodes expanded and speeds up A\* significantly.

### `int[][] dist` — Cost Table
A 2D array tracking the best known `g`-cost to reach every grid cell. Initialised to `Integer.MAX_VALUE`. When a cheaper path to a cell is found, its entry is updated and a new `Node` is enqueued — the stale entry is discarded via the `visited` guard.

### `int[][] px / py` — Parent Pointer Arrays
Two parallel 2D arrays storing the row and column of each cell's predecessor on the best-known path. Used at termination to reconstruct the full route by walking backwards from `E` to `S`.

### `boolean[][] visited` — Closed List
A 2D boolean array marking cells that have been **finalized** (popped from the priority queue and fully processed). Prevents re-expansion of already-settled nodes, ensuring each cell is relaxed at most once per search.

### `List<int[]> finalPath`
An `ArrayList` holding the reconstructed path as `[row, col]` pairs, stored **goal → start** (reversed after `buildPath()`). During simulation, the agent consumes this list from the tail, moving one step per tick.

### `Set<String> frontierSet / exploredSet`
Two `HashSet<String>` instances keyed by `"row,col"` strings. Used exclusively for **UI rendering** — they let the frontend colour frontier and explored cells without walking the full priority queue each frame.

### `Queue<int[]>` — BFS Reachability Check
A plain `LinkedList`-backed `Queue` used in `isReachable()` for a lightweight connectivity check before committing to a disaster spawn. Ensures a disaster is never placed if it would permanently disconnect the agent from the exit.

---

## 📁 Project Structure

```
├── PathfindingLogic.java   Core algorithm engine — grid generation, A*/Dijkstra/
│                           Greedy/BFS, simulation loop, disaster spawning, replanning
├── Grid.java               Entry point / original prototype — static 4×4 grid demo
│                           of A* with controlled obstacle injection
├── GridServer.java         Plain Java HTTP server — exposes pathfinding as REST API
│                           (GET /api/state, POST /api/start|step|reset)
└── index.html              Browser UI — connects to the Java REST API, renders the
                            live grid, path, metrics, and incident log
```

---

## 🚀 Running the Project

### Prerequisites
- Java 11 or higher (uses `com.sun.net.httpserver` — no extra dependencies)
- A modern browser (Chrome, Firefox, Edge)

### Start the server
```bash
javac GridServer.java PathfindingLogic.java
java GridServer
```

You should see:
```
╔══════════════════════════════════════╗
║  GridServer running on port 8080     ║
║  Open index.html in your browser     ║
╚══════════════════════════════════════╝
```

### Open the UI
Open `index.html` directly in your browser — no additional web server needed.

The status bar at the bottom turns **green** when the Java server is connected.

---

## 🔁 Simulation Flow

```
generateCityMap()
      │
      ▼
startSimulation(algo, disasterRate)
      │  instantly runs selected algorithm to find initial path
      ▼
loop: simulateStep()
      ├── maybe spawn disaster ON the planned path (max 3 active)
      │       └── reachability check → revert if goal becomes disconnected
      ├── if disaster spawned → return SIMULATION_REROUTING
      │       └── recalculatePathFromAgent() → full re-run from current pos
      ├── move agent one step along finalPath
      └── if agent == goal → return SIMULATION_REACHED
```

---

## ⚙️ Configuration

Tune these in `PathfindingLogic.java`:

```java
private int corridorCost = 1;   // Cost to traverse a road cell
private int disasterRate;       // % chance per step of a disaster spawning (0–100)
```

---

## 👥 Authors

Janhavi Khare
Kavya Thacker
Rujuta Walavalkar
