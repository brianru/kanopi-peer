FROM clojure:onbuild
MAINTAINER Brian James Rubinton <brian@kanopi.io>

# NOTE: prefix in below env var value comes from alias used to link
# transactor container to kanopi-peer container
#ENV DATOMIC_TRANSACTOR_URI $TRANSACTOR_ENV_DATOMIC_TRANSACTOR_URI

EXPOSE 8080

RUN ["lein", "cljsbuild", "once"]

CMD ["lein", "run"]
