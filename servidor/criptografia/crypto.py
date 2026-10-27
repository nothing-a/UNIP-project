
import os
import base64

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt


def derive_key(password: str, salt: bytes) -> bytes:
    if not password:
        raise ValueError("Password cannot be empty")

    kdf = Scrypt(
        salt=salt,
        length=32,
        n=2**14,
        r=8,
        p=1,
    )

    return kdf.derive(password.encode("utf-8"))


def encrypt_text(plaintext: str, password: str) -> dict:
    salt = os.urandom(16)
    nonce = os.urandom(12)

    key = derive_key(password, salt)
    aesgcm = AESGCM(key)

    ciphertext = aesgcm.encrypt(
        nonce,
        plaintext.encode("utf-8"),
        None,
    )

    return {
        "version": 1,
        "salt": base64.b64encode(salt).decode("ascii"),
        "nonce": base64.b64encode(nonce).decode("ascii"),
        "ciphertext": base64.b64encode(ciphertext).decode("ascii"),
    }


def decrypt_text(package: dict, password: str) -> str:
    salt = base64.b64decode(package["salt"], validate=True)
    nonce = base64.b64decode(package["nonce"], validate=True)
    ciphertext = base64.b64decode(
        package["ciphertext"],
        validate=True,
    )

    if len(salt) != 16 or len(nonce) != 12:
        raise ValueError("Invalid encryption package")

    key = derive_key(password, salt)
    aesgcm = AESGCM(key)

    plaintext = aesgcm.decrypt(nonce, ciphertext, None)
    return plaintext.decode("utf-8")
