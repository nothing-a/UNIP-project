package br.unip.tmb.telas.fragmentos;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;
import br.unip.tmb.telas.LoginActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Dados da conta e opção de sair
public class PerfilFragment extends Fragment {

    public PerfilFragment() {
        super(R.layout.fragment_perfil);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.Avatar)).setText(Sessao.iniciais(requireContext()));
        ((TextView) view.findViewById(R.id.Nome)).setText(Sessao.nome(requireContext()));
        ((TextView) view.findViewById(R.id.Email)).setText(Sessao.email(requireContext()));
        ((TextView) view.findViewById(R.id.UltimoAcesso)).setText(Sessao.ultimoAcesso(requireContext()));

        view.findViewById(R.id.Sair).setOnClickListener(v -> {
            Sessao.sair(requireContext());
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
    }
}
