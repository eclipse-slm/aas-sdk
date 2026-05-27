package org.eclipse.slm.aas.clients.base;

import feign.Client;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.slm.aas.clients.auth.AuthRequestInterceptor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class FeignClientFactory {

    public static <T> T createClient(Class<T> clientClass, String baseUrl, AuthRequestInterceptor authRequestInterceptor) {
        return createClient(clientClass, baseUrl, authRequestInterceptor, false);
    }

    public static <T> T createClient(Class<T> clientClass,
                                     String baseUrl,
                                     AuthRequestInterceptor authRequestInterceptor,
                                     boolean disableSslCertificateValidation) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null or empty");
        }

        Decoder decoder = (response, type) -> {
            String body = "";
            try {
                body = new String(response.body().asInputStream().readAllBytes());
                return new JsonDeserializer().read(body, (Class<?>) type);
            } catch (IOException | DeserializationException e) {
                if (e instanceof DeserializationException) {
                    throw new RuntimeException("DeserializationException: " + e.getMessage() + ", Response Body: " + body, e);
                }
                throw new RuntimeException();
            }
        };

        Encoder encoder = (object, bodyType, template) -> {
            try {
                var jsonSerializer = new CustomAasJsonSerializer();
                var json = jsonSerializer.write(object);
                template.body(json);
            } catch (org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException e) {
                throw new RuntimeException(e);
            }
        };

        var apiClientBuilder = feign.Feign.builder()
                .decoder(decoder)
                .encoder(encoder)
                .errorDecoder(new ResponseErrorDecoder());

        if (disableSslCertificateValidation) {
            apiClientBuilder.client(createInsecureClient());
        }

        if (authRequestInterceptor != null) {
            apiClientBuilder.requestInterceptor(authRequestInterceptor);
        }

        return apiClientBuilder.target(clientClass, baseUrl);
    }

    private static Client createInsecureClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HostnameVerifier hostnameVerifier = (hostname, session) -> true;

            return new Client.Default(sslSocketFactory, hostnameVerifier);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize insecure SSL client", e);
        }
    }

}
