package dev.marvel.elastic.apiclient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final String INDEX_NAME = "employees";
    private static final String AGG_QUERY_TEMPLATE = """
        {
          "size": 0,
          "aggs": {
            "custom": {
              "terms": {
                "field": "%s.keyword"
              },
              "aggs": {
                "custom": {
                  "%s": {
                    "field": "%s"
                  }
                }
              }
            }
          }
        }""";

    private final ElasticsearchClient esClient;

    public EmployeeService() {
        var restClient = RestClient.builder(HttpHost.create("http://localhost:9200"))
            .build();
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));
        this.esClient = new ElasticsearchClient(transport);
    }

    public Set<Employee> getAll() {
        var result = new HashSet<Employee>();
        try {
            var searchResponse = esClient.search(SearchRequest.of(t -> t
                    .index(INDEX_NAME)
                    .size(10_000)
                    .query(q -> q.matchAll(MatchAllQuery.of(m -> m)))),
                Employee.class);
            searchResponse.hits().hits().stream()
                .map(Hit::source)
                .forEach(result::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Employee getById(String id) {
        GetResponse<Employee> response;
        try {
            response = esClient.get(g -> g.index(INDEX_NAME).id(id), Employee.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.found()) {
            return response.source();
        } else {
            throw new NotFoundException();
        }
    }

    public String add(Employee employee) {
        IndexResponse response;
        try {
            response = esClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(employee.getId())
                    .document(employee));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response.id();
    }

    public void deleteById(String id) {
        try {
            esClient.delete(d -> d.index(INDEX_NAME).id(id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Employee> find(String fieldName, String fieldValue) {
        var result = new HashSet<Employee>();
        try {
            var searchResponse = esClient.search(SearchRequest.of(t -> t
                    .index(INDEX_NAME)
                    .size(10_000)
                    .query(q -> q.match(m -> m.field(fieldName).query(fieldValue)))),
                Employee.class);
            searchResponse.hits().hits().stream()
                .map(Hit::source)
                .forEach(result::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Map<String, Double> aggregate(String aggregationField, String metricType, String metricField) {
        var aggQuery = String.format(AGG_QUERY_TEMPLATE, aggregationField, metricType, metricField);
        var aggRequest = SearchRequest.of(b -> b
                .withJson(new StringReader(aggQuery))
                .ignoreUnavailable(true));
        try {
            return esClient.search(aggRequest, Void.class).aggregations().get("custom")
                .sterms().buckets().array().stream()
                .collect(Collectors.toMap(s -> s.key().stringValue(), s -> s.aggregations().get("custom").avg().value()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
