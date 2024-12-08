const WebSocket = require('ws');

const username = process.argv[2] || "unknown";
const serverUrl = process.env.SERVER_URL || 'ws://localhost:3000';

const socket = new WebSocket(${serverUrl}?username=${username});

console.log("Полученные аргументы:", process.argv);
console.log("Подключение к серверу:", serverUrl);

socket.onopen = () => {
    console.log("WebSocket соединение установлено.");
};

socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log("Статусы друзей:", data.friends);
};

socket.onclose = () => {
    console.log("WebSocket соединение закрыто.");
};

socket.onerror = (error) => {
    console.log("Ошибка WebSocket:", error);
};