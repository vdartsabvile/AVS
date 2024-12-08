package io.vdartsabvile.avspeak.Client

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import io.vdartsabvile.avspeak.*
import javafx.animation.*
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.media.AudioClip
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


class ClientProfile(private val friend: Friend) {
    private lateinit var chatBox: VBox
    private lateinit var scrollPane: ScrollPane

    private lateinit var webSocket: WebSocketClient
    private val chatMessages = mutableListOf<Pair<String, String>>() // Sender and content

    private var isCalling = false
    private lateinit var connectButton: Button
    private lateinit var createServerButton: Button
    private lateinit var settingsButton: Button
    private lateinit var smallAvatarWrapper: StackPane
    private lateinit var serverComboBox: ComboBox<String>
    private var smallAvatarImageView = AppState.smallAvatarImageView
    private lateinit var helpButton: Button
    private var friendsButton = Button("Друзья")
    val smallUserNameLabel = Label(" ${AppState.username ?: "Гость"} ")

    private var xOffset = 0.0
    private var yOffset = 0.0

    val user = Friend(
        AppState.username ?: "Гость",
        friendIp = "127.0.0.1" ?: "Неизвестно",
        AppState.avatarImageView?.image ?: Image(javaClass.getResource("/io/vdartsabvile/avspeak/null.png")?.toExternalForm()),
        true, // предполагаем, что текущий пользователь всегда онлайн
    )

    val participants = HBox(20.0).apply {
        alignment = Pos.CENTER }

    val members = HBox(20.0).apply {    alignment   = Pos.CENTER
        children.addAll(createHeaderBox(friend), participants)}

