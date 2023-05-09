package uk.gov.hmcts.reform.ia.casecreator;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@SpringBootApplication
@EnableFeignClients(basePackages =
        {
                "uk.gov.hmcts.reform.ccd",
                "uk.gov.hmcts.reform.idam",
                "uk.gov.hmcts.reform.authorisation",
                "uk.gov.hmcts.reform.ia.casecreator"
        })
@ComponentScan(basePackages = {"uk.gov.hmcts.reform"})
public class Application implements ApplicationRunner {

    @Autowired
    private ArgumentParser argumentParser;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.setProperty("http.proxyHost", "proxyout.reform.hmcts.net");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyHost", "proxyout.reform.hmcts.net");
        System.setProperty("https.proxyPort", "8080");

        argumentParser.parse(args);
    }

    @Bean
    public AuthTokenGenerator serviceAuthTokenGenerator(
            @Value("${idam.s2s-auth.totp_secret}") final String secret,
            @Value("${idam.s2s-auth.microservice}") final String microService,
            final ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, microService, serviceAuthorisationApi);
    }

    @Bean
    public HttpClient serviceTokenParserHttpClient() {
        String proxyHost = System.getProperty("http.proxyHost");
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder = httpClientBuilder.setUserAgent("christest");
        if (proxyHost != null) {
            Integer proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
            httpClientBuilder = httpClientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
        }
        return httpClientBuilder.build();
    }

    @Bean
    public HttpClient userTokenParserHttpClient() {
        return serviceTokenParserHttpClient();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
