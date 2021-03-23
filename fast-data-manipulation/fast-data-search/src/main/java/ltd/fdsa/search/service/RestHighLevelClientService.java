package ltd.fdsa.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ltd.fdsa.search.model.entity.ElasticSearchDoc;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class RestHighLevelClientService {

  @Autowired private RestHighLevelClient client;

  @Autowired private ObjectMapper mapper;

  /**
   * 创建索引
   *
   * @param indexName
   * @param settings
   * @param mapping
   * @return
   * @throws IOException
   */
  public CreateIndexResponse createIndex(String indexName, String settings, String mapping)
      throws IOException {
    CreateIndexRequest request = new CreateIndexRequest(indexName);
    if (null != settings && !"".equals(settings)) {
      request.settings(settings, XContentType.JSON);
    }
    if (null != mapping && !"".equals(mapping)) {
      request.mapping(mapping, XContentType.JSON);
    }
    return client.indices().create(request, RequestOptions.DEFAULT);
  }

  /**
   * 删除索引
   *
   * @param indexNames
   * @return
   * @throws IOException
   */
  public AcknowledgedResponse deleteIndex(String... indexNames) throws IOException {
    DeleteIndexRequest request = new DeleteIndexRequest(indexNames);
    return client.indices().delete(request, RequestOptions.DEFAULT);
  }

  /**
   * 判断 index 是否存在
   *
   * @param indexName
   * @return
   * @throws IOException
   */
  public boolean indexExists(String indexName) throws IOException {
    GetIndexRequest request = new GetIndexRequest(indexName);
    return client.indices().exists(request, RequestOptions.DEFAULT);
  }

  /**
   * 简单模糊匹配 默认分页为 0,10
   *
   * @param field
   * @param key
   * @param page
   * @param size
   * @param indexNames
   * @return
   * @throws IOException
   */
  public SearchResponse search(String field, String key, int page, int size, String... indexNames)
      throws IOException {
    SearchRequest request = new SearchRequest(indexNames);
    SearchSourceBuilder builder = new SearchSourceBuilder();
    builder.query(new MatchQueryBuilder(field, key)).from(page).size(size);
    request.source(builder);
    return client.search(request, RequestOptions.DEFAULT);
  }

  /**
   * 简单模糊匹配 默认分页为 0,10
   *
   * @param keyword
   * @param type
   * @param source
   * @return
   * @throws IOException
   */
  public SearchResponse search(String keyword, String type, String source, int page, int size)
      throws IOException {

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    //    HighlightBuilder highlightBuilder = new HighlightBuilder();
    //    highlightBuilder.field("summary").field("title").preTags("<em>").postTags("</em>");
    //    searchSourceBuilder.highlighter(highlightBuilder);
    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
    if (!Strings.isNullOrEmpty(type)) {
      String[] types = type.split(",");
      queryBuilder = queryBuilder.should(QueryBuilders.multiMatchQuery("type", types));
    }
    if (!Strings.isNullOrEmpty(source)) {
      String[] sources = source.split(",");
      queryBuilder = queryBuilder.should(QueryBuilders.multiMatchQuery("source", sources));
    }
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("title", keyword).analyzer("ik_smart"));
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("title.pinyin", keyword).analyzer("pinyin"));
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("summary", keyword).analyzer("ik_smart"));
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("summary.pinyin", keyword).analyzer("pinyin"));
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("content", keyword).analyzer("ik_smart"));
    queryBuilder =
        queryBuilder.should(QueryBuilders.matchQuery("content.pinyin", keyword).analyzer("pinyin"));

    searchSourceBuilder.query(queryBuilder);

    SearchRequest request = new SearchRequest("resources");
    searchSourceBuilder.from(page - 1).size(size);
    System.out.println(searchSourceBuilder.toString());
    request.source(searchSourceBuilder);
    return client.search(request, RequestOptions.DEFAULT);
  }

  public SearchResponse getMetaData(String field, String doc) throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(field);
    aggregationBuilder.field(field);
    searchSourceBuilder.aggregation(aggregationBuilder);
    SearchRequest request = new SearchRequest(doc);
    System.out.println(searchSourceBuilder.toString());
    request.source(searchSourceBuilder);
    return client.search(request, RequestOptions.DEFAULT);
  }

  /**
   * term 查询 精准匹配
   *
   * @param field
   * @param key
   * @param page
   * @param size
   * @param indexNames
   * @return
   * @throws IOException
   */
  public SearchResponse termSearch(
      String field, String key, int page, int size, String... indexNames) throws IOException {
    SearchRequest request = new SearchRequest(indexNames);
    SearchSourceBuilder builder = new SearchSourceBuilder();
    builder.query(QueryBuilders.termsQuery(field, key)).from(page).size(size);
    request.source(builder);
    return client.search(request, RequestOptions.DEFAULT);
  }

  /**
   * 批量导入
   *
   * @param indexName
   * @param isAutoId 使用自动id 还是使用传入对象的id
   * @param source
   * @return
   * @throws IOException
   */
  public BulkResponse importAll(String indexName, boolean isAutoId, String source)
      throws IOException {
    if (0 == source.length()) {
      // todo 抛出异常 导入数据为空
    }
    BulkRequest request = new BulkRequest();
    JsonNode jsonNode = mapper.readTree(source);

    if (jsonNode.isArray()) {
      for (JsonNode node : jsonNode) {
        if (isAutoId) {
          request.add(new IndexRequest(indexName).source(node.toString(), XContentType.JSON));
        } else {
          request.add(
              new IndexRequest(indexName)
                  .id(node.get("id").asText())
                  .source(node.asText(), XContentType.JSON));
        }
      }
    }
    return client.bulk(request, RequestOptions.DEFAULT);
  }

  SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * 批量导入
   *
   * @param indexName
   * @param sources
   * @return
   * @throws IOException
   */
  public boolean importAll(String indexName, ElasticSearchDoc... sources) {
    if (sources.length <= 0) {
      return false;
    }
    BulkRequest request = new BulkRequest();

    for (ElasticSearchDoc item : sources) {
      String type = item.getType();
      if (Strings.isNullOrEmpty(type)) {
        type = "";
      }
      Date datetime = item.getDatetime();
      if (datetime == null) {
        datetime = new Date();
      }
      String source = item.getSource();
      if (Strings.isNullOrEmpty(source)) {
        source = "";
      }
      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("type", type);
      jsonMap.put("title", item.getTitle());
      jsonMap.put("summary", item.getSummary());
      jsonMap.put("content", item.getContent());
      jsonMap.put("url", item.getUrl());
      jsonMap.put("source", source);
      jsonMap.put("datetime", f.format(datetime));
      IndexRequest indexRequest = new IndexRequest(indexName).id(item.getId()).source(jsonMap);
      try {
        IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println(response.toString());
      } catch (IOException e) {
        e.printStackTrace();
      }
      request.add(indexRequest);
    }
    BulkResponse bulkResponse = null;
    try {

      bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      return false;
    }

    System.out.println(bulkResponse.toString());
    return !bulkResponse.hasFailures();
  }
}