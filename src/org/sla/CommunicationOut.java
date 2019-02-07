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
    private boolean serverMode;

    // CommunicationOut gets data from the Program's outQueue and writes it to 1 or many Sockets

    CommunicationOut(Socket s, ObjectOutputStream out, SynchronizedQueue outQ, TextField status) {
        socket = s;
        writer = out;
        outQueue = outQ;
        statusText = status;
        serverMode = false;
    }

    CommunicationOut(Socket s, ArrayList<ObjectOutputStream> outs, SynchronizedQueue outQ, TextField status) {
        socket = s;
        outStreams = outs;
        outQueue = outQ;
        statusText = status;
        serverMode = true;
    }

    public void run() {
        Thread.currentThread().setName("CommunicationOut Thread");
        System.out.println("CommunicationOut thread running");

        try {
            while (ClientServerPictureViewerController.connected && !Thread.interrupted()) {
                // keep getting from output Queue until it has a message
                Message message = (Message) outQueue.get();
                while (message == null) {
                    Thread.currentThread().yield();
                    message = (Message) outQueue.get();
                }
                Message finalMessage = message;
                System.out.println("CommunicationOut GOT: " + message);

                // write message to 1 or many sockets
                if (serverMode && MainServer.multicastMode) {
                    int clientCount = 0;
                    Iterator<ObjectOutputStream> allClients = outStreams.iterator();
                    while (allClients.hasNext()) {
                        ObjectOutputStream nextWriter = allClients.next();
                        // writer writes to 1 socket's output stream
                        nextWriter.writeObject(message);
                        nextWriter.flush();
                        System.out.println("CommunicationOut to Client " + clientCount + ": " + message);
                        clientCount = clientCount + 1;
                    }
                } else {
                    // writer writes to 1 socket's output stream
                    writer.writeObject(message);
                    writer.flush();
                }

                Platform.runLater(() -> statusText.setText("SENT: " + finalMessage));
                System.out.println("CommunicationOut SENT: " + message);

            }

            // while loop ended!
            socket.close();
            System.out.println("CommunicationOut thread DONE; reader and socket closed.");

        } catch (Exception ex) {
            if (ClientServerPictureViewerController.connected) {
                ex.printStackTrace();
                Platform.runLater(() -> statusText.setText("CommunicationOut: networking failed. Exiting...."));
            }
        }

        try {
            // CommunicationOut ending!
            socket.close();
            System.out.println("CommunicationOut thread DONE; reader and socket closed.");
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusText.setText("CommunicationOut: reader and socket closing failed...."));
        }
    }
}
