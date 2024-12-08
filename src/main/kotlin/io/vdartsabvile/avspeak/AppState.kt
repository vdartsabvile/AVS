package io.vdartsabvile.avspeak

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.sound.sampled.Mixer

class AppState {
    companion object {
        val friendsList = FXCollections.observableArrayList<Friend>()


        private const val SETTINGS_FILE = "app_settings.dat"

        val selectedMicrophone = SimpleObjectProperty<Mixer.Info>()
        val selectedSpeaker = SimpleObjectProperty<Mixer.Info>()


        var isAuthenticated = SimpleBooleanProperty(false)  // Изменили тип на SimpleBooleanProperty
        var username: String? = null

        var smallAvatarImageView: ImageView? = ImageView(Image(javaClass.getResource("/io/vdartsabvile/avspeak/null.png")?.toExternalForm()))
        var avatarImageView: ImageView? = ImageView(Image(javaClass.getResource("/io/vdartsabvile/avspeak/null.png")?.toExternalForm()))

        // Create a property for the avatar
        val avatarProperty: ObjectProperty<Image> = SimpleObjectProperty()

        // Method to update the avatar
        fun updateAvatar(newAvatar: Image?) {
            if (newAvatar != null) {
                smallAvatarImageView?.image = newAvatar
                avatarImageView?.image = newAvatar

            } else {
                smallAvatarImageView?.image = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
                avatarImageView?.image = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
            }
        }
        // Method to add a listener for avatar changes
        fun onAvatarChanged(listener: (Image) -> Unit) {
            avatarProperty.addListener { _, _, newAvatar -> listener(newAvatar) }
        }

        // Method to bind the avatar to the account
        fun bindAvatarToAccount() {
            onAvatarChanged { newAvatar ->
                if (isAuthenticated.get()) {
                    avatarProperty.set(newAvatar) // Set the avatar if user is authenticated
                } else {
                    avatarProperty.set(Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())) // Set empty avatar if user is not authenticated
                }
            }
        }
        // Метод для сохранения настроек
        fun saveSettings() {
            try {
                ObjectOutputStream(File(SETTINGS_FILE).outputStream()).use { out ->
                    // Сохраните строковые представления выбранных устройств вместо самих объектов
                    out.writeObject(selectedMicrophone.value?.toString())
                    out.writeObject(selectedSpeaker.value?.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadSettings(audioDevicesMic: List<Mixer.Info>, audioDevicesDyn: List<Mixer.Info>) {
            try {
                ObjectInputStream(File(SETTINGS_FILE).inputStream()).use { `in` ->
                    val savedMicrophoneName = `in`.readObject() as? String
                    val savedSpeakerName = `in`.readObject() as? String

                    // Поиск и установка устройств на основе сохранённых строковых значений
                    val microphone = audioDevicesMic.find { it.toString() == savedMicrophoneName }
                    val speaker = audioDevicesDyn.find { it.toString() == savedSpeakerName }

                    if (microphone != null) selectedMicrophone.set(microphone)
                    if (speaker != null) selectedSpeaker.set(speaker)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}