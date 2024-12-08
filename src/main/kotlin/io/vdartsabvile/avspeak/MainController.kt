package io.vdartsabvile.avspeak

import javafx.animation.FadeTransition
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.io.File
import java.io.IOException

class MainController {

    @FXML
    private lateinit var closeButton: Button

    @FXML
    private lateinit var minimizeButton: Button

    @FXML
    private lateinit var loginButton: Button

    @FXML
    private lateinit var registerButton: Button

    @FXML
    private lateinit var connectButton: Button

    @FXML
    private lateinit var createServerButton: Button

    @FXML
    private lateinit var settingsButton: Button

    @FXML
    private lateinit var serverComboBox: ComboBox<String>

    @FXML
    private lateinit var smallAvatarWrapper: StackPane

    @FXML
    private lateinit var smallAvatarImageView: ImageView

    @FXML
    private lateinit var smallBackgroundCircle: Circle

    @FXML
    private lateinit var smallUserNameLabel: Label

    private var isAuthenticated = false
    private var xOffset = 0.0
    private var yOffset = 0.0

    @FXML
    fun initialize() {
        // Обработка кнопок закрытия и сворачивания
        closeButton.setOnAction {
            (closeButton.scene.window as Stage).close()
        }

        minimizeButton.setOnAction {
            (minimizeButton.scene.window as Stage).isIconified = true
        }

        // Перемещение окна
        setupWindowDragging()

        // Обработка кнопок входа и регистрации
        loginButton.setOnAction {
            openLoginForm() // Вызов метода для авторизации
        }

        registerButton.setOnAction {
            openRegistrationForm() // Вызов метода для регистрации
        }

        // Инициализация остальных кнопок
        connectButton.setOnAction {
            openConnectionForm() // Вызов формы подключения к серверу
        }

        createServerButton.setOnAction {
            openCreationForm() // Вызов формы создания сервера
        }

        settingsButton.setOnAction {
            openSettingsForm() // Вызов формы настроек
        }

        // Инициализация выпадающего списка серверов
        initializeServers()

        // Обновление интерфейса на основе статуса авторизации
        applyAuthenticationRestrictions()
        updateAvatarAppearance()
    }

    private fun setupWindowDragging() {
        val scene = closeButton.scene
        scene.root.setOnMousePressed { event: MouseEvent ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }

        scene.root.setOnMouseDragged { event: MouseEvent ->
            val stage = scene.window as Stage
            stage.x = event.screenX - xOffset
            stage.y = event.screenY - yOffset
        }
    }

    private fun openLoginForm() {
        // Логика открытия формы авторизации
        // После успешной авторизации:
        isAuthenticated = true
        applyAuthenticationRestrictions() // Включаем элементы интерфейса
        updateAvatarAppearance() // Обновляем аватар
    }

    private fun openRegistrationForm() {
        // Логика открытия формы регистрации
    }

    private fun openConnectionForm() {
        // Логика открытия формы подключения к серверу
    }

    private fun openCreationForm() {
        // Логика открытия формы создания сервера
    }

    private fun openSettingsForm() {
        // Логика открытия формы настроек
    }

    private fun initializeServers() {
        val servers = getSavedServers()
        if (servers.isNotEmpty()) {
            serverComboBox.items.addAll(servers)
        }
        applyAuthenticationRestrictionsToComboBox()
    }

    private fun applyAuthenticationRestrictions() {
        val buttons = arrayOf(connectButton, createServerButton, settingsButton)
        for (button in buttons) {
            button.isDisable = !isAuthenticated
            button.style = if (!isAuthenticated) "-fx-opacity: 0.5;" else "-fx-opacity: 1.0;"
        }
    }

    private fun updateAvatarAppearance() {
        smallAvatarWrapper.opacity = if (!isAuthenticated) 0.5 else 1.0
        smallAvatarWrapper.isMouseTransparent = !isAuthenticated
    }

    private fun applyAuthenticationRestrictionsToComboBox() {
        serverComboBox.isDisable = !isAuthenticated
        serverComboBox.opacity = if (!isAuthenticated) 0.5 else 1.0
    }

    private fun getSavedServers(): List<String> {
        val servers = mutableListOf<String>()
        try {
            File("saved_servers.txt").forEachLine { line ->
                servers.add(line.trim())
            }
        } catch (e: IOException) {
            showErrorPopup("Ошибка загрузки серверов: ${e.message}")
        }
        return servers
    }

    private fun showErrorPopup(message: String) {
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.UNDECORATED)
        val label = Label(message)
        val layout = VBox(10.0, label)
        layout.setStyle("-fx-background-color: rgba(255, 0, 0, 0.8); -fx-padding: 10;")
        popupStage.scene = Scene(layout)
        popupStage.show()

        val fadeOut = FadeTransition(Duration.millis(500.0), layout)
        fadeOut.play()
        fadeOut.setOnFinished { popupStage.close() }
    }
}
