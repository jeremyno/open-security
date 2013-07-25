package com.github.opencam.encryption;

public class Aes128Encyption implements EncyptionPlugin {
  String passphrase;

  public void setPassphrase(final String passphrase) {
    this.passphrase = passphrase;

  }

  public byte[] encrypt(final byte[] original) {
    return EncryptionUtils.encrypt(original, passphrase);
  }

  public byte[] decrypt(final byte[] encd) {
    return EncryptionUtils.encrypt(encd, passphrase);
  }

}
