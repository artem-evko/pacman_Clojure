// ======= Этап 4: STM на сервере (players list + roles) + локальный Pacman =======

const MAP = [
  "#################################################",
  "#.............#.............#.................#.#",
  "#.###########.#.###########.#.###############.#.#",
  "#.#.........#.#.#.........#.#.#.............#.#.#",
  "#.#.#######.#.#.#.#######.#.#.#.###########.#.#.#",
  "#.#.#.....#.#.#.#.#.....#.#.#.#.#.........#.#.#.#",
  "#.#.#.###.#.#.#.#.#.###.#.#.#.#.#.#######.#.#.#.#",
  "#...#...#.#...#...#...#.#...#...#...#.....#...#.#",
  "###.###.#.###########.#.###########.#.#########.#",
  "#...#...#.....#.......#.....#.......#.........#.#",
  "#.###.#######.#.###########.#.###############.#.#",
  "#.....#.......#.....#.......#.......#.........#.#",
  "#.#####.###########.#.#############.#.#########.#",
  "#.....#.....#.......#.....#.......#.#.#.......#.#",
  "#####.#####.#.###########.#.#####.#.#.#.#####.#.#",
  "#.....#.....#.....#.......#.....#.#.#.#.....#.#.#",
  "#.#####.#########.#.#############.#.#.#####.#.#.#",
  "#.....#.........#.#.....#.........#.#.....#.#.#.#",
  "#.###.#########.#.#####.#.#########.#####.#.#.#.#",
  "#...#...........#.....#.#.....P...........#...#.#",
  "###.#################.#.#####################.#.#",
  "#.....................#.......................#.#",
  "#################################################"
];

const CELL_SIZE = 22;

// ======= локальный Pacman =======
let player = { x: 1, y: 1 };

function findPacmanStart() {
  for (let y = 0; y < MAP.length; y++) {
    const x = MAP[y].indexOf("P");
    if (x !== -1) return { x, y };
  }
  return { x: 1, y: 1 };
}

function createMap() {
  const mapEl = document.getElementById("map");
  const arenaEl = document.getElementById("arena");

  const rows = MAP.length;
  const cols = MAP[0].length;

  mapEl.style.gridTemplateColumns = `repeat(${cols}, ${CELL_SIZE}px)`;
  mapEl.style.gridAutoRows = `${CELL_SIZE}px`;

  arenaEl.style.width = `${cols * CELL_SIZE}px`;
  arenaEl.style.height = `${rows * CELL_SIZE}px`;

  const start = findPacmanStart();
  player.x = start.x;
  player.y = start.y;

  mapEl.innerHTML = "";
  for (let y = 0; y < rows; y++) {
    const row = MAP[y];
    for (let x = 0; x < cols; x++) {
      const ch = row[x];
      const cell = document.createElement("div");
      cell.classList.add("cell");
      cell.style.width = `${CELL_SIZE}px`;
      cell.style.height = `${CELL_SIZE}px`;

      if (ch === "#") cell.classList.add("wall");
      else if (ch === ".") cell.classList.add("dot");
      else cell.classList.add("empty");

      mapEl.appendChild(cell);
    }
  }
}

function isWall(nx, ny) {
  if (ny < 0 || ny >= MAP.length) return true;
  if (nx < 0 || nx >= MAP[0].length) return true;
  return MAP[ny][nx] === "#";
}

function createPacmanElement() {
  const arena = document.getElementById("arena");
  const pac = document.createElement("div");
  pac.id = "pacman";
  pac.style.width = `${CELL_SIZE}px`;
  pac.style.height = `${CELL_SIZE}px`;
  arena.appendChild(pac);
  updatePacmanPosition();
}

function updatePacmanPosition() {
  const pac = document.getElementById("pacman");
  pac.style.left = (player.x * CELL_SIZE) + "px";
  pac.style.top = (player.y * CELL_SIZE) + "px";
}

