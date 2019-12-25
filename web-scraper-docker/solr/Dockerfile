FROM solr:8.1.1

COPY conf/* /opt/solr/

COPY schema/*.json /opt/solr/

COPY run-solr /opt/docker-solr/scripts/

CMD [ "run-solr" ]