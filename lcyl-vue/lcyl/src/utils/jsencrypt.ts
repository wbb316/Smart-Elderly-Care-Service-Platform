import JSEncrypt from 'jsencrypt/bin/jsencrypt.min'

const publicKey = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdH\n' +
  'nzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ=='

// 加密
export function encrypt(txt: string): string | false {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  return encryptor.encrypt(txt)
}

// 解密：前端不应持有私钥，密码解密应在后端完成
export function decrypt(txt: string): string | false {
  console.warn('前端不应持有私钥，解密功能已禁用')
  return false
}
