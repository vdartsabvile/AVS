package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class RegistrationWindow {
    private val userManager = UserManager()
    private var confirmationCode: String = ""
    private val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен Яндекс.Диска


    fun openRegistrationForm(owner: Stage) {
        val registrationStage = Stage()
        registrationStage.initOwner(owner)
        registrationStage.initModality(Modality.APPLICATION_MODAL)
        registrationStage.title = "Регистрация"

        registrationStage.initStyle(StageStyle.TRANSPARENT) // Прозрачный стиль для окна



        val usernameField = TextField()
        usernameField.promptText = "Имя пользователя"

        val emailField = TextField()
        emailField.promptText = "Электронная почта"

        val passwordField = PasswordField()
        passwordField.promptText = "Пароль"

        val registerButton = Button("Зарегистрироваться")
        registerButton.setOnAction {
            val username = usernameField.text.trim()
            val email = emailField.text.trim()
            val password = passwordField.text.trim()
            val ipAddress = getPublicIpAddress()


            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showErrorPopup("Имя пользователя, электронная почта и пароль не могут быть пустыми.")
                return@setOnAction
            }

            // Проверка формата email
            if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
                showErrorPopup("Пожалуйста, введите корректный адрес электронной почты.")
                return@setOnAction
            }


            showPopup("Открытие окна подтверждения может занять некоторое время..")
                confirmationCode = generateConfirmationCode()
                sendConfirmationEmail(email, confirmationCode)
                println("Код подтверждения отправлен на $email.")

                // Открываем окно для ввода кода подтверждения
                openConfirmationCodeWindow(registrationStage, email, username, password)

        }

        val cancelButton = Button("Отмена")
        cancelButton.setOnAction {
            registrationStage.close()
        }

        val layout = VBox(10.0, Label("Пожалуйста, введите свои данные для регистрации:"), usernameField, emailField, passwordField, registerButton, cancelButton)
        layout.alignment = Pos.CENTER
        layout.setStyle("-fx-padding: 20;")

        val scene = Scene(layout, 450.0, 350.0)
        registrationStage.scene = scene
        registrationStage.show()

        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
    }
    private fun getPublicIpAddress(): String? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.ipify.org")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()
                } else {
                    println("Не удалось получить IP-адрес: ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Ошибка при получении IP-адреса: ${e.message}")
            null
        }
    }


    private fun sendConfirmationEmail(recipientEmail: String, confirmationCode: String) {
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = "smtp.gmail.com" // Замените на ваш SMTP-сервер
        properties["mail.smtp.port"] = "587"

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("avspeakoff@gmail.com", "tskk vshh xemx kwou")
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress("avspeakoff@gmail.com"))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            message.subject = "Подтверждение регистрации в AVSpeak"

            // Установка кодировки в UTF-8
            message.setHeader("Content-Type", "text/html; charset=UTF-8")

            val emailContent = """
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Подтверждение регистрации</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #f7f7f7;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        background-color: #ffffff;
                        border-radius: 8px;
                        padding: 20px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                    }
                    h2 {
                        color: #333333;
                    }
                    .confirmation-code {
                        font-size: 24px;
                        font-weight: bold;
                        color: #4CAF50; /* Зеленый цвет для кода подтверждения */
                        padding: 10px;
                        border: 2px solid #4CAF50;
                        border-radius: 4px;
                        display: inline-block;
                    }
                    p {
                        color: #555555;
                    }
                    .footer {
                        margin-top: 20px;
                        font-size: 12px;
                        color: #888888;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Здравствуйте!</h2>
                    <p>Спасибо за регистрацию в нашем приложении <strong>AVSpeak</strong>!</p>
                    <p>Чтобы завершить процесс регистрации, пожалуйста, введите следующий код подтверждения в приложении:</p>
                    <div class="confirmation-code">$confirmationCode</div>
                    <p>Если у вас возникли трудности с регистрацией, не стесняйтесь обращаться в службу поддержки: avspeakoff@gmail.com.</p>
                    <p class="footer">С уважением,<br>Команда AVSpeak</p>
                </div>
            </body>
            </html>
        """.trimIndent()

            message.setContent(emailContent, "text/html; charset=UTF-8")

            Transport.send(message)

            println("Сообщение отправлено")
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorPopup("Ошибка при отправке сообщения: ${e.message}")
        }
    }


    private fun generateConfirmationCode(): String {
        // Генерируем случайный 6-значный код
        return (100000..999999).random().toString()
    }

    private fun openConfirmationCodeWindow(owner: Stage, email: String, username: String, password: String) {
        val confirmationStage = Stage()
        confirmationStage.initOwner(owner)
        confirmationStage.initModality(Modality.APPLICATION_MODAL)
        confirmationStage.title = "Подтверждение регистрации"

        confirmationStage.initStyle(StageStyle.TRANSPARENT)


        val codeField = TextField()
        codeField.promptText = "Введите код подтверждения"

        val confirmButton = Button("Подтвердить")
        confirmButton.setOnAction {
            val enteredCode = codeField.text.trim()
            if (enteredCode == confirmationCode) {
                println("Регистрация завершена для $username.")
                // Получаем IP-адрес и сохраняем данные в Яндекс.Диск после подтверждения
                val ipAddress = getPublicIpAddress()

                val userInfo = mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password, // Лучше хешировать пароль
                    "ipAddress" to (ipAddress ?: "Неизвестный IP")
                )
                if (ipAddress != null) {
                    userManager.registerUser(username, password, ipAddress, token)
                }
                confirmationStage.close()
                owner.close() // Закрываем основное окно регистрации
            } else {
                showErrorPopup("Неверный код подтверждения. Попробуйте еще раз.")
            }
        }

        val cancelButton = Button("Отмена")
        cancelButton.setOnAction {
            confirmationStage.close()
        }

        val layout = VBox(10.0, Label("Пожалуйста, введите код подтверждения, отправленный на $email:"), codeField, confirmButton, cancelButton)
        layout.alignment = Pos.CENTER
        layout.setStyle("-fx-padding: 20;")

        val scene = Scene(layout, 450.0, 350.0)
        confirmationStage.scene = scene
        confirmationStage.show()

        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
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
    private fun showPopup(message: String) {
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
        layout.setStyle("-fx-background-color: rgba(127, 199, 255, 0.8); -fx-padding: 10;")

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
