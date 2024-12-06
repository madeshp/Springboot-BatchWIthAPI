// application.yml additions
ssl:
  key-store: classpath:keystore.p12
  key-store-password: your-keystore-password
  key-store-type: PKCS12
  key-alias: client
  trust-store: classpath:truststore.jks
  trust-store-password: your-truststore-password
  trust-store-type: JKS

// SSLConfiguration.java
@Configuration
@Slf4j
public class SSLConfiguration {

    @Value("${ssl.key-store}")
    private Resource keyStore;

    @Value("${ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${ssl.key-store-type}")
    private String keyStoreType;

    @Value("${ssl.key-alias}")
    private String keyAlias;

    @Value("${ssl.trust-store}")
    private Resource trustStore;

    @Value("${ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${ssl.trust-store-type}")
    private String trustStoreType;

    @Bean
    public WebClient webClient(@Value("${api.base-url}") String baseUrl) throws Exception {
        SslContext sslContext = createSslContext();
        
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private SslContext createSslContext() throws Exception {
        // Load client certificate and private key
        KeyStore clientKeyStore = KeyStore.getInstance(keyStoreType);
        clientKeyStore.load(keyStore.getInputStream(), keyStorePassword.toCharArray());

        // Load trusted certificates
        KeyStore trustKeyStore = KeyStore.getInstance(trustStoreType);
        trustKeyStore.load(trustStore.getInputStream(), trustStorePassword.toCharArray());

        // Set up key manager factory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, keyStorePassword.toCharArray());

        // Set up trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustKeyStore);

        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                null
        );

        return SslContextBuilder
                .forClient()
                .trustManager(trustManagerFactory)
                .keyManager(
                        keyManagerFactory.getKeyManagers()[0]
                )
                .build();
    }
}

// Enhanced ApiProcessor with error handling
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiProcessor implements ItemProcessor<InputData, ProcessedResult> {
    private final WebClient webClient;
    private final int MAX_RETRIES = 3;
    private final long RETRY_DELAY_MS = 1000;

    @Override
    public ProcessedResult process(InputData item) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                ApiResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/endpoint")
                        .queryParam("startDate", item.getStartDate())
                        .queryParam("endDate", item.getEndDate())
                        .queryParam("vtype", item.getVtype())
                        .build())
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                        log.error("Client error when processing vtype: {}", item.getVtype());
                        return Mono.error(new RuntimeException("Client error: " + clientResponse.statusCode()));
                    })
                    .onStatus(HttpStatus::is5xxServerError, serverResponse -> {
                        log.error("Server error when processing vtype: {}", item.getVtype());
                        return Mono.error(new RuntimeException("Server error: " + serverResponse.statusCode()));
                    })
                    .bodyToMono(ApiResponse.class)
                    .block();

                return new ProcessedResult(item.getVtype(), response.getCount());
            } catch (WebClientRequestException e) {
                log.error("SSL/TLS error for vtype {}: {}", item.getVtype(), e.getMessage());
                if (++retryCount >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to process after " + MAX_RETRIES + " retries", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            } catch (Exception e) {
                log.error("Error processing vtype {}: {}", item.getVtype(), e.getMessage());
                throw new RuntimeException("Failed to process vtype: " + item.getVtype(), e);
            }
        }
        throw new RuntimeException("Failed to process after " + MAX_RETRIES + " retries");
    }
}
