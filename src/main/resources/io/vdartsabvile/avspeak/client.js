const WebSocket = require('ws');
const readline = require('readline');

const username = process.argv[2] || "unknown";
const serverUrl = process.env.SERVER_URL || 'ws://localhost:3000';

const socket = new WebSocket(`${serverUrl}?username=${username}`);

console.log("Полученные аргументы:", process.argv);
console.log("Подключение к серверу:", serverUrl);

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

socket.onopen = () => {
    console.log("WebSocket соединение установлено.");
    console.log("Введите команду (message, call, status) или 'exit' для выхода:");
    promptUser();
};

socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    switch (data.type) {
        case "statusUpdate":
            console.log("Обновление статусов пользователей:", data.users);
            break;
        case "chat":
            console.log(`Новое сообщение от ${data.message.sender}: ${data.message.content}`);
            break;
        case "callRequest":
            console.log(`Входящий звонок от ${data.from}`);
            rl.question("Принять звонок? (y/n): ", (answer) => {
                if (answer.toLowerCase() === 'y') {
                    sendMessage({ type: "callAccept", recipient: data.from });
                } else {
                    sendMessage({ type: "callReject", recipient: data.from });
                }
            });
            break;
        case "callAccepted":
            console.log(`${data.from} принял ваш звонок`);
            break;
        case "callRejected":
            console.log(`${data.from} отклонил ваш звонок`);
            break;
        case "callEnded":
            console.log(`${data.from} завершил звонок`);
            break;
        default:
            console.log("Получено неизвестное сообщение:", data);
    }
    promptUser();
};

socket.onclose = () => {
    console.log("WebSocket соединение закрыто.");
    rl.close();
};

socket.onerror = (error) => {
    console.log("Ошибка WebSocket:", error);
};

function sendMessage(message) {
    socket.send(JSON.stringify(message));
}

function promptUser() {
    rl.question("> ", (input) => {
        const [command, ...args] = input.split(' ');
        switch (command) {
            case "message":
                if (args.length < 2) {
                    console.log("Использование: message <получатель> <сообщение>");
                } else {
                    const [recipient, ...contentParts] = args;
                    const content = contentParts.join(' ');
                    sendMessage({ type: "chat", recipient, content });
                }
                break;
            case "call":
                if (args.length !== 1) {
                    console.log("Использование: call <получатель>");
                } else {
                    sendMessage({ type: "call", recipient: args[0] });
                }
                break;
            case "status":
                sendMessage({ type: "getStatus" });
                break;
            case "exit":
                socket.close();
                rl.close();
                return;
            default:
                console.log("Неизвестная команда. Доступные команды: message, call, status, exit");
        }
        promptUser();
    });
}