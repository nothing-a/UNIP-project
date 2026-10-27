
from crypto import encrypt_data, decrypt_data

password = "Example-Password-For-Testing-Only!"
personal_data = "Example personal data: John, john@example.com"

encrypted = encrypt_data(personal_data, password)
print("Encrypted:", encrypted)

decrypted = decrypt_data(encrypted, password)
print("Decrypted:", decrypted)

assert decrypted == personal_data

print("Test passed!")
