package com.example.cypthchatapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class MainActivity extends Activity {

    private EditText inputText;
    private EditText inputKey;
    private TextView outputText;
    private TextView outputCodedsha1;
    private TextView outputSecodedsha1;
    private TextView outputCompare;

    private Button encryptButton;
    private Button decryptButton;
    private Button compareButton;

    private static final String KEY = "mySecretKey12345";
    private static final String INIT_VECTOR = "16BytesInitVector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.input_text);
        inputKey = findViewById(R.id.input_key);
        outputText = findViewById(R.id.output_text);
        outputCodedsha1 = findViewById(R.id.output_codedsha1);
        outputSecodedsha1 = findViewById(R.id.output_decodedsha1);
        outputCompare = findViewById(R.id.output_compare);
        encryptButton = findViewById(R.id.encrypt_button);
        decryptButton = findViewById(R.id.decrypt_button);
        compareButton = findViewById(R.id.compare_button);

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = inputText.getText().toString();
                encrypt(input);
                Toast.makeText(MainActivity.this, "Tekst zaszyfrowany", Toast.LENGTH_SHORT).show();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = inputText.getText().toString();
                String key = inputKey.getText().toString();
                decrytpt(input, key);
                Toast.makeText(MainActivity.this, "Tekst zaszyfrowany", Toast.LENGTH_SHORT).show();
            }
        });

        compareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //decrytpt();
            }
        });
    }

    private String encrypt(String input) {
        try {
            // generowanie klucza AES
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // klucz 256 bitowy
            SecretKey secretKey = keyGen.generateKey();

            // szyfrowanie
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedMessage = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

            // kodowanie Base64
            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            outputText.setText("Zakodowana wiadomość: " + encodedMessage + " ///Klucz: " + encodedKey);
            inputText.setText(encodedMessage);
            inputKey.setText(encodedKey);
            sha1hashCoded(input);
        } catch (Exception e) {
            outputText.setText("Błąd: " + e.toString());
        }
        return null;
    }

    private String decrytpt(String input, String key){
        try {
            // dekodowanie Base64
            byte[] decodedMessage = Base64.getDecoder().decode(input);
            byte[] decodedKey = Base64.getDecoder().decode(key);

            // odtworzenie klucza
            SecretKey secretKey = new SecretKeySpec(decodedKey, "AES");

            // deszyfrowanie
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedMessage = cipher.doFinal(decodedMessage);

            String message = new String(decryptedMessage, StandardCharsets.UTF_8);

            outputText.setText("Odkodowana wiadomość: " + message);
            sha1hashDecoded(message);
        } catch (Exception e) {
            outputText.setText("Błąd: " + e.toString());
        }
        return null;
    }

    private String sha1hashCoded(String input){
        try {
            // tworzenie instancji obiektu MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // obliczanie skrótu wiadomości
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // konwertowanie tablicy bajtów na łańcuch szesnastkowy
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hashString = hexString.toString();

            outputCodedsha1.setText("Zahashowana wiadomość o kodowaniu: " + hashString);
        } catch (Exception e) {
            outputCodedsha1.setText("Błąd: " + e.toString());
        }
        return null;
    }

    private String sha1hashDecoded(String input){
        try {
            // tworzenie instancji obiektu MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // obliczanie skrótu wiadomości
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // konwertowanie tablicy bajtów na łańcuch szesnastkowy
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hashString = hexString.toString();

            outputSecodedsha1.setText("Zahashowana wiadomość po decodowaniu: " + hashString);
        } catch (Exception e) {
            outputSecodedsha1.setText("Błąd: " + e.toString());
        }
        return null;
    }
}