package br.edu.login;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.edu.login.session.MongoSessionRepository;
import java.time.*;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.session.MapSession;

class MongoSessionRepositoryTest {
  @Test
  void roundTripAndRotation() {
    var mongo = mock(MongoTemplate.class);
    var repo = new MongoSessionRepository(mongo);
    var s = repo.createSession();
    s.setAttribute("marker", "value");
    repo.save(s);
    var capt = org.mockito.ArgumentCaptor.forClass(Document.class);
    verify(mongo).save(capt.capture(), eq("sessions"));
    Document d = capt.getValue();
    d.put("attributes", new Binary((byte[]) d.get("attributes")));
    when(mongo.findById(s.getId(), Document.class, "sessions")).thenReturn(d);
    var restored = repo.findById(s.getId());
    assertEquals("value", restored.getAttribute("marker"));
    assertEquals(Duration.ofMinutes(30), restored.getMaxInactiveInterval());
    s.changeSessionId();
    repo.save(s);
    verify(mongo)
        .remove(any(org.springframework.data.mongodb.core.query.Query.class), eq("sessions"));
  }

  @Test
  void expiredSessionIsRejected() {
    var mongo = mock(MongoTemplate.class);
    var repo = new MongoSessionRepository(mongo);
    MapSession s = repo.createSession();
    s.setLastAccessedTime(Instant.now().minus(Duration.ofHours(1)));
    repo.save(s);
    var capt = org.mockito.ArgumentCaptor.forClass(Document.class);
    verify(mongo).save(capt.capture(), eq("sessions"));
    when(mongo.findById(s.getId(), Document.class, "sessions")).thenReturn(capt.getValue());
    assertNull(repo.findById(s.getId()));
    verify(mongo)
        .remove(any(org.springframework.data.mongodb.core.query.Query.class), eq("sessions"));
  }
}
