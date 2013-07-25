package com.github.opencam.encryption;

public interface EncryptionPlugin {
  public void setPassphrase(String passphrase);

  public byte[] encrypt(byte[] original);

  public byte[] decrypt(byte[] encd);
}
