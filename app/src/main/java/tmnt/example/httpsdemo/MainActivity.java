package tmnt.example.httpsdemo;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tmnt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getHttp();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void getHttp() throws IOException {
        URL url = null;
        try {
            url = new URL("https://github.com/tmntduke");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(5000);
        Log.e(TAG, "https: " + (connection instanceof HttpsURLConnection));
        if (connection instanceof HttpsURLConnection) {
            SSLContext context = null;
            try {
                context = getSSLContextWithCer(getApplicationContext());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
            if (context != null) {
                SSLSocketFactory factory = context.getSocketFactory();
                ((HttpsURLConnection) connection).setDefaultSSLSocketFactory(factory);
                ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);

            }
        }

        int code = connection.getResponseCode();
        Log.e(TAG, "responseCode: " + code);
        if (code == 200) {
            InputStream is = connection.getInputStream();
            Log.e(TAG, "is: " + is.toString());
            is.close();
        }
    }

    /**
     * 验证主机名
     */
    public HostnameVerifier hostnameVerifier = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            // TODO Auto-generated method stub
            return true;
        }
    };

    /**
     * 有证书的SSLContext
     *
     * @param context
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
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
//       keyStory.load(inStream, "123456".toCharArray());
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

    /**
     * 没有证书的SSLCotext
     *
     * @param context
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private SSLContext getSSLContextWithoutCer(Context context) throws NoSuchAlgorithmException, KeyManagementException {

        //实例化SSLContext
        SSLContext sslContext = SSLContext.getInstance("SSL");

        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());

        return sslContext;
    }

}
