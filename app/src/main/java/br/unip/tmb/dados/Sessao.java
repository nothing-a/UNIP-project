package br.unip.tmb.dados;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Dados do usuário logado, guardados localmente
public class Sessao {

    private static final String PREFS = "user_session";
    private static final int LIMITE_HISTORICO = 30;

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void salvarLogin(Context context, String token, String email) {
        prefs(context).edit()
                .putString("access_token", token)
                .putString("email", email)
                .putLong("ultimo_acesso", System.currentTimeMillis())
                .apply();
    }

    public static void sair(Context context) {
        prefs(context).edit()
                .remove("access_token")
                .apply();
    }

    public static String email(Context context) {
        return prefs(context).getString("email", "");
    }

    // "sophia.costa@email.com" -> "Sophia Costa"
    public static String nome(Context context) {
        String email = email(context);
        if (email.isEmpty()) return "Usuário";

        String usuario = email.split("@")[0];
        StringBuilder nome = new StringBuilder();
        for (String parte : usuario.split("[._\\-]+")) {
            if (parte.isEmpty()) continue;
            if (nome.length() > 0) nome.append(' ');
            nome.append(Character.toUpperCase(parte.charAt(0)))
                    .append(parte.substring(1).toLowerCase(Locale.getDefault()));
        }
        return nome.length() > 0 ? nome.toString() : "Usuário";
    }

    public static String iniciais(Context context) {
        String[] partes = nome(context).split(" ");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) iniciais += partes[partes.length - 1].charAt(0);
        return iniciais.toUpperCase(Locale.getDefault());
    }

    public static String ultimoAcesso(Context context) {
        long quando = prefs(context).getLong("ultimo_acesso", 0);
        return quando == 0 ? "—" : formatarData(quando);
    }

    public static String formatarData(long quando) {
        return new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(new Date(quando));
    }

    // ==========================
    // HISTÓRICO DE CÁLCULOS DE TMB
    // ==========================

    public static void adicionarCalculo(Context context, double tmb, String sexo) {
        JSONArray antigo = historico(context);
        JSONArray novo = new JSONArray();
        try {
            JSONObject calculo = new JSONObject();
            calculo.put("tmb", Math.round(tmb));
            calculo.put("sexo", sexo);
            calculo.put("data", System.currentTimeMillis());
            novo.put(calculo);

            // Mais recente primeiro, até o limite
            for (int i = 0; i < antigo.length() && novo.length() < LIMITE_HISTORICO; i++) {
                novo.put(antigo.get(i));
            }
        } catch (JSONException e) {
            return;
        }
        prefs(context).edit().putString(chaveHistorico(context), novo.toString()).apply();
    }

    public static JSONArray historico(Context context) {
        try {
            return new JSONArray(prefs(context).getString(chaveHistorico(context), "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    // Um histórico separado para cada conta
    private static String chaveHistorico(Context context) {
        return "historico_" + email(context);
    }
}
