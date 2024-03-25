### Task

See [task.md](task.md).

### Runbook

1. Prerequisites:
    1. Docker
    2. curl
2. Run

```shell
docker compose up
```

in the current working directory to run Elasticsearch locally in a Docker container. It will be available at http://localhost:9200. Note the
use of `xpack.security.enabled: false` in [compose.yml](compose.yml). This turns off x-pack security features (such as authentication and
TLS) and should NEVER be used in a production environment.

### Searching

[Search API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html)

#### Check Elasticsearch cluster status

```shell
curl http://localhost:9200/_cluster/health
```

#### Create an employees index and populate it

```shell
curl -X PUT -H "Content-Type: application/x-ndjson" --data-binary @data/employees.json http://localhost:9200/employees/_bulk
```

Populates the index with the contents of the ./data/employees.json

#### Get all records in the employees index.

```shell
curl -X POST http://localhost:9200/employees/_search?size=100
```

Size parameter to get all documents rather than first ten by default.

#### Create a new employee.

```shell
curl -X POST -H "Content-Type: application/json" -d @data/new-employee.json http://localhost:9200/employees/_doc/123
```

We specify an ID here to be able to query it by ID later. Otherwise, Elasticsearch assigns a random ID.

#### Query the new employee by ID

```shell
curl http://localhost:9200/employees/_doc/123
```

#### Add same employee under a different ID

```shell
curl -X POST -H "Content-Type: application/json" -d @data/new-employee.json http://localhost:9200/employees/_doc/234
```

#### Multi-get

```shell
curl -X POST -H 'Content-Type: application/json' -d'{"docs": [{"_id": "123"},{"_id": "234"}]}' http://localhost:9200/employees/_mget
```

Gets employees with IDs '123' and '234'.

#### Review the mapping automatically created by Elasticsearch.

```shell
curl http://localhost:9200/employees/_mapping
```

Note that text fields have a 'keyword' subfield, so they can be accessed as both 'text' for full text search and 'keyword' for exact matches
in filtering, sorting, and aggregation

#### Update employee

```shell
curl -X POST -H "Content-Type: application/json" -d '{"script":{"source":"ctx._source.skills.add(params.skill)","lang":"painless","params":{"skill":"Python"}}}' http://localhost:9200/employees/_update/123
```

Updates employee with ID '123' and add the 'Python' skill

#### Delete employee with ID '123'

```shell
curl -X DELETE http://localhost:9200/employees/_doc/123
```

#### Multi-delete

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"terms": {"_id": ["123", "234"]}}}' http://localhost:9200/employees/_delete_by_query
```

### Querying and aggregating data

[Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)

#### Match query

```shell
curl -X GET -H "Content-Type: application/json" -d '{"query": {"match": {"description": "motivated"}}}' http://localhost:9200/employees/_search
```

Gets the 'Carey Catlett' employee, who happens to have the word 'motivated' in the description field.

#### Another match query

```shell
curl -X GET -H "Content-Type: application/json" -d '{"query": {"match": {"description": "highly motivated"}}}' http://localhost:9200/employees/_search
```

We still get the same employee although she only has 'motivated' in her description. It's because by default Elastic uses the OR condition
between the two words in a Match condition. To search for 'highly' AND 'motivated', we need to restructure the query:

```shell
curl -H "Content-Type: application/json" -d '{"query": {"bool": {"must": [{"match": {"description": "highly"}},{"match": {"description": "motivated"}}]}}}' http://localhost:9200/employees/_search
```

This query returns an empty hits response - there isn't any 'highly motivated' employee, alas.

#### Multi-match

```shell
curl -H "Content-Type: application/json" -d '{"query": {"bool": {"should": [{"multi_match": {"query": "Python","fields": ["skills", "description"]}}]}}}' http://localhost:9200/employees/_search
```

This query returns all employees that have 'Python' either in skills or description.

### Term query

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"term": {"name": "Stepanie Spain"}}}' http://localhost:9200/employees/_search
```

