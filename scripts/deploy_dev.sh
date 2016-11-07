# deploy_dev.sh
#! /bin/bash

COMMIT_SHA=$1

lein build!
docker build -t tutum.co/$TUTUM_USER/kanopi-peer:$COMMIT_SHA .
docker push tutum.co/$TUTUM_USER/kanopi-peer:$COMMIT_SHA

#
# EB_DOCKERRUN_FILE = system-spec/dev-dockerrun.aws.json

# aws elasticbeanstalk create-application-version \
#     --application-name kanopi \
#     --version-label $COMMIT_SHA \
#     --source-bundle S3Bucket=$EB_SPEC_BUCKET,S3Key=$EB_DOCKERRUN_FILE

# aws elasticbeanstalk update-environment \
#     --environment-name kanopi-dev-env \
#     --version-label $COMMIT_SHA

