package br.unip.tmb.telas;

import br.unip.tmb.R;
import br.unip.tmb.telas.fragmentos.HistoricoFragment;
import br.unip.tmb.telas.fragmentos.InicioFragment;
import br.unip.tmb.telas.fragmentos.PerfilFragment;
import br.unip.tmb.telas.fragmentos.TmbFragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// Tela principal: mostra uma aba por vez e o menu inferior para navegar
public class MainActivity extends AppCompatActivity {

    BottomNavigationView menu;
    View conteudo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menu = findViewById(R.id.MenuInferior);
        conteudo = findViewById(R.id.Conteudo);

        ajustarBordas();

        menu.setOnItemSelectedListener(item -> {
            abrir(item.getItemId());
            return true;
        });

        // Ao girar a tela o fragment atual já é restaurado sozinho
        if (savedInstanceState == null) {
            abrir(R.id.nav_inicio);
        }
    }

    // Usado pelos atalhos da tela inicial para trocar de aba
    public void irPara(int itemId) {
        menu.setSelectedItemId(itemId);
    }

    private void abrir(int itemId) {
        Fragment tela;
        if (itemId == R.id.nav_calcular) {
            tela = new TmbFragment();
        } else if (itemId == R.id.nav_historico) {
            tela = new HistoricoFragment();
        } else if (itemId == R.id.nav_perfil) {
            tela = new PerfilFragment();
        } else {
            tela = new InicioFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Conteudo, tela)
                .commit();
    }

    // Afasta o conteúdo da barra de status e esconde o menu com o teclado aberto
    private void ajustarBordas() {
        ViewCompat.setOnApplyWindowInsetsListener(conteudo, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean tecladoAberto = insets.isVisible(WindowInsetsCompat.Type.ime());

            menu.setVisibility(tecladoAberto ? View.GONE : View.VISIBLE);
            v.setPadding(barras.left, barras.top, barras.right,
                    tecladoAberto ? teclado.bottom : 0);
            return insets;
        });

        // Menu flutuante: fica acima da barra de navegação do sistema, sem padding interno extra
        int margem = getResources().getDimensionPixelSize(R.dimen.margem_menu);
        menu.setClipToOutline(true);
        ViewCompat.setOnApplyWindowInsetsListener(menu, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = barras.bottom + margem;
            v.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
