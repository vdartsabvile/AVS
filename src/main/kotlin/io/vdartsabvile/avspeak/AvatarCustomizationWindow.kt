package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import javax.imageio.ImageIO

class AvatarCustomizationWindow {
    private val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен Яндекс.Диска
    var text = Text()
    var whiteOrBlack = CheckBox()



    fun openAvatarCustomizationForm(owner: Stage) {
            val AvatarStage = Stage()
            AvatarStage.initOwner(owner)
            AvatarStage.initModality(Modality.APPLICATION_MODAL)
            AvatarStage.title = "Avatar Customization"


            // Выбор цвета фона
            val colorPicker = ColorPicker(Color.GRAY)

            val avatarComboBox = ComboBox<String>()
            avatarComboBox.items.addAll(
                "😓", "😢", "😭", "😤", "😡", "😠", "🤬", "😈", "👿", "💀",
                "😀", "😎", "😊", "🚀", "🐱", "🐶", "🐵", "🦄", "🌟", "❤",
                "💀", "👻", "👑", "💩", "🔥", "🌈", "🎉", "🌹", "🌸", "🍀",
                "💫", "🎶", "🦋", "🐝", "🐼", "🦁", "🐯", "🐷", "🐸", "🦄",
                "🎯", "🍕", "🍔", "🍟", "🍩", "🍦", "🍎", "🍉", "🍓", "🍒",
                "💖", "💙", "💚", "💛", "💜", "🤖", "👽", "🤠", "💋", "🌼",
                "🌻", "🍄", "🦄", "🥑", "🥒", "🌽", "🍓", "🍍", "🍒", "🍇",
                "🍊", "🍋", "🍈", "🍌", "🍐", "🥝", "🍍", "🐉", "🦋", "🦀",
                "🦜", "🐧", "🐦", "🦈", "🦑", "🎩", "👒", "👑", "👟", "👗",
                "👚", "👖", "🛒", "🛀", "🎒", "⛺", "🏕", "🛏", "🏖", "🏠", "🏡"
            )
            avatarComboBox.promptText = "Выберите аватар"

            // ImageView для предпросмотра
            val previewImageView = ImageView()
            previewImageView.fitWidth = 100.0
            previewImageView.fitHeight = 100.0
            previewImageView.clip = Circle(50.0, 50.0, 50.0)

            // Обновляем предпросмотр каждый раз при выборе цвета или буквы
            val updatePreview = {
                val selectedColor = colorPicker.value ?: Color.BLACK
                val selectedLetter = avatarComboBox.value ?: "💩"
                previewImageView.image = createAvatarImage(selectedColor, selectedLetter)
            }

            // Устанавливаем обработчики для обновления предпросмотра
            colorPicker.setOnAction { updatePreview() }
            avatarComboBox.setOnAction { updatePreview() }




        // Кнопка для сохранения аватара
            val saveButton = Button("Сохранить")
            saveButton.style = "-fx-font-size: 18px;"
            saveButton.setOnAction {
                val username = AppState.username // Сохранение имени пользователя

                AppState.avatarImageView?.image = previewImageView.image
                AppState.smallAvatarImageView?.image = previewImageView.image
                AppState.updateAvatar(previewImageView.image)  // Обновляем в AppState
                val avatarWritableImage = previewImageView.image as? WritableImage ?: createAvatarImage(colorPicker.value, avatarComboBox.value ?: "💩")

                if (username != null) {
                    uploadAvatarToYandexDisk(token, avatarWritableImage, username)
                }
                AvatarStage.close()
            }
            // Создаем круг для фона (немного больше аватара)
            val backgroundCircle = Circle(60.0) // Радиус чуть больше радиуса аватара
            backgroundCircle.fill = Color.GRAY // Задаем цвет фона

            val avatarWrapper = StackPane(backgroundCircle, previewImageView)
            avatarWrapper.alignment = Pos.CENTER

        // Внутри метода openAvatarCustomizationForm
        whiteOrBlack = CheckBox("Черный контур")

        // Измените цвет текста и обновите изображение в зависимости от состояния чекбокса
        whiteOrBlack.setOnAction {
            text.fill = if (whiteOrBlack.isSelected) Color.BLACK else Color.WHITE
            updatePreview()
        }


        // Layout для диалога
            val layout = VBox(20.0, Label("Предпросмотр аватара:"), avatarWrapper, Label("Выберите цвет фона и изображение:"), colorPicker, whiteOrBlack,avatarComboBox, saveButton)
            layout.alignment = Pos.CENTER
            layout.padding = Insets(20.0)

            val scene = Scene(layout, 550.0, 500.0)
            AvatarStage.scene = scene
            AvatarStage.show()

            scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        }
        private fun createAvatarImage(color: Color, letter: String): WritableImage {
            // Создаем круглый фон с текстом внутри
            val circle = Circle(50.0)
            circle.fill = color

            text = Text(letter)
            text.font = Font.font(70.0)
            text.fill = if (whiteOrBlack.isSelected) Color.BLACK else Color.WHITE

            // StackPane для объединения фона и текста
            val avatarStack = StackPane(circle, text)
            avatarStack.alignment = Pos.CENTER

            // Создаем снимок StackPane
            val snapshotParams = SnapshotParameters()
            snapshotParams.fill = Color.TRANSPARENT // Прозрачный фон

            return avatarStack.snapshot(snapshotParams, null)
        }
    fun uploadAvatarToYandexDisk(token: String, avatarImage: WritableImage, username: String) {
        val client = OkHttpClient()

        // Сохраняем изображение как временный файл .png
        val tempFile = File.createTempFile("avatar", ".png")
        ImageIO.write(SwingFXUtils.fromFXImage(avatarImage, null), "png", tempFile)

        // Указываем путь на Яндекс.Диске, например, "AVSpeak/avatar.png"
        val uploadPath = "AVSpeak/Avatars/avatar_$username.png"
        val request = Request.Builder()
            .url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=$uploadPath&overwrite=true")
            .addHeader("Authorization", "OAuth $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                // Получаем URL для загрузки
                val uploadUrl = JSONObject(response.body?.string()).getString("href")

                // Загружаем файл по указанному URL
                val fileRequest = Request.Builder()
                    .url(uploadUrl)
                    .put(tempFile.asRequestBody("image/png".toMediaTypeOrNull()))
                    .build()

                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        println("Аватар успешно загружен на Яндекс.Диск под именем $uploadPath")
                    } else {
                        println("Ошибка загрузки аватара: ${fileResponse.message}")
                    }
                }
            } else {
                println("Ошибка при запросе URL для загрузки: ${response.message}")
            }
        }

        // Удаляем временный файл после загрузки
        tempFile.delete()
    }
}

