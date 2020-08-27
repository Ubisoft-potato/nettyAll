package org.cyka.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class ServiceEndpoint {
  private String host;
  private Integer port;
}
