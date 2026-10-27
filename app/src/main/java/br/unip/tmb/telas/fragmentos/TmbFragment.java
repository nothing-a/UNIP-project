package br.unip.tmb.telas.fragmentos;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

// Tela da calculadora de TMB (Taxa Metabólica Basal)
public class TmbFragment extends Fragment {

    // Componentes da interface
    EditText peso, altura, idade; // Campos de entrada
    RadioGroup sexo;              // Controle segmentado (Masculino/Feminino)
    Button calcular;              // Botão de calcular
    TextView resultado;           // Número do resultado
    View cardResultado;           // Cartão que mostra o resultado
    ScrollView rolagem;           // Rolagem da tela

    public TmbFragment() {
        // Define qual layout XML será usado nessa tela
        super(R.layout.fragment_tmb);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Liga os componentes Java aos elementos do XML pelos IDs
        peso = view.findViewById(R.id.Peso);
        altura = view.findViewById(R.id.Altura);
        idade = view.findViewById(R.id.Idade);
        sexo = view.findViewById(R.id.Sexo);
        calcular = view.findViewById(R.id.Calcular);
        resultado = view.findViewById(R.id.Resultado);
        cardResultado = view.findViewById(R.id.CardResultado);
        rolagem = view.findViewById(R.id.Rolagem);

        // ==========================
        // AÇÃO DO BOTÃO
        // ==========================

        calcular.setOnClickListener(v -> {

            // Verifica se algum campo está vazio
            if (peso.getText().toString().isEmpty() ||
                    altura.getText().toString().isEmpty() ||
                    idade.getText().toString().isEmpty()) {

                cardResultado.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Converte os valores digitados para números
            double p = Double.parseDouble(peso.getText().toString());   // peso (kg)
            double a = Double.parseDouble(altura.getText().toString()); // altura (cm)
            int i = Integer.parseInt(idade.getText().toString());       // idade (anos)

            double tmb;
            boolean masculino = sexo.getCheckedRadioButtonId() == R.id.Masculino;

            if (masculino) {
                // Fórmula da TMB para homens
                tmb = 66 + (13.7 * p) + (5 * a) - (6.8 * i);
            } else {
                // Fórmula da TMB para mulheres
                tmb = 655 + (9.6 * p) + (1.8 * a) - (4.7 * i);
            }

            // Exibe o resultado arredondado no cartão
            resultado.setText(String.format(Locale.getDefault(), "%.0f", tmb));
            cardResultado.setVisibility(View.VISIBLE);

            // Guarda no histórico do usuário
            Sessao.adicionarCalculo(requireContext(), tmb, masculino ? "Masculino" : "Feminino");

            esconderTeclado(v);
            rolagem.post(() -> rolagem.smoothScrollTo(0, cardResultado.getBottom()));
        });
    }

    // Fecha o teclado para o resultado ficar visível
    private void esconderTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }
}
