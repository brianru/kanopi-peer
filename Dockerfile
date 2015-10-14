FROM clojure:onbuild
MAINTAINER Brian James Rubinton <brian@kanopi.io>

# NOTE: prefix in below env var value comes from alias used to link
# transactor container to kanopi-peer container

EXPOSE 8080

RUN ["lein", "cljsbuild", "once"]

CMD ["lein", "run"]
