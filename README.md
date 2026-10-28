# Calculadora de TMB

Aplicativo Android para calcular a **Taxa Metabólica Basal (TMB)**, com login de usuário e histórico de cálculos. Projeto acadêmico desenvolvido para a UNIP.

O projeto tem duas partes:

- **App Android** (Java) — interface, cálculo da TMB e histórico.
- **Servidor de autenticação** (Python) — cadastro e login de usuários, com toda a comunicação cifrada.

## Funcionalidades

- Cadastro e login com email e senha
- Cálculo da TMB a partir de peso, altura, idade e sexo
- Histórico dos últimos 30 cálculos, com detalhes de cada um
- Tela de perfil com os dados da conta e opção de sair
- Suporte a tema claro e escuro

## Como a TMB é calculada

O app usa a equação de **Harris-Benedict**:

| Sexo      | Fórmula                                                     |
|-----------|-------------------------------------------------------------|
| Masculino | `66 + (13,7 × peso) + (5 × altura) − (6,8 × idade)`         |
| Feminino  | `655 + (9,6 × peso) + (1,8 × altura) − (4,7 × idade)`       |

Peso em kg, altura em cm e idade em anos. O resultado é dado em kcal/dia.

## Segurança

A comunicação entre o app e o servidor é protegida em duas camadas:

1. **App ↔ API:** todo corpo de requisição e resposta é cifrado com **AES-256-GCM** usando uma chave de transporte compartilhada. Cada mensagem leva um timestamp, e requisições com mais de 5 minutos são recusadas.
2. **API ↔ Banco de dados:**
   - o email é gravado cifrado (AES-256-GCM), junto com um HMAC-SHA256 usado para busca;
   - a senha é gravada como hash **PBKDF2-SHA256** (310.000 iterações) com salt aleatório;
   - o token de sessão é gravado apenas como hash SHA-256 e expira em 7 dias.

## Estrutura do projeto

```
├── app/                              # App Android
│   └── src/main/java/br/unip/tmb/
│       ├── dados/Sessao.java         # Sessão do usuário e histórico (SharedPreferences)
│       ├── rede/ApiClient.java       # Requisições HTTP para a API
│       ├── seguranca/CryptoUtils.java# Cifragem AES-GCM do lado do app
│       └── telas/                    # Activities, fragments e componentes de UI
└── servidor/
    ├── autenticacao/server.py        # API de cadastro e login
    └── criptografia/                 # API auxiliar de cifrar/decifrar textos (FastAPI)
```

## Tecnologias

- **Android:** Java 11, AndroidX, Material Components, ConstraintLayout (minSdk 24, targetSdk 37)
- **Servidor:** Python 3, `http.server`, SQLite, biblioteca `cryptography`

## Como executar

### 1. Servidor

Requer Python 3.

```bash
cd servidor/autenticacao
pip install cryptography
python server.py
```

Na primeira execução, o servidor gera as chaves no arquivo `keys.json` e mostra no terminal uma linha assim:

```
API_TRANSPORT_KEY=...
```

Copie essa linha, pois o app precisa dela. O servidor fica ouvindo em `http://0.0.0.0:8000`.

> `keys.json` e `app.db` não são versionados. Nunca publique esses arquivos.

### 2. App Android

1. Abra o projeto no **Android Studio**.
2. No arquivo `local.properties` (na raiz do projeto), adicione:

   ```properties
   API_TRANSPORT_KEY=<chave mostrada pelo servidor>
   API_BASE_URL=http://10.0.2.2:8000
   ```

   `10.0.2.2` é o endereço do computador visto de dentro do emulador. Se for usar um celular físico, troque pelo IP do computador na rede local (por exemplo, `http://192.168.0.10:8000`).
3. Sincronize o Gradle e rode o app (**Run ▶**).

### API de criptografia (opcional)

Uma API separada para cifrar e decifrar textos com senha (AES-256-GCM + scrypt):

```bash
cd servidor/criptografia
pip install -r requirements.txt
uvicorn main:app --reload
```

A documentação interativa fica em `http://localhost:8000/docs`.

## Endpoints da API de autenticação

| Método | Rota        | Corpo (antes da cifragem)        | Resposta                       |
|--------|-------------|----------------------------------|--------------------------------|
| POST   | `/register` | `{ "email", "password" }`        | `201` conta criada · `409` email já existe |
| POST   | `/login`    | `{ "email", "password" }`        | `200` `{ access_token, email }` · `401` credenciais inválidas |

A senha precisa ter pelo menos 6 caracteres.
