package org.sla;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class CommunicationOut implements Runnable {
    private Socket socket;
    private ObjectOutputStream writer;
    private ArrayList<ObjectOutputStream> outStreams;
    private SynchronizedQueue outQueue;
    private TextField statusText;
    private TextField yourNameText;
    private boolean serverMode;

    // CommunicationOut gets data from the Program's outQueue and writes it to 1 or many Sockets

    CommunicationOut(Socket s, ObjectOutputStream out, SynchronizedQueue outQ, TextField status, TextField name) {
        socket = s;
        writer = out;
        outQueue = outQ;
        statusText = status;
        yourNameText = name;
        serverMode = false;
    }

    CommunicationOut(Socket s, ArrayList<ObjectOutputStream> outs, SynchronizedQueue outQ, TextField status, TextField name) {
        socket = s;
        outStreams = outs;
        outQueue = outQ;
        statusText = status;
        yourNameText = name;
        serverMode = true;
    }

    public void run() {
        Thread.currentThread().setName("CommunicationOut Thread");
        System.out.println("CommunicationOut thread running");

        try {
            while (ClientServerPictureViewerController.connected && !Thread.interrupted()) {
                // keep getting from output Queue until it has data
                String sender = (String) outQueue.get();
                while (sender == null) {
                    Thread.currentThread().yield();
                    sender = (String) outQueue.get();
                }
                String finalSender = sender;
                System.out.println("CommunicationOut GOT: \"" + sender + "\"");

                Image message = (Image)outQueue.get();
                while (message == null) {
                    Thread.currentThread().yield();
                    message = (Image)outQueue.get();
                }
                Image finalMessage = message;
                System.out.println("CommunicationOut GOT: \"" + message + "\"");

                // write both data to 1 or many sockets
                if (serverMode && MainServer.multicastMode) {
                    int clientCount = 0;
                    Iterator<ObjectOutputStream> allClients = outStreams.iterator();
                    while (allClients.hasNext()) {
                        ObjectOutputStream nextWriter = allClients.next();
                        // writer writes to 1 socket's output stream
                        Integer dataCount = 2;
                        nextWriter.writeObject(dataCount);
                        nextWriter.flush();
                        nextWriter.writeObject(sender);
                        nextWriter.flush();
                        //nextWriter.writeObject(message);
                        //nextWriter.flush();
                        ImageIO.write(SwingFXUtils.fromFXImage(message, null), "png", socket.getOutputStream());
                        System.out.println("CommunicationOut to Client " + clientCount + ": \"" + message + "\" from " + sender);
                        clientCount = clientCount + 1;
                    }
                } else {
                    // writer writes to 1 socket's output stream
                    Integer dataCount = 2;
                    writer.writeObject(dataCount);
                    writer.flush();
                    writer.writeObject(sender);
                    writer.flush();
                    //writer.writeObject(message);
                    ImageIO.write(SwingFXUtils.fromFXImage(message, null), "png", socket.getOutputStream());
                    //writer.flush();
                }

                Platform.runLater(() -> statusText.setText("SENT: \"" + finalMessage + "\" from " + finalSender));
                System.out.println("CommunicationOut SENT: \"" + message + "\" from " + sender);

            }

            // while loop ended!
            writer.close();
            socket.close();
            System.out.println("CommunicationOut thread DONE; reader and socket closed.");

        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusText.setText("CommunicationOut: networking failed. Exiting...."));
        }
    }
}
