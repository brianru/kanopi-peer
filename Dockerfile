FROM clojure:onbuild
MAINTAINER Brian James Rubinton <brian@kanopi.io>

# NOTE: prefix in below env var value comes from alias used to link
# transactor container to kanopi-peer container

# Aliasing env vars from linked containers.
# Otherwise, the code accessing env vars would be coupled with the
# linked containers' names. I prefer that coupling to remain at one
# layer in the application.
ENV DATOMIC_TRANSACTOR_URI $DATOMIC_TRANSACTOR_ENV_DATOMIC_TRANSACTOR_URI
ENV DATOMIC_DATABASE_NAME $DATOMIC_DATABASE_ENV_DATABASE_NAME

EXPOSE 8080

RUN ["lein", "cljsbuild", "once"]

CMD ["lein", "run"]
