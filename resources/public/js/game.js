// ======= Client: WS + render from server state + Add bot =======

const MAP = [
  "#################################################",
 "#......................#........................#",
  "#........###.........###.........###............#",
  "#..........#..........###.....###..........#....#",
  "#......###.............................###......#",
  "#.......................#.......................#",
  "#.......###.......#.......###.......#...........#",
  "#......................P........................#",
  "#.......................#.......................#",
  "#......###.........###.............###..........#",
  "#...............................................#",
  "#...........#.........###...........###.........#",
  "#..................###..............###.........#",
  "#...........###...........#...........###.......#",
  "#...............#...............#...............#",
  "#......###.............................###......#",
  "#.......................#.......................#",
  "#........###.........###.........###............#",
  "#..........#..........###.....###..........#....#",
  "#........................#......................#",
  "#......###.............................###......#",
  "#...............................................#",
  "#################################################"
];

const CELL_SIZE = 22;

let ws = null;
let you = null;           // {id,nickname,role,...}
let serverState = null;   // payload state
let playerSprites = {};   // id -> element

function $(id) { return document.getElementById(id); }

function logWs(line) {
  const logEl = $("ws-log");
  if (!logEl) return;
  const t = new Date().toLocaleTimeString();
  const div = document.createElement("div");
  div.textContent = `[${t}] ${line}`;
  logEl.appendChild(div);
  logEl.scrollTop = logEl.scrollHeight;
}

function setWsStatus(text) {
  const el = $("ws-status");
  if (el) el.textContent = text;
}

function createMapBase() {
  const mapEl = $("map");
  const arenaEl = $("arena");

  const rows = MAP.length;
  const cols = MAP[0].length;

  mapEl.style.gridTemplateColumns = `repeat(${cols}, ${CELL_SIZE}px)`;
  mapEl.style.gridAutoRows = `${CELL_SIZE}px`;

  arenaEl.style.width = `${cols * CELL_SIZE}px`;
  arenaEl.style.height = `${rows * CELL_SIZE}px`;

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
      else cell.classList.add("empty");

      mapEl.appendChild(cell);
    }
  }
}

function renderDots() {
  if (!serverState) return;
  const mapEl = $("map");
  if (!mapEl) return;

  const cols = MAP[0].length;
  const children = mapEl.children;

  for (let i = 0; i < children.length; i++) {
    children[i].classList.remove("dot");
  }

  const dots = serverState.dots || [];
  for (const [x, y] of dots) {
    const idx = y * cols + x;
    if (idx >= 0 && idx < children.length) {
      if (!children[idx].classList.contains("wall")) {
        children[idx].classList.add("dot");
      }
    }
  }
}

function renderYou() {
  const el = $("you-info");
  if (!el) return;
  el.textContent = you ? `${you.nickname} (${you.role})` : "—";
}

function renderStatus() {
  const el = $("game-status");
  if (!el) return;
  el.textContent = serverState ? serverState.status : "—";
}

function renderWinner() {
  const el = $("winner");
  if (!el) return;
  el.textContent = serverState && serverState.winner ? serverState.winner : "—";
}

function renderPlayersList() {
  const list = $("players-list");
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
      const isBot = p.isBot ? " (bot)" : "";
      const score = (p.score !== undefined) ? ` | score: ${p.score}` : "";
      const div = document.createElement("div");
      div.className = "player-item";
      div.innerHTML = `<div><b>${p.nickname}${isBot}</b>${score}</div><div class="role">${p.role}</div>`;
      list.appendChild(div);
    });
}

function ensureSprite(id, role) {
  if (playerSprites[id]) {
    playerSprites[id].className = "sprite " + role;
    return playerSprites[id];
  }
  const arena = $("arena");
  const el = document.createElement("div");
  el.className = "sprite " + role;
  el.style.width = `${CELL_SIZE}px`;
  el.style.height = `${CELL_SIZE}px`;
  arena.appendChild(el);
  playerSprites[id] = el;
  return el;
}

function clearSpritesNotInState() {
  const players = serverState?.players || {};
  for (const id of Object.keys(playerSprites)) {
    if (!players[id] || !players[id].pos) {
      playerSprites[id].remove();
      delete playerSprites[id];
    }
  }
}

function renderWorld() {
  if (!serverState) return;

  clearSpritesNotInState();

  const players = serverState.players || {};
  for (const [id, p] of Object.entries(players)) {
    if (!p.pos) continue;
    const role = p.role || "spectator";
    const el = ensureSprite(id, role);
    const [x, y] = p.pos;
    el.style.left = (x * CELL_SIZE) + "px";
    el.style.top = (y * CELL_SIZE) + "px";
  }
}

function handleServerMessage(raw) {
  logWs("recv: " + raw);
  let msg;
  try { msg = JSON.parse(raw); } catch { return; }

  if (msg.type === "joined") {
    you = msg.payload.you;
    serverState = msg.payload.state;
  } else if (msg.type === "state") {
    serverState = msg.payload;
  } else {
    return;
  }

  renderYou();
  renderStatus();
  renderWinner();
  renderPlayersList();
  renderDots();
  renderWorld();
}

function connectWs() {
  const proto = window.location.protocol === "https:" ? "wss" : "ws";
  const url = `${proto}://${window.location.host}/ws`;

  ws = new WebSocket(url);

  ws.onopen = () => { setWsStatus("WS: connected"); logWs("connected"); };
  ws.onclose = () => { setWsStatus("WS: closed"); logWs("closed"); };
  ws.onerror = () => { setWsStatus("WS: error"); logWs("error"); };
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
  $("join-btn").onclick = () => {
    const nick = $("nickname").value || "Anon";
    wsSend({ type: "join", payload: { nickname: nick } });
  };

  $("ping-btn").onclick = () => wsSend({ type: "ping", payload: {} });

  const restartBtn = $("restart-btn");
  if (restartBtn) restartBtn.onclick = () => wsSend({ type: "restart", payload: {} });

  const botBtn = $("bot-btn");
  if (botBtn) botBtn.onclick = () => wsSend({ type: "add-bot", payload: {} });
}

function keyToDir(e) {
  switch (e.key) {
    case "ArrowUp":
    case "w":
    case "W": return "up";
    case "ArrowDown":
    case "s":
    case "S": return "down";
    case "ArrowLeft":
    case "a":
    case "A": return "left";
    case "ArrowRight":
    case "d":
    case "D": return "right";
    default: return null;
  }
}

function handleKeydown(e) {
  const dir = keyToDir(e);
  if (!dir) return;

  if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(e.key)) {
    e.preventDefault();
  }

  if (!you || you.role === "spectator") return;
  if (!serverState) return;
  if (serverState.status !== "running") return;

  wsSend({ type: "dir", payload: { dir } });
}

window.addEventListener("DOMContentLoaded", () => {
  createMapBase();
  setupWsUi();
  connectWs();
  window.addEventListener("keydown", handleKeydown);
});
