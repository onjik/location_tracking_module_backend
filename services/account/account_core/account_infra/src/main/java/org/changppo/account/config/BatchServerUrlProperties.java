package org.changppo.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "batch")
public class BatchServerUrlProperties {
    private String url;
}
