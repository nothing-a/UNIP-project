"""
API de autenticação que fica entre o app Android e o banco de dados.

Camadas de criptografia:
  1. App <-> API: todo corpo de requisição/resposta é um envelope
     {"iv": base64, "data": base64} cifrado com AES-256-GCM (TRANSPORT_KEY).
  2. API <-> BD: dados sensíveis são cifrados antes de gravar e decifrados
     ao ler (DB_KEY). O email é gravado cifrado + um HMAC para busca;
     a senha é gravada como hash PBKDF2 (não reversível); o token de sessão
     é gravado apenas como hash SHA-256.

Uso:
    python server.py            # sobe em 0.0.0.0:8000
Na primeira execução as chaves são geradas em keys.json e a chave de
transporte é exibida para ser colocada em local.properties do app.
"""

import base64
import hashlib
import hmac
import json
import os
import secrets
import sqlite3
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "app.db")
KEYS_PATH = os.path.join(BASE_DIR, "keys.json")

PBKDF2_ITERATIONS = 310_000
MAX_CLOCK_SKEW = 300          # segundos: requisições mais antigas são recusadas
SESSION_TTL = 7 * 24 * 3600   # validade do token de sessão


# ==========================
# CHAVES
# ==========================

def load_keys():
    if not os.path.exists(KEYS_PATH):
        keys = {
            "transport_key": base64.b64encode(AESGCM.generate_key(256)).decode(),
            "db_key": base64.b64encode(AESGCM.generate_key(256)).decode(),
            "hmac_key": base64.b64encode(secrets.token_bytes(32)).decode(),
        }
        with open(KEYS_PATH, "w") as f:
            json.dump(keys, f, indent=2)
        print("Chaves geradas em", KEYS_PATH)
    with open(KEYS_PATH) as f:
        keys = json.load(f)
    return (
        base64.b64decode(keys["transport_key"]),
        base64.b64decode(keys["db_key"]),
        base64.b64decode(keys["hmac_key"]),
    )


TRANSPORT_KEY, DB_KEY, HMAC_KEY = load_keys()


# ==========================
# CRIPTOGRAFIA
# ==========================

def aes_encrypt(key, plaintext: bytes):
    iv = secrets.token_bytes(12)
    return iv, AESGCM(key).encrypt(iv, plaintext, None)


def aes_decrypt(key, iv: bytes, ciphertext: bytes) -> bytes:
    return AESGCM(key).decrypt(iv, ciphertext, None)


# Camada App <-> API
def open_envelope(envelope: dict) -> dict:
    iv = base64.b64decode(envelope["iv"])
    data = base64.b64decode(envelope["data"])
    payload = json.loads(aes_decrypt(TRANSPORT_KEY, iv, data))
    if abs(time.time() * 1000 - payload.get("ts", 0)) > MAX_CLOCK_SKEW * 1000:
        raise ValueError("Requisição expirada")
    return payload


def seal_envelope(payload: dict) -> dict:
    payload = dict(payload, ts=int(time.time() * 1000))
    iv, data = aes_encrypt(TRANSPORT_KEY, json.dumps(payload).encode())
    return {"iv": base64.b64encode(iv).decode(), "data": base64.b64encode(data).decode()}


# Camada API <-> BD
def db_encrypt(value: str) -> bytes:
    iv, data = aes_encrypt(DB_KEY, value.encode())
    return iv + data


def db_decrypt(blob: bytes) -> str:
    return aes_decrypt(DB_KEY, blob[:12], blob[12:]).decode()


def lookup_hash(value: str) -> str:
    return hmac.new(HMAC_KEY, value.encode(), hashlib.sha256).hexdigest()


def hash_password(password: str, salt: bytes) -> bytes:
    return hashlib.pbkdf2_hmac("sha256", password.encode(), salt, PBKDF2_ITERATIONS)


# ==========================
# BANCO DE DADOS
# ==========================

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    with get_db() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                email_hash    TEXT UNIQUE NOT NULL,
                email_enc     BLOB NOT NULL,
                password_hash BLOB NOT NULL,
                salt          BLOB NOT NULL,
                created_at    INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS sessions (
                token_hash TEXT PRIMARY KEY,
                user_id    INTEGER NOT NULL REFERENCES users(id),
                expires_at INTEGER NOT NULL
            );
        """)


def create_user(email, password):
    salt = secrets.token_bytes(16)
    with get_db() as conn:
        conn.execute(
            "INSERT INTO users (email_hash, email_enc, password_hash, salt, created_at)"
            " VALUES (?, ?, ?, ?, ?)",
            (lookup_hash(email), db_encrypt(email), hash_password(password, salt),
             salt, int(time.time())),
        )


def authenticate(email, password):
    with get_db() as conn:
        row = conn.execute(
            "SELECT * FROM users WHERE email_hash = ?", (lookup_hash(email),)
        ).fetchone()
    if row is None:
        return None
    if not hmac.compare_digest(hash_password(password, row["salt"]), row["password_hash"]):
        return None
    return row


def create_session(user_id):
    token = secrets.token_urlsafe(32)
    with get_db() as conn:
        conn.execute(
            "INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, ?)",
            (hashlib.sha256(token.encode()).hexdigest(), user_id,
             int(time.time()) + SESSION_TTL),
        )
    return token


# ==========================
# ROTAS
# ==========================

def handle_register(payload):
    email = str(payload.get("email", "")).strip().lower()
    password = str(payload.get("password", ""))
    if "@" not in email or len(password) < 6:
        return 400, {"error": "Email inválido ou senha com menos de 6 caracteres"}
    try:
        create_user(email, password)
    except sqlite3.IntegrityError:
        return 409, {"error": "Email já cadastrado"}
    return 201, {"message": "Conta criada com sucesso"}


def handle_login(payload):
    email = str(payload.get("email", "")).strip().lower()
    password = str(payload.get("password", ""))
    user = authenticate(email, password)
    if user is None:
        return 401, {"error": "Email ou senha incorretos"}
    return 200, {
        "access_token": create_session(user["id"]),
        "email": db_decrypt(user["email_enc"]),
    }


ROUTES = {"/register": handle_register, "/login": handle_login}


class Handler(BaseHTTPRequestHandler):

    def do_POST(self):
        route = ROUTES.get(self.path)
        if route is None:
            return self.respond(404, {"error": "Rota não encontrada"})
        try:
            length = int(self.headers.get("Content-Length", 0))
            payload = open_envelope(json.loads(self.rfile.read(length)))
        except Exception:
            return self.respond(400, {"error": "Requisição inválida"})
        status, body = route(payload)
        self.respond(status, body)

    def respond(self, status, body):
        raw = json.dumps(seal_envelope(body)).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)


if __name__ == "__main__":
    init_db()
    print("API_TRANSPORT_KEY=" + base64.b64encode(TRANSPORT_KEY).decode())
    print("Servidor ouvindo em http://0.0.0.0:8000")
    ThreadingHTTPServer(("0.0.0.0", 8000), Handler).serve_forever()
