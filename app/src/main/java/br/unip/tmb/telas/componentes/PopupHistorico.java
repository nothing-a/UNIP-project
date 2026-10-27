package br.unip.tmb.telas.componentes;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

// Pop-up animado com os detalhes de um cálculo e o que o usuário deve fazer
public class PopupHistorico {

    // ==========================
    // DADOS DE EXEMPLO — altere aqui o conteúdo do pop-up
    // Cada passo: { título, texto, cor }
    // ==========================
    private static Object[][] passos(long tmb) {
        return new Object[][]{
                {"Manter o peso",
                        "Consuma cerca de " + Math.round(tmb * 1.55) + " kcal por dia (atividade moderada).",
                        R.color.green},
                {"Perder peso",
                        "Reduza para cerca de " + (Math.round(tmb * 1.55) - 500) + " kcal por dia.",
                        R.color.orange},
                {"Ganhar massa",
                        "Aumente para cerca de " + (Math.round(tmb * 1.55) + 300) + " kcal por dia.",
                        R.color.accent},
                {"Hidratação",
                        "Beba pelo menos 2 litros de água ao longo do dia.",
                        R.color.accent},
        };
    }

    public static void mostrar(Context context, JSONObject calculo) {
        long tmb = calculo.optLong("tmb");

        Dialog popup = new Dialog(context, R.style.Popup);
        View conteudo = LayoutInflater.from(context).inflate(R.layout.dialog_historico, null);
        popup.setContentView(conteudo);

        ((TextView) conteudo.findViewById(R.id.Valor)).setText(tmb + " kcal");
        ((TextView) conteudo.findViewById(R.id.Descricao)).setText(
                calculo.optString("sexo") + " · " + Sessao.formatarData(calculo.optLong("data")));

        LinearLayout lista = conteudo.findViewById(R.id.Passos);
        Object[][] dados = passos(tmb);
        for (int i = 0; i < dados.length; i++) {
            View item = LayoutInflater.from(context).inflate(R.layout.item_passo, lista, false);
            ((TextView) item.findViewById(R.id.Titulo)).setText((String) dados[i][0]);
            ((TextView) item.findViewById(R.id.Texto)).setText((String) dados[i][1]);
            ((ImageView) item.findViewById(R.id.Icone)).setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, (int) dados[i][2])));
            lista.addView(item);

            // Cada passo sobe e aparece um pouco depois do anterior
            float distancia = 24 * context.getResources().getDisplayMetrics().density;
            item.setAlpha(0f);
            item.setTranslationY(distancia);
            item.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(180 + i * 70L)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        conteudo.findViewById(R.id.Fechar).setOnClickListener(v -> popup.dismiss());

        popup.show();

        // Ocupa a largura da tela, com margem nas laterais
        Window janela = popup.getWindow();
        if (janela != null) {
            int margem = (int) (20 * context.getResources().getDisplayMetrics().density);
            janela.getDecorView().setPadding(margem, 0, margem, 0);
            janela.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