    fun openFriendProfileWindow(primaryStage: Stage) {
        val root = BorderPane()
        root.style = "-fx-background-radius: 13;"
        // Заголовок
        val titleBar = createTitleBar(primaryStage)
        root.top = titleBar

        initWebSocket(AppState.username ?: "Unknown User") // Определение моего имени

        loadSound() // Загрузка звонка

        // Центральная панель
        val mainContent = VBox(20.0).apply {
            alignment = Pos.CENTER
            padding = Insets(20.0)
            children.addAll(
                members,
                setupButtons(participants, primaryStage),
                createChatBox()
            )
        }
        root.center = mainContent

        // Правая панель
        root.right = createRightPanel(primaryStage)

        // Настройка сцены
        val scene = Scene(root, 1600.0, 900.0).apply {
            fill = Color.TRANSPARENT
        }
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "AVSpeak"
        primaryStage.show()
    }
    private fun initWebSocket(username: String) {
        val serverUrl = "ws://localhost:3000?username=$username"
        webSocket = object : WebSocketClient(URI(serverUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("Connected to server")
            }

            override fun onMessage(message: String?) {
                message?.let {
                    val json = JSONObject(it)
                    when (json.getString("type")) {
                        "chat" -> handleChatMessage(json)
                        "callRequest" -> handleCallRequest(json)
                        "callAccepted" -> handleCallAccepted(json)
                        "callRejected" -> handleCallRejected(json)
                        "callEnded" -> handleCallEnded(json)
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Disconnected from server")
            }

            override fun onError(ex: Exception?) {
                println("Error: ${ex?.message}")
            }
        }
        webSocket.connect()
    }

    private fun handleChatMessage(json: JSONObject) {
        val message = json.getJSONObject("message")
        val sender = message.getString("sender")
        val content = message.getString("content")
        Platform.runLater {
            addMessageToChat(sender, content, chatBox, scrollPane)
        }
    }

    private fun handleCallRequest(json: JSONObject) {
        val from = json.getString("from")
        javafx.application.Platform.runLater {
            val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Incoming Call"
                headerText = "Call from $from"
                contentText = "Do you want to accept the call?"
            }
            alert.showAndWait().ifPresent { result ->
                if (result == ButtonType.OK) {
                    acceptCall(from)
                } else {
                    rejectCall(from)
                }
            }
        }
    }

    private fun acceptCall(from: String) {
        val message = JSONObject().apply {
            put("type", "callAccept")
            put("recipient", from)
        }
        webSocket.send(message.toString())
        // Implement call acceptance logic here
    }

    private fun rejectCall(from: String) {
        val message = JSONObject().apply {
            put("type", "callReject")
            put("recipient", from)
        }
        webSocket.send(message.toString())
        // Implement call rejection logic here
    }

    private fun handleCallAccepted(json: JSONObject) {
        val from = json.getString("from")
        javafx.application.Platform.runLater {
            // Implement call accepted logic here
        }
    }

    private fun handleCallRejected(json: JSONObject) {
        val from = json.getString("from")
        javafx.application.Platform.runLater {
            // Implement call rejected logic here
        }
    }

    private fun handleCallEnded(json: JSONObject) {
        val from = json.getString("from")
        javafx.application.Platform.runLater {
            // Implement call ended logic here
        }
    }

    // Создаем заголовок окна
    private fun createTitleBar(primaryStage: Stage): HBox {
        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT

        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/letter_a.png")?.toExternalForm())
        val iconView = ImageView(icon).apply {
            fitWidth = 34.0
            fitHeight = 34.0
            isPreserveRatio = true
        }
        HBox.setMargin(iconView, javafx.geometry.Insets(0.0, 4.0, 0.0, 4.0)) // 5 пикселей отступа слева

        val appNameLabel = Label("AVSpeak").apply {
            style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal;"
            setOnMouseEntered { style += "-fx-opacity: 0.8;" }
            setOnMouseExited { style = style.replace("-fx-opacity: 0.8;", "-fx-opacity: 1.0;") }
            setOnMouseClicked { MainApp().start(primaryStage) }
        }

        val minimizeButton = createIconButton("−") {
            primaryStage.isIconified = true
        }
        val closeButton = createIconButton("✖") {
            StartUp().stopNodeServer()
            DisconnectServer().notifyServerLogout()
            StartUp().stopNodeClient()
            primaryStage.close()
            StartUp().killAllNodeProcesses()
        }
        val helpButtonTop = createIconButton("?") {
            HelpWindow().openHelpForm(primaryStage)
        }

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        titleBar.children.addAll(iconView, appNameLabel, spacer, helpButtonTop, Label("  "),minimizeButton, closeButton)

        titleBar.setOnMousePressed { event ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
        titleBar.setOnMouseDragged { event ->
            primaryStage.x = event.screenX - xOffset
            primaryStage.y = event.screenY - yOffset
        }

        return titleBar
    }

    // Создаем блок с аватаром и информацией о друге
    // Создаем блок с аватаром и информацией о друге
    private fun createHeaderBox(friend: Friend): StackPane {
        val avatarView = ImageView(friend.avatarImage).apply {
            fitWidth = 100.0
            fitHeight = 100.0
            isPreserveRatio = true
            styleClass.add("avatar-image-view") // Добавляем класс
        }

        val backgroundCircle = Circle(60.0).apply { fill = Color.GRAY }
        val avatarWrapper = StackPane(backgroundCircle, avatarView).apply { alignment = Pos.CENTER }

        avatarWrapper.userData = mapOf(
            "circle" to backgroundCircle,
            "avatarView" to avatarView
        )

        val infoBox = VBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            style = """
            -fx-background-color: rgba(128, 128, 128, 0.22); 
            -fx-background-radius: 10; 
            -fx-padding: 15; 
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0, 0, 5);
        """.trimIndent()

            children.addAll(
                Label(friend.friendUsername).apply {
                    style = "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;"
                },
                Label("IP: ${friend.friendIp}").apply {
                    style = "-fx-text-fill: lightgray; -fx-font-size: 14px;"
                },
                Label("Статус: ${if (friend.isOnline) "ОНЛАЙН" else "ОФФЛАЙН"}").apply {
                    style = "-fx-text-fill: ${if (friend.isOnline) "#00bd00" else "red"}; -fx-font-size: 16px;"
                }
            )
        }

        return StackPane().apply {
            alignment = Pos.CENTER
            children.add(HBox(20.0, avatarWrapper, infoBox).apply {
                alignment = Pos.CENTER
                maxWidth = 360.0
                prefWidth = 340.0
                style = """
                -fx-background-color: rgba(128, 128, 128, 0.22); 
                -fx-background-radius: 10; 
                -fx-padding: 15; 
                -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0, 0, 5);
            """.trimIndent()
            })
        }
    }

    // Создаем панель чата
    private fun createChatBox(): VBox {
        // Основная область чата
        val chatBox = VBox(10.0).apply {
            alignment = Pos.TOP_CENTER
            style = "-fx-background-color: #3a3a3a; -fx-padding: 10; -fx-background-radius: 10;"
            styleClass.add("chat-area")
            prefHeight = 400.0 // Установите стартовую высоту
            maxWidth = 1150.0
        }

        // Добавляем область прокрутки
        val scrollPane = ScrollPane(chatBox).apply {
            style = "-fx-background-color: transparent;"
            isFitToWidth = true
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED // Показывать при необходимости
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            prefHeight = 400.0
            maxWidth = 1150.0
        }

        // Поле ввода сообщения
        val chatInput = TextField().apply {
            promptText = "Напишите сообщение..."
            style = "-fx-background-radius: 8; -fx-font-size: 16px;"
            prefWidth = 400.0

            // Добавляем ограничение на ввод
            textProperty().addListener { _, _, newValue ->
                if (newValue.length > 100) {
                    text = newValue.substring(0, 100)
                }
            }
        }


        // Добавляем обработчик нажатия клавиши
        chatInput.setOnKeyPressed { event ->
            if (event.code == javafx.scene.input.KeyCode.ENTER) {
                sendMessage(chatInput, chatBox, scrollPane) // Вызываем метод отправки сообщения
            }
        }

        // Кнопка отправки сообщения
        val sendButton = Button("⤴").apply {
            style = "-fx-background-color: rgba(0, 189, 0, 0.7); -fx-text-fill: white; -fx-background-radius: 15; -fx-font-size: 16px;"
            setOnAction {
                sendMessage(chatInput, chatBox, scrollPane) // Вызываем метод отправки сообщения
            }
        }

        // Контейнер для ввода сообщения и кнопки
        val chatControls = HBox(10.0, chatInput, sendButton).apply {
            alignment = Pos.CENTER
        }

        // Возвращаем основную область чата с прокруткой

        return VBox(10.0, scrollPane, chatControls).apply {
            alignment = Pos.CENTER // Устанавливаем выравнивание на центр
        }
    }

    /*fun sendMessage(chatInput: TextField, chatBox: VBox, scrollPane: ScrollPane) {
        val messageText = chatInput.text.trim()
        if (messageText.isNotBlank()) {
            // Создаём текст сообщения
            val messageLabel = Label(messageText).apply {
                style = "-fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 10; -fx-background-radius: 15; -fx-wrap-text: true;"
                maxWidth = 990.0 // Ограничиваем ширину текста
                isWrapText = true // Включаем перенос текста
            }

            // Метка времени
            val timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
            val timestampLabel = Label(timestamp).apply {
                style = "-fx-text-fill: lightgray; -fx-font-size: 11px;"
                padding = Insets(0.0, 10.0, 0.0, 10.0) // Отступы внутри "пузырика"
            }

            // Контейнер для текста и времени
            val bubbleContainer = VBox(2.0, messageLabel, timestampLabel).apply {
                alignment = Pos.CENTER_RIGHT
                style = "-fx-background-radius: 15; -fx-background-color: rgb(76, 175, 80, 0.4);"
                padding = Insets(5.0)
                maxHeight = 400.0
                maxWidth = Double.MAX_VALUE // Контейнер растягивается
            }

            // Создаем новый мини-аватар
            val miniAvatarView = ImageView(AppState.avatarImageView?.image).apply {
                fitWidth = 50.0
                fitHeight = 50.0
                isPreserveRatio = true
                clip = Circle(25.0, 25.0, 25.0) // Обрезаем до круга
            }
            val miniBackgroundCircle = Circle(25.0).apply { fill = Color.GRAY }
            val miniAvatarWrapper = StackPane(miniBackgroundCircle, miniAvatarView)

            // Контейнер для аватара и текста
            val messageBox = HBox(10.0, bubbleContainer, miniAvatarWrapper).apply {
                alignment = Pos.TOP_RIGHT
                maxWidth = chatBox.width - 20 // Учитываем отступы
            }

            // Добавляем сообщение в общий чат
            chatBox.children.add(messageBox)
            chatInput.clear() // Очищаем поле ввода

            // Автоматическая прокрутка вниз
            javafx.application.Platform.runLater {
                chatBox.layout() // Пересчитываем расположение всех элементов
                scrollPane.layout() // Пересчитываем ScrollPane
                scrollPane.vvalue = 1.0 // Устанавливаем прокрутку на самый низ
            }
        }
    } */
    fun sendMessage(chatInput: TextField, chatBox: VBox, scrollPane: ScrollPane) {
        val messageText = chatInput.text.trim()
        if (messageText.isNotBlank()) {
            val message = JSONObject().apply {
                put("type", "chat")
                put("recipient", friend.friendUsername)
                put("content", messageText)
            }
            webSocket.send(message.toString())

            // Add message to local chat
            addMessageToChat(AppState.username ?: "Me", messageText, chatBox, scrollPane)

            chatInput.clear()
        }
    }

    private fun addMessageToChat(sender: String, content: String, chatBox: VBox, scrollPane: ScrollPane) {
        // Создаём текст сообщения
        val messageLabel = Label(content).apply {
            style = "-fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 10; -fx-background-radius: 15; -fx-wrap-text: true;"
            maxWidth = 990.0 // Ограничиваем ширину текста
            isWrapText = true // Включаем перенос текста
        }

        // Метка времени
        val timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
        val timestampLabel = Label(timestamp).apply {
            style = "-fx-text-fill: lightgray; -fx-font-size: 11px;"
            padding = Insets(0.0, 10.0, 0.0, 10.0) // Отступы внутри "пузырика"
        }

        // Контейнер для текста и времени
        val bubbleContainer = VBox(2.0, messageLabel, timestampLabel).apply {
            alignment = if (sender == AppState.username) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            style = "-fx-background-radius: 15; -fx-background-color: ${if (sender == AppState.username) "rgb(76, 175, 80, 0.4)" else "rgb(33, 150, 243, 0.4)"};"
            padding = Insets(5.0)
            maxHeight = 400.0
            maxWidth = Double.MAX_VALUE // Контейнер растягивается
        }

        // Создаем новый мини-аватар
        val miniAvatarView = ImageView(if (sender == AppState.username) AppState.avatarImageView?.image else friend.avatarImage).apply {
            fitWidth = 50.0
            fitHeight = 50.0
            isPreserveRatio = true
            clip = Circle(25.0, 25.0, 25.0) // Обрезаем до круга
        }
        val miniBackgroundCircle = Circle(25.0).apply { fill = Color.GRAY }
        val miniAvatarWrapper = StackPane(miniBackgroundCircle, miniAvatarView)

        // Контейнер для аватара и текста
        val messageBox = HBox(10.0).apply {
            children.addAll(if (sender == AppState.username) listOf(bubbleContainer, miniAvatarWrapper) else listOf(miniAvatarWrapper, bubbleContainer))
            alignment = if (sender == AppState.username) Pos.TOP_RIGHT else Pos.TOP_LEFT
            maxWidth = chatBox.width - 20 // Учитываем отступы
        }

        // Добавляем сообщение в общий чат
        chatBox.children.add(messageBox)

        // Автоматическая прокрутка вниз
        Platform.runLater {
            chatBox.layout() // Пересчитываем расположение всех элементов
            scrollPane.layout() // Пересчитываем ScrollPane
            scrollPane.vvalue = 1.0 // Устанавливаем прокрутку на самый низ
        }
    }


    private fun createSVGIconButton(svgPath: String, backgroundColor: String): Button {
        val button = Button()

        // Создаем SVG иконку с целевым размером
        val svgGraphic = createSVGIconGraphic(svgPath, 40.0)

        // Присваиваем SVG в качестве графики кнопки
        button.graphic = svgGraphic

        // Стиль кнопки
        button.style = """
            -fx-background-color: $backgroundColor;
            -fx-background-radius: 50;
            -fx-border-radius: 50;
            -fx-content-display: center;
            -fx-padding: 23;
        """.trimIndent()

        // Устанавливаем размеры кнопки
        button.prefWidth = 70.0
        button.prefHeight = 70.0
        button.maxWidth = 70.0
        button.maxHeight = 70.0
        button.minWidth = 70.0
        button.minHeight = 70.0

        // Анимация при наведении
        button.setOnMouseEntered { animateButtonHover(button, true) }
        button.setOnMouseExited { animateButtonHover(button, false) }

        return button
    }


    private fun animateButtonHover(button: Button, hover: Boolean) {
        val scaleTransition = ScaleTransition(javafx.util.Duration.millis(140.0), button)
        scaleTransition.toX = if (hover) 1.07 else 1.0
        scaleTransition.toY = if (hover) 1.07 else 1.0
        scaleTransition.play()
    }

    private fun createSVGIconGraphic(svgPath: String, targetSize: Double = 40.0): Node {
        val svgIcon = SVGPath()
        svgIcon.content = svgPath
        svgIcon.fill = Color.WHITE

        val wrapper = StackPane(svgIcon)
        wrapper.maxWidth = targetSize
        wrapper.maxHeight = targetSize
        wrapper.minWidth = targetSize
        wrapper.minHeight = targetSize
        wrapper.prefWidth = targetSize
        wrapper.prefHeight = targetSize

        // Настраиваем масштабирование после того, как SVG будет добавлен в сцену
        val bounds = svgIcon.boundsInLocal
        val scale = minOf(targetSize / bounds.width, targetSize / bounds.height)
        svgIcon.scaleX = scale
        svgIcon.scaleY = scale

        return wrapper
    }

    fun setupButtons(participants: HBox, primaryStage: Stage): HBox {
        // Создаем кнопки с начальными иконками
        val svgMicContent = "m 99.195934,186.7 c -0.727306,-0.495 -1.698925,-1.40791 -2.159153,-2.02869 -0.805083,-1.08593 -0.841126,-1.64815 -0.951538,-14.84255 l -0.114757,-13.71386 -2.97198,-0.25123 c -14.049309,-1.1876 -28.202846,-8.36925 -37.665374,-19.11179 -6.802508,-7.72269 -11.020629,-16.1613 -13.408984,-26.82549 -0.873721,-3.90123 -0.924147,-4.92685 -0.924147,-18.796082 0,-14.257744 0.0235,-14.701434 0.836779,-15.8 1.598167,-2.158777 3.326735,-3.020549 5.679809,-2.831651 2.405893,0.193138 3.884187,1.107191 5.083412,3.143156 0.747079,1.268343 0.815249,2.310764 1.030517,15.758188 0.22552,14.087899 0.254203,14.486699 1.323397,18.399999 1.31807,4.8242 3.869587,10.34601 6.617348,14.32079 2.42622,3.50965 8.06053,9.09214 11.540657,11.43451 3.66667,2.46793 9.345712,5.07836 13.688081,6.29188 3.510417,0.98103 4.541577,1.0762 13.531179,1.24885 10.68513,0.20521 14.18394,-0.1146 19.45936,-1.77866 12.90709,-4.07137 23.90979,-14.46088 28.60295,-27.00889 2.38691,-6.38183 2.52059,-7.49788 2.77166,-23.140533 L 151.4,76.535893 l 1.15273,-1.510388 c 2.75985,-3.616154 7.61296,-3.474062 10.41049,0.304803 0.81335,1.098651 0.83674,1.541155 0.83542,15.8 -0.001,16.087402 -0.11727,17.176972 -2.68502,25.269692 -6.82235,21.50186 -26.89928,37.56909 -49.31212,39.46367 l -2.97198,0.25123 -0.11476,13.71386 c -0.11041,13.1944 -0.14645,13.75662 -0.95154,14.84255 -1.4192,1.91429 -3.27685,2.92869 -5.36322,2.92869 -1.2929,0 -2.29547,-0.28161 -3.204066,-0.9 z m -3.129245,-54.74826 c -1.173322,-0.19511 -3.848603,-0.92353 -5.945069,-1.6187 C 78.374555,126.43778 69.089751,116.46849 65.674816,104.08399 L 64.603836,100.2 64.601918,74.800001 C 64.600186,51.846375 64.667874,49.135607 65.30471,46.654875 69.847621,28.958404 84.648128,17.21051 102.4,17.21051 c 7.83032,0 14.703,2.086786 21.21234,6.440811 7.56859,5.062544 12.98542,12.692156 15.50686,21.841414 l 1.0768,3.907266 0.002,25.2 c 0.002,29.167029 0.0598,28.637469 -3.99426,36.799999 -5.47497,11.02342 -15.75589,18.63124 -27.80374,20.57457 -3.0542,0.49265 -9.30293,0.48108 -12.333311,-0.0228 z m 12.149271,-12.95571 c 1.31122,-0.33219 3.87402,-1.33338 5.69511,-2.22486 5.6281,-2.75516 10.42228,-8.37857 12.59298,-14.77117 0.86417,-2.544915 0.88551,-3.09236 1.0073,-25.839778 0.14073,-26.282252 0.0971,-26.801995 -2.72358,-32.406142 -2.93017,-5.821815 -7.95252,-10.265587 -14.11731,-12.490988 -2.52232,-0.910523 -3.50113,-1.037828 -8.07046,-1.049644 -4.454625,-0.01152 -5.627139,0.126432 -8.179892,0.962402 -6.099712,1.997521 -11.5544,6.762155 -14.452484,12.624151 -2.700615,5.462573 -2.753732,6.060538 -2.753732,31 0,24.269213 0.0904,25.443219 2.32274,30.164979 3.283113,6.9443 9.368625,11.99477 16.863369,13.99518 2.90041,0.77414 8.830759,0.79214 11.815959,0.0359 z"
        val svgMicMuteContent = "m 100.76543,187.6302 c -0.56901,-0.17931 -1.501507,-0.63196 -2.072207,-1.0059 -0.570699,-0.37394 -1.365142,-1.16238 -1.765428,-1.75209 -0.658416,-0.97 -0.746859,-2.3877 -0.927794,-14.87221 l -0.2,-13.8 -3.2,-0.28204 c -1.76,-0.15512 -4.91,-0.6724 -7,-1.14951 -2.09,-0.47711 -5.24,-1.39724 -7,-2.04475 -1.76,-0.64751 -5.143983,-2.18732 -7.519962,-3.4218 l -4.319962,-2.24451 -12.080038,12.04185 c -10.452395,10.41936 -12.282146,12.07877 -13.580038,12.31583 -0.825,0.15068 -2.166107,0.149 -2.980237,-0.004 -0.928996,-0.17428 -2.033841,-0.8313 -2.966822,-1.76428 -0.932982,-0.93298 -1.589999,-2.03782 -1.76428,-2.96682 -0.152732,-0.81413 -0.154332,-2.15524 -0.0036,-2.98024 0.236849,-1.29596 1.854465,-3.08557 11.892042,-13.15649 l 11.617904,-11.65649 -2.595377,-3.14351 C 52.872218,134.01458 50.893628,131.34 49.902809,129.8 c -0.990819,-1.54 -2.433587,-4.06 -3.206151,-5.6 -0.772564,-1.54 -1.97973,-4.42 -2.682591,-6.4 -0.702861,-1.98 -1.649032,-5.13 -2.102603,-7 -0.737547,-3.04079 -0.843312,-4.99213 -1.001092,-18.469955 -0.160521,-13.711955 -0.113741,-15.202039 0.51912,-16.535695 0.382546,-0.806157 1.384294,-1.921536 2.226106,-2.478621 1.319981,-0.873521 1.861786,-0.988923 3.937892,-0.838751 1.983154,0.143448 2.624985,0.381067 3.642628,1.348577 0.679417,0.645945 1.489234,1.660507 1.799593,2.254582 0.448051,0.857638 0.566834,3.53552 0.57664,13 0.0068,6.555926 0.185333,13.252543 0.396755,14.881373 0.211423,1.62883 0.676097,4.14883 1.03261,5.6 0.356513,1.45117 1.19375,3.98849 1.860527,5.63849 0.666777,1.65 2.041545,4.41391 3.05504,6.14202 1.013495,1.72811 2.809776,4.31505 3.991735,5.74876 l 2.149018,2.60674 4.394445,-4.39444 4.394446,-4.39445 -2.243282,-2.75431 C 71.40984,116.63944 69.507283,113.6 68.415741,111.4 c -1.091543,-2.2 -2.39131,-5.44 -2.888373,-7.2 -0.887097,-3.14104 -0.906121,-3.65801 -1.032487,-28.057351 -0.121066,-23.376247 -0.0822,-25.085171 0.652349,-28.680831 0.429597,-2.102914 1.412721,-5.284443 2.18472,-7.070065 0.771999,-1.785622 2.003128,-4.251099 2.735842,-5.478838 0.732715,-1.227739 2.76106,-3.750462 4.507432,-5.606051 1.746373,-1.85559 4.266373,-4.152413 5.6,-5.104053 1.333628,-0.951639 3.891419,-2.462093 5.683982,-3.356564 1.792562,-0.894471 4.582562,-1.986574 6.2,-2.426895 1.617437,-0.440321 4.605795,-0.986565 6.640795,-1.213875 2.560559,-0.286014 4.839439,-0.284253 7.399999,0.0057 2.035,0.230455 4.70054,0.678211 5.92342,0.995014 1.22288,0.316804 3.54313,1.120212 5.15612,1.785352 1.61298,0.66514 4.41132,2.193191 6.21853,3.39567 1.8718,1.245452 4.73096,3.714052 6.64389,5.73634 1.84692,1.952509 3.95754,4.556122 4.69025,5.785806 0.73272,1.229685 1.96325,3.695375 2.73452,5.479311 0.77127,1.783935 1.74441,4.901273 2.16253,6.927416 0.41813,2.026143 0.7626,4.669534 0.76549,5.874201 l 0.005,2.190305 10.9,-10.860309 c 9.38922,-9.35503 11.10791,-10.898358 12.4,-11.134821 0.825,-0.150982 2.16611,-0.14955 2.98024,0.0032 0.929,0.174281 2.03384,0.831298 2.96682,1.76428 0.93298,0.932981 1.59,2.037826 1.76428,2.966822 0.15273,0.81413 0.1549,2.155237 0.005,2.980237 -0.23809,1.308776 -2.22941,3.463226 -15.62043,16.9 l -15.34755,15.4 -0.15356,13.8 c -0.14277,12.829419 -0.2168,14.024219 -1.05266,16.988049 -0.4945,1.75343 -1.49974,4.48807 -2.23386,6.07697 -0.73412,1.58891 -2.02626,3.93247 -2.87143,5.20792 -0.84517,1.27545 -2.68714,3.5397 -4.09328,5.03168 -1.40613,1.49197 -3.65613,3.53932 -5,4.54965 -1.34386,1.01034 -3.79339,2.51104 -5.44339,3.3349 -1.65,0.82385 -4.17,1.87415 -5.6,2.33398 -1.43,0.45984 -3.66806,1.02524 -4.97346,1.25646 -1.30541,0.23122 -4.30457,0.42039 -6.66481,0.42039 -2.36025,0 -5.612188,-0.25459 -7.226541,-0.56576 -1.614353,-0.31116 -4.195188,-1.04581 -5.735188,-1.63255 -1.54,-0.58673 -3.16,-1.15135 -3.6,-1.25471 -0.605434,-0.14222 -1.859759,0.87845 -5.157442,4.1967 l -4.357443,4.38463 2.062605,1.0452 c 1.134433,0.57486 3.636071,1.57236 5.559195,2.21667 1.923123,0.6443 5.094379,1.45009 7.047233,1.79064 2.397382,0.41807 6.115038,0.61918 11.445851,0.61918 5.25938,0 9.08118,-0.20379 11.44761,-0.61043 1.95381,-0.33573 4.72239,-0.99009 6.15239,-1.45412 1.43,-0.46404 3.72527,-1.34818 5.1006,-1.96476 1.37532,-0.61658 4.07532,-2.1456 6,-3.39781 1.92467,-1.25222 5.04414,-3.82839 6.93215,-5.72482 1.88802,-1.89643 4.20389,-4.59392 5.14639,-5.99442 0.9425,-1.4005 2.39839,-3.9205 3.2353,-5.6 0.83692,-1.6795 1.90022,-4.18536 2.3629,-5.56857 0.46267,-1.38321 1.12673,-3.72321 1.47567,-5.2 0.49284,-2.08572 0.67345,-5.76545 0.80914,-16.485069 0.10108,-7.985526 0.34674,-14.241346 0.58304,-14.847485 0.2246,-0.576117 1.02431,-1.644487 1.77714,-2.374155 1.17386,-1.137752 1.71215,-1.351506 3.78014,-1.501091 2.08061,-0.150497 2.62131,-0.03549 3.94193,0.838459 0.84182,0.557085 1.84356,1.672464 2.22611,2.478621 0.63295,1.333851 0.67952,2.82174 0.51751,16.535695 -0.16036,13.575235 -0.26119,15.409315 -1.01655,18.491425 -0.46119,1.88181 -1.40664,5.03181 -2.101,7 -0.69435,1.96819 -1.89344,4.83853 -2.66464,6.37853 -0.7712,1.54 -2.34545,4.24 -3.49834,6 -1.15289,1.76 -3.28417,4.58052 -4.73618,6.26781 -1.45201,1.6873 -3.89895,4.15134 -5.43764,5.47565 -1.5387,1.32431 -4.11348,3.29577 -5.72175,4.38102 -1.60826,1.08524 -4.61898,2.82438 -6.69047,3.86475 -2.0715,1.04037 -5.57875,2.48501 -7.7939,3.21031 -2.21515,0.7253 -5.82755,1.60636 -8.02755,1.95791 -2.2,0.35155 -4.72,0.68494 -5.6,0.74087 L 109,156.2 108.8,170 c -0.18232,12.58028 -0.26553,13.8966 -0.94139,14.89296 -0.40776,0.60112 -1.30776,1.44101 -2,1.86641 -0.69223,0.42541 -1.88861,0.86872 -2.65861,0.98515 -0.77,0.11643 -1.86556,0.065 -2.43457,-0.11432 z M 110.2,118.24081 c 1.76,-0.56259 4.30583,-1.74849 5.6574,-2.63533 1.35156,-0.88684 3.54496,-2.77506 4.87422,-4.19604 1.51869,-1.62348 3.02696,-3.81489 4.05865,-5.89694 1.05378,-2.12662 1.84599,-4.45934 2.21183,-6.512926 0.31349,-1.759765 0.57627,-5.358729 0.58395,-7.997698 L 127.6,86.203751 111.6,102.2 c -8.8,8.79794 -15.999999,16.11837 -15.999999,16.26762 0,0.14925 0.945,0.45568 2.1,0.68095 1.155,0.22528 3.719999,0.34334 5.699999,0.26236 2.41731,-0.0989 4.65128,-0.48329 6.8,-1.17012 z m 17.38605,-57.239433 c -0.008,-4.265128 -0.23553,-8.424579 -0.55737,-10.201376 -0.29887,-1.65 -0.9302,-3.970947 -1.40293,-5.15766 -0.47274,-1.186712 -1.49337,-3.112582 -2.26807,-4.27971 -0.77469,-1.167129 -2.37232,-3.069486 -3.55027,-4.227462 -1.17796,-1.157976 -3.28152,-2.795146 -4.67458,-3.638156 -1.39305,-0.843009 -3.79283,-1.9292 -5.33283,-2.413757 -2.20763,-0.694623 -3.77318,-0.881011 -7.4,-0.881011 -3.626816,0 -5.19237,0.186388 -7.399999,0.881011 -1.54,0.484557 -3.939774,1.570748 -5.332832,2.413757 -1.393058,0.84301 -3.496615,2.48018 -4.674572,3.638156 -1.177957,1.157976 -2.817397,3.12333 -3.643199,4.367453 -0.825802,1.244124 -2.052245,3.799994 -2.725428,5.679711 -1.223969,3.417668 -1.223969,3.417668 -1.34822,25.700483 -0.102268,18.340335 -0.01892,22.893186 0.471076,25.732611 0.367398,2.128983 1.14989,4.617543 2.043777,6.499813 0.796647,1.67751 2.096275,3.84061 2.888063,4.80688 l 1.439615,1.75686 L 105.85914,89.940869 127.6,68.202753 Z"
        val svgStreanContent = "m 189.38775,394.326 c -1.16174,-0.23595 -3.3606,-1.732 -4.88636,-3.32455 -4.50156,-4.69861 -3.86344,-8.31983 4.35662,-24.72295 L 195.98479,352.05701 149.01441,351.7785 102.04403,351.5 99.272016,348.72743 96.5,345.95486 96.246317,235.72743 C 95.998232,127.93188 96.035693,125.42965 97.9443,122.31137 c 1.309461,-2.1394 3.20274,-3.53605 5.75368,-4.24443 5.30995,-1.47453 299.29409,-1.47453 304.60404,0 2.55094,0.70838 4.44422,2.10503 5.75368,4.24443 1.90861,3.11828 1.94607,5.62051 1.69798,113.41606 L 415.5,345.95486 l -2.77202,2.77257 -2.77201,2.77257 -46.97038,0.2785 -46.97038,0.27851 7.12678,14.22149 c 8.40568,16.77354 8.93431,20.10646 3.97563,25.06591 l -3.1551,3.15559 -66.23126,0.1275 c -36.42719,0.0701 -67.18177,-0.0655 -68.34351,-0.3015 z M 302,372.48183 c 0,-0.28499 -2.2541,-5.00999 -5.00912,-10.5 L 291.98177,352 H 256 220.01823 l -5.00911,9.98183 C 212.2541,367.47184 210,372.19684 210,372.48183 210,372.76682 230.7,373 256,373 c 25.3,0 46,-0.23318 46,-0.51817 z M 395,320 V 309 H 256 117 v 11 11 h 139 139 z m 0,-106.5 V 139 H 256 117 v 74.5 74.5 h 139 139 z"
        val svgStreamStopContent = "m 252.51701,525.76802 c -1.54898,-0.3146 -4.4808,-2.30933 -6.51515,-4.43274 -6.00208,-6.26482 -5.15125,-11.0931 5.80883,-32.96393 l 9.50238,-18.96199 -62.62718,-0.37134 -62.62718,-0.37134 -3.69602,-3.69676 -3.69602,-3.69675 -0.35065,-146.6693 c -0.38929,-162.82901 -0.9259,-152.8138 8.39638,-156.7089 6.80237,-2.84221 402.43952,-2.84221 409.24188,0 9.32229,3.8951 8.78568,-6.12011 8.39639,156.7089 l -0.35065,146.6693 -3.69603,3.69675 -3.69602,3.69676 -62.62717,0.37134 -62.62718,0.37134 9.50238,18.96199 c 11.20758,22.36472 11.91241,26.80861 5.30084,33.42121 l -4.20681,4.20746 -88.30834,0.17 c -48.56959,0.0935 -89.5757,-0.0874 -91.12468,-0.402 z m 150.14967,-29.12556 c 0,-0.37999 -3.00547,-6.67999 -6.67882,-14 l -6.67883,-13.30911 h -47.97569 -47.97568 l -6.67883,13.30911 c -3.67335,7.32001 -6.67882,13.62001 -6.67882,14 0,0.37999 27.6,0.69089 61.33333,0.69089 33.73334,0 61.33334,-0.3109 61.33334,-0.69089 z m 124,-69.97578 V 412.00001 H 341.33334 156 v 14.66667 14.66667 h 185.33334 185.33334 z m 0,-142 V 185.33334 H 341.33334 156 v 99.33334 99.33333 H 341.33334 526.66668 Z M 233.8505,369.94576 c -7.48142,-3.78761 -9.91633,-13.76607 -4.93289,-20.21538 1.32865,-1.71948 20.55114,-15.33245 42.71665,-30.25104 36.78472,-24.75812 40.10016,-27.29658 38,-29.09467 -1.26551,-1.08348 -19.24242,-13.49971 -39.9487,-27.59163 -20.70628,-14.09191 -38.55628,-26.91873 -39.66667,-28.50402 -5.99276,-8.55586 1.26175,-20.95568 12.2601,-20.95568 4.08753,0 7.2934,2.03994 60.21125,38.31332 19.0255,13.04133 34.63481,22.8736 35.6778,22.47337 1.01982,-0.39134 21.03291,-13.62844 44.47352,-29.41578 35.68777,-24.03584 43.3918,-28.70424 47.36912,-28.70424 9.18615,0 15.14588,7.417 13.40712,16.68541 -1.0451,5.57087 1.13306,3.89101 -45.79005,35.31459 -19.16332,12.83333 -34.8691,23.67194 -34.90173,24.08578 -0.0326,0.41385 18.28068,13.27807 40.69625,28.58716 22.41558,15.30908 41.31558,28.88103 42,30.15989 0.68443,1.27886 1.24441,4.79191 1.24441,7.80679 0,9.00768 -9.06096,15.55766 -17.24725,12.46768 -1.89399,-0.71491 -23.45174,-14.99385 -47.90611,-31.73099 l -44.46247,-30.43117 -45.88707,30.89976 c -25.23788,16.99487 -47.52432,31.19268 -49.52542,31.55069 -2.00109,0.35801 -5.50563,-0.29442 -7.78786,-1.44984 z"
        val svgCallContent = "m 401,509.56619 c -31.11267,-4.81275 -56.06863,-10.897 -84.28568,-20.5488 C 185.35306,444.08453 80.854143,344.03666 30.506123,215 17.08964,180.61497 7.8548154,145.67657 2.8988562,110.55255 -0.71224555,84.959855 0.39698039,73.898676 8.1654839,58.033849 16.005522,42.022935 32.294119,29.112131 50.460099,24.509939 c 8.759957,-2.219259 90.319841,-2.219259 99.079801,0 18.67254,4.730525 34.56432,17.560907 42.82926,34.578624 3.9136,8.058193 4.94419,11.568245 7.16746,24.411437 3.27499,18.9186 6.29762,30.94335 12.69407,50.5 4.75669,14.5432 5.0725,16.14815 5.11624,26 0.0547,12.32361 -1.71041,20.01047 -7.11847,31 -2.9712,6.03766 -6.08298,9.95707 -15.95964,20.10176 C 187.52097,218.03273 182,224.01249 182,224.39012 c 0,1.49635 12.51945,19.17854 22.15896,31.29681 20.45401,25.71369 55.68622,57.57212 79.78396,72.144 l 3.44293,2.08193 12.61176,-12.29659 c 14.28654,-13.92951 22.8325,-19.10718 36.58862,-22.16762 12.8241,-2.8531 23.71857,-1.73729 42.41377,4.34398 19.829,6.45008 28.34207,8.59889 46.33704,11.69607 8.02737,1.38162 16.74796,3.22827 19.37909,4.10368 15.21181,5.06115 29.90957,17.93406 37.0017,32.40762 6.80194,13.88134 7.28217,18.10185 7.28217,64 0,32.47477 -0.29707,41.52253 -1.56487,47.66032 -4.76753,23.08112 -21.57118,41.83121 -43.85672,48.93684 -11.67516,3.72257 -23.09731,3.98252 -42.57841,0.96903 z m 34.42736,-42.40388 c 5.25892,-2.84146 9.82261,-8.80357 11.57956,-15.12779 0.55977,-2.01494 0.98432,-19.26594 0.9875,-40.12559 0.006,-40.26049 -0.28308,-42.69778 -5.90977,-49.80204 -4.33377,-5.47184 -9.53911,-7.63076 -24.34182,-10.09582 -16.73871,-2.78745 -33.06255,-6.85526 -49.99849,-12.45934 -15.14639,-5.01192 -18.05272,-5.34282 -25.3162,-2.88235 -4.14922,1.40553 -7.85538,4.54576 -23.44845,19.86788 -17.28453,16.98418 -18.8549,18.27025 -23.53465,19.27388 -6.38852,1.37009 -10.5182,0.43083 -19.13737,-4.3526 -30.78009,-17.08224 -69.21864,-48.97161 -95.22087,-78.99707 -23.18649,-26.77407 -46.09427,-61.71869 -46.08122,-70.29452 0.0135,-8.86221 1.27436,-10.74438 19.53924,-29.16695 11.88249,-11.98508 17.99011,-18.91812 19.38073,-22 3.1846,-7.05763 2.71356,-13.60786 -1.9776,-27.50026 -5.3011,-15.69871 -9.29557,-31.61459 -12.41961,-49.485715 -2.9728,-17.00593 -5.2889,-21.734212 -12.97469,-26.487589 L 141.66017,64.5 101.58009,64.213284 C 56.774861,63.892766 56.466578,63.931264 49.476552,70.719891 43.2723,76.745384 41.615469,81.790357 42.338553,92.454767 43.610449,111.21334 51.425135,148.18332 59.575523,174 80.987839,241.82436 124.09825,310.03811 175.46105,357.36608 241.74438,418.44248 323.4745,457.31312 411,469.38782 c 12.70152,1.75226 17.95044,1.27404 24.42736,-2.22551 z M 330.5,198.50852 c -0.55,-0.18347 -2.22925,-0.60444 -3.73166,-0.9355 -3.85333,-0.84908 -9.75454,-6.33439 -11.90336,-11.06444 -2.34226,-5.15588 -2.32017,-11.94459 0.0553,-17.00858 1.27085,-2.70914 21.91885,-24.00835 63.98207,-66 l 62.10567,-62 -46.41236,-0.5 -46.41236,-0.5 -3.84168,-3.092825 C 336.56585,31.147073 333.94581,24.072193 336.1547,15.299892 337.62684,9.4535277 344.11436,2.6172803 349.64871,1.0805474 352.21914,0.3668139 377.11326,0.01660323 424.5,0.02753893 c 66.91122,0.01544148 71.23443,0.12285979 75.07074,1.86527437 5.51798,2.5062111 10.12188,8.3444027 11.47428,14.5505137 0.76171,3.495423 1.00205,26.514826 0.77845,74.556673 -0.32083,68.93226 -0.34106,69.53268 -2.47615,73.5 -4.06502,7.55339 -10.53575,11.5 -18.85504,11.5 -8.33582,0 -17.21096,-6.7242 -19.43215,-14.72267 -0.65757,-2.36788 -1.05106,-20.00753 -1.05455,-47.27435 L 470,70.505952 407.2134,133.28662 c -57.89343,57.88797 -63.16632,62.89495 -67.65893,64.24702 -4.72804,1.42293 -6.98338,1.66576 -9.05447,0.97488 z"
        val svgCallStopContent = "M 12.5,510.10414 C 1.7969134,505.14877 -2.6700635,493.5481 2.0632262,483 5.0135585,476.42521 475.19137,6.1107368 482.44634,2.4772575 488.34773,-0.47830722 493.95886,-0.66960217 499.5,1.8958635 510.20398,6.8516421 514.6711,18.454531 509.93512,29 508.35639,32.515324 480.56607,60.858672 382.9325,158.53 l -124.98167,125.03001 3.27458,3.06644 C 269.41501,294.29549 295.02062,313 297.32958,313 c 0.52505,0 6.22516,-5.11804 12.66692,-11.37341 17.94995,-17.43059 29.54446,-22.28786 50.97771,-21.356 10.33136,0.44917 12.88023,0.96577 24.59563,4.98495 14.81848,5.08374 30.29811,8.96005 44.93016,11.2511 12.13142,1.89951 17.96934,3.68695 26.52153,8.1203 12.88718,6.68055 23.65867,18.84784 29.1068,32.87853 4.31631,11.11586 5.14575,22.36347 4.63465,62.84751 -0.4176,33.07785 -0.57661,35.55912 -2.70105,42.14702 -3.72039,11.53697 -7.90989,18.37632 -16.54775,27.01418 -17.13685,17.13686 -33.92917,21.32479 -66.18051,16.50515 C 321.13802,473.43711 241.78448,437.24471 177.65089,382.17536 l -9.84911,-8.4571 -67.15089,67.09497 C 49.075363,492.34581 32.456244,508.37764 29,509.93214 c -5.60231,2.51972 -11.300862,2.57912 -16.5,0.172 z m 421.57772,-64.24557 c 5.22674,-1.56597 10.33475,-6.01345 12.83249,-11.17308 2.00624,-4.14434 2.08979,-5.8217 2.08979,-41.95416 0,-27.07609 -0.33707,-38.42209 -1.20124,-40.43428 -4.07945,-9.49882 -9.35537,-12.6545 -25.58312,-15.30198 -14.73263,-2.40355 -39.81244,-8.74792 -51.03386,-12.90989 -9.34084,-3.46446 -16.18743,-3.94531 -21.94176,-1.541 -2.25046,0.9403 -10.15696,7.96098 -20.21788,17.95272 C 311.63664,357.76287 308.37405,360 300.57917,360 295.04191,360 293.15548,359.18978 280,351.16129 265.69866,342.43351 255.7914,335.39793 240.88209,323.38195 l -12.61792,-10.16926 -15.64623,15.6692 -15.64624,15.66919 10.26415,8.58296 c 58.68568,49.07341 127.73528,80.37836 202.713,91.90378 13.9643,2.14656 19.11852,2.32189 24.12887,0.82075 z M 101.1758,291.40031 C 95.703585,288.6734 94.03118,286.67058 85.811018,273 56.588984,224.40215 37.295722,172.0797 27.366083,114.5 23.874282,94.251845 23.482057,75.488946 26.373131,67 31.533891,51.846659 40.523915,39.881641 52.741275,31.906064 c 8.761712,-5.719707 17.06284,-8.626544 28.33618,-9.922586 11.112802,-1.277585 66.009115,-1.243565 77.281745,0.04789 21.15196,2.423294 39.62931,14.843763 49.01386,32.947095 4.43335,8.552192 6.22079,14.39011 8.1203,26.521534 2.29105,14.632048 6.16736,30.11168 11.2511,44.93016 4.01918,11.7154 4.53578,14.26427 4.98495,24.59563 0.58988,13.56751 -0.79121,20.45875 -6.40803,31.97421 -3.19521,6.55075 -6.01001,9.885 -22.2398,26.34393 -10.21987,10.36417 -19.84327,19.50073 -21.38534,20.30349 -1.54207,0.80275 -5.01333,1.72123 -7.7139,2.04108 -4.03488,0.47787 -5.87403,0.10994 -10.31758,-2.06409 -7.05088,-3.44967 -10.84683,-8.77476 -11.45134,-16.06435 -0.78273,-9.43871 0.4988,-11.48186 18.89447,-30.12353 10.08541,-10.22029 17.45065,-18.52905 18.3652,-20.71788 2.38294,-5.70318 1.89756,-12.57891 -1.54604,-21.90043 -4.30946,-11.66535 -10.50226,-36.04243 -12.87879,-50.695528 -2.69386,-16.60965 -5.67075,-21.606033 -15.41307,-25.869101 -2.13085,-0.932422 -12.50691,-1.251187 -40.5,-1.244207 L 81.5,63.018768 76.295542,66.003602 C 68.534834,70.454484 65.464872,76.201956 65.614644,86 c 0.150768,9.863257 4.577768,35.59695 9.492334,55.17794 9.492405,37.82039 25.640322,76.55124 46.038482,110.42349 3.94098,6.54421 7.77047,13.34662 8.50998,15.11645 5.24117,12.54347 -5.65482,27.24996 -20.2091,27.27654 -1.67951,0.003 -5.40125,-1.16428 -8.27054,-2.59411 z"
        // Создаем кнопки
        val micButton = createSVGIconButton(svgMicContent, "#2d2d2d").apply { isVisible = false }
        var isMicMuted = false
        micButton.setOnAction {
            isMicMuted = !isMicMuted
            micButton.graphic = createSVGIconGraphic(if (isMicMuted) svgMicMuteContent else svgMicContent)
            micButton.style = """
                -fx-background-color: ${if (isMicMuted) "red" else "#2d2d2d"};
                -fx-background-radius: 50;
                -fx-border-radius: 50;
                -fx-padding: 10;
            """.trimIndent()
        }

        val streamButton = createSVGIconButton(svgStreanContent, "#2d2d2d").apply { isVisible = false }
        var isStreaming = false
        streamButton.setOnAction {
            isStreaming = !isStreaming
            streamButton.graphic = createSVGIconGraphic(if (isStreaming) svgStreamStopContent else svgStreanContent)
            streamButton.style = """
                -fx-background-color: ${if (isStreaming) "red" else "#2d2d2d"};
                -fx-background-radius: 50;
                -fx-border-radius: 50;
                -fx-padding: 10;
            """.trimIndent()
        }

        val callButton = createSVGIconButton(svgCallContent, "#00bd00")
        callButton.setOnAction {
            initiateCall()
            isCalling = !isCalling
            callButton.graphic = createSVGIconGraphic(if (isCalling) svgCallStopContent else svgCallContent)
            callButton.style = """
                -fx-background-color: ${if (isCalling) "red" else "#00bd00"};
                -fx-background-radius: 50;
                -fx-border-radius: 50;
                -fx-padding: 10;
            """.trimIndent()

            micButton.isVisible = isCalling
            streamButton.isVisible = isCalling
            // Управляем участниками
            if (isCalling) {
                participants.children.add(createHeaderBox(user))
                applyPulsatingEffect(members)
                showInfoPopup("Вы начали звонок ${friend.friendUsername}", primaryStage)

                // Создаем таймер на 13 секунд
                val callTimeoutTimeline = Timeline(
                    KeyFrame(Duration.seconds(18.0), {
                        // Автоматически сбрасываем звонок
                        isCalling = false
                        callButton.graphic = createSVGIconGraphic(svgCallContent)
                        callButton.style = """
                            -fx-background-color: #00bd00;
                            -fx-background-radius: 50;
                            -fx-border-radius: 50;
                            -fx-padding: 10;
                        """.trimIndent()
                        micButton.isVisible = false
                        streamButton.isVisible = false

                        // Останавливаем эффекты и очищаем участников
                        stopPulsatingEffect(members)
                        participants.children.clear()
                        showErrorPopup("Звонок не был принят.", primaryStage)
                    }
                    ))
                callTimeoutTimeline.play()

                // Сохраняем таймер в свойствах узла (на случай принудительного завершения)
                members.properties["callTimeoutTimeline"] = callTimeoutTimeline
            } else {
                // Принудительное завершение звонка
                stopPulsatingEffect(members)
                participants.children.clear()
                showErrorPopup("Звонок не был принят.", primaryStage)

                // Остановка таймера, если он активен
                val callTimeoutTimeline = members.properties["callTimeoutTimeline"] as? Timeline
                callTimeoutTimeline?.stop()
                members.properties.remove("callTimeoutTimeline")
            }
        }
        // Контейнер для кнопок
        val hbox = HBox(10.0, streamButton, callButton, micButton).apply {
            style = "-fx-padding: 20; -fx-alignment: center;"
        }
        return hbox
    }

    private fun initiateCall() {
        val message = JSONObject().apply {
            put("type", "call")
            put("recipient", friend.friendUsername)
        }
        webSocket.send(message.toString())
        // Implement call initiation UI feedback here
    }

    private var pulseSound: AudioClip? = null

    private fun loadSound() {
        pulseSound = AudioClip(javaClass.getResource("/io/vdartsabvile/avspeak/callSound.mp3")?.toExternalForm())
    }

    private fun applyPulsatingEffect(members: HBox) {
        val avatarImageView = members.lookup(".avatar-image-view") as? ImageView ?: return

        avatarImageView.opacity = 0.6

        // Настраиваем звук перед воспроизведением
        pulseSound?.let {
            it.stop() // Полностью останавливаем предыдущий клип, если он воспроизводится
            it.cycleCount = AudioClip.INDEFINITE // Устанавливаем зацикливание
            it.play() // Начинаем воспроизведение
        }

        // Если анимация уже существует, просто запустим её
        val pulseAnimation = members.properties["pulseAnimation"] as? ScaleTransition
            ?: ScaleTransition(Duration.seconds(1.0), avatarImageView).apply {
                fromX = 1.0
                fromY = 1.0
                toX = 1.1
                toY = 1.1
                cycleCount = Animation.INDEFINITE
                isAutoReverse = true
            }

        pulseAnimation.play()

        // Сохраняем анимацию в свойствах узла, если это новая анимация
        if (members.properties["pulseAnimation"] == null) {
            members.properties["pulseAnimation"] = pulseAnimation
        }
    }

    private fun stopPulsatingEffect(members: HBox) {
        // Получаем анимацию из свойств узла
        val pulseAnimation = members.properties["pulseAnimation"] as? ScaleTransition
        val avatarImageView = members.lookup(".avatar-image-view") as? ImageView ?: return

        // Проверяем, что анимация существует и активна, перед остановкой
        pulseAnimation?.let {
            if (it.status == Animation.Status.RUNNING) {
                it.stop()
                avatarImageView.scaleX = 1.0  // Сброс масштаба по оси X
                avatarImageView.scaleY = 1.0  // Сброс масштаба по оси Y
                avatarImageView.opacity = 1.0
            }
        }

        // Останавливаем звук
        pulseSound?.stop()
    }

    private fun createIconButton(text: String, onClick: () -> Unit) = Button(text).apply {
        style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        setOnAction { onClick() }
    }

    // Создаем правую панель
    private fun createRightPanel(primaryStage: Stage): VBox {
        val rightPanel = VBox(20.0)
        rightPanel.styleClass.add("rightPanel")

        //Инициализация кнопок из rightPanel
        connectButton = Button("Подключиться к серверу")
        createServerButton = Button("Создать сервер")
        settingsButton = Button("Настройки")
        helpButton = Button("Помощь")

        smallAvatarImageView?.fitWidth = 100.0
        smallAvatarImageView?.fitHeight = 100.0
        smallAvatarImageView?.isPreserveRatio = true
        smallAvatarImageView?.let {
            it.fitWidth = 100.0
            it.fitHeight = 100.0
            it.isPreserveRatio = true
            it.clip = Circle(it.fitWidth / 2, it.fitHeight / 2, it.fitWidth / 2)
        }

        val smallBackgroundCircle = Circle(60.0) // Фон немного больше
        smallBackgroundCircle.fill = Color.GRAY

        smallAvatarWrapper = StackPane(smallBackgroundCircle, smallAvatarImageView)
        smallAvatarWrapper.setOnMouseEntered {
            smallAvatarWrapper.style = "-fx-opacity: 0.8;"
        }
        smallAvatarWrapper.setOnMouseExited {
            smallAvatarWrapper.style = "-fx-opacity: 1.0;"
        }

        val savedServersLabel = Label("Сохраненные сервера")
        savedServersLabel.styleClass.add("savedd")
        savedServersLabel.style = "-fx-text-fill: white;"
        // Создаем выпадающий список (ComboBox) для сохраненных серверов
        val savedServers = getSavedServers()
        serverComboBox = ComboBox<String>()
        serverComboBox.promptText = "Выберите сервер"
        // Заполняем ComboBox сохраненными серверами
        if (savedServers.isNotEmpty()) {
            serverComboBox.items.addAll(savedServers)
        }

        connectButton.styleClass.add("Rbutton")
        connectButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                ConnectionWindow().openConnectionForm(primaryStage)
            }
        }

