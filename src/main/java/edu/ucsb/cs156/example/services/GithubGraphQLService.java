package edu.ucsb.cs156.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GithubGraphQLService {

  @Autowired
  private OAuth2AuthorizedClientService clientService;

  private WebClient client =
    WebClient.builder()
      .baseUrl("https://api.github.com/graphql")
      .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
      .build();

  private String getAccessTokenFromAuth(OAuth2AuthenticationToken token) {
    OAuth2AuthorizedClient client =
      clientService.loadAuthorizedClient(
        token.getAuthorizedClientRegistrationId(),
        token.getName()
      );

    return client.getAccessToken().getTokenValue();
  }

  private String getCurrentAccessToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof OAuth2AuthenticationToken)) {
      throw new IllegalStateException(
        "Not logged in, or logged in without an OAuth token. Authentication was %s".formatted(authentication)
      );
    }

    return getAccessTokenFromAuth(((OAuth2AuthenticationToken) authentication));
  }

  public JsonNode executeGraphQLQuery(String query) {
    return executeGraphQLQuery(query, Map.of());
  }

  public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables) {
    String accessToken = getCurrentAccessToken();

    WebClient.ResponseSpec responseSpec = client.post()
      .headers(h -> h.setBearerAuth(accessToken))
      .bodyValue(
        Map.of(
          "query", query,
          "variables", variables
        )
      )
      .retrieve();

    JsonNode response = responseSpec.bodyToMono(JsonNode.class).block();

    return response;
  }
}