This query returns an empty hits response although there is an employee called 'Stepanie Spain'. Why? This is due to the way Elasticsearch
analyzes and indexes text fields by default. Elasticsearch uses analyzers to preprocess text before indexing, which includes steps like
lowercasing, tokenization, and stemming. When you perform a term query, Elasticsearch searches for the exact term as it is stored in the
inverted index. If the text has been analyzed during indexing, the term you're searching for might not match exactly. When inserting
Stepanie Spain in the index, Elasticsearch tokenized and lowercased the name, hence the inverted index has two entries: 'stepanie' and
'spain', both linking to her in the index. So the following two queries successfully return the correct employee:

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"term": {"name": "stepanie"}}}' http://localhost:9200/employees/_search
```

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"term": {"name": "spain"}}}' http://localhost:9200/employees/_search
```

Another option would be to search using keyword search:

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"term": {"name.keyword": "Stepanie Spain"}}}' http://localhost:9200/employees/_search
```

This search successfully returns the correct employee. Elasticsearch provides a special type of field called a "keyword" field. These fields
are not analyzed and store the exact value as it is. They are useful for scenarios where you need to perform exact matches or sorting on the
original, unanalyzed value.

#### Multi-get terms query

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"terms": {"name": ["stepanie", "brandon"]}}}' http://localhost:9200/employees/_search
```

This query returns all employees that have "stepanie" or "brandon" in their name (case-insensitive).

#### Range query

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"range": {"experience": {"gte": 10, "lte": 20}}}}' http://localhost:9200/employees/_search
```

This query returns all employees with experience greater than or equal to ten years and less than or equal to 20 years.

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"range": {"dob": {"gte": "2000-01-01"}}}}' http://localhost:9200/employees/_search
```

This query returns all employees younger than 1 Jan 2000.

#### Wildcard and regex search

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"wildcard": {"address.town":{"value": "b*y"}}}}' http://localhost:9200/employees/_search
```

This wildcard query returns two employees whose towns (Birtley and Batley) start with 'B' and end in 'Y'.

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"regexp": {"address.town":{"value": "ba.*y"}}}}' http://localhost:9200/employees/_search
```

This regexp query returns only 'Batley' residents - the single Alice Creed.

#### Fuzzy search

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"fuzzy": {"address.town":{"value": "Badly", "fuzziness": 2}}}}' http://localhost:9200/employees/_search
```

This fuzzy query with a fuzziness of two returns Alice Creed even though we made two spelling mistakes in the city and called it 'Badly'
instead of 'Batley'.

### Aggregations

#### Simple aggregation

```shell
curl -X POST -H "Content-Type: application/json" -d '{"aggs": {"marvels-agg": {"terms": {"field": "skills.keyword"}}}}' http://localhost:9200/employees/_search
```

This query aggregates employees by skill.

#### Aggregation with filtering

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"term": {"verified": true}}, "aggs": {"skills": {"terms": {"field": "skills.keyword"}}}}' http://localhost:9200/employees/_search
```

This query aggregates employees by skill but filters out non-verified employees.

```shell
curl -X GET -H 'Content-Type: application/json' -d'{"aggs": {"skills": {"terms": {"field": "skills.keyword"}, "aggs": {"average_rating": {"avg": {"field": "rating"}}}}}}' http://localhost:9200/employees/_search?size=0
```

This query calculates the average rating for each bucket from the previous aggregation. We add `?size=0` to only get the statistics, not
employees themselves.

#### Stats aggregation

```shell
curl -X GET -H 'Content-Type: application/json' -d'{"aggs": {"skills": {"terms": {"field": "skills.keyword", "order": {"rating_stats.avg": "desc"}}, "aggs": {"rating_stats": {"stats": {"field": "rating"}}}}}}' http://localhost:9200/employees/_search?size=0
```

This query calculates statistics for each bucket and then orders the result by average rating in the descending order.

### Joins

First let's add a mapping that defines the relationship between posts and comments:

```shell
curl -X PUT -H "Content-Type: application/json" --data-binary @data/joins-mapping.json http://localhost:9200/post-comments
```

Now, let's index several posts and comments:

```shell
curl -X PUT -H "Content-Type: application/x-ndjson" --data-binary @data/joins-data.json http://localhost:9200/post-comments/_bulk
```

