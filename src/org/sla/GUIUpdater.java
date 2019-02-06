package org.sla;

import javafx.application.Platform;
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
            // Try to get 2 strings from the inputQueue
            String sender = (String)inputQueue.get();
            while (sender == null) {
                Thread.currentThread().yield();
                sender = (String)inputQueue.get();
            }
            String finalSender = sender;

            Image message = (Image)inputQueue.get();
            while (message == null) {
                Thread.currentThread().yield();
                message = (Image)inputQueue.get();
            }
            Image finalMessage = message;

            if (!sender.equals(yourNameText.getText())) {
                // Got a string... update the GUI with it
                Platform.runLater(() -> receivedImage.setImage(finalMessage));
            }
        }
    }
}
