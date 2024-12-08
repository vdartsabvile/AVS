const WebSocket = require("ws");
const bodyParser = require("body-parser");
const express = require("express");
const cors = require('cors');

const app = express();
const port = process.env.PORT || 3000;

// Хранилище статусов пользователей
let users = {
    admin: { isOnline: false, friendIp: "95.25.87.26" },
    dartsabvile: { isOnline: false, friendIp: "95.25.87.26" },
};

// Определение имени пользователя через аргументы командной строки
const username = process.argv[2] || "unknown";

// Поддержка JSON-запросов и CORS
app.use(bodyParser.json());
app.use(cors());

// Эндпоинт для получения статусов друзей
app.get("/api/getFriendStatuses", (req, res) => {
    const friends = Object.keys(users).map((username) => ({
        username,
        isOnline: users[username].isOnline,
        friendIp: users[username].friendIp,
    }));
    res.json({ friends });
});

// Эндпоинт для обновления статуса
app.post("/api/updateStatus", (req, res) => {
    const { username, isOnline } = req.body;

    if (users[username] !== undefined) {
        users[username].isOnline = isOnline;
        notifyClients();
        res.json({ success: true });
    } else {
        res.status(404).json({ success: false, message: "User not found" });
    }
});

// WebSocket сервер
const wss = new WebSocket.Server({ noServer: true });
const clients = new Set();

// Уведомляем всех клиентов через WebSocket
function notifyClients() {
    const friends = Object.keys(users).map((username) => ({
        username,
        isOnline: users[username].isOnline,
        friendIp: users[username].friendIp,
    }));
    const message = JSON.stringify({ friends });

    console.log("Обновление статусов:", friends);

    for (const client of clients) {
        if (client.readyState === WebSocket.OPEN) {
            client.send(message);
        }
    }
}

wss.on("connection", (ws, req) => {
    ws.isAlive = true;

    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    const url = new URL(req.url, `http://${req.headers.host}`);
    const wsUsername = url.searchParams.get("username");

    console.log("Подключение WebSocket:", { url: req.url, username: wsUsername, ip });

    if (wsUsername && users[wsUsername]) {
        users[wsUsername].isOnline = true;
        users[wsUsername].friendIp = ip;
        console.log(`Пользователь ${wsUsername} подключился. IP: ${ip}`);
        notifyClients();
    } else {
        console.warn(`Пользователь ${wsUsername} не найден.`);
    }

    clients.add(ws);

    ws.on("pong", () => {
        ws.isAlive = true;
    });

    ws.on("close", () => {
        if (wsUsername && users[wsUsername]) {
            users[wsUsername].isOnline = false;
            console.log(`Пользователь ${wsUsername} отключился.`);
            notifyClients();
        }
        clients.delete(ws);
    });
});

// Интеграция HTTP и WebSocket
const server = app.listen(port, '0.0.0.0', () => console.log(`Server running on port ${port}`));

// Инициализация WebSocket клиента
const serverUrl = process.env.SERVER_URL || `ws://localhost:${port}`;
const socket = new WebSocket(`${serverUrl}?username=${username}`);

socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log("Получены обновленные статусы друзей:", data.friends);
};

// Обработка пинга для проверки активности клиентов
setInterval(() => {
    for (const client of clients) {
        if (client.isAlive === false) {
            client.terminate();
            console.log("WebSocket соединение завершено из-за неактивности.");
        } else {
            client.isAlive = false;
            client.ping();
        }
    }
}, 10000); // Проверяем каждые 10 секунд

app.post("/stop", (req, res) => {
    res.send("Server stopping...");
    server.close(() => {
        console.log("Server stopped");
        process.exit(0);
    });
});

server.on("upgrade", (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit("connection", ws, request);
    });
});