#### Query children and return parent

Suppose we need to query for the term “music” in the field “comment_description” in the child documents, and to get the parent documents
corresponding to the search results:

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"has_child": {"type": "comment", "query": {"match": {"comment_description": "music"}}}}}' http://localhost:9200/post-comments/_search
```

We are getting a single result - a post with the title "Beauty and the beast - a nice movie". Indeed, it has a comment "Stellar music, brisk
storytelling, delightful animation, and compelling characters make this both a great animated feature for kids and a great movie for
anyone".

#### Query parents and return children

Now suppose we need to query for the word ”Beauty" in the parent documents, and return all child documents corresponding to the search
results:

```shell
curl -X POST -H "Content-Type: application/json" -d '{"query": {"has_parent": {"parent_type": "post", "query": {"match": {"post_title": "beauty"}}}}}' http://localhost:9200/post-comments/_search
```

We are getting two results - the two comments for the "Beauty and the Beast - a nice movie" post.

### Suggesting

The suggest feature suggests similar looking terms based on a provided text by using a suggester. The suggest request part is defined
alongside the query part in a _search request. If the query part is left out, only suggestions are returned.

Let's ask Elasticsearch to suggest terms based on "Beaooty and the Beest":

```shell
curl -X POST -H "Content-Type: application/json" -d '{"suggest": {"text": "beaooty and the beest", "b-and-b-suggest": {"phrase": {"field": "post_title", "size": 3}}}}' http://localhost:9200/post-comments/_search
```

Elasticsearch suggests three phrases: "beauty and the beast", "beaooty and the beast", and "beauty and the beest".

### REST client

#### Overview

The `rest-client` Gradle sub-project showcases the usage of the low-level Elasticsearch REST client. The project is a very basic Spring Boot
3 project that exposes several HTTP endpoints to perform operations on employees against the local Elasticsearch cluster spun up using
[compose.yml](./compose.yml):

1. Get all employees.
2. Get an employee by ID.
3. Search for employees having a specific text in a specific field.
4. Calculate an aggregate statistic based on a specified field and group by another field.
5. Add an employee to the index.
6. Delete an employee by ID.

The application also exposes Swagger at `http://localhost:8080/swagger-ui/index.html`.

#### How to run

Just build and run in your favorite IDE.

#### Notes & Limitations

This project serves as a basic example to demonstrate some Elasticsearch capabilities. Due to its simplicity, several shortcuts have been
taken and certain hardcodings have been implemented. In a production setting, the following improvements would be made:

1. Testing: Currently, no tests are available. A TDD approach would be adopted.
2. Observability: Logging and performance metrics would be added.
3. Exception handling: Currently, only happy path is supported and has been verified to work correctly. A robust exception handling
   mechanism needs to be implemented for paths of sorrow / edge cases including segregating exceptions into domain-specific and platform /
   infrastructure.
4. Architecture: Domain-Driven Design (DDD) would be used for better decoupling, and an expressive package structure would be implemented.
5. The `EmployeeController` class is a very shallow wrapper / proxy over `EmployeeService`. In a production setting, it would have more
   responsibilities and wouldn't be as shallow.
6. The `EmployeeService` class has certain code duplications relating to JSON operations. These would be reworked and extracted to a
   separate infrastructure layer / ES client wrapper that would consume raw ES responses and transform them into domain objects.
7. Etc., etc.

### API client

#### Overview

The `api-client` Gradle sub-project showcases the usage of the high-level Elasticsearch API client. The project is almost identical to
`rest-client` except for the `EmployeeService` class, which uses the Elasticsearch API client. The project exposes the same endpoints, and
the same notes and limitations apply.

One additional limitation is that, although the aggregation endpoint receives the aggregation statistic as a request parameter, only 
the `avg` statistic is supported. This is because working with the API client involves using Elasticsearch API objects instead of parsing
JSON responses. Those API objects, when it comes to aggregations, don't lend themselves to polymorphic treatment and need to be handled
separately. Also, the aim of this project is to provide just an example of how one works with the ES API client rather than a comprehensive
solution.