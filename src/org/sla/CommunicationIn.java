package org.sla;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class CommunicationIn implements Runnable {
    private Socket socket;
    private ObjectInputStream messageReader;
    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;
    private TextField statusText;
    private TextField yourNameText;
    private boolean serverMode;

    // CommunicationIn reads from a Socket and puts data into the Program's inQueue

    CommunicationIn(Socket s, ObjectInputStream in, SynchronizedQueue inQ, SynchronizedQueue outQ, TextField status, TextField name) {
        socket = s;
        messageReader = in;
        // CommunicationIn puts data read from the socket into the inQueue
        inQueue = inQ;
        // Only the server needs the outQueue from CommunicationIn
        outQueue = outQ;
        statusText = status;
        yourNameText = name;
        serverMode = (outQ != null);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("CommunicationIn Thread");
        System.out.println("CommunicationIn thread running");

        try {

            // Read all incoming communication
            // dataReader reads objects from 1 socket
            while (ClientServerPictureViewerController.connected && !Thread.interrupted()) {
                Message message = null;
                while (message == null) {
                    try {
                        message = (Message) messageReader.readObject();
                    } catch (EOFException ex) {
                        // EOFException means data has NOT been written yet; so yield and try reading again
                        Thread.currentThread().yield();
                    }
                }
                Message finalMessage = message;
                System.out.println("CommunicationIn: RECEIVING " + message);
                // Receiving incoming message!!!

                Platform.runLater(() -> statusText.setText("RECEIVED: " + finalMessage));

                // ignore any messages sent by yourself: only put messages from others into your inQueue
                if (!message.sender().equals(yourNameText.getText())) {
                    // Now put message on the InputQueue so that the GUI will see it
                    boolean putSucceeded = inQueue.put(message);
                    while (!putSucceeded) {
                        Thread.currentThread().yield();
                        putSucceeded = inQueue.put(message);
                    }
                    System.out.println("CommunicationIn PUT into InputQueue: " + message);
                    Platform.runLater(() -> statusText.setText("PUT into InputQueue: " + finalMessage));

                    // IF SERVER and MULTICAST: also put that incoming message on the OutputQueue so ALL clients see it
                    if (serverMode && MainServer.multicastMode) {
                        putSucceeded = outQueue.put(message);
                        while (!putSucceeded) {
                            Thread.currentThread().yield();
                            putSucceeded = outQueue.put(message);
                        }
                        System.out.println("CommunicationIn MULTICAST into OutputQueue: " + message);
                        Platform.runLater(() -> statusText.setText("MULTICAST into OutputQueue: " + finalMessage));

                    }
                }
            }

            // while loop ended!  close reader and socket
            socket.close();
            System.out.println("CommunicationIn thread DONE; reader and socket closed.");

        } catch (SocketException se) {
            // nothing to do
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusText.setText("CommunicationIn: networking failed. Exiting...."));
        }

        try {
            // CommunicationIn ending!
            socket.close();
            System.out.println("CommunicationIn thread DONE; reader and socket closed.");
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusText.setText("CommunicationIn: reader and socket closing failed...."));
        }

    }
}
