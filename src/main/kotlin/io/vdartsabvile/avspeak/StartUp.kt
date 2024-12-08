package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.media.AudioClip
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

class StartUp : Application() {
    private val userManager = UserManager()
    private val preferences = Preferences.userNodeForPackage(LoginWindow::class.java)
    private val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен Яндекс.Диска
    var nodeProcess: Process? = null // Ссылка на процесс Node.js

    var pid = getProcessIdByPort(3000)

    override fun start(primaryStage: Stage) {
        primaryStage.setOnCloseRequest { event ->
            stopNodeServer()
        }

        primaryStage.initStyle(StageStyle.TRANSPARENT) // Прозрачный стиль для окна
        // Установка иконки
        val ico = Image(javaClass.getResourceAsStream("/io/vdartsabvile/avspeak/ico.png"))
        primaryStage.icons.add(ico)

        // Создаем основной контейнер
        val root = BorderPane()
        val titleBar = HBox()
        val versionBar = HBox()
        versionBar.setStyle("-fx-padding: 20;")

        // Применяем закругленные края к root
        root.style = """  
            -fx-background-radius: 13;                    
        """.trimIndent()

        // Создаем текст для анимации (например, "AVSPEAK")
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/ico.png")?.toExternalForm())
        val iconView = javafx.scene.image.ImageView(icon)
        iconView.fitWidth = 76.0 // Установите ширину иконки
        iconView.fitHeight = 76.0 // Установите высоту иконки
        iconView.isPreserveRatio = true // Сохраняем пропорции

        val textString = "AVSPEAK"
        val text = Text()
        val version = Text("1.0-SNAPSHOT")
        version.style = "-fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0); -fx-font-weight: bold; -fx-font-size: 12px;"
        text.style = "-fx-font-family: 'Geologica Roman' ;-fx-font-size: 68px; -fx-font-weight: normal;"
        text.font = Font.font(30.0)

        titleBar.alignment = Pos.CENTER
        root.center = titleBar
        versionBar.alignment = Pos.TOP_RIGHT
        root.bottom = versionBar
        titleBar.children.addAll(iconView, text)
        versionBar.children.addAll(version)

        // Создаем сцену и устанавливаем ее на stage
        val scene = Scene(root, 535.0, 400.0)
        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены
        primaryStage.scene = scene
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        primaryStage.show()

        // Запуск Node.js сервера
        startNodeServer()


        // Загружаем звук
        val typeSound = AudioClip(javaClass.getResource("/io/vdartsabvile/avspeak/p_32488884_182.mp3")?.toExternalForm())

        // Анимация для "печати" текста по буквам
        val textLength = textString.length
        var currentText = ""
        val timeline = Timeline()

        for (i in 0 until textLength) {
            val keyFrame = KeyFrame(
                Duration.seconds(0.1 * (i + 1)), {
                    currentText += textString[i]
                    text.text = currentText
                }
            )
            timeline.keyFrames.add(keyFrame)
        }
        timeline.play()

        // Воспроизводим звук один раз перед началом анимации
        typeSound.play()

        // Таймер для задержки перед проверкой авторизации (задержка 2 секунды)
        val authDelayTimeline = Timeline(
            KeyFrame(Duration.seconds(2.0), {
                val savedUsername = preferences.get("username", null)
                val savedPassword = preferences.get("password", null)


                if (savedUsername != null && savedPassword != null) {
                    val currentUsername = preferences.get("username", null)
                    FriendsWindow(currentUsername).loadFriendsInfo()
                    val (isAuthenticated, avatarImage) = userManager.loginUser(savedUsername, savedPassword, token)
                    if (isAuthenticated) {
                        AppState.username = savedUsername
                        AppState.isAuthenticated.set(true)
                        AppState.updateAvatar(avatarImage)

                        startNodeClient()
                        openMainApp(primaryStage)
                        return@KeyFrame
                    }
                }
                PreLoginWindow().openLoginForm(primaryStage) { success ->
                    if (success) {
                        openMainApp(primaryStage)
                    } else {
                        openMainApp(primaryStage)
                    }
                }
            })
        )
        authDelayTimeline.play()
    }
    fun startNodeClient() {
        val scriptPath = "client.js"
        val workingDirectory = java.io.File("src/main/resources/io/vdartsabvile/avspeak/")

        val username = AppState.username
        val serverUrl = System.getenv("SERVER_URL") ?: "ws://localhost:3000"

        val processBuilder = ProcessBuilder("node", scriptPath, username)
            .directory(workingDirectory)
            .redirectErrorStream(true)

        // Добавляем SERVER_URL в environment переменные процесса
        processBuilder.environment()["SERVER_URL"] = serverUrl

        try {
            nodeProcess = processBuilder.start()
            println("Node.js клиент запущен. Подключение к $serverUrl")

            Thread {
                val reader = nodeProcess?.inputStream?.bufferedReader()
                reader?.lines()?.forEach { line -> println(line) }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun startNodeServer() {
        // Укажите путь к вашему файлу server.js относительно рабочей директории
        val scriptPath = "server.js"


        // Устанавливаем директорию, в которой находится server.js
        val workingDirectory = java.io.File("src/main/resources/io/vdartsabvile/avspeak/")

        val username = preferences.get("username", "unknown") // Загружаем из Preferences
        val processBuilder = ProcessBuilder("node", scriptPath, username)
            .directory(workingDirectory) // Устанавливаем рабочую директорию для процесса
            .redirectErrorStream(true) // Объединить стандартный и поток ошибок

        try {
            nodeProcess = processBuilder.start() // Запускаем процесс и сохраняем ссылку
            println("Node.js сервер запущен.")
            println("PID по порту 3000: $pid")

            // Опционально: обрабатываем вывод сервера
            Thread {
                val reader = nodeProcess?.inputStream?.bufferedReader()
                reader?.lines()?.forEach { line -> println(line) }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun stopNodeServer() {
        try {
            // Попробуем остановить сервер через API
            val url = java.net.URL("http://localhost:3000/stop")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            val responseCode = connection.responseCode
            println("Попытка остановки сервера через API. Код ответа: $responseCode")

            Thread.sleep(1000)

            if (isServerRunning()) {
                println("Сервер все еще работает. Попробуем завершить процесс принудительно.")
                if (pid != null) {
                    val runtime = Runtime.getRuntime()
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        runtime.exec("taskkill /PID $pid /F")
                    } else {
                        runtime.exec("kill -9 $pid")
                    }
                    println("Процесс с PID $pid завершен.")
                } else {
                    println("Процесс на порту 3000 не найден.")
                }
            } else {
                println("Сервер успешно остановлен через API.")
            }
        } catch (e: Exception) {
            println("Ошибка при остановке сервера: ${e.message}")
        }
    }

    fun stopNodeClient() {
        nodeProcess?.let { process ->
            try {
                println("Попытка остановки Node.js клиента...")

                // Сначала пробуем мягко завершить процесс
                process.destroy()

                // Даем немного времени на завершение
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    println("Node.js клиент успешно остановлен.")
                } else {
                    // Если не завершился за 5 секунд, принудительно останавливаем
                    println("Node.js клиент не остановился. Применяем принудительную остановку.")
                    process.destroyForcibly()

                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        println("Node.js клиент принудительно остановлен.")
                    } else {
                        println("Не удалось остановить Node.js клиент.")
                    }
                }
            } catch (e: Exception) {
                println("Ошибка при остановке Node.js клиента: ${e.message}")
            } finally {
                nodeProcess = null
            }
        } ?: println("Node.js клиент не был запущен или уже остановлен.")
    }
    fun killAllNodeProcesses() {
        try {
            val runtime = Runtime.getRuntime()
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                runtime.exec("taskkill /IM node.exe /F")
            } else {
                runtime.exec("pkill -f node")
            }
            println("Все процессы Node.js завершены.")
        } catch (e: Exception) {
            println("Ошибка при завершении процессов Node.js: ${e.message}")
        }
    }


    private fun isServerRunning(): Boolean {
        return try {
            val url = java.net.URL("http://localhost:3000")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }

    fun getProcessIdByPort(port: Int): Int? {
        return try {
            val command = listOf("cmd.exe", "/c", "netstat -ano | findstr :$port")
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            println("Вывод netstat для порта $port:\n$output")

            // Парсим строки вывода для извлечения PID
            val lines = output.lines().filter { it.contains(":$port") }
            val pid = lines.mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 5) parts[4].toIntOrNull() else null
            }.firstOrNull()

            pid
        } catch (e: Exception) {
            println("Ошибка при получении PID: ${e.message}")
            null
        }
    }



    private fun openMainApp(primaryStage: Stage) {
        primaryStage.close()
        MainApp().start(primaryStage)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(StartUp::class.java)
        }
    }
}