package com.topcoder.scraper.module.amazon;

import com.gargoylesoftware.htmlunit.WebClient;
import com.topcoder.scraper.Consts;
import com.topcoder.scraper.config.AmazonProperty;
import com.topcoder.scraper.config.MonitorTargetDefinitionProperty;
import com.topcoder.scraper.dao.NormalDataDAO;
import com.topcoder.scraper.model.ProductInfo;
import com.topcoder.scraper.model.PurchaseHistory;
import com.topcoder.scraper.module.ChangeDetectionInitModule;
import com.topcoder.scraper.module.amazon.crawler.*;
import com.topcoder.scraper.repository.NormalDataRepository;
import com.topcoder.scraper.service.WebpageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Amazon implementation for ChangeDetectionInitModule
 */
@Component
public class AmazonChangeDetectionInitModule extends ChangeDetectionInitModule {

  private static Logger LOGGER = LoggerFactory.getLogger(AmazonChangeDetectionInitModule.class);

  private final AmazonProperty property;
  private final MonitorTargetDefinitionProperty monitorTargetDefinitionProperty;
  private final WebClient webClient;
  private final WebpageService webpageService;
  private final NormalDataRepository repository;

  @Autowired
  public AmazonChangeDetectionInitModule(
          AmazonProperty property,
          MonitorTargetDefinitionProperty monitorTargetDefinitionProperty,
          WebClient webClient,
          WebpageService webpageService,
          NormalDataRepository repository
  ) {
    this.property = property;
    this.monitorTargetDefinitionProperty = monitorTargetDefinitionProperty;
    this.webClient = webClient;
    this.webpageService = webpageService;
    this.repository = repository;
  }

  @Override
  public String getECName() {
    return "amazon";
  }

  /**
   * Implementation of init method
   */
  @Override
  public void init() throws IOException {
    for (MonitorTargetDefinitionProperty.MonitorTargetCheckPage monitorTargetCheckPage : monitorTargetDefinitionProperty.getCheckPages()) {
      if (monitorTargetCheckPage.getPageName().equalsIgnoreCase(Consts.PURCHASE_HISTORY_LIST_PAGE_NAME)) {
        List<String> usernameList = monitorTargetCheckPage.getCheckTargetKeys();

        String passwordListString = System.getenv(Consts.AMAZON_CHECK_TARGET_KEYS_PASSWORDS);
        if (passwordListString == null) {
          LOGGER.error("Please set environment variable AMAZON_CHECK_TARGET_KEYS_PASSWORDS first");
          throw new RuntimeException("environment variable AMAZON_CHECK_TARGET_KEYS_PASSWORDS not set");
        }
        List<String> passwordList = Arrays.asList(passwordListString.split(","));

        for (int i = 0; i < usernameList.size(); i++) {
          String username = usernameList.get(i);
          String password = passwordList.get(i);

          LOGGER.info("init ...");
          AmazonAuthenticationCrawler authenticationCrawler = new AmazonAuthenticationCrawler(getECName(), property, webpageService);
          AmazonAuthenticationCrawlerResult loginResult = authenticationCrawler.authenticate(webClient, username, password);
          if (!loginResult.isSuccess()) {
            LOGGER.error(String.format("Failed to login %s with username %s. Skip.", getECName(), username));
            continue;
          }

          AmazonPurchaseHistoryListCrawler purchaseHistoryListCrawler = new AmazonPurchaseHistoryListCrawler(getECName(), property, webpageService);
          AmazonPurchaseHistoryListCrawlerResult crawlerResult = purchaseHistoryListCrawler.fetchPurchaseHistoryList(webClient, null, false);
          processPurchaseHistory(crawlerResult, username);
        }

      } else if (monitorTargetCheckPage.getPageName().equalsIgnoreCase(Consts.PRODUCT_DETAIL_PAGE_NAME)) {
        AmazonProductDetailCrawler crawler = new AmazonProductDetailCrawler(getECName(), property, webpageService);
        for (String productCode : monitorTargetCheckPage.getCheckTargetKeys()) {
          AmazonProductDetailCrawlerResult crawlerResult = crawler.fetchProductInfo(webClient, productCode, false);
          processProductInfo(crawlerResult);
        }

      } else {
        throw new RuntimeException("Unknown monitor target definition " + monitorTargetCheckPage.getPageName());
      }

    }

  }

  /**
   * Save normal data in database
   * @param normalData normal data as string
   * @param page the page name
   * @param pageKey the page key
   */
  private void saveNormalData(String normalData, String pageKey, String page) {
    NormalDataDAO dao = repository.findFirstByEcSiteAndPageAndPageKey(getECName(), page, pageKey);
    if (dao == null) {
      dao = new NormalDataDAO();
    }

    dao.setEcSite(getECName());
    dao.setNormalData(normalData);
    dao.setDownloadedAt(new Date());
    dao.setPage(page);
    dao.setPageKey(pageKey);
    repository.save(dao);
  }

  /**
   * process purchase history crawler result
   * @param crawlerResult the crawler result
   * @param pageKey the page key
   */
  private void processPurchaseHistory(AmazonPurchaseHistoryListCrawlerResult crawlerResult, String pageKey) {
    List<PurchaseHistory> purchaseHistoryList = crawlerResult.getPurchaseHistoryList();
    saveNormalData(PurchaseHistory.toArrayJson(purchaseHistoryList), pageKey, Consts.PURCHASE_HISTORY_LIST_PAGE_NAME);
  }

  /**
   * process product info crawler result
   * @param crawlerResult the crawler result
   */
  private void processProductInfo(AmazonProductDetailCrawlerResult crawlerResult) {
    ProductInfo productInfo = crawlerResult.getProductInfo();
    saveNormalData(productInfo.toJson(), productInfo.getCode(), Consts.PRODUCT_DETAIL_PAGE_NAME);
  }
}
