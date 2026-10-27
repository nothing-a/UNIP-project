
import os
import json
import base64

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt


def derive_key(password: str, salt: bytes) -> bytes:
    kdf = Scrypt(
        salt=salt,
        length=32,
        n=2**14,
        r=8,
        p=1,
    )
    return kdf.derive(password.encode("utf-8"))


def encrypt_data(data: str, password: str) -> str:
    salt = os.urandom(16)
    nonce = os.urandom(12)

    key = derive_key(password, salt)
    aes = AESGCM(key)

    ciphertext = aes.encrypt(
        nonce,
        data.encode("utf-8"),
        None,
    )

    result = {
        "version": 1,
        "salt": base64.b64encode(salt).decode("ascii"),
        "nonce": base64.b64encode(nonce).decode("ascii"),
        "ciphertext": base64.b64encode(ciphertext).decode("ascii"),
    }

    return json.dumps(result)


def decrypt_data(encrypted_data: str, password: str) -> str:
    package = json.loads(encrypted_data)

    if package.get("version") != 1:
        raise ValueError("Unsupported encrypted-data version")

    salt = base64.b64decode(package["salt"], validate=True)
    nonce = base64.b64decode(package["nonce"], validate=True)
    ciphertext = base64.b64decode(
        package["ciphertext"], validate=True
    )

    if len(salt) != 16 or len(nonce) != 12:
        raise ValueError("Invalid encrypted-data format")

    key = derive_key(password, salt)
    aes = AESGCM(key)

    plaintext = aes.decrypt(nonce, ciphertext, None)
    return plaintext.decode("utf-8")
