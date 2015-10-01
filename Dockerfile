FROM clojure:onbuild
MAINTAINER Brian James Rubinton <brian@kanopi.io>

EXPOSE 8080

RUN ["lein", "cljsbuild", "once"]

CMD ["lein", "run"]
