package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.util.prefs.Preferences

class LoginWindow {
    private val userManager = UserManager()
    private val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен Яндекс.Диска
    private val preferences = Preferences.userNodeForPackage(LoginWindow::class.java)



    fun openLoginForm(owner: Stage, onLoginSuccess: (Boolean) -> Unit) {

        val loginStage = Stage()
        loginStage.initOwner(owner)
        loginStage.initModality(Modality.APPLICATION_MODAL)
        loginStage.title = "Вход"

        loginStage.initStyle(StageStyle.TRANSPARENT) // Прозрачный стиль для окна


        val usernameField = TextField()
        usernameField.promptText = "Имя пользователя"

        val passwordField = PasswordField()
        passwordField.promptText = "Пароль"

        val rememberMeCheckBox = CheckBox("Не выходить из аккаунта")



        val loginButton = Button("Войти")
        loginButton.setOnAction {
            val username = usernameField.text.trim()
            val password = passwordField.text.trim()


            // Используем token для аутентификации через UserManager
            val (isAuthenticated, avatarImage) = userManager.loginUser(username, password, token)


            if (isAuthenticated) {
                println("Вход выполнен: $username")

                AppState.username = username // Сохранение имени пользователя
                AppState.isAuthenticated.set(true)  // Устанавливаем значение через метод set
                // Скачивание инфы о друзьях
                val currentUsername = username
                FriendsWindow(currentUsername).loadFriendsInfo()
                StartUp().startNodeClient()



                // Если аватар успешно загружен, обновляем его в AppState
                if (avatarImage != null) {
                    AppState.updateAvatar(avatarImage)
                    AppState.avatarImageView?.image = avatarImage // Обновление главного ImageView
                    AppState.smallAvatarImageView?.image = avatarImage // Обновление маленького ImageView
                } else {
                    println("Аватар не найден.")
                }

                // Сохранение данных, если установлен флажок "Не выходить из аккаунта"
                if (rememberMeCheckBox.isSelected) {
                    preferences.put("username", username)
                    preferences.put("password", password)
                    println("Функция не выхрдить из аккаунта включена")
                } else {
                    preferences.remove("username")
                    preferences.remove("password")
                    //DisconnectServer().notifyServerLogout()
                }

                loginStage.close()
                onLoginSuccess(true)
            } else {
                showErrorPopup("Неверные имя пользователя или пароль.")
                onLoginSuccess(false)
            }
        }


        val cancelButton = Button("Отмена")
        cancelButton.setOnAction {
            loginStage.close()
            onLoginSuccess(false) // или обработать как успех при закрытии, если это необходимо
        }

        val layout = VBox(
            10.0,
            Label("Пожалуйста, введите свои данные для входа:"),
            usernameField,
            passwordField,
            rememberMeCheckBox,
            loginButton,
            cancelButton
        )
        layout.alignment = Pos.CENTER
        layout.setStyle("-fx-padding: 20;")

        val scene = Scene(layout, 450.0, 350.0)
        loginStage.scene = scene
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)

        loginStage.show()
    }



    private fun showErrorPopup(message: String) {
        // Метод для показа всплывающего окна с ошибкой
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.UNDECORATED)
        popupStage.title = "Ошибка"

        // Делаем окно всегда поверх других
        popupStage.isAlwaysOnTop = true

        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.setStyle("-fx-background-color: rgba(255, 0, 0, 0.8); -fx-padding: 10;")

        val scene = Scene(layout)
        popupStage.scene = scene
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


}