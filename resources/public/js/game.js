// Карта уровня: каждый символ - тип клетки
// # - стена
// . - точка
// ' ' (пробел) - пусто
// P - стартовая позиция Pacman

const MAP = [
  "#####################",
  "#.........#.........#",
  "#.###.###.#.###.###.#",
  "#.#.....#.#.#.....#.#",
  "#.###.#.#.#.#.#.###.#",
  "#.....#.....#.....#.#",
  "#####.#.#####.#.#####",
  "#.........P.........#",
  "#####################"
];

const CELL_SIZE = 20;

// Состояние игрока (Pacman)
let player = {
  x: 0,
  y: 0
};

function findPacmanStart() {
  for (let y = 0; y < MAP.length; y++) {
    const row = MAP[y];
    const x = row.indexOf("P");
    if (x !== -1) {
      return { x, y };
    }
  }
  // На всякий случай дефолт
  return { x: 1, y: 1 };
}

function createMap() {
  const mapEl = document.getElementById("map");
  const rows = MAP.length;
  const cols = MAP[0].length;

  mapEl.style.gridTemplateColumns = `repeat(${cols}, ${CELL_SIZE}px)`;

  const startPos = findPacmanStart();
  player.x = startPos.x;
  player.y = startPos.y;

  for (let y = 0; y < rows; y++) {
    const row = MAP[y];
    for (let x = 0; x < cols; x++) {
      const ch = row[x];
      const cell = document.createElement("div");
      cell.classList.add("cell");

      if (ch === "#") {
        cell.classList.add("wall");
      } else if (ch === ".") {
        cell.classList.add("dot");
      } else if (ch === "P") {
        // Клетка под Pacman'ом — пустая
        cell.classList.add("empty");
      } else {
        cell.classList.add("empty");
      }

      mapEl.appendChild(cell);
    }
  }
}

// Проверка: стена ли в клетке (nx, ny)
function isWall(nx, ny) {
  if (ny < 0 || ny >= MAP.length) return true;
  if (nx < 0 || nx >= MAP[0].length) return true;
  return MAP[ny][nx] === "#";
}

// Создаём DOM-элемент Pacman
function createPacmanElement() {
  const gameContainer = document.getElementById("game-container");
  const pac = document.createElement("div");
  pac.id = "pacman";
  gameContainer.appendChild(pac);
  updatePacmanPosition();
}

// Обновляем позицию Pacman в пикселях
function updatePacmanPosition() {
  const pac = document.getElementById("pacman");
  pac.style.left = (player.x * CELL_SIZE) + "px";
  pac.style.top = (player.y * CELL_SIZE) + "px";
}

// Попытаться сдвинуть Pacman на (dx, dy)
function movePlayer(dx, dy) {
  const nx = player.x + dx;
  const ny = player.y + dy;
  if (!isWall(nx, ny)) {
    player.x = nx;
    player.y = ny;
    updatePacmanPosition();
  }
}

// Обработчик клавиш
function handleKeydown(e) {
  // Чтобы стрелки не скроллили страницу
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
