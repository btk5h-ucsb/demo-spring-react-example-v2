package edu.ucsb.cs156.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
public class GithubService {
  @Autowired
  GithubGraphQLService githubApi;

  public List<String> userOrganizations() {
    JsonNode result = githubApi.executeGraphQLQuery("""
      query {
        viewer {
          organizations(first: 20) {
            edges {
              node {
                name
              }
            }
          }
        }
      }
      """);

    return
      StreamSupport.stream(result.at("/data/viewer/organizations/edges").spliterator(), false)
        .map(n -> n.at("/node/name").asText())
        .toList();
  }

  public String projectId(String org, int number) {
    JsonNode result = githubApi.executeGraphQLQuery("""
      query($org: String! $number: Int!){
        organization(login: $org){
          projectNext(number: $number) {
            id
          }
        }
      }
      """,
      Map.of(
        "org", org,
        "number", number
      ));

    return result.at("/data/organization/projectNext/id").asText();
  }
}
