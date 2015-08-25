#!/bin/sh

if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

VERSION=$1

# prod
s3cmd put suripu-admin.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-admin/$VERSION/suripu-admin.prod.yml

# staging
s3cmd put suripu-admin.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-admin/$VERSION/suripu-admin.staging.yml