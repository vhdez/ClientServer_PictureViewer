package org.sla;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class CommunicationIn implements Runnable {
    private Socket socket;
    private ObjectInputStream dataReader;
    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;
    private TextField statusText;
    private TextField yourNameText;
    private boolean serverMode;

    // CommunicationIn reads from a Socket and puts data into the Program's inQueue

    CommunicationIn(Socket s, ObjectInputStream in, SynchronizedQueue inQ, SynchronizedQueue outQ, TextField status, TextField name) {
        socket = s;
        dataReader = in;
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
                try {
                    // Find out how much data was sent
                    int dataCount = (Integer) dataReader.readObject();
                    System.out.println("CommunicationIn: RECEIVING " + dataCount + " data");
                    // Receiving incoming data!!!

                    // Read 2 data from the input side of the socket.
                    String sender = (String)dataReader.readObject();
                    Image message;
                    message = SwingFXUtils.toFXImage(ImageIO.read(socket.getInputStream()), null);

                    //Object message = dataReader.readObject();
                    System.out.println("CommunicationIn RECEIVED: \"" + message + "\" from " + sender);
                    Platform.runLater(() -> statusText.setText("RECEIVED: \"" + message + "\" FROM " + sender));

                    // ignore any messages sent by yourself
                    if (!sender.equals(yourNameText.getText())) {
                        // Now put both incoming data on the InputQueue so that the GUI will see it
                        boolean putSucceeded = inQueue.put(sender);
                        while (!putSucceeded) {
                            Thread.currentThread().yield();
                            putSucceeded = inQueue.put(sender);
                        }
                        System.out.println("CommunicationIn PUT into InputQueue: \"" + sender + "\"");
                        Platform.runLater(() -> statusText.setText("PUT into InputQueue: \"" + sender + "\""));

                        putSucceeded = inQueue.put(message);
                        while (!putSucceeded) {
                            Thread.currentThread().yield();
                            putSucceeded = inQueue.put(message);
                        }
                        System.out.println("CommunicationIn PUT into InputQueue: \"" + message + "\"");
                        Platform.runLater(() -> statusText.setText("PUT into InputQueue: \"" + message + "\""));

                        // IF SERVER and MULTICAST: also put that incoming data on the OutputQueue so ALL clients see it
                        if (serverMode && MainServer.multicastMode) {
                            putSucceeded = outQueue.put(sender);
                            while (!putSucceeded) {
                                Thread.currentThread().yield();
                                putSucceeded = outQueue.put(sender);
                            }
                            System.out.println("CommunicationIn MULTICAST into OutputQueue: \"" + sender + "\"");
                            Platform.runLater(() -> statusText.setText("MULTICAST into OutputQueue: \"" + sender + "\""));

                            putSucceeded = outQueue.put(message);
                            while (!putSucceeded) {
                                Thread.currentThread().yield();
                                putSucceeded = outQueue.put(message);
                            }
                            System.out.println("CommunicationIn MULTICAST into OutputQueue: \"" + message + "\"");
                            Platform.runLater(() -> statusText.setText("MULTICAST into OutputQueue: \"" + message + "\""));

                        }
                    }
                } catch (EOFException ex) {
                    // EOFException happens when there is no data to read in dataReader
                    // Just yield until there is some more data
                    Thread.currentThread().yield();
                }
            }

            // while loop ended!  close reader and socket
            dataReader.close();
            socket.close();
            System.out.println("CommunicationIn thread DONE; reader and socket closed.");

        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusText.setText("CommunicationIn: networking failed. Exiting...."));
        }

    }
}
