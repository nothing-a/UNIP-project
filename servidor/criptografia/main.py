
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from cryptography.exceptions import InvalidTag

from crypto import encrypt_text, decrypt_text

app = FastAPI(
    title="SecureVault API",
    description="API for text encryption and decryption",
    version="1.0.0",
)


class EncryptRequest(BaseModel):
    text: str
    password: str = Field(min_length=1, max_length=1024)


class DecryptRequest(BaseModel):
    encrypted_data: dict
    password: str = Field(min_length=1, max_length=1024)


@app.get("/")
def home():
    return {"message": "SecureVault API is running"}


@app.post("/encrypt")
def encrypt(request: EncryptRequest):
    try:
        encrypted_data = encrypt_text(
            request.text,
            request.password,
        )
        return {"encrypted_data": encrypted_data}

    except ValueError as error:
        raise HTTPException(
            status_code=400,
            detail=str(error),
        )


@app.post("/decrypt")
def decrypt(request: DecryptRequest):
    try:
        text = decrypt_text(
            request.encrypted_data,
            request.password,
        )
        return {"text": text}

    except (InvalidTag, ValueError, KeyError, TypeError):
        raise HTTPException(
            status_code=400,
            detail="Decryption failed. Check the password and encrypted data.",
        )
