package br.unip.tmb.seguranca;

import br.unip.tmb.BuildConfig;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Cifra/decifra os dados trocados entre o app e a API (AES-256-GCM)
public class CryptoUtils {

    private static final int IV_SIZE = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKeySpec key() {
        byte[] raw = Base64.decode(BuildConfig.API_TRANSPORT_KEY, Base64.NO_WRAP);
        return new SecretKeySpec(raw, "AES");
    }

    // Transforma o JSON em {"iv": ..., "data": ...} cifrado
    public static JSONObject encrypt(JSONObject payload) throws Exception {
        payload.put("ts", System.currentTimeMillis());

        byte[] iv = new byte[IV_SIZE];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
        byte[] data = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));

        JSONObject envelope = new JSONObject();
        envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        envelope.put("data", Base64.encodeToString(data, Base64.NO_WRAP));
        return envelope;
    }

    // Abre o envelope recebido da API e devolve o JSON original
    public static JSONObject decrypt(JSONObject envelope) throws Exception {
        byte[] iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
        byte[] data = Base64.decode(envelope.getString("data"), Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
        byte[] plain = cipher.doFinal(data);

        return new JSONObject(new String(plain, StandardCharsets.UTF_8));
    }
}
