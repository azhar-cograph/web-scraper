package com.topcoder.scraper.module.ecisolatedmodule.amazon.crawler;

import com.topcoder.common.model.CodeType;
import com.topcoder.scraper.module.ecisolatedmodule.crawler.AbstractAuthenticationCrawlerResult;
import lombok.Data;

/**
 * Result from AmazonAuthenticationCrawler
 */
public class AmazonAuthenticationCrawlerResult extends AbstractAuthenticationCrawlerResult {

  public AmazonAuthenticationCrawlerResult(boolean success, String htmlPath) {
    super(success, htmlPath);
  }

  public AmazonAuthenticationCrawlerResult(boolean success, String reason, CodeType codeType, String img, boolean needContinue) {
    super(success, reason, codeType, img, needContinue);
  }
}