function movePlayer(dx, dy) {
  const nx = player.x + dx;
  const ny = player.y + dy;
  if (!isWall(nx, ny)) {
    player.x = nx;
    player.y = ny;
    updatePacmanPosition();
  }
}

function handleKeydown(e) {
  if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(e.key)) {
    e.preventDefault();
  }
  switch (e.key) {
    case "ArrowUp":
    case "w":
    case "W":
      movePlayer(0, -1);
      break;
    case "ArrowDown":
    case "s":
    case "S":
      movePlayer(0, 1);
      break;
    case "ArrowLeft":
    case "a":
    case "A":
      movePlayer(-1, 0);
      break;
    case "ArrowRight":
    case "d":
    case "D":
      movePlayer(1, 0);
      break;
  }
}

// ======= WebSocket + server state rendering =======
let ws = null;
let you = null;        // {id, nickname, role}
let serverState = null; // {players, status, winner}

function logWs(line) {
  const logEl = document.getElementById("ws-log");
  if (!logEl) return;
  const t = new Date().toLocaleTimeString();
  const div = document.createElement("div");
  div.textContent = `[${t}] ${line}`;
  logEl.appendChild(div);
  logEl.scrollTop = logEl.scrollHeight;
}

function setWsStatus(text) {
  const el = document.getElementById("ws-status");
  if (el) el.textContent = text;
}

function renderPlayers() {
  const list = document.getElementById("players-list");
  if (!list || !serverState) return;

  const players = serverState.players || {};
  const items = Object.values(players);

  list.innerHTML = "";
  if (items.length === 0) {
    list.textContent = "Пока никого нет";
    return;
  }

  items
    .sort((a, b) => (a.role || "").localeCompare(b.role || ""))
    .forEach(p => {
    const div = document.createElement("div");
    div.className = "player-item";
    div.innerHTML = `<div><b>${p.nickname}</b></div>
                       <div class="role">${p.role}</div>`;
    list.appendChild(div);
  });
}

function renderYou() {
  const el = document.getElementById("you-info");
  if (!el) return;
  el.textContent = you ? `${you.nickname} (${you.role})` : "—";
}

function renderStatus() {
  const el = document.getElementById("game-status");
  if (!el) return;
  el.textContent = serverState ? serverState.status : "—";
}

function handleServerMessage(raw) {
  logWs("recv: " + raw);
  let msg;
  try { msg = JSON.parse(raw); } catch { return; }

  if (msg.type === "joined") {
    you = msg.payload.you;
    serverState = msg.payload.state;
    renderYou();
    renderStatus();
    renderPlayers();
    return;
  }

  if (msg.type === "state") {
    serverState = msg.payload;
    renderStatus();
    renderPlayers();
    return;
  }
}

function connectWs() {
  const proto = window.location.protocol === "https:" ? "wss" : "ws";
  const url = `${proto}://${window.location.host}/ws`;

  ws = new WebSocket(url);

  ws.onopen = () => {
    setWsStatus("WS: connected");
    logWs("connected");
  };

  ws.onclose = () => {
    setWsStatus("WS: closed");
    logWs("closed");
  };

  ws.onerror = () => {
    setWsStatus("WS: error");
    logWs("error");
  };

  ws.onmessage = (ev) => handleServerMessage(ev.data);
}

function wsSend(obj) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    logWs("send failed: ws not open");
    return;
  }
  const s = JSON.stringify(obj);
  ws.send(s);
  logWs("send: " + s);
}

function setupWsUi() {
  document.getElementById("join-btn").onclick = () => {
    const nick = document.getElementById("nickname").value || "Anon";
    wsSend({ type: "join", payload: { nickname: nick } });
  };

  document.getElementById("ping-btn").onclick = () => {
    wsSend({ type: "ping", payload: {} });
  };
}

window.addEventListener("DOMContentLoaded", () => {
  createMap();
  createPacmanElement();
  window.addEventListener("keydown", handleKeydown);

  setupWsUi();
  connectWs();
});