package org.sla;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GUIUpdater implements Runnable {
    private SynchronizedQueue inputQueue;
    private ImageView receivedImage;
    private TextField yourNameText;

    // GUIUpdater tries to get data from the Programs inputQueue and updates the GUI when data arrives

    GUIUpdater(SynchronizedQueue q, ImageView image, TextField name) {
        inputQueue = q;
        receivedImage = image;
        yourNameText = name;
    }

    public void run() {
        Thread.currentThread().setName("GUIUpdater Thread");

        while (!Thread.interrupted()) {
            // Try to get a Message from the inputQueue
            Message message = (Message)inputQueue.get();
            while (message == null) {
                Thread.currentThread().yield();
                message = (Message)inputQueue.get();
            }
            Message finalMessage = message; // needed for Platform.runLater()


            if (!message.sender().equals(yourNameText.getText())) {
                // Got a message... update the GUI with its image
                Platform.runLater(() -> receivedImage.setImage(finalMessage.data()));
            }
        }

    }
}
