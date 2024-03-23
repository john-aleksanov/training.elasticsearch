package dev.marvel.elastic.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class EmployeeService {

    private static final String MATCH_QUERY_TEMPLATE = "{\"query\": {\"match\": {\"%s\": \"%s\"}}}";
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

    private final RestClient elasticClient;
    private final ObjectMapper mapper;

    public EmployeeService() {
        this.elasticClient = RestClient.builder(new HttpHost("localhost", 9200, "http"))
            .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Set<Employee> getAll() {
        var request = new Request("POST", "/employees/_search");
        request.addParameter("size", "10000");
        var result = new HashSet<Employee>();
        var jsonResult = performRequest(request);
        var employeesNode = (ArrayNode) jsonResult.get("hits").get("hits");
        for (JsonNode employeeNode : employeesNode) {
            var json = employeeNode.get("_source");
            try {
                var employee = mapper.treeToValue(json, Employee.class);
                employee.setId(employeeNode.get("_id").asText());
                result.add(employee);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public Employee getById(String id) {
        var request = new Request("GET", "/employees/_doc/" + id);
        try {
            var jsonResult = performRequest(request);
            var employeeJson = jsonResult.get("_source");
            var employee = mapper.treeToValue(employeeJson, Employee.class);
            employee.setId(employeeJson.get("_id").asText());
            return employee;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String add(Employee employee) {
        var id = employee.getId();
        var request = (id == null)
            ? new Request("POST", "employees/_doc")
            : new Request("PUT", "employees/_doc/" + id);
        try {
            request.setEntity(new NStringEntity(mapper.writeValueAsString(employee), ContentType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        var jsonResult = performRequest(request);
        return jsonResult.get("_id").asText();
    }

    public void deleteById(String id) {
        var request = new Request("DELETE", "employees/_doc/" + id);
        var jsonResult = performRequest(request);
    }

    public Set<Employee> find(String fieldName, String fieldValue) {
        var matchQuery = String.format(MATCH_QUERY_TEMPLATE, fieldName, fieldValue);
        var request = new Request("POST", "employees/_search");
        request.setEntity(new NStringEntity(matchQuery, ContentType.APPLICATION_JSON));
        var result = new HashSet<Employee>();
        var jsonResult = performRequest(request);
        var employeesNode = (ArrayNode) jsonResult.get("hits").get("hits");
        for (JsonNode employeeNode : employeesNode) {
            var json = employeeNode.get("_source");
            try {
                var employee = mapper.treeToValue(json, Employee.class);
                employee.setId(employeeNode.get("_id").asText());
                result.add(employee);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public Map<String, Double> aggregate(String aggregationField, String metricType, String metricField) {
        var aggQuery = String.format(AGG_QUERY_TEMPLATE, aggregationField, metricType, metricField);
        var request = new Request("POST", "employees/_search");
        request.setEntity(new NStringEntity(aggQuery, ContentType.APPLICATION_JSON));
        var result = new HashMap<String, Double>();
        var jsonResult = performRequest(request);
        var buckets = (ArrayNode) jsonResult.get("aggregations").get("custom").get("buckets");
        for (JsonNode bucket: buckets) {
            var key = bucket.get("key").asText();
            var value = bucket.get("custom").get("value").asDouble();
            result.put(key, value);
        }
        return result;
    }

    private JsonNode performRequest(Request request) {
        try {
            var response = elasticClient.performRequest(request);
            var entity = response.getEntity().getContent().readAllBytes();
            return mapper.readTree(entity);
        } catch (ResponseException e) {
            throw new NotFoundException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
