### 安卓使用https
---

#### 使用keytool获取证书

1. 命令行（可根据keytool -genkey -help查看指令）

    ` keytool -genkey -v -alias 别名 -keyalg 证书算法 -keypass 密钥密码 -storepass 密钥库密码 -keystore 密钥存放路径`

2. 导出密钥到cer文件(根据 -help查看指令）

    `keytool -export -alias 别名 -keystore 密钥库路径 -file 输出文件名称 -storepass 密钥库密码`

#### 使用HttpURLConnection 访问https

```java
if (connection instanceof HttpsURLConnection) {// 判断是否为https请求
     SSLContext sslContext = getSSLContextWithCer(context);
     if (sslContext != null) {
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        ((HttpsURLConnection) connection).setDefaultSSLSocketFactory(sslSocketFactory);
        ((HttpsURLConnection) connection).setHostnameVerifier(HttpsUtil.hostnameVerifier);
    }
}
```

##### getSSLContextWithCer(Context context)

```java
private SSLContext getSSLContextWithCer(Context context) throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        // 实例化SSLContext
        SSLContext sslContext = SSLContext.getInstance("SSL");

        // 从assets中加载证书
        // 在HTTPS通讯中最常用的是cer/crt和pem
        InputStream inStream = context.getAssets().open("https.cer");

        /*
         * X.509 标准规定了证书可以包含什么信息，并说明了记录信息的方法 常见的X.509证书格式包括：
         * cer/crt是用于存放证书，它是2进制形式存放的，不含私钥。
         * pem跟crt/cer的区别是它以Ascii来表示，可以用于存放证书或私钥。
         */
        // 证书工厂
        CertificateFactory cerFactory = CertificateFactory.getInstance("X.509");
        Certificate cer = cerFactory.generateCertificate(inStream);

        /*
         * Pkcs12也是证书格式 PKCS#12是“个人信息交换语法”。它可以用来将x.509的证书和证书对应的私钥打包，进行交换。
         */
        KeyStore keyStory = KeyStore.getInstance("PKCS12");

        keyStory.load(null, null);

        // 加载证书到密钥库中
        keyStory.setCertificateEntry("tmnt", cer);

        // 密钥管理器
        KeyManagerFactory kMFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kMFactory.init(keyStory, null);
        // 信任管理器
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(keyStory);

        // 初始化sslContext
        sslContext.init(kMFactory.getKeyManagers(), tmFactory.getTrustManagers(), new SecureRandom());
        inStream.close();
        return sslContext;
    }

```
