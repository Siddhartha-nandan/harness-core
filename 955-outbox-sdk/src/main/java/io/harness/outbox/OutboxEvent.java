package io.harness.outbox;

import io.harness.context.GlobalContext;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "OutboxEventKeys")
@Entity(value = "outboxEvents", noClassnameStored = true)
@Document("outboxEvents")
@TypeAlias("outboxEvents")
public class OutboxEvent implements PersistentIterable, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull ResourceScope resourceScope;
  @NotNull @Valid Resource resource;

  @NotNull String eventType;
  @NotNull String eventData;

  @CreatedDate Long createdAt;
  @Setter @Builder.Default Long attempts = 0L;
  @Setter @Builder.Default Boolean blocked = Boolean.FALSE;

  GlobalContext globalContext;

  Long nextIteration;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("blocked_createdAt_nextIteration_outbox_Idx")
                 .field(OutboxEventKeys.blocked)
                 .field(OutboxEventKeys.createdAt)
                 .field(OutboxEventKeys.nextIteration)
                 .build())
        .build();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return this.nextIteration;
  }

  @Override
  public String getUuid() {
    return this.id;
  }
}
