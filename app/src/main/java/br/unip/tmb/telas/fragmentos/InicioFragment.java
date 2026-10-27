package br.unip.tmb.telas.fragmentos;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;
import br.unip.tmb.telas.MainActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

// Tela inicial com as informações do usuário
public class InicioFragment extends Fragment {

    TextView saudacao, avatar, nome, email, ultimaTmb, totalCalculos, ultimoAcesso;

    public InicioFragment() {
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        saudacao = view.findViewById(R.id.Saudacao);
        avatar = view.findViewById(R.id.Avatar);
        nome = view.findViewById(R.id.Nome);
        email = view.findViewById(R.id.Email);
        ultimaTmb = view.findViewById(R.id.UltimaTmb);
        totalCalculos = view.findViewById(R.id.TotalCalculos);
        ultimoAcesso = view.findViewById(R.id.UltimoAcesso);

        MainActivity main = (MainActivity) requireActivity();
        view.findViewById(R.id.CardUsuario).setOnClickListener(v -> main.irPara(R.id.nav_perfil));
        view.findViewById(R.id.AtalhoCalcular).setOnClickListener(v -> main.irPara(R.id.nav_calcular));
        view.findViewById(R.id.AtalhoHistorico).setOnClickListener(v -> main.irPara(R.id.nav_historico));
        view.findViewById(R.id.AtalhoPerfil).setOnClickListener(v -> main.irPara(R.id.nav_perfil));
    }

    @Override
    public void onResume() {
        super.onResume();

        String nomeUsuario = Sessao.nome(requireContext());
        saudacao.setText("Olá, " + nomeUsuario.split(" ")[0]);
        avatar.setText(Sessao.iniciais(requireContext()));
        nome.setText(nomeUsuario);
        email.setText(Sessao.email(requireContext()));
        ultimoAcesso.setText("Último acesso: " + Sessao.ultimoAcesso(requireContext()));

        JSONArray historico = Sessao.historico(requireContext());
        totalCalculos.setText(String.valueOf(historico.length()));
        if (historico.length() > 0) {
            ultimaTmb.setText(String.valueOf(historico.optJSONObject(0).optLong("tmb")));
        } else {
            ultimaTmb.setText("—");
        }
    }
}
