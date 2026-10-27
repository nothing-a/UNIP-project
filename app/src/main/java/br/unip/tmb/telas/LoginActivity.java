package br.unip.tmb.telas;

import br.unip.tmb.R;
import br.unip.tmb.dados.Sessao;
import br.unip.tmb.rede.ApiClient;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    // Android 17+ (API 37) exige esta permissão para acessar a API na rede local
    private static final String PERMISSAO_REDE_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK";

    private EditText editEmail, editSenha;
    private Button btnLogin, btnCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        btnLogin = findViewById(R.id.btnLogin);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        pedirPermissaoRedeLocal();

        btnLogin.setOnClickListener(v -> {
            if (!validarCampos()) return;

            setBotoesAtivos(false);
            ApiClient.login(email(), senha(), new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setBotoesAtivos(true);

                    String token = response.optString("access_token", null);
                    if (token == null) {
                        mostrar("Resposta inválida do servidor");
                        return;
                    }

                    Sessao.salvarLogin(LoginActivity.this, token, email());

                    mostrar("Login realizado com sucesso!");

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String message) {
                    setBotoesAtivos(true);
                    mostrar(message);
                }
            });
        });

        btnCadastrar.setOnClickListener(v -> {
            if (!validarCampos()) return;

            setBotoesAtivos(false);
            ApiClient.register(email(), senha(), new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setBotoesAtivos(true);
                    mostrar(response.optString("message", "Conta criada!"));
                }

                @Override
                public void onError(String message) {
                    setBotoesAtivos(true);
                    mostrar(message);
                }
            });
        });
    }

    private void pedirPermissaoRedeLocal() {
        if (Build.VERSION.SDK_INT >= 37
                && ContextCompat.checkSelfPermission(this, PERMISSAO_REDE_LOCAL)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSAO_REDE_LOCAL}, 1);
        }
    }

    private String email() {
        return editEmail.getText().toString().trim();
    }

    private String senha() {
        return editSenha.getText().toString();
    }

    private boolean validarCampos() {
        if (TextUtils.isEmpty(email())) {
            editEmail.setError("Digite o email");
            return false;
        }
        if (TextUtils.isEmpty(senha())) {
            editSenha.setError("Digite a senha");
            return false;
        }
        return true;
    }

    private void setBotoesAtivos(boolean ativos) {
        btnLogin.setEnabled(ativos);
        btnCadastrar.setEnabled(ativos);
    }

    private void mostrar(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }
}
