package br.unip.tmb.telas.fragmentos;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;
import br.unip.tmb.telas.componentes.GraficoHistorico;
import br.unip.tmb.telas.componentes.PopupHistorico;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Gráfico com a evolução dos cálculos de TMB já feitos pelo usuário
public class HistoricoFragment extends Fragment {

    public HistoricoFragment() {
        super(R.layout.fragment_historico);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View conteudo = view.findViewById(R.id.Conteudo);
        View vazio = view.findViewById(R.id.Vazio);

        JSONArray historico = Sessao.historico(requireContext());
        boolean temItens = historico.length() > 0;
        conteudo.setVisibility(temItens ? View.VISIBLE : View.GONE);
        vazio.setVisibility(temItens ? View.GONE : View.VISIBLE);
        if (!temItens) return;

        // O histórico vem do mais recente para o mais antigo; o gráfico usa a ordem inversa
        List<JSONObject> calculos = new ArrayList<>();
        long soma = 0;
        for (int i = historico.length() - 1; i >= 0; i--) {
            JSONObject calculo = historico.optJSONObject(i);
            calculos.add(calculo);
            soma += calculo.optLong("tmb");
        }

        long primeiro = calculos.get(0).optLong("tmb");
        long atual = calculos.get(calculos.size() - 1).optLong("tmb");
        long variacao = atual - primeiro;

        ((TextView) view.findViewById(R.id.Atual)).setText(String.valueOf(atual));
        ((TextView) view.findViewById(R.id.Media)).setText(String.valueOf(soma / calculos.size()));

        TextView textoVariacao = view.findViewById(R.id.Variacao);
        if (variacao > 0) {
            textoVariacao.setText("▲ " + variacao);
            textoVariacao.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        } else if (variacao < 0) {
            textoVariacao.setText("▼ " + Math.abs(variacao));
            textoVariacao.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange));
        } else {
            textoVariacao.setText("0");
        }

        // Cartão com o ponto escolhido no gráfico; tocando nele abre os detalhes
        FrameLayout selecionado = view.findViewById(R.id.Selecionado);
        View item = getLayoutInflater().inflate(R.layout.item_historico, selecionado, false);
        selecionado.addView(item);

        GraficoHistorico grafico = view.findViewById(R.id.Grafico);
        grafico.setAoSelecionar(calculo -> {
            ((TextView) item.findViewById(R.id.Data))
                    .setText(Sessao.formatarData(calculo.optLong("data")));
            ((TextView) item.findViewById(R.id.Sexo))
                    .setText(calculo.optString("sexo"));
            ((TextView) item.findViewById(R.id.Valor))
                    .setText(calculo.optLong("tmb") + " kcal");
            item.setOnClickListener(v -> PopupHistorico.mostrar(requireContext(), calculo));
        });
        grafico.setCalculos(calculos);
    }
}
