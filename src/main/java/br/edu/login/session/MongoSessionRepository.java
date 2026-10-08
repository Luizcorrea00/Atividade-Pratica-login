package br.edu.login.session;

import jakarta.annotation.PostConstruct;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.session.*;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
public class MongoSessionRepository implements SessionRepository<MapSession> {
  private final MongoTemplate mongo;

  public MongoSessionRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @PostConstruct
  void index() {
    mongo
        .indexOps("sessions")
        .ensureIndex(
            new Index()
                .on("expiresAt", org.springframework.data.domain.Sort.Direction.ASC)
                .expire(Duration.ZERO));
  }

  public MapSession createSession() {
    MapSession s = new MapSession();
    s.setMaxInactiveInterval(Duration.ofMinutes(30));
    return s;
  }

  public void save(MapSession s) {
    if (!s.getOriginalId().equals(s.getId())) deleteById(s.getOriginalId());
    Map<String, Object> attrs = new HashMap<>();
    for (String key : s.getAttributeNames()) attrs.put(key, s.getAttribute(key));
    Document d =
        new Document("_id", s.getId())
            .append("created", Date.from(s.getCreationTime()))
            .append("accessed", Date.from(s.getLastAccessedTime()))
            .append("timeout", s.getMaxInactiveInterval().getSeconds())
            .append(
                "expiresAt", Date.from(s.getLastAccessedTime().plus(s.getMaxInactiveInterval())))
            .append("attributes", SerializationUtils.serialize(attrs));
    mongo.save(d, "sessions");
  }

  @SuppressWarnings("unchecked")
  public MapSession findById(String id) {
    Document d = mongo.findById(id, Document.class, "sessions");
    if (d == null) return null;
    MapSession s = new MapSession(id);
    s.setCreationTime(d.getDate("created").toInstant());
    s.setLastAccessedTime(d.getDate("accessed").toInstant());
    s.setMaxInactiveInterval(Duration.ofSeconds(((Number) d.get("timeout")).longValue()));
    if (s.isExpired()) {
      deleteById(id);
      return null;
    }
    byte[] bytes = ((org.bson.types.Binary) d.get("attributes")).getData();
    ((Map<String, Object>) SerializationUtils.deserialize(bytes)).forEach(s::setAttribute);
    return s;
  }

  public void deleteById(String id) {
    mongo.remove(Query.query(Criteria.where("_id").is(id)), "sessions");
  }
}
