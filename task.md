# Elasticsearch Practical Tasks

### Prerequisites

1. Elasticsearch of version 8+. [Download](https://www.elastic.co/downloads/elasticsearch)
2. Postman or some other tool to perform http requests to Elasticsearch
3. [Optional] [Elasticsearch Head extension for Chrome](https://chrome.google.com/webstore/detail/multi-elasticsearch-head/cpmmilfkofbeimbmgiclohpodggeheim?hl=en-US)
4. [employees.json](./Content/employees.json) - sample dataset to upload to Elasticsearch

## Indexing and CRUD operations


##### 1. Start Elasticsearch 
##### 2. Create an index and populate it with data from employees.json
##### 3. Perform CRUD operations using Postman, Elasticsearch Head or other Rest client.

[Document API](https://www.elastic.co/guide/en/elasticsearch/reference/8.6/docs.html)
[Bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html)
[Keyword vs Text](https://codecurated.com/blog/elasticsearch-text-vs-keyword/)    
[Search API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html)

    1. Get all records  (you may starts with POST http://localhost:9200/employees/_search to see ids assigned to the records. 
       It was possible to assign ids explicitely while doing bulk upload, but we let Elasticsearch to set ids randomly).

    2. Get an employee by id.

    3. Create a new employee and put in into the index. Let it be

```json
{
  "name": "Ana Brown",
  "dob": "1993-03-19",
  "address": {
    "country": "Belarus",
    "town": "Gomel"
  },
  "email": "anabrown9@gmail.com",
  "skills": [
    "Java",
    "AWS"
  ],
  "experience": 10,
  "rating": 9.2,
  "description": "confident, ambitious, highly motivated Java experience interview learning python",
  "verified": true,
  "salary": 30000
}
```
       Specify id in the url. Query the new employee by its id.
    
    4. Add one more employee and try multi-get request. It may look like: GET http://localhost:9200/employees/_mget {"docs": [{"_id": "1"},{"_id": "2"}]}
    
    5. Learn mapping, automatically created by Elasticsearch. 
       GET http://localhost:9200/employees/_mapping
       Pay attention to string values types. They are both "text" and "keyword". Learn about the difference.

    6. Update one of empoyee data. Add one more skill.
    
    7. Delete one of employees by its id.

## Task 2 - Querying and aggregating data

[Query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
[Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html)
[Metrics aggregation](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics.html)
[Joining queries](https://www.elastic.co/guide/en/elasticsearch/reference/master/joining-queries.html)
[Suggesters](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html)

##### 1. Searching

     1. Get employees with  "description":"motivated" using Match query.
 
     2. Get employees with  "description":"hightly motivated" using Match query. 
        Do you still get Carey Catlett in the response? Why?
     
     3. Run multi_match query to get employees with "python" either in "skills" or in "description" fields.

     4. Get employee with "name" : "stepanie" using Term query. 
        Try the same with "name" : "Stepanie" and  "name" : "Stepanie Spain". Did you get the result? Why?

     5. Get employee with "name.keyword" : "stepanie" using Terms query.
        Try "name.keyword" : "Stepanie Spain". Review the difference between Match and Term queries as well as between text and keyword types search.
     
     6. Get employees by several names using Terms query with array of values. 
      
     7. Get employees with experience from 5 to 20 years using Range query.

     8. Get employees younger then 1,Jan,2000 using Range query.
     
     9. Perform Wildcard search, for example "address.town":{"value": "b*y"}.

     10. Replace it by Regexp to get only "Batley".

     11. Investigate how Fuzzy query works. Make two spelling mistakes in the town, set "fuzziness": "2",  and check you get the town in the response anyway. 

##### 2. Aggregating

     1. Aggregate employees by skills. You shoud use "skills.keyword" here.
      
     2. Filter out employees by adding filter condition to the previous aggregation query.
        Aggregate verified employees only.

     3. Add average metric on "field": "rating" to every bucket from the previous aggregation. 
        You'll need to add nested avg aggregation.

     4. Replace avarage aggregation by stats aggregation. Sort skills buckets by average stats metric DESC.
        "order": {"rating_stats.avg": "desc"}. 

##### 3. Joining

    One to many relationships can be handled using the parent-child method (now called the join operation) in Elasticsearch.
    Consider we have a forum, where anyone can post any topic (say posts). Users can comment on individual posts. 
    So in this scenario, we can consider that the individual posts as the parent documents and the comments to them as their children.
    For this operation, we will have a separate index created, with special mapping (schema) applied.
    
    Create the index with join data type with the below request
```json
PUT post-comments
{
  "mappings": {
    "properties": {
      "_join_type": { 
        "type": "join",
        "relations": {
          "post": "comment" 
        }
      }
    }
  }
}
```
    Let's index some documents for this:
```json
PUT post-comments/_doc/1
{
"_join_type": {
    "name": "post" 
  },
"post_title" : "Angel Has Fallen"
}
```
```json
PUT post-comments/_doc/2
{
"_join_type": {
    "name": "post" 
  },
"post_title" : "Beauty and the beast - a nice movie"
}
```

```json
PUT post-comments/_doc/A?routing=1
{
"_join_type": {
"name": "comment",
"parent": "1"
},
"comment_author": "Neil Soans",
"comment_description": "'Angel has Fallen' has some redeeming qualities, but they're too few and far in between to justify its existence"
}
```
```json
PUT post-comments/_doc/B?routing=1
{
  "_join_type": {
    "name": "comment",
    "parent": "1"
  },
  "comment_author": "Exiled Universe",
  "comment_description": "Best in the trilogy! This movie wasn't better than the Rambo movie but it was very very close."
}
```
```json
PUT post-comments/_doc/C?routing=2
{
"_join_type": {
"name": "comment",
"parent": "2"
},
"comment_author": "Emma Cochrane",
"comment_description": "There's the sublime beauty of a forgotten world and the promise of happily-ever-after to draw you to one of your favourite fairy tales, once again. Give it an encore."
}
```
```json
PUT post-comments/_doc/D?routing=2
{
"_join_type": {
"name": "comment",
"parent": "2"
},
"comment_author": "Common Sense Media Editors",
"comment_description": "Stellar music, brisk storytelling, delightful animation, and compelling characters make this both a great animated feature for kids and a great movie for anyone"
}
```

    1. Query the child’s documents and then returns the parents associated with them as the results. 
       Suppose we need to query for the term “music” in the field “comments_description” in the child documents, 
       and to get the parent documents corresponding to the search results. Use has_child query.

    2. Has_parent query would perform the opposite of the has_child query, that is it will return the child documents of the parent documents that matched the query. 
       Search for the word “Beauty” in the parent document and return the child documents for the matched parents.


##### 4. Suggesting

     Suggests similar looking terms based on a provided text by using a suggester.
     
     1. Run a "suggest" request on "description" field. Provide a text (or a single word) which is supposed to exist in the description field with 1-2 mistakes. 
        See if suggester returns the original text/word. Change number of mistakes and observe the result.



## Task 3 - Implement Java Low Level REST Client for retrieval of employees info

[Java Low Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/java-rest-low.html)


    1. Get all employees.

    2. Get an employee by id.

    3. Create an employee providing id and employee data json.
    
    4. Delete an employee by its id.

    5. Search employees by any field. Field name and field value should be provided as http request parameters.
       Implement the endpoint and try to get all employees with Java skill. 
       Make choise between "Match" and "Term" query.

    6. Perform an aggregation by any numeric field with metric calculation. 
       Aggregation field, metric type and metric field should be provided as http request parameters.


## Task 4 - Implement Java API Client for retrieval of employees info

[Java API Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/introduction.html)

    Perform the same tasks as for Java Low REST Client.