package org.sla;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientServerPictureViewerController {
    public TextField IPAddressText;
    public TextField portText;
    public Button    startButton;
    public TextField statusText;
    public TextField yourNameText;
    public ImageView sendImage;
    public Button    openButton;
    public Button    sendButton;
    public ImageView receivedImage;

    // Each Program has only 1 inQueue for incoming data and 1 outQueue for outgoing data
    //     There can be many different Streams where incoming data is read from
    //     There can be many different Streams where outgoing data will be written to
    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;
    private Stage stage;

    private boolean serverMode;
    static boolean connected;

    public void initialize() {
        inQueue = new SynchronizedQueue();
        outQueue = new SynchronizedQueue();
        connected = false;

        // Create and start the GUI updater thread
        GUIUpdater updater = new GUIUpdater(inQueue, receivedImage, yourNameText);
        Thread updaterThread = new Thread(updater);
        updaterThread.start();
    }

    public void setStage(Stage theStage) {
        stage = theStage;
    }


    void setServerMode() {
        serverMode = true;
        startButton.setText("Listen");
        sendButton.setDisable(true);
        try {
            IPAddressText.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception ex) {
            ex.printStackTrace();
            statusText.setText("Server start: getLocalHost failed. Exiting....");
        }
    }

    void setClientMode() {
        serverMode = false;
        startButton.setText("Connect");
        IPAddressText.setText("127.0.0.1");
    }

    public void startButtonPressed() {
        // If we're already connected, start button actually means stop
        if (connected) {
            // disconnect the program from the other programs its talking to
            connected = false;
            if (serverMode) {
                startButton.setDisable(false);
            } else {
                startButton.setText("Connect");
            }
            // don't do anything else; the threads will stop and everything will be cleaned up by them.
            return;
        }

        if (serverMode) {
            if (portText.getText().isEmpty()) {
                // user did not enter a Port number, so we can't connect.
                statusText.setText("Type a port number BEFORE listening.");
                return;
            }

            // We're gonna connect!
            connected = true;
            startButton.setDisable(true);

            // We're a server: create a thread for listening for connecting clients
            ConnectToNewClients connectToNewClients = new ConnectToNewClients(Integer.parseInt(portText.getText()), inQueue, outQueue, statusText, yourNameText);
            Thread connectThread = new Thread(connectToNewClients);
            connectThread.start();

        } else {

            // We're gonna connect!
            connected = true;
            startButton.setDisable(true);

            // We're a client: connect to a server
            try {
                Socket serverSocket = new Socket(IPAddressText.getText(), Integer.parseInt(portText.getText()));
                statusText.setText("Connected to server at IP address " + IPAddressText.getText() + " on port " + portText.getText());

                // The serverSocket provides 2 separate streams for 2-way communication
                //   the InputStream is for communication FROM server TO client
                //   the OutputStream is for communication TO server FROM client

                // Every client prepares for communication with its server by creating 2 new threads:
                //   Thread 1: handles communication TO server FROM client
                CommunicationOut communicationOut = new CommunicationOut(serverSocket, new ObjectOutputStream(serverSocket.getOutputStream()), outQueue, statusText, yourNameText);
                Thread communicationOutThread = new Thread(communicationOut);
                communicationOutThread.start();
                //   Thread 2: handles communication FROM server TO client
                CommunicationIn communicationIn = new CommunicationIn(serverSocket, new ObjectInputStream(serverSocket.getInputStream()), inQueue, null, statusText, yourNameText);
                Thread communicationInThread = new Thread(communicationIn);
                communicationInThread.start();

            } catch (Exception ex) {
                ex.printStackTrace();
                statusText.setText("Client start: networking failed. Exiting....");
            }

            // We connected!  Update GUI button to stop connection.
            startButton.setText("Disconnect");
            startButton.setDisable(false);
        }

    }

    public void openPicture() {
        // Show a FileChooser
        final FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);

        // If user chose a file via FileChooser
        if (file != null) {
            Image newImage = new Image(file.toURI().toString());
            sendImage.setImage(newImage);
        }
    }

    public void sendButtonPressed() {
        // send puts sender and data into the outQueue
        String sender = yourNameText.getText();
        Image message = sendImage.getImage();

        boolean putSucceeded = outQueue.put(sender);
        while (!putSucceeded) {
            Thread.currentThread().yield();
            putSucceeded = outQueue.put(sender);
        }

        putSucceeded = outQueue.put(message);
        while (!putSucceeded) {
            Thread.currentThread().yield();
            putSucceeded = outQueue.put(message);
        }
    }

}
