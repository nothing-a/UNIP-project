package br.unip.tmb.rede;

import br.unip.tmb.BuildConfig;
import br.unip.tmb.seguranca.CryptoUtils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    public static void login(String email, String senha, Callback callback) {
        sendCredentials("/login", email, senha, callback);
    }

    public static void register(String email, String senha, Callback callback) {
        sendCredentials("/register", email, senha, callback);
    }

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private static void sendCredentials(
            String endpoint,
            String email,
            String senha,
            Callback callback
    ) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", senha);

            post(endpoint, body, callback);

        } catch (Exception e) {
            callback.onError("Não foi possível preparar a requisição.");
        }
    }

    private static void post(
            String endpoint,
            JSONObject body,
            Callback callback
    ) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(BASE_URL + endpoint);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type", "application/json"
                );
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // Cifra o corpo antes de enviar
                byte[] encrypted = CryptoUtils.encrypt(body).toString()
                        .getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(encrypted);
                }

                int status = connection.getResponseCode();

                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder responseText = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseText.append(line);
                        }
                    }
                }

                // Decifra a resposta da API
                JSONObject response = CryptoUtils.decrypt(
                        new JSONObject(responseText.toString())
                );

                if (status >= 200 && status < 300) {
                    mainHandler.post(() -> callback.onSuccess(response));
                } else {
                    String message = response.optString("error", "Erro " + status);
                    mainHandler.post(() -> callback.onError(message));
                }

            } catch (Exception e) {
                Log.e("ApiClient", "Falha em " + endpoint, e);
                String message = e.getMessage() != null
                        ? e.getMessage()
                        : "Falha na conexão";

                mainHandler.post(() -> callback.onError(message));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                executor.shutdown();
            }
        });
    }
}