        createServerButton.styleClass.add("Rbutton")
        createServerButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                CreationWindow().openCreationForm(primaryStage)
                println("Создание нового сервера")
            }
        }

        val path = Path()
        path.elements.add(MoveTo(0.0, 0.0))
        path.elements.add(LineTo(300.0, 0.0))
        path.styleClass.add("line") // Применение стиля для линии
        val path2 = Path()
        path2.elements.add(MoveTo(0.0, 0.0))
        path2.elements.add(LineTo(300.0, 0.0))
        path2.styleClass.add("line") // Применение стиля для линии
        val path3 = Path()
        path3.elements.add(MoveTo(0.0, 0.0))
        path3.elements.add(LineTo(300.0, 0.0))
        path3.styleClass.add("line") // Применение стиля для линии

        // Добавляем аватар и имя пользователя в уменьшенном виде на правую панель
        smallAvatarWrapper.alignment = Pos.CENTER
        smallAvatarWrapper.style = "-fx-cursor: hand;"
        smallAvatarWrapper.setOnMouseClicked {
            if (AppState.isAuthenticated.get()) {
                ProfileWindow().openProfileForm(primaryStage) // Открываем профиль, если авторизован
            }
        }

        smallUserNameLabel.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold"

        settingsButton.styleClass.add("Rbutton")
        settingsButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                SettingsWindow().openSettingsForm(primaryStage)
                println("Открытие настроек пользователя")
            }
        }

        helpButton.styleClass.add("Rbutton")
        helpButton.setOnAction {
            HelpWindow().openHelpForm(primaryStage)
            println("Открытие раздела помощи")
        }

        friendsButton.styleClass.add("Rbutton")
        friendsButton.setOnAction {
            FriendsWindow(AppState.username.toString()).openFriendsForm(primaryStage)
            println("Открытие окна друзей")
        }

        // Добавляем элементы на правую панель
        rightPanel.children.addAll(savedServersLabel, serverComboBox, path, connectButton, createServerButton, path2,Region())
        rightPanel.children.addAll(smallAvatarWrapper, smallUserNameLabel,path3, friendsButton, settingsButton)

        return rightPanel

    }

    private fun getSavedServers(): List<String> {
        val servers = mutableListOf<String>()
        try {
            File("saved_servers.txt").forEachLine { line ->
                servers.add(line.trim())
            }
        } catch (e: IOException) {
            println("Ошибка загрузки серверов: ${e.message}")
        }
        return servers
    }

    private fun showErrorPopup(message: String, primaryStage: Stage) {
        // Метод для показа всплывающего окна с ошибкой
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.TRANSPARENT) // Устанавливаем стиль для прозрачного окна
        popupStage.initOwner(primaryStage) // Устанавливаем владельца окна
        popupStage.title = "Ошибка"

        // Делаем окно всегда поверх других
        popupStage.isAlwaysOnTop = true

        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.style = """
                -fx-background-color: rgba(255, 0, 0, 0.8); 
                -fx-padding: 10; 
                -fx-background-radius: 8px; 
                -fx-border-radius: 8px;
            """.trimIndent()

        val scene = Scene(layout)
        popupStage.scene = scene
        scene.fill = null // Убираем цвет фона сцены

        popupStage.x = 180.0
        popupStage.y = 120.0
        popupStage.show()

        // Анимация появления окна (плавное появление)
        val fadeIn = FadeTransition(Duration.millis(500.0), layout)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.play() // Запускаем анимацию

        // Показываем окно
        popupStage.show()

        // Таймер для автоматического закрытия через 5 секунд с анимацией исчезновения
        val timeline = Timeline()
        timeline.keyFrames.add(KeyFrame(Duration.seconds(5.0), {
            // Анимация исчезновения (плавное исчезновение)
            val fadeOut = FadeTransition(Duration.millis(500.0), layout)
            fadeOut.fromValue = 1.0
            fadeOut.toValue = 0.0
            fadeOut.play() // Запускаем анимацию

            fadeOut.setOnFinished {
                popupStage.close() // Закрываем окно после анимации
            }
        }))
        timeline.play()
    }
    private fun showInfoPopup(message: String,  primaryStage: Stage) {
        // Создаем всплывающее окно
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.TRANSPARENT) // Устанавливаем стиль для прозрачного окна
        popupStage.initOwner(primaryStage) // Устанавливаем владельца окна

        // Создаем текст сообщения
        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")

        // Настраиваем компоновку
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.style = """
                -fx-background-color: rgba(127, 199, 255, 0.8); 
                -fx-padding: 10; 
                -fx-background-radius: 8px; 
                -fx-border-radius: 8px;
            """.trimIndent()

        // Создаем сцену с прозрачным фоном
        val scene = Scene(layout)
        scene.fill = null // Убираем цвет фона сцены

        // Устанавливаем сцену в окно
        popupStage.scene = scene

        // Устанавливаем положение окна
        popupStage.x = 180.0
        popupStage.y = 120.0
        popupStage.isAlwaysOnTop = true // Делаем окно всегда поверх других

        popupStage.show()

        // Анимация появления
        val fadeIn = FadeTransition(Duration.millis(500.0), layout)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.play()

        // Таймер для автоматического закрытия через 5 секунд с анимацией исчезновения
        val timeline = Timeline()
        timeline.keyFrames.add(KeyFrame(Duration.seconds(5.0), {
            // Анимация исчезновения (плавное исчезновение)
            val fadeOut = FadeTransition(Duration.millis(500.0), layout)
            fadeOut.fromValue = 1.0
            fadeOut.toValue = 0.0
            fadeOut.play() // Запускаем анимацию

            fadeOut.setOnFinished {
                popupStage.close() // Закрываем окно после анимации
            }
        }))
        timeline.play()
    }
}