package sample.clientsidechat;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private Button button_send;
    @FXML
    private TextField tf_message;
    @FXML
    private VBox vbox_message;
    @FXML
    private ScrollPane sp_main;

    private Client client;

    //Zmienne potrzebne do szyfrowania
    private static boolean areKeysReceived;
    private static String receivedPublicDHKey;
    private static PublicKey receivedPublicRSAKey;
    private String publicKeyString;
    private KeyPairGenerator keyPairGeneratorClient;
    private static KeyPair keyPair;
    private DHPublicKey kluczPublicznyDH;
    private static String secretKey;
    private static String hash;
    private static String decryptedRSAHash;
    private PublicKey publicKey;
    private static PrivateKey privateKey;
    private String encryptHashRSA;
    private static String decryptedMessage;
    private static String receivedMessageHash;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try{
            client = new Client(new Socket("localhost", 9999));
            System.out.println("Podlaczono do serwera");
        } catch (IOException e){
            e.printStackTrace();
        }

        Controller.areKeysReceived = false;

        // Inicjalizacja generatora kluczy Diffie-Hellmana
        keyPairGeneratorClient = null;
        try {
            keyPairGeneratorClient = KeyPairGenerator.getInstance("DiffieHellman");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Wygenerowanie parametrów Diffie-Hellmana (p i g)
        keyPairGeneratorClient.initialize(2048); // Długość klucza w bitach

        // Generowanie klucza prywatnego i publicznego
        Controller.keyPair = keyPairGeneratorClient.generateKeyPair();
        System.out.println("Wygenerowano klucze Diffie-Hellman");
        kluczPublicznyDH = (DHPublicKey) keyPair.getPublic();

        // Kodowanie klucza publicznego do postaci Base64
        publicKeyString = Base64.getEncoder().encodeToString(kluczPublicznyDH.getEncoded());
        System.out.println("Klucz publiczny DH (w postaci Stringa): " + publicKeyString);

        try {
            KeyPair keyPair = generateRSAKeyPair();
            publicKey = keyPair.getPublic();
            Controller.privateKey = keyPair.getPrivate();

            System.out.println("Wygenerowano klucze RSA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Nie można znaleźć algorytmu RSA.");
        }

        String publicKeyRSAString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        System.out.println("Klucz publiczny RSA (w postaci Stringa): " + publicKeyRSAString);

        String keys = publicKeyString + "," + publicKeyRSAString;

        client.sendMessageToServer(keys);

        vbox_message.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                sp_main.setVvalue((Double) newValue);
            }
        });

        client.receiveMessageFromServer(vbox_message);

        button_send.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String messageToSend = tf_message.getText();
                if(!messageToSend.isEmpty()){
                    HBox hBox = new HBox();
                    hBox.setAlignment(Pos.CENTER_RIGHT);
                    hBox.setPadding(new Insets(5,5,5,10));

                    Text text = new Text(messageToSend);
                    TextFlow textFlow = new TextFlow(text);

                    textFlow.setStyle("-fx-color: rgb(239,242,255);" + "-fx-background-color: rgb(207,153,6);" + "-fx-background-radius: 20px;");
                    textFlow.setPadding(new Insets(5,10,5,10));
                    text.setFill(Color.color(0.934, 0.945, 0.996));

                    try {
                        Controller.hash = calculateSHA256Hash(messageToSend);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    System.out.println("SHA-256 Hash wiadomosci do wyslania: " + hash);
                    try {
                        byte[] encryptedBytes = encryptRSA(hash, receivedPublicRSAKey);
                        encryptHashRSA = Base64.getEncoder().encodeToString(encryptedBytes);
                        System.out.println("Zaszyfrowany przez RSA HASH: " + bytesToHex(encryptedBytes));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        byte[] secretKeyByte = Base64.getDecoder().decode(Controller.secretKey);
                        SecretKey secretKey = new SecretKeySpec(secretKeyByte , 0, 16, "AES");
                        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                        byte[] encryptedMessage = cipher.doFinal(messageToSend.getBytes(StandardCharsets.UTF_8));
                        String encryptMessageAES = Base64.getEncoder().encodeToString(encryptedMessage);
                        System.out.println("Zaszyfrowana wiadomość AES: " + encryptMessageAES);

                        String combined = encryptMessageAES + "," + encryptHashRSA;
                        client.sendMessageToServer(combined);
                        tf_message.clear();

                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    }

                    hBox.getChildren().add(textFlow);
                    vbox_message.getChildren().add(hBox);

                }
            }
        });
    }

    public static byte[] encryptRSA(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptRSA(byte[] encryptedBytes, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String calculateSHA256Hash(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // Długość klucza (w bitach)
        return keyPairGenerator.generateKeyPair();
    }

    public static void addLabel(String messageFromServer, VBox vbox){
        if(!areKeysReceived){
            String[] splittedKeys = messageFromServer.split(",");
            Controller.receivedPublicDHKey = splittedKeys[0];
            String receivedPublicRSAKeyTemp = splittedKeys[1];
            byte[] publicKeyBytes = Base64.getDecoder().decode(receivedPublicRSAKeyTemp);

            System.out.println("Otrzymano klucze od Alice");


            // Tworzenie specyfikacji klucza publicznego
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

            // Tworzenie obiektu klucza publicznego RSA
            KeyFactory keyFactory = null;
            try {
                keyFactory = KeyFactory.getInstance("RSA");
                Controller.receivedPublicRSAKey = keyFactory.generatePublic(publicKeySpec);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
            generateSecretKey(receivedPublicDHKey);
            Controller.areKeysReceived = true;
            return;
        }

        String[] splittedKeys = messageFromServer.split(",");
        String encryptedAESMessage = splittedKeys[0];
        String encryptedRSAHash = splittedKeys[1];

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5,5,5,10));
        try {
            // Dekoduj zaszyfrowaną wiadomość z formatu Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedAESMessage);
            byte[] secretKeyByte = Base64.getDecoder().decode(Controller.secretKey);
            SecretKey secretKey = new SecretKeySpec(secretKeyByte, 0, 16, "AES");

            // Inicjalizuj obiekt klasy Cipher w trybie deszyfrowania

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            // Deszyfruj wiadomość
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // Konwertuj zdeszyfrowane bajty na łańcuch znaków
            Controller.decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("Odszyfrowana otrzymana wiadomosc AES: " + decryptedMessage);

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
                byte[] encryptedHashBytes = Base64.getDecoder().decode(encryptedRSAHash);
                Controller.decryptedRSAHash = decryptRSA(encryptedHashBytes, Controller.privateKey);
                System.out.println("Odszyfrowana przez RSA Hash otrzymanej wiadomosci: " + decryptedRSAHash);
        } catch (Exception e) {
                e.printStackTrace();
            }

        try {
            Controller.receivedMessageHash = calculateSHA256Hash(decryptedMessage);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if(receivedMessageHash.equals(decryptedRSAHash)) {
            System.out.println("Hashe zgodne");
            Text text = new Text(decryptedMessage);
            TextFlow textFlow = new TextFlow(text);


            textFlow.setStyle("-fx-text-fill: rgb(239,242,255);" + "-fx-background-color: rgb(48,47,44);" + "-fx-background-radius: 20px;");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            hBox.getChildren().add(textFlow);


            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    vbox.getChildren().add(hBox);
                }
            });
        }
    }
    public static void generateSecretKey(String receivedPublicDHKey){
        try {
            // Dekodowanie klucza publicznego z Base64
            byte[] receivedPublicKeyBytes = Base64.getDecoder().decode(receivedPublicDHKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(receivedPublicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("DiffieHellman");
            PublicKey receivedPublicKey = keyFactory.generatePublic(x509KeySpec);

            // Inicjalizacja protokołu Diffie-Hellmana na drugiej stronie
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DiffieHellman");
            keyAgreement.init(keyPair.getPrivate());

            // Wykonanie fazy pierwszej protokołu Diffie-Hellmana
            keyAgreement.doPhase(receivedPublicKey, true);

            // Generowanie tajnego klucza
            byte[] secretKeyByte = keyAgreement.generateSecret();
            Controller.secretKey = new BigInteger(1, secretKeyByte).toString(16);
            // Wyświetlenie tajnego klucza
            System.out.println("Wspólny klucz dla obu uzytkownikow: " + Controller.secretKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

    }
}