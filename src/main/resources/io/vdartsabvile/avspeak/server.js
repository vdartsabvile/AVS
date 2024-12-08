const WebSocket = require("ws");
const bodyParser = require("body-parser");
const express = require("express");
const cors = require('cors');
const fs = require('fs');

const app = express();
const port = process.env.PORT || 3000;

// Хранилище пользователей и их сообщений
let users = {};
let messageHistory = {};

// Поддержка JSON-запросов и CORS
app.use(bodyParser.json());
app.use(cors());

// WebSocket сервер
const wss = new WebSocket.Server({ noServer: true });
const clients = new Map();

// Функция для сохранения истории сообщений
function saveMessageHistory() {
    fs.writeFileSync('messageHistory.json', JSON.stringify(messageHistory));
}

// Загрузка истории сообщений при запуске сервера
try {
    const data = fs.readFileSync('messageHistory.json', 'utf8');
    messageHistory = JSON.parse(data);
} catch (err) {
    console.log("No existing message history found. Starting fresh.");
}

// Эндпоинт для обновления статуса пользователя
app.post("/api/updateStatus", (req, res) => {
    const { username, isOnline, ip } = req.body;

    if (!users[username]) {
        users[username] = { isOnline: false, ip: null };
    }

    users[username].isOnline = isOnline;
    users[username].ip = ip;
    notifyClients();
    res.json({ success: true });
});

// Эндпоинт для получения статусов друзей
app.get("/api/getFriendStatuses", (req, res) => {
    const friends = Object.keys(users).map((username) => ({
        username,
        isOnline: users[username].isOnline,
        ip: users[username].ip,
    }));
    res.json({ friends });
});

// Обработка WebSocket соединений
wss.on("connection", (ws, req) => {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    const url = new URL(req.url, `http://${req.headers.host}`);
    const username = url.searchParams.get("username");

    if (!users[username]) {
        users[username] = { isOnline: true, ip: ip };
    } else {
        users[username].isOnline = true;
        users[username].ip = ip;
    }

    clients.set(username, ws);
    notifyClients();

    ws.on("message", (message) => {
        const data = JSON.parse(message);
        handleMessage(username, data);
    });

    ws.on("close", () => {
        users[username].isOnline = false;
        clients.delete(username);
        notifyClients();
    });
});

function handleMessage(sender, data) {
    switch (data.type) {
        case "chat":
            handleChatMessage(sender, data);
            break;
        case "call":
            handleCallRequest(sender, data);
            break;
        case "callAccept":
            handleCallAccept(sender, data);
            break;
        case "callReject":
            handleCallReject(sender, data);
            break;
        case "endCall":
            handleEndCall(sender, data);
            break;
    }
}

function handleChatMessage(sender, data) {
    const { recipient, content } = data;
    const message = { sender, content, timestamp: new Date().toISOString() };

    if (!messageHistory[sender]) {
        messageHistory[sender] = {};
    }
    if (!messageHistory[sender][recipient]) {
        messageHistory[sender][recipient] = [];
    }
    messageHistory[sender][recipient].push(message);

    if (!messageHistory[recipient]) {
        messageHistory[recipient] = {};
    }
    if (!messageHistory[recipient][sender]) {
        messageHistory[recipient][sender] = [];
    }
    messageHistory[recipient][sender].push(message);

    saveMessageHistory();

    const recipientWs = clients.get(recipient);
    if (recipientWs) {
        recipientWs.send(JSON.stringify({ type: "chat", message }));
    }
}

function handleCallRequest(sender, data) {
    const { recipient } = data;
    const recipientWs = clients.get(recipient);
    if (recipientWs) {
        recipientWs.send(JSON.stringify({ type: "callRequest", from: sender }));
    }
}

function handleCallAccept(sender, data) {
    const { recipient, ip, port } = data;
    console.log(`Call accepted: from ${sender} to ${recipient}, IP: ${ip}, Port: ${port}`);
    const recipientWs = clients.get(recipient);
    if (recipientWs) {
        recipientWs.send(JSON.stringify({
            type: "callAccepted",
            from: sender,
            ip: ip,
            port: port
        }));
    } else {
        console.log(`Recipient ${recipient} not found or offline`);
    }
}

function handleCallReject(sender, data) {
    const { recipient } = data;
    const recipientWs = clients.get(recipient);
    if (recipientWs) {
        recipientWs.send(JSON.stringify({ type: "callRejected", from: sender }));
    }
}

function handleEndCall(sender, data) {
    const { recipient } = data;
    const recipientWs = clients.get(recipient);
    if (recipientWs) {
        recipientWs.send(JSON.stringify({ type: "callEnded", from: sender }));
    }
}

function notifyClients() {
    const statusUpdate = JSON.stringify({
        type: "statusUpdate",
        users: Object.keys(users).map(username => ({
            username,
            isOnline: users[username].isOnline,
            ip: users[username].ip
        }))
    });

    clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(statusUpdate);
        }
    });
}

// Эндпоинт для получения истории сообщений
app.get("/api/getMessageHistory", (req, res) => {
    const { user1, user2 } = req.query;
    if (messageHistory[user1] && messageHistory[user1][user2]) {
        res.json(messageHistory[user1][user2]);
    } else {
        res.json([]);
    }
});

const server = app.listen(port, '0.0.0.0', () => console.log(`Server running on port ${port}`));

server.on("upgrade", (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit("connection", ws, request);
    });
});