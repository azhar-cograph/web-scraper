# Solr Docker Image for Product Search Service

## Build and Run

1. Build image
   ```bash
   docker build -t azh4r/ecsite_products-solr:1.0 .
   ```

1. Run image
   ```bash
   docker run -t --name solr -p 8983:8983 azh4r/ecsite_products-solr:1.0
   ```
