// Большой лабиринт: # стены, . точки, пробел = пусто, P = старт пакмена
// Это "реально большой" уровень: 31 (ширина) x 17 (высота)

const MAP = [
  "###############################",
  "#.............#...............#",
  "#.#####.######.#.######.#####.#",
  "#.#...#.................#...#.#",
  "#.#.#.#######.###.#######.#.#.#",
  "#...#.....#...# #...#.....#...#",
  "###.#####.#.### # ###.#.#####.##",
  "#...#.....#.....#.....#.....#..#",
  "#.###.###.#####.#.#####.###.###.#",
  "#.....#...#.....P.....#...#.....#",
  "#.###.#.###.#########.###.#.###.#",
  "#...#.#.....#.......#.....#.#...#",
  "###.#.#####.#.#####.#.#####.#.###",
  "#...#.....#.#...#...#.#.....#...#",
  "#.#####.###.###.#.###.###.#####.#",
  "#.............#...............#",
  "###############################"
].map(row => {
  // Небольшая защита: выровняем строки по длине самой длинной
  return row;
});

// Если вдруг некоторые строки разной длины — выравниваем пробелами
const MAX_COLS = Math.max(...MAP.map(r => r.length));
const NORMALIZED_MAP = MAP.map(r => r.padEnd(MAX_COLS, " "));

const CELL_SIZE = 24; // можешь поставить 20/24/28/32 по вкусу

let player = { x: 0, y: 0 };

function findPacmanStart() {
  for (let y = 0; y < NORMALIZED_MAP.length; y++) {
    const x = NORMALIZED_MAP[y].indexOf("P");
    if (x !== -1) return { x, y };
  }
  return { x: 1, y: 1 };
}

function createMap() {
  const mapEl = document.getElementById("map");
  const arenaEl = document.getElementById("arena");

  const rows = NORMALIZED_MAP.length;
  const cols = MAX_COLS;

  // Настроим grid под размер клеток
  mapEl.style.gridTemplateColumns = `repeat(${cols}, ${CELL_SIZE}px)`;
  mapEl.style.gridAutoRows = `${CELL_SIZE}px`;

  // Размер арены равен карте
  arenaEl.style.width = `${cols * CELL_SIZE}px`;
  arenaEl.style.height = `${rows * CELL_SIZE}px`;

  const start = findPacmanStart();
  player.x = start.x;
  player.y = start.y;

  mapEl.innerHTML = "";

  for (let y = 0; y < rows; y++) {
    const row = NORMALIZED_MAP[y];
    for (let x = 0; x < cols; x++) {
      const ch = row[x] || " ";
      const cell = document.createElement("div");
      cell.classList.add("cell");
      cell.style.width = `${CELL_SIZE}px`;
      cell.style.height = `${CELL_SIZE}px`;

      if (ch === "#") {
        cell.classList.add("wall");
      } else if (ch === ".") {
        cell.classList.add("dot");
      } else if (ch === "P") {
        cell.classList.add("empty");
      } else {
        cell.classList.add("empty");
      }

      mapEl.appendChild(cell);
    }
  }
}

function isWall(nx, ny) {
  if (ny < 0 || ny >= NORMALIZED_MAP.length) return true;
  if (nx < 0 || nx >= MAX_COLS) return true;
  return (NORMALIZED_MAP[ny][nx] === "#");
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

window.addEventListener("DOMContentLoaded", () => {
  createMap();
  createPacmanElement();
  window.addEventListener("keydown", handleKeydown);
});
