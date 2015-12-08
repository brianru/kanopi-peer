## Instructions for building and running docker container

~ docker build -t kanopi .
~ docker run -p 8080:8080 --rm --name running-kanopi kanopi

~ docker stop running-kanopi

## Env Vars
### Change Web Server Port
~ docker run --rm
             --name running-kanopi
             ;; run web server on 9090
             -e "WEB_SERVER_PORT=9090"
             ;; map host port 9090 to container port 9090
             -p 9090:9090
             kanopi

### Change Datomic Transactor URI
