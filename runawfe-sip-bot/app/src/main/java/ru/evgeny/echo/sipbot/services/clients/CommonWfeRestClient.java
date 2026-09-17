package ru.evgeny.echo.sipbot.services.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;


@Slf4j
@Service
public class CommonWfeRestClient {
    private static final String AUTH_BASIC_URL = "/auth/basic";
    private static final String START_PROCESS_URL = "/process/start?name=";

    private final String apiBase;
    private final String login;
    private final String pass;
    private final ObjectMapper mapper;

    private volatile String currentJWT;

    public CommonWfeRestClient(
            @Value("${app.wfe.api.url}") String apiBase,
            @Value("${app.wfe.login}") String login,
            @Value("${app.wfe.pass}") String pass,
            ObjectMapper mapper
    ) {
        this.apiBase = apiBase;
        this.login = login;
        this.pass = pass;
        this.mapper = mapper;
    }

    @SneakyThrows
    private void authenticate() {
        ObjectNode credentials = mapper.createObjectNode()
                .put("login", login)
                .put("password", pass);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiBase + AUTH_BASIC_URL);
            post.setEntity(new StringEntity(
                    mapper.writeValueAsString(credentials),
                    ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)
            ));

            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Ошибка авторизации: " + response.getStatusLine());
            }

            currentJWT = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
        }
    }

    @SneakyThrows
    private int startProcess(String processName, Map<String, Object> variables) {
        String jsonBody = mapper.writeValueAsString(variables);
        String encodedName = java.net.URLEncoder.encode(processName, StandardCharsets.UTF_8);
        String url = apiBase + START_PROCESS_URL + encodedName;

        HttpPut put = new HttpPut(url);
        put.setHeader("Authorization", "Bearer " + currentJWT);
        put.setHeader("Content-Type", "application/json; charset=UTF-8");
        put.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = client.execute(put);
            String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            log.info("Response: {}", content);
            return response.getStatusLine().getStatusCode();
        }
    }


    public void startTaxiProcess(String processName, Map<String, Object> variables) {
        try {
            if (null == currentJWT) {
                authenticate();
            }

            int status = startProcess(processName, variables);
            if (HttpStatus.SC_UNAUTHORIZED == status) {
                authenticate();
                startProcess(processName, variables);
            }
        } catch (Exception e) {
            log.warn("", e);
        }
    }

}
