// Карта уровня: каждый символ - тип клетки
// # - стена
// . - точка
// ' ' (пробел) - пусто

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

// P - начальная позиция Pacman (пока просто будем рисовать пустую клетку + точку позже)

function createMap() {
    const mapEl = document.getElementById("map");
    const rows = MAP.length;
    const cols = MAP[0].length;

    // Настраиваем CSS grid по размеру карты
    mapEl.style.gridTemplateColumns = `repeat(${cols}, 20px)`;

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
                // Пока Pacman не двигается, просто считаем эту клетку пустой
                cell.classList.add("empty");
                // В будущем сюда будем ставить Pacman'а
            } else {
                cell.classList.add("empty");
            }

            mapEl.appendChild(cell);
        }
    }
}

// Запускаем отрисовку при загрузке страницы
window.addEventListener("DOMContentLoaded", () => {
    createMap();
});
