package br.unip.tmb.telas.fragmentos;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;
import br.unip.tmb.telas.componentes.PopupHistorico;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

// Lista dos cálculos de TMB já feitos pelo usuário
public class HistoricoFragment extends Fragment {

    public HistoricoFragment() {
        super(R.layout.fragment_historico);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LinearLayout lista = view.findViewById(R.id.Lista);
        View vazio = view.findViewById(R.id.Vazio);

        JSONArray historico = Sessao.historico(requireContext());
        boolean temItens = historico.length() > 0;
        lista.setVisibility(temItens ? View.VISIBLE : View.GONE);
        vazio.setVisibility(temItens ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < historico.length(); i++) {
            JSONObject calculo = historico.optJSONObject(i);

            View item = inflater.inflate(R.layout.item_historico, lista, false);
            if (i > 0) {
                // Espaço entre um cartão e outro
                ((LinearLayout.LayoutParams) item.getLayoutParams()).topMargin =
                        getResources().getDimensionPixelSize(R.dimen.espaco_cartoes);
            }
            ((TextView) item.findViewById(R.id.Data))
                    .setText(Sessao.formatarData(calculo.optLong("data")));
            ((TextView) item.findViewById(R.id.Sexo))
                    .setText(calculo.optString("sexo"));
            ((TextView) item.findViewById(R.id.Valor))
                    .setText(calculo.optLong("tmb") + " kcal");
            item.setOnClickListener(v -> PopupHistorico.mostrar(requireContext(), calculo));
            lista.addView(item);
        }
    }
}
