package org.bootcamp.customerservice.infrastructure.mongo.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDocument {
  @Id
  private String id;
  @Indexed(unique = true)
  private String customerId;
  private String documentNumber;
  private String fullName;
  private String type;
  private String profile;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
